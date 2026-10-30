package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Representa una línea de coste dentro de un Presupuesto Desglosado (Historia 1).
 * Puede ser de tipo "PIEZA" (Pieza/Repuesto) o "MANO_OBRA" (Mano de obra).
 */
@Entity
@Table(name = "lineas_presupuesto")
public class LineaPresupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "presupuesto_id", nullable = false)
    @JsonIgnore
    private PresupuestoReparacion presupuesto;

    // "PIEZA" o "MANO_OBRA"
    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 250)
    private String concepto;

    // Cantidad de piezas o número de horas de mano de obra
    @Column(nullable = false)
    private Double cantidad = 1.0;

    // Precio unitario de la pieza o tarifa €/hora de mano de obra
    @Column(nullable = false)
    private Double precioUnitario = 0.0;

    @Column(nullable = false)
    private Double subtotal = 0.0;

    // ID opcional si corresponde a un material del inventario
    @Column
    private Long materialId;

    public LineaPresupuesto() {}

    public LineaPresupuesto(PresupuestoReparacion presupuesto, String tipo, String concepto,
                            Double cantidad, Double precioUnitario, Long materialId) {
        this.presupuesto = presupuesto;
        this.tipo = (tipo != null && tipo.equalsIgnoreCase("MANO_OBRA")) ? "MANO_OBRA" : "PIEZA";
        this.concepto = concepto;
        this.cantidad = redondear(cantidad != null ? cantidad : 1.0);
        this.precioUnitario = redondear(precioUnitario != null ? precioUnitario : 0.0);
        this.subtotal = redondear(this.cantidad * this.precioUnitario);
        this.materialId = materialId;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PresupuestoReparacion getPresupuesto() { return presupuesto; }
    public void setPresupuesto(PresupuestoReparacion presupuesto) { this.presupuesto = presupuesto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public Double getCantidad() { return cantidad; }
    public void setCantidad(Double cantidad) { this.cantidad = cantidad; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
}
