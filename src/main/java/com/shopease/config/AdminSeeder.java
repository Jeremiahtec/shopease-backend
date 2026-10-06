package com.shopease.config;

import com.shopease.entity.User;
import com.shopease.enums.Role;
import com.shopease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first admin account on startup (admins cannot self-register). */
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String DEFAULT_PASSWORD = "Admin@12345";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = props.admin();
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        String email = admin.email().trim().toLowerCase();

        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (prod && passwordEncoder.matches(DEFAULT_PASSWORD, existing.getPassword())) {
                log.error("SECURITY: the admin account {} still uses the default password. Change it now.", email);
            }
            return;
        }

        if (prod && DEFAULT_PASSWORD.equals(admin.password())) {
            throw new IllegalStateException("Set ADMIN_PASSWORD to a strong, private value before the first production start");
        }
        User user = new User();
        user.setFullName(admin.fullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(admin.password()));
        user.setRole(Role.ADMIN);
        user.setEnabled(true);
        userRepository.save(user);
        log.info("Created admin account: {}", email);
    }
}
