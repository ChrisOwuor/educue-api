package com.owuor.educue.common.config;

import com.owuor.educue.roles.entity.Role;
import com.owuor.educue.roles.repository.RoleRepository;
import com.owuor.educue.users.entity.User;
import com.owuor.educue.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminSeeder.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap-admin.email:}")
    private String adminEmail;

    @Value("${app.bootstrap-admin.password:}")
    private String adminPassword;

    @Value("${app.bootstrap-admin.full-name:System Administrator}")
    private String adminFullName;

    public BootstrapAdminSeeder(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Only seed if NO users exist at all - this runs on every startup,
        // so without this guard it would try to create a duplicate every
        // single time the app restarts.
        if (userRepository.count() > 0) {
            return;
        }

        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.warn(
                    "No users exist yet, and BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD " +
                            "are not set. Skipping admin seed - you will not be able to log in until " +
                            "you set these env vars and restart, or insert a user manually."
            );
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found - did the V1 Flyway migration run?"
                ));

        User admin = new User();
        admin.setFullName(adminFullName);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setActive(true);

        userRepository.save(admin);

        log.info("✅ Bootstrap admin created: {}", adminEmail);
        log.warn("⚠️  Log in and consider changing this password, or disabling these env vars going forward.");
    }
}
