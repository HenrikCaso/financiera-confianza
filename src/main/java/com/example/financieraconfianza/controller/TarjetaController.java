package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TarjetaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. Mostrar la pantalla de tarjetas
    @GetMapping("/tarjetas")
    public String mostrarTarjetas(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        // Traemos el dato fresco para saber si está bloqueada o no
        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);
        model.addAttribute("usuario", usuarioBD);

        return "tarjetas";
    }
    
    @PostMapping("/tarjetas/toggle")
    public String toggleTarjeta(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        Usuario usuario = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        // Invertimos el estado actual
        boolean nuevoEstado = !usuario.getTarjetaBloqueada();
        usuario.setTarjetaBloqueada(nuevoEstado);
        usuarioRepository.save(usuario);

        if (nuevoEstado) {
            redirectAttributes.addFlashAttribute("alerta", "Tarjeta bloqueada por seguridad.");
        } else {
            redirectAttributes.addFlashAttribute("exito", "Tarjeta desbloqueada correctamente.");
        }

        return "redirect:/tarjetas";
    }

    // 2. Procesar el bloqueo / desbloqueo
    @PostMapping("/tarjetas/toggle-bloqueo")
    public String toggleBloqueo(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        // ¡Aquí está la magia del Toggle! Invertimos el estado actual
        boolean nuevoEstado = !usuarioBD.getTarjetaBloqueada();
        usuarioBD.setTarjetaBloqueada(nuevoEstado);

        usuarioRepository.save(usuarioBD);

        // Mensaje dinámico dependiendo de lo que el usuario acaba de hacer
        if (nuevoEstado) {
            // Si ahora es true (está bloqueada), mandamos alerta roja
            redirectAttributes.addFlashAttribute("error", "¡Tarjeta bloqueada por seguridad! No se podrán hacer compras.");
        } else {
            // Si ahora es false (está libre), mandamos alerta verde
            redirectAttributes.addFlashAttribute("exito", "¡Tarjeta desbloqueada con éxito! Ya puedes usarla.");
        }

        return "redirect:/tarjetas";
    }

    // AGREGA ESTE NUEVO MÉTODO PARA EL BLOQUEO POR ROBO
    @PostMapping("/tarjetas/bloqueo-definitivo")
    public String bloquearPorRobo(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        Usuario usuario = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        if (usuario != null) {
            usuario.setTarjetaBloqueada(true); // Bloquea la tarjeta
            usuario.setBloqueoDefinitivo(true); // Activa el candado de nivel 2 (Administrador)
            usuarioRepository.save(usuario);
        }

        redirectAttributes.addFlashAttribute("error", "ALERTA DE SEGURIDAD: Tu tarjeta ha sido bloqueada definitivamente por reporte de robo/extravío.");
        return "redirect:/tarjetas";
    }

}