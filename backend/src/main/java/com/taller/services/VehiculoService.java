package com.taller.services;

import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.models.Cliente;
import com.taller.models.Vehiculo;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Contiene la lógica de negocio para la gestión de vehículos.
 * El controlador llama a estos métodos, nunca accede directamente a la base de datos.
 */
@Service
public class VehiculoService {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Registra un nuevo vehículo en el sistema.
     *
     * Si el cliente (identificado por teléfono) ya existe, reutiliza sus datos.
     * Si no existe, lo crea automáticamente.
     * Si la matrícula ya está registrada, lanza un error.
     *
     * @param dto Los datos que envió el frontend.
     * @return Un DTO con los datos del vehículo registrado y un mensaje de confirmación.
     */
    public VehiculoResponseDTO registrarVehiculo(VehiculoRequestDTO dto) {

        // 1. Comprobar si la matrícula ya existe
        if (vehiculoRepository.existsByMatricula(dto.getMatricula())) {
            throw new IllegalArgumentException("Ya existe un vehículo con la matrícula: " + dto.getMatricula());
        }

        // 2. Buscar si el cliente ya existe por teléfono, o crear uno nuevo
        Cliente cliente = clienteRepository.findByTelefono(dto.getClienteTelefono())
                .orElseGet(() -> {
                    Cliente nuevoCliente = new Cliente(dto.getClienteNombre(), dto.getClienteTelefono());
                    return clienteRepository.save(nuevoCliente);
                });

        // 3. Crear y guardar el nuevo vehículo
        Vehiculo vehiculo = new Vehiculo(
                dto.getMatricula().toUpperCase(),
                dto.getMarca(),
                dto.getModelo(),
                dto.getAno(),
                cliente
        );
        vehiculoRepository.save(vehiculo);

        // 4. Devolver la respuesta con mensaje de éxito
        return new VehiculoResponseDTO(
                vehiculo.getId(),
                vehiculo.getMatricula(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAno(),
                cliente.getNombre(),
                cliente.getTelefono(),
                "Vehículo registrado correctamente."
        );
    }
}
