package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.HistorialPrestamo;
import com.example.financieraconfianza.model.entity.SolicitudCredito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialPrestamoRepository extends JpaRepository<HistorialPrestamo, Long> {

    List<HistorialPrestamo> findBySolicitudCreditoOrderByFechaAsc(
            SolicitudCredito solicitudCredito
    );
}