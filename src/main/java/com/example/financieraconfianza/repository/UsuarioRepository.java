package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNumDoc(String numDoc);

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByNumeroCuenta(String numeroCuenta);

    Optional<Usuario> findByCci(String cci);

    List<Usuario> findByTarjetaBloqueada(Boolean tarjetaBloqueada);

    // Versión sin paginación, por si se usa en otra parte
    List<Usuario> findByRol(String rol);

    // Versión con paginación usada en Gestión de Usuarios
    Page<Usuario> findByRol(
            String rol,
            Pageable pageable
    );

    // Buscador sin paginación
    @Query("""
           SELECT u
           FROM Usuario u
           WHERE u.rol = 'CLIENTE'
           AND (
               LOWER(COALESCE(u.numDoc, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.nombres, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.apellidos, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.correo, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
           )
           ORDER BY u.id DESC
           """)
    List<Usuario> buscarClientes(
            @Param("texto") String texto
    );

    // Buscador con paginación
    @Query("""
           SELECT u
           FROM Usuario u
           WHERE u.rol = 'CLIENTE'
           AND (
               LOWER(COALESCE(u.numDoc, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.nombres, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.apellidos, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(COALESCE(u.correo, '')) LIKE LOWER(CONCAT('%', :texto, '%'))
           )
           """)
    Page<Usuario> buscarClientes(
            @Param("texto") String texto,
            Pageable pageable
    );
}
