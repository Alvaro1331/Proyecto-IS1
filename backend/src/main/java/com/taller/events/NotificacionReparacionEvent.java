package com.taller.events;

import com.taller.models.Cliente;
import com.taller.models.Reparacion;
import com.taller.models.TipoNotificacion;

/**
 * Evento de dominio de Spring publicado por los servicios del taller cuando
 * ocurre un hito o cambio relevante en una reparación.
 */
public class NotificacionReparacionEvent {

    private final Cliente cliente;
    private final Reparacion reparacion;
    private final TipoNotificacion tipo;
    private final String titulo;
    private final String mensaje;

    public NotificacionReparacionEvent(Cliente cliente, Reparacion reparacion, TipoNotificacion tipo, String titulo, String mensaje) {
        this.cliente = cliente;
        this.reparacion = reparacion;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Reparacion getReparacion() {
        return reparacion;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getMensaje() {
        return mensaje;
    }
}
