package com.taller.models;

import jakarta.persistence.*;
import java.security.SecureRandom;
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

    private static final String CARACTERES_LOCALIZADOR = "ABCDEFGHJKMNPQRSTVWXYZ23456789";
    private static final int LONGITUD_LOCALIZADOR = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Código alfanumérico único para que el cliente consulte el estado de su reparación
    @Column(unique = true, nullable = false, length = 8)
    private String localizador;

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
        this.localizador = generarLocalizador();
        this.vehiculo = vehiculo;
        this.recepcionista = recepcionista;
        this.descripcionAveria = descripcionAveria;
        this.fechaEntrada = LocalDate.now();
        this.estado = EstadoReparacion.PENDIENTE;
    }

    /**
     * Genera un código localizador aleatorio de 8 caracteres alfanuméricos.
     * Excluye caracteres confusos como O, 0, I, 1, L.
     */
    public static String generarLocalizador() {
        StringBuilder sb = new StringBuilder(LONGITUD_LOCALIZADOR);
        for (int i = 0; i < LONGITUD_LOCALIZADOR; i++) {
            int index = RANDOM.nextInt(CARACTERES_LOCALIZADOR.length());
            sb.append(CARACTERES_LOCALIZADOR.charAt(index));
        }
        return sb.toString();
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLocalizador() { return localizador; }
    public void setLocalizador(String localizador) { this.localizador = localizador; }

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
