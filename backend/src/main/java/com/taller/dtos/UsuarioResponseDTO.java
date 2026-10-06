package com.taller.dtos;

/**
 * DTO de respuesta al crear o consultar un usuario.
 * Nunca devolvemos la contraseña al frontend.
 */
public class UsuarioResponseDTO {

    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private String rol;
    private String mensaje;

    public UsuarioResponseDTO(Long id, String nombre, String email, String telefono, String rol, String mensaje) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.telefono = telefono;
        this.rol = rol;
        this.mensaje = mensaje;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getRol() { return rol; }
    public String getMensaje() { return mensaje; }
}
