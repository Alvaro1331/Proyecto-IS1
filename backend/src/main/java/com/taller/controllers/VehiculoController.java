package com.taller.controllers;

import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.services.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la gestión de vehículos.
 *
 * ENDPOINTS DISPONIBLES:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ POST   /api/vehiculos          → Registrar un nuevo vehículo    │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * ROLES PERMITIDOS:
 * - RECEPCIONISTA: puede registrar vehículos
 * - ADMIN/JEFE: puede hacer todo lo que hace el recepcionista
 * (La validación de roles se añadirá en el Sprint de Seguridad)
 */
@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin(origins = "*") // Permite peticiones desde el Frontend (HTML/JS)
public class VehiculoController {

    @Autowired
    private VehiculoService vehiculoService;

    /**
     * POST /api/vehiculos
     * Registra un nuevo vehículo en la base de datos.
     *
     * Responde con:
     *   - 201 CREATED  → Si el vehículo se registró correctamente
     *   - 400 BAD REQUEST → Si la matrícula ya existe o los datos son incorrectos
     */
    @PostMapping
    public ResponseEntity<?> registrarVehiculo(@Valid @RequestBody VehiculoRequestDTO dto) {
        try {
            VehiculoResponseDTO respuesta = vehiculoService.registrarVehiculo(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            // Error controlado: matrícula duplicada
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
