package com.taller.controllers;

import com.taller.models.ConfiguracionTaller;
import com.taller.services.ConfiguracionTallerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador REST para consultar y actualizar la configuración económica del taller
 * (porcentaje de margen de beneficio, precio por hora de mano de obra y fondo inicial).
 */
@RestController
@RequestMapping("/api/configuracion")
@CrossOrigin(origins = "*")
public class ConfiguracionTallerController {

    @Autowired
    private ConfiguracionTallerService configuracionService;

    @GetMapping
    public ResponseEntity<ConfiguracionTaller> obtenerConfiguracion() {
        return ResponseEntity.ok(configuracionService.obtenerConfiguracion());
    }

    @PutMapping
    public ResponseEntity<?> actualizarConfiguracion(@RequestBody Map<String, Object> body) {
        try {
            Double margen = body.get("margenBeneficioPorcentaje") != null
                    ? Double.valueOf(body.get("margenBeneficioPorcentaje").toString())
                    : null;
            Double precioHora = body.get("precioHoraManoObra") != null
                    ? Double.valueOf(body.get("precioHoraManoObra").toString())
                    : null;
            Double fondoInicial = body.get("fondoInicial") != null
                    ? Double.valueOf(body.get("fondoInicial").toString())
                    : null;

            ConfiguracionTaller actualizada = configuracionService.actualizarConfiguracion(margen, precioHora, fondoInicial);

            Map<String, Object> respuesta = new LinkedHashMap<>();
            respuesta.put("id", actualizada.getId());
            respuesta.put("margenBeneficioPorcentaje", actualizada.getMargenBeneficioPorcentaje());
            respuesta.put("precioHoraManoObra", actualizada.getPrecioHoraManoObra());
            respuesta.put("fondoInicial", actualizada.getFondoInicial());
            respuesta.put("mensaje", "Configuración del taller actualizada correctamente.");
            return ResponseEntity.ok(respuesta);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Los valores numéricos de configuración no son válidos."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
