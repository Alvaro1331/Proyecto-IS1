package com.taller.models;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa a un cliente del taller.
 * Un cliente NO necesita una cuenta para existir en el sistema.
 * El recepcionista lo crea al registrar un vehículo.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String telefono;

    // Campos opcionales para clientes con cuenta en el sistema
    @Column(unique = true)
    private String email;

    private String password;

    // Un cliente puede tener múltiples vehículos
    // mappedBy = "cliente" indica que el campo "cliente" en Vehiculo es el que gestiona la relación
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    private List<Vehiculo> vehiculos;

    // Reparaciones vinculadas a la cuenta del cliente para seguimiento
    @ManyToMany
    @JoinTable(
        name = "cliente_reparaciones_vinculadas",
        joinColumns = @JoinColumn(name = "cliente_id"),
        inverseJoinColumns = @JoinColumn(name = "reparacion_id")
    )
    private Set<Reparacion> reparacionesVinculadas = new HashSet<>();

    // --- Constructores ---
    public Cliente() {}

    public Cliente(String nombre, String telefono) {
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public Cliente(String nombre, String telefono, String email, String password) {
        this(nombre, telefono);
        this.email = email;
        this.password = password;
    }

    /**
     * Comprueba si el cliente tiene credenciales de acceso registradas.
     */
    public boolean tieneCuenta() {
        return email != null && password != null;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public List<Vehiculo> getVehiculos() { return vehiculos; }
    public void setVehiculos(List<Vehiculo> vehiculos) { this.vehiculos = vehiculos; }

    public Set<Reparacion> getReparacionesVinculadas() { return reparacionesVinculadas; }
    public void setReparacionesVinculadas(Set<Reparacion> reparacionesVinculadas) { this.reparacionesVinculadas = reparacionesVinculadas; }
}
