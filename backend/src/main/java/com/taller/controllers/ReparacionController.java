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
 * Controlador REST para la gestión de reparaciones y finanzas del taller.
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

    /**
     * Devuelve el resumen financiero de la Cuenta General del Taller.
     * GET /api/reparaciones/cuenta-taller
     */
    @GetMapping("/cuenta-taller")
    public ResponseEntity<Map<String, Object>> obtenerCuentaTaller() {
        return ResponseEntity.ok(reparacionService.obtenerCuentaTaller());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/localizador/{codigo}")
    public ResponseEntity<?> obtenerPorLocalizador(@PathVariable String codigo) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerPorLocalizador(codigo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/asignar/{mecanicoId}")
    public ResponseEntity<?> asignarMecanico(@PathVariable Long id, @PathVariable Long mecanicoId) {
        try {
            ReparacionResponseDTO respuesta = reparacionService.asignarMecanico(id, mecanicoId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

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

    /**
     * El Administrador actualiza o amplía el presupuesto asignado al mecánico para una reparación.
     * PUT /api/reparaciones/{id}/presupuesto  con body { "presupuestoMecanico": 100.0 } o { "presupuesto": 100.0 }
     */
    @PutMapping("/{id}/presupuesto")
    public ResponseEntity<?> actualizarPresupuesto(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object val = body.get("presupuestoMecanico") != null ? body.get("presupuestoMecanico") : body.get("presupuesto");
            if (val == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'presupuestoMecanico' es obligatorio."));
            }
            Double nuevoPresupuesto = Double.valueOf(val.toString());
            ReparacionResponseDTO respuesta = reparacionService.actualizarPresupuesto(id, nuevoPresupuesto);
            return ResponseEntity.ok(respuesta);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El presupuesto debe ser un número válido."));
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
