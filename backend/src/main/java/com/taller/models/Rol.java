package com.taller.models;

/**
 * Define los roles de usuario del sistema.
 * Cada rol tiene acceso a diferentes funcionalidades:
 *
 * - ADMIN: Acceso total. Puede hacer todo lo que hacen los demás roles.
 * - RECEPCIONISTA: Registra vehículos, crea reparaciones, gestiona clientes.
 * - MECANICO: Actualiza el estado de las reparaciones y registra materiales usados.
 * - CLIENTE: Consulta el estado de sus vehículos (uso futuro, por ahora los clientes no tienen cuenta).
 */
public enum Rol {
    ADMIN,
    RECEPCIONISTA,
    MECANICO,
    CLIENTE
}
