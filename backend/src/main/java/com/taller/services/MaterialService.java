package com.taller.services;

import com.taller.dtos.MaterialRequestDTO;
import com.taller.dtos.MaterialResponseDTO;
import com.taller.models.Material;
import com.taller.models.ReparacionMaterial;
import com.taller.models.Reparacion;
import com.taller.repositories.MaterialRepository;
import com.taller.repositories.ReparacionMaterialRepository;
import com.taller.repositories.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialService {

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private ReparacionMaterialRepository reparacionMaterialRepository;

    /**
     * Crea un nuevo material en el inventario.
     */
    public MaterialResponseDTO crearMaterial(MaterialRequestDTO dto) {
        Material material = new Material(
                dto.getNombre(),
                dto.getDescripcion(),
                dto.getStockDisponible(),
                dto.getPrecioUnitario()
        );
        materialRepository.save(material);

        return convertirAResponseDTO(material, "Material creado correctamente.");
    }

    /**
     * Devuelve todos los materiales del inventario.
     */
    public List<MaterialResponseDTO> obtenerTodos() {
        return materialRepository.findAll().stream()
                .map(m -> convertirAResponseDTO(m, null))
                .collect(Collectors.toList());
    }

    /**
     * Busca un material por su ID.
     */
    public MaterialResponseDTO obtenerPorId(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el material con ID: " + id));
        return convertirAResponseDTO(material, null);
    }

    /**
     * Busca materiales por nombre (búsqueda parcial).
     * Ej: buscar "filtro" devuelve "Filtro de aceite", "Filtro de aire", etc.
     */
    public List<MaterialResponseDTO> buscarPorNombre(String nombre) {
        return materialRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(m -> convertirAResponseDTO(m, null))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el stock de un material.
     * Útil cuando llega un pedido nuevo al taller.
     */
    public MaterialResponseDTO actualizarStock(Long id, Integer nuevoStock) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el material con ID: " + id));

        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }

        material.setStockDisponible(nuevoStock);
        materialRepository.save(material);

        return convertirAResponseDTO(material, "Stock actualizado correctamente.");
    }

    /**
     * Registra el uso de un material en una reparación.
     * Descuenta automáticamente la cantidad del stock disponible.
     *
     * Este método lo llamará el mecánico cuando use una pieza.
     */
    public MaterialResponseDTO usarMaterialEnReparacion(Long reparacionId, Long materialId, Integer cantidad) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el material con ID: " + materialId));

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        }

        if (material.getStockDisponible() < cantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente de " + material.getNombre()
                            + ". Disponible: " + material.getStockDisponible()
                            + ", solicitado: " + cantidad);
        }

        // Registrar el uso del material en la reparación
        ReparacionMaterial uso = new ReparacionMaterial(reparacion, material, cantidad);
        reparacionMaterialRepository.save(uso);

        // Descontar del stock
        material.setStockDisponible(material.getStockDisponible() - cantidad);
        materialRepository.save(material);

        return convertirAResponseDTO(material,
                "Se han usado " + cantidad + " unidades de " + material.getNombre()
                        + " en la reparación #" + reparacionId
                        + ". Stock restante: " + material.getStockDisponible());
    }

    /**
     * Devuelve los materiales con stock bajo (menos de X unidades).
     * Útil para que el jefe sepa qué hay que reponer.
     */
    public List<MaterialResponseDTO> obtenerStockBajo(Integer umbral) {
        return materialRepository.findByStockDisponibleLessThan(umbral).stream()
                .map(m -> convertirAResponseDTO(m, null))
                .collect(Collectors.toList());
    }

    // --- Método auxiliar ---
    private MaterialResponseDTO convertirAResponseDTO(Material material, String mensaje) {
        return new MaterialResponseDTO(
                material.getId(),
                material.getNombre(),
                material.getDescripcion(),
                material.getStockDisponible(),
                material.getPrecioUnitario(),
                mensaje
        );
    }
}
