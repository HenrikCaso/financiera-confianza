    package com.example.financieraconfianza.controller;

    import com.example.financieraconfianza.config.JwtUtil;
    import com.example.financieraconfianza.model.entity.Auditoria;
    import com.example.financieraconfianza.model.entity.Usuario;
    import com.example.financieraconfianza.model.Movimiento;
    import com.example.financieraconfianza.repository.AuditoriaRepository;
    import com.example.financieraconfianza.repository.SolicitudCreditoRepository;
    import com.example.financieraconfianza.repository.UsuarioRepository;
    import com.example.financieraconfianza.service.ClaveService;
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
    import org.springframework.transaction.annotation.Transactional;

    import com.example.financieraconfianza.service.PdfReporteService;

    import java.io.IOException;
    import java.security.SecureRandom;
    import java.util.Locale;
    import java.util.Optional;
    import java.util.regex.Pattern;

    @Controller
    public class HomeController {

        private static final SecureRandom RANDOM = new SecureRandom();
        private static final Pattern CORREO_PATTERN = Pattern.compile(
                "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
                Pattern.CASE_INSENSITIVE
        );

        @Autowired
        private UsuarioRepository usuarioRepository;

        @Autowired
        private MovimientoRepository movimientoRepository;

        @Autowired
        private SolicitudCreditoRepository solicitudCreditoRepository;

        @Autowired
        private JwtUtil jwtUtil;

        @Autowired
        private PdfReporteService pdfReporteService;

        @Autowired
        private AuditoriaRepository auditoriaRepository;

        @Autowired
        private ClaveService claveService;

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
        public String home() { return "redirect:/login"; }

        @GetMapping("/academia")
        public String academia() { return "academia"; }

        @GetMapping("/productos/ahorros")
        public String productoAhorros() { return "productos-ahorros"; }

        @GetMapping("/productos/cts")
        public String productoCts() { return "productos-cts"; }

        @GetMapping("/productos/deposito-plazo-fijo")
        public String productoDepositoPlazoFijo() { return "productos-deposito"; }

        @GetMapping("/productos/credito")
        public String productoCredito() { return "productos-credito"; }

        @GetMapping("/login")
        public String mostrarLogin(HttpSession session) {

            if (session.getAttribute("usuarioCambioClaveId") != null) {
                return "redirect:/cambiar-clave-temporal";
            }

            if (session.getAttribute("usuarioLogueado") != null) {
                return "redirect:/dashboard";
            }

            return "login";
        }

        @PostMapping("/login")
        @Transactional
        public String procesarLogin(
                @RequestParam String numDoc,
                @RequestParam String clave,
                HttpSession session,
                RedirectAttributes redirectAttributes) {

            String documento = numDoc.trim();

            Optional<Usuario> usuarioOpt =
                    usuarioRepository.findByNumDoc(documento);

            // No revelar si el documento existe o no.
            if (usuarioOpt.isEmpty()) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "El documento o la clave ingresada son incorrectos."
                );

                return "redirect:/login";
            }

            Usuario usuario = usuarioOpt.get();

            // El portal normal solo permite clientes.
            if (!"CLIENTE".equals(usuario.getRol())) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "El documento o la clave ingresada son incorrectos."
                );

                return "redirect:/login";
            }

            // Impedir el acceso a usuarios bloqueados.
            if (!Boolean.TRUE.equals(usuario.getActivo())) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Tu acceso está bloqueado. Comunícate con la financiera."
                );

                return "redirect:/login";
            }

            // =====================================================
            // CLAVE INCORRECTA
            // =====================================================

            if (!claveService.coincide(clave, usuario.getClave())) {

                int intentosActuales =
                        usuario.getIntentosFallidos() != null
                                ? usuario.getIntentosFallidos()
                                : 0;

                intentosActuales++;

                usuario.setIntentosFallidos(intentosActuales);
                usuario.setSesionActiva(false);

                // Bloqueo al llegar a tres intentos.
                if (intentosActuales >= 3) {

                    usuario.setActivo(false);

                    usuarioRepository.save(usuario);

                    Auditoria auditoria = new Auditoria();
                    auditoria.setUsuario(usuario.getNumDoc());
                    auditoria.setAccion("BLOQUEO_AUTOMATICO_LOGIN");
                    auditoria.setDetalle(
                            "La cuenta del cliente con DNI "
                                    + usuario.getNumDoc()
                                    + " fue bloqueada por 3 intentos fallidos"
                    );
                    auditoria.setFecha(LocalDateTime.now());

                    auditoriaRepository.save(auditoria);

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Tu cuenta fue bloqueada por superar los 3 intentos permitidos."
                    );

                    return "redirect:/login";
                }

                usuarioRepository.save(usuario);

                Auditoria auditoria = new Auditoria();
                auditoria.setUsuario(usuario.getNumDoc());
                auditoria.setAccion("LOGIN_FALLIDO");
                auditoria.setDetalle(
                        "Intento fallido de inicio de sesión. Intento número "
                                + intentosActuales
                );
                auditoria.setFecha(LocalDateTime.now());

                auditoriaRepository.save(auditoria);

                int intentosRestantes = 3 - intentosActuales;

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Clave incorrecta. Te quedan "
                                + intentosRestantes
                                + " intento(s)."
                );

                return "redirect:/login";
            }

            // =====================================================
            // CLAVE CORRECTA
            // =====================================================

            if (!claveService.estaCifrada(usuario.getClave())) {
                usuario.setClave(
                        claveService.cifrar(clave)
                );
            }

            usuario.setIntentosFallidos(0);

            /*
             * Si tiene una clave temporal, todavía no se crea
             * la sesión bancaria completa.
             */
            if (Boolean.TRUE.equals(
                    usuario.getCambioClaveObligatorio()
            )) {

                usuario.setSesionActiva(false);
                usuarioRepository.save(usuario);

                session.removeAttribute("usuarioLogueado");
                session.removeAttribute("tokenJwt");

                session.setAttribute(
                        "usuarioCambioClaveId",
                        usuario.getId()
                );

                return "redirect:/cambiar-clave-temporal";
            }

            usuario.setSesionActiva(true);
            usuarioRepository.save(usuario);

            String token = jwtUtil.generarToken(
                    usuario.getNumDoc(),
                    "CLIENTE"
            );

            session.removeAttribute("usuarioCambioClaveId");
            session.setAttribute("tokenJwt", token);
            session.setAttribute("usuarioLogueado", usuario);

            Auditoria auditoria = new Auditoria();
            auditoria.setUsuario(usuario.getNumDoc());
            auditoria.setAccion("LOGIN_EXITOSO");
            auditoria.setDetalle(
                    "El cliente con DNI "
                            + usuario.getNumDoc()
                            + " inició sesión correctamente"
            );
            auditoria.setFecha(LocalDateTime.now());

            auditoriaRepository.save(auditoria);

            return "redirect:/dashboard";
        }

        @GetMapping("/cambiar-clave-temporal")
        public String mostrarCambioClaveTemporal(
                HttpSession session,
                Model model,
                RedirectAttributes redirectAttributes) {

            Long usuarioId =
                    (Long) session.getAttribute("usuarioCambioClaveId");

            if (usuarioId == null) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Debes iniciar sesión con tu clave temporal."
                );

                return "redirect:/login";
            }

            Usuario usuario =
                    usuarioRepository.findById(usuarioId).orElse(null);

            if (usuario == null) {
                session.removeAttribute("usuarioCambioClaveId");

                redirectAttributes.addFlashAttribute(
                        "error",
                        "No se encontró la cuenta del cliente."
                );

                return "redirect:/login";
            }

            if (!Boolean.TRUE.equals(usuario.getCambioClaveObligatorio())) {
                session.removeAttribute("usuarioCambioClaveId");

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Esta cuenta no tiene un cambio de clave pendiente."
                );

                return "redirect:/login";
            }

            model.addAttribute("usuario", usuario);

            return "cambiar-clave-temporal";
        }

        @PostMapping("/cambiar-clave-temporal")
        @Transactional
        public String procesarCambioClaveTemporal(
                @RequestParam String nuevaClave,
                @RequestParam String confirmarClave,
                HttpSession session,
                RedirectAttributes redirectAttributes) {

            Long usuarioId =
                    (Long) session.getAttribute("usuarioCambioClaveId");

            if (usuarioId == null) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Tu proceso de cambio de clave ha expirado."
                );

                return "redirect:/login";
            }

            Usuario usuario =
                    usuarioRepository.findById(usuarioId).orElse(null);

            if (usuario == null) {
                session.removeAttribute("usuarioCambioClaveId");

                redirectAttributes.addFlashAttribute(
                        "error",
                        "No se encontró la cuenta del cliente."
                );

                return "redirect:/login";
            }

            nuevaClave = nuevaClave.trim();
            confirmarClave = confirmarClave.trim();

            if (!nuevaClave.matches("\\d{6}")) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "La nueva clave debe contener exactamente 6 dígitos."
                );

                return "redirect:/cambiar-clave-temporal";
            }

            if (!nuevaClave.equals(confirmarClave)) {
                redirectAttributes.addFlashAttribute(
                        "error",
                        "Las claves ingresadas no coinciden."
                );

                return "redirect:/cambiar-clave-temporal";
            }

            if (claveService.coincide(
                    nuevaClave,
                    usuario.getClave()
            )) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "La nueva clave debe ser diferente de la clave temporal."
                );

                return "redirect:/cambiar-clave-temporal";
            }

            usuario.setClave(
                    claveService.cifrar(nuevaClave)
            );

            usuario.setCambioClaveObligatorio(false);
            usuario.setIntentosFallidos(0);
            usuario.setSesionActiva(true);

            usuarioRepository.save(usuario);

            Auditoria auditoria = new Auditoria();
            auditoria.setUsuario(usuario.getNumDoc());
            auditoria.setAccion("CAMBIO_CLAVE_CLIENTE");
            auditoria.setDetalle(
                    "El cliente con DNI "
                            + usuario.getNumDoc()
                            + " cambió su clave temporal obligatoria"
            );
            auditoria.setFecha(LocalDateTime.now());

            auditoriaRepository.save(auditoria);

            String token = jwtUtil.generarToken(
                    usuario.getNumDoc(),
                    "CLIENTE"
            );

            session.removeAttribute("usuarioCambioClaveId");
            session.setAttribute("tokenJwt", token);
            session.setAttribute("usuarioLogueado", usuario);

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Tu clave fue actualizada correctamente."
            );

            return "redirect:/dashboard";
        }

        @GetMapping("/registro")
        public String mostrarRegistro() { return "registro"; }

        @PostMapping("/registro")
        public String procesarRegistro(
                @RequestParam(required = false) String tipoDoc,
                @RequestParam(required = false) String numDoc,
                @RequestParam(required = false) String nombres,
                @RequestParam(required = false) String apellidos,
                @RequestParam(required = false) String correo,
                @RequestParam(required = false) String celular,
                @RequestParam(required = false) String direccion,
                @RequestParam(required = false) String clave,
                @RequestParam(required = false) String confirmarClave,
                RedirectAttributes redirectAttributes) {

            tipoDoc = normalizar(tipoDoc).toUpperCase(Locale.ROOT);
            numDoc = normalizarDocumento(numDoc, tipoDoc);
            nombres = normalizar(nombres);
            apellidos = normalizar(apellidos);
            correo = normalizarCorreo(correo);
            celular = soloDigitos(celular);
            direccion = normalizar(direccion);
            clave = clave == null ? "" : clave.trim();
            confirmarClave = confirmarClave == null ? "" : confirmarClave.trim();

            if (tipoDoc.isEmpty()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Selecciona el tipo de documento.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (!"DNI".equals(tipoDoc) && !"CE".equals(tipoDoc)) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Selecciona un tipo de documento valido.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if ("DNI".equals(tipoDoc) && !numDoc.matches("\\d{8}")) {
                return volverRegistroConError(
                        redirectAttributes,
                        "El DNI debe tener 8 digitos.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if ("CE".equals(tipoDoc) && !numDoc.matches("[A-Z0-9]{9,12}")) {
                return volverRegistroConError(
                        redirectAttributes,
                        "El carne de extranjeria debe tener de 9 a 12 caracteres alfanumericos.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (nombres.isEmpty()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Ingresa tus nombres.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (apellidos.isEmpty()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Ingresa tus apellidos.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (correo.isEmpty() || !CORREO_PATTERN.matcher(correo).matches()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Ingresa un correo valido.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (!celular.matches("\\d{9}")) {
                return volverRegistroConError(
                        redirectAttributes,
                        "El celular debe tener 9 digitos.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (direccion.isEmpty()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Ingresa tu direccion.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (clave.length() < 6) {
                return volverRegistroConError(
                        redirectAttributes,
                        "La clave debe tener minimo 6 caracteres.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (!clave.equals(confirmarClave)) {
                return volverRegistroConError(
                        redirectAttributes,
                        "Las claves ingresadas no coinciden.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (usuarioRepository.findByNumDoc(numDoc).isPresent()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "El numero de documento ya se encuentra registrado.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            if (usuarioRepository.findByCorreoIgnoreCase(correo).isPresent()) {
                return volverRegistroConError(
                        redirectAttributes,
                        "El correo ya se encuentra registrado.",
                        tipoDoc,
                        numDoc,
                        nombres,
                        apellidos,
                        correo,
                        celular,
                        direccion
                );
            }

            String numeroCuenta = generarNumeroCuentaUnico();

            Usuario nuevoUsuario = new Usuario();
            nuevoUsuario.setTipoDoc(tipoDoc);
            nuevoUsuario.setNumDoc(numDoc);
            nuevoUsuario.setNombres(nombres);
            nuevoUsuario.setApellidos(apellidos);
            nuevoUsuario.setCorreo(correo);
            nuevoUsuario.setCelular(celular);
            nuevoUsuario.setDireccion(direccion);
            nuevoUsuario.setClave(
                    claveService.cifrar(clave)
            );
            nuevoUsuario.setRol("CLIENTE");
            nuevoUsuario.setActivo(true);
            nuevoUsuario.setIntentosFallidos(0);
            nuevoUsuario.setSesionActiva(false);
            nuevoUsuario.setBloqueoDefinitivo(false);
            nuevoUsuario.setCambioClaveObligatorio(false);
            nuevoUsuario.setSaldo(0.00);
            nuevoUsuario.setNumeroCuenta(numeroCuenta);
            nuevoUsuario.setCci(generarCciUnico(numeroCuenta));
            nuevoUsuario.setTarjetaBloqueada(false);

            usuarioRepository.save(nuevoUsuario);

            redirectAttributes.addFlashAttribute("exito", "Cuenta creada con exito. Ya puedes iniciar sesion con tu documento y clave web.");
            return "redirect:/login";
        }

        private String volverRegistroConError(
                RedirectAttributes redirectAttributes,
                String mensaje,
                String tipoDoc,
                String numDoc,
                String nombres,
                String apellidos,
                String correo,
                String celular,
                String direccion) {

            redirectAttributes.addFlashAttribute("error", mensaje);
            redirectAttributes.addFlashAttribute("tipoDoc", tipoDoc);
            redirectAttributes.addFlashAttribute("numDoc", numDoc);
            redirectAttributes.addFlashAttribute("nombres", nombres);
            redirectAttributes.addFlashAttribute("apellidos", apellidos);
            redirectAttributes.addFlashAttribute("correo", correo);
            redirectAttributes.addFlashAttribute("celular", celular);
            redirectAttributes.addFlashAttribute("direccion", direccion);

            return "redirect:/registro";
        }

        private String normalizar(String valor) {
            return valor == null ? "" : valor.trim().replaceAll("\\s+", " ");
        }

        private String normalizarCorreo(String correo) {
            return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        }

        private String normalizarDocumento(String documento, String tipoDoc) {
            String documentoNormalizado = normalizar(documento).replace(" ", "");

            if ("DNI".equals(tipoDoc)) {
                return soloDigitos(documentoNormalizado);
            }

            return documentoNormalizado.toUpperCase(Locale.ROOT);
        }

        private String soloDigitos(String valor) {
            return valor == null ? "" : valor.replaceAll("\\D", "");
        }

        private String generarNumeroCuentaUnico() {
            String numeroCuenta;

            do {
                numeroCuenta = generarDigitos("193", 11);
            } while (usuarioRepository.findByNumeroCuenta(numeroCuenta).isPresent());

            return numeroCuenta;
        }

        private String generarCciUnico(String numeroCuenta) {
            String cci;

            do {
                cci = "002" + numeroCuenta + String.format("%03d", RANDOM.nextInt(1000));
            } while (usuarioRepository.findByCci(cci).isPresent());

            return cci;
        }

        private String generarDigitos(String prefijo, int cantidad) {
            StringBuilder valor = new StringBuilder(prefijo);

            for (int i = 0; i < cantidad; i++) {
                valor.append(RANDOM.nextInt(10));
            }

            return valor.toString();
        }

        private String obtenerNombreCliente(Usuario usuario) {
            String nombres = normalizar(usuario.getNombres());

            if (nombres.isEmpty()) {
                return usuario.getCorreo();
            }

            String apellidos = normalizar(usuario.getApellidos());
            return normalizar(nombres + " " + apellidos);
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

            if (!Boolean.TRUE.equals(usuarioBD.getActivo())) {

                usuarioBD.setSesionActiva(false);
                usuarioRepository.save(usuarioBD);

                session.invalidate();

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Tu acceso fue bloqueado. Comunícate con la financiera."
                );

                return "redirect:/login";
            }

            if (Boolean.TRUE.equals(
                    usuarioBD.getCambioClaveObligatorio()
            )) {

                session.removeAttribute("usuarioLogueado");
                session.removeAttribute("tokenJwt");

                session.setAttribute(
                        "usuarioCambioClaveId",
                        usuarioBD.getId()
                );

                return "redirect:/cambiar-clave-temporal";
            }
            // GENERADOR DE CUENTA Y CCI (Se ejecuta si el usuario es nuevo o no los tiene)
            boolean datosActualizados = false;

            if (usuarioBD.getNumeroCuenta() == null || usuarioBD.getNumeroCuenta().isBlank()) {
                usuarioBD.setNumeroCuenta(generarNumeroCuentaUnico());
                datosActualizados = true;
            }

            if (usuarioBD.getCci() == null || usuarioBD.getCci().isBlank()) {
                usuarioBD.setCci(generarCciUnico(usuarioBD.getNumeroCuenta()));
                datosActualizados = true;
            }

            if (datosActualizados) {
                // saveAndFlush obliga a la base de datos a guardar los datos en ese exacto milisegundo
                usuarioRepository.saveAndFlush(usuarioBD);
                session.setAttribute("usuarioLogueado", usuarioBD);
            }
            model.addAttribute("usuario", usuarioBD);
            model.addAttribute("nombreCliente", obtenerNombreCliente(usuarioBD));

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
        @Transactional
        public String cerrarSesion(HttpSession session) {

            Usuario usuarioSesion =
                    (Usuario) session.getAttribute("usuarioLogueado");

            if (usuarioSesion != null) {

                Usuario usuarioBD =
                        usuarioRepository.findById(
                                usuarioSesion.getId()
                        ).orElse(null);

                if (usuarioBD != null) {

                    usuarioBD.setSesionActiva(false);
                    usuarioRepository.save(usuarioBD);

                    Auditoria auditoria = new Auditoria();
                    auditoria.setUsuario(usuarioBD.getNumDoc());
                    auditoria.setAccion("CIERRE_SESION");
                    auditoria.setDetalle(
                            "El cliente con DNI "
                                    + usuarioBD.getNumDoc()
                                    + " cerró su sesión"
                    );
                    auditoria.setFecha(LocalDateTime.now());

                    auditoriaRepository.save(auditoria);
                }
            }

            session.invalidate();

            return "redirect:/login";
        }
    }
