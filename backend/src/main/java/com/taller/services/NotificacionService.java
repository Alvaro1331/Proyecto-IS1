package com.taller.services;

import com.taller.events.NotificacionReparacionEvent;
import com.taller.models.Cliente;
import com.taller.models.NotificacionCliente;
import com.taller.models.Reparacion;
import com.taller.models.TipoNotificacion;
import com.taller.repositories.NotificacionClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * Servicio encargado de la lógica de negocio y procesamiento de avisos/notificaciones
 * dirigidas a clientes, incluyendo escucha reactiva de eventos y gestión de lectura.
 */
@Service
public class NotificacionService {

    @Autowired
    private NotificacionClienteRepository notificacionRepository;

    /**
     * Listener que escucha los eventos publicados desde ReparacionService y genera
     * de forma persistente la notificación correspondiente para el cliente.
     */
    @EventListener
    public void handleNotificacionReparacion(NotificacionReparacionEvent event) {
        if (event == null || event.getCliente() == null) {
            return;
        }
        crearYProcesarNotificacion(
                event.getCliente(),
                event.getReparacion(),
                event.getTipo(),
                event.getTitulo(),
                event.getMensaje()
        );
    }

    /**
     * Crea y persiste una nueva notificación en la base de datos.
     */
    @Transactional
    public NotificacionCliente crearYProcesarNotificacion(
            Cliente cliente,
            Reparacion reparacion,
            TipoNotificacion tipo,
            String titulo,
            String mensaje
    ) {
        NotificacionCliente notificacion = new NotificacionCliente(cliente, reparacion, tipo, titulo, mensaje);
        return notificacionRepository.save(notificacion);
    }

    /**
     * Obtiene el listado completo de notificaciones de un cliente ordenadas cronológicamente de más reciente a más antigua.
     */
    public List<NotificacionCliente> obtenerPorCliente(Long clienteId) {
        if (clienteId == null) {
            return Collections.emptyList();
        }
        return notificacionRepository.findByClienteIdOrderByFechaCreacionDesc(clienteId);
    }

    /**
     * Marca una notificación específica como leída.
     */
    @Transactional
    public void marcarComoLeida(Long notificacionId) {
        if (notificacionId == null) return;
        notificacionRepository.findById(notificacionId).ifPresent(notif -> {
            notif.setLeida(true);
            notificacionRepository.save(notif);
        });
    }

    /**
     * Marca todas las notificaciones pendientes de un cliente como leídas.
     */
    @Transactional
    public void marcarTodasComoLeidas(Long clienteId) {
        if (clienteId == null) return;
        List<NotificacionCliente> pendientes = notificacionRepository.findByClienteIdAndLeidaFalse(clienteId);
        for (NotificacionCliente notif : pendientes) {
            notif.setLeida(true);
        }
        notificacionRepository.saveAll(pendientes);
    }

    /**
     * Cuenta el número de avisos no leídos para un cliente.
     */
    public long contarNoLeidas(Long clienteId) {
        if (clienteId == null) return 0;
        return notificacionRepository.countByClienteIdAndLeidaFalse(clienteId);
    }
}
