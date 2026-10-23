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
 * 1. El recepcionista registra una reparación en estado PENDIENTE con:
 *    - precio acordado con el cliente (presupuesto)
 *    - presupuesto estimado pensado para el arreglo (presupuestoEstimado)
 * 2. El administrador decide el presupuesto real para el mecánico (presupuestoMecanico) y asigna mecánico → EN_PROCESO.
 * 3. El mecánico usa materiales sin superar su presupuestoMecanico y termina → FINALIZADA.
 * 4. El administrador verifica y confirma que la reparación es correcta → CONFIRMADA.
 * 5. La recepcionista entrega el coche al cliente y cobra → ENTREGADA.
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

    // Precio total acordado a cobrar al cliente (ej. 200 €)
    @Column(nullable = false)
    private Double presupuesto = 200.0;

    // Presupuesto estimado por la recepcionista para arreglar el vehículo (ej. 100 €)
    @Column(nullable = false)
    private Double presupuestoEstimado = 100.0;

    // Presupuesto máximo real aprobado por el Administrador que el mecánico puede gastar en piezas (ej. 100 €, 150 € o 75 €)
    @Column(nullable = false)
    private Double presupuestoMecanico = 100.0;

    // Materiales usados en esta reparación
    @OneToMany(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private List<ReparacionMaterial> materialesUsados;

    // --- Constructores ---
    public Reparacion() {}

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria) {
        this(vehiculo, recepcionista, descripcionAveria, 200.0, 100.0, 100.0);
    }

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria, Double presupuesto) {
        this(vehiculo, recepcionista, descripcionAveria, presupuesto,
                presupuesto != null ? Math.round(presupuesto * 50.0) / 100.0 : 100.0,
                presupuesto != null ? Math.round(presupuesto * 50.0) / 100.0 : 100.0);
    }

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria,
                      Double presupuesto, Double presupuestoEstimado, Double presupuestoMecanico) {
        this.localizador = generarLocalizador();
        this.vehiculo = vehiculo;
        this.recepcionista = recepcionista;
        this.descripcionAveria = descripcionAveria;
        this.fechaEntrada = LocalDate.now();
        this.estado = EstadoReparacion.PENDIENTE;
        double precioCli = (presupuesto != null && presupuesto > 0) ? presupuesto : 200.0;
        double estRecep = (presupuestoEstimado != null && presupuestoEstimado > 0) ? presupuestoEstimado : Math.round(precioCli * 50.0) / 100.0;
        double presMec = (presupuestoMecanico != null && presupuestoMecanico > 0) ? presupuestoMecanico : estRecep;
        this.presupuesto = precioCli;
        this.presupuestoEstimado = estRecep;
        this.presupuestoMecanico = presMec;
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

    public Double getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Double presupuesto) { this.presupuesto = presupuesto; }

    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(Double presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public void setPresupuestoMecanico(Double presupuestoMecanico) { this.presupuestoMecanico = presupuestoMecanico; }

    public List<ReparacionMaterial> getMaterialesUsados() { return materialesUsados; }
    public void setMaterialesUsados(List<ReparacionMaterial> materialesUsados) { this.materialesUsados = materialesUsados; }
}
