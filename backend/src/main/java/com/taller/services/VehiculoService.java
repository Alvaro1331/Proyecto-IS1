package com.taller.services;

import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.models.Cliente;
import com.taller.models.Vehiculo;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Registra un nuevo vehículo en el sistema.
     * Crea automáticamente el cliente si no existe por su teléfono,
     * o asocia el vehículo al cliente existente.
     */
    public VehiculoResponseDTO registrarVehiculo(VehiculoRequestDTO dto) {
        if (dto.getMatricula() == null || dto.getMatricula().isBlank()) {
            throw new IllegalArgumentException("La matrícula no puede estar vacía.");
        }
        String matricula = dto.getMatricula().toUpperCase().trim();

        if (vehiculoRepository.existsByMatricula(matricula)) {
            throw new IllegalArgumentException("Ya existe un vehículo con la matrícula: " + matricula);
        }

        Integer ano = dto.getAno();
        if (ano == null || ano < 1900) {
            throw new IllegalArgumentException("El año del vehículo debe ser válido y posterior a 1900.");
        }

        String clienteNombre = dto.getClienteNombre();
        if (clienteNombre == null || clienteNombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío.");
        }

        String clienteTelefono = dto.getClienteTelefono();
        if (clienteTelefono == null || clienteTelefono.isBlank()) {
            throw new IllegalArgumentException("El teléfono del cliente no puede estar vacío.");
        }

        final String nom = clienteNombre.trim();
        final String tel = clienteTelefono.trim();

        // Buscar cliente existente por teléfono o crearlo nuevo
        Cliente cliente = clienteRepository.findByTelefono(tel)
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente(nom, tel);
                    return clienteRepository.save(nuevoCliente);
                });

        Vehiculo vehiculo = new Vehiculo(
                matricula,
                dto.getMarca().trim(),
                dto.getModelo().trim(),
                ano,
                cliente
        );
        vehiculoRepository.save(vehiculo);

        return convertirAResponseDTO(vehiculo, "Vehículo registrado correctamente.");
    }

    /**
     * Devuelve todos los vehículos del sistema.
     */
    public List<VehiculoResponseDTO> obtenerTodos() {
        return vehiculoRepository.findAll().stream()
                .map(v -> convertirAResponseDTO(v, null))
                .collect(Collectors.toList());
    }

    /**
     * Busca un vehículo por su ID.
     */
    public VehiculoResponseDTO obtenerPorId(Long id) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con ID: " + id));
        return convertirAResponseDTO(vehiculo, null);
    }

    /**
     * Busca un vehículo por su matrícula.
     */
    public VehiculoResponseDTO obtenerPorMatricula(String matricula) {
        Vehiculo vehiculo = vehiculoRepository.findByMatricula(matricula.toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró un vehículo con la matrícula: " + matricula));
        return convertirAResponseDTO(vehiculo, null);
    }

    // --- Método auxiliar: convierte entidad a DTO de respuesta ---
    private VehiculoResponseDTO convertirAResponseDTO(Vehiculo vehiculo, String mensaje) {
        return new VehiculoResponseDTO(
                vehiculo.getId(),
                vehiculo.getMatricula(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAno(),
                vehiculo.getCliente().getNombre(),
                vehiculo.getCliente().getTelefono(),
                mensaje
        );
    }
}
