package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.Movimiento;
import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.repository.MovimientoRepository;
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
public class ServicioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    // 1. DIBUJAR LA PANTALLA DE SERVICIOS
    @GetMapping("/servicios")
    public String mostrarServicios(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        // Traemos el saldo actualizado de la BD
        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);
        model.addAttribute("usuario", usuarioBD);

        return "servicios";
    }

    // 2. PROCESAR EL PAGO DEL RECIBO (PROTEGIDO CON ACID)
    @PostMapping("/servicios/pagar")
    @Transactional
    public String pagarServicio(@RequestParam String tipoServicio,
                                @RequestParam String codigoSuministro,
                                @RequestParam Double monto,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        Usuario titular = (Usuario) session.getAttribute("usuarioLogueado");
        if (titular == null) return "redirect:/login";

        // Filtro 1: Montos válidos
        if (monto <= 0) {
            redirectAttributes.addFlashAttribute("error", "El monto a pagar debe ser mayor a cero.");
            return "redirect:/servicios";
        }

        Usuario titularBD = usuarioRepository.findById(titular.getId()).orElse(null);

        if (titularBD.getTarjetaBloqueada() != null && titularBD.getTarjetaBloqueada()) {
            redirectAttributes.addFlashAttribute("error", "Operación denegada: Tu tarjeta se encuentra bloqueada. Por seguridad, no puedes pagar servicios.");
            return "redirect:/servicios";
        }   

        // Filtro 2: Saldo suficiente
        if (titularBD.getSaldo() < monto) {
            redirectAttributes.addFlashAttribute("error", "Saldo insuficiente. Tu saldo actual es S/ " + titularBD.getSaldo());
            return "redirect:/servicios";
        }

        // =======================================================
        // ZONA CRÍTICA: MATEMÁTICA PROTEGIDA POR @Transactional
        // =======================================================

        // 1. Descontamos el dinero del cliente
        titularBD.setSaldo(titularBD.getSaldo() - monto);
        usuarioRepository.save(titularBD);

        // 2. Generamos el comprobante en el historial (ROJO / NEGATIVO)
        Movimiento recibo = new Movimiento();
        recibo.setUsuario(titularBD);
        recibo.setTipo("PAGO DE SERVICIOS");
        recibo.setMonto(-monto);
        recibo.setFecha(LocalDateTime.now());
        // El concepto guardará el nombre de la empresa y el número de suministro
        recibo.setConcepto("Pago a " + tipoServicio + " (Suministro: " + codigoSuministro + ")");
        recibo.setDestino(tipoServicio);
        movimientoRepository.save(recibo);

        // Mensaje de éxito
        redirectAttributes.addFlashAttribute("exito", "¡Pago de S/ " + monto + " a " + tipoServicio + " realizado con éxito!");
        return "redirect:/servicios";
    }
}