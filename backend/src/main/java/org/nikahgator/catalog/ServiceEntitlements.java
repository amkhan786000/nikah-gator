package org.nikahgator.catalog;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.StaffPermission;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.StaffPermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides which services an imam may use, and lets the platform admin change that.
 * Effective = masjid allows it AND the imam's own override (if any) allows it.
 */
@Service
public class ServiceEntitlements {

    /** One service as the admin sees it. {@code imamEnabled} is null when the imam has no override of their own. */
    public record ServiceView(String key, String slug, String label, String description, String icon,
                              boolean masjidEnabled, Boolean imamEnabled, boolean effective) {
    }

    /** One service as the imam's menu needs it. */
    public record EnabledService(String key, String slug, String label, String icon, String level) {
    }

    private final MasjidServiceRepository masjidRows;
    private final ImamServiceRepository imamRows;
    private final MasjidRepository masjids;
    private final AppUserRepository users;
    private final StaffPermissionRepository staffRows;

    public ServiceEntitlements(MasjidServiceRepository masjidRows, ImamServiceRepository imamRows,
                               MasjidRepository masjids, AppUserRepository users, StaffPermissionRepository staffRows) {
        this.masjidRows = masjidRows;
        this.imamRows = imamRows;
        this.masjids = masjids;
        this.users = users;
        this.staffRows = staffRows;
    }

    /** The services this imam can use right now. */
    @Transactional(readOnly = true)
    public Set<ServiceKey> enabledFor(AppUser imam) {
        Map<String, Boolean> masjid = masjidRows.findByMasjidId(imam.getMasjidId()).stream()
                .collect(Collectors.toMap(MasjidService::getServiceKey, MasjidService::isEnabled));
        Map<String, Boolean> own = imamRows.findByUserId(imam.getId()).stream()
                .collect(Collectors.toMap(ImamService::getServiceKey, ImamService::isEnabled));
        Set<ServiceKey> out = EnumSet.noneOf(ServiceKey.class);
        for (ServiceKey k : ServiceKey.values()) {
            if (k.available() && masjid.getOrDefault(k.name(), k.defaultEnabled()) && own.getOrDefault(k.name(), true)) {
                out.add(k);
            }
        }
        return out;
    }

    /**
     * What this signed-in user may use: an imam gets every enabled service with full access; staff get the
     * services the imam granted, limited to what the platform admin has enabled for the masjid and its imam.
     */
    @Transactional(readOnly = true)
    public Map<ServiceKey, AccessLevel> accessFor(AppUser user) {
        Map<ServiceKey, AccessLevel> out = new java.util.EnumMap<>(ServiceKey.class);
        if (AppUser.IMAM.equals(user.getRole())) {
            enabledFor(user).forEach(k -> out.put(k, AccessLevel.MANAGE));
        } else if (AppUser.STAFF.equals(user.getRole())) {
            AppUser imam = users.findFirstByMasjidIdAndRole(user.getMasjidId(), AppUser.IMAM).orElse(null);
            if (imam != null) {
                Set<ServiceKey> on = enabledFor(imam);
                for (StaffPermission p : staffRows.findByUserId(user.getId())) {
                    AccessLevel level = AccessLevel.parse(p.getLevel());
                    ServiceKey key = Arrays.stream(ServiceKey.values()).filter(k -> k.name().equals(p.getServiceKey())).findFirst().orElse(null);
                    if (level != null && key != null && on.contains(key)) {
                        out.put(key, level);
                    }
                }
            }
        }
        return out;
    }

    public List<EnabledService> menuFor(AppUser user) {
        Map<ServiceKey, AccessLevel> access = accessFor(user);
        return Arrays.stream(ServiceKey.values()).filter(access::containsKey)
                .map(k -> new EnabledService(k.name(), k.slug(), k.label(), k.icon(), access.get(k).name())).toList();
    }

    /** Admin: every service with the masjid switch, the imam override and the result. */
    @Transactional(readOnly = true)
    public List<ServiceView> viewFor(Long masjidId) {
        requireMasjid(masjidId);
        Map<String, Boolean> masjid = masjidRows.findByMasjidId(masjidId).stream()
                .collect(Collectors.toMap(MasjidService::getServiceKey, MasjidService::isEnabled));
        AppUser imam = users.findFirstByMasjidIdAndRole(masjidId, AppUser.IMAM).orElse(null);
        Map<String, Boolean> own = imam == null ? Map.of() : imamRows.findByUserId(imam.getId()).stream()
                .collect(Collectors.toMap(ImamService::getServiceKey, ImamService::isEnabled));
        return Arrays.stream(ServiceKey.values()).filter(ServiceKey::available).map(k -> {
            boolean m = masjid.getOrDefault(k.name(), k.defaultEnabled());
            Boolean i = own.get(k.name());
            return new ServiceView(k.name(), k.slug(), k.label(), k.description(), k.icon(), m, i, m && (i == null || i));
        }).toList();
    }

    @Transactional
    public List<ServiceView> setForMasjid(Long masjidId, ServiceKey key, boolean enabled) {
        requireMasjid(masjidId);
        MasjidService row = masjidRows.findByMasjidIdAndServiceKey(masjidId, key.name()).orElseGet(() -> {
            MasjidService r = new MasjidService();
            r.setMasjidId(masjidId);
            r.setServiceKey(key.name());
            return r;
        });
        row.setEnabled(enabled);
        masjidRows.save(row);
        return viewFor(masjidId);
    }

    /** {@code enabled == null} removes the imam's override, so the masjid setting applies again. */
    @Transactional
    public List<ServiceView> setForImam(Long masjidId, ServiceKey key, Boolean enabled) {
        requireMasjid(masjidId);
        AppUser imam = users.findFirstByMasjidIdAndRole(masjidId, AppUser.IMAM)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This masjid has no imam yet."));
        var existing = imamRows.findByUserIdAndServiceKey(imam.getId(), key.name());
        if (enabled == null) {
            existing.ifPresent(imamRows::delete);
        } else {
            ImamService row = existing.orElseGet(() -> {
                ImamService r = new ImamService();
                r.setUserId(imam.getId());
                r.setServiceKey(key.name());
                return r;
            });
            row.setEnabled(enabled);
            imamRows.save(row);
        }
        return viewFor(masjidId);
    }

    private void requireMasjid(Long masjidId) {
        if (!masjids.existsById(masjidId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Masjid not found");
        }
    }
}
