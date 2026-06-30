package com.example.financieraconfianza.service;

import com.example.financieraconfianza.model.entity.CronogramaPago;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.repository.CronogramaPagoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CronogramaService {

    @Autowired
    private CronogramaPagoRepository cronogramaPagoRepository;

    @Autowired
    private CreditoCalculoService creditoCalculoService;

    @Transactional
    public List<CronogramaPago> generarCronograma(
            SolicitudCredito solicitud,
            LocalDate fechaDesembolso
    ) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "La solicitud es obligatoria."
            );
        }

        if (solicitud.getMonto() == null
                || solicitud.getMonto() <= 0) {

            throw new IllegalArgumentException(
                    "El monto del crédito no es válido."
            );
        }

        if (solicitud.getCuotas() == null
                || solicitud.getCuotas() <= 0) {

            throw new IllegalArgumentException(
                    "El número de cuotas no es válido."
            );
        }

        /*
         * Evita generar dos cronogramas para el mismo crédito.
         */
        List<CronogramaPago> existentes =
                cronogramaPagoRepository
                        .findBySolicitudCreditoOrderByNumeroCuotaAsc(
                                solicitud
                        );

        if (!existentes.isEmpty()) {
            return existentes;
        }

        LocalDate fechaBase =
                fechaDesembolso != null
                        ? fechaDesembolso
                        : LocalDate.now();

        double tem =
                obtenerTem(solicitud);

        double cuotaBase =
                solicitud.getCuotaCalculada() != null
                        ? solicitud.getCuotaCalculada()
                        : creditoCalculoService.calcularCuota(
                        solicitud.getMonto(),
                        solicitud.getCuotas(),
                        solicitud.getTieneSeguro()
                );

        int diaPago =
                solicitud.getDiaPago() != null
                        ? solicitud.getDiaPago()
                        : 15;

        double saldoPendiente =
                redondear(solicitud.getMonto());

        List<CronogramaPago> cronograma =
                new ArrayList<>();

        for (int numeroCuota = 1;
             numeroCuota <= solicitud.getCuotas();
             numeroCuota++) {

            double saldoInicial =
                    redondear(saldoPendiente);

            double interes =
                    redondear(
                            saldoInicial * tem
                    );

            double capital;
            double montoCuota;
            double saldoFinal;

            /*
             * En la última cuota se ajustan los centavos
             * para dejar el saldo exactamente en cero.
             */
            if (numeroCuota == solicitud.getCuotas()) {

                capital = saldoInicial;

                montoCuota =
                        redondear(
                                capital + interes
                        );

                saldoFinal = 0.0;

            } else {

                montoCuota =
                        redondear(cuotaBase);

                capital =
                        redondear(
                                montoCuota - interes
                        );

                if (capital <= 0) {
                    throw new IllegalStateException(
                            "La cuota no cubre el interés generado."
                    );
                }

                if (capital > saldoInicial) {
                    capital = saldoInicial;
                }

                saldoFinal =
                        redondear(
                                saldoInicial - capital
                        );
            }

            CronogramaPago cuota =
                    new CronogramaPago();

            cuota.setSolicitudCredito(solicitud);
            cuota.setNumeroCuota(numeroCuota);

            cuota.setSaldoInicial(saldoInicial);
            cuota.setCapital(capital);
            cuota.setInteres(interes);
            cuota.setMontoCuota(montoCuota);
            cuota.setSaldoFinal(saldoFinal);

            cuota.setFechaVencimiento(
                    creditoCalculoService
                            .calcularFechaVencimiento(
                                    fechaBase,
                                    diaPago,
                                    numeroCuota
                            )
            );

            cuota.setFechaPago(null);
            cuota.setPagada(false);
            cuota.setEstado("PENDIENTE");

            cronograma.add(cuota);

            saldoPendiente = saldoFinal;
        }

        return cronogramaPagoRepository.saveAll(
                cronograma
        );
    }

    private double obtenerTem(
            SolicitudCredito solicitud
    ) {

        if (solicitud.getTem() != null
                && solicitud.getTem() > 0) {

            return solicitud.getTem();
        }

        double tea;

        if (solicitud.getTea() != null
                && solicitud.getTea() > 0) {

            tea = solicitud.getTea();

        } else {

            tea =
                    Boolean.TRUE.equals(
                            solicitud.getTieneSeguro()
                    )
                            ? CreditoCalculoService.TEA_CON_SEGURO
                            : CreditoCalculoService.TEA_SIN_SEGURO;
        }

        return Math.pow(
                1.0 + tea,
                1.0 / 12.0
        ) - 1.0;
    }

    private double redondear(double valor) {

        return BigDecimal.valueOf(valor)
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}