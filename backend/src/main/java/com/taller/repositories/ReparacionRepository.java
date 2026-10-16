package com.taller.repositories;

import com.taller.models.EstadoReparacion;
import com.taller.models.Reparacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReparacionRepository extends JpaRepository<Reparacion, Long> {

    // Buscar una reparación por su código localizador único
    Optional<Reparacion> findByLocalizador(String localizador);

    // Todas las reparaciones de un vehículo concreto
    List<Reparacion> findByVehiculoId(Long vehiculoId);

    // Todas las reparaciones asignadas a un mecánico
    List<Reparacion> findByMecanicoId(Long mecanicoId);

    // Todas las reparaciones en un estado concreto (ej: todas las PENDIENTES)
    List<Reparacion> findByEstado(EstadoReparacion estado);

    // Todas las reparaciones que creó un recepcionista
    List<Reparacion> findByRecepcionistaId(Long recepcionistaId);
}
