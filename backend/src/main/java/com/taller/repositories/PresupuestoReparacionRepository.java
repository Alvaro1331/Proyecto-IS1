package com.taller.repositories;

import com.taller.models.PresupuestoReparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PresupuestoReparacionRepository extends JpaRepository<PresupuestoReparacion, Long> {
    Optional<PresupuestoReparacion> findByReparacionId(Long reparacionId);
    Optional<PresupuestoReparacion> findByNumeroPresupuesto(String numeroPresupuesto);
}
