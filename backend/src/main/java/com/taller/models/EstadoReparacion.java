package com.taller.models;

/**
 * Define los estados posibles de una reparación.
 *
 * Flujo normal de una reparación:
 * PENDIENTE → EN_PROCESO → FINALIZADA → ENTREGADA
 *
 * - PENDIENTE: El coche ha sido registrado pero ningún mecánico ha empezado a trabajar.
 * - EN_PROCESO: Un mecánico está trabajando en la reparación.
 * - FINALIZADA: La reparación ha terminado pero el coche sigue en el taller.
 * - ENTREGADA: El cliente ha recogido el coche.
 */
public enum EstadoReparacion {
    PENDIENTE,
    EN_PROCESO,
    FINALIZADA,
    ENTREGADA
}
