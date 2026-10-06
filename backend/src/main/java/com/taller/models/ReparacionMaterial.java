package com.taller.models;

import jakarta.persistence.*;

/**
 * Tabla intermedia entre Reparación y Material.
 * Registra cuántas unidades de un material concreto se han gastado
 * en una reparación específica.
 *
 * Ejemplo: En la reparación #5 se usaron 2 unidades del material "Filtro de aceite".
 */
@Entity
@Table(name = "reparacion_materiales")
public class ReparacionMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reparacion_id", nullable = false)
    private Reparacion reparacion;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(nullable = false)
    private Integer cantidad;

    // --- Constructores ---
    public ReparacionMaterial() {}

    public ReparacionMaterial(Reparacion reparacion, Material material, Integer cantidad) {
        this.reparacion = reparacion;
        this.material = material;
        this.cantidad = cantidad;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reparacion getReparacion() { return reparacion; }
    public void setReparacion(Reparacion reparacion) { this.reparacion = reparacion; }

    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
