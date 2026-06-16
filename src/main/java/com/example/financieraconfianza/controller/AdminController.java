package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.config.JwtUtil;
import com.example.financieraconfianza.model.entity.Movimiento;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.MovimientoRepository;
import com.example.financieraconfianza.repository.SolicitudCreditoRepository;
import com.example.financieraconfianza.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudCreditoRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 1. PANTALLA DE LOGIN PARA EMPLEADOS
    @GetMapping("/admin/login")
    public String mostrarLoginAdmin(HttpSession session) {
        if (session.getAttribute("adminLogueado") != null) {
            return "redirect:/admin/prestamos";
        }
        return "admin-login";
    }

    // 2. PROCESAR LOGIN DE EMPLEADOS
    @PostMapping("/admin/login")
    public String procesarLoginAdmin(@RequestParam String numDoc,
                                     @RequestParam String clave,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByNumDoc(numDoc);

        if (usuarioOpt.isPresent() &&
                usuarioOpt.get().getClave().equals(clave) &&
                "ADMIN".equals(usuarioOpt.get().getRol())) {

            String token = jwtUtil.generarToken(usuarioOpt.get().getNumDoc(), "ADMIN");
            session.setAttribute("tokenJwt", token);
            session.setAttribute("adminLogueado", usuarioOpt.get());
            return "redirect:/admin/prestamos";
        } else {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado. Credenciales incorrectas o no eres ADMIN.");
            return "redirect:/admin/login";
        }
    }

    // 3. LA BANDEJA DE PRÉSTAMOS (EVALUACIÓN DE CRÉDITOS)
    @GetMapping("/admin/prestamos")
    public String bandejaPrestamos(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario adminSesion = (Usuario) session.getAttribute("adminLogueado");
        if (adminSesion == null) {
            redirectAttributes.addFlashAttribute("error", "Debes iniciar sesión en el portal corporativo.");
            return "redirect:/admin/login";
        }

        List<SolicitudCredito> pendientes = solicitudCreditoRepository.findByEstado("PENDIENTE");
        model.addAttribute("solicitudes", pendientes);
        model.addAttribute("admin", adminSesion);

        return "admin-prestamos";
    }

    // 4. CERRAR SESIÓN DEL ADMIN
    @GetMapping("/admin/logout")
    public String logoutAdmin(HttpSession session) {
        session.removeAttribute("adminLogueado");
        session.removeAttribute("tokenJwt");
        return "redirect:/admin/login";
    }

    @PostMapping("/admin/prestamos/aprobar/{id}")
    @Transactional
    public String aprobarSolicitud(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SolicitudCredito solicitud = solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud != null && "PENDIENTE".equals(solicitud.getEstado())) {

            if ("ROJO".equals(solicitud.getSemaforoRiesgo())) {
                redirectAttributes.addFlashAttribute("error", "Alerta de Riesgo: No se puede aprobar un crédito con RDS mayor al 40%.");
                return "redirect:/admin/prestamos";
            }

            Usuario cliente = solicitud.getUsuario();
            solicitud.setEstado("APROBADA");
            solicitudCreditoRepository.save(solicitud);

            cliente.setSaldo(cliente.getSaldo() + solicitud.getMonto());
            usuarioRepository.saveAndFlush(cliente);

            Movimiento mov = new Movimiento();
            mov.setTipo("PRÉSTAMO DESEMBOLSADO");
            mov.setMonto(solicitud.getMonto());
            mov.setFecha(LocalDateTime.now());
            mov.setConcepto("Crédito aprobado " + solicitud.getCuotas() + " cuotas (Días " + solicitud.getDiaPago() + ")");
            mov.setDestino("Cuenta Propia");
            mov.setUsuario(cliente);
            movimientoRepository.save(mov);

            redirectAttributes.addFlashAttribute("exito", "¡Crédito ID #" + id + " aprobado! El dinero ya está en la cuenta del cliente.");
        }
        return "redirect:/admin/prestamos";
    }

    @PostMapping("/admin/prestamos/rechazar/{id}")
    public String rechazarSolicitud(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        SolicitudCredito solicitud = solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud != null && "PENDIENTE".equals(solicitud.getEstado())) {
            solicitud.setEstado("RECHAZADA");
            solicitudCreditoRepository.save(solicitud);

            redirectAttributes.addFlashAttribute("error", "El crédito ID #" + id + " ha sido RECHAZADO.");
        }
        return "redirect:/admin/prestamos";
    }

    // =======================================================
    // NUEVO 5. BANDEJA DE SOPORTE / DESBLOQUEO DE TARJETAS
    // =======================================================
    @GetMapping("/admin/tarjetas")
    public String bandejaTarjetas(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario adminSesion = (Usuario) session.getAttribute("adminLogueado");
        if (adminSesion == null) return "redirect:/admin/login";

        // Filtramos y mandamos a la vista solo los usuarios que tienen la tarjeta bloqueada (temporal o definitivo)
        List<Usuario> clientesBloqueados = usuarioRepository.findByTarjetaBloqueada(true);
        model.addAttribute("clientes", clientesBloqueados);
        model.addAttribute("admin", adminSesion);

        return "admin-tarjetas";
    }

    // NUEVO 6. PROCESAR EL DESBLOQUEO DESDE LA AGENCIA (POST)
    @PostMapping("/admin/tarjetas/desbloquear/{id}")
    public String desbloquearTarjetaCliente(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Usuario cliente = usuarioRepository.findById(id).orElse(null);

        if (cliente != null) {
            cliente.setTarjetaBloqueada(false);
            cliente.setBloqueoDefinitivo(false); // Reseteamos por completo ambos candados
            usuarioRepository.save(cliente);

            redirectAttributes.addFlashAttribute("exito", "¡Operación Exitosa! La tarjeta del cliente " + cliente.getCorreo() + " ha sido restablecida y desbloqueada.");
        }
        return "redirect:/admin/tarjetas";
    }

    // =======================================================
    // NUEVO 7. BANDEJA DE GESTIÓN DE MORA
    // =======================================================
    @GetMapping("/admin/mora")
    public String bandejaMora(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario adminSesion = (Usuario) session.getAttribute("adminLogueado");
        if (adminSesion == null) return "redirect:/admin/login";

        // Para simular la gestión de mora de forma realista, listamos los créditos que ya fueron "APROBADOS"
        // y que se encuentran actualmente activos en la cartera de la financiera
        List<SolicitudCredito> creditosActivos = solicitudCreditoRepository.findByEstado("APROBADA");
        model.addAttribute("creditos", creditosActivos);
        model.addAttribute("admin", adminSesion);

        return "admin-mora";
    }
}