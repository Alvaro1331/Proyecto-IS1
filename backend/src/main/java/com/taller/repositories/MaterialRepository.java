package com.taller.repositories;

import com.taller.models.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    // Buscar materiales por nombre (búsqueda parcial, ej: "filtro" encuentra "Filtro de aceite")
    List<Material> findByNombreContainingIgnoreCase(String nombre);

    // Encontrar materiales con stock bajo (para avisar al jefe de que hay que reponer)
    List<Material> findByStockDisponibleLessThan(Integer cantidad);
}
