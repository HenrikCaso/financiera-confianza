package com.example.financieraconfianza.dto;

public class ResultadoCalculoCredito {

    private final double tea;
    private final double tem;
    private final double cuotaMensual;
    private final double rds;
    private final double capacidadDisponible;

    public ResultadoCalculoCredito(
            double tea,
            double tem,
            double cuotaMensual,
            double rds,
            double capacidadDisponible
    ) {
        this.tea = tea;
        this.tem = tem;
        this.cuotaMensual = cuotaMensual;
        this.rds = rds;
        this.capacidadDisponible = capacidadDisponible;
    }

    public double getTea() {
        return tea;
    }

    public double getTem() {
        return tem;
    }

    public double getCuotaMensual() {
        return cuotaMensual;
    }

    public double getRds() {
        return rds;
    }

    public double getCapacidadDisponible() {
        return capacidadDisponible;
    }
}