package com.taller.repositories;

import com.taller.models.IncidenciaReparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidenciaReparacionRepository extends JpaRepository<IncidenciaReparacion, Long> {
    List<IncidenciaReparacion> findByReparacionIdOrderByFechaAsc(Long reparacionId);
}
