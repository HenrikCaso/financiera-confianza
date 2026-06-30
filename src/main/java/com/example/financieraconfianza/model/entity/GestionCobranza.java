package com.example.financieraconfianza.model.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gestion_cobranza")
public class GestionCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Una cuota puede tener varias gestiones de cobranza
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cronograma_id", nullable = false)
    private CronogramaPago cronogramaPago;

    // Administrador que realizó la gestión
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Usuario gestor;

    // LLAMADA, WHATSAPP, CORREO, VISITA, SMS
    @Column(nullable = false, length = 30)
    private String canal;

    /*
     * CONTACTADO
     * SIN_RESPUESTA
     * COMPROMISO_PAGO
     * RECHAZA_PAGO
     * NUMERO_INCORRECTO
     */
    @Column(nullable = false, length = 40)
    private String resultado;

    @Column(length = 500)
    private String observacion;

    // Solo se llena cuando el cliente promete una fecha de pago
    private LocalDate fechaCompromisoPago;

    @Column(nullable = false)
    private LocalDateTime fechaGestion;

    @PrePersist
    public void asignarFechaGestion() {
        if (fechaGestion == null) {
            fechaGestion = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CronogramaPago getCronogramaPago() {
        return cronogramaPago;
    }

    public void setCronogramaPago(CronogramaPago cronogramaPago) {
        this.cronogramaPago = cronogramaPago;
    }

    public Usuario getGestor() {
        return gestor;
    }

    public void setGestor(Usuario gestor) {
        this.gestor = gestor;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDate getFechaCompromisoPago() {
        return fechaCompromisoPago;
    }

    public void setFechaCompromisoPago(LocalDate fechaCompromisoPago) {
        this.fechaCompromisoPago = fechaCompromisoPago;
    }

    public LocalDateTime getFechaGestion() {
        return fechaGestion;
    }

    public void setFechaGestion(LocalDateTime fechaGestion) {
        this.fechaGestion = fechaGestion;
    }
}