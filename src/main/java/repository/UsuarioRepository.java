package com.example.financieraconfianza.repository;

import com.example.financieraconfianza.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Método personalizado indispensable para buscar al usuario por su número de documento en el login
    Optional<Usuario> findByNumDoc(String numDoc);
}