package com.taller.dtos;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta con todos los datos de una reparación:
 * - Datos del vehículo, cliente, mecánico y recepcionista
 * - Estimación inicial orientativa (estimacionInicial / presupuesto)
 * - Seguimiento temporal: diasEstimados, fechaEstimadaFin, diasRestantes
 * - Trabajo real del mecánico: horasTrabajoMecanico, costeMateriales, materialesDetalle, incidencias (con fotos)
 * - Previsión y facturación real: totalCalculadoPrevisto, factura (si ya se ha emitido), beneficio
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

    // Estimación inicial orientativa en Recepción
    private Double presupuesto;
    private Double estimacionInicial;

    // Seguimiento de tiempo y mano de obra
    private Integer diasEstimados;
    private LocalDate fechaEstimadaFin;
    private Long diasRestantes;
    private Double horasTrabajoMecanico;
    private Double precioHoraManoObra;
    private Double subtotalManoObra;
    private Double porcentajeMargen;

    // Costes, impactos e importes
    private Double costeMateriales;
    private Double impactoIncidencias;
    private Double totalCalculadoPrevisto;
    private Double totalFinalFactura;
    private Double beneficio;

    // Mantenidos por compatibilidad JSON
    private Double presupuestoEstimado;
    private Double presupuestoMecanico;
    private Double presupuestoRestante;

    private List<String> materialesDetalle;
    private List<Map<String, Object>> incidencias;
    private Map<String, Object> factura;
    private Map<String, Object> presupuestoFormal;
    private String estadoPresupuesto;
    private String dniCifCliente;
    private String direccionFiscalCliente;
    private String mensaje;

    public ReparacionResponseDTO() {}

    // Constructor clásico compatible
    public ReparacionResponseDTO(Long id, String localizador, String matriculaVehiculo, String marcaVehiculo,
                                  String modeloVehiculo, String nombreCliente, String telefonoCliente,
                                  String nombreMecanico, String nombreRecepcionista,
                                  LocalDate fechaEntrada, LocalDate fechaSalida,
                                  String estado, String descripcionAveria, String mensaje) {
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
        this.presupuesto = 150.0;
        this.estimacionInicial = 150.0;
        this.diasEstimados = 3;
        this.fechaEstimadaFin = fechaEntrada != null ? fechaEntrada.plusDays(3) : LocalDate.now().plusDays(3);
        this.diasRestantes = 3L;
        this.horasTrabajoMecanico = 0.0;
        this.costeMateriales = 0.0;
        this.impactoIncidencias = 0.0;
        this.materialesDetalle = Collections.emptyList();
        this.incidencias = Collections.emptyList();
        this.mensaje = mensaje;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLocalizador() { return localizador; }
    public void setLocalizador(String localizador) { this.localizador = localizador; }

    public String getMatriculaVehiculo() { return matriculaVehiculo; }
    public void setMatriculaVehiculo(String matriculaVehiculo) { this.matriculaVehiculo = matriculaVehiculo; }

    public String getMarcaVehiculo() { return marcaVehiculo; }
    public void setMarcaVehiculo(String marcaVehiculo) { this.marcaVehiculo = marcaVehiculo; }

    public String getModeloVehiculo() { return modeloVehiculo; }
    public void setModeloVehiculo(String modeloVehiculo) { this.modeloVehiculo = modeloVehiculo; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefonoCliente() { return telefonoCliente; }
    public void setTelefonoCliente(String telefonoCliente) { this.telefonoCliente = telefonoCliente; }

    public String getNombreMecanico() { return nombreMecanico; }
    public void setNombreMecanico(String nombreMecanico) { this.nombreMecanico = nombreMecanico; }

    public String getNombreRecepcionista() { return nombreRecepcionista; }
    public void setNombreRecepcionista(String nombreRecepcionista) { this.nombreRecepcionista = nombreRecepcionista; }

    public LocalDate getFechaEntrada() { return fechaEntrada; }
    public void setFechaEntrada(LocalDate fechaEntrada) { this.fechaEntrada = fechaEntrada; }

    public LocalDate getFechaSalida() { return fechaSalida; }
    public void setFechaSalida(LocalDate fechaSalida) { this.fechaSalida = fechaSalida; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getDescripcionAveria() { return descripcionAveria; }
    public void setDescripcionAveria(String descripcionAveria) { this.descripcionAveria = descripcionAveria; }

    public Double getPresupuesto() { return presupuesto; }
    public void setPresupuesto(Double presupuesto) { this.presupuesto = presupuesto; }

    public Double getEstimacionInicial() { return estimacionInicial; }
    public void setEstimacionInicial(Double estimacionInicial) { this.estimacionInicial = estimacionInicial; }

    public Integer getDiasEstimados() { return diasEstimados; }
    public void setDiasEstimados(Integer diasEstimados) { this.diasEstimados = diasEstimados; }

    public LocalDate getFechaEstimadaFin() { return fechaEstimadaFin; }
    public void setFechaEstimadaFin(LocalDate fechaEstimadaFin) { this.fechaEstimadaFin = fechaEstimadaFin; }

    public Long getDiasRestantes() { return diasRestantes; }
    public void setDiasRestantes(Long diasRestantes) { this.diasRestantes = diasRestantes; }

    public Double getHorasTrabajoMecanico() { return horasTrabajoMecanico; }
    public void setHorasTrabajoMecanico(Double horasTrabajoMecanico) { this.horasTrabajoMecanico = horasTrabajoMecanico; }

    public Double getPrecioHoraManoObra() { return precioHoraManoObra; }
    public void setPrecioHoraManoObra(Double precioHoraManoObra) { this.precioHoraManoObra = precioHoraManoObra; }

    public Double getSubtotalManoObra() { return subtotalManoObra; }
    public void setSubtotalManoObra(Double subtotalManoObra) { this.subtotalManoObra = subtotalManoObra; }

    public Double getPorcentajeMargen() { return porcentajeMargen; }
    public void setPorcentajeMargen(Double porcentajeMargen) { this.porcentajeMargen = porcentajeMargen; }

    public Double getCosteMateriales() { return costeMateriales; }
    public void setCosteMateriales(Double costeMateriales) { this.costeMateriales = costeMateriales; }

    public Double getImpactoIncidencias() { return impactoIncidencias; }
    public void setImpactoIncidencias(Double impactoIncidencias) { this.impactoIncidencias = impactoIncidencias; }

    public Double getTotalCalculadoPrevisto() { return totalCalculadoPrevisto; }
    public void setTotalCalculadoPrevisto(Double totalCalculadoPrevisto) { this.totalCalculadoPrevisto = totalCalculadoPrevisto; }

    public Double getTotalFinalFactura() { return totalFinalFactura; }
    public void setTotalFinalFactura(Double totalFinalFactura) { this.totalFinalFactura = totalFinalFactura; }

    public Double getBeneficio() { return beneficio; }
    public void setBeneficio(Double beneficio) { this.beneficio = beneficio; }

    public Double getPresupuestoEstimado() { return presupuestoEstimado; }
    public void setPresupuestoEstimado(Double presupuestoEstimado) { this.presupuestoEstimado = presupuestoEstimado; }

    public Double getPresupuestoMecanico() { return presupuestoMecanico; }
    public void setPresupuestoMecanico(Double presupuestoMecanico) { this.presupuestoMecanico = presupuestoMecanico; }

    public Double getPresupuestoRestante() { return presupuestoRestante; }
    public void setPresupuestoRestante(Double presupuestoRestante) { this.presupuestoRestante = presupuestoRestante; }

    public List<String> getMaterialesDetalle() { return materialesDetalle; }
    public void setMaterialesDetalle(List<String> materialesDetalle) { this.materialesDetalle = materialesDetalle; }

    public List<Map<String, Object>> getIncidencias() { return incidencias; }
    public void setIncidencias(List<Map<String, Object>> incidencias) { this.incidencias = incidencias; }

    public Map<String, Object> getFactura() { return factura; }
    public void setFactura(Map<String, Object> factura) { this.factura = factura; }

    public Map<String, Object> getPresupuestoFormal() { return presupuestoFormal; }
    public void setPresupuestoFormal(Map<String, Object> presupuestoFormal) { this.presupuestoFormal = presupuestoFormal; }

    public String getEstadoPresupuesto() { return estadoPresupuesto; }
    public void setEstadoPresupuesto(String estadoPresupuesto) { this.estadoPresupuesto = estadoPresupuesto; }

    public String getDniCifCliente() { return dniCifCliente; }
    public void setDniCifCliente(String dniCifCliente) { this.dniCifCliente = dniCifCliente; }

    public String getDireccionFiscalCliente() { return direccionFiscalCliente; }
    public void setDireccionFiscalCliente(String direccionFiscalCliente) { this.direccionFiscalCliente = direccionFiscalCliente; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
