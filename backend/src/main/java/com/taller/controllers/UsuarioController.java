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

    /**
     * HU #59: Actualiza datos personales, rol y (opcionalmente) contraseña de un empleado.
     * PUT /api/usuarios/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String nombre = body.get("nombre") != null ? body.get("nombre").toString() : null;
            String email = body.get("email") != null ? body.get("email").toString() : null;
            String telefono = body.get("telefono") != null ? body.get("telefono").toString() : null;
            String rol = body.get("rol") != null ? body.get("rol").toString() : null;
            String nuevaPassword = body.get("password") != null ? body.get("password").toString()
                    : (body.get("nuevaPassword") != null ? body.get("nuevaPassword").toString() : null);
            Long adminActualId = body.get("adminActualId") != null && !body.get("adminActualId").toString().isBlank()
                    ? Long.valueOf(body.get("adminActualId").toString()) : null;

            UsuarioResponseDTO respuesta = usuarioService.actualizarUsuario(
                    id, nombre, email, telefono, rol, nuevaPassword, adminActualId);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * HU #59: Elimina un empleado del taller protegiendo contra autoborrado del admin.
     * DELETE /api/usuarios/{id}?adminActualId=1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id,
                                             @RequestParam(required = false) Long adminActualId) {
        try {
            return ResponseEntity.ok(usuarioService.eliminarUsuario(id, adminActualId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
