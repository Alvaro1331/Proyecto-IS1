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
}
