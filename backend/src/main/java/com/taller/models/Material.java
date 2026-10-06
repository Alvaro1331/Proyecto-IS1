package com.taller.models;

import jakarta.persistence.*;

/**
 * Representa un material o pieza del inventario del taller.
 * Ejemplos: filtro de aceite, pastillas de freno, líquido refrigerante, etc.
 *
 * El stock se actualiza automáticamente cuando un mecánico registra
 * materiales usados en una reparación.
 */
@Entity
@Table(name = "materiales")
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private Double precioUnitario;

    // --- Constructores ---
    public Material() {}

    public Material(String nombre, String descripcion, Integer stockDisponible, Double precioUnitario) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stockDisponible = stockDisponible;
        this.precioUnitario = precioUnitario;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(Integer stockDisponible) { this.stockDisponible = stockDisponible; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
}
