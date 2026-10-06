package com.taller.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear o actualizar un material del inventario.
 *
 * Ejemplo de JSON:
 * {
 *   "nombre": "Filtro de aceite",
 *   "descripcion": "Filtro compatible con motores gasolina 1.6",
 *   "stockDisponible": 25,
 *   "precioUnitario": 12.50
 * }
 */
public class MaterialRequestDTO {

    @NotBlank(message = "El nombre del material no puede estar vacío")
    private String nombre;

    private String descripcion;

    @NotNull(message = "El stock no puede estar vacío")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockDisponible;

    @NotNull(message = "El precio no puede estar vacío")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Double precioUnitario;

    // --- Getters y Setters ---
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(Integer stockDisponible) { this.stockDisponible = stockDisponible; }

    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
}
