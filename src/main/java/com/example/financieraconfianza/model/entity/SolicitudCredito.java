package com.example.financieraconfianza.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDateTime;

@Entity
@Table(name = "solicitud_credito")
public class SolicitudCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "usuario_id",
            nullable = false
    )
    private Usuario usuario;

    private Double monto;
    private Integer cuotas;
    private Integer diaPago;

    private String estado;
    private String etapa;

    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaDesembolso;
    private LocalDateTime fechaSolicitud;

    private String observacion;

    private Boolean desembolsado = false;

    private Double ingresosMensuales;
    private Double gastosMensuales;

    // Nuevos datos financieros persistentes
    private Boolean tieneSeguro = true;
    private Double tea;
    private Double tem;
    private Double cuotaCalculada;
    private Double rdsCalculado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }

    public Integer getCuotas() {
        return cuotas;
    }

    public void setCuotas(Integer cuotas) {
        this.cuotas = cuotas;
    }

    public Integer getDiaPago() {
        return diaPago;
    }

    public void setDiaPago(Integer diaPago) {
        this.diaPago = diaPago;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(
            LocalDateTime fechaAprobacion
    ) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public LocalDateTime getFechaDesembolso() {
        return fechaDesembolso;
    }

    public void setFechaDesembolso(
            LocalDateTime fechaDesembolso
    ) {
        this.fechaDesembolso = fechaDesembolso;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(
            LocalDateTime fechaSolicitud
    ) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(
            String observacion
    ) {
        this.observacion = observacion;
    }

    public Boolean getDesembolsado() {
        return desembolsado;
    }

    public void setDesembolsado(
            Boolean desembolsado
    ) {
        this.desembolsado = desembolsado;
    }

    public Double getIngresosMensuales() {
        return ingresosMensuales;
    }

    public void setIngresosMensuales(
            Double ingresosMensuales
    ) {
        this.ingresosMensuales = ingresosMensuales;
    }

    public Double getGastosMensuales() {
        return gastosMensuales;
    }

    public void setGastosMensuales(
            Double gastosMensuales
    ) {
        this.gastosMensuales = gastosMensuales;
    }

    public Boolean getTieneSeguro() {
        return tieneSeguro;
    }

    public void setTieneSeguro(
            Boolean tieneSeguro
    ) {
        this.tieneSeguro = tieneSeguro;
    }

    public Double getTea() {
        return tea;
    }

    public void setTea(Double tea) {
        this.tea = tea;
    }

    public Double getTem() {
        return tem;
    }

    public void setTem(Double tem) {
        this.tem = tem;
    }

    public Double getCuotaCalculada() {
        return cuotaCalculada;
    }

    public void setCuotaCalculada(
            Double cuotaCalculada
    ) {
        this.cuotaCalculada = cuotaCalculada;
    }

    public Double getRdsCalculado() {
        return rdsCalculado;
    }

    public void setRdsCalculado(
            Double rdsCalculado
    ) {
        this.rdsCalculado = rdsCalculado;
    }

    @Transient
    public Double getCuotaEstimada() {

        if (cuotaCalculada != null) {
            return cuotaCalculada;
        }

        if (monto == null
                || cuotas == null
                || cuotas <= 0) {

            return 0.0;
        }

        double teaUtilizada;

        if (tea != null) {
            teaUtilizada = tea;
        } else {
            teaUtilizada =
                    Boolean.FALSE.equals(tieneSeguro)
                            ? 0.4392
                            : 0.4092;
        }

        double temUtilizada =
                Math.pow(
                        1.0 + teaUtilizada,
                        1.0 / 12.0
                ) - 1.0;

        return (monto * temUtilizada)
                /
                (
                        1.0
                                - Math.pow(
                                1.0 + temUtilizada,
                                -cuotas
                        )
                );
    }

    @Transient
    public Double getRds() {

        if (rdsCalculado != null) {
            return rdsCalculado;
        }

        if (ingresosMensuales == null
                || ingresosMensuales <= 0) {

            return 100.0;
        }

        double gastos =
                gastosMensuales != null
                        ? gastosMensuales
                        : 0.0;

        return (
                (gastos + getCuotaEstimada())
                        / ingresosMensuales
        ) * 100.0;
    }

    @Transient
    public String getSemaforoRiesgo() {

        double rds = getRds();

        if (rds <= 30.0) {
            return "VERDE";
        }

        if (rds <= 40.0) {
            return "AMARILLO";
        }

        return "ROJO";
    }
}