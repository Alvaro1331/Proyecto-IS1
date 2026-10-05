package com.taller.repositories;

import com.taller.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio para acceder a la tabla de clientes en la base de datos.
 * Spring genera automáticamente los métodos básicos (save, findById, findAll, delete...).
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Buscar un cliente por su número de teléfono
    Optional<Cliente> findByTelefono(String telefono);
}
