package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Representa una Factura formal emitida para una reparación terminada.
 * Desglosa:
 * - Coste de materiales (piezas empleadas)
 * - Mano de obra (horasManoObra * precioHora)
 * - Subtotal base (materiales + mano de obra)
 * - Margen de beneficio del taller (% sobre subtotalBase)
 * - Base imponible (subtotalBase + importeMargen)
 * - IVA (21% sobre baseImponible)
 * - Total a pagar (baseImponible + importeIva)
 */
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30)
    private String numeroFactura;

    @OneToOne
    @JoinColumn(name = "reparacion_id", nullable = false, unique = true)
    @JsonIgnore
    private Reparacion reparacion;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    @Column(nullable = false)
    private Double costeMateriales = 0.0;

    @Column(nullable = false)
    private Double horasManoObra = 0.0;

    @Column(nullable = false)
    private Double precioHora = 35.0;

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
    private Double totalFactura = 0.0;

    @Column(length = 25)
    private String dniCifCliente;

    @Column(length = 250)
    private String direccionFiscalCliente;

    public Factura() {}

    public Factura(String numeroFactura, Reparacion reparacion, Double costeMateriales,
                   Double horasManoObra, Double precioHora, Double porcentajeMargen, Double porcentajeIva) {
        this.numeroFactura = numeroFactura;
        this.reparacion = reparacion;
        this.fechaEmision = LocalDate.now();
        recalcularImportes(costeMateriales, horasManoObra, precioHora, porcentajeMargen, porcentajeIva);
    }

    public void recalcularImportes(Double costeMateriales, Double horasManoObra, Double precioHora,
                                   Double porcentajeMargen, Double porcentajeIva) {
        this.costeMateriales = redondear(costeMateriales != null ? costeMateriales : 0.0);
        this.horasManoObra = redondear(horasManoObra != null ? horasManoObra : 0.0);
        this.precioHora = redondear(precioHora != null ? precioHora : 35.0);
        this.subtotalManoObra = redondear(this.horasManoObra * this.precioHora);
        this.subtotalBase = redondear(this.costeMateriales + this.subtotalManoObra);
        this.porcentajeMargen = redondear(porcentajeMargen != null ? porcentajeMargen : 15.0);
        this.importeMargen = redondear(this.subtotalBase * (this.porcentajeMargen / 100.0));
        this.baseImponible = redondear(this.subtotalBase + this.importeMargen);
        this.porcentajeIva = redondear(porcentajeIva != null ? porcentajeIva : 21.0);
        this.importeIva = redondear(this.baseImponible * (this.porcentajeIva / 100.0));
        this.totalFactura = redondear(this.baseImponible + this.importeIva);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroFactura() { return numeroFactura; }
    public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }

    public Reparacion getReparacion() { return reparacion; }
    public void setReparacion(Reparacion reparacion) { this.reparacion = reparacion; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public Double getCosteMateriales() { return costeMateriales; }
    public void setCosteMateriales(Double costeMateriales) { this.costeMateriales = costeMateriales; }

    public Double getHorasManoObra() { return horasManoObra; }
    public void setHorasManoObra(Double horasManoObra) { this.horasManoObra = horasManoObra; }

    public Double getPrecioHora() { return precioHora; }
    public void setPrecioHora(Double precioHora) { this.precioHora = precioHora; }

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

    public Double getTotalFactura() { return totalFactura; }
    public void setTotalFactura(Double totalFactura) { this.totalFactura = totalFactura; }

    public String getDniCifCliente() { return dniCifCliente; }
    public void setDniCifCliente(String dniCifCliente) { this.dniCifCliente = dniCifCliente; }

    public String getDireccionFiscalCliente() { return direccionFiscalCliente; }
    public void setDireccionFiscalCliente(String direccionFiscalCliente) { this.direccionFiscalCliente = direccionFiscalCliente; }
}
