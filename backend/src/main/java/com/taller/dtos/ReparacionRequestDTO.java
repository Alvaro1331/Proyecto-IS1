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
 *   "descripcionAveria": "El motor hace un ruido extraño al acelerar",
 *   "presupuesto": 200.00,
 *   "presupuestoEstimado": 100.00
 * }
 */
public class ReparacionRequestDTO {

    @NotNull(message = "El ID del vehículo es obligatorio")
    private Long vehiculoId;

    @NotNull(message = "El ID del recepcionista es obligatorio")
    private Long recepcionistaId;

    @NotBlank(message = "La descripción de la avería no puede estar vacía")
    private String descripcionAveria;

    // Precio acordado con el cliente (ej. 200 €)
    private Double presupuesto;

    // Presupuesto estimado por la recepcionista para arreglarlo (ej. 100 €)
    private Double presupuestoEstimado;

    // Presupuesto máximo aprobado por el administrador para el mecánico
    private Double presupuestoMecanico;

    // --- Getters y Setters ---
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public Long getRecepcionistaId() { return recepcionistaId; }
    public void setRecepcionistaId(Long recepcionistaId) { this.recepcionistaId = recepcionistaId; }

    public String getDescripcionAveria() { return descripcionAveria; }
    public void setDescripcionAveria(String descripcionAveria) { this.descripcionAveria = descripcionAveria; }

    public Double getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Double presupuesto) { this.presupuesto = presupuesto; }

    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(Double presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public void setPresupuestoMecanico(Double presupuestoMecanico) { this.presupuestoMecanico = presupuestoMecanico; }
}
