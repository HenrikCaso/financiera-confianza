package com.example.financieraconfianza.model.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "cronograma_pago")
public class CronogramaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private SolicitudCredito solicitudCredito;

    private Integer numeroCuota;

    /*
     * Detalle financiero de la cuota.
     */
    private Double saldoInicial;
    private Double capital;
    private Double interes;
    private Double montoCuota;
    private Double saldoFinal;

    private LocalDate fechaVencimiento;
    private LocalDate fechaPago;

    private Boolean pagada = false;

    /*
     * PENDIENTE, PAGADA o VENCIDA.
     */
    private String estado = "PENDIENTE";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SolicitudCredito getSolicitudCredito() {
        return solicitudCredito;
    }

    public void setSolicitudCredito(
            SolicitudCredito solicitudCredito
    ) {
        this.solicitudCredito = solicitudCredito;
    }

    public Integer getNumeroCuota() {
        return numeroCuota;
    }

    public void setNumeroCuota(Integer numeroCuota) {
        this.numeroCuota = numeroCuota;
    }

    public Double getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(Double saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public Double getCapital() {
        return capital;
    }

    public void setCapital(Double capital) {
        this.capital = capital;
    }

    public Double getInteres() {
        return interes;
    }

    public void setInteres(Double interes) {
        this.interes = interes;
    }

    public Double getMontoCuota() {
        return montoCuota;
    }

    public void setMontoCuota(Double montoCuota) {
        this.montoCuota = montoCuota;
    }

    public Double getSaldoFinal() {
        return saldoFinal;
    }

    public void setSaldoFinal(Double saldoFinal) {
        this.saldoFinal = saldoFinal;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(
            LocalDate fechaVencimiento
    ) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public Boolean getPagada() {
        return pagada;
    }

    public void setPagada(Boolean pagada) {
        this.pagada = pagada;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Transient
    public long getDiasAtraso() {

        if (Boolean.TRUE.equals(pagada)
                || fechaVencimiento == null) {

            return 0;
        }

        LocalDate hoy = LocalDate.now();

        if (fechaVencimiento.isBefore(hoy)) {

            return ChronoUnit.DAYS.between(
                    fechaVencimiento,
                    hoy
            );
        }

        return 0;
    }

    @Transient
    public String getEstadoMora() {

        long dias = getDiasAtraso();

        if (dias == 0) {
            return "AL DIA";
        }

        if (dias <= 15) {
            return "MORA TEMPRANA";
        }

        if (dias <= 30) {
            return "MORA INTERMEDIA";
        }

        return "MORA CRITICA";
    }
}