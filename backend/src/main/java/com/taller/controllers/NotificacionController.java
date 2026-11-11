package com.taller.controllers;

import com.taller.models.NotificacionCliente;
import com.taller.services.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para consultar y gestionar las notificaciones de los clientes.
 */
@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(origins = "*")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Obtiene todas las notificaciones recibidas por un cliente, ordenadas de más reciente a más antigua.
     * GET /api/notificaciones/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<NotificacionCliente>> obtenerPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(notificacionService.obtenerPorCliente(clienteId));
    }

    /**
     * Devuelve el número de notificaciones no leídas para actualizar la campana 🔔 del cliente.
     * GET /api/notificaciones/cliente/{clienteId}/no-leidas
     */
    @GetMapping("/cliente/{clienteId}/no-leidas")
    public ResponseEntity<Map<String, Object>> contarNoLeidas(@PathVariable Long clienteId) {
        long cuenta = notificacionService.contarNoLeidas(clienteId);
        return ResponseEntity.ok(Map.of(
                "clienteId", clienteId,
                "noLeidas", cuenta
        ));
    }

    /**
     * Marca una notificación como leída.
     * PUT /api/notificaciones/{id}/leer
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Long id) {
        try {
            NotificacionCliente actualizada = notificacionService.marcarComoLeida(id);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Marca todas las notificaciones de un cliente como leídas.
     * PUT /api/notificaciones/cliente/{clienteId}/leer-todas
     */
    @PutMapping("/cliente/{clienteId}/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas(@PathVariable Long clienteId) {
        try {
            notificacionService.marcarTodasComoLeidas(clienteId);
            return ResponseEntity.ok(Map.of("mensaje", "Todas las notificaciones se han marcado como leídas."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Elimina una notificación por ID.
     * DELETE /api/notificaciones/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarNotificacion(@PathVariable Long id) {
        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.ok(Map.of("mensaje", "Notificación eliminada correctamente."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
