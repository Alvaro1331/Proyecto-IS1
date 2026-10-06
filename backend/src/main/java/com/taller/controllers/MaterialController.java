package com.taller.controllers;

import com.taller.dtos.MaterialRequestDTO;
import com.taller.dtos.MaterialResponseDTO;
import com.taller.services.MaterialService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de materiales del inventario.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/materiales                                → Crear nuevo material                │
 * │ GET    /api/materiales                                → Listar todos los materiales         │
 * │ GET    /api/materiales/{id}                           → Buscar material por ID               │
 * │ GET    /api/materiales/buscar?nombre=filtro           → Buscar materiales por nombre          │
 * │ PUT    /api/materiales/{id}/stock                     → Actualizar stock de un material       │
 * │ POST   /api/materiales/{materialId}/usar/{reparacionId} → Registrar uso en reparación        │
 * │ GET    /api/materiales/stock-bajo?umbral=5            → Materiales con stock bajo             │
 * └──────────────────────────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/materiales")
@CrossOrigin(origins = "*")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @PostMapping
    public ResponseEntity<?> crearMaterial(@Valid @RequestBody MaterialRequestDTO dto) {
        try {
            MaterialResponseDTO respuesta = materialService.crearMaterial(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<MaterialResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(materialService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(materialService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<MaterialResponseDTO>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(materialService.buscarPorNombre(nombre));
    }

    /**
     * Actualizar el stock de un material.
     * El frontend envía: { "stock": 50 }
     */
    @PutMapping("/{id}/stock")
    public ResponseEntity<?> actualizarStock(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            Integer nuevoStock = body.get("stock");
            if (nuevoStock == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'stock' es obligatorio."));
            }
            MaterialResponseDTO respuesta = materialService.actualizarStock(id, nuevoStock);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Registrar el uso de un material en una reparación.
     * El frontend envía: { "cantidad": 2 }
     *
     * Ejemplo: POST /api/materiales/3/usar/1
     * → Usa el material con ID 3 en la reparación con ID 1
     */
    @PostMapping("/{materialId}/usar/{reparacionId}")
    public ResponseEntity<?> usarMaterial(@PathVariable Long materialId,
                                           @PathVariable Long reparacionId,
                                           @RequestBody Map<String, Integer> body) {
        try {
            Integer cantidad = body.get("cantidad");
            if (cantidad == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'cantidad' es obligatorio."));
            }
            MaterialResponseDTO respuesta = materialService.usarMaterialEnReparacion(
                    reparacionId, materialId, cantidad);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Materiales con stock por debajo de un umbral.
     * GET /api/materiales/stock-bajo?umbral=5  → devuelve materiales con menos de 5 unidades
     */
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<MaterialResponseDTO>> obtenerStockBajo(
            @RequestParam(defaultValue = "5") Integer umbral) {
        return ResponseEntity.ok(materialService.obtenerStockBajo(umbral));
    }
}
