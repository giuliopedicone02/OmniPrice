package com.unict.dmi.omniprice.config;

import com.unict.dmi.omniprice.model.Alert;
import com.unict.dmi.omniprice.model.User;
import com.unict.dmi.omniprice.repository.AlertRepository;
import com.unict.dmi.omniprice.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seed iniziale del database con utenti e alert di test.
 * Credenziali e alert di default per la demo e i test.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AlertRepository alertRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AlertRepository alertRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.alertRepository = alertRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        User mario = seedUser("mario@example.com", "PasswordSuperSicura123!", "Mario Rossi", "STANDARD");
        User laura = seedUser("laura@example.com", "Password123!", "Laura Bianchi", "PREMIUM");
        seedUser("admin@example.com", "Admin1234!", "Admin OmniPrice", "ADMIN");

        seedDemoAlerts(mario, laura);
    }

    private User seedUser(String email, String password, String name, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setName(name);
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    private void seedDemoAlerts(User mario, User laura) {
        if (mario != null && alertRepository.findByUserId(mario.getId()).isEmpty()) {
            // Alert attivo per Mario (Laptop Dell XPS 13 a 1179€)
            Alert alert1 = new Alert();
            alert1.setUserId(mario.getId());
            alert1.setProductId("PROD001");
            alert1.setProductName("Laptop Dell XPS 13");
            alert1.setTargetPrice(1179.00);
            alert1.setStatus("ACTIVE");
            alertRepository.save(alert1);
        }

        if (laura != null && alertRepository.findByUserId(laura.getId()).isEmpty()) {
            // Alert scattato per Laura (iPhone 15 Pro Max)
            Alert alert2 = new Alert();
            alert2.setUserId(laura.getId());
            alert2.setProductId("PROD002");
            alert2.setProductName("iPhone 15 Pro Max");
            alert2.setTargetPrice(1150.00);
            alert2.setStatus("TRIGGERED");
            alert2.setTriggeredStore("TechMart");
            alert2.setTriggeredPrice(1139.99);
            alert2.setTriggeredAt(LocalDateTime.now().minusHours(2));
            alertRepository.save(alert2);

            // Alert attivo per Laura (Sony WH-1000XM5)
            Alert alert3 = new Alert();
            alert3.setUserId(laura.getId());
            alert3.setProductId("PROD003");
            alert3.setProductName("Sony WH-1000XM5 Headphones");
            alert3.setTargetPrice(350.00);
            alert3.setStatus("ACTIVE");
            alertRepository.save(alert3);
        }
    }
}
