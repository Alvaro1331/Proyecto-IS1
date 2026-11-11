package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa una notificación dirigida a un cliente sobre el avance de la reparación de su vehículo,
 * avisos de presupuestos o incidencias técnicas.
 */
@Entity
@Table(name = "notificaciones_cliente")
public class NotificacionCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    @JsonIgnoreProperties({"vehiculos", "reparacionesVinculadas", "password"})
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reparacion_id")
    @JsonIgnoreProperties({"materialesUsados", "incidencias", "factura", "presupuestoFormal"})
    private Reparacion reparacion;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoNotificacion tipo;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Column(nullable = false)
    private boolean leida = false;

    // --- Constructores ---
    public NotificacionCliente() {}

    public NotificacionCliente(Cliente cliente, Reparacion reparacion, TipoNotificacion tipo, String titulo, String mensaje) {
        this.cliente = cliente;
        this.reparacion = reparacion;
        this.tipo = tipo != null ? tipo : TipoNotificacion.INFO;
        this.titulo = titulo;
        this.mensaje = mensaje;
        this.fechaCreacion = LocalDateTime.now();
        this.leida = false;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Reparacion getReparacion() { return reparacion; }
    public void setReparacion(Reparacion reparacion) { this.reparacion = reparacion; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public TipoNotificacion getTipo() { return tipo; }
    public void setTipo(TipoNotificacion tipo) { this.tipo = tipo; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }

    // Campos auxiliares para facilitar consumo en frontend
    @Transient
    public String getLocalizador() {
        return reparacion != null ? reparacion.getLocalizador() : null;
    }

    @Transient
    public String getMatriculaVehiculo() {
        return (reparacion != null && reparacion.getVehiculo() != null) ? reparacion.getVehiculo().getMatricula() : null;
    }
}
