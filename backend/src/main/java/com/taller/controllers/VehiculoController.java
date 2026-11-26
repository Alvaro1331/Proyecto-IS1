package com.taller.controllers;

import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.services.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de vehículos.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/vehiculos                  → Registrar un nuevo vehículo                │
 * │ GET    /api/vehiculos                  → Listar todos los vehículos                 │
 * │ GET    /api/vehiculos/{id}             → Buscar vehículo por ID                      │
 * │ GET    /api/vehiculos/matricula/{mat}  → Buscar vehículo por matrícula               │
 * └──────────────────────────────────────────────────────────────────────────────────────┘
 *
 * ROLES PERMITIDOS: RECEPCIONISTA, ADMIN
 */
@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin(origins = "*")
public class VehiculoController {

    @Autowired
    private VehiculoService vehiculoService;

    @PostMapping
    public ResponseEntity<?> registrarVehiculo(@Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO respuesta = vehiculoService.registrarVehiculo(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<VehiculoResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(vehiculoService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehiculoService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/matricula/{matricula}")
    public ResponseEntity<?> obtenerPorMatricula(@PathVariable String matricula) {
        try {
            return ResponseEntity.ok(vehiculoService.obtenerPorMatricula(matricula));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #61: Actualiza matrícula, marca, modelo y año de un vehículo registrado.
     * PUT /api/vehiculos/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarVehiculo(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String matricula = body.get("matricula") != null ? body.get("matricula").toString() : null;
            String marca = body.get("marca") != null ? body.get("marca").toString() : null;
            String modelo = body.get("modelo") != null ? body.get("modelo").toString() : null;
            Object anoRaw = body.get("ano") != null ? body.get("ano") : body.get("anio");
            Integer ano = (anoRaw != null && !anoRaw.toString().isBlank()) ? Integer.valueOf(anoRaw.toString()) : null;
            return ResponseEntity.ok(vehiculoService.actualizarVehiculo(id, matricula, marca, modelo, ano));
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El año de fabricación debe ser un número entero válido."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #61: Elimina un vehículo si no tiene historial de reparaciones ni citas asociadas.
     * DELETE /api/vehiculos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(vehiculoService.eliminarVehiculo(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
