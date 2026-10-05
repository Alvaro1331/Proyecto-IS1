package com.taller.dtos;

/**
 * DTO de respuesta que el Backend devuelve al Frontend tras registrar un vehículo.
 * Nunca devolvemos el objeto interno directamente para no exponer datos innecesarios.
 */
public class VehiculoResponseDTO {

    private Long id;
    private String matricula;
    private String marca;
    private String modelo;
    private Integer ano;
    private String clienteNombre;
    private String clienteTelefono;
    private String mensaje;

    // Constructor completo
    public VehiculoResponseDTO(Long id, String matricula, String marca, String modelo,
                                Integer ano, String clienteNombre, String clienteTelefono, String mensaje) {
        this.id = id;
        this.matricula = matricula;
        this.marca = marca;
        this.modelo = modelo;
        this.ano = ano;
        this.clienteNombre = clienteNombre;
        this.clienteTelefono = clienteTelefono;
        this.mensaje = mensaje;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getMatricula() { return matricula; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Integer getAno() { return ano; }
    public String getClienteNombre() { return clienteNombre; }
    public String getClienteTelefono() { return clienteTelefono; }
    public String getMensaje() { return mensaje; }
}
