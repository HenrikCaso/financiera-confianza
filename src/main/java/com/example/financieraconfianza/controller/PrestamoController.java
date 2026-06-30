package com.example.financieraconfianza.controller;

import com.example.financieraconfianza.dto.CreditoClienteResumen;
import com.example.financieraconfianza.dto.ResultadoCalculoCredito;
import com.example.financieraconfianza.model.Movimiento;
import com.example.financieraconfianza.model.entity.*;
import com.example.financieraconfianza.repository.*;
import com.example.financieraconfianza.service.CreditoCalculoService;
import com.example.financieraconfianza.service.MoraService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PrestamoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SolicitudCreditoRepository solicitudCreditoRepository;

    @Autowired
    private HistorialPrestamoRepository historialPrestamoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private CronogramaPagoRepository cronogramaPagoRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MoraRepository moraRepository;

    @Autowired
    private MoraService moraService;

    @Autowired
    private CreditoCalculoService creditoCalculoService;

    @GetMapping("/prestamos")
    public String mostrarPrestamos(
            HttpSession session,
            Model model
    ) {

        Usuario usuarioSesion =
                (Usuario) session.getAttribute("usuarioLogueado");

        if (usuarioSesion == null) {
            return "redirect:/login";
        }

        Usuario usuario =
                usuarioRepository.findById(usuarioSesion.getId())
                        .orElse(null);

        if (usuario == null) {
            session.invalidate();
            return "redirect:/login";
        }

        /*
         * Actualiza los días de atraso y los intereses
         * antes de mostrar las cuotas.
         */
        moraService.sincronizarMoras();

        List<SolicitudCredito> solicitudes =
                solicitudCreditoRepository
                        .findByUsuarioOrderByFechaSolicitudDesc(
                                usuario
                        );

        List<SolicitudCredito> creditosActivos =
                obtenerCreditosActivos(
                        usuario
                );

        boolean tieneSolicitudPendiente =
                solicitudCreditoRepository
                        .existsByUsuarioAndEstado(
                                usuario,
                                "PENDIENTE"
                        );

        boolean puedeSolicitarNuevoCredito =
                creditosActivos.isEmpty()
                        && !tieneSolicitudPendiente;

        String motivoBloqueoSolicitud = "";

        if (tieneSolicitudPendiente) {

            motivoBloqueoSolicitud =
                    "Ya tienes una solicitud de crédito en evaluación.";

        } else if (!creditosActivos.isEmpty()) {

            long cuotasActivas =
                    creditosActivos.stream()
                            .mapToLong(credito ->
                                    cronogramaPagoRepository
                                            .countBySolicitudCreditoAndPagadaFalse(
                                                    credito
                                            )
                            )
                            .sum();

            motivoBloqueoSolicitud =
                    "Mantienes "
                            + creditosActivos.size()
                            + " crédito(s) activo(s) con "
                            + cuotasActivas
                            + " cuota(s) pendientes.";
        }

        // =====================================================
        // OBTENER TODAS LAS CUOTAS DEL CLIENTE
        // =====================================================

        List<CronogramaPago> todasLasCuotas =
                new ArrayList<>();

        for (SolicitudCredito solicitud : solicitudes) {

            todasLasCuotas.addAll(
                    cronogramaPagoRepository
                            .findBySolicitudCreditoOrderByNumeroCuotaAsc(
                                    solicitud
                            )
            );
        }

        // =====================================================
        // FILTRAR Y ORDENAR CUOTAS PENDIENTES
        // =====================================================

        List<CronogramaPago> cuotasPendientes =
                todasLasCuotas.stream()
                        .filter(cuota ->
                                !Boolean.TRUE.equals(
                                        cuota.getPagada()
                                )
                        )
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
                                    .compareTo(
                                            cuota2.getFechaVencimiento()
                                    );
                        })
                        .toList();

        // =====================================================
        // AGRUPAR CRÉDITOS ACTIVOS Y FINALIZADOS
        // =====================================================

        List<CreditoClienteResumen> creditosResumen =
                new ArrayList<>();

        List<CreditoClienteResumen> creditosFinalizadosResumen =
                new ArrayList<>();

        /*
         * Guarda la fecha en que cada crédito fue cancelado.
         * La clave del mapa es el ID del crédito.
         */
        Map<Long, LocalDateTime> fechasFinalizacionPorCredito =
                new HashMap<>();

        for (SolicitudCredito solicitud : solicitudes) {

            List<CronogramaPago> cuotasCredito =
                    cronogramaPagoRepository
                            .findBySolicitudCreditoOrderByNumeroCuotaAsc(
                                    solicitud
                            );

            /*
             * Las solicitudes rechazadas o todavía no desembolsadas
             * pueden no tener cronograma.
             */
            if (cuotasCredito.isEmpty()) {
                continue;
            }

            List<CronogramaPago> pendientesCredito =
                    cuotasCredito.stream()
                            .filter(cuota ->
                                    !Boolean.TRUE.equals(
                                            cuota.getPagada()
                                    )
                            )
                            .sorted((cuota1, cuota2) ->
                                    Integer.compare(
                                            cuota1.getNumeroCuota(),
                                            cuota2.getNumeroCuota()
                                    )
                            )
                            .toList();

            int totalCuotas =
                    cuotasCredito.size();

            int cuotasPagadas =
                    (int) cuotasCredito.stream()
                            .filter(cuota ->
                                    Boolean.TRUE.equals(
                                            cuota.getPagada()
                                    )
                            )
                            .count();

            int porcentajePagado =
                    totalCuotas > 0
                            ? (int) Math.round(
                            cuotasPagadas * 100.0
                            / totalCuotas
                    )
                            : 0;

            // =================================================
            // CRÉDITO FINALIZADO
            // =================================================

            if (pendientesCredito.isEmpty()) {

                LocalDateTime fechaFinalizacion =
                        null;

                /*
                 * Buscamos el evento CANCELADO más reciente.
                 * El repositorio devuelve el historial de forma
                 * ascendente, por eso el último CANCELADO encontrado
                 * será el cierre definitivo del crédito.
                 */
                List<HistorialPrestamo> historialCredito =
                        historialPrestamoRepository
                                .findBySolicitudCreditoOrderByFechaAsc(
                                        solicitud
                                );

                for (HistorialPrestamo evento : historialCredito) {

                    if ("CANCELADO".equalsIgnoreCase(
                            evento.getEstado()
                    )) {

                        fechaFinalizacion =
                                evento.getFecha();
                    }
                }

                fechasFinalizacionPorCredito.put(
                        solicitud.getId(),
                        fechaFinalizacion
                );

                creditosFinalizadosResumen.add(
                        new CreditoClienteResumen(
                                solicitud,
                                cuotasCredito,
                                List.of(),
                                totalCuotas,
                                cuotasPagadas,
                                100,
                                0.0,
                                null
                        )
                );

                continue;
            }

            // =================================================
            // CRÉDITO ACTIVO
            // =================================================

            double totalBasePendiente =
                    BigDecimal.valueOf(
                                    pendientesCredito.stream()
                                            .mapToDouble(cuota ->
                                                    cuota.getMontoCuota() != null
                                                            ? cuota.getMontoCuota()
                                                            : 0.0
                                            )
                                            .sum()
                            )
                            .setScale(
                                    2,
                                    RoundingMode.HALF_UP
                            )
                            .doubleValue();

            CronogramaPago proximaCuota =
                    pendientesCredito.get(0);

            creditosResumen.add(
                    new CreditoClienteResumen(
                            solicitud,
                            cuotasCredito,
                            pendientesCredito,
                            totalCuotas,
                            cuotasPagadas,
                            porcentajePagado,
                            totalBasePendiente,
                            proximaCuota
                    )
            );
        }

        // =====================================================
        // OBTENER REGISTROS DE MORA
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

        Map<Long, Mora> morasPorCuota =
                new HashMap<>();

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
        // PRIMERA CUOTA PENDIENTE DE CADA CRÉDITO
        // =====================================================

        Map<Long, CronogramaPago> primeraPendientePorCredito =
                new HashMap<>();

        for (CronogramaPago cuota : cuotasPendientes) {

            if (cuota.getSolicitudCredito() == null
                    || cuota.getSolicitudCredito().getId() == null
                    || cuota.getId() == null
                    || cuota.getNumeroCuota() == null) {

                continue;
            }

            Long creditoId =
                    cuota.getSolicitudCredito().getId();

            CronogramaPago primeraRegistrada =
                    primeraPendientePorCredito.get(
                            creditoId
                    );

            if (primeraRegistrada == null
                    || cuota.getNumeroCuota()
                    < primeraRegistrada.getNumeroCuota()) {

                primeraPendientePorCredito.put(
                        creditoId,
                        cuota
                );
            }
        }

        // IDs de las cuotas que sí están habilitadas
        Set<Long> cuotasPagablesIds =
                new HashSet<>();

        // Número de la primera cuota pendiente de cada crédito
        Map<Long, Integer> primeraCuotaNumeroPorCredito =
                new HashMap<>();

        for (Map.Entry<Long, CronogramaPago> entrada
                : primeraPendientePorCredito.entrySet()) {

            Long creditoId =
                    entrada.getKey();

            CronogramaPago primeraCuota =
                    entrada.getValue();

            cuotasPagablesIds.add(
                    primeraCuota.getId()
            );

            primeraCuotaNumeroPorCredito.put(
                    creditoId,
                    primeraCuota.getNumeroCuota()
            );
        }

        // =====================================================
        // DATOS PARA THYMELEAF
        // =====================================================

        model.addAttribute(
                "usuario",
                usuario
        );

        model.addAttribute(
                "solicitudes",
                solicitudes
        );

        model.addAttribute(
                "cuotasPendientes",
                cuotasPendientes
        );

        model.addAttribute(
                "morasPorCuota",
                morasPorCuota
        );

        model.addAttribute(
                "cuotasPagablesIds",
                cuotasPagablesIds
        );

        model.addAttribute(
                "primeraCuotaNumeroPorCredito",
                primeraCuotaNumeroPorCredito
        );

        model.addAttribute(
                "creditosActivos",
                creditosActivos
        );

        model.addAttribute(
                "cantidadCreditosActivos",
                creditosActivos.size()
        );

        model.addAttribute(
                "puedeSolicitarNuevoCredito",
                puedeSolicitarNuevoCredito
        );

        model.addAttribute(
                "motivoBloqueoSolicitud",
                motivoBloqueoSolicitud
        );

        model.addAttribute(
                "tieneSolicitudPendiente",
                tieneSolicitudPendiente
        );

        model.addAttribute(
                "creditosResumen",
                creditosResumen
        );

        model.addAttribute(
                "creditosFinalizadosResumen",
                creditosFinalizadosResumen
        );

        model.addAttribute(
                "cantidadCreditosFinalizados",
                creditosFinalizadosResumen.size()
        );

        model.addAttribute(
                "fechasFinalizacionPorCredito",
                fechasFinalizacionPorCredito
        );

        boolean puedeSolicitarNuevoPrestamo =
                creditosActivos == null || creditosActivos.isEmpty();

        String mensajeBloqueoPrestamo = "";

        if (!puedeSolicitarNuevoPrestamo) {
            int totalCuotasPendientes = cuotasPendientes != null
                    ? cuotasPendientes.size()
                    : 0;

            mensajeBloqueoPrestamo =
                    "No puedes solicitar otro préstamo porque mantienes "
                            + creditosActivos.size()
                            + " crédito(s) activo(s) con "
                            + totalCuotasPendientes
                            + " cuota(s) pendientes.";
        }

        model.addAttribute(
                "puedeSolicitarNuevoPrestamo",
                puedeSolicitarNuevoPrestamo
        );

        model.addAttribute(
                "mensajeBloqueoPrestamo",
                mensajeBloqueoPrestamo
        );

        return "prestamos";
    }

    @PostMapping("/prestamos/solicitar")
    @Transactional
    public String solicitarPrestamo(
            @RequestParam Double monto,
            @RequestParam Integer cuotas,
            @RequestParam Integer diaPago,
            @RequestParam Boolean tieneSeguro,
            @RequestParam Double ingresosMensuales,
            @RequestParam Double gastosMensuales,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {

        Usuario usuarioSesion =
                (Usuario) session.getAttribute(
                        "usuarioLogueado"
                );

        if (usuarioSesion == null) {
            return "redirect:/login";
        }

        Usuario usuarioBD =
                usuarioRepository.findById(
                        usuarioSesion.getId()
                ).orElse(null);

        if (usuarioBD == null) {

            session.invalidate();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se encontró la cuenta del cliente."
            );

            return "redirect:/login";
        }

        // =====================================================
// IMPEDIR SOLICITUDES SIMULTÁNEAS
// =====================================================

        boolean tieneSolicitudPendiente =
                solicitudCreditoRepository
                        .existsByUsuarioAndEstado(
                                usuarioBD,
                                "PENDIENTE"
                        );

        if (tieneSolicitudPendiente) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No puedes solicitar otro préstamo porque "
                            + "ya tienes una solicitud en evaluación."
            );

            return "redirect:/prestamos";
        }

        List<SolicitudCredito> creditosActivos =
                obtenerCreditosActivos(
                        usuarioBD
                );

        if (!creditosActivos.isEmpty()) {

            long totalCuotasPendientes =
                    creditosActivos.stream()
                            .mapToLong(credito ->
                                    cronogramaPagoRepository
                                            .countBySolicitudCreditoAndPagadaFalse(
                                                    credito
                                            )
                            )
                            .sum();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No puedes solicitar otro préstamo porque "
                            + "mantienes "
                            + creditosActivos.size()
                            + (
                            creditosActivos.size() == 1
                                    ? " crédito activo con "
                                    : " créditos activos con "
                    )
                            + totalCuotasPendientes
                            + " cuota(s) pendientes."
            );

            return "redirect:/prestamos";
        }

        ResultadoCalculoCredito resultado;

        try {

            resultado =
                    creditoCalculoService.calcular(
                            monto,
                            cuotas,
                            diaPago,
                            tieneSeguro,
                            ingresosMensuales,
                            gastosMensuales
                    );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

            return "redirect:/prestamos";
        }

        SolicitudCredito solicitud =
                new SolicitudCredito();

        solicitud.setUsuario(usuarioBD);
        solicitud.setMonto(monto);
        solicitud.setCuotas(cuotas);
        solicitud.setDiaPago(diaPago);

        solicitud.setIngresosMensuales(
                ingresosMensuales
        );

        solicitud.setGastosMensuales(
                gastosMensuales
        );

        solicitud.setTieneSeguro(
                tieneSeguro
        );

        solicitud.setTea(
                resultado.getTea()
        );

        solicitud.setTem(
                resultado.getTem()
        );

        solicitud.setCuotaCalculada(
                resultado.getCuotaMensual()
        );

        solicitud.setRdsCalculado(
                resultado.getRds()
        );

        solicitud.setFechaSolicitud(
                LocalDateTime.now()
        );

        solicitud.setEtapa("SOLICITADO");
        solicitud.setDesembolsado(false);

        boolean rechazadoPorRiesgo =
                resultado.getRds() > 40.0
                        || resultado.getCapacidadDisponible() < 0;

        if (rechazadoPorRiesgo) {

            solicitud.setEstado(
                    "RECHAZADO_RIESGO"
            );

            solicitud.setEtapa(
                    "FINALIZADO"
            );

            solicitud.setObservacion(
                    "Solicitud rechazada automáticamente por RDS o capacidad de pago."
            );

        } else {

            solicitud.setEstado(
                    "PENDIENTE"
            );

            solicitud.setObservacion(
                    "Solicitud pendiente de evaluación."
            );
        }

        solicitudCreditoRepository.save(
                solicitud
        );

        HistorialPrestamo historial =
                new HistorialPrestamo();

        historial.setSolicitudCredito(
                solicitud
        );

        historial.setEstado(
                rechazadoPorRiesgo
                        ? "RECHAZADO_RIESGO"
                        : "SOLICITADO"
        );

        historial.setObservacion(
                rechazadoPorRiesgo
                        ? "Solicitud rechazada por riesgo financiero. RDS: "
                          + resultado.getRds() + "%"
                        : "Solicitud creada por el cliente. RDS: "
                          + resultado.getRds() + "%"
        );

        historial.setFecha(
                LocalDateTime.now()
        );

        historialPrestamoRepository.save(
                historial
        );

        Auditoria auditoriaSistema =
                new Auditoria();

        auditoriaSistema.setUsuario(
                usuarioBD.getNumDoc()
        );

        auditoriaSistema.setAccion(
                rechazadoPorRiesgo
                        ? "SOLICITUD_RECHAZADA_RIESGO"
                        : "SOLICITUD_PRESTAMO"
        );

        auditoriaSistema.setDetalle(
                "Solicitud por S/ "
                        + monto
                        + ", plazo "
                        + cuotas
                        + " meses, día "
                        + diaPago
                        + ", seguro: "
                        + (
                        Boolean.TRUE.equals(tieneSeguro)
                                ? "SÍ"
                                : "NO"
                )
                        + ", cuota S/ "
                        + resultado.getCuotaMensual()
                        + ", RDS "
                        + resultado.getRds()
                        + "%"
        );

        auditoriaSistema.setFecha(
                LocalDateTime.now()
        );

        auditoriaRepository.save(
                auditoriaSistema
        );

        if (rechazadoPorRiesgo) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Solicitud rechazada por riesgo. Tu RDS calculado es "
                            + resultado.getRds()
                            + "%."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Solicitud enviada. Cuota estimada: S/ "
                            + String.format(
                            "%.2f",
                            resultado.getCuotaMensual()
                    )
                            + " | RDS: "
                            + String.format(
                            "%.2f",
                            resultado.getRds()
                    )
                            + "%."
            );
        }

        return "redirect:/prestamos";
    }

    @PostMapping("/prestamos/pagar-cuota")
    @Transactional
    public String pagarCuota(
            @RequestParam Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuarioSesion =
                (Usuario) session.getAttribute("usuarioLogueado");

        if (usuarioSesion == null) {
            return "redirect:/login";
        }

        Usuario usuario =
                usuarioRepository.findById(usuarioSesion.getId())
                        .orElse(null);

        if (usuario == null) {
            session.invalidate();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No se encontró la cuenta del cliente."
            );

            return "redirect:/login";
        }

        CronogramaPago cuota =
                cronogramaPagoRepository.findById(id)
                        .orElse(null);

        if (cuota == null) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuota seleccionada no existe."
            );

            return "redirect:/prestamos";
        }

        /*
         * SEGURIDAD:
         * Impide que un cliente intente pagar una cuota
         * perteneciente a otro usuario cambiando el ID.
         */
        Usuario propietario =
                cuota.getSolicitudCredito().getUsuario();

        if (propietario == null
                || propietario.getId() == null
                || !propietario.getId().equals(usuario.getId())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No tienes autorización para pagar esta cuota."
            );

            return "redirect:/prestamos";
        }

        if (Boolean.TRUE.equals(cuota.getPagada())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "La cuota ya fue pagada."
            );

            return "redirect:/prestamos";
        }

        // =====================================================
        // VALIDAR ORDEN DE PAGO
        // =====================================================

        CronogramaPago primeraCuotaPendiente =
                cronogramaPagoRepository
                        .findBySolicitudCreditoOrderByNumeroCuotaAsc(
                                cuota.getSolicitudCredito()
                        )
                        .stream()
                        .filter(c ->
                                !Boolean.TRUE.equals(c.getPagada())
                        )
                        .findFirst()
                        .orElse(null);

        if (primeraCuotaPendiente == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Este crédito ya no tiene cuotas pendientes."
            );

            return "redirect:/prestamos";
        }

        if (!primeraCuotaPendiente.getId().equals(cuota.getId())) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Debes pagar primero la cuota N.° "
                            + primeraCuotaPendiente.getNumeroCuota()
                            + "."
            );

            return "redirect:/prestamos";
        }

        /*
         * Actualiza los días de atraso y el interés
         * antes de calcular cuánto debe cobrarse.
         */
        moraService.sincronizarMoras();

        Mora mora =
                moraRepository.findByCronogramaPago(cuota)
                        .orElse(null);

        double montoCuota =
                cuota.getMontoCuota() != null
                        ? cuota.getMontoCuota()
                        : 0.0;

        double interesMora = 0.0;

        if (mora != null
                && mora.getDiasAtraso() != null
                && mora.getDiasAtraso() > 0
                && !"REGULARIZADA".equals(mora.getEstado())
                && mora.getInteresMora() != null) {

            interesMora = mora.getInteresMora();
        }

        double totalPagar =
                BigDecimal.valueOf(montoCuota)
                        .add(BigDecimal.valueOf(interesMora))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        double saldoActual =
                usuario.getSaldo() != null
                        ? usuario.getSaldo()
                        : 0.0;

        if (saldoActual < totalPagar) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Saldo insuficiente. Necesitas S/ "
                            + String.format("%.2f", totalPagar)
                            + " para pagar la cuota."
            );

            return "redirect:/prestamos";
        }

        // =====================================================
        // DESCONTAR SALDO
        // =====================================================

        double nuevoSaldo =
                BigDecimal.valueOf(saldoActual)
                        .subtract(BigDecimal.valueOf(totalPagar))
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        usuario.setSaldo(nuevoSaldo);
        usuarioRepository.save(usuario);

        // =====================================================
        // MARCAR CUOTA COMO PAGADA
        // =====================================================

        cuota.setPagada(true);

        cuota.setFechaPago(
                LocalDate.now()
        );

        cuota.setEstado(
                "PAGADA"
        );

        cronogramaPagoRepository.saveAndFlush(cuota);

        // =====================================================
        // REGULARIZAR LA MORA
        // =====================================================

        if (mora != null) {
            mora.setDiasAtraso(0);
            mora.setInteresMora(0.0);
            mora.setEstado("REGULARIZADA");
            mora.setFechaActualizacion(LocalDateTime.now());

            moraRepository.save(mora);
        }

        SolicitudCredito solicitud =
                cuota.getSolicitudCredito();

        /*
         * La cuota ya fue guardada mediante saveAndFlush().
         * Por eso podemos consultar directamente si todavía
         * queda alguna cuota pendiente.
         */
        boolean todasPagadas =
                !cronogramaPagoRepository
                        .existsBySolicitudCreditoAndPagadaFalse(
                                solicitud
                        );

        // =====================================================
        // REGISTRAR MOVIMIENTO
        // =====================================================

        Movimiento movimiento =
                new Movimiento();

        movimiento.setUsuario(usuario);
        movimiento.setTipo("PAGO DE CUOTA");
        movimiento.setMonto(-totalPagar);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setDestino("Préstamo");

        String concepto =
                "Pago cuota N° " + cuota.getNumeroCuota();

        if (interesMora > 0) {
            concepto +=
                    " - Incluye interés moratorio S/ "
                            + String.format("%.2f", interesMora);
        }

        movimiento.setConcepto(concepto);

        movimientoRepository.save(movimiento);

        // =====================================================
        // HISTORIAL DEL PRÉSTAMO
        // =====================================================

        HistorialPrestamo historial =
                new HistorialPrestamo();

        historial.setSolicitudCredito(solicitud);
        historial.setEstado("CUOTA_PAGADA");

        historial.setObservacion(
                "Se pagó la cuota N° "
                        + cuota.getNumeroCuota()
                        + " por un total de S/ "
                        + String.format("%.2f", totalPagar)
                        + (
                        interesMora > 0
                                ? ", incluyendo S/ "
                                  + String.format("%.2f", interesMora)
                                  + " de interés moratorio"
                                : ""
                )
        );

        historial.setFecha(LocalDateTime.now());

        historialPrestamoRepository.save(historial);

        // =====================================================
        // AUDITORÍA
        // =====================================================

        Auditoria auditoria =
                new Auditoria();

        auditoria.setUsuario(usuario.getNumDoc());
        auditoria.setAccion("PAGO_CUOTA");

        auditoria.setDetalle(
                "Pago cuota N° "
                        + cuota.getNumeroCuota()
                        + " del crédito #"
                        + solicitud.getId()
                        + " por S/ "
                        + String.format("%.2f", totalPagar)
                        + (
                        interesMora > 0
                                ? ". Interés moratorio incluido: S/ "
                                  + String.format("%.2f", interesMora)
                                : ""
                )
        );

        auditoria.setFecha(LocalDateTime.now());

        auditoriaRepository.save(auditoria);

        // =====================================================
        // FINALIZAR EL CRÉDITO AL PAGAR LA ÚLTIMA CUOTA
        // =====================================================

        if (todasPagadas) {

            solicitud.setEstado("CANCELADO");
            solicitud.setEtapa("FINALIZADO");
            solicitud.setObservacion(
                    "Crédito cancelado completamente por el cliente."
            );

            solicitudCreditoRepository.saveAndFlush(
                    solicitud
            );

            // ---------------------------------------------
            // HISTORIAL DEL CIERRE
            // ---------------------------------------------

            HistorialPrestamo cierre =
                    new HistorialPrestamo();

            cierre.setSolicitudCredito(
                    solicitud
            );

            cierre.setEstado(
                    "CANCELADO"
            );

            cierre.setObservacion(
                    "El cliente pagó la última cuota N.° "
                            + cuota.getNumeroCuota()
                            + " y canceló completamente el crédito #"
                            + solicitud.getId()
                            + "."
            );

            cierre.setFecha(
                    LocalDateTime.now()
            );

            historialPrestamoRepository.save(
                    cierre
            );

            // ---------------------------------------------
            // AUDITORÍA DEL CIERRE
            // ---------------------------------------------

            Auditoria auditoriaCierre =
                    new Auditoria();

            auditoriaCierre.setUsuario(
                    usuario.getNumDoc()
            );

            auditoriaCierre.setAccion(
                    "CREDITO_CANCELADO"
            );

            auditoriaCierre.setDetalle(
                    "El cliente con DNI "
                            + usuario.getNumDoc()
                            + " terminó de pagar el crédito #"
                            + solicitud.getId()
                            + ". Última cuota pagada: N.° "
                            + cuota.getNumeroCuota()
                            + " por S/ "
                            + String.format(
                            "%.2f",
                            totalPagar
                    )
                            + "."
            );

            auditoriaCierre.setFecha(
                    LocalDateTime.now()
            );

            auditoriaRepository.save(
                    auditoriaCierre
            );
        }

        if (todasPagadas) {

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Última cuota pagada correctamente por S/ "
                            + String.format("%.2f", totalPagar)
                            + ". El crédito #"
                            + solicitud.getId()
                            + " fue cancelado completamente."
            );

        } else if (interesMora > 0) {

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Cuota pagada correctamente. Se descontó S/ "
                            + String.format("%.2f", totalPagar)
                            + ", incluyendo S/ "
                            + String.format("%.2f", interesMora)
                            + " de interés moratorio."
            );

        } else {

            redirectAttributes.addFlashAttribute(
                    "exito",
                    "Cuota pagada correctamente por S/ "
                            + String.format("%.2f", totalPagar)
                            + "."
            );
        }

        return "redirect:/prestamos";
    }

    @GetMapping("/prestamos/seguimiento")
    public String verSeguimiento(
            @RequestParam Long id,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {

        // =====================================================
        // VALIDAR SESIÓN
        // =====================================================

        Usuario usuarioSesion =
                (Usuario) session.getAttribute(
                        "usuarioLogueado"
                );

        if (usuarioSesion == null) {
            return "redirect:/login";
        }

        Usuario usuario =
                usuarioRepository.findById(
                        usuarioSesion.getId()
                ).orElse(null);

        if (usuario == null) {

            session.invalidate();

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Tu sesión ya no es válida. Inicia sesión nuevamente."
            );

            return "redirect:/login";
        }

        // =====================================================
        // BUSCAR SOLICITUD
        // =====================================================

        SolicitudCredito solicitud =
                solicitudCreditoRepository.findById(id)
                        .orElse(null);

        if (solicitud == null) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "El crédito solicitado no existe."
            );

            return "redirect:/prestamos";
        }

        // =====================================================
        // VALIDAR PROPIETARIO DEL CRÉDITO
        // =====================================================

        Usuario propietario =
                solicitud.getUsuario();

        if (propietario == null
                || propietario.getId() == null
                || !propietario.getId().equals(
                usuario.getId()
        )) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "No tienes autorización para consultar este crédito."
            );

            return "redirect:/prestamos";
        }

        // =====================================================
        // OBTENER HISTORIAL
        // =====================================================

        List<HistorialPrestamo> historial =
                historialPrestamoRepository
                        .findBySolicitudCreditoOrderByFechaAsc(
                                solicitud
                        );

        model.addAttribute(
                "solicitud",
                solicitud
        );

        model.addAttribute(
                "historial",
                historial
        );

        return "seguimiento-prestamo";
    }

    private List<SolicitudCredito> obtenerCreditosActivos(
            Usuario usuario
    ) {

        if (usuario == null) {
            return List.of();
        }

        List<SolicitudCredito> solicitudes =
                solicitudCreditoRepository
                        .findByUsuarioOrderByFechaSolicitudDesc(
                                usuario
                        );

        return solicitudes.stream()
                .filter(solicitud ->
                        cronogramaPagoRepository
                                .existsBySolicitudCreditoAndPagadaFalse(
                                        solicitud
                                )
                )
                .toList();
    }
}