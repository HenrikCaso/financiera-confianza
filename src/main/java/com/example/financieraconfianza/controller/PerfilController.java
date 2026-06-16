package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // 1. DIBUJAR LA PANTALLA DE PERFIL
    @GetMapping("/perfil")
    public String mostrarPerfil(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);
        model.addAttribute("usuario", usuarioBD);

        return "perfil";
    }

    // 2. PROCESAR LA ACTUALIZACIÓN DE DATOS
    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(
            @RequestParam String celular,
            @RequestParam(required = false) String clave,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        usuarioBD.setCelular(celular);

        // Cambiamos setPassword por setClave (asumiendo que así se llama en tu entidad)
        if (clave != null && !clave.trim().isEmpty()) {
            usuarioBD.setClave(clave);
        }

        usuarioRepository.save(usuarioBD);

        redirectAttributes.addFlashAttribute("exito", "¡Tus datos han sido actualizados correctamente!");
        return "redirect:/perfil";
    }
}