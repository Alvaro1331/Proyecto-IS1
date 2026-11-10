package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una notificación o aviso emitido para un cliente
 * respecto al estado o incidencias de una reparación.
 */
@Entity
@Table(name = "notificaciones_cliente")
public class NotificacionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnore
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reparacion_id")
    @JsonIgnore
    private Reparacion reparacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    public NotificacionCliente() {
        this.fechaCreacion = LocalDateTime.now();
        this.leida = false;
    }

    public NotificacionCliente(Cliente cliente, Reparacion reparacion, TipoNotificacion tipo, String titulo, String mensaje) {
        this.cliente = cliente;
        this.reparacion = reparacion;
        this.tipo = tipo;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.leida = false;
        this.fechaCreacion = LocalDateTime.now();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Reparacion getReparacion() {
        return reparacion;
    }

    public void setReparacion(Reparacion reparacion) {
        this.reparacion = reparacion;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    // Propiedades adicionales calculadas para serialización JSON hacia el frontend
    @Transient
    public String getLocalizador() {
        return (reparacion != null) ? reparacion.getLocalizador() : null;
    }

    @Transient
    public String getMatriculaVehiculo() {
        if (reparacion != null && reparacion.getVehiculo() != null) {
            return reparacion.getVehiculo().getMatricula();
        }
        return null;
    }

    @Transient
    public Long getClienteId() {
        return (cliente != null) ? cliente.getId() : null;
    }

    @Transient
    public Long getReparacionId() {
        return (reparacion != null) ? reparacion.getId() : null;
    }
}
