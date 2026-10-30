package com.taller.services;

import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de cuentas y operaciones de clientes.
 * Permite el registro de cuentas de clientes, autenticación mediante
 * email o teléfono, vinculación de reparaciones mediante código localizador
 * y consulta de reparaciones vinculadas a la cuenta con su línea temporal,
 * fotos de incidencias y factura desglosada.
 */
@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private ReparacionService reparacionService;

    public Map<String, Object> registrarCliente(String nombre, String telefono, String email, String password) {
        if (clienteRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo: " + email);
        }

        var clienteExistente = clienteRepository.findByTelefono(telefono);

        if (clienteExistente.isPresent()) {
            Cliente cliente = clienteExistente.get();
            if (cliente.tieneCuenta()) {
                throw new IllegalArgumentException("Ya existe una cuenta asociada a este teléfono.");
            }
            cliente.setEmail(email);
            cliente.setPassword(password);
            cliente.setNombre(nombre);
            clienteRepository.save(cliente);
            return Map.of(
                "id", cliente.getId(),
                "nombre", cliente.getNombre(),
                "email", cliente.getEmail(),
                "telefono", cliente.getTelefono(),
                "mensaje", "Cuenta creada correctamente. Se ha vinculado a tus vehículos existentes."
            );
        }

        Cliente nuevoCliente = new Cliente(nombre, telefono, email, password);
        clienteRepository.save(nuevoCliente);
        return Map.of(
            "id", nuevoCliente.getId(),
            "nombre", nuevoCliente.getNombre(),
            "email", nuevoCliente.getEmail(),
            "telefono", nuevoCliente.getTelefono(),
            "mensaje", "Cuenta creada correctamente."
        );
    }

    public Map<String, Object> loginCliente(String emailOrPhone, String password) {
        var porEmail = clienteRepository.findByEmail(emailOrPhone);
        if (porEmail.isPresent()) {
            Cliente c = porEmail.get();
            if (!c.tieneCuenta()) {
                throw new IllegalArgumentException("Este cliente no tiene cuenta. Regístrate primero.");
            }
            if (!c.getPassword().equals(password)) {
                throw new IllegalArgumentException("Contraseña incorrecta.");
            }
            return clienteToLoginResponse(c);
        }
        var porTelefono = clienteRepository.findByTelefono(emailOrPhone);
        if (porTelefono.isPresent()) {
            Cliente c = porTelefono.get();
            if (!c.tieneCuenta()) {
                throw new IllegalArgumentException("Este cliente no tiene cuenta. Regístrate primero.");
            }
            if (!c.getPassword().equals(password)) {
                throw new IllegalArgumentException("Contraseña incorrecta.");
            }
            return clienteToLoginResponse(c);
        }
        throw new IllegalArgumentException("Usuario no encontrado: no existe ninguna cuenta con ese correo o teléfono.");
    }

    public Map<String, Object> vincularLocalizador(Long clienteId, String codigoLocalizador) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        if (!cliente.tieneCuenta()) {
            throw new IllegalArgumentException("El cliente no tiene cuenta registrada.");
        }

        Reparacion reparacion = reparacionRepository.findByLocalizador(codigoLocalizador.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ninguna reparación con el código: " + codigoLocalizador));

        cliente.getReparacionesVinculadas().add(reparacion);
        clienteRepository.save(cliente);

        return Map.of(
            "mensaje", "Reparación vinculada correctamente.",
            "localizador", reparacion.getLocalizador(),
            "estado", reparacion.getEstado().name(),
            "descripcion", reparacion.getDescripcionAveria()
        );
    }

    /**
     * Obtiene el listado enriquecido de reparaciones vinculadas a la cuenta del cliente,
     * incluyendo línea temporal, días estimados, incidencias con fotos y factura.
     */
    public List<Map<String, Object>> obtenerMisReparaciones(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        return cliente.getReparacionesVinculadas().stream()
                .map(r -> {
                    ReparacionResponseDTO dto = reparacionService.convertirAResponseDTO(r, null);
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", dto.getId());
                    map.put("localizador", dto.getLocalizador());
                    map.put("matricula", dto.getMatriculaVehiculo());
                    map.put("marca", dto.getMarcaVehiculo());
                    map.put("modelo", dto.getModeloVehiculo());
                    map.put("estado", dto.getEstado());
                    map.put("descripcionAveria", dto.getDescripcionAveria());
                    map.put("fechaEntrada", dto.getFechaEntrada() != null ? dto.getFechaEntrada().toString() : "");
                    map.put("fechaSalida", dto.getFechaSalida() != null ? dto.getFechaSalida().toString() : null);
                    map.put("fechaEstimadaFin", dto.getFechaEstimadaFin() != null ? dto.getFechaEstimadaFin().toString() : "");
                    map.put("diasEstimados", dto.getDiasEstimados());
                    map.put("diasRestantes", dto.getDiasRestantes());
                    map.put("mecanico", dto.getNombreMecanico());
                    map.put("estimacionInicial", dto.getEstimacionInicial());
                    map.put("horasTrabajoMecanico", dto.getHorasTrabajoMecanico());
                    map.put("costeMateriales", dto.getCosteMateriales());
                    map.put("materialesDetalle", dto.getMaterialesDetalle());
                    map.put("impactoIncidencias", dto.getImpactoIncidencias());
                    map.put("totalCalculadoPrevisto", dto.getTotalCalculadoPrevisto());
                    map.put("incidencias", dto.getIncidencias());
                    map.put("factura", dto.getFactura());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private Map<String, Object> clienteToLoginResponse(Cliente c) {
        return Map.of(
            "id", c.getId(),
            "nombre", c.getNombre(),
            "email", c.getEmail() != null ? c.getEmail() : "",
            "telefono", c.getTelefono(),
            "rol", "CLIENTE"
        );
    }
}
