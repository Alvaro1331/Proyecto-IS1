package com.taller.services;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.UsuarioRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReparacionService {

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Crea una nueva reparación para un vehículo.
     * Se crea en estado PENDIENTE y sin mecánico asignado.
     * Se genera automáticamente un código localizador único
     * para que el cliente pueda consultar el estado de su reparación.
     */
    public ReparacionResponseDTO crearReparacion(ReparacionRequestDTO dto) {
        // Buscar el vehículo
        Vehiculo vehiculo = vehiculoRepository.findById(dto.getVehiculoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con ID: " + dto.getVehiculoId()));

        // Buscar el recepcionista y verificar que tiene el rol correcto
        Usuario recepcionista = usuarioRepository.findById(dto.getRecepcionistaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el usuario con ID: " + dto.getRecepcionistaId()));

        if (recepcionista.getRol() != Rol.RECEPCIONISTA && recepcionista.getRol() != Rol.ADMIN) {
            throw new IllegalArgumentException(
                    "El usuario " + recepcionista.getNombre() + " no tiene permisos de recepcionista.");
        }

        // Crear la reparación (el localizador se genera automáticamente en el constructor)
        Reparacion reparacion = new Reparacion(vehiculo, recepcionista, dto.getDescripcionAveria());
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Reparación creada correctamente. Localizador: " + reparacion.getLocalizador());
    }

    /**
     * Busca una reparación por su código localizador.
     * Este es el endpoint público que usan los clientes para consultar
     * el estado de su reparación sin necesidad de iniciar sesión.
     */
    public ReparacionResponseDTO obtenerPorLocalizador(String localizador) {
        Reparacion reparacion = reparacionRepository.findByLocalizador(localizador.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ninguna reparación con el código: " + localizador));
        return convertirAResponseDTO(reparacion, null);
    }

    /**
     * Asigna un mecánico a una reparación existente.
     * Cambia el estado de PENDIENTE a EN_PROCESO.
     */
    public ReparacionResponseDTO asignarMecanico(Long reparacionId, Long mecanicoId) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        Usuario mecanico = usuarioRepository.findById(mecanicoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el usuario con ID: " + mecanicoId));

        if (mecanico.getRol() != Rol.MECANICO && mecanico.getRol() != Rol.ADMIN) {
            throw new IllegalArgumentException(
                    "El usuario " + mecanico.getNombre() + " no tiene rol de mecánico.");
        }

        reparacion.setMecanico(mecanico);
        reparacion.setEstado(EstadoReparacion.EN_PROCESO);
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Mecánico " + mecanico.getNombre() + " asignado correctamente.");
    }

    /**
     * Actualiza el estado de una reparación.
     * Valida que el flujo sea lógico (no se puede pasar de ENTREGADA a PENDIENTE, etc.)
     */
    public ReparacionResponseDTO actualizarEstado(Long reparacionId, String nuevoEstadoStr) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        EstadoReparacion nuevoEstado;
        try {
            nuevoEstado = EstadoReparacion.valueOf(nuevoEstadoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + nuevoEstadoStr
                    + ". Los estados válidos son: PENDIENTE, EN_PROCESO, FINALIZADA, ENTREGADA");
        }

        // Validar que el flujo tenga sentido
        validarTransicionEstado(reparacion.getEstado(), nuevoEstado);

        // Impedir que una reparación esté EN_PROCESO, FINALIZADA o ENTREGADA sin mecánico asignado
        if (nuevoEstado != EstadoReparacion.PENDIENTE && reparacion.getMecanico() == null) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el estado a " + nuevoEstado + " porque la reparación no tiene ningún mecánico asignado.");
        }

        reparacion.setEstado(nuevoEstado);

        // Si se marca como finalizada o entregada, guardamos la fecha de salida
        if (nuevoEstado == EstadoReparacion.FINALIZADA || nuevoEstado == EstadoReparacion.ENTREGADA) {
            reparacion.setFechaSalida(LocalDate.now());
        }

        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Estado actualizado a " + nuevoEstado.name() + ".");
    }

    /**
     * Devuelve todas las reparaciones del sistema.
     */
    public List<ReparacionResponseDTO> obtenerTodas() {
        return reparacionRepository.findAll().stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve una reparación por su ID.
     */
    public ReparacionResponseDTO obtenerPorId(Long id) {
        Reparacion reparacion = reparacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + id));
        return convertirAResponseDTO(reparacion, null);
    }

    /**
     * Devuelve todas las reparaciones en un estado concreto.
     */
    public List<ReparacionResponseDTO> obtenerPorEstado(String estadoStr) {
        EstadoReparacion estado;
        try {
            estado = EstadoReparacion.valueOf(estadoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado no válido: " + estadoStr);
        }

        return reparacionRepository.findByEstado(estado).stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve todas las reparaciones asignadas a un mecánico.
     */
    public List<ReparacionResponseDTO> obtenerPorMecanico(Long mecanicoId) {
        return reparacionRepository.findByMecanicoId(mecanicoId).stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    /**
     * Devuelve todas las reparaciones de un vehículo.
     */
    public List<ReparacionResponseDTO> obtenerPorVehiculo(Long vehiculoId) {
        return reparacionRepository.findByVehiculoId(vehiculoId).stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    // --- Método auxiliar: valida que la transición de estado sea lógica ---
    private void validarTransicionEstado(EstadoReparacion actual, EstadoReparacion nuevo) {
        // No se puede retroceder en el flujo
        if (actual.ordinal() > nuevo.ordinal()) {
            throw new IllegalArgumentException(
                    "No se puede cambiar de " + actual + " a " + nuevo
                            + ". El flujo es: PENDIENTE → EN_PROCESO → FINALIZADA → ENTREGADA");
        }
        // No se puede cambiar si ya está entregada
        if (actual == EstadoReparacion.ENTREGADA) {
            throw new IllegalArgumentException(
                    "La reparación ya está ENTREGADA, no se puede modificar su estado.");
        }
    }

    // --- Método auxiliar: convierte entidad a DTO ---
    private ReparacionResponseDTO convertirAResponseDTO(Reparacion reparacion, String mensaje) {
        return new ReparacionResponseDTO(
                reparacion.getId(),
                reparacion.getLocalizador(),
                reparacion.getVehiculo().getMatricula(),
                reparacion.getVehiculo().getMarca(),
                reparacion.getVehiculo().getModelo(),
                reparacion.getVehiculo().getCliente().getNombre(),
                reparacion.getVehiculo().getCliente().getTelefono(),
                reparacion.getMecanico() != null ? reparacion.getMecanico().getNombre() : "Sin asignar",
                reparacion.getRecepcionista().getNombre(),
                reparacion.getFechaEntrada(),
                reparacion.getFechaSalida(),
                reparacion.getEstado().name(),
                reparacion.getDescripcionAveria(),
                mensaje
        );
    }
}
