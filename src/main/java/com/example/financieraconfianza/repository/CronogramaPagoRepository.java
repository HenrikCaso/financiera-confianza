package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.CronogramaPago;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CronogramaPagoRepository
        extends JpaRepository<CronogramaPago, Long> {

    List<CronogramaPago> findBySolicitudCredito(
            SolicitudCredito solicitudCredito
    );

    List<CronogramaPago>
    findBySolicitudCreditoOrderByNumeroCuotaAsc(
            SolicitudCredito solicitudCredito
    );

    List<CronogramaPago> findByPagadaFalse();

    List<CronogramaPago>
    findByPagadaFalseAndFechaVencimientoBefore(
            LocalDate fecha
    );

    List<CronogramaPago>
    findBySolicitudCreditoAndPagadaFalse(
            SolicitudCredito solicitudCredito
    );

    /*
     * Indica si el crédito todavía tiene al menos
     * una cuota sin pagar.
     */
    boolean existsBySolicitudCreditoAndPagadaFalse(
            SolicitudCredito solicitudCredito
    );

    /*
     * Cantidad de cuotas pendientes del crédito.
     */
    long countBySolicitudCreditoAndPagadaFalse(
            SolicitudCredito solicitudCredito
    );

    /*
     * Cantidad de cuotas ya pagadas.
     */
    long countBySolicitudCreditoAndPagadaTrue(
            SolicitudCredito solicitudCredito
    );
}