package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.Usuario;
import com.example.financieraconfianza.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==========================================
    // 1. RUTAS GET (Para mostrar las pantallas)
    // ==========================================

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/academia")
    public String academia() {
        return "academia";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    // ¡ESTOS DOS FALTABAN! Son para mostrar el HTML del login y registro
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login"; // Muestra login.html
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro"; // Muestra registro.html
    }


    // ==========================================
    // 2. RUTAS POST (Para procesar formularios)
    // ==========================================

    // PROCESAR EL REGISTRO
    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String tipoDoc,
                                   @RequestParam String numDoc,
                                   @RequestParam String correo,
                                   @RequestParam String celular,
                                   @RequestParam String clave,
                                   RedirectAttributes redirectAttributes) {

        if (usuarioRepository.findByNumDoc(numDoc).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "El número de documento ya se encuentra registrado.");
            return "redirect:/registro";
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setTipoDoc(tipoDoc);
        nuevoUsuario.setNumDoc(numDoc);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setCelular(celular);
        nuevoUsuario.setClave(clave);

        usuarioRepository.save(nuevoUsuario);

        redirectAttributes.addFlashAttribute("exito", "¡Cuenta creada con éxito! Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    // PROCESAR EL LOGIN
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String numDoc,
                                @RequestParam String clave,
                                RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByNumDoc(numDoc);

        if (usuarioOpt.isPresent() && usuarioOpt.get().getClave().equals(clave)) {
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "El documento o la clave ingresada son incorrectos.");
            return "redirect:/login";
        }
    }
}