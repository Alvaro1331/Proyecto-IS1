package com.taller.services;

import com.taller.models.ConfiguracionTaller;
import com.taller.repositories.ConfiguracionTallerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio Singleton para gestionar los parámetros globales de configuración del taller:
 * - Porcentaje de margen de beneficio (%)
 * - Precio por hora de mano de obra (€/h)
 * - Fondo inicial de tesorería (€)
 */
@Service
public class ConfiguracionTallerService {

    @Autowired
    private ConfiguracionTallerRepository configuracionRepository;

    /**
     * Devuelve siempre la configuración activa del taller.
     * Si no existe todavía en la base de datos, crea el registro por defecto (15%, 35 €/h, 2000 €).
     */
    public synchronized ConfiguracionTaller obtenerConfiguracion() {
        List<ConfiguracionTaller> lista = configuracionRepository.findAll();
        if (lista.isEmpty()) {
            ConfiguracionTaller inicial = new ConfiguracionTaller(15.0, 35.0, 2000.0);
            return configuracionRepository.save(inicial);
        }
        return lista.get(0);
    }

    /**
     * Actualiza los parámetros de configuración del taller.
     */
    public synchronized ConfiguracionTaller actualizarConfiguracion(Double margenBeneficioPorcentaje,
                                                                     Double precioHoraManoObra,
                                                                     Double fondoInicial) {
        ConfiguracionTaller config = obtenerConfiguracion();

        if (margenBeneficioPorcentaje != null) {
            if (margenBeneficioPorcentaje < 0 || margenBeneficioPorcentaje > 500) {
                throw new IllegalArgumentException("El porcentaje de margen de beneficio debe estar entre 0% y 500%.");
            }
            config.setMargenBeneficioPorcentaje(Math.round(margenBeneficioPorcentaje * 100.0) / 100.0);
        }

        if (precioHoraManoObra != null) {
            if (precioHoraManoObra <= 0) {
                throw new IllegalArgumentException("El precio por hora de mano de obra debe ser mayor que 0 €.");
            }
            config.setPrecioHoraManoObra(Math.round(precioHoraManoObra * 100.0) / 100.0);
        }

        if (fondoInicial != null) {
            if (fondoInicial < 0) {
                throw new IllegalArgumentException("El fondo inicial no puede ser negativo.");
            }
            config.setFondoInicial(Math.round(fondoInicial * 100.0) / 100.0);
        }

        return configuracionRepository.save(config);
    }
}
