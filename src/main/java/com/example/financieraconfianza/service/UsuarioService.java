package com.example.financieraconfianza.service;

import com.example.financieraconfianza.model.entity.Usuario;
import java.util.List;

public interface UsuarioService {

    List<Usuario> listarTodos();

    Usuario buscarPorId(Long id);

    void bloquear(Long id);

    void desbloquear(Long id);

    void guardar(Usuario usuario);
}