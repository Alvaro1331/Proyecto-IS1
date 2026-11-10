package com.taller.repositories;

import com.taller.models.NotificacionCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la gestión y persistencia de avisos al cliente.
 */
@Repository
public interface NotificacionClienteRepository extends JpaRepository<NotificacionCliente, Long> {

    @Query("SELECT n FROM NotificacionCliente n WHERE n.cliente.id = :clienteId ORDER BY n.fechaCreacion DESC")
    List<NotificacionCliente> findByClienteIdOrderByFechaCreacionDesc(@Param("clienteId") Long clienteId);

    @Query("SELECT n FROM NotificacionCliente n WHERE n.cliente.id = :clienteId AND n.leida = false")
    List<NotificacionCliente> findByClienteIdAndLeidaFalse(@Param("clienteId") Long clienteId);

    @Query("SELECT COUNT(n) FROM NotificacionCliente n WHERE n.cliente.id = :clienteId AND n.leida = false")
    long countByClienteIdAndLeidaFalse(@Param("clienteId") Long clienteId);
}
