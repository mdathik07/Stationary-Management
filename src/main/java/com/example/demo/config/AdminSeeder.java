package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

/**
 * Admin accounts are never created through public registration; the shop's
 * admin login is seeded here from configuration on first startup.
 */
@Configuration
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    @Bean
    ApplicationRunner seedAdmin(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.email}") String adminEmail,
                                @Value("${app.admin.password}") String adminPassword,
                                @Value("${app.admin.name:Shop Admin}") String adminName) {
        return args -> {
            if (userRepository.existsByRole(User.Role.ADMIN)) {
                return;
            }
            User admin = User.builder()
                    .email(adminEmail.toLowerCase())
                    .password(passwordEncoder.encode(adminPassword))
                    .name(adminName)
                    .role(User.Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.warn("Seeded initial admin account '{}'. Change its password via the ADMIN_PASSWORD env var in production.", adminEmail);
        };
    }
}
