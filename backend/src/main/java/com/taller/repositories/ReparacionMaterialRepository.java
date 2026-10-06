package com.taller.repositories;

import com.taller.models.ReparacionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReparacionMaterialRepository extends JpaRepository<ReparacionMaterial, Long> {

    // Todos los materiales usados en una reparación
    List<ReparacionMaterial> findByReparacionId(Long reparacionId);

    // Todas las reparaciones que han usado un material concreto
    List<ReparacionMaterial> findByMaterialId(Long materialId);
}
