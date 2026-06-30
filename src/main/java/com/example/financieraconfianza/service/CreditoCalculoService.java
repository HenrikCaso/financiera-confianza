package com.example.financieraconfianza.service;

import com.example.financieraconfianza.dto.ResultadoCalculoCredito;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

@Service
public class CreditoCalculoService {

    /*
     * Tasas utilizadas para los casos de evaluación.
     * Se almacenan como decimal:
     * 40.92 % = 0.4092
     * 43.92 % = 0.4392
     */
    public static final double TEA_CON_SEGURO = 0.4092;
    public static final double TEA_SIN_SEGURO = 0.4392;

    private static final Set<Integer> PLAZOS_VALIDOS =
            Set.of(6, 12, 18, 24, 36);

    private static final Set<Integer> DIAS_PAGO_VALIDOS =
            Set.of(3, 5, 10, 15);

    public ResultadoCalculoCredito calcular(
            Double monto,
            Integer cuotas,
            Integer diaPago,
            Boolean tieneSeguro,
            Double ingresosMensuales,
            Double gastosMensuales
    ) {

        validarSolicitud(
                monto,
                cuotas,
                diaPago,
                ingresosMensuales,
                gastosMensuales
        );

        boolean seguro =
                Boolean.TRUE.equals(tieneSeguro);

        double tea =
                seguro
                        ? TEA_CON_SEGURO
                        : TEA_SIN_SEGURO;

        double tem =
                calcularTem(tea);

        double cuotaMensual =
                calcularCuotaDesdeTem(
                        monto,
                        cuotas,
                        tem
                );

        double rds =
                ((gastosMensuales + cuotaMensual)
                        / ingresosMensuales) * 100.0;

        double capacidadDisponible =
                ingresosMensuales
                        - gastosMensuales
                        - cuotaMensual;

        return new ResultadoCalculoCredito(
                redondear(tea, 6),
                redondear(tem, 8),
                redondear(cuotaMensual, 2),
                redondear(rds, 2),
                redondear(capacidadDisponible, 2)
        );
    }

    public double calcularCuota(
            Double monto,
            Integer cuotas,
            Boolean tieneSeguro
    ) {

        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException(
                    "El monto debe ser mayor que cero."
            );
        }

        if (cuotas == null
                || !PLAZOS_VALIDOS.contains(cuotas)) {

            throw new IllegalArgumentException(
                    "El plazo seleccionado no está permitido."
            );
        }

        double tea =
                Boolean.TRUE.equals(tieneSeguro)
                        ? TEA_CON_SEGURO
                        : TEA_SIN_SEGURO;

        double tem =
                calcularTem(tea);

        return redondear(
                calcularCuotaDesdeTem(
                        monto,
                        cuotas,
                        tem
                ),
                2
        );
    }

    public LocalDate calcularFechaVencimiento(
            LocalDate fechaBase,
            Integer diaPago,
            int numeroCuota
    ) {

        if (fechaBase == null) {
            throw new IllegalArgumentException(
                    "La fecha base es obligatoria."
            );
        }

        if (diaPago == null
                || !DIAS_PAGO_VALIDOS.contains(diaPago)) {

            throw new IllegalArgumentException(
                    "El día de pago no está permitido."
            );
        }

        if (numeroCuota <= 0) {
            throw new IllegalArgumentException(
                    "El número de cuota debe ser mayor que cero."
            );
        }

        YearMonth mesVencimiento =
                YearMonth.from(fechaBase)
                        .plusMonths(numeroCuota);

        int diaReal =
                Math.min(
                        diaPago,
                        mesVencimiento.lengthOfMonth()
                );

        return mesVencimiento.atDay(diaReal);
    }

    private double calcularTem(double tea) {
        return Math.pow(
                1.0 + tea,
                1.0 / 12.0
        ) - 1.0;
    }

    private double calcularCuotaDesdeTem(
            double monto,
            int cuotas,
            double tem
    ) {

        return (monto * tem)
                /
                (
                        1.0
                                - Math.pow(
                                1.0 + tem,
                                -cuotas
                        )
                );
    }

    private void validarSolicitud(
            Double monto,
            Integer cuotas,
            Integer diaPago,
            Double ingresosMensuales,
            Double gastosMensuales
    ) {

        if (monto == null
                || monto < 500
                || monto > 30000) {

            throw new IllegalArgumentException(
                    "El monto debe estar entre S/ 500 y S/ 30,000."
            );
        }

        if (cuotas == null
                || !PLAZOS_VALIDOS.contains(cuotas)) {

            throw new IllegalArgumentException(
                    "El plazo debe ser de 6, 12, 18, 24 o 36 meses."
            );
        }

        if (diaPago == null
                || !DIAS_PAGO_VALIDOS.contains(diaPago)) {

            throw new IllegalArgumentException(
                    "El día de pago debe ser 3, 5, 10 o 15."
            );
        }

        if (ingresosMensuales == null
                || ingresosMensuales <= 0) {

            throw new IllegalArgumentException(
                    "Los ingresos mensuales deben ser mayores que cero."
            );
        }

        if (gastosMensuales == null
                || gastosMensuales < 0) {

            throw new IllegalArgumentException(
                    "Los gastos mensuales no pueden ser negativos."
            );
        }
    }

    private double redondear(
            double valor,
            int decimales
    ) {

        return BigDecimal.valueOf(valor)
                .setScale(
                        decimales,
                        RoundingMode.HALF_UP
                )
                .doubleValue();
    }
}