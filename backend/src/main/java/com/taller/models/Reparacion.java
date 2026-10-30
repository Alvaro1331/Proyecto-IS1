package com.taller.models;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una reparación en el taller.
 * Es la entidad central del negocio: conecta un vehículo (y su cliente)
 * con el mecánico que lo repara y el recepcionista que lo registró.
 *
 * Nuevo flujo de negocio:
 * 1. Recepción crea la reparación en estado PENDIENTE con una estimación inicial orientativa (presupuesto / estimacionInicial)
 *    y los días estimados de trabajo (diasEstimados -> fechaEstimadaFin).
 * 2. El mecánico trabaja sin límite presupuestario artificial, registra las piezas usadas, anota sus horas reales
 *    de trabajo (horasTrabajoMecanico), puede ajustar los días estimados y subir incidencias con fotos.
 * 3. El cliente visualiza una línea temporal interactiva con las incidencias, fotos y días estimados.
 * 4. Al finalizar la reparación, Recepción formaliza la Factura real (materiales + mano de obra + margen % + IVA 21%),
 *    que queda registrada en el Archivo de Facturas y alimenta la tesorería real del taller.
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

    // Puede ser nula hasta que se complete
    private LocalDate fechaSalida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReparacion estado;

    @Column(nullable = false)
    private String descripcionAveria;

    // Estimación inicial orientativa dada al cliente en Recepción (en euros, no vinculante)
    @Column(nullable = false)
    private Double presupuesto = 150.0;

    // Mantenidos por compatibilidad con esquema MySQL existente (sin efecto bloqueante)
    @Column(nullable = false)
    private Double presupuestoEstimado = 0.0;

    @Column(nullable = false)
    private Double presupuestoMecanico = 0.0;

    // Días estimados de duración de la reparación
    @Column
    private Integer diasEstimados = 3;

    // Fecha estimada de finalización (fechaEntrada + diasEstimados)
    @Column
    private LocalDate fechaEstimadaFin;

    // Horas reales de mano de obra trabajadas por el mecánico
    @Column
    private Double horasTrabajoMecanico = 0.0;

    // Materiales usados en esta reparación
    @OneToMany(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private List<ReparacionMaterial> materialesUsados = new ArrayList<>();

    // Incidencias y fotos subidas por el mecánico durante la reparación
    @OneToMany(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private List<IncidenciaReparacion> incidencias = new ArrayList<>();

    // Factura formal emitida para esta reparación
    @OneToOne(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private Factura factura;

    // Presupuesto desglosado tras diagnóstico (Historias 1 y 3)
    @OneToOne(mappedBy = "reparacion", cascade = CascadeType.ALL)
    private PresupuestoReparacion presupuestoFormal;

    // --- Constructores ---
    public Reparacion() {}

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria) {
        this(vehiculo, recepcionista, descripcionAveria, 150.0, 3);
    }

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria, Double estimacionInicial) {
        this(vehiculo, recepcionista, descripcionAveria, estimacionInicial, 3);
    }

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria,
                      Double estimacionInicial, Integer diasEstimados) {
        this.localizador = generarLocalizador();
        this.vehiculo = vehiculo;
        this.recepcionista = recepcionista;
        this.descripcionAveria = descripcionAveria;
        this.fechaEntrada = LocalDate.now();
        this.estado = EstadoReparacion.PENDIENTE;
        double est = (estimacionInicial != null && estimacionInicial > 0) ? estimacionInicial : 150.0;
        int dias = (diasEstimados != null && diasEstimados > 0) ? diasEstimados : 3;
        this.presupuesto = est;
        this.presupuestoEstimado = est;
        this.presupuestoMecanico = 0.0;
        this.diasEstimados = dias;
        this.fechaEstimadaFin = this.fechaEntrada.plusDays(dias);
        this.horasTrabajoMecanico = 0.0;
    }

    public Reparacion(Vehiculo vehiculo, Usuario recepcionista, String descripcionAveria,
                      Double presupuesto, Double presupuestoEstimado, Double presupuestoMecanico) {
        this(vehiculo, recepcionista, descripcionAveria, presupuesto, 3);
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

    public Double getEstimacionInicial() { return presupuesto; }
    public void setEstimacionInicial(Double estimacionInicial) { this.presupuesto = estimacionInicial; }

    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(Double presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public void setPresupuestoMecanico(Double presupuestoMecanico) { this.presupuestoMecanico = presupuestoMecanico; }

    public Integer getDiasEstimados() { return diasEstimados != null ? diasEstimados : 3; }
    public void setDiasEstimados(Integer diasEstimados) {
        this.diasEstimados = diasEstimados;
        if (this.fechaEntrada != null && diasEstimados != null && diasEstimados >= 0) {
            this.fechaEstimadaFin = this.fechaEntrada.plusDays(diasEstimados);
        }
    }

    public LocalDate getFechaEstimadaFin() {
        if (fechaEstimadaFin != null) {
            return fechaEstimadaFin;
        }
        if (fechaEntrada != null) {
            return fechaEntrada.plusDays(getDiasEstimados());
        }
        return LocalDate.now().plusDays(getDiasEstimados());
    }
    public void setFechaEstimadaFin(LocalDate fechaEstimadaFin) { this.fechaEstimadaFin = fechaEstimadaFin; }

    public Double getHorasTrabajoMecanico() { return horasTrabajoMecanico != null ? horasTrabajoMecanico : 0.0; }
    public void setHorasTrabajoMecanico(Double horasTrabajoMecanico) { this.horasTrabajoMecanico = horasTrabajoMecanico; }

    public List<ReparacionMaterial> getMaterialesUsados() { return materialesUsados; }
    public void setMaterialesUsados(List<ReparacionMaterial> materialesUsados) { this.materialesUsados = materialesUsados; }

    public List<IncidenciaReparacion> getIncidencias() { return incidencias; }
    public void setIncidencias(List<IncidenciaReparacion> incidencias) { this.incidencias = incidencias; }

    public Factura getFactura() { return factura; }
    public void setFactura(Factura factura) { this.factura = factura; }

    public PresupuestoReparacion getPresupuestoFormal() { return presupuestoFormal; }
    public void setPresupuestoFormal(PresupuestoReparacion presupuestoFormal) { this.presupuestoFormal = presupuestoFormal; }
}
