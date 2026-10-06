package com.taller.controllers;

import com.taller.models.Cliente;
import com.taller.repositories.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de clientes.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ GET    /api/clientes                → Listar todos los clientes     │
 * │ GET    /api/clientes/{id}           → Buscar cliente por ID          │
 * │ GET    /api/clientes/telefono/{tel} → Buscar cliente por teléfono    │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * NOTA: Los clientes se crean automáticamente al registrar un vehículo,
 * no hace falta un endpoint POST separado.
 */
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    public ResponseEntity<List<Cliente>> obtenerTodos() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(
                        Map.of("error", "No se encontró el cliente con ID: " + id)));
    }

    @GetMapping("/telefono/{telefono}")
    public ResponseEntity<?> obtenerPorTelefono(@PathVariable String telefono) {
        return clienteRepository.findByTelefono(telefono)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(
                        Map.of("error", "No se encontró un cliente con teléfono: " + telefono)));
    }
}
