package com.taller.controllers;

import com.taller.models.Cliente;
import com.taller.models.Reparacion;
import com.taller.models.Vehiculo;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.VehiculoRepository;
import com.taller.services.CitaPreviaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Controlador REST para la gestión de Citas Previas de clientes y Calendario del Taller
 * (Historias de Usuario 4 y 5).
 */
@RestController
@RequestMapping("/api/citas")
@CrossOrigin(origins = "*")
public class CitaPreviaController {

    @Autowired
    private CitaPreviaService citaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    /**
     * Consulta la disponibilidad de franjas horarias para una fecha dada.
     * GET /api/citas/disponibilidad?fecha=2026-11-02
     */
    @GetMapping("/disponibilidad")
    public ResponseEntity<?> obtenerDisponibilidad(@RequestParam(required = false) String fecha) {
        try {
            LocalDate f = (fecha != null && !fecha.isBlank()) ? LocalDate.parse(fecha.trim()) : LocalDate.now();
            return ResponseEntity.ok(citaService.obtenerDisponibilidadDia(f));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Formato de fecha inválido: " + e.getMessage()));
        }
    }

    /**
     * Obtiene la vista de calendario del taller (mensual, semanal o diaria) con indicadores de ocupación.
     * GET /api/citas/calendario?desde=2026-11-01&hasta=2026-11-30&estado=TODOS
     */
    @GetMapping("/calendario")
    public ResponseEntity<?> obtenerCalendario(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false, defaultValue = "TODOS") String estado) {
        try {
            LocalDate d = (desde != null && !desde.isBlank()) ? LocalDate.parse(desde.trim()) : LocalDate.now().withDayOfMonth(1);
            LocalDate h = (hasta != null && !hasta.isBlank()) ? LocalDate.parse(hasta.trim()) : d.plusMonths(1).minusDays(1);
            return ResponseEntity.ok(citaService.obtenerCalendarioTaller(d, h, estado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Obtiene las citas previas de un cliente concreto.
     * GET /api/citas/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<?> obtenerCitasPorCliente(@PathVariable Long clienteId) {
        try {
            return ResponseEntity.ok(citaService.obtenerCitasCliente(clienteId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Devuelve todos los vehículos asociados a un cliente (tanto por propiedad directa como por reparaciones vinculadas)
     * para poblar el selector de "Solicitar Cita Previa".
     */
    @GetMapping("/cliente/{clienteId}/vehiculos")
    public ResponseEntity<?> obtenerVehiculosCliente(@PathVariable Long clienteId) {
        Optional<Cliente> opt = clienteRepository.findById(clienteId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Cliente no encontrado."));
        }
        Cliente c = opt.get();
        Map<Long, Vehiculo> mapaVehiculos = new LinkedHashMap<>();

        List<Vehiculo> directos = vehiculoRepository.findAll();
        for (Vehiculo v : directos) {
            if (v.getCliente() != null && clienteId.equals(v.getCliente().getId())) {
                mapaVehiculos.put(v.getId(), v);
            }
        }
        if (c.getReparacionesVinculadas() != null) {
            for (Reparacion r : c.getReparacionesVinculadas()) {
                if (r.getVehiculo() != null) {
                    mapaVehiculos.put(r.getVehiculo().getId(), r.getVehiculo());
                }
            }
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Vehiculo v : mapaVehiculos.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", v.getId());
            item.put("matricula", v.getMatricula());
            item.put("marca", v.getMarca());
            item.put("modelo", v.getModelo());
            item.put("anio", v.getAno());
            resultado.add(item);
        }
        return ResponseEntity.ok(resultado);
    }

    /**
     * Crea una nueva cita previa (desde la app del cliente o manualmente desde recepción).
     * POST /api/citas
     */
    @PostMapping
    public ResponseEntity<?> crearCita(@RequestBody Map<String, Object> body) {
        try {
            Long clienteId = body.get("clienteId") != null && !body.get("clienteId").toString().isBlank()
                    ? Long.valueOf(body.get("clienteId").toString()) : null;
            Long vehiculoId = body.get("vehiculoId") != null && !body.get("vehiculoId").toString().isBlank()
                    ? Long.valueOf(body.get("vehiculoId").toString()) : null;
            String matriculaManual = body.get("matricula") != null ? body.get("matricula").toString() : null;
            String nombreCliente = body.get("nombreCliente") != null ? body.get("nombreCliente").toString() : null;
            String telefonoCliente = body.get("telefonoCliente") != null ? body.get("telefonoCliente").toString() : null;
            String marcaManual = body.get("marca") != null ? body.get("marca").toString() : null;
            String modeloManual = body.get("modelo") != null ? body.get("modelo").toString() : null;

            String fechaStr = body.get("fecha") != null ? body.get("fecha").toString() : null;
            LocalDate fecha = (fechaStr != null && !fechaStr.isBlank()) ? LocalDate.parse(fechaStr.trim()) : null;
            String franjaHoraria = body.get("franjaHoraria") != null ? body.get("franjaHoraria").toString() : null;
            String motivo = body.get("motivo") != null ? body.get("motivo").toString() : null;
            String observaciones = body.get("observaciones") != null ? body.get("observaciones").toString() : null;
            String estado = body.get("estado") != null ? body.get("estado").toString() : "CONFIRMADA";

            Map<String, Object> creada = citaService.crearCita(
                    clienteId, vehiculoId, matriculaManual, nombreCliente, telefonoCliente,
                    marcaManual, modeloManual, fecha, franjaHoraria, motivo, observaciones, estado
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IllegalStateException e) {
            // Conflicto de concurrencia o aforo máximo superado
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Error al agendar la cita: " + e.getMessage()));
        }
    }

    /**
     * Cambia el estado de una cita (PENDIENTE, CONFIRMADA, RECEPCIONADA, CANCELADA).
     * PUT /api/citas/{id}/estado
     */
    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(citaService.cambiarEstadoCita(id, body.get("estado")));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
