package org.nikahgator.staff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nikahgator.catalog.AccessLevel;
import org.nikahgator.catalog.ServiceEntitlements;
import org.nikahgator.catalog.ServiceKey;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.entity.StaffPermission;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.nikahgator.repository.StaffPermissionRepository;
import org.nikahgator.service.EmailService;
import org.nikahgator.service.PasswordPolicy;
import org.nikahgator.staff.StaffDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The imam adds people to the masjid and picks, per service, whether each may only view or also manage. */
@Service
public class StaffService {
    private static final Logger log = LoggerFactory.getLogger(StaffService.class);

    private final AppUserRepository users;
    private final StaffPermissionRepository permissions;
    private final MasjidRepository masjids;
    private final ServiceEntitlements entitlements;
    private final PasswordEncoder encoder;
    private final EmailService email;

    @Value("${app.site-url:}")
    private String siteUrl;

    /** Local testing only (LOG_OTP=true): print the temporary password so it can be used without SMTP. */
    @Value("${app.mail.log-otp:false}")
    private boolean logOtp;

    public StaffService(AppUserRepository users, StaffPermissionRepository permissions, MasjidRepository masjids,
                        ServiceEntitlements entitlements, PasswordEncoder encoder, EmailService email) {
        this.users = users;
        this.permissions = permissions;
        this.masjids = masjids;
        this.entitlements = entitlements;
        this.encoder = encoder;
        this.email = email;
    }

    @Transactional(readOnly = true)
    public List<StaffView> list(AppUser imam) {
        return users.findByMasjidIdAndRoleOrderByFullName(imam.getMasjidId(), AppUser.STAFF).stream()
                .map(u -> view(u, false)).toList();
    }

    @Transactional(readOnly = true)
    public StaffView get(AppUser imam, Long id) {
        return view(find(imam, id), false);
    }

    @Transactional
    public StaffView create(AppUser imam, CreateStaff req) {
        String address = req.email().trim();
        if (users.findByEmailIgnoreCase(address).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with the email " + address + " already exists.");
        }
        Map<ServiceKey, AccessLevel> grants = parse(imam, req.permissions());
        AppUser staff = new AppUser();
        staff.setMasjidId(imam.getMasjidId());
        staff.setEmail(address);
        staff.setFullName(req.fullName().trim());
        staff.setPhone(blankToNull(req.phone()));
        staff.setJobTitle(blankToNull(req.jobTitle()));
        staff.setRole(AppUser.STAFF);
        boolean sent = issueTemporaryPassword(staff, imam, true);
        saveGrants(staff, grants);
        return view(staff, sent);
    }

    @Transactional
    public StaffView update(AppUser imam, Long id, UpdateStaff req) {
        AppUser staff = find(imam, id);
        Map<ServiceKey, AccessLevel> grants = parse(imam, req.permissions());
        staff.setFullName(req.fullName().trim());
        staff.setPhone(blankToNull(req.phone()));
        staff.setJobTitle(blankToNull(req.jobTitle()));
        users.save(staff);
        saveGrants(staff, grants);
        return view(staff, false);
    }

    @Transactional
    public StaffView setActive(AppUser imam, Long id, boolean active) {
        AppUser staff = find(imam, id);
        staff.setEnabled(active);
        users.save(staff);
        return view(staff, false);
    }

    @Transactional
    public StaffView resendCredentials(AppUser imam, Long id) {
        AppUser staff = find(imam, id);
        boolean sent = issueTemporaryPassword(staff, imam, false);
        return view(staff, sent);
    }

    /** Staff of this masjid only; anything else is "not found". */
    private AppUser find(AppUser imam, Long id) {
        return users.findById(id)
                .filter(u -> AppUser.STAFF.equals(u.getRole()) && imam.getMasjidId().equals(u.getMasjidId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Staff member not found"));
    }

    /** Only services the masjid actually has (switched on by the platform admin) can be granted. */
    private Map<ServiceKey, AccessLevel> parse(AppUser imam, Map<String, String> requested) {
        Set<ServiceKey> available = entitlements.enabledFor(imam);
        Map<ServiceKey, AccessLevel> out = new LinkedHashMap<>();
        if (requested == null) {
            return out;
        }
        for (var e : requested.entrySet()) {
            AccessLevel level = AccessLevel.parse(e.getValue());
            if (level == null) {
                continue; // NONE / blank: not granted
            }
            ServiceKey key;
            try {
                key = ServiceKey.valueOf(e.getKey());
            } catch (IllegalArgumentException ex) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown service " + e.getKey());
            }
            if (!available.contains(key)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, key.label() + " is not enabled for your masjid.");
            }
            out.put(key, level);
        }
        return out;
    }

    private void saveGrants(AppUser staff, Map<ServiceKey, AccessLevel> grants) {
        permissions.deleteByUserId(staff.getId());
        permissions.flush();
        grants.forEach((k, level) -> {
            StaffPermission p = new StaffPermission();
            p.setUserId(staff.getId());
            p.setServiceKey(k.name());
            p.setLevel(level.name());
            permissions.save(p);
        });
    }

    private boolean issueTemporaryPassword(AppUser staff, AppUser imam, boolean welcome) {
        Masjid masjid = masjids.findById(imam.getMasjidId()).orElseThrow();
        String password = PasswordPolicy.temporaryPassword();
        staff.setPasswordHash(encoder.encode(password));
        staff.setMustChangePassword(true);
        staff.setFailedAttempts(0);
        staff.setLockedUntil(null);
        users.save(staff);
        log.info("Staff account for {} ({}): temporary password issued", staff.getEmail(), masjid.getName());
        if (logOtp) {
            log.warn("LOG_OTP is on: temporary password for {} is {}", staff.getEmail(), password);
        }
        String site = siteUrl == null || siteUrl.isBlank() ? "" : "\nSign in at: " + siteUrl;
        return email.send(EmailService.Meta.of(EmailService.WELCOME, masjid.getId()), staff.getEmail(), "Your Masjidly account for " + masjid.getName(),
                "Assalamu alaikum " + staff.getFullName() + ",\n\n"
                        + (welcome ? imam.getFullName() + " added you to " + masjid.getName() + " on Masjidly."
                                   : "A new temporary password was issued for your Masjidly account.")
                        + site + "\n\nE-mail: " + staff.getEmail() + "\nTemporary password: " + password
                        + "\n\nWhen you sign in you will get a code by e-mail, and you must then choose a new password.",
                password);
    }

    private StaffView view(AppUser u, boolean emailed) {
        Map<String, String> grants = new LinkedHashMap<>();
        permissions.findByUserId(u.getId()).forEach(p -> grants.put(p.getServiceKey(), p.getLevel()));
        return new StaffView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getJobTitle(), u.isEnabled(),
                u.isMustChangePassword(), grants, emailed, u.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
