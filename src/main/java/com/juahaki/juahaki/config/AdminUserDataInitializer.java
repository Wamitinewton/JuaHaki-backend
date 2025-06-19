package com.juahaki.juahaki.config;

import com.juahaki.juahaki.enums.AuthProvider;
import com.juahaki.juahaki.enums.Role;
import com.juahaki.juahaki.model.user.User;
import com.juahaki.juahaki.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default.username:admin}")
    private String defaultAdminUsername;

    @Value("${app.admin.default.email:admin@juahaki.com}")
    private String defaultAdminEmail;

    @Value("${app.admin.default.password:Admin@123}")
    private String defaultAdminPassword;

    @Value("${app.admin.default.firstname:System}")
    private String defaultAdminFirstName;

    @Value("${app.admin.default.lastname:Administrator}")
    private String defaultAdminLastName;

    @Value("${app.admin.initialize:true}")
    private boolean initializeAdmin;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!initializeAdmin) {
            log.info("Admin user initialization is disabled");
            return;
        }

        try {
            initializeDefaultAdminUser();
        } catch (Exception e) {
            log.error("Failed to initialize default admin user", e);
        }
    }

    private void initializeDefaultAdminUser() {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Admin user already exists, skipping initialization");
            return;
        }

        if (userRepository.existsByUsername(defaultAdminUsername)) {
            log.warn("Username '{}' already exists, cannot create default admin", defaultAdminUsername);
            return;
        }

        if (userRepository.existsByEmail(defaultAdminEmail)) {
            log.warn("Email '{}' already exists, cannot create default admin", defaultAdminEmail);
            return;
        }

        User adminUser = createDefaultAdminUser();
        User savedUser = userRepository.save(adminUser);

        log.info("Default admin user created successfully with ID: {} and username: '{}'",
                savedUser.getId(), savedUser.getUsername());
        log.info("Default admin credentials - Username: '{}', Email: '{}'",
                defaultAdminUsername, defaultAdminEmail);

        if (isProductionEnvironment()) {
            log.warn("⚠️  SECURITY WARNING: Default admin user created in production environment. " +
                    "Please change the default password immediately!");
        }
    }

    private User createDefaultAdminUser() {
        return User.builder()
                .firstName(defaultAdminFirstName)
                .lastName(defaultAdminLastName)
                .username(defaultAdminUsername.toLowerCase().trim())
                .email(defaultAdminEmail.toLowerCase().trim())
                .password(passwordEncoder.encode(defaultAdminPassword))
                .phoneNumber("0712345678")
                .role(Role.ADMIN)
                .provider(AuthProvider.LOCAL)
                .emailVerified(true)
                .isEnabled(true)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .build();
    }

    private boolean isProductionEnvironment() {
        String profile = System.getProperty("spring.profiles.active", "");
        return profile.contains("prod") || profile.contains("production");
    }
}
