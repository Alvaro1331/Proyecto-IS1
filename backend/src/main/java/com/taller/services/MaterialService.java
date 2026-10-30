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

    @Autowired
    private ReparacionService reparacionService;

    /**
     * Crea un nuevo material en el inventario y descuenta su compra inicial de la cuenta del taller.
     */
    public MaterialResponseDTO crearMaterial(MaterialRequestDTO dto) {
        int stockInicial = dto.getStockDisponible() != null ? dto.getStockDisponible() : 0;
        double precioUnit = dto.getPrecioUnitario() != null ? dto.getPrecioUnitario() : 0.0;
        double costeCompra = stockInicial * precioUnit;

        Material material = new Material(
                dto.getNombre(),
                dto.getDescripcion(),
                stockInicial,
                precioUnit
        );
        materialRepository.save(material);

        if (costeCompra > 0 && reparacionService != null) {
            reparacionService.registrarGastoCompraStock(costeCompra);
        }

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
     */
    public List<MaterialResponseDTO> buscarPorNombre(String nombre) {
        return materialRepository.findByNombreContainingIgnoreCase(nombre).stream()
                .map(m -> convertirAResponseDTO(m, null))
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el stock de un material.
     */
    public MaterialResponseDTO actualizarStock(Long id, Integer nuevoStock) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el material con ID: " + id));

        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }

        int stockAnterior = material.getStockDisponible() != null ? material.getStockDisponible() : 0;
        int diferencia = nuevoStock - stockAnterior;
        double precioUnit = material.getPrecioUnitario() != null ? material.getPrecioUnitario() : 0.0;

        material.setStockDisponible(nuevoStock);
        materialRepository.save(material);

        if (diferencia > 0 && reparacionService != null) {
            reparacionService.registrarGastoCompraStock(diferencia * precioUnit);
        }

        return convertirAResponseDTO(material, "Stock actualizado correctamente.");
    }

    /**
     * Registra el uso de un material en una reparación.
     * El mecánico trabaja libremente sin tope presupuestario artificial:
     * únicamente se comprueba que haya stock disponible suficiente en el almacén.
     */
    public MaterialResponseDTO usarMaterialEnReparacion(Long reparacionId, Long materialId, Integer cantidad) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        if (!reparacionService.tienePresupuestoAceptado(reparacionId)) {
            throw new IllegalArgumentException(
                    "Operación bloqueada: no se pueden consumir piezas del almacén hasta que el cliente haya ACEPTADO el presupuesto desglosado.");
        }

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el material con ID: " + materialId));

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0.");
        }

        if (material.getStockDisponible() < cantidad) {
            throw new IllegalArgumentException(
                    "Stock insuficiente de " + material.getNombre()
                            + ". Disponible: " + material.getStockDisponible()
                            + ", solicitado: " + cantidad);
        }

        double precioUnit = material.getPrecioUnitario() != null ? material.getPrecioUnitario() : 0.0;
        double costeNuevo = cantidad * precioUnit;

        // Registrar el uso del material en la reparación
        ReparacionMaterial uso = new ReparacionMaterial(reparacion, material, cantidad);
        reparacionMaterialRepository.save(uso);

        // Descontar del stock físico del almacén
        material.setStockDisponible(material.getStockDisponible() - cantidad);
        materialRepository.save(material);

        double costeAcumulado = reparacionMaterialRepository.findByReparacionId(reparacionId).stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();

        return convertirAResponseDTO(material,
                String.format("Se han usado %d ud(s) de %s (%.2f €) en la reparación #%d. Coste total acumulado en piezas: %.2f €.",
                        cantidad, material.getNombre(), costeNuevo, reparacionId, costeAcumulado));
    }

    /**
     * Devuelve los materiales con stock bajo (menos de X unidades).
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
