package com.taller.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para crear un nuevo usuario del sistema.
 *
 * Ejemplo de JSON:
 * {
 *   "nombre": "Carlos López",
 *   "email": "carlos@taller.com",
 *   "password": "miContraseña123",
 *   "telefono": "600111222",
 *   "rol": "MECANICO"
 * }
 */
public class UsuarioRequestDTO {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    private String telefono;

    @NotNull(message = "El rol no puede estar vacío")
    private String rol;

    // --- Getters y Setters ---
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
