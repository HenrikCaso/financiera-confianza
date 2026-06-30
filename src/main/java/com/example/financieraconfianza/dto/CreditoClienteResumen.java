package com.example.financieraconfianza.dto;

import com.example.financieraconfianza.model.entity.CronogramaPago;
import com.example.financieraconfianza.model.entity.SolicitudCredito;

import java.util.List;

public class CreditoClienteResumen {

    private final SolicitudCredito credito;

    /*
     * Cronograma completo: pagadas y pendientes.
     */
    private final List<CronogramaPago> todasLasCuotas;

    /*
     * Solo cuotas que todavía faltan pagar.
     */
    private final List<CronogramaPago> cuotasPendientes;

    private final int totalCuotas;
    private final int cuotasPagadas;
    private final int porcentajePagado;
    private final double totalBasePendiente;
    private final CronogramaPago proximaCuota;

    public CreditoClienteResumen(
            SolicitudCredito credito,
            List<CronogramaPago> todasLasCuotas,
            List<CronogramaPago> cuotasPendientes,
            int totalCuotas,
            int cuotasPagadas,
            int porcentajePagado,
            double totalBasePendiente,
            CronogramaPago proximaCuota
    ) {
        this.credito = credito;
        this.todasLasCuotas = todasLasCuotas;
        this.cuotasPendientes = cuotasPendientes;
        this.totalCuotas = totalCuotas;
        this.cuotasPagadas = cuotasPagadas;
        this.porcentajePagado = porcentajePagado;
        this.totalBasePendiente = totalBasePendiente;
        this.proximaCuota = proximaCuota;
    }

    public SolicitudCredito getCredito() {
        return credito;
    }

    public List<CronogramaPago> getTodasLasCuotas() {
        return todasLasCuotas;
    }

    public List<CronogramaPago> getCuotasPendientes() {
        return cuotasPendientes;
    }

    public int getTotalCuotas() {
        return totalCuotas;
    }

    public int getCuotasPagadas() {
        return cuotasPagadas;
    }

    public int getPorcentajePagado() {
        return porcentajePagado;
    }

    public double getTotalBasePendiente() {
        return totalBasePendiente;
    }

    public CronogramaPago getProximaCuota() {
        return proximaCuota;
    }
}