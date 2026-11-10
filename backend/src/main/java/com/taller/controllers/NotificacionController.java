package com.taller.controllers;

import com.taller.models.NotificacionCliente;
import com.taller.services.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la consulta y actualización de notificaciones del cliente.
 */
@RestController
@RequestMapping({"/api/notificaciones", "/notificaciones"})
@CrossOrigin(origins = "*")
public class NotificacionController {

    @Autowired
    private NotificacionService notificacionService;

    /**
     * Obtiene todas las notificaciones asociadas a un cliente ordenadas de más recientes a más antiguas.
     * GET /api/notificaciones/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<NotificacionCliente>> obtenerPorCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(notificacionService.obtenerPorCliente(clienteId));
    }

    /**
     * Marca una notificación individual como leída.
     * PUT /api/notificaciones/{id}/leer
     */
    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarComoLeida(@PathVariable Long id) {
        notificacionService.marcarComoLeida(id);
        return ResponseEntity.ok(Map.of("message", "Notificación marcada como leída", "id", id));
    }

    /**
     * Marca todas las notificaciones pendientes de un cliente como leídas.
     * PUT /api/notificaciones/cliente/{clienteId}/leer-todas
     */
    @PutMapping("/cliente/{clienteId}/leer-todas")
    public ResponseEntity<?> marcarTodasComoLeidas(@PathVariable Long clienteId) {
        notificacionService.marcarTodasComoLeidas(clienteId);
        return ResponseEntity.ok(Map.of("message", "Todas las notificaciones marcadas como leídas", "clienteId", clienteId));
    }

    /**
     * Obtiene el recuento de notificaciones sin leer de un cliente.
     * GET /api/notificaciones/cliente/{clienteId}/no-leidas/count
     */
    @GetMapping("/cliente/{clienteId}/no-leidas/count")
    public ResponseEntity<?> contarNoLeidas(@PathVariable Long clienteId) {
        long count = notificacionService.contarNoLeidas(clienteId);
        return ResponseEntity.ok(Map.of("clienteId", clienteId, "noLeidas", count));
    }
}
