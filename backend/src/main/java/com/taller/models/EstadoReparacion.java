package com.taller.models;

/**
 * Define los estados posibles de una reparación.
 *
 * Flujo normal de una reparación:
 * PENDIENTE → EN_PROCESO → FINALIZADA → CONFIRMADA → ENTREGADA
 *
 * También puede pasar a CANCELADA desde PENDIENTE o EN_PROCESO si el cliente
 * rechaza el presupuesto y decide retirar el vehículo sin reparar.
 *
 * - PENDIENTE: El coche ha sido registrado por recepción pero ningún mecánico ha empezado a trabajar.
 * - EN_PROCESO: Un mecánico está trabajando en la reparación.
 * - FINALIZADA: El mecánico ha terminado la reparación y está pendiente de que el Administrador la confirme.
 * - CONFIRMADA: El Administrador ha verificado y confirmado que la reparación es correcta; recepción ya puede entregar y cobrar.
 * - ENTREGADA: La recepcionista ha entregado el coche al cliente y ha cobrado la reparación.
 * - CANCELADA: El cliente rechazó el presupuesto y retiró el vehículo sin reparar.
 */
public enum EstadoReparacion {
    PENDIENTE,
    EN_PROCESO,
    FINALIZADA,
    CONFIRMADA,
    ENTREGADA,
    CANCELADA
}
