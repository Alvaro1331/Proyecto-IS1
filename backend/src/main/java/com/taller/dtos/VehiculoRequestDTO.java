package com.taller.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO (Data Transfer Object) para registrar un vehículo.
 * Este es el objeto que el Frontend debe enviar en el cuerpo (body) de la petición.
 *
 * Ejemplo de JSON que debe enviar el frontend:
 * {
 *   "matricula": "1234ABC",
 *   "marca": "Toyota",
 *   "modelo": "Corolla",
 *   "ano": 2020,
 *   "clienteNombre": "Juan García",
 *   "clienteTelefono": "600123456"
 * }
 */
public class VehiculoRequestDTO {

    @NotBlank(message = "La matrícula no puede estar vacía")
    private String matricula;

    @NotBlank(message = "La marca no puede estar vacía")
    private String marca;

    @NotBlank(message = "El modelo no puede estar vacío")
    private String modelo;

    @NotNull(message = "El año no puede estar vacío")
    @Min(value = 1900, message = "El año debe ser posterior a 1900")
    private Integer ano;

    @NotBlank(message = "El nombre del cliente no puede estar vacío")
    private String clienteNombre;

    @NotBlank(message = "El teléfono del cliente no puede estar vacío")
    private String clienteTelefono;

    // --- Getters y Setters ---
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteTelefono() { return clienteTelefono; }
    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }
}
