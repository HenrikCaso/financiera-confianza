package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.SolicitudCreditoRepository;
import com.example.financieraconfianza.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class PrestamoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudCreditoRepository;

    // 1. Mostrar la pantalla del simulador
    @GetMapping("/prestamos")
    public String mostrarPrestamos(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);
        model.addAttribute("usuario", usuarioBD);

        return "prestamos";
    }

    // 2. Procesar la solicitud con el Motor de Riesgos e Historial
    @PostMapping("/prestamos/solicitar")
    @Transactional // Protección ACID para asegurar que el depósito y el registro ocurran juntos
    public String solicitarPrestamo(
            @RequestParam Double monto,
            @RequestParam Integer cuotas,
            @RequestParam Double ingresosMensuales,
            @RequestParam Double gastosMensuales,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        // --- MOTOR DE RIESGOS ---
        double capacidadLibre = ingresosMensuales - gastosMensuales;

        // Cálculo matemático del Sistema Francés en el Servidor (Seguridad)
        double tea = (cuotas == 6) ? 0.155 : (cuotas == 12) ? 0.185 : 0.225;
        double tem = Math.pow(1 + tea, 1.0 / 12.0) - 1;
        double cuotaMensual = (monto * tem) / (1 - Math.pow(1 + tem, -cuotas));

        // Instanciamos el objeto de auditoría para la base de datos
        SolicitudCredito auditoria = new SolicitudCredito();
        auditoria.setUsuario(usuarioBD);
        auditoria.setMonto(monto);
        auditoria.setCuotas(cuotas);
        auditoria.setFechaSolicitud(LocalDateTime.now());

        // REGLA DE NEGOCIO: Capacidad de endeudamiento máxima del 40% del saldo libre
        if (capacidadLibre <= 0 || cuotaMensual > (capacidadLibre * 0.40)) {

            // Caso A: RECHAZADO por el algoritmo
            auditoria.setEstado("RECHAZADO_RIESGO");
            solicitudCreditoRepository.save(auditoria); // Guardamos la auditoría del rechazo

            redirectAttributes.addFlashAttribute("error", "Lo sentimos. Tu nivel de gastos actuales no nos permite aprobar este préstamo por riesgo de sobreendeudamiento.");
        } else {

            // Caso B: APROBADO por el algoritmo
            auditoria.setEstado("APROBADO_ALGORITMO");
            solicitudCreditoRepository.save(auditoria); // Guardamos la auditoría del éxito

            // Desembolso inmediato del dinero
            usuarioBD.setSaldo(usuarioBD.getSaldo() + monto);
            usuarioRepository.save(usuarioBD);

            redirectAttributes.addFlashAttribute("exito", "¡Aprobado! El motor financiero validó tu excelente capacidad de pago. Se han depositado S/ " + monto + " en tu cuenta.");
        }

        return "redirect:/prestamos";
    }
}