package com.example.financieraconfianza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinancieraConfianzaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinancieraConfianzaApplication.class, args);
    }
}