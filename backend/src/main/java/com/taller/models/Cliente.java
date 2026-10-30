package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    private String password;

    // Datos fiscales del cliente para emisión de facturas oficiales (DNI/CIF y Dirección)
    @Column(length = 25)
    private String dniCif;

    @Column(length = 250)
    private String direccionFiscal;

    // Un cliente puede tener múltiples vehículos
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Vehiculo> vehiculos;

    // Reparaciones vinculadas a la cuenta del cliente para seguimiento
    @ManyToMany
    @JoinTable(
        name = "cliente_reparaciones_vinculadas",
        joinColumns = @JoinColumn(name = "cliente_id"),
        inverseJoinColumns = @JoinColumn(name = "reparacion_id")
    )
    @JsonIgnore
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

    public Cliente(String nombre, String telefono, String email, String password, String dniCif, String direccionFiscal) {
        this(nombre, telefono, email, password);
        this.dniCif = dniCif;
        this.direccionFiscal = direccionFiscal;
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

    public String getDniCif() { return dniCif; }
    public void setDniCif(String dniCif) { this.dniCif = dniCif; }

    public String getDireccionFiscal() { return direccionFiscal; }
    public void setDireccionFiscal(String direccionFiscal) { this.direccionFiscal = direccionFiscal; }

    public List<Vehiculo> getVehiculos() { return vehiculos; }
    public void setVehiculos(List<Vehiculo> vehiculos) { this.vehiculos = vehiculos; }

    public Set<Reparacion> getReparacionesVinculadas() { return reparacionesVinculadas; }
    public void setReparacionesVinculadas(Set<Reparacion> reparacionesVinculadas) { this.reparacionesVinculadas = reparacionesVinculadas; }
}
