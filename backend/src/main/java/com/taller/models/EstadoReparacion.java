package com.taller.models;

/**
 * Define los estados posibles de una reparación.
 *
 * Flujo normal de una reparación:
 * PENDIENTE → EN_PROCESO → FINALIZADA → CONFIRMADA → ENTREGADA
 *
 * - PENDIENTE: El coche ha sido registrado por recepción pero ningún mecánico ha empezado a trabajar.
 * - EN_PROCESO: Un mecánico está trabajando en la reparación.
 * - FINALIZADA: El mecánico ha terminado la reparación y está pendiente de que el Administrador la confirme.
 * - CONFIRMADA: El Administrador ha verificado y confirmado que la reparación es correcta; recepción ya puede entregar y cobrar.
 * - ENTREGADA: La recepcionista ha entregado el coche al cliente y ha cobrado la reparación.
 */
public enum EstadoReparacion {
    PENDIENTE,
    EN_PROCESO,
    FINALIZADA,
    CONFIRMADA,
    ENTREGADA
}
