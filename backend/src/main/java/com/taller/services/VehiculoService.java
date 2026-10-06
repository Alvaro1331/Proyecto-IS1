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
     */
    public VehiculoResponseDTO registrarVehiculo(VehiculoRequestDTO dto) {
        if (vehiculoRepository.existsByMatricula(dto.getMatricula().toUpperCase())) {
            throw new IllegalArgumentException("Ya existe un vehículo con la matrícula: " + dto.getMatricula());
        }

        Cliente cliente = clienteRepository.findByTelefono(dto.getClienteTelefono())
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente(dto.getClienteNombre(), dto.getClienteTelefono());
                    return clienteRepository.save(nuevoCliente);
                });

        Vehiculo vehiculo = new Vehiculo(
                dto.getMatricula().toUpperCase(),
                dto.getMarca(),
                dto.getModelo(),
                dto.getAno(),
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
        Vehiculo vehiculo = vehiculoRepository.findByMatricula(matricula.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con matrícula: " + matricula));
        return convertirAResponseDTO(vehiculo, null);
    }

    // --- Método auxiliar ---
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
