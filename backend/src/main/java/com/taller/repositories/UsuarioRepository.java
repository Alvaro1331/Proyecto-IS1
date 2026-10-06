package com.taller.repositories;

import com.taller.models.Rol;
import com.taller.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar un usuario por email (para el login)
    Optional<Usuario> findByEmail(String email);

    // Comprobar si un email ya existe (para evitar duplicados al registrar)
    boolean existsByEmail(String email);

    // Buscar todos los usuarios con un rol concreto (ej: todos los mecánicos)
    List<Usuario> findByRol(Rol rol);
}
