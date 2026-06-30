package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudCreditoRepository
        extends JpaRepository<SolicitudCredito, Long> {

    /*
     * Solicitudes por estado para la bandeja administrativa.
     */
    List<SolicitudCredito> findByEstado(
            String estado
    );

    /*
     * Historial completo del cliente.
     */
    List<SolicitudCredito>
    findByUsuarioOrderByFechaSolicitudDesc(
            Usuario usuario
    );

    /*
     * Comprueba si el cliente ya tiene una solicitud
     * en un estado determinado.
     */
    boolean existsByUsuarioAndEstado(
            Usuario usuario,
            String estado
    );
}