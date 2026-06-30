package com.example.financieraconfianza.service;

import com.example.financieraconfianza.model.entity.CronogramaPago;
import com.example.financieraconfianza.model.entity.Mora;
import com.example.financieraconfianza.repository.CronogramaPagoRepository;
import com.example.financieraconfianza.repository.MoraRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MoraService {

    private final CronogramaPagoRepository cronogramaPagoRepository;
    private final MoraRepository moraRepository;

    /*
     * 0.0005 equivale a 0.05 % diario.
     * Es un valor de prueba configurable desde application.properties.
     */
    @Value("${financiera.mora.tasa-diaria:0.0005}")
    private double tasaMoraDiaria;

    public MoraService(
            CronogramaPagoRepository cronogramaPagoRepository,
            MoraRepository moraRepository
    ) {
        this.cronogramaPagoRepository = cronogramaPagoRepository;
        this.moraRepository = moraRepository;
    }

    /**
     * Revisa todas las cuotas del sistema y actualiza su situación de mora.
     */
    @Transactional
    public void sincronizarMoras() {

        List<CronogramaPago> cuotas =
                cronogramaPagoRepository.findAll();

        for (CronogramaPago cuota : cuotas) {

            /*
             * Si una cuota no tiene fecha de vencimiento,
             * no puede evaluarse todavía.
             */
            if (cuota.getFechaVencimiento() == null) {
                continue;
            }

            long diasAtraso = cuota.getDiasAtraso();

            /*
             * Cuota pagada o sin atraso.
             * Solo actualizamos el registro si anteriormente tuvo mora.
             */
            if (Boolean.TRUE.equals(cuota.getPagada())
                    || diasAtraso <= 0) {

                moraRepository
                        .findByCronogramaPago(cuota)
                        .ifPresent(mora -> {

                            mora.setDiasAtraso(0);
                            mora.setInteresMora(0.0);
                            mora.setEstado("REGULARIZADA");
                            mora.setFechaActualizacion(
                                    LocalDateTime.now()
                            );

                            moraRepository.save(mora);
                        });

                continue;
            }

            Mora mora =
                    moraRepository
                            .findByCronogramaPago(cuota)
                            .orElseGet(Mora::new);

            mora.setCronogramaPago(cuota);

            mora.setDiasAtraso(
                    diasAtraso > Integer.MAX_VALUE
                            ? Integer.MAX_VALUE
                            : (int) diasAtraso
            );

            mora.setInteresMora(
                    calcularInteresMoratorio(
                            cuota.getMontoCuota(),
                            diasAtraso
                    )
            );

            mora.setEstado(
                    cuota.getEstadoMora()
            );

            mora.setFechaActualizacion(
                    LocalDateTime.now()
            );

            moraRepository.save(mora);
        }
    }

    /**
     * Ejecución automática todos los días a las 00:05,
     * usando la hora de Perú.
     */
    @Scheduled(
            cron = "0 5 0 * * *",
            zone = "America/Lima"
    )
    @Transactional
    public void sincronizacionDiaria() {
        sincronizarMoras();
    }

    private double calcularInteresMoratorio(
            Double montoCuota,
            long diasAtraso
    ) {

        if (montoCuota == null
                || montoCuota <= 0
                || diasAtraso <= 0) {

            return 0.0;
        }

        BigDecimal monto =
                BigDecimal.valueOf(montoCuota);

        BigDecimal tasa =
                BigDecimal.valueOf(tasaMoraDiaria);

        BigDecimal dias =
                BigDecimal.valueOf(diasAtraso);

        return monto
                .multiply(tasa)
                .multiply(dias)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}