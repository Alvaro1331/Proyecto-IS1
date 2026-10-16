package com.taller.services;

import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de cuentas y operaciones de clientes.
 * Permite el registro de cuentas de clientes, autenticación mediante
 * email o teléfono, vinculación de reparaciones mediante código localizador
 * y consulta de reparaciones vinculadas a la cuenta.
 */
@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ReparacionRepository reparacionRepository;

    /**
     * Registra una nueva cuenta de cliente en el sistema.
     * Valida que el email no esté ya en uso y que no haya duplicados conflictivos.
     * Si ya existe un cliente con ese número de teléfono (creado previamente por un recepcionista
     * al registrar un vehículo), se actualiza asociándole las credenciales (email y contraseña).
     * Si el cliente no existe previamente, se crea uno nuevo con su cuenta.
     *
     * @param nombre Nombre del cliente
     * @param telefono Teléfono del cliente
     * @param email Correo electrónico del cliente
     * @param password Contraseña de acceso
     * @return Mapa con los datos del cliente y mensaje de confirmación
     */
    public Map<String, Object> registrarCliente(String nombre, String telefono, String email, String password) {
        // Verificar si el correo ya está en uso
        if (clienteRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo: " + email);
        }

        // Comprobar si existe un cliente con ese teléfono (creado previamente por el recepcionista)
        var clienteExistente = clienteRepository.findByTelefono(telefono);
        
        if (clienteExistente.isPresent()) {
            Cliente cliente = clienteExistente.get();
            if (cliente.tieneCuenta()) {
                throw new IllegalArgumentException("Ya existe una cuenta asociada a este teléfono.");
            }
            // Actualización: vincular credenciales de cuenta al cliente existente
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

        // Crear nuevo cliente con cuenta
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

    /**
     * Autentica a un cliente utilizando su correo electrónico o teléfono junto con la contraseña.
     *
     * @param emailOrPhone Correo electrónico o teléfono del cliente
     * @param password Contraseña de acceso
     * @return Mapa con los datos del cliente autenticado y su rol (CLIENTE)
     */
    public Map<String, Object> loginCliente(String emailOrPhone, String password) {
        // Intentar primero búsqueda por email
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
        // Intentar búsqueda por teléfono
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
        throw new IllegalArgumentException("No se encontró ninguna cuenta con esos datos.");
    }

    /**
     * Vincula una reparación a la cuenta del cliente a través del código localizador.
     *
     * @param clienteId ID del cliente
     * @param codigoLocalizador Código alfanumérico identificador de la reparación
     * @return Mapa con los detalles de la reparación vinculada y mensaje de éxito
     */
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
     * Obtiene el listado de reparaciones vinculadas a la cuenta del cliente.
     *
     * @param clienteId ID del cliente
     * @return Lista de reparaciones con sus datos descriptivos y estado
     */
    public List<Map<String, Object>> obtenerMisReparaciones(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
        
        return cliente.getReparacionesVinculadas().stream()
                .map(r -> Map.<String, Object>of(
                    "localizador", r.getLocalizador(),
                    "matricula", r.getVehiculo().getMatricula(),
                    "marca", r.getVehiculo().getMarca(),
                    "modelo", r.getVehiculo().getModelo(),
                    "estado", r.getEstado().name(),
                    "descripcionAveria", r.getDescripcionAveria(),
                    "fechaEntrada", r.getFechaEntrada().toString(),
                    "mecanico", r.getMecanico() != null ? r.getMecanico().getNombre() : "Sin asignar"
                ))
                .collect(Collectors.toList());
    }

    /**
     * Mapea un objeto Cliente al formato de respuesta para el inicio de sesión.
     *
     * @param c Entidad Cliente
     * @return Mapa con los datos esenciales de sesión
     */
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
