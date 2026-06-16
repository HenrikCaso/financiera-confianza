package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método personalizado indispensable para buscar al usuario por su número de documento en el login
    Optional<Usuario> findByNumDoc(String numDoc);

    // Método para buscar a un usuario por su cuenta bancaria
    java.util.Optional<Usuario> findByNumeroCuenta(String numeroCuenta);

    List<Usuario> findByTarjetaBloqueada(Boolean tarjetaBloqueada);
    
}