package com.taller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear una nueva reparación.
 *
 * Ejemplo de JSON:
 * {
 *   "vehiculoId": 1,
 *   "recepcionistaId": 2,
 *   "descripcionAveria": "El motor hace un ruido extraño al acelerar"
 * }
 *
 * Nota: El mecánico se asigna después (no hace falta al crear la reparación).
 */
public class ReparacionRequestDTO {

    @NotNull(message = "El ID del vehículo es obligatorio")
    private Long vehiculoId;

    @NotNull(message = "El ID del recepcionista es obligatorio")
    private Long recepcionistaId;

    @NotBlank(message = "La descripción de la avería no puede estar vacía")
    private String descripcionAveria;

    // --- Getters y Setters ---
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public Long getRecepcionistaId() { return recepcionistaId; }
    public void setRecepcionistaId(Long recepcionistaId) { this.recepcionistaId = recepcionistaId; }

    public String getDescripcionAveria() { return descripcionAveria; }
    public void setDescripcionAveria(String descripcionAveria) { this.descripcionAveria = descripcionAveria; }
}
