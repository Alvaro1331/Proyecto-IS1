package com.taller.services;

import com.taller.dtos.UsuarioRequestDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.models.Reparacion;
import com.taller.models.Rol;
import com.taller.models.Usuario;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private ReparacionRepository reparacionRepository;

    @Autowired
    private PasswordSecurityService passwordSecurityService = new PasswordSecurityService();

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

        passwordSecurityService.validarFortaleza(dto.getPassword());
        String passwordHash = passwordSecurityService.hashear(dto.getPassword());

        Usuario usuario = new Usuario(
                dto.getNombre(),
                dto.getEmail().toLowerCase(),
                passwordHash,
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
     * Login seguro: busca un usuario por email y comprueba la contraseña frente al hash BCrypt.
     * Si la cuenta tuviera una clave heredada en texto plano, la migra a BCrypt tras autenticar.
     */
    public UsuarioResponseDTO login(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: no existe ningún empleado con ese correo."));

        if (!passwordSecurityService.coincide(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }

        if (!passwordSecurityService.estaHasheada(usuario.getPassword())) {
            usuario.setPassword(passwordSecurityService.hashear(password));
            usuarioRepository.save(usuario);
        }

        return convertirAResponseDTO(usuario, "Login correcto. Bienvenido, " + usuario.getNombre() + ".");
    }

    /**
     * HU #59: Modifica los datos personales, rol y (opcionalmente) contraseña de un empleado.
     */
    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, String nombre, String email,
                                                String telefono, String rolStr,
                                                String nuevaPassword, Long adminActualId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario con ID: " + id));

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del empleado no puede estar vacío.");
        }
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            throw new IllegalArgumentException("Debes introducir un correo electrónico válido.");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del empleado no puede estar vacío.");
        }

        String emailNorm = email.trim().toLowerCase();
        Optional<Usuario> otroConEmail = usuarioRepository.findByEmail(emailNorm);
        if (otroConEmail.isPresent() && !otroConEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro empleado registrado con el correo: " + emailNorm);
        }

        Rol nuevoRol;
        try {
            nuevoRol = Rol.valueOf(rolStr.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Rol no válido: " + rolStr
                    + ". Los roles válidos son: ADMIN, RECEPCIONISTA, MECANICO.");
        }

        if (adminActualId != null && adminActualId.equals(id) && nuevoRol != Rol.ADMIN) {
            throw new IllegalStateException("No puedes quitarte a ti mismo el rol de ADMIN mientras tienes la sesión iniciada.");
        }

        if (usuario.getRol() == Rol.ADMIN && nuevoRol != Rol.ADMIN) {
            List<Usuario> admins = usuarioRepository.findByRol(Rol.ADMIN);
            if (admins != null && admins.size() <= 1) {
                throw new IllegalStateException("No se puede cambiar el rol del único administrador del taller.");
            }
        }

        usuario.setNombre(nombre.trim());
        usuario.setEmail(emailNorm);
        usuario.setTelefono(telefono.trim());
        usuario.setRol(nuevoRol);

        if (nuevaPassword != null && !nuevaPassword.isBlank()) {
            passwordSecurityService.validarFortaleza(nuevaPassword);
            usuario.setPassword(passwordSecurityService.hashear(nuevaPassword));
        }

        usuarioRepository.save(usuario);
        return convertirAResponseDTO(usuario, "Datos del empleado actualizados correctamente.");
    }

    /**
     * HU #59: Da de baja a un empleado del taller, protegiendo contra autoborrado del admin
     * y desasignando de forma segura cualquier reparación en la que figure como mecánico.
     */
    @Transactional
    public Map<String, Object> eliminarUsuario(Long id, Long adminActualId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario con ID: " + id));

        if (adminActualId != null && adminActualId.equals(id)) {
            throw new IllegalStateException("No puedes eliminar tu propia cuenta de administrador mientras tienes la sesión iniciada.");
        }

        if (usuario.getRol() == Rol.ADMIN) {
            List<Usuario> admins = usuarioRepository.findByRol(Rol.ADMIN);
            if (admins != null && admins.size() <= 1) {
                throw new IllegalStateException("No se puede eliminar al único administrador del taller.");
            }
        }

        int reparacionesDesasignadas = 0;
        if (reparacionRepository != null) {
            List<Reparacion> comoRecep = reparacionRepository.findByRecepcionistaId(id);
            if (comoRecep != null && !comoRecep.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar a " + usuario.getNombre()
                        + " porque figura como recepcionista en " + comoRecep.size()
                        + " reparación(es) del historial.");
            }

            List<Reparacion> comoMecanico = reparacionRepository.findByMecanicoId(id);
            if (comoMecanico != null && !comoMecanico.isEmpty()) {
                for (Reparacion rep : comoMecanico) {
                    rep.setMecanico(null);
                    reparacionRepository.save(rep);
                    reparacionesDesasignadas++;
                }
            }
        }

        usuarioRepository.delete(usuario);

        String msg = "Empleado " + usuario.getNombre() + " eliminado correctamente.";
        if (reparacionesDesasignadas > 0) {
            msg += " Se han desasignado " + reparacionesDesasignadas + " reparación(es) (quedaron como 'Sin asignar').";
        }
        return Map.of(
                "id", id,
                "reparacionesDesasignadas", reparacionesDesasignadas,
                "mensaje", msg
        );
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
