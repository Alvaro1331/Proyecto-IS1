package com.taller.dtos;

/**
 * DTO de respuesta con los datos de un material del inventario.
 */
public class MaterialResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private Integer stockDisponible;
    private Double precioUnitario;
    private String mensaje;

    public MaterialResponseDTO(Long id, String nombre, String descripcion,
                                Integer stockDisponible, Double precioUnitario, String mensaje) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.stockDisponible = stockDisponible;
        this.precioUnitario = precioUnitario;
        this.mensaje = mensaje;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Integer getStockDisponible() { return stockDisponible; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public String getMensaje() { return mensaje; }
}
