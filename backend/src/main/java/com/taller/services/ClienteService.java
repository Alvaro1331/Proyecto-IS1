package com.taller.services;

import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.ReparacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Autowired
    private PasswordSecurityService passwordSecurityService = new PasswordSecurityService();

    @Transactional
    public Map<String, Object> registrarCliente(String nombre, String telefono, String email, String password) {
        String emailLimpio = email != null ? email.trim() : "";
        String telefonoLimpio = telefono != null ? telefono.trim() : "";
        String nombreLimpio = nombre != null ? nombre.trim() : "";

        if (clienteRepository.findByEmail(emailLimpio).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con el correo: " + emailLimpio);
        }

        var clienteExistente = clienteRepository.findByTelefono(telefonoLimpio);

        if (clienteExistente.isPresent()) {
            Cliente cliente = clienteExistente.get();
            if (cliente.tieneCuenta()) {
                throw new IllegalArgumentException("Ya existe una cuenta asociada a este teléfono.");
            }
            passwordSecurityService.validarFortaleza(password);
            cliente.setEmail(emailLimpio);
            cliente.setPassword(passwordSecurityService.hashear(password));
            cliente.setNombre(nombreLimpio);
            int nuevasVinculadas = vincularReparacionesDeVehiculosPropios(cliente);
            clienteRepository.save(cliente);
            int totalVinculadas = cliente.getReparacionesVinculadas() != null
                    ? cliente.getReparacionesVinculadas().size() : nuevasVinculadas;
            return Map.of(
                "id", cliente.getId(),
                "nombre", cliente.getNombre(),
                "email", cliente.getEmail(),
                "telefono", cliente.getTelefono(),
                "reparacionesVinculadas", totalVinculadas,
                "mensaje", "Cuenta creada correctamente. Se ha vinculado a tus vehículos y reparaciones existentes (" + totalVinculadas + ")."
            );
        }

        passwordSecurityService.validarFortaleza(password);
        Cliente nuevoCliente = new Cliente(nombreLimpio, telefonoLimpio, emailLimpio, passwordSecurityService.hashear(password));
        clienteRepository.save(nuevoCliente);
        return Map.of(
            "id", nuevoCliente.getId(),
            "nombre", nuevoCliente.getNombre(),
            "email", nuevoCliente.getEmail(),
            "telefono", nuevoCliente.getTelefono(),
            "reparacionesVinculadas", 0,
            "mensaje", "Cuenta creada correctamente."
        );
    }

    public Map<String, Object> loginCliente(String emailOrPhone, String password) {
        var porEmail = clienteRepository.findByEmail(emailOrPhone);
        if (porEmail.isPresent()) {
            return autenticarCliente(porEmail.get(), password);
        }
        var porTelefono = clienteRepository.findByTelefono(emailOrPhone);
        if (porTelefono.isPresent()) {
            return autenticarCliente(porTelefono.get(), password);
        }
        throw new IllegalArgumentException("Usuario no encontrado: no existe ninguna cuenta con ese correo o teléfono.");
    }

    private Map<String, Object> autenticarCliente(Cliente c, String password) {
        if (!c.tieneCuenta()) {
            throw new IllegalArgumentException("Este cliente no tiene cuenta. Regístrate primero.");
        }
        if (!passwordSecurityService.coincide(password, c.getPassword())) {
            throw new IllegalArgumentException("Contraseña incorrecta.");
        }
        if (!passwordSecurityService.estaHasheada(c.getPassword())) {
            c.setPassword(passwordSecurityService.hashear(password));
            clienteRepository.save(c);
        }
        return clienteToLoginResponse(c);
    }

    @Transactional
    public Map<String, Object> vincularLocalizador(Long clienteId, String codigoLocalizador) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        if (!cliente.tieneCuenta()) {
            throw new IllegalArgumentException("El cliente no tiene cuenta registrada.");
        }

        Reparacion reparacion = reparacionRepository.findByLocalizador(codigoLocalizador.toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ninguna reparación con el código: " + codigoLocalizador));

        if (cliente.getReparacionesVinculadas() == null) {
            cliente.setReparacionesVinculadas(new HashSet<>());
        }
        boolean yaEsta = cliente.getReparacionesVinculadas().stream()
                .anyMatch(r -> reparacion.getId() != null && reparacion.getId().equals(r.getId()));
        if (!yaEsta) {
            cliente.getReparacionesVinculadas().add(reparacion);
            clienteRepository.save(cliente);
        }

        return Map.of(
            "mensaje", "Reparación vinculada correctamente.",
            "localizador", reparacion.getLocalizador(),
            "estado", reparacion.getEstado().name(),
            "descripcion", reparacion.getDescripcionAveria()
        );
    }

    /**
     * Obtiene el listado enriquecido de reparaciones vinculadas a la cuenta del cliente,
     * auto-vinculando previamente cualquier reparación de sus vehículos propios para que
     * no necesite pegar el código localizador manualmente.
     */
    @Transactional
    public List<Map<String, Object>> obtenerMisReparaciones(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        if (vincularReparacionesDeVehiculosPropios(cliente) > 0) {
            clienteRepository.save(cliente);
        }

        if (cliente.getReparacionesVinculadas() == null) {
            return List.of();
        }

        return cliente.getReparacionesVinculadas().stream()
                .sorted(Comparator.comparing(Reparacion::getId, Comparator.nullsLast(Comparator.reverseOrder())))
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
                    map.put("presupuestoFormal", dto.getPresupuestoFormal());
                    map.put("estadoPresupuesto", dto.getEstadoPresupuesto());
                    map.put("dniCifCliente", dto.getDniCifCliente());
                    map.put("direccionFiscalCliente", dto.getDireccionFiscalCliente());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private int vincularReparacionesDeVehiculosPropios(Cliente cliente) {
        if (cliente == null || cliente.getId() == null || reparacionRepository == null) {
            return 0;
        }
        List<Reparacion> propias = reparacionRepository.findByVehiculoClienteId(cliente.getId());
        if (propias == null || propias.isEmpty()) {
            return 0;
        }
        if (cliente.getReparacionesVinculadas() == null) {
            cliente.setReparacionesVinculadas(new HashSet<>());
        }
        Set<Long> idsYaVinculados = cliente.getReparacionesVinculadas().stream()
                .map(Reparacion::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        int anadidas = 0;
        for (Reparacion rep : propias) {
            Long repId = rep.getId();
            if (repId != null ? !idsYaVinculados.contains(repId) : !cliente.getReparacionesVinculadas().contains(rep)) {
                cliente.getReparacionesVinculadas().add(rep);
                if (repId != null) {
                    idsYaVinculados.add(repId);
                }
                anadidas++;
            }
        }
        return anadidas;
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
