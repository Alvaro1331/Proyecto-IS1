package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un Presupuesto Desglosado emitido tras el diagnóstico técnico (Historias 1 y 3).
 *
 * Estados posibles:
 * - "PENDIENTE_ACEPTACION": Enviado al cliente, a la espera de que lo acepte o rechace con su contraseña.
 * - "ACEPTADO": Confirmado por el cliente (la reparación pasa a lista para reparar / EN_PROCESO).
 * - "RECHAZADO": Rechazado por el cliente (con motivo opcional registrado).
 */
@Entity
@Table(name = "presupuestos_reparacion")
public class PresupuestoReparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String numeroPresupuesto;

    @OneToOne
    @JoinColumn(name = "reparacion_id", nullable = false, unique = true)
    @JsonIgnore
    private Reparacion reparacion;

    @Column(length = 1000)
    private String diagnosticoPrevio;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private LocalDate fechaValidez;

    // "PENDIENTE_ACEPTACION", "ACEPTADO", "RECHAZADO"
    @Column(nullable = false, length = 35)
    private String estado = "PENDIENTE_ACEPTACION";

    @Column(length = 500)
    private String motivoRechazo;

    private LocalDateTime fechaRespuestaCliente;

    @Column(nullable = false)
    private Double subtotalPiezas = 0.0;

    @Column(nullable = false)
    private Double subtotalManoObra = 0.0;

    @Column(nullable = false)
    private Double subtotalBase = 0.0;

    @Column(nullable = false)
    private Double porcentajeMargen = 15.0;

    @Column(nullable = false)
    private Double importeMargen = 0.0;

    @Column(nullable = false)
    private Double baseImponible = 0.0;

    @Column(nullable = false)
    private Double porcentajeIva = 21.0;

    @Column(nullable = false)
    private Double importeIva = 0.0;

    @Column(nullable = false)
    private Double totalPresupuesto = 0.0;

    @OneToMany(mappedBy = "presupuesto", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<LineaPresupuesto> lineas = new ArrayList<>();

    public PresupuestoReparacion() {}

    public PresupuestoReparacion(String numeroPresupuesto, Reparacion reparacion, String diagnosticoPrevio,
                                 LocalDate fechaValidez, Double porcentajeMargen, Double porcentajeIva) {
        this.numeroPresupuesto = numeroPresupuesto;
        this.reparacion = reparacion;
        this.diagnosticoPrevio = diagnosticoPrevio;
        this.fechaEmision = LocalDate.now();
        this.fechaValidez = fechaValidez != null ? fechaValidez : LocalDate.now().plusDays(15);
        this.estado = "PENDIENTE_ACEPTACION";
        this.porcentajeMargen = porcentajeMargen != null ? porcentajeMargen : 15.0;
        this.porcentajeIva = porcentajeIva != null ? porcentajeIva : 21.0;
    }

    public void recalcularTotales() {
        double sumaPiezas = 0.0;
        double sumaManoObra = 0.0;
        for (LineaPresupuesto l : this.lineas) {
            double sub = redondear((l.getCantidad() != null ? l.getCantidad() : 0.0)
                    * (l.getPrecioUnitario() != null ? l.getPrecioUnitario() : 0.0));
            l.setSubtotal(sub);
            if ("MANO_OBRA".equalsIgnoreCase(l.getTipo())) {
                sumaManoObra += sub;
            } else {
                sumaPiezas += sub;
            }
        }
        this.subtotalPiezas = redondear(sumaPiezas);
        this.subtotalManoObra = redondear(sumaManoObra);
        this.subtotalBase = redondear(this.subtotalPiezas + this.subtotalManoObra);
        double pctMargen = this.porcentajeMargen != null ? this.porcentajeMargen : 15.0;
        this.importeMargen = redondear(this.subtotalBase * (pctMargen / 100.0));
        this.baseImponible = redondear(this.subtotalBase + this.importeMargen);
        double pctIva = this.porcentajeIva != null ? this.porcentajeIva : 21.0;
        this.importeIva = redondear(this.baseImponible * (pctIva / 100.0));
        this.totalPresupuesto = redondear(this.baseImponible + this.importeIva);
    }

    public boolean estaCaducado() {
        return fechaValidez != null && LocalDate.now().isAfter(fechaValidez)
                && "PENDIENTE_ACEPTACION".equalsIgnoreCase(estado);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPresupuesto() { return numeroPresupuesto; }
    public void setNumeroPresupuesto(String numeroPresupuesto) { this.numeroPresupuesto = numeroPresupuesto; }

    public Reparacion getReparacion() { return reparacion; }
    public void setReparacion(Reparacion reparacion) { this.reparacion = reparacion; }

    public String getDiagnosticoPrevio() { return diagnosticoPrevio; }
    public void setDiagnosticoPrevio(String diagnosticoPrevio) { this.diagnosticoPrevio = diagnosticoPrevio; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public LocalDate getFechaValidez() { return fechaValidez; }
    public void setFechaValidez(LocalDate fechaValidez) { this.fechaValidez = fechaValidez; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public LocalDateTime getFechaRespuestaCliente() { return fechaRespuestaCliente; }
    public void setFechaRespuestaCliente(LocalDateTime fechaRespuestaCliente) { this.fechaRespuestaCliente = fechaRespuestaCliente; }

    public Double getSubtotalPiezas() { return subtotalPiezas; }
    public void setSubtotalPiezas(Double subtotalPiezas) { this.subtotalPiezas = subtotalPiezas; }

    public Double getSubtotalManoObra() { return subtotalManoObra; }
    public void setSubtotalManoObra(Double subtotalManoObra) { this.subtotalManoObra = subtotalManoObra; }

    public Double getSubtotalBase() { return subtotalBase; }
    public void setSubtotalBase(Double subtotalBase) { this.subtotalBase = subtotalBase; }

    public Double getPorcentajeMargen() { return porcentajeMargen; }
    public void setPorcentajeMargen(Double porcentajeMargen) { this.porcentajeMargen = porcentajeMargen; }

    public Double getImporteMargen() { return importeMargen; }
    public void setImporteMargen(Double importeMargen) { this.importeMargen = importeMargen; }

    public Double getBaseImponible() { return baseImponible; }
    public void setBaseImponible(Double baseImponible) { this.baseImponible = baseImponible; }

    public Double getPorcentajeIva() { return porcentajeIva; }
    public void setPorcentajeIva(Double porcentajeIva) { this.porcentajeIva = porcentajeIva; }

    public Double getImporteIva() { return importeIva; }
    public void setImporteIva(Double importeIva) { this.importeIva = importeIva; }

    public Double getTotalPresupuesto() { return totalPresupuesto; }
    public void setTotalPresupuesto(Double totalPresupuesto) { this.totalPresupuesto = totalPresupuesto; }

    public List<LineaPresupuesto> getLineas() { return lineas; }
    public void setLineas(List<LineaPresupuesto> lineas) { this.lineas = lineas; }
}
