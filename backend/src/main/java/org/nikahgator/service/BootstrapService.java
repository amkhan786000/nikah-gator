package org.nikahgator.service;

import org.nikahgator.entity.AppUser;
import org.nikahgator.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * When there is no platform admin yet, creates one for ADMIN_EMAIL (or MAIL_USERNAME) with a random temporary
 * password, e-mails it to that address and prints it once in the backend log. The admin must change it at first
 * sign-in. Nothing happens on a database that already has a platform admin.
 */
@Service
public class BootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapService.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final EmailService email;

    @Value("${app.bootstrap.admin-email:}")
    private String adminEmail;
    @Value("${app.bootstrap.admin-name:Administrator}")
    private String adminName;
    @Value("${spring.mail.username:}")
    private String mailUsername;
    @Value("${app.site-url:}")
    private String siteUrl;

    public BootstrapService(AppUserRepository users, PasswordEncoder encoder, EmailService email) {
        this.users = users;
        this.encoder = encoder;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.countByRole(AppUser.PLATFORM_ADMIN) > 0) {
            return;
        }
        String address = !adminEmail.isBlank() ? adminEmail.trim() : mailUsername.trim();
        if (address.isEmpty()) {
            log.warn("Bootstrap: there is no platform admin and no ADMIN_EMAIL / MAIL_USERNAME is set, so none was created.");
            return;
        }
        if (users.findByEmailIgnoreCase(address).isPresent()) {
            log.warn("Bootstrap: there is no platform admin, but {} already belongs to another user. Not creating one.", address);
            return;
        }
        String password = PasswordPolicy.temporaryPassword();
        AppUser admin = new AppUser();
        admin.setEmail(address);
        admin.setFullName(adminName.isBlank() ? "Administrator" : adminName.trim());
        admin.setRole(AppUser.PLATFORM_ADMIN);
        admin.setPasswordHash(encoder.encode(password));
        admin.setMustChangePassword(true);
        users.save(admin);

        log.warn("Bootstrap: created the first platform admin. Sign in with e-mail {} and temporary password {} "
                + "(you will be asked to choose a new one).", address, password);
        email.send(EmailService.Meta.of(EmailService.WELCOME), address, "Your Masjidly administrator account",
                "Assalamu alaikum " + admin.getFullName() + ",\n\nAn administrator account was created for you"
                        + (siteUrl.isBlank() ? "" : " at " + siteUrl) + ".\n\nE-mail: " + address + "\nTemporary password: "
                        + password + "\n\nYou will get a code by e-mail when you sign in and must choose a new password.", password);
    }
}
