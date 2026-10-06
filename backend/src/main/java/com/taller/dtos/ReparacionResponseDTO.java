package com.taller.dtos;

import java.time.LocalDate;

/**
 * DTO de respuesta con los datos de una reparación.
 * Incluye los nombres del mecánico y recepcionista (no solo sus IDs)
 * para que el frontend pueda mostrar información legible.
 */
public class ReparacionResponseDTO {

    private Long id;
    private String matriculaVehiculo;
    private String marcaVehiculo;
    private String modeloVehiculo;
    private String nombreCliente;
    private String telefonoCliente;
    private String nombreMecanico;
    private String nombreRecepcionista;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private String estado;
    private String descripcionAveria;
    private String mensaje;

    // Constructor completo
    public ReparacionResponseDTO(Long id, String matriculaVehiculo, String marcaVehiculo,
                                  String modeloVehiculo, String nombreCliente, String telefonoCliente,
                                  String nombreMecanico, String nombreRecepcionista,
                                  LocalDate fechaEntrada, LocalDate fechaSalida,
                                  String estado, String descripcionAveria, String mensaje) {
        this.id = id;
        this.matriculaVehiculo = matriculaVehiculo;
        this.marcaVehiculo = marcaVehiculo;
        this.modeloVehiculo = modeloVehiculo;
        this.nombreCliente = nombreCliente;
        this.telefonoCliente = telefonoCliente;
        this.nombreMecanico = nombreMecanico;
        this.nombreRecepcionista = nombreRecepcionista;
        this.fechaEntrada = fechaEntrada;
        this.fechaSalida = fechaSalida;
        this.estado = estado;
        this.descripcionAveria = descripcionAveria;
        this.mensaje = mensaje;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getMatriculaVehiculo() { return matriculaVehiculo; }
    public String getMarcaVehiculo() { return marcaVehiculo; }
    public String getModeloVehiculo() { return modeloVehiculo; }
    public String getNombreCliente() { return nombreCliente; }
    public String getTelefonoCliente() { return telefonoCliente; }
    public String getNombreMecanico() { return nombreMecanico; }
    public String getNombreRecepcionista() { return nombreRecepcionista; }
    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public LocalDate getFechaSalida() { return fechaSalida; }
    public String getEstado() { return estado; }
    public String getDescripcionAveria() { return descripcionAveria; }
    public String getMensaje() { return mensaje; }
}
