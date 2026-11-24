package com.taller.services;

import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.models.CitaPrevia;
import com.taller.models.Cliente;
import com.taller.models.Reparacion;
import com.taller.models.Vehiculo;
import com.taller.repositories.CitaPreviaRepository;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VehiculoService {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired(required = false)
    private ReparacionRepository reparacionRepository;

    @Autowired(required = false)
    private CitaPreviaRepository citaPreviaRepository;

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

    /**
     * HU #61: Modifica matrícula, marca, modelo y año de fabricación de un vehículo registrado.
     */
    @Transactional
    public VehiculoResponseDTO actualizarVehiculo(Long id, String matricula, String marca,
                                                  String modelo, Integer ano) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con ID: " + id));

        if (matricula == null || matricula.trim().isEmpty()) {
            throw new IllegalArgumentException("La matrícula no puede estar vacía.");
        }
        String matNorm = matricula.trim().toUpperCase();
        Optional<Vehiculo> otroConMat = vehiculoRepository.findByMatricula(matNorm);
        if (otroConMat.isPresent() && !otroConMat.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro vehículo registrado con la matrícula: " + matNorm);
        }

        if (marca == null || marca.trim().isEmpty()) {
            throw new IllegalArgumentException("La marca del vehículo no puede estar vacía.");
        }
        if (modelo == null || modelo.trim().isEmpty()) {
            throw new IllegalArgumentException("El modelo del vehículo no puede estar vacío.");
        }
        int anioActual = LocalDate.now().getYear();
        if (ano == null || ano < 1900 || ano > anioActual) {
            throw new IllegalArgumentException("El año de fabricación debe estar comprendido entre 1900 y " + anioActual + ".");
        }

        vehiculo.setMatricula(matNorm);
        vehiculo.setMarca(marca.trim());
        vehiculo.setModelo(modelo.trim());
        vehiculo.setAno(ano);
        vehiculoRepository.save(vehiculo);

        return convertirAResponseDTO(vehiculo, "Vehículo actualizado correctamente.");
    }

    /**
     * HU #61: Elimina un vehículo si no tiene órdenes de reparación ni citas previas asociadas.
     */
    @Transactional
    public Map<String, Object> eliminarVehiculo(Long id) {
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con ID: " + id));

        if (reparacionRepository != null) {
            List<Reparacion> reps = reparacionRepository.findByVehiculoId(id);
            if (reps != null && !reps.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar el vehículo " + vehiculo.getMatricula()
                        + " porque tiene " + reps.size() + " orden(es) de reparación en el historial del taller.");
            }
        }
        if (citaPreviaRepository != null) {
            List<CitaPrevia> citas = citaPreviaRepository.findByVehiculoId(id);
            if (citas != null && !citas.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar el vehículo " + vehiculo.getMatricula()
                        + " porque tiene " + citas.size() + " cita(s) previa(s) asociada(s).");
            }
        }

        vehiculoRepository.delete(vehiculo);
        return Map.of(
                "id", id,
                "mensaje", "Vehículo " + vehiculo.getMatricula() + " eliminado correctamente."
        );
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
