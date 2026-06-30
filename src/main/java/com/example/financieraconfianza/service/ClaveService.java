package com.example.financieraconfianza.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ClaveService {

    private final PasswordEncoder passwordEncoder;

    public ClaveService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cifra una clave nueva utilizando BCrypt.
     */
    public String cifrar(String clavePlana) {
        return passwordEncoder.encode(clavePlana);
    }

    /**
     * Comprueba una clave.
     *
     * Permite temporalmente:
     * 1. Claves nuevas cifradas con BCrypt.
     * 2. Claves antiguas almacenadas en texto plano.
     */
    public boolean coincide(String claveIngresada, String claveGuardada) {

        if (claveIngresada == null || claveGuardada == null) {
            return false;
        }

        if (estaCifrada(claveGuardada)) {
            return passwordEncoder.matches(
                    claveIngresada,
                    claveGuardada
            );
        }

        // Compatibilidad temporal con claves antiguas
        return claveIngresada.equals(claveGuardada);
    }

    /**
     * Identifica hashes generados con BCrypt.
     */
    public boolean estaCifrada(String claveGuardada) {

        if (claveGuardada == null) {
            return false;
        }

        return claveGuardada.startsWith("$2a$")
                || claveGuardada.startsWith("$2b$")
                || claveGuardada.startsWith("$2y$");
    }
}