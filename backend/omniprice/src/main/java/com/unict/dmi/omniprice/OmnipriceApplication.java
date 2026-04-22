package com.unict.dmi.omniprice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point OmniPrice.
 * @EnableAsync  - abilita CompletableFuture per query parallele ai microservizi store
 * @EnableScheduling - abilita price check scheduler e batch write scheduler
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class OmnipriceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmnipriceApplication.class, args);
    }
}
