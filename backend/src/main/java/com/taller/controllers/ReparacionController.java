package com.taller.controllers;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.services.ReparacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de reparaciones.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/reparaciones                          → Crear nueva reparación           │
 * │ GET    /api/reparaciones                          → Listar todas las reparaciones    │
 * │ GET    /api/reparaciones/{id}                     → Buscar reparación por ID          │
 * │ PUT    /api/reparaciones/{id}/asignar/{mecanicoId}→ Asignar mecánico                  │
 * │ PUT    /api/reparaciones/{id}/estado              → Actualizar estado                 │
 * │ GET    /api/reparaciones/estado/{estado}          → Filtrar por estado                │
 * │ GET    /api/reparaciones/mecanico/{mecanicoId}    → Reparaciones de un mecánico       │
 * │ GET    /api/reparaciones/vehiculo/{vehiculoId}    → Reparaciones de un vehículo       │
 * └──────────────────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/reparaciones")
@CrossOrigin(origins = "*")
public class ReparacionController {

    @Autowired
    private ReparacionService reparacionService;

    @PostMapping
    public ResponseEntity<?> crearReparacion(@Valid @RequestBody ReparacionRequestDTO dto) {
        try {
            ReparacionResponseDTO respuesta = reparacionService.crearReparacion(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(reparacionService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Asigna un mecánico a una reparación.
     * PUT /api/reparaciones/5/asignar/3   → Asigna el mecánico con ID 3 a la reparación 5
     */
    @PutMapping("/{id}/asignar/{mecanicoId}")
    public ResponseEntity<?> asignarMecanico(@PathVariable Long id, @PathVariable Long mecanicoId) {
        try {
            ReparacionResponseDTO respuesta = reparacionService.asignarMecanico(id, mecanicoId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualiza el estado de una reparación.
     * El frontend envía: { "estado": "FINALIZADA" }
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String nuevoEstado = body.get("estado");
            if (nuevoEstado == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es obligatorio."));
            }
            ReparacionResponseDTO respuesta = reparacionService.actualizarEstado(id, nuevoEstado);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<?> obtenerPorEstado(@PathVariable String estado) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerPorEstado(estado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mecanico/{mecanicoId}")
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerPorMecanico(@PathVariable Long mecanicoId) {
        return ResponseEntity.ok(reparacionService.obtenerPorMecanico(mecanicoId));
    }

    @GetMapping("/vehiculo/{vehiculoId}")
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerPorVehiculo(@PathVariable Long vehiculoId) {
        return ResponseEntity.ok(reparacionService.obtenerPorVehiculo(vehiculoId));
    }
}
