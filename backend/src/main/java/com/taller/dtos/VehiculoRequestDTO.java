package com.taller.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO (Data Transfer Object) para registrar un vehículo.
 * Soporta tanto formato plano como formato con objeto anidado para mayor flexibilidad:
 *
 * Formato plano recomendado:
 * {
 *   "matricula": "1234ABC",
 *   "marca": "Toyota",
 *   "modelo": "Corolla",
 *   "ano": 2020,
 *   "clienteNombre": "Juan García",
 *   "clienteTelefono": "600123456"
 * }
 *
 * Formato alternativo con objeto anidado:
 * {
 *   "matricula": "1234ABC",
 *   "marca": "Toyota",
 *   "modelo": "Corolla",
 *   "anio": 2020,
 *   "cliente": { "nombre": "Juan García", "telefono": "600123456" }
 * }
 */
public class VehiculoRequestDTO {

    @NotBlank(message = "La matrícula no puede estar vacía")
    private String matricula;

    @NotBlank(message = "La marca no puede estar vacía")
    private String marca;

    @NotBlank(message = "El modelo no puede estar vacío")
    private String modelo;

    private Integer ano;
    private Integer anio;

    private String clienteNombre;
    private String clienteTelefono;

    private ClienteNestedDTO cliente;

    public static class ClienteNestedDTO {
        private String nombre;
        private String telefono;

        public String getNombre() { return nombre; }
        public void setNombre(String nombre) { this.nombre = nombre; }
        public String getTelefono() { return telefono; }
        public void setTelefono(String telefono) { this.telefono = telefono; }
    }

    // --- Getters y Setters ---
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAno() { return ano != null ? ano : anio; }
    public void setAno(Integer ano) { this.ano = ano; }

    public Integer getAnio() { return anio != null ? anio : ano; }
    public void setAnio(Integer anio) { this.anio = anio; }

    public String getClienteNombre() {
        if (clienteNombre != null && !clienteNombre.isBlank()) return clienteNombre;
        if (cliente != null && cliente.getNombre() != null) return cliente.getNombre();
        return null;
    }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteTelefono() {
        if (clienteTelefono != null && !clienteTelefono.isBlank()) return clienteTelefono;
        if (cliente != null && cliente.getTelefono() != null) return cliente.getTelefono();
        return null;
    }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public ClienteNestedDTO getCliente() { return cliente; }
    public void setCliente(ClienteNestedDTO cliente) { this.cliente = cliente; }
}
