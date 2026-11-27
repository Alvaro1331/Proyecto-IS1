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
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerTodas(
            @RequestParam(value = "orden", required = false) String orden) {
        return ResponseEntity.ok(reparacionService.obtenerTodas(orden));
    }

    /**
     * HU #8: Registro cronológico de vehículos pendientes en espera,
     * ordenados automáticamente desde el que lleva más tiempo en el taller al más reciente.
     * GET /api/reparaciones/registro-cronologico
     */
    @GetMapping("/registro-cronologico")
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerRegistroCronologicoEspera() {
        return ResponseEntity.ok(reparacionService.obtenerRegistroCronologicoEspera());
    }

    /**
     * HU #20: Supervisión del tiempo invertido por cada mecánico (protegido para rol ADMIN).
     * GET /api/reparaciones/supervision-tiempos?mecanicoId=...&periodo=TODOS&rol=ADMIN
     */
    @GetMapping("/supervision-tiempos")
    public ResponseEntity<?> obtenerSupervisionTiempos(
            @RequestParam(value = "mecanicoId", required = false) Long mecanicoId,
            @RequestParam(value = "periodo", required = false, defaultValue = "TODOS") String periodo,
            @RequestParam(value = "rol", required = false) String rol) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerSupervisionTiemposMecanicos(mecanicoId, periodo, rol));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
     * Devuelve el historial detallado de gastos de Tesorería (compras de stock y uso de materiales).
     * GET /api/reparaciones/gastos
     */
    @GetMapping("/gastos")
    public ResponseEntity<List<Map<String, Object>>> obtenerHistorialGastos() {
        return ResponseEntity.ok(reparacionService.obtenerHistorialGastos());
    }

    /**
     * Elimina un gasto registrado en la Tesorería (por ejemplo, una compra de material añadida por error),
     * reembolsando el importe a la caja del taller y revirtiendo las unidades de stock.
     * DELETE /api/reparaciones/gastos/{gastoId}
     */
    @com.taller.security.RequiereRol({"ADMIN"})
    @DeleteMapping("/gastos/{gastoId}")
    public ResponseEntity<?> eliminarGastoTesoreria(@PathVariable String gastoId) {
        try {
            return ResponseEntity.ok(reparacionService.eliminarGastoTesoreria(gastoId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
     * Elimina únicamente la foto subida de una incidencia (manteniendo el texto de la incidencia).
     * DELETE /api/reparaciones/{id}/incidencias/{incidenciaId}/foto
     */
    @DeleteMapping("/{id}/incidencias/{incidenciaId}/foto")
    public ResponseEntity<?> eliminarFotoIncidencia(@PathVariable Long id, @PathVariable Long incidenciaId) {
        try {
            return ResponseEntity.ok(reparacionService.eliminarFotoIncidencia(id, incidenciaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Elimina por completo una incidencia (y su foto asociada si la tiene).
     * DELETE /api/reparaciones/{id}/incidencias/{incidenciaId}
     */
    @DeleteMapping("/{id}/incidencias/{incidenciaId}")
    public ResponseEntity<?> eliminarIncidencia(@PathVariable Long id, @PathVariable Long incidenciaId) {
        try {
            return ResponseEntity.ok(reparacionService.eliminarIncidencia(id, incidenciaId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            String dniCif = null;
            String direccionFiscal = null;
            boolean validarDatosFiscales = false;
            if (body != null) {
                if (body.get("horasManoObra") != null && !body.get("horasManoObra").toString().isBlank()) {
                    horasOpcionales = Double.valueOf(body.get("horasManoObra").toString());
                }
                if (body.get("dniCif") != null) {
                    dniCif = body.get("dniCif").toString();
                }
                if (body.get("direccionFiscal") != null) {
                    direccionFiscal = body.get("direccionFiscal").toString();
                }
                if (body.get("validarDatosFiscales") != null) {
                    validarDatosFiscales = Boolean.parseBoolean(body.get("validarDatosFiscales").toString());
                }
            }
            Map<String, Object> factura = reparacionService.generarFactura(id, horasOpcionales, dniCif, direccionFiscal, validarDatosFiscales);
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

    /**
     * Historia de Usuario 1: Crea o actualiza el Presupuesto Desglosado (Piezas + Mano de obra)
     * tras realizar el diagnóstico técnico, dejándolo en estado PENDIENTE_ACEPTACION.
     * POST /api/reparaciones/{id}/presupuesto-formal
     */
    @PostMapping("/{id}/presupuesto-formal")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> guardarPresupuestoFormal(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String diagnostico = body.get("diagnosticoPrevio") != null ? body.get("diagnosticoPrevio").toString() : null;
            Integer diasValidez = body.get("diasValidez") != null && !body.get("diasValidez").toString().isBlank()
                    ? Integer.valueOf(body.get("diasValidez").toString()) : 15;
            List<Map<String, Object>> lineas = (List<Map<String, Object>>) body.get("lineas");
            Map<String, Object> resultado = reparacionService.guardarPresupuestoDesglosado(id, diagnostico, diasValidez, lineas);
            return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al procesar el presupuesto: " + e.getMessage()));
        }
    }

    /**
     * Obtiene el Presupuesto Desglosado de una reparación.
     * GET /api/reparaciones/{id}/presupuesto-formal
     */
    @GetMapping("/{id}/presupuesto-formal")
    public ResponseEntity<?> obtenerPresupuestoFormal(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.obtenerPresupuestoPorReparacion(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Historia de Usuario 3: Permite al cliente ACEPTAR o RECHAZAR el presupuesto
     * introduciendo obligatoriamente su contraseña de cuenta.
     * POST /api/reparaciones/{id}/presupuesto-formal/responder
     */
    @PostMapping("/{id}/presupuesto-formal/responder")
    public ResponseEntity<?> responderPresupuestoFormal(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            String accion = body.get("accion");
            String password = body.get("password");
            String motivoRechazo = body.get("motivoRechazo");
            Map<String, Object> respuesta = reparacionService.responderPresupuestoCliente(id, accion, password, motivoRechazo);
            return ResponseEntity.ok(respuesta);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Punto 2: Permite a Recepción o Administración registrar la ACEPTACIÓN o RECHAZO
     * presencial/telefónico de un presupuesto cuando el cliente no tiene cuenta web.
     * POST /api/reparaciones/{id}/presupuesto-formal/responder-presencial
     */
    @PostMapping("/{id}/presupuesto-formal/responder-presencial")
    public ResponseEntity<?> responderPresupuestoFormalPresencial(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            String accion = body.get("accion") != null ? body.get("accion").toString() : null;
            String canal = body.get("canal") != null ? body.get("canal").toString()
                    : (body.get("canalConfirmacion") != null ? body.get("canalConfirmacion").toString() : null);
            String motivoRechazo = body.get("motivoRechazo") != null ? body.get("motivoRechazo").toString() : null;
            Long usuarioId = body.get("usuarioId") != null && !body.get("usuarioId").toString().isBlank()
                    ? Long.valueOf(body.get("usuarioId").toString()) : null;
            Map<String, Object> respuesta = reparacionService.responderPresupuestoPersonal(
                    id, accion, canal, motivoRechazo, usuarioId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Punto 3: Cancela definitivamente una reparación tras el rechazo del presupuesto
     * y retira el vehículo sin reparar de la lista activa del taller.
     * POST /api/reparaciones/{id}/cancelar
     */
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<?> cancelarReparacionPorRechazo(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String motivo = (body != null && body.get("motivo") != null) ? body.get("motivo").toString() : null;
            Long clienteId = (body != null && body.get("clienteId") != null && !body.get("clienteId").toString().isBlank())
                    ? Long.valueOf(body.get("clienteId").toString()) : null;
            Long usuarioId = (body != null && body.get("usuarioId") != null && !body.get("usuarioId").toString().isBlank())
                    ? Long.valueOf(body.get("usuarioId").toString()) : null;
            return ResponseEntity.ok(reparacionService.cancelarReparacionPorRechazo(id, motivo, clienteId, usuarioId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #19: Inicia el temporizador de mano de obra de una reparación EN_PROCESO.
     * POST /api/reparaciones/{id}/temporizador/iniciar
     */
    @PostMapping("/{id}/temporizador/iniciar")
    public ResponseEntity<?> iniciarTemporizador(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Long mecanicoId = (body != null && body.get("mecanicoId") != null && !body.get("mecanicoId").toString().isBlank())
                    ? Long.valueOf(body.get("mecanicoId").toString()) : null;
            return ResponseEntity.ok(reparacionService.iniciarTemporizador(id, mecanicoId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #19: Pausa el temporizador de mano de obra acumulando los minutos dedicados.
     * POST /api/reparaciones/{id}/temporizador/pausar
     */
    @PostMapping("/{id}/temporizador/pausar")
    public ResponseEntity<?> pausarTemporizador(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.pausarTemporizador(id));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #19: Detiene definitivamente el temporizador y guarda el total en la ficha de la avería.
     * POST /api/reparaciones/{id}/temporizador/terminar
     */
    @PostMapping("/{id}/temporizador/terminar")
    public ResponseEntity<?> terminarTemporizador(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reparacionService.terminarTemporizador(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #21: Historial técnico de reparaciones pasadas concluidas de un vehículo (modo solo lectura para mecánicos).
     * GET /api/reparaciones/vehiculo/{vehiculoId}/historial-mecanico?excluirReparacionId=...
     */
    @GetMapping("/vehiculo/{vehiculoId}/historial-mecanico")
    public ResponseEntity<List<ReparacionResponseDTO>> obtenerHistorialVehiculoParaMecanico(
            @PathVariable Long vehiculoId,
            @RequestParam(value = "excluirReparacionId", required = false) Long excluirReparacionId) {
        return ResponseEntity.ok(reparacionService.obtenerHistorialVehiculoParaMecanico(vehiculoId, excluirReparacionId));
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
