package com.taller.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Representa una incidencia, hallazgo o nota explicativa registrada por el mecánico
 * durante la reparación, incluyendo el impacto orientativo en el precio y una foto opcional.
 */
@Entity
@Table(name = "incidencias_reparacion")
public class IncidenciaReparacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reparacion_id", nullable = false)
    @JsonIgnore
    private Reparacion reparacion;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    // Impacto orientativo en el coste (puede ser 0.0 o positivo si añade coste previsto)
    private Double impactoPrecio = 0.0;

    // Ruta pública relativa de la foto subida (ej. "/uploads/incidencia_3_1710000.jpg")
    private String rutaFoto;

    public IncidenciaReparacion() {}

    public IncidenciaReparacion(Reparacion reparacion, String descripcion, Double impactoPrecio, String rutaFoto) {
        this.reparacion = reparacion;
        this.fecha = LocalDateTime.now();
        this.descripcion = descripcion;
        this.impactoPrecio = impactoPrecio != null ? impactoPrecio : 0.0;
        this.rutaFoto = rutaFoto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Reparacion getReparacion() { return reparacion; }
    public void setReparacion(Reparacion reparacion) { this.reparacion = reparacion; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Double getImpactoPrecio() { return impactoPrecio; }
    public void setImpactoPrecio(Double impactoPrecio) { this.impactoPrecio = impactoPrecio; }

    public String getRutaFoto() { return rutaFoto; }
    public void setRutaFoto(String rutaFoto) { this.rutaFoto = rutaFoto; }
}
