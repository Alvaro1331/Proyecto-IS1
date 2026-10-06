package com.taller.controllers;

import com.taller.dtos.UsuarioRequestDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.services.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de usuarios.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/usuarios              → Crear un nuevo usuario              │
 * │ GET    /api/usuarios              → Listar todos los usuarios           │
 * │ GET    /api/usuarios/{id}         → Buscar usuario por ID               │
 * │ GET    /api/usuarios/rol/{rol}    → Listar usuarios por rol             │
 * │ POST   /api/usuarios/login        → Iniciar sesión                      │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<?> crearUsuario(@Valid @RequestBody UsuarioRequestDTO dto) {
        try {
            UsuarioResponseDTO respuesta = usuarioService.crearUsuario(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> obtenerTodos() {
        return ResponseEntity.ok(usuarioService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rol/{rol}")
    public ResponseEntity<?> obtenerPorRol(@PathVariable String rol) {
        try {
            return ResponseEntity.ok(usuarioService.obtenerPorRol(rol));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Login básico.
     * El frontend envía: { "email": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        try {
            String email = credenciales.get("email");
            String password = credenciales.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email y contraseña son obligatorios."));
            }

            UsuarioResponseDTO respuesta = usuarioService.login(email, password);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}
