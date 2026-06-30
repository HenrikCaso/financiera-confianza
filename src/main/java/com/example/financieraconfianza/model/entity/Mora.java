package com.example.financieraconfianza.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mora")
public class Mora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cronograma_id")
    private CronogramaPago cronogramaPago;

    private Integer diasAtraso;

    private Double interesMora;

    private String estado;

    private LocalDateTime fechaActualizacion;

    // getters y setters

    public Long getId() {
        return id;
    }

    public CronogramaPago getCronogramaPago() {
        return cronogramaPago;
    }

    public void setCronogramaPago(CronogramaPago cronogramaPago) {
        this.cronogramaPago = cronogramaPago;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDiasAtraso() {
        return diasAtraso;
    }

    public void setDiasAtraso(Integer diasAtraso) {
        this.diasAtraso = diasAtraso;
    }

    public Double getInteresMora() {
        return interesMora;
    }

    public void setInteresMora(Double interesMora) {
        this.interesMora = interesMora;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}