package com.taller.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa un gasto de compra de stock o alta de nuevo material en la Tesorería del Taller.
 * Permite consultar el historial de gastos de inventario y eliminar compras registradas por error.
 */
@Entity
@Table(name = "gastos_tesoreria")
public class GastoTesoreria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long materialId;

    @Column(nullable = false, length = 150)
    private String nombreMaterial;

    @Column(nullable = false, length = 300)
    private String concepto;

    // "ALTA_MATERIAL" o "COMPRA_STOCK"
    @Column(nullable = false, length = 40)
    private String tipo = "COMPRA_STOCK";

    @Column(nullable = false)
    private Integer cantidad = 1;

    @Column(nullable = false)
    private Double precioUnitario = 0.0;

    @Column(nullable = false)
    private Double importe = 0.0;

    private Integer stockPrevio = 0;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public GastoTesoreria() {}

    public GastoTesoreria(Long materialId, String nombreMaterial, String concepto, String tipo,
                          Integer cantidad, Double precioUnitario, Double importe, Integer stockPrevio) {
        this.materialId = materialId;
        this.nombreMaterial = nombreMaterial != null && !nombreMaterial.isBlank() ? nombreMaterial : "Material";
        this.concepto = concepto != null && !concepto.isBlank() ? concepto : "Compra de stock de materiales";
        this.tipo = tipo != null && !tipo.isBlank() ? tipo : "COMPRA_STOCK";
        this.cantidad = cantidad != null && cantidad > 0 ? cantidad : 1;
        this.precioUnitario = precioUnitario != null ? Math.round(precioUnitario * 100.0) / 100.0 : 0.0;
        this.importe = importe != null ? Math.round(importe * 100.0) / 100.0 : 0.0;
        this.stockPrevio = stockPrevio != null ? Math.max(0, stockPrevio) : 0;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getNombreMaterial() { return nombreMaterial; }
    public void setNombreMaterial(String nombreMaterial) { this.nombreMaterial = nombreMaterial; }

    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }

    public Double getImporte() { return importe; }
    public void setImporte(Double importe) { this.importe = importe; }

    public Integer getStockPrevio() { return stockPrevio; }
    public void setStockPrevio(Integer stockPrevio) { this.stockPrevio = stockPrevio; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
