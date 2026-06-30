package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.CronogramaPago;
import com.example.financieraconfianza.model.entity.GestionCobranza;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GestionCobranzaRepository
        extends JpaRepository<GestionCobranza, Long> {

    // Historial de gestiones de una cuota específica
    List<GestionCobranza>
    findByCronogramaPagoOrderByFechaGestionDesc(
            CronogramaPago cronogramaPago
    );

    // Historial de todas las cuotas de un crédito
    List<GestionCobranza>
    findByCronogramaPago_SolicitudCreditoOrderByFechaGestionDesc(
            SolicitudCredito solicitudCredito
    );

    @Query(value = """
        SELECT *
        FROM gestion_cobranza
        WHERE cronograma_id = :cronogramaId
        ORDER BY fecha_gestion DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<GestionCobranza> buscarUltimaGestionPorCronograma(@Param("cronogramaId") Long cronogramaId);
}