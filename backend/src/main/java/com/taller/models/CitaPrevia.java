package com.taller.models;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una Cita Previa agendada en el taller (Historias 4 y 5).
 *
 * Estados posibles:
 * - "PENDIENTE": Cita solicitada pendiente de confirmación manual
 * - "CONFIRMADA": Cita confirmada y con plaza reservada en el taller
 * - "RECEPCIONADA": El cliente ya ha entregado el vehículo en recepción
 * - "CANCELADA": Cita anulada (libera cupo de capacidad)
 */
@Entity
@Table(name = "citas_previas")
public class CitaPrevia {

    private static final String CARACTERES_CODIGO = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15)
    private String codigoCita;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 10)
    private String franjaHoraria;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(length = 500)
    private String observaciones;

    // "PENDIENTE", "CONFIRMADA", "RECEPCIONADA", "CANCELADA"
    @Column(nullable = false, length = 25)
    private String estado = "CONFIRMADA";

    private Long reparacionId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public CitaPrevia() {}

    public CitaPrevia(Cliente cliente, Vehiculo vehiculo, LocalDate fecha,
                      String franjaHoraria, String motivo, String observaciones, String estado) {
        this.codigoCita = generarCodigoCita();
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.fecha = fecha;
        this.franjaHoraria = franjaHoraria;
        this.motivo = motivo;
        this.observaciones = observaciones;
        this.estado = (estado != null && !estado.isBlank()) ? estado.toUpperCase() : "CONFIRMADA";
        this.fechaCreacion = LocalDateTime.now();
    }

    public static String generarCodigoCita() {
        StringBuilder sb = new StringBuilder("CITA-");
        for (int i = 0; i < 4; i++) {
            sb.append(CARACTERES_CODIGO.charAt(RANDOM.nextInt(CARACTERES_CODIGO.length())));
        }
        return sb.toString();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoCita() { return codigoCita; }
    public void setCodigoCita(String codigoCita) { this.codigoCita = codigoCita; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getFranjaHoraria() { return franjaHoraria; }
    public void setFranjaHoraria(String franjaHoraria) { this.franjaHoraria = franjaHoraria; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getReparacionId() { return reparacionId; }
    public void setReparacionId(Long reparacionId) { this.reparacionId = reparacionId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
