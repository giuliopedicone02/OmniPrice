package com.unict.dmi.omniprice.config;

import com.unict.dmi.omniprice.model.User;
import com.unict.dmi.omniprice.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seed iniziale del database con utenti di test.
 * Credenziali di default per il frontend LoginPage.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser("mario@example.com", "PasswordSuperSicura123!", "Mario Rossi", "STANDARD");
        seedUser("laura@example.com", "Password123!", "Laura Bianchi", "PREMIUM");
        seedUser("admin@example.com", "Admin1234!", "Admin OmniPrice", "ADMIN");
    }

    private void seedUser(String email, String password, String name, String role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setName(name);
            user.setRole(role);
            userRepository.save(user);
        }
    }
}
