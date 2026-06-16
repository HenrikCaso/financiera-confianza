package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.model.entity.Movimiento;
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
import java.util.Optional;

@Controller
public class TransferenciaController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    // 1. DIBUJAR LA PANTALLA DE TRANSFERENCIAS (GET)
    @GetMapping("/transferencias")
    public String mostrarTransferencias(HttpSession session, Model model) {
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) return "redirect:/login";

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);
        model.addAttribute("usuario", usuarioBD);

        return "transferencias";
    }

    // 2. PROCESAR EL FORMULARIO AVANZADO (POST) - ¡BLINDADO CON ACID!
    @PostMapping("/transferencias")
    @Transactional
    public String procesarTransferencia(@RequestParam String cuentaDestino,
                                        @RequestParam Double monto,
                                        @RequestParam(required = false, defaultValue = "") String concepto,
                                        @RequestParam(required = false) String tipoTransferencia,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {

        Usuario remitente = (Usuario) session.getAttribute("usuarioLogueado");
        if (remitente == null) return "redirect:/login";

        // Filtro 1: Montos válidos
        if (monto <= 0) {
            redirectAttributes.addFlashAttribute("error", "El monto debe ser mayor a cero.");
            return "redirect:/transferencias";
        }

        Usuario remitenteBD = usuarioRepository.findById(remitente.getId()).orElse(null);

        if (remitenteBD.getTarjetaBloqueada() != null && remitenteBD.getTarjetaBloqueada()) {
            redirectAttributes.addFlashAttribute("error", "Operación denegada: Tu tarjeta se encuentra bloqueada. Por seguridad, no puedes realizar transferencias.");
            return "redirect:/transferencias";
        }

        // Filtro 2: No transferirse a uno mismo
        if (remitenteBD.getNumeroCuenta().equals(cuentaDestino)) {
            redirectAttributes.addFlashAttribute("error", "No puedes transferir dinero a tu propia cuenta.");
            return "redirect:/transferencias";
        }

        // Filtro 3: Buscar al destinatario usando tu input "cuentaDestino"
        Optional<Usuario> destinatarioOpt = usuarioRepository.findByNumeroCuenta(cuentaDestino);
        if (destinatarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "La cuenta destino (" + cuentaDestino + ") no existe en el banco.");
            return "redirect:/transferencias";
        }

        Usuario destinatarioBD = destinatarioOpt.get();

        // Filtro 4: Saldo suficiente
        if (remitenteBD.getSaldo() < monto) {
            redirectAttributes.addFlashAttribute("error", "Saldo insuficiente. Tienes S/ " + remitenteBD.getSaldo());
            return "redirect:/transferencias";
        }

        // =======================================================
        // ZONA CRÍTICA: MATEMÁTICA PROTEGIDA POR @Transactional
        // =======================================================
        remitenteBD.setSaldo(remitenteBD.getSaldo() - monto);
        destinatarioBD.setSaldo(destinatarioBD.getSaldo() + monto);

        usuarioRepository.save(remitenteBD);
        usuarioRepository.save(destinatarioBD);

        // Ajustamos el concepto por si el usuario lo dejó en blanco
        String conceptoFinal = concepto.isEmpty() ? "Transferencia enviada" : concepto;

        // Historial para el remitente (ROJO / NEGATIVO)
        Movimiento movSalida = new Movimiento();
        movSalida.setUsuario(remitenteBD);
        movSalida.setTipo(tipoTransferencia != null ? tipoTransferencia.toUpperCase() : "TRANSFERENCIA A TERCEROS");
        movSalida.setMonto(-monto);
        movSalida.setFecha(LocalDateTime.now());
        movSalida.setConcepto(conceptoFinal + " (Para: " + destinatarioBD.getCorreo() + ")");
        movSalida.setDestino(cuentaDestino);
        movimientoRepository.save(movSalida);

        // Historial para el destinatario (VERDE / POSITIVO)
        Movimiento movEntrada = new Movimiento();
        movEntrada.setUsuario(destinatarioBD);
        movEntrada.setTipo("ABONO RECIBIDO");
        movEntrada.setMonto(monto);
        movEntrada.setFecha(LocalDateTime.now());
        movEntrada.setConcepto("Depósito recibido de " + remitenteBD.getCorreo());
        movEntrada.setDestino("Cuenta Propia");
        movimientoRepository.save(movEntrada);

        // Mensaje de éxito final
        redirectAttributes.addFlashAttribute("exito", "¡Transferencia de S/ " + monto + " realizada con éxito a la cuenta " + cuentaDestino + "!");
        return "redirect:/transferencias";
    }
}