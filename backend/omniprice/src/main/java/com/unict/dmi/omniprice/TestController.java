package com.unict.dmi.omniprice;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173") // FONDAMENTALE! Permette a Vue di parlare col backend
public class TestController {

    @GetMapping("/api/test")
    public String sayHello() {
        return "Ciao Vue, sono Spring Boot! Il collegamento funziona!";
    }
}