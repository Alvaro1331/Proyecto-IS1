package com.taller.controllers;

import com.taller.dtos.ClienteLoginDTO;
import com.taller.dtos.ClienteRegistroDTO;
import com.taller.models.Cliente;
import com.taller.repositories.ClienteRepository;
import com.taller.services.ClienteService;
import com.taller.services.RecuperacionPasswordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la gestión de clientes.
 *
 * ENDPOINTS:
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │ GET    /api/clientes                          → Listar todos los clientes    │
 * │ GET    /api/clientes/{id}                     → Buscar cliente por ID         │
 * │ GET    /api/clientes/telefono/{tel}           → Buscar cliente por teléfono   │
 * │ POST   /api/clientes/registro                 → Registrar cuenta de cliente   │
 * │ POST   /api/clientes/login                    → Iniciar sesión como cliente   │
 * │ POST   /api/clientes/{id}/vincular            → Vincular un localizador       │
 * │ GET    /api/clientes/{id}/mis-reparaciones    → Ver reparaciones vinculadas   │
 * └──────────────────────────────────────────────────────────────────────────────┘
 *
 * NOTA: Los clientes también se crean automáticamente al registrar un vehículo
 * (sin cuenta). Si luego el cliente se registra con el mismo teléfono,
 * se "vincula" la cuenta a su perfil existente.
 */
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private RecuperacionPasswordService recuperacionPasswordService;

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

    /**
     * Registra una nueva cuenta de cliente.
     * Si el teléfono ya existe en el sistema (creado por el recepcionista),
     * se vincula la cuenta al perfil existente en vez de crear uno duplicado.
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registrarCliente(@Valid @RequestBody ClienteRegistroDTO dto) {
        try {
            Map<String, Object> respuesta = clienteService.registrarCliente(
                    dto.getNombre(), dto.getTelefono(), dto.getEmail(), dto.getPassword());
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Inicio de sesión para clientes.
     * El cliente puede usar su email o su teléfono + contraseña.
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginCliente(@Valid @RequestBody ClienteLoginDTO dto) {
        try {
            Map<String, Object> respuesta = clienteService.loginCliente(
                    dto.getEmailOrTelefono(), dto.getPassword());
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recuperación de contraseña - Paso 1.
     * Envía un código de 6 dígitos al correo del cliente (si existe como cliente).
     * Body: { "email": "juan@email.com" }
     */
    @PostMapping("/recuperar/solicitar")
    public ResponseEntity<?> solicitarCodigoRecuperacion(@RequestBody Map<String, String> body) {
        try {
            recuperacionPasswordService.solicitarCodigo(body.get("email"));
            return ResponseEntity.ok(Map.of("mensaje", "Te hemos enviado un código a tu correo electrónico."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recuperación de contraseña - Paso 2.
     * Comprueba que el código introducido es correcto.
     * Body: { "email": "...", "codigo": "123456" }
     */
    @PostMapping("/recuperar/verificar")
    public ResponseEntity<?> verificarCodigoRecuperacion(@RequestBody Map<String, String> body) {
        try {
            recuperacionPasswordService.verificarCodigo(body.get("email"), body.get("codigo"));
            return ResponseEntity.ok(Map.of("mensaje", "Código correcto."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Recuperación de contraseña - Paso 3.
     * Guarda la nueva contraseña (introducida dos veces) si el código sigue siendo válido.
     * Body: { "email": "...", "codigo": "123456", "nuevaPassword": "...", "confirmarPassword": "..." }
     */
    @PostMapping("/recuperar/restablecer")
    public ResponseEntity<?> restablecerPassword(@RequestBody Map<String, String> body) {
        try {
            recuperacionPasswordService.restablecerPassword(
                    body.get("email"), body.get("codigo"),
                    body.get("nuevaPassword"), body.get("confirmarPassword"));
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Vincula un código localizador a la cuenta del cliente.
     * El cliente introduce el código que le dio el recepcionista
     * y la reparación se guarda en su perfil para consultarla siempre.
     *
     * Ejemplo: POST /api/clientes/5/vincular
     * Body: { "localizador": "A3F7K9X2" }
     */
    @PostMapping("/{id}/vincular")
    public ResponseEntity<?> vincularLocalizador(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String localizador = body.get("localizador");
            if (localizador == null || localizador.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El código localizador es obligatorio."));
            }
            Map<String, Object> respuesta = clienteService.vincularLocalizador(id, localizador);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Devuelve todas las reparaciones vinculadas a la cuenta de un cliente.
     * GET /api/clientes/5/mis-reparaciones
     */
    @GetMapping("/{id}/mis-reparaciones")
    public ResponseEntity<?> obtenerMisReparaciones(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(clienteService.obtenerMisReparaciones(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
