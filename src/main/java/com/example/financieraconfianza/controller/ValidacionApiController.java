package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class ValidacionApiController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/api/validar-cuenta")
    public ResponseEntity<?> validarCuenta(@RequestParam String cuenta) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByNumeroCuenta(cuenta);

        if (usuarioOpt.isPresent()) {
            Usuario destinatario = usuarioOpt.get();

            Map<String, String> respuesta = new HashMap<>();

            // Ahora solo enviamos el correo directo
            respuesta.put("nombre", destinatario.getCorreo());

            return ResponseEntity.ok(respuesta);

        } else {
            // Si la cuenta no existe, devolvemos un error 404 (Not Found).
            // Esto hará que tu JavaScript salte inmediatamente al bloque 'catch(error)'.
            return ResponseEntity.notFound().build();
        }
    }
}