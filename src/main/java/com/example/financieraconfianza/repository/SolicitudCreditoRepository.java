package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.SolicitudCredito;
import com.example.financieraconfianza.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCredito, Long> {

    // Para buscar todas las solicitudes pendientes (Para la vista del empleado del banco)
    List<SolicitudCredito> findByEstado(String estado);

    // Para que el cliente pueda ver su historial de solicitudes
    List<SolicitudCredito> findByUsuarioOrderByFechaSolicitudDesc(Usuario usuario);
    
}