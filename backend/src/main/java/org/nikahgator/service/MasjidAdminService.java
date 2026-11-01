package org.nikahgator.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.nikahgator.dto.AdminDtos.*;
import org.nikahgator.entity.AppUser;
import org.nikahgator.entity.Masjid;
import org.nikahgator.exception.ApiException;
import org.nikahgator.repository.AppUserRepository;
import org.nikahgator.repository.MasjidRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The platform admin creates a masjid together with its imam; the imam gets a temporary password by e-mail. */
@Service
public class MasjidAdminService {
    private static final Logger log = LoggerFactory.getLogger(MasjidAdminService.class);

    private final MasjidRepository masjids;
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final EmailService email;

    /** feature:reminders - told when a masjid is created (blank = nobody). */
    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;

    @Value("${app.site-url:}")
    private String siteUrl;

    /** Local testing only (LOG_OTP=true): print the temporary password so it can be used without SMTP. */
    @Value("${app.mail.log-otp:false}")
    private boolean logOtp;

    public MasjidAdminService(MasjidRepository masjids, AppUserRepository users, PasswordEncoder encoder, EmailService email) {
        this.masjids = masjids;
        this.users = users;
        this.encoder = encoder;
        this.email = email;
    }

    @Transactional
    public MasjidView create(CreateMasjidRequest req) {
        String imamEmail = req.imamEmail().trim();
        if (users.findByEmailIgnoreCase(imamEmail).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "A user with the email " + imamEmail + " already exists.");
        }
        Masjid masjid = new Masjid();
        masjid.setName(req.name().trim());
        masjid.setSlug(uniqueSlug(req.name()));
        masjid.setAddressLine(blankToNull(req.addressLine()));
        masjid.setCity(blankToNull(req.city()));
        masjid.setState(blankToNull(req.state()) == null ? null : req.state().trim().toUpperCase(Locale.ROOT));
        masjid.setZip(blankToNull(req.zip()));
        masjid.setPhone(blankToNull(req.phone()));
        masjid.setEmail(blankToNull(req.email()));
        if (req.timezone() != null && !req.timezone().isBlank()) {
            masjid.setTimezone(req.timezone().trim());
        }
        masjids.save(masjid);

        AppUser imam = new AppUser();
        imam.setMasjidId(masjid.getId());
        imam.setEmail(imamEmail);
        imam.setFullName(req.imamName().trim());
        imam.setPhone(blankToNull(req.imamPhone()));
        imam.setRole(AppUser.IMAM);

        // saves the imam together with a random temporary password
        boolean sent = issueTemporaryPassword(imam, masjid, true);
        if (adminEmail != null && !adminEmail.isBlank()) { // feature:reminders
            ReminderMessages.Message note = ReminderMessages.adminMasjidCreated(masjid.getName(), imam.getFullName(), imam.getEmail());
            email.send(EmailService.Meta.of(EmailService.OTHER), adminEmail.trim(), note.subject(), note.body());
        }
        return view(masjid, imam, sent);
    }

    public List<MasjidView> list() {
        return masjids.findAll().stream()
                .map(m -> view(m, users.findFirstByMasjidIdAndRole(m.getId(), AppUser.IMAM).orElse(null), false))
                .sorted((a, b) -> b.id().compareTo(a.id()))
                .toList();
    }

    /** Activating or deactivating a masjid also enables or disables its imam's sign-in. */
    @Transactional
    public MasjidView setActive(Long id, boolean active) {
        Masjid masjid = masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        masjid.setActive(active);
        masjids.save(masjid);
        AppUser imam = users.findFirstByMasjidIdAndRole(id, AppUser.IMAM).orElse(null);
        if (imam != null) {
            imam.setEnabled(active);
            users.save(imam);
        }
        return view(masjid, imam, false);
    }

    /** A new temporary password for the imam (lost e-mail, or the imam forgot it). */
    @Transactional
    public MasjidView resendCredentials(Long id) {
        Masjid masjid = masjids.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Masjid not found"));
        AppUser imam = users.findFirstByMasjidIdAndRole(id, AppUser.IMAM)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "This masjid has no imam yet."));
        boolean sent = issueTemporaryPassword(imam, masjid, false);
        return view(masjid, imam, sent);
    }

    private boolean issueTemporaryPassword(AppUser imam, Masjid masjid, boolean welcome) {
        String password = PasswordPolicy.temporaryPassword();
        imam.setPasswordHash(encoder.encode(password));
        imam.setMustChangePassword(true);
        imam.setFailedAttempts(0);
        imam.setLockedUntil(null);
        users.save(imam);
        log.info("Imam account for {} ({}): temporary password issued", imam.getEmail(), masjid.getName());
        if (logOtp) {
            log.warn("LOG_OTP is on: temporary password for {} is {}", imam.getEmail(), password);
        }
        String site = siteUrl == null || siteUrl.isBlank() ? "" : "\nSign in at: " + siteUrl;
        return email.send(EmailService.Meta.of(EmailService.WELCOME, masjid.getId()), imam.getEmail(), "Your Masjidly account for " + masjid.getName(),
                "Assalamu alaikum " + imam.getFullName() + ",\n\n"
                        + (welcome ? "An account was created for you to manage nikah records for " + masjid.getName() + "."
                                   : "A new temporary password was issued for your Masjidly account.")
                        + site + "\n\nE-mail: " + imam.getEmail() + "\nTemporary password: " + password
                        + "\n\nWhen you sign in you will get a code by e-mail, and you must then choose a new password.",
                password);
    }

    private MasjidView view(Masjid m, AppUser imam, boolean emailed) {
        return new MasjidView(m.getId(), m.getName(), m.getCity(), m.getState(), m.isActive(), m.getCreatedAt(),
                imam == null ? null : imam.getId(), imam == null ? null : imam.getFullName(),
                imam == null ? null : imam.getEmail(), imam != null && imam.isMustChangePassword(), emailed);
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "masjid";
        }
        if (base.length() > 70) {
            base = base.substring(0, 70);
        }
        String slug = base;
        for (int i = 2; masjids.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
