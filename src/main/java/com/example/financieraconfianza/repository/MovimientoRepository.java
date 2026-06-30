package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.Usuario;
import com.example.financieraconfianza.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {
    // Busca todos los movimientos de un usuario ordenados de más reciente a más antiguo
    List<Movimiento> findByUsuarioOrderByFechaDesc(Usuario usuario);

    // Busca movimientos de un usuario en un rango de fechas
    List<Movimiento> findByUsuarioAndFechaBetweenOrderByFechaDesc(Usuario usuario, LocalDateTime inicio, LocalDateTime fin);

    List<Movimiento> findByUsuario(Usuario usuario);

    List<Movimiento> findTop10ByUsuarioOrderByFechaDesc(Usuario usuario);
}