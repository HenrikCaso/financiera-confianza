package com.example.financieraconfianza.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jakarta.persistence.Transient;

@Entity
@Table(name = "solicitud_credito")
public class SolicitudCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación: Una solicitud pertenece a un usuario
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private Double monto;
    private Integer cuotas;
    private Integer diaPago;

    // Estados: PENDIENTE, APROBADA, RECHAZADA
    private String estado;

    private LocalDateTime fechaSolicitud;

    // --- NUEVOS CAMPOS PARA EVALUACIÓN DE RIESGO (CRITERIO 2) ---
    private Double ingresosMensuales;
    private Double gastosMensuales;

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Double getMonto() { return monto; }
    public void setMonto(Double monto) { this.monto = monto; }

    public Integer getCuotas() { return cuotas; }
    public void setCuotas(Integer cuotas) { this.cuotas = cuotas; }

    public Integer getDiaPago() { return diaPago; }
    public void setDiaPago(Integer diaPago) { this.diaPago = diaPago; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    // --- AGREGA ESTOS GETTERS Y SETTERS AL FINAL ---
    public Double getIngresosMensuales() { return ingresosMensuales; }
    public void setIngresosMensuales(Double ingresosMensuales) { this.ingresosMensuales = ingresosMensuales; }

    public Double getGastosMensuales() { return gastosMensuales; }
    public void setGastosMensuales(Double gastosMensuales) { this.gastosMensuales = gastosMensuales; }

    // =========================================================
    // LÓGICA DE NEGOCIO Y RIESGOS
    // =========================================================

    @Transient
    public Double getCuotaEstimada() {
        if (monto == null || cuotas == null || cuotas == 0) return 0.0;
        // Fórmula básica (Simulación con TEA aprox 15.5%)
        return (monto * 1.155) / cuotas;
    }

    @Transient
    public Double getRds() {
        if (ingresosMensuales == null || ingresosMensuales == 0.0) return 100.0; // Riesgo máximo si no hay ingresos
        double gastos = (gastosMensuales != null) ? gastosMensuales : 0.0;
        // RDS = (Gastos + Cuota del nuevo préstamo) / Ingresos
        return ((gastos + getCuotaEstimada()) / ingresosMensuales) * 100;
    }

    @Transient
    public String getSemaforoRiesgo() {
        double rds = getRds();
        if (rds <= 30.0) return "VERDE";
        if (rds <= 40.0) return "AMARILLO";
        return "ROJO";
    }
}