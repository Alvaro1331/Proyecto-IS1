package com.taller.controllers;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.services.ReparacionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de reparaciones, seguimiento temporal, incidencias con fotos,
 * historial de precios, facturación y tesorería del taller.
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

    /**
     * Buscador de historial de precios de reparaciones pasadas similares para orientar a Recepción.
     * GET /api/reparaciones/historial-precios?query=...
     */
    @GetMapping("/historial-precios")
    public ResponseEntity<List<Map<String, Object>>> buscarHistorialPrecios(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        return ResponseEntity.ok(reparacionService.buscarHistorialPrecios(query));
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
     * Actualiza la estimación inicial orientativa de una reparación.
     * PUT /api/reparaciones/{id}/presupuesto
     */
    @PutMapping("/{id}/presupuesto")
    public ResponseEntity<?> actualizarPresupuesto(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object val = body.get("estimacionInicial") != null ? body.get("estimacionInicial")
                    : (body.get("presupuesto") != null ? body.get("presupuesto") : body.get("presupuestoMecanico"));
            if (val == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El importe de la estimación es obligatorio."));
            }
            Double nuevaEstimacion = Double.valueOf(val.toString());
            ReparacionResponseDTO respuesta = reparacionService.actualizarPresupuesto(id, nuevaEstimacion);
            return ResponseEntity.ok(respuesta);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "El importe debe ser un número válido."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * El mecánico actualiza las horas reales de mano de obra dedicadas a la reparación.
     * PUT /api/reparaciones/{id}/horas-trabajo  con body { "horas": 3.5 }
     */
    @PutMapping("/{id}/horas-trabajo")
    public ResponseEntity<?> actualizarHorasTrabajo(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object val = body.get("horas") != null ? body.get("horas") : body.get("horasTrabajoMecanico");
            if (val == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'horas' es obligatorio."));
            }
            Double horas = Double.valueOf(val.toString());
            ReparacionResponseDTO respuesta = reparacionService.actualizarHorasTrabajo(id, horas);
            return ResponseEntity.ok(respuesta);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Las horas deben ser un número válido."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * El mecánico o recepcionista modifica los días estimados de la reparación.
     * PUT /api/reparaciones/{id}/dias-estimados  con body { "dias": 5 }
     */
    @PutMapping("/{id}/dias-estimados")
    public ResponseEntity<?> actualizarDiasEstimados(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Object val = body.get("dias") != null ? body.get("dias") : body.get("diasEstimados");
            if (val == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'dias' es obligatorio."));
            }
            Integer dias = Integer.valueOf(val.toString());
            ReparacionResponseDTO respuesta = reparacionService.actualizarDiasEstimados(id, dias);
            return ResponseEntity.ok(respuesta);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Los días estimados deben ser un número entero válido."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Registra una incidencia con foto opcional subida por el mecánico (Multipart).
     * POST /api/reparaciones/{id}/incidencias
     */
    @PostMapping(value = "/{id}/incidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registrarIncidenciaMultipart(
            @PathVariable Long id,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "impactoPrecio", required = false) Double impactoPrecio,
            @RequestParam(value = "foto", required = false) MultipartFile foto) {
        try {
            Map<String, Object> respuesta = reparacionService.registrarIncidencia(id, descripcion, impactoPrecio, foto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Variante JSON para registrar una incidencia sin foto.
     */
    @PostMapping(value = "/{id}/incidencias", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registrarIncidenciaJson(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String descripcion = body.get("descripcion") != null ? body.get("descripcion").toString() : null;
            Double impacto = body.get("impactoPrecio") != null && !body.get("impactoPrecio").toString().isBlank()
                    ? Double.valueOf(body.get("impactoPrecio").toString())
                    : 0.0;
            Map<String, Object> respuesta = reparacionService.registrarIncidencia(id, descripcion, impacto, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene todas las incidencias y fotos de una reparación en orden cronológico.
     * GET /api/reparaciones/{id}/incidencias
     */
    @GetMapping("/{id}/incidencias")
    public ResponseEntity<?> obtenerIncidencias(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerIncidencias(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recepción formaliza (o actualiza) la Factura oficial de una reparación.
     * POST /api/reparaciones/{id}/factura
     */
    @PostMapping("/{id}/factura")
    public ResponseEntity<?> generarFactura(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Double horasOpcionales = null;
            if (body != null && body.get("horasManoObra") != null && !body.get("horasManoObra").toString().isBlank()) {
                horasOpcionales = Double.valueOf(body.get("horasManoObra").toString());
            }
            Map<String, Object> factura = reparacionService.generarFactura(id, horasOpcionales);
            return ResponseEntity.status(HttpStatus.CREATED).body(factura);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene la Factura emitida de una reparación.
     * GET /api/reparaciones/{id}/factura
     */
    @GetMapping("/{id}/factura")
    public ResponseEntity<?> obtenerFactura(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerFacturaPorReparacion(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
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
