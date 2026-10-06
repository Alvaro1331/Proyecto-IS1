package com.taller.services;

import com.taller.dtos.UsuarioRequestDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.models.Rol;
import com.taller.models.Usuario;
import com.taller.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crea un nuevo usuario en el sistema.
     * Solo el ADMIN puede crear usuarios (eso se controlará en el Controller).
     */
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO dto) {
        // Comprobar que el email no esté ya registrado
        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Ya existe un usuario con el email: " + dto.getEmail());
        }

        // Convertir el String del rol a un Enum
        Rol rol;
        try {
            rol = Rol.valueOf(dto.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido: " + dto.getRol()
                    + ". Los roles válidos son: ADMIN, RECEPCIONISTA, MECANICO, CLIENTE");
        }

        Usuario usuario = new Usuario(
                dto.getNombre(),
                dto.getEmail().toLowerCase(),
                dto.getPassword(), // En un proyecto real, aquí se cifraría la contraseña
                dto.getTelefono(),
                rol
        );
        usuarioRepository.save(usuario);

        return convertirAResponseDTO(usuario, "Usuario creado correctamente.");
    }

    /**
     * Devuelve todos los usuarios del sistema.
     */
    public List<UsuarioResponseDTO> obtenerTodos() {
        return usuarioRepository.findAll().stream()
                .map(u -> convertirAResponseDTO(u, null))
                .collect(Collectors.toList());
    }

    /**
     * Busca un usuario por su ID.
     */
    public UsuarioResponseDTO obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario con ID: " + id));
        return convertirAResponseDTO(usuario, null);
    }

    /**
     * Devuelve todos los usuarios con un rol concreto (ej: todos los mecánicos).
     */
    public List<UsuarioResponseDTO> obtenerPorRol(String rolStr) {
        Rol rol;
        try {
            rol = Rol.valueOf(rolStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol no válido: " + rolStr);
        }

        return usuarioRepository.findByRol(rol).stream()
                .map(u -> convertirAResponseDTO(u, null))
                .collect(Collectors.toList());
    }

    /**
     * Login básico: busca un usuario por email y comprueba la contraseña.
     * NOTA: En un proyecto real se usaría Spring Security con BCrypt.
     * Para este proyecto académico, usamos una comparación directa.
     */
    public UsuarioResponseDTO login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email o contraseña incorrectos."));

        if (!usuario.getPassword().equals(password)) {
            throw new IllegalArgumentException("Email o contraseña incorrectos.");
        }

        return convertirAResponseDTO(usuario, "Login correcto. Bienvenido, " + usuario.getNombre() + ".");
    }

    // --- Método auxiliar para convertir entidad a DTO ---
    private UsuarioResponseDTO convertirAResponseDTO(Usuario usuario, String mensaje) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getTelefono(),
                usuario.getRol().name(),
                mensaje
        );
    }
}
