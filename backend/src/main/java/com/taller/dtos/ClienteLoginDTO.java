package com.taller.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para el inicio de sesión de un cliente.
 * El cliente puede identificarse con su email o su teléfono.
 */
public class ClienteLoginDTO {

    @NotBlank(message = "El email o teléfono es obligatorio")
    private String emailOrTelefono;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // --- Getters y Setters ---
    public String getEmailOrTelefono() { return emailOrTelefono; }
    public void setEmailOrTelefono(String emailOrTelefono) { this.emailOrTelefono = emailOrTelefono; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
