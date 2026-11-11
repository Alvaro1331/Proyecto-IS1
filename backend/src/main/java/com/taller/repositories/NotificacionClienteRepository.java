package com.taller.repositories;

import com.taller.models.NotificacionCliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionClienteRepository extends JpaRepository<NotificacionCliente, Long> {

    List<NotificacionCliente> findByClienteIdOrderByFechaCreacionDesc(Long clienteId);

    long countByClienteIdAndLeidaFalse(Long clienteId);

    List<NotificacionCliente> findByReparacionIdOrderByFechaCreacionDesc(Long reparacionId);
}
