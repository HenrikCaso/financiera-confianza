package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.config.JwtUtil;
import com.example.financieraconfianza.model.Movimiento;
import com.example.financieraconfianza.model.entity.*;
import com.example.financieraconfianza.repository.*;
import com.example.financieraconfianza.service.ClaveService;
import com.example.financieraconfianza.service.CronogramaService;
import com.example.financieraconfianza.service.MoraService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import java.time.LocalDate;

@Controller
public class AdminController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudCreditoRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private HistorialPrestamoRepository historialPrestamoRepository;

    @Autowired
    private CronogramaPagoRepository cronogramaPagoRepository;

    @Autowired
    private ClaveService claveService;

    @Autowired
    private GestionCobranzaRepository gestionCobranzaRepository;

    @Autowired
    private MoraService moraService;

    @Autowired
    private MoraRepository moraRepository;

    @Autowired
    private CronogramaService cronogramaService;

    @Autowired
    private JwtUtil jwtUtil;

    private String obtenerRolSesion(HttpSession session) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null || adminSesion.getRol() == null) {
            return "";
        }

        return adminSesion.getRol().trim().toUpperCase();
    }

    private boolean tieneRol(HttpSession session, String... rolesPermitidos) {

        String rolActual = obtenerRolSesion(session);

        for (String rolPermitido : rolesPermitidos) {
            if (rolActual.equals(rolPermitido)) {
                return true;
            }
        }

        return false;
    }

    private String bloquearPorRol(
            RedirectAttributes redirectAttributes,
            String destino
    ) {
        redirectAttributes.addFlashAttribute(
                "error",
                "Acceso denegado. Tu rol no tiene permiso para realizar esta acción."
        );

        return "redirect:" + destino;
    }

    private String redireccionPorRol(
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addFlashAttribute(
                "error",
                "Acceso denegado. Tu rol no tiene permiso para ingresar a esa sección."
        );

        String rol = obtenerRolSesion(session);

        if ("RECUPERACIONES".equals(rol)) {
            return "redirect:/admin/mora";
        }

        if ("AUDITOR".equals(rol)) {
            return "redirect:/admin/auditoria";
        }

        if ("ASESOR".equals(rol)
                || "RIESGOS".equals(rol)
                || "OPERACIONES".equals(rol)) {
            return "redirect:/admin/prestamos";
        }

        if ("ADMIN".equals(rol)) {
            return "redirect:/admin/dashboard";
        }

        return "redirect:/admin/login";
    }

    @GetMapping("/admin/login")
    public String mostrarLoginAdmin(HttpSession session) {

        Usuario adminSesion = (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion != null) {

            String rol = adminSesion.getRol() == null
                    ? ""
                    : adminSesion.getRol().trim().toUpperCase();

            if ("RECUPERACIONES".equals(rol)) {
                return "redirect:/admin/mora";
            }

            if ("AUDITOR".equals(rol)) {
                return "redirect:/admin/auditoria";
            }

            if ("ASESOR".equals(rol) || "RIESGOS".equals(rol) || "OPERACIONES".equals(rol) || "ADMIN".equals(rol)) {
                return "redirect:/admin/prestamos";
            }
        }

        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String procesarLoginAdmin(
            @RequestParam String numDoc,
            @RequestParam String clave,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Optional<Usuario> usuarioOpt =
                usuarioRepository.findByNumDoc(numDoc.trim());

        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Acceso denegado. Credenciales incorrectas."
            );
            return "redirect:/admin/login";
        }

        Usuario admin = usuarioOpt.get();

        String rol = admin.getRol() == null
                ? ""
                : admin.getRol().trim().toUpperCase();

        List<String> rolesCorporativos = List.of(
                "ADMIN",
                "ASESOR",
                "RIESGOS",
                "OPERACIONES",
                "RECUPERACIONES",
                "AUDITOR"
        );

        if (!rolesCorporativos.contains(rol)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Acceso denegado. No tienes rol corporativo."
            );
            return "redirect:/admin/login";
        }

        if (!Boolean.TRUE.equals(admin.getActivo())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuenta corporativa se encuentra bloqueada."
            );
            return "redirect:/admin/login";
        }

        if (!claveService.coincide(clave, admin.getClave())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Acceso denegado. Credenciales incorrectas."
            );
            return "redirect:/admin/login";
        }

        // Migrar automáticamente la clave antigua si estaba sin cifrar
        if (!claveService.estaCifrada(admin.getClave())) {
            admin.setClave(claveService.cifrar(clave));
            usuarioRepository.save(admin);
        }

        String token = jwtUtil.generarToken(
                admin.getNumDoc(),
                rol
        );

        session.setAttribute("tokenJwt", token);
        session.setAttribute("adminLogueado", admin);
        session.setAttribute("rolAdmin", rol);

        if ("RECUPERACIONES".equals(rol)) {
            return "redirect:/admin/mora";
        }

        if ("AUDITOR".equals(rol)) {
            return "redirect:/admin/auditoria";
        }

        if ("ASESOR".equals(rol)
                || "RIESGOS".equals(rol)
                || "OPERACIONES".equals(rol)) {
            return "redirect:/admin/prestamos";
        }

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/prestamos")
    public String bandejaPrestamos(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Debes iniciar sesión en el portal corporativo."
            );

            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "ASESOR", "RIESGOS", "OPERACIONES")) {
            return redireccionPorRol(session, redirectAttributes);
        }

        List<SolicitudCredito> solicitudes =
                solicitudCreditoRepository.findAll()
                        .stream()
                        .filter(sol -> {

                            boolean pendiente =
                                    "PENDIENTE".equals(sol.getEstado());

                            boolean aprobadaSinDesembolso =
                                    "APROBADA".equals(sol.getEstado())
                                            && !Boolean.TRUE.equals(
                                            sol.getDesembolsado()
                                    );

                            return pendiente || aprobadaSinDesembolso;
                        })
                        .sorted((a, b) -> {

                            LocalDateTime fechaA =
                                    a.getFechaSolicitud() != null
                                            ? a.getFechaSolicitud()
                                            : LocalDateTime.MIN;

                            LocalDateTime fechaB =
                                    b.getFechaSolicitud() != null
                                            ? b.getFechaSolicitud()
                                            : LocalDateTime.MIN;

                            return fechaB.compareTo(fechaA);
                        })
                        .toList();

        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("admin", adminSesion);
        model.addAttribute("activePage", "prestamos");

        return "admin-prestamos";
    }

    @GetMapping("/admin/logout")
    public String logoutAdmin(HttpSession session) {
        session.removeAttribute("adminLogueado");
        session.removeAttribute("tokenJwt");
        session.removeAttribute("rolAdmin");
        return "redirect:/admin/login";
    }

    @PostMapping("/admin/prestamos/riesgos/{id}")
    @Transactional
    public String enviarARiesgos(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "ASESOR")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        SolicitudCredito solicitud =
                solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La solicitud no existe."
            );

            return "redirect:/admin/prestamos";
        }

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo se pueden enviar a riesgos las solicitudes pendientes."
            );

            return "redirect:/admin/prestamos";
        }

        String etapaActual =
                solicitud.getEtapa() != null
                        ? solicitud.getEtapa()
                        : "SOLICITADO";

        if (!"SOLICITADO".equals(etapaActual)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Esta solicitud ya fue enviada a otra etapa."
            );

            return "redirect:/admin/prestamos";
        }

        solicitud.setEtapa("EN_RIESGOS");
        solicitudCreditoRepository.save(solicitud);

        HistorialPrestamo historial = new HistorialPrestamo();
        historial.setSolicitudCredito(solicitud);
        historial.setEstado("EN_RIESGOS");
        historial.setObservacion("Solicitud enviada a evaluación de riesgos");
        historial.setFecha(LocalDateTime.now());
        historialPrestamoRepository.save(historial);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("ENVIAR_RIESGOS");
        auditoria.setDetalle(
                "Se envió la solicitud #" + solicitud.getId()
                        + " a evaluación de riesgos"
        );
        auditoria.setFecha(LocalDateTime.now());
        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "La solicitud #" + id + " fue enviada a evaluación de riesgos."
        );

        return "redirect:/admin/prestamos";
    }

    @PostMapping("/admin/prestamos/aprobar/{id}")
    @Transactional
    public String aprobarSolicitud(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "RIESGOS")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        SolicitudCredito solicitud =
                solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La solicitud no existe."
            );

            return "redirect:/admin/prestamos";
        }

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo se pueden aprobar solicitudes pendientes."
            );

            return "redirect:/admin/prestamos";
        }

        if (!"EN_RIESGOS".equals(solicitud.getEtapa())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Primero debes enviar la solicitud a evaluación de riesgos."
            );

            return "redirect:/admin/prestamos";
        }

        if ("ROJO".equals(solicitud.getSemaforoRiesgo())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Alerta de riesgo: no se puede aprobar un crédito con RDS mayor al 40%."
            );

            return "redirect:/admin/prestamos";
        }

        LocalDateTime ahora = LocalDateTime.now();

        solicitud.setEstado("APROBADA");
        solicitud.setEtapa("APROBADA");
        solicitud.setFechaAprobacion(ahora);
        solicitud.setDesembolsado(false);

        solicitudCreditoRepository.save(solicitud);

        HistorialPrestamo historial = new HistorialPrestamo();
        historial.setSolicitudCredito(solicitud);
        historial.setEstado("APROBADA");
        historial.setObservacion("Crédito aprobado por administrador. Pendiente de desembolso.");
        historial.setFecha(ahora);
        historialPrestamoRepository.save(historial);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("APROBAR_PRESTAMO");
        auditoria.setDetalle(
                "Se aprobó la solicitud #" + solicitud.getId()
                        + ". Pendiente de desembolso."
        );
        auditoria.setFecha(ahora);
        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Crédito #" + id + " aprobado. Ahora falta realizar el desembolso."
        );

        return "redirect:/admin/prestamos";
    }

    @PostMapping("/admin/prestamos/desembolsar/{id}")
    @Transactional
    public String desembolsarSolicitud(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "OPERACIONES")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        SolicitudCredito solicitud =
                solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La solicitud no existe."
            );

            return "redirect:/admin/prestamos";
        }

        if (!"APROBADA".equals(solicitud.getEstado())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo se pueden desembolsar créditos aprobados."
            );

            return "redirect:/admin/prestamos";
        }

        if (Boolean.TRUE.equals(solicitud.getDesembolsado())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Este crédito ya fue desembolsado."
            );

            return "redirect:/admin/prestamos";
        }

        Usuario cliente = solicitud.getUsuario();
        LocalDateTime ahora = LocalDateTime.now();

        solicitud.setEtapa("DESEMBOLSADO");
        solicitud.setFechaDesembolso(ahora);
        solicitud.setDesembolsado(true);

        solicitudCreditoRepository.saveAndFlush(solicitud);

        double saldoAnterior =
                cliente.getSaldo() != null
                        ? cliente.getSaldo()
                        : 0.0;

        cliente.setSaldo(
                saldoAnterior + solicitud.getMonto()
        );

        usuarioRepository.saveAndFlush(cliente);

        cronogramaService.generarCronograma(
                solicitud,
                ahora.toLocalDate()
        );

        Movimiento mov = new Movimiento();
        mov.setTipo("PRÉSTAMO DESEMBOLSADO");
        mov.setMonto(solicitud.getMonto());
        mov.setFecha(ahora);
        mov.setConcepto(
                "Crédito desembolsado "
                        + solicitud.getCuotas()
                        + " cuotas (Día "
                        + solicitud.getDiaPago()
                        + ")"
        );
        mov.setDestino("Cuenta Propia");
        mov.setUsuario(cliente);
        movimientoRepository.save(mov);

        HistorialPrestamo historial = new HistorialPrestamo();
        historial.setSolicitudCredito(solicitud);
        historial.setEstado("DESEMBOLSADA");
        historial.setObservacion("Monto abonado a la cuenta del cliente");
        historial.setFecha(ahora);
        historialPrestamoRepository.save(historial);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("DESEMBOLSAR_PRESTAMO");
        auditoria.setDetalle(
                "Se desembolsó el crédito #" + solicitud.getId()
                        + " por S/ " + solicitud.getMonto()
        );
        auditoria.setFecha(ahora);
        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Crédito #" + id + " desembolsado correctamente. El dinero ya está en la cuenta del cliente."
        );

        return "redirect:/admin/prestamos";
    }

    @PostMapping("/admin/prestamos/rechazar/{id}")
    @Transactional
    public String rechazarSolicitud(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "ASESOR", "RIESGOS")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        SolicitudCredito solicitud =
                solicitudCreditoRepository.findById(id).orElse(null);

        if (solicitud == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La solicitud no existe."
            );

            return "redirect:/admin/prestamos";
        }

        if (!"PENDIENTE".equals(solicitud.getEstado())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo se pueden rechazar solicitudes pendientes."
            );

            return "redirect:/admin/prestamos";
        }

        String rolActual = obtenerRolSesion(session);

        String etapaActual =
                solicitud.getEtapa() != null
                        ? solicitud.getEtapa()
                        : "SOLICITADO";

        boolean etapaSolicitado =
                "SOLICITADO".equals(etapaActual);

        boolean etapaRiesgos =
                "EN_RIESGOS".equals(etapaActual);

        boolean puedeRechazar =
                ("ADMIN".equals(rolActual)
                        && (etapaSolicitado || etapaRiesgos))
                        || ("ASESOR".equals(rolActual)
                        && etapaSolicitado)
                        || ("RIESGOS".equals(rolActual)
                        && etapaRiesgos);

        if (!puedeRechazar) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo puedes rechazar solicitudes iniciales o en riesgos segun tu rol."
            );

            return "redirect:/admin/prestamos";
        }

        solicitud.setEstado("RECHAZADA");
        solicitud.setEtapa("RECHAZADA");
        solicitud.setDesembolsado(false);

        solicitudCreditoRepository.save(solicitud);

        HistorialPrestamo historial = new HistorialPrestamo();
        historial.setSolicitudCredito(solicitud);
        historial.setEstado("RECHAZADA");
        historial.setObservacion("Crédito rechazado por administrador");
        historial.setFecha(LocalDateTime.now());
        historialPrestamoRepository.save(historial);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("RECHAZAR_PRESTAMO");
        auditoria.setDetalle(
                "Se rechazó la solicitud #" + solicitud.getId()
        );
        auditoria.setFecha(LocalDateTime.now());
        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "error",
                "El crédito #" + id + " fue rechazado."
        );

        return "redirect:/admin/prestamos";
    }

    @GetMapping("/admin/tarjetas")
    public String bandejaTarjetas(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Usuario adminSesion = (Usuario) session.getAttribute("adminLogueado");
        if (adminSesion == null) return "redirect:/admin/login";

        if (!tieneRol(session, "ADMIN", "OPERACIONES")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        // Filtramos y mandamos a la vista solo los usuarios que tienen la tarjeta bloqueada (temporal o definitivo)
        List<Usuario> clientesBloqueados = usuarioRepository.findByTarjetaBloqueada(true);
        model.addAttribute("clientes", clientesBloqueados);
        model.addAttribute("admin", adminSesion);
        model.addAttribute("activePage", "tarjetas");

        return "admin-tarjetas";
    }

    @PostMapping("/admin/tarjetas/desbloquear/{id}")
    public String desbloquearTarjetaCliente(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "OPERACIONES")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario cliente = usuarioRepository.findById(id).orElse(null);

        if (cliente != null) {
            cliente.setTarjetaBloqueada(false);
            cliente.setBloqueoDefinitivo(false);
            usuarioRepository.save(cliente);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "¡Operación Exitosa! La tarjeta del cliente "
                            + cliente.getCorreo()
                            + " ha sido restablecida y desbloqueada."
            );
        }

        return "redirect:/admin/tarjetas";
    }

    @PostMapping("/admin/usuarios/bloquear/{id}")
    @Transactional
    public String bloquearUsuario(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El usuario no existe."
            );

            return "redirect:/admin/usuarios";
        }

        if ("ADMIN".equals(usuario.getRol())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se puede bloquear a un administrador."
            );

            return "redirect:/admin/usuarios/detalle/" + id;
        }

        usuario.setActivo(false);
        usuario.setSesionActiva(false);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("BLOQUEAR_USUARIO");
        auditoria.setDetalle(
                "Se bloqueó al cliente con DNI " + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Usuario bloqueado correctamente."
        );

        return "redirect:/admin/usuarios/detalle/" + id;
    }

    @PostMapping("/admin/usuarios/desbloquear/{id}")
    @Transactional
    public String desbloquearUsuario(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El usuario no existe."
            );

            return "redirect:/admin/usuarios";
        }

        usuario.setActivo(true);
        usuario.setIntentosFallidos(0);
        usuario.setSesionActiva(false);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("DESBLOQUEAR_USUARIO");
        auditoria.setDetalle(
                "Se desbloqueó al cliente con DNI " + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Usuario desbloqueado correctamente."
        );

        return "redirect:/admin/usuarios/detalle/" + id;
    }

    @PostMapping("/admin/usuarios/tarjeta/bloquear/{id}")
    @Transactional
    public String bloquearTarjetaUsuario(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El usuario no existe."
            );

            return "redirect:/admin/usuarios";
        }

        usuario.setTarjetaBloqueada(true);
        usuario.setBloqueoDefinitivo(false);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("BLOQUEAR_TARJETA");
        auditoria.setDetalle(
                "Se bloqueó temporalmente la tarjeta del cliente con DNI "
                        + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Tarjeta bloqueada correctamente."
        );

        return "redirect:/admin/usuarios/detalle/" + id;
    }

    @PostMapping("/admin/usuarios/tarjeta/desbloquear/{id}")
    @Transactional
    public String desbloquearTarjetaUsuario(
            @PathVariable Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El usuario no existe."
            );

            return "redirect:/admin/usuarios";
        }

        usuario.setTarjetaBloqueada(false);
        usuario.setBloqueoDefinitivo(false);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("DESBLOQUEAR_TARJETA");
        auditoria.setDetalle(
                "Se desbloqueó la tarjeta del cliente con DNI "
                        + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Tarjeta desbloqueada correctamente."
        );

        return "redirect:/admin/usuarios/detalle/" + id;
    }

    @GetMapping("/admin/mora")
    public String bandejaMora(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "RECUPERACIONES")) {
            return redireccionPorRol(session, redirectAttributes);
        }

        // Actualiza los días de atraso e intereses moratorios
        moraService.sincronizarMoras();

        LocalDate hoy = LocalDate.now();

        // =====================================================
        // CUOTAS PENDIENTES
        // =====================================================

        List<CronogramaPago> cuotasPendientes =
                cronogramaPagoRepository
                        .findByPagadaFalse()
                        .stream()
                        .sorted((cuota1, cuota2) -> {

                            if (cuota1.getFechaVencimiento() == null
                                    && cuota2.getFechaVencimiento() == null) {
                                return 0;
                            }

                            if (cuota1.getFechaVencimiento() == null) {
                                return 1;
                            }

                            if (cuota2.getFechaVencimiento() == null) {
                                return -1;
                            }

                            return cuota1.getFechaVencimiento()
                                    .compareTo(cuota2.getFechaVencimiento());
                        })
                        .toList();

        Map<Long, GestionCobranza> ultimaGestionPorCuota = new HashMap<>();

        for (CronogramaPago cuota : cuotasPendientes) {
            gestionCobranzaRepository.buscarUltimaGestionPorCronograma(cuota.getId())
                    .ifPresent(gestion -> ultimaGestionPorCuota.put(cuota.getId(), gestion));
        }

        model.addAttribute("ultimaGestionPorCuota", ultimaGestionPorCuota);

        // =====================================================
        // CUOTAS VENCIDAS
        // =====================================================

        List<CronogramaPago> cuotasVencidas =
                cuotasPendientes.stream()
                        .filter(cuota ->
                                cuota.getFechaVencimiento() != null
                                        && cuota.getFechaVencimiento()
                                        .isBefore(hoy)
                        )
                        .toList();

        // =====================================================
        // TOTALES PRINCIPALES
        // =====================================================

        double totalPendiente =
                cuotasPendientes.stream()
                        .mapToDouble(cuota ->
                                cuota.getMontoCuota() != null
                                        ? cuota.getMontoCuota()
                                        : 0.0
                        )
                        .sum();

        double totalVencido =
                cuotasVencidas.stream()
                        .mapToDouble(cuota ->
                                cuota.getMontoCuota() != null
                                        ? cuota.getMontoCuota()
                                        : 0.0
                        )
                        .sum();

        // =====================================================
        // REGISTROS DE LA TABLA MORA
        // =====================================================

        List<Mora> registrosMora;

        if (cuotasPendientes.isEmpty()) {
            registrosMora = List.of();
        } else {
            registrosMora =
                    moraRepository.findByCronogramaPagoIn(
                            cuotasPendientes
                    );
        }

        // Relaciona el ID de cada cuota con su registro de mora
        Map<Long, Mora> morasPorCuota = new HashMap<>();

        for (Mora mora : registrosMora) {

            if (mora.getCronogramaPago() != null
                    && mora.getCronogramaPago().getId() != null) {

                morasPorCuota.put(
                        mora.getCronogramaPago().getId(),
                        mora
                );
            }
        }

        // =====================================================
        // INTERÉS MORATORIO
        // =====================================================

        double totalInteresMora =
                registrosMora.stream()
                        .filter(mora ->
                                mora.getDiasAtraso() != null
                                        && mora.getDiasAtraso() > 0
                                        && !"REGULARIZADA".equals(
                                        mora.getEstado()
                                )
                        )
                        .mapToDouble(mora ->
                                mora.getInteresMora() != null
                                        ? mora.getInteresMora()
                                        : 0.0
                        )
                        .sum();

        double totalVencidoActualizado =
                totalVencido + totalInteresMora;

        double totalCarteraActualizada =
                totalPendiente + totalInteresMora;

        // =====================================================
        // CLASIFICACIÓN DE CARTERA
        // =====================================================

        long cuotasAlDia =
                cuotasPendientes.stream()
                        .filter(cuota ->
                                "AL DIA".equals(cuota.getEstadoMora())
                        )
                        .count();

        long moraTemprana =
                cuotasPendientes.stream()
                        .filter(cuota ->
                                "MORA TEMPRANA".equals(
                                        cuota.getEstadoMora()
                                )
                        )
                        .count();

        long moraIntermedia =
                cuotasPendientes.stream()
                        .filter(cuota ->
                                "MORA INTERMEDIA".equals(
                                        cuota.getEstadoMora()
                                )
                        )
                        .count();

        long moraCritica =
                cuotasPendientes.stream()
                        .filter(cuota ->
                                "MORA CRITICA".equals(
                                        cuota.getEstadoMora()
                                )
                        )
                        .count();

        // =====================================================
        // DATOS ENVIADOS AL HTML
        // =====================================================

        model.addAttribute("cuotas", cuotasPendientes);

        model.addAttribute(
                "totalCuotasPendientes",
                cuotasPendientes.size()
        );

        model.addAttribute(
                "totalCuotasVencidas",
                cuotasVencidas.size()
        );

        model.addAttribute(
                "totalPendiente",
                totalPendiente
        );

        model.addAttribute(
                "totalVencido",
                totalVencido
        );

        model.addAttribute(
                "morasPorCuota",
                morasPorCuota
        );

        model.addAttribute(
                "totalInteresMora",
                totalInteresMora
        );

        model.addAttribute(
                "totalVencidoActualizado",
                totalVencidoActualizado
        );

        model.addAttribute(
                "totalCarteraActualizada",
                totalCarteraActualizada
        );

        model.addAttribute(
                "cuotasAlDia",
                cuotasAlDia
        );

        model.addAttribute(
                "moraTemprana",
                moraTemprana
        );

        model.addAttribute(
                "moraIntermedia",
                moraIntermedia
        );

        model.addAttribute(
                "moraCritica",
                moraCritica
        );

        model.addAttribute(
                "admin",
                adminSesion
        );

        model.addAttribute(
                "activePage",
                "mora"
        );

        return "admin-mora";
    }

    @GetMapping("/admin/auditoria")
    public String verAuditoria(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if(adminSesion == null){
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "AUDITOR")) {
            return redireccionPorRol(session, redirectAttributes);
        }

        model.addAttribute(
                "auditorias",
                auditoriaRepository.findAllByOrderByFechaDesc()
        );
        model.addAttribute("activePage", "auditoria");

        return "admin-auditoria";
    }

    @GetMapping("/admin/dashboard")
    public String dashboardAdmin(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if(adminSesion == null){
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return redireccionPorRol(session, redirectAttributes);
        }

        List<Usuario> clientes =
                usuarioRepository.findByRol("CLIENTE");

        List<SolicitudCredito> solicitudes =
                solicitudCreditoRepository.findAll();

        List<CronogramaPago> cuotas =
                cronogramaPagoRepository.findAll();

        List<Movimiento> movimientos =
                movimientoRepository.findAll();

        List<Auditoria> auditorias =
                auditoriaRepository.findAllByOrderByFechaDesc();

        long etapaSolicitado =
                solicitudes.stream()
                        .filter(s -> s.getEtapa() == null
                                || "SOLICITADO".equals(s.getEtapa()))
                        .count();

        long etapaRiesgos =
                solicitudes.stream()
                        .filter(s -> "EN_RIESGOS".equals(s.getEtapa()))
                        .count();

        long etapaAprobada =
                solicitudes.stream()
                        .filter(s -> "APROBADA".equals(s.getEtapa()))
                        .count();

        long etapaDesembolsado =
                solicitudes.stream()
                        .filter(s -> "DESEMBOLSADO".equals(s.getEtapa()))
                        .count();

        long etapaFinalizado =
                solicitudes.stream()
                        .filter(s -> "FINALIZADO".equals(s.getEtapa())
                                || "CANCELADO".equals(s.getEstado()))
                        .count();

        long etapaRechazada =
                solicitudes.stream()
                        .filter(s -> "RECHAZADA".equals(s.getEtapa())
                                || "RECHAZADA".equals(s.getEstado()))
                        .count();

        long solicitudesPendientes =
                solicitudes.stream()
                        .filter(s -> "PENDIENTE".equals(s.getEstado()))
                        .count();

        long creditosAprobados =
                solicitudes.stream()
                        .filter(s -> "APROBADA".equals(s.getEtapa()))
                        .count();

        long creditosDesembolsados =
                solicitudes.stream()
                        .filter(s -> Boolean.TRUE.equals(s.getDesembolsado()))
                        .count();

        long creditosFinalizados =
                etapaFinalizado;

        long cuotasPendientes =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .count();

        LocalDate hoy = LocalDate.now();

        long cuotasVencidas =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .filter(c -> c.getFechaVencimiento() != null)
                        .filter(c -> c.getFechaVencimiento().isBefore(hoy))
                        .count();

        double montoTotalDesembolsado =
                solicitudes.stream()
                        .filter(s -> Boolean.TRUE.equals(s.getDesembolsado()))
                        .mapToDouble(s -> s.getMonto() != null
                                ? s.getMonto()
                                : 0.0)
                        .sum();

        double totalPagadoClientes =
                movimientos.stream()
                        .filter(m -> "PAGO DE CUOTA".equals(m.getTipo()))
                        .mapToDouble(m -> m.getMonto() != null
                                ? Math.abs(m.getMonto())
                                : 0.0)
                        .sum();

        double montoPendienteCobrar =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .mapToDouble(c -> c.getMontoCuota() != null
                                ? c.getMontoCuota()
                                : 0.0)
                        .sum();

        long carteraAlDia =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .filter(c -> c.getDiasAtraso() == 0)
                        .count();

        long moraTemprana =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .filter(c -> c.getDiasAtraso() > 0
                                && c.getDiasAtraso() <= 15)
                        .count();

        long moraIntermedia =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .filter(c -> c.getDiasAtraso() > 15
                                && c.getDiasAtraso() <= 30)
                        .count();

        long moraCritica =
                cuotas.stream()
                        .filter(c -> !Boolean.TRUE.equals(c.getPagada()))
                        .filter(c -> c.getDiasAtraso() > 30)
                        .count();

        List<SolicitudCredito> ultimasSolicitudes =
                solicitudes.stream()
                        .sorted((a, b) -> {
                            LocalDateTime fechaA =
                                    a.getFechaSolicitud();

                            LocalDateTime fechaB =
                                    b.getFechaSolicitud();

                            if (fechaA == null && fechaB == null) {
                                return 0;
                            }

                            if (fechaA == null) {
                                return 1;
                            }

                            if (fechaB == null) {
                                return -1;
                            }

                            return fechaB.compareTo(fechaA);
                        })
                        .limit(8)
                        .toList();

        List<Auditoria> actividadReciente =
                auditorias.stream()
                        .limit(8)
                        .toList();

        model.addAttribute("totalClientes", clientes.size());
        model.addAttribute("solicitudesPendientes", solicitudesPendientes);
        model.addAttribute("creditosAprobados", creditosAprobados);
        model.addAttribute("creditosDesembolsados", creditosDesembolsados);
        model.addAttribute("creditosFinalizados", creditosFinalizados);
        model.addAttribute(
                "tarjetasBloqueadas",
                usuarioRepository.findByTarjetaBloqueada(true).size()
        );
        model.addAttribute("cuotasPendientes", cuotasPendientes);
        model.addAttribute("cuotasVencidas", cuotasVencidas);
        model.addAttribute(
                "montoTotalDesembolsado",
                montoTotalDesembolsado
        );
        model.addAttribute("totalPagadoClientes", totalPagadoClientes);
        model.addAttribute("montoPendienteCobrar", montoPendienteCobrar);

        model.addAttribute("etapaSolicitado", etapaSolicitado);
        model.addAttribute("etapaRiesgos", etapaRiesgos);
        model.addAttribute("etapaAprobada", etapaAprobada);
        model.addAttribute("etapaDesembolsado", etapaDesembolsado);
        model.addAttribute("etapaFinalizado", etapaFinalizado);
        model.addAttribute("etapaRechazada", etapaRechazada);

        model.addAttribute("carteraAlDia", carteraAlDia);
        model.addAttribute("moraTemprana", moraTemprana);
        model.addAttribute("moraIntermedia", moraIntermedia);
        model.addAttribute("moraCritica", moraCritica);

        model.addAttribute("ultimasSolicitudes", ultimasSolicitudes);
        model.addAttribute("actividadReciente", actividadReciente);

        model.addAttribute(
                "activePage",
                "dashboard"
        );

        return "admin-dashboard";
    }

    @GetMapping("/admin/prestamo/{id}")
    public String detallePrestamo(
            @PathVariable Long id,
            @RequestParam(defaultValue = "prestamos") String origen,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!"mora".equals(origen)
                && !"prestamos".equals(origen)) {

            origen = "prestamos";
        }

        boolean accesoPrestamos =
                "prestamos".equals(origen)
                        && tieneRol(
                        session,
                        "ADMIN",
                        "ASESOR",
                        "RIESGOS",
                        "OPERACIONES"
                );

        boolean accesoMora =
                "mora".equals(origen)
                        && tieneRol(
                        session,
                        "ADMIN",
                        "RECUPERACIONES"
                );

        if (!accesoPrestamos && !accesoMora) {
            return redireccionPorRol(
                    session,
                    redirectAttributes
            );
        }

        SolicitudCredito credito =
                solicitudCreditoRepository
                        .findById(id)
                        .orElse(null);

        if (credito == null) {
            return "mora".equals(origen)
                    ? "redirect:/admin/mora"
                    : "redirect:/admin/prestamos";
        }

        List<CronogramaPago> cuotas =
                cronogramaPagoRepository
                        .findBySolicitudCreditoOrderByNumeroCuotaAsc(
                                credito
                        );

        List<HistorialPrestamo> historial =
                historialPrestamoRepository
                        .findBySolicitudCreditoOrderByFechaAsc(
                                credito
                        );

        model.addAttribute("credito", credito);
        model.addAttribute("cuotas", cuotas);
        model.addAttribute("historial", historial);
        model.addAttribute("origen", origen);

        model.addAttribute(
                "activePage",
                "mora".equals(origen)
                        ? "mora"
                        : "prestamos"
        );

        return "admin-detalle-prestamo";
    }

    @GetMapping("/admin/prestamos/detalle/{id}")
    public String verDetallePrestamo(
            @PathVariable Long id,
            HttpSession session,
            Model model
    ) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if(adminSesion == null){
            return "redirect:/admin/login";
        }

        return "redirect:/admin/prestamo/" + id + "?origen=prestamos";
    }

    @GetMapping("/admin/usuarios")
    public String gestionUsuarios(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int pagina,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        // Evita números de página negativos.
        if (pagina < 0) {
            pagina = 0;
        }

        Pageable pageable = PageRequest.of(
                pagina,
                10,
                Sort.by("id").descending()
        );

        Page<Usuario> paginaUsuarios;

        if (buscar == null || buscar.trim().isEmpty()) {

            paginaUsuarios =
                    usuarioRepository.findByRol(
                            "CLIENTE",
                            pageable
                    );

        } else {

            paginaUsuarios =
                    usuarioRepository.buscarClientes(
                            buscar.trim(),
                            pageable
                    );
        }

        model.addAttribute(
                "usuarios",
                paginaUsuarios.getContent()
        );

        model.addAttribute(
                "paginaActual",
                paginaUsuarios.getNumber()
        );

        model.addAttribute(
                "totalPaginas",
                paginaUsuarios.getTotalPages()
        );

        model.addAttribute(
                "totalElementos",
                paginaUsuarios.getTotalElements()
        );

        model.addAttribute(
                "primeraPagina",
                paginaUsuarios.isFirst()
        );

        model.addAttribute(
                "ultimaPagina",
                paginaUsuarios.isLast()
        );

        model.addAttribute("buscar", buscar);
        model.addAttribute("activePage", "usuarios");

        return "admin-usuarios";
    }
    @GetMapping("/admin/usuarios/detalle/{id}")
    public String detalleUsuario(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) { 

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if(adminSesion == null){
            return "redirect:/admin/login";
        }

        Usuario usuario =
                usuarioRepository.findById(id)
                        .orElse(null);

        if(usuario == null){
            return "redirect:/admin/usuarios";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        model.addAttribute(
                "usuario",
                usuario
        );

        model.addAttribute(
                "prestamos",
                solicitudCreditoRepository
                        .findByUsuarioOrderByFechaSolicitudDesc(usuario)
        );
        model.addAttribute("activePage", "usuarios");

        model.addAttribute(
                "movimientos",
                movimientoRepository
                        .findTop10ByUsuarioOrderByFechaDesc(usuario)
        );

        return "admin-detalle-usuario";
    }

    @GetMapping("/admin/usuarios/editar/{id}")
    public String mostrarEditarUsuario(
            @PathVariable Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario =
                usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El cliente no existe."
            );

            return "redirect:/admin/usuarios";
        }

        if (!"CLIENTE".equals(usuario.getRol())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solo se pueden editar usuarios con rol CLIENTE."
            );

            return "redirect:/admin/usuarios";
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("activePage", "usuarios");

        return "admin-editar-usuario";
    }


    @PostMapping("/admin/usuarios/editar/{id}")
    @Transactional
    public String actualizarUsuario(
            @PathVariable Long id,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String correo,
            @RequestParam String celular,
            @RequestParam(required = false) String direccion,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario =
                usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El cliente no existe."
            );

            return "redirect:/admin/usuarios";
        }

        if (!"CLIENTE".equals(usuario.getRol())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se puede modificar este tipo de usuario."
            );

            return "redirect:/admin/usuarios";
        }

        nombres = nombres == null ? "" : nombres.trim();
        apellidos = apellidos == null ? "" : apellidos.trim();
        correo = correo == null ? "" : correo.trim();
        celular = celular == null ? "" : celular.trim();
        direccion = direccion == null ? "" : direccion.trim();

        if (nombres.isEmpty() || apellidos.isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Los nombres y apellidos son obligatorios."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        if (correo.isEmpty() || !correo.contains("@")) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Ingresa un correo válido."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        if (!celular.matches("\\d{9}")) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El celular debe tener exactamente 9 dígitos."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        if (direccion.length() > 250) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La dirección no puede superar los 250 caracteres."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        // Solo se modifican los campos permitidos
        usuario.setNombres(nombres);
        usuario.setApellidos(apellidos);
        usuario.setCorreo(correo);
        usuario.setCelular(celular);
        usuario.setDireccion(direccion);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("EDITAR_CLIENTE");
        auditoria.setDetalle(
                "Se actualizaron los datos personales del cliente con DNI "
                        + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Datos del cliente actualizados correctamente."
        );

        return "redirect:/admin/usuarios/detalle/" + id;
    }


    @PostMapping("/admin/usuarios/restablecer-clave/{id}")
    @Transactional
    public String restablecerClaveUsuario(
            @PathVariable Long id,
            @RequestParam String nuevaClave,
            @RequestParam String confirmarClave,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/prestamos"
            );
        }

        Usuario usuario =
                usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "El cliente no existe."
            );

            return "redirect:/admin/usuarios";
        }

        if (!"CLIENTE".equals(usuario.getRol())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se puede restablecer la clave de este usuario."
            );

            return "redirect:/admin/usuarios";
        }

        if (nuevaClave == null || !nuevaClave.matches("\\d{6}")) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La nueva clave debe contener exactamente 6 dígitos."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        if (!nuevaClave.equals(confirmarClave)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Las claves ingresadas no coinciden."
            );

            return "redirect:/admin/usuarios/editar/" + id;
        }

        usuario.setClave(
                claveService.cifrar(nuevaClave)
        );
        usuario.setCambioClaveObligatorio(true);
        usuario.setIntentosFallidos(0);
        usuario.setSesionActiva(false);

        usuarioRepository.save(usuario);

        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("RESTABLECER_CLAVE");
        auditoria.setDetalle(
                "Se restableció la clave del cliente con DNI "
                        + usuario.getNumDoc()
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "Clave restablecida correctamente."
        );

        return "redirect:/admin/usuarios/editar/" + id;
    }

    @GetMapping("/admin/mora/gestionar/{cuotaId}")
    public String mostrarGestionCobranza(
            @PathVariable Long cuotaId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "RECUPERACIONES")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/mora"
            );
        }

        CronogramaPago cuota =
                cronogramaPagoRepository.findById(cuotaId)
                        .orElse(null);

        if (cuota == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuota seleccionada no existe."
            );

            return "redirect:/admin/mora";
        }

        if (Boolean.TRUE.equals(cuota.getPagada())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se puede gestionar cobranza sobre una cuota pagada."
            );

            return "redirect:/admin/mora";
        }

        List<GestionCobranza> historial =
                gestionCobranzaRepository
                        .findByCronogramaPagoOrderByFechaGestionDesc(cuota);

        model.addAttribute("cuota", cuota);
        model.addAttribute("historialGestiones", historial);
        model.addAttribute("activePage", "mora");

        return "admin-gestion-cobranza";
    }

    @PostMapping("/admin/mora/gestionar/{cuotaId}")
    @Transactional
    public String registrarGestionCobranza(
            @PathVariable Long cuotaId,
            @RequestParam String canal,
            @RequestParam String resultado,
            @RequestParam(required = false) String observacion,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fechaCompromisoPago,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario adminSesion =
                (Usuario) session.getAttribute("adminLogueado");

        if (adminSesion == null) {
            return "redirect:/admin/login";
        }

        if (!tieneRol(session, "ADMIN", "RECUPERACIONES")) {
            return bloquearPorRol(
                    redirectAttributes,
                    "/admin/mora"
            );
        }

        CronogramaPago cuota =
                cronogramaPagoRepository.findById(cuotaId)
                        .orElse(null);

        if (cuota == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuota seleccionada no existe."
            );

            return "redirect:/admin/mora";
        }

        if (Boolean.TRUE.equals(cuota.getPagada())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuota ya fue pagada."
            );

            return "redirect:/admin/mora";
        }

        String canalLimpio =
                canal != null ? canal.trim().toUpperCase() : "";

        String resultadoLimpio =
                resultado != null ? resultado.trim().toUpperCase() : "";

        String observacionLimpia =
                observacion != null ? observacion.trim() : "";

        List<String> canalesPermitidos = List.of(
                "LLAMADA",
                "WHATSAPP",
                "CORREO",
                "SMS",
                "VISITA"
        );

        List<String> resultadosPermitidos = List.of(
                "CONTACTADO",
                "SIN_RESPUESTA",
                "COMPROMISO_PAGO",
                "RECHAZA_PAGO",
                "NUMERO_INCORRECTO"
        );

        if (!canalesPermitidos.contains(canalLimpio)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Selecciona un canal de contacto válido."
            );

            return "redirect:/admin/mora/gestionar/" + cuotaId;
        }

        if (!resultadosPermitidos.contains(resultadoLimpio)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Selecciona un resultado válido."
            );

            return "redirect:/admin/mora/gestionar/" + cuotaId;
        }

        if (observacionLimpia.length() > 500) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La observación no puede superar los 500 caracteres."
            );

            return "redirect:/admin/mora/gestionar/" + cuotaId;
        }

        if ("COMPROMISO_PAGO".equals(resultadoLimpio)) {

            if (fechaCompromisoPago == null) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Debes registrar la fecha del compromiso de pago."
                );

                return "redirect:/admin/mora/gestionar/" + cuotaId;
            }

            if (fechaCompromisoPago.isBefore(LocalDate.now())) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "La fecha de compromiso no puede estar en el pasado."
                );

                return "redirect:/admin/mora/gestionar/" + cuotaId;
            }

        } else {
            fechaCompromisoPago = null;
        }

        GestionCobranza gestion = new GestionCobranza();

        gestion.setCronogramaPago(cuota);
        gestion.setGestor(adminSesion);
        gestion.setCanal(canalLimpio);
        gestion.setResultado(resultadoLimpio);
        gestion.setObservacion(
                observacionLimpia.isEmpty()
                        ? null
                        : observacionLimpia
        );
        gestion.setFechaCompromisoPago(fechaCompromisoPago);
        gestion.setFechaGestion(LocalDateTime.now());

        gestionCobranzaRepository.save(gestion);

        Auditoria auditoria = new Auditoria();

        auditoria.setUsuario(adminSesion.getNumDoc());
        auditoria.setAccion("GESTION_COBRANZA");
        auditoria.setDetalle(
                "Gestión de cobranza sobre la cuota N° "
                        + cuota.getNumeroCuota()
                        + " del crédito #"
                        + cuota.getSolicitudCredito().getId()
                        + ". Canal: "
                        + canalLimpio
                        + ". Resultado: "
                        + resultadoLimpio
        );
        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        redirectAttributes.addFlashAttribute(
                "exito",
                "La gestión de cobranza fue registrada correctamente."
        );

        return "redirect:/admin/mora/gestionar/" + cuotaId;
    }

}

