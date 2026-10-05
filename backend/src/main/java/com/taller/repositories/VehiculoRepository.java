package com.taller.repositories;

import com.taller.models.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repositorio para acceder a la tabla de vehículos en la base de datos.
 */
@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    // Buscar un vehículo por matrícula (para evitar duplicados)
    Optional<Vehiculo> findByMatricula(String matricula);

    // Comprobar si una matrícula ya existe
    boolean existsByMatricula(String matricula);
}
