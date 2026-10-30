package com.taller.controllers;

import com.taller.services.ReparacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el Archivo de Facturas del taller (accesible para Admin y Recepcionista).
 */
@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaController {

    @Autowired
    private ReparacionService reparacionService;

    /**
     * Devuelve el listado completo de facturas emitidas con todos los detalles de la reparación,
     * vehículo, cliente, desglose de materiales, mano de obra, margen e IVA.
     * GET /api/facturas
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> obtenerTodasLasFacturas() {
        return ResponseEntity.ok(reparacionService.obtenerTodasLasFacturas());
    }
}
