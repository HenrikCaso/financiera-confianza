package com.example.financieraconfianza.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Genera una llave secreta criptográfica super segura (HS256)
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Tiempo de vida del token: 1 hora en milisegundos
    private final long EXPIRATION_TIME = 3600000;

    // MÉTODO 1: Fabricar el Token cuando el usuario hace Login
    public String generarToken(String numDoc, String rol) {
        return Jwts.builder()
                .setSubject(numDoc)
                .claim("rol", rol) // Guardamos el rol (ADMIN o CLIENTE) dentro del token
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    // MÉTODO 2: Verificar que el Token sea auténtico y no esté modificado
    public boolean validarToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // El token es válido y firmado por nosotros
        } catch (Exception e) {
            return false; // El token expiró o es falso
        }
    }

    // MÉTODO 3: Extraer el rol sin tener que ir a la base de datos
    public String obtenerRolDeToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("rol", String.class);
    }
}