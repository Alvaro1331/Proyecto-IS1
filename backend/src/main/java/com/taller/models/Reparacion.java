package com.taller.models;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Representa una reparación en el taller.
 * Es la entidad central del negocio: conecta un vehículo (y su cliente)
 * con el mecánico que lo repara y el recepcionista que lo registró.
 *
 * Flujo de vida de una reparación:
 * 1. El recepcionista registra un coche → crea una Reparación en estado PENDIENTE.
 * 2. El jefe (o el sistema) asigna un mecánico → pasa a EN_PROCESO.
 * 3. El mecánico termina → pasa a FINALIZADA.
 * 4. El cliente recoge el coche → pasa a ENTREGADA.
 */
@Entity
@Table(name = "reparaciones")
public class Reparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El vehículo que se está reparando
    @ManyToOne
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    // El mecánico asignado (puede ser nulo si aún no se ha asignado)
    @ManyToOne
    @JoinColumn(name = "mecanico_id")
    private Usuario mecanico;

    // El recepcionista que registró la reparación
    @ManyToOne
    @JoinColumn(name = "recepcionista_id", nullable = false)
    private Usuario recepcionista;

    @Column(nullable = false)
    private LocalDate fechaEntrada;

    // Puede ser nula hasta que se estime o se complete
    private LocalDate fechaSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReparacion estado;

    @Column(nullable = false)
    private String descripcionAveria;

    // Materiales usados en esta reparación
    @OneToMany(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private List<ReparacionMaterial> materialesUsados;

    // --- Constructores ---
    public Reparacion() {}

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria) {
        this.vehiculo = vehiculo;
        this.recepcionista = recepcionista;
        this.descripcionAveria = descripcionAveria;
        this.fechaEntrada = LocalDate.now();
        this.estado = EstadoReparacion.PENDIENTE;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }

    public Usuario getMecanico() { return mecanico; }
    public void setMecanico(Usuario mecanico) { this.mecanico = mecanico; }

    public Usuario getRecepcionista() { return recepcionista; }
    public void setRecepcionista(Usuario recepcionista) { this.recepcionista = recepcionista; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public EstadoReparacion getEstado() { return estado; }
    public void setEstado(EstadoReparacion estado) { this.estado = estado; }

    public String getDescripcionAveria() { return descripcionAveria; }
    public void setDescripcionAveria(String descripcionAveria) { this.descripcionAveria = descripcionAveria; }

    public List<ReparacionMaterial> getMaterialesUsados() { return materialesUsados; }
    public void setMaterialesUsados(List<ReparacionMaterial> materialesUsados) { this.materialesUsados = materialesUsados; }
}
