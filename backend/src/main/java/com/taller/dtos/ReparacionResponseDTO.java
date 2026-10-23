package com.taller.dtos;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * DTO de respuesta con los datos de una reparación.
 * Incluye tanto los datos del vehículo/mecánico como el desglose económico:
 * - presupuesto: precio acordado con el cliente (ej. 200 €)
 * - presupuestoEstimado: lo pensado por recepción para arreglarlo (ej. 100 €)
 * - presupuestoMecanico: límite decidido por el Admin para el mecánico (ej. 100 €, 150 € o 75 €)
 * - costeMateriales: gasto real en piezas usadas por el mecánico
 * - presupuestoRestante: saldo que le queda al mecánico (presupuestoMecanico - costeMateriales)
 * - beneficio: ganancia del taller (presupuesto - costeMateriales)
 */
public class ReparacionResponseDTO {

    private Long id;
    private String localizador;
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
    private Double presupuesto;
    private Double presupuestoEstimado;
    private Double presupuestoMecanico;
    private Double costeMateriales;
    private Double presupuestoRestante;
    private Double beneficio;
    private List<String> materialesDetalle;
    private String mensaje;

    // Constructor clásico compatible
    public ReparacionResponseDTO(Long id, String localizador, String matriculaVehiculo, String marcaVehiculo,
                                  String modeloVehiculo, String nombreCliente, String telefonoCliente,
                                  String nombreMecanico, String nombreRecepcionista,
                                  LocalDate fechaEntrada, LocalDate fechaSalida,
                                  String estado, String descripcionAveria, String mensaje) {
        this(id, localizador, matriculaVehiculo, marcaVehiculo, modeloVehiculo,
                nombreCliente, telefonoCliente, nombreMecanico, nombreRecepcionista,
                fechaEntrada, fechaSalida, estado, descripcionAveria,
                200.0, 100.0, 100.0, 0.0, 100.0, 200.0, Collections.emptyList(), mensaje);
    }

    // Constructor completo con datos económicos
    public ReparacionResponseDTO(Long id, String localizador, String matriculaVehiculo, String marcaVehiculo,
                                  String modeloVehiculo, String nombreCliente, String telefonoCliente,
                                  String nombreMecanico, String nombreRecepcionista,
                                  LocalDate fechaEntrada, LocalDate fechaSalida,
                                  String estado, String descripcionAveria,
                                  Double presupuesto, Double presupuestoEstimado, Double presupuestoMecanico,
                                  Double costeMateriales, Double presupuestoRestante, Double beneficio,
                                  List<String> materialesDetalle, String mensaje) {
        this.id = id;
        this.localizador = localizador;
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
        this.presupuesto = presupuesto;
        this.presupuestoEstimado = presupuestoEstimado;
        this.presupuestoMecanico = presupuestoMecanico;
        this.costeMateriales = costeMateriales;
        this.presupuestoRestante = presupuestoRestante;
        this.beneficio = beneficio;
        this.materialesDetalle = materialesDetalle;
        this.mensaje = mensaje;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getLocalizador() { return localizador; }
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
    public Double getPresupuesto() { return presupuesto; }
    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public Double getCosteMateriales() { return costeMateriales; }
    public Double getPresupuestoRestante() { return presupuestoRestante; }
    public Double getBeneficio() { return beneficio; }
    public List<String> getMaterialesDetalle() { return materialesDetalle; }
    public String getMensaje() { return mensaje; }
}
