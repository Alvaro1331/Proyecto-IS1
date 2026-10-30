package com.taller.repositories;

import com.taller.models.CitaPrevia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaPreviaRepository extends JpaRepository<CitaPrevia, Long> {
    List<CitaPrevia> findByFechaOrderByFranjaHorariaAsc(LocalDate fecha);
    List<CitaPrevia> findByFechaBetweenOrderByFechaAscFranjaHorariaAsc(LocalDate desde, LocalDate hasta);
    List<CitaPrevia> findByClienteIdOrderByFechaDescFranjaHorariaAsc(Long clienteId);
    Optional<CitaPrevia> findByCodigoCita(String codigoCita);
}
