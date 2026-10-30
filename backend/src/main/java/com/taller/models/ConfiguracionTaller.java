package com.taller.models;

import jakarta.persistence.*;

/**
 * Entidad Singleton con los parámetros globales de facturación y beneficio del taller.
 * - margenBeneficioPorcentaje: porcentaje de beneficio comercial del taller sobre (materiales + mano de obra), ej. 15.0%
 * - precioHoraManoObra: tarifa por hora de trabajo del mecánico en euros, ej. 35.0 €/h
 * - fondoInicial: capital base de tesorería del taller, ej. 2000.0 €
 */
@Entity
@Table(name = "configuracion_taller")
public class ConfiguracionTaller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double margenBeneficioPorcentaje = 15.0;

    @Column(nullable = false)
    private Double precioHoraManoObra = 35.0;

    @Column(nullable = false)
    private Double fondoInicial = 2000.0;

    public ConfiguracionTaller() {}

    public ConfiguracionTaller(Double margenBeneficioPorcentaje, Double precioHoraManoObra, Double fondoInicial) {
        this.margenBeneficioPorcentaje = margenBeneficioPorcentaje != null ? margenBeneficioPorcentaje : 15.0;
        this.precioHoraManoObra = precioHoraManoObra != null ? precioHoraManoObra : 35.0;
        this.fondoInicial = fondoInicial != null ? fondoInicial : 2000.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getMargenBeneficioPorcentaje() { return margenBeneficioPorcentaje; }
    public void setMargenBeneficioPorcentaje(Double margenBeneficioPorcentaje) { this.margenBeneficioPorcentaje = margenBeneficioPorcentaje; }

    public Double getPrecioHoraManoObra() { return precioHoraManoObra; }
    public void setPrecioHoraManoObra(Double precioHoraManoObra) { this.precioHoraManoObra = precioHoraManoObra; }

    public Double getFondoInicial() { return fondoInicial; }
    public void setFondoInicial(Double fondoInicial) { this.fondoInicial = fondoInicial; }
}
