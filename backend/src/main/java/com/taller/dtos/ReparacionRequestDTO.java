package com.taller.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear una nueva reparación desde Recepción.
 *
 * Ejemplo de JSON:
 * {
 *   "vehiculoId": 1,
 *   "recepcionistaId": 2,
 *   "descripcionAveria": "El motor hace un ruido extraño al acelerar",
 *   "estimacionInicial": 180.00,
 *   "diasEstimados": 3
 * }
 */
public class ReparacionRequestDTO {

    @NotNull(message = "El ID del vehículo es obligatorio")
    private Long vehiculoId;

    @NotNull(message = "El ID del recepcionista es obligatorio")
    private Long recepcionistaId;

    @NotBlank(message = "La descripción de la avería no puede estar vacía")
    private String descripcionAveria;

    // Estimación inicial orientativa dada al cliente (ej. 180 €)
    private Double estimacionInicial;

    // Alias compatible con formularios anteriores
    private Double presupuesto;

    // Días estimados para completar la reparación (ej. 3 días)
    private Integer diasEstimados;

    // Campos mantenidos por compatibilidad
    private Double presupuestoEstimado;
    private Double presupuestoMecanico;

    // --- Getters y Setters ---
    public Long getVehiculoId() { return vehiculoId; }
    public void setVehiculoId(Long vehiculoId) { this.vehiculoId = vehiculoId; }

    public Long getRecepcionistaId() { return recepcionistaId; }
    public void setRecepcionistaId(Long recepcionistaId) { this.recepcionistaId = recepcionistaId; }

    public String getDescripcionAveria() { return descripcionAveria; }
    public void setDescripcionAveria(String descripcionAveria) { this.descripcionAveria = descripcionAveria; }

    public Double getEstimacionInicial() {
        return estimacionInicial != null ? estimacionInicial : presupuesto;
    }
    public void setEstimacionInicial(Double estimacionInicial) { this.estimacionInicial = estimacionInicial; }

    public Double getPresupuesto() {
        return presupuesto != null ? presupuesto : estimacionInicial;
    }
    public void setPresupuesto(Double presupuesto) { this.presupuesto = presupuesto; }

    public Integer getDiasEstimados() { return diasEstimados; }
    public void setDiasEstimados(Integer diasEstimados) { this.diasEstimados = diasEstimados; }

    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(Double presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public void setPresupuestoMecanico(Double presupuestoMecanico) { this.presupuestoMecanico = presupuestoMecanico; }
}
