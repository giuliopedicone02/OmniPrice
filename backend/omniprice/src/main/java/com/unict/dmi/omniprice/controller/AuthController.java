package com.unict.dmi.omniprice.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unict.dmi.omniprice.dto.UserDTO;

@RestController
@RequestMapping("/auth")
public class AuthController {

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody Map<String, String> request) {
        // TODO: Aggiungere logica di salvataggio nel database e hashing password

        // Mock della risposta per ora
        UserDTO response = new UserDTO();
        response.setId("usr_001");
        response.setEmail(request.get("email"));
        response.setName(request.get("name"));
        response.setRole("STANDARD");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        // TODO: Aggiungere verifica password e generazione vero token JWT

        Map<String, Object> response = new HashMap<>();
        response.put("token", "mock_jwt_token_da_sostituire_presto");
        response.put("expiresIn", 3600);

        UserDTO user = new UserDTO();
        user.setId("usr_001");
        user.setEmail(request.get("email"));
        user.setName("John Doe");
        user.setRole("STANDARD");

        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    // POST /api/auth/logout
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Logout successful");
        return ResponseEntity.ok(response);
    }
}