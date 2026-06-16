package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.config.JwtUtil;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.model.entity.Movimiento;
import com.example.financieraconfianza.repository.SolicitudCreditoRepository;
import com.example.financieraconfianza.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.financieraconfianza.repository.MovimientoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.time.format.DateTimeFormatter;
import com.example.financieraconfianza.service.PdfReporteService;
import com.example.financieraconfianza.repository.MovimientoRepository;
import com.example.financieraconfianza.model.entity.Movimiento;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import java.util.Optional;

@Controller
public class HomeController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudCreditoRepository;

    @Autowired
    private JwtUtil jwtUtil; // Inyectamos la herramienta JWT

    @Autowired
    private PdfReporteService pdfReporteService;

    @GetMapping("/dashboard/descargar-pdf")
    public void descargarEstadoCuenta(HttpSession session, HttpServletResponse response) throws IOException {
        // 1. Verificamos que el usuario esté logueado
        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) {
            response.sendRedirect("/login");
            return;
        }

        // 2. Traemos los datos frescos de la Base de Datos
        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(null);

        // 3. Traemos el historial de movimientos (usa el método que tengas en tu repositorio, por ejemplo findByUsuario)
        List<Movimiento> movimientos = movimientoRepository.findByUsuario(usuarioBD);

        // 4. Configuramos las cabeceras HTTP obligatorias para que el navegador descargue un archivo
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Estado_Cuenta_" + usuarioBD.getNumDoc() + ".pdf";
        response.setHeader(headerKey, headerValue);

        // 5. Enviamos todo al servicio para que dibuje y descargue el PDF
        pdfReporteService.generarEstadoCuentaPdf(usuarioBD, movimientos, response);
    }

    @GetMapping("/")
    public String home() { return "index"; }

    @GetMapping("/academia")
    public String academia() { return "academia"; }

    @GetMapping("/login")
    public String mostrarLogin(HttpSession session) {
        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String numDoc,
                                @RequestParam String clave,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByNumDoc(numDoc);

        if (usuarioOpt.isPresent() && usuarioOpt.get().getClave().equals(clave)) {
            // ¡NUEVO: Generamos el Token JWT para el Cliente!
            String token = jwtUtil.generarToken(usuarioOpt.get().getNumDoc(), "CLIENTE");
            session.setAttribute("tokenJwt", token); // Guardamos el pase VIP

            session.setAttribute("usuarioLogueado", usuarioOpt.get());
            return "redirect:/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("error", "El documento o la clave ingresada son incorrectos.");
            return "redirect:/login";
        }
    }

    @GetMapping("/registro")
    public String mostrarRegistro() { return "registro"; }
    
    // PROCESAR EL REGISTRO (CON SALDO INICIAL Y GENERADOR DE CUENTAS)
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
        nuevoUsuario.setSaldo(2500.00); // ¡SALDO INICIAL DE REGALO PARA PRUEBAS!

        // =========================================================
        // NUEVO: GENERAR CUENTA, CCI Y DESBLOQUEAR TARJETA AL NACER
        // =========================================================
        StringBuilder num = new StringBuilder("193");
        java.util.Random random = new java.util.Random();
        for(int i = 0; i < 11; i++) {
            num.append(random.nextInt(10));
        }
        nuevoUsuario.setNumeroCuenta(num.toString());
        nuevoUsuario.setCci("002" + num.toString() + "12");
        nuevoUsuario.setTarjetaBloqueada(false);
        // =========================================================

        // Finalmente, guardamos al usuario completo en Supabase
        usuarioRepository.save(nuevoUsuario);

        redirectAttributes.addFlashAttribute("exito", "¡Cuenta creada con éxito! Se te ha asignado un saldo de S/ 2,500.00 y tu número de cuenta oficial.");
        return "redirect:/login";
    }

    // ==========================================
    // DASHBOARD CON FILTRO DE FECHAS
    // ==========================================
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String fechaInicio,
                            @RequestParam(required = false) String fechaFin,
                            HttpSession session, Model model, RedirectAttributes redirectAttributes, HttpServletResponse response) {

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        Usuario usuarioSesion = (Usuario) session.getAttribute("usuarioLogueado");
        if (usuarioSesion == null) {
            redirectAttributes.addFlashAttribute("error", "Acceso denegado. Debes iniciar sesión primero.");
            return "redirect:/login";
        }

        Usuario usuarioBD = usuarioRepository.findById(usuarioSesion.getId()).orElse(usuarioSesion);
        // GENERADOR DE CUENTA Y CCI (Se ejecuta si el usuario es nuevo o no los tiene)
        boolean datosActualizados = false;

        if (usuarioBD.getNumeroCuenta() == null) {
            StringBuilder num = new StringBuilder("193"); // Simulando BCP
            java.util.Random random = new java.util.Random();
            for(int i=0; i<11; i++){
                num.append(random.nextInt(10));
            }
            usuarioBD.setNumeroCuenta(num.toString());
            datosActualizados = true;
        }

        if (usuarioBD.getCci() == null) {
            // El CCI en Perú suele ser el código del banco (ej. 002) + la cuenta + dígitos de control
            usuarioBD.setCci("002" + usuarioBD.getNumeroCuenta() + "12");
            datosActualizados = true;
        }

        if (datosActualizados) {
            // saveAndFlush obliga a la base de datos a guardar los datos en ese exacto milisegundo
            usuarioRepository.saveAndFlush(usuarioBD);
            session.setAttribute("usuarioLogueado", usuarioBD);
        }
        model.addAttribute("usuario", usuarioBD);

        // LÓGICA DEL FILTRO DE MOVIMIENTOS
        List<Movimiento> movimientos;
        if (fechaInicio != null && !fechaInicio.isEmpty() && fechaFin != null && !fechaFin.isEmpty()) {
            LocalDateTime inicio = LocalDate.parse(fechaInicio).atStartOfDay(); // 00:00:00
            LocalDateTime fin = LocalDate.parse(fechaFin).atTime(23, 59, 59);   // 23:59:59
            movimientos = movimientoRepository.findByUsuarioAndFechaBetweenOrderByFechaDesc(usuarioBD, inicio, fin);
        } else {
            movimientos = movimientoRepository.findByUsuarioOrderByFechaDesc(usuarioBD);
        }

        model.addAttribute("movimientos", movimientos);
        return "dashboard";
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}