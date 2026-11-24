package com.taller.services;

import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.CitaPreviaRepository;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    @Autowired(required = false)
    private VehiculoRepository vehiculoRepository;

    @Autowired(required = false)
    private CitaPreviaRepository citaPreviaRepository;

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

    /**
     * HU #61: Actualiza los datos de contacto y fiscales de la ficha de un cliente
     * desde recepción o administración, garantizando unicidad de teléfono y correo.
     */
    @Transactional
    public Map<String, Object> actualizarCliente(Long id, String nombre, String telefono,
                                                 String email, String dniCif, String direccionFiscal) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente con ID: " + id));

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente no puede estar vacío.");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente no puede estar vacío.");
        }

        String telLimpio = telefono.trim();
        Optional<Cliente> otroConTel = clienteRepository.findByTelefono(telLimpio);
        if (otroConTel.isPresent() && !otroConTel.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro cliente registrado con el teléfono: " + telLimpio);
        }

        if (email != null && !email.trim().isEmpty()) {
            String emailLimpio = email.trim().toLowerCase();
            if (!emailLimpio.contains("@") || emailLimpio.contains(" ")) {
                throw new IllegalArgumentException("El formato del correo electrónico no es válido.");
            }
            Optional<Cliente> otroConEmail = clienteRepository.findByEmail(emailLimpio);
            if (otroConEmail.isPresent() && !otroConEmail.get().getId().equals(id)) {
                throw new IllegalArgumentException("Ya existe otro cliente registrado con el correo: " + emailLimpio);
            }
            cliente.setEmail(emailLimpio);
        } else if (email != null && !cliente.tieneCuenta()) {
            cliente.setEmail(null);
        }

        cliente.setNombre(nombre.trim());
        cliente.setTelefono(telLimpio);
        if (dniCif != null) {
            cliente.setDniCif(dniCif.trim().isEmpty() ? null : dniCif.trim().toUpperCase());
        }
        if (direccionFiscal != null) {
            cliente.setDireccionFiscal(direccionFiscal.trim().isEmpty() ? null : direccionFiscal.trim());
        }

        clienteRepository.save(cliente);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", cliente.getId());
        res.put("nombre", cliente.getNombre());
        res.put("telefono", cliente.getTelefono());
        res.put("email", cliente.getEmail() != null ? cliente.getEmail() : "");
        res.put("dniCif", cliente.getDniCif() != null ? cliente.getDniCif() : "");
        res.put("direccionFiscal", cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : "");
        res.put("mensaje", "Ficha del cliente actualizada correctamente.");
        return res;
    }

    /**
     * HU #61: Elimina una ficha de cliente siempre que no tenga historial de reparaciones,
     * citas previas ni vehículos registrados en el sistema.
     */
    @Transactional
    public Map<String, Object> eliminarCliente(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente con ID: " + id));

        if (reparacionRepository != null) {
            List<Reparacion> reps = reparacionRepository.findByVehiculoClienteId(id);
            if (reps != null && !reps.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar al cliente '" + cliente.getNombre()
                        + "' porque tiene " + reps.size() + " orden(es) de reparación asociadas en el historial del taller.");
            }
        }
        if (cliente.getReparacionesVinculadas() != null && !cliente.getReparacionesVinculadas().isEmpty()) {
            throw new IllegalStateException("No se puede eliminar al cliente '" + cliente.getNombre()
                    + "' porque tiene reparaciones vinculadas en su historial.");
        }
        if (citaPreviaRepository != null) {
            List<CitaPrevia> citas = citaPreviaRepository.findByClienteIdOrderByFechaDescFranjaHorariaAsc(id);
            if (citas != null && !citas.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar al cliente '" + cliente.getNombre()
                        + "' porque tiene " + citas.size() + " cita(s) previa(s) registrada(s) en el sistema.");
            }
        }
        if (vehiculoRepository != null) {
            List<Vehiculo> vehiculos = vehiculoRepository.findByClienteId(id);
            if (vehiculos != null && !vehiculos.isEmpty()) {
                throw new IllegalStateException("No se puede eliminar al cliente '" + cliente.getNombre()
                        + "' porque tiene " + vehiculos.size() + " vehículo(s) asociado(s). Elimina primero sus vehículos.");
            }
        }

        clienteRepository.delete(cliente);
        return Map.of(
                "id", id,
                "mensaje", "Cliente '" + cliente.getNombre() + "' eliminado correctamente."
        );
    }

    /**
     * HU #67: Permite al propio cliente actualizar sus datos de contacto (nombre, email, teléfono)
     * y sus datos fiscales (dniCif en mayúsculas y direccionFiscal) desde su área personal.
     */
    @Transactional
    public Map<String, Object> actualizarPerfilCliente(Long id, String nombre, String email,
                                                       String telefono, String dniCif,
                                                       String direccionFiscal) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente con ID: " + id));

        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        }
        if (telefono == null || telefono.trim().isEmpty()) {
            throw new IllegalArgumentException("El teléfono no puede estar vacío.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }

        String emailLimpio = email.trim().toLowerCase();
        int at = emailLimpio.indexOf('@');
        if (emailLimpio.contains(" ") || at <= 0 || at != emailLimpio.lastIndexOf('@')
                || emailLimpio.substring(at + 1).indexOf('.') <= 0 || emailLimpio.endsWith(".")) {
            throw new IllegalArgumentException("El formato del correo electrónico no es válido.");
        }

        String telLimpio = telefono.trim();
        Optional<Cliente> otroConEmail = clienteRepository.findByEmail(emailLimpio);
        if (otroConEmail.isPresent() && !otroConEmail.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro cliente registrado con el correo: " + emailLimpio);
        }

        Optional<Cliente> otroConTel = clienteRepository.findByTelefono(telLimpio);
        if (otroConTel.isPresent() && !otroConTel.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro cliente registrado con el teléfono: " + telLimpio);
        }

        cliente.setNombre(nombre.trim());
        cliente.setEmail(emailLimpio);
        cliente.setTelefono(telLimpio);
        cliente.setDniCif(dniCif != null && !dniCif.trim().isEmpty() ? dniCif.trim().toUpperCase() : null);
        cliente.setDireccionFiscal(direccionFiscal != null && !direccionFiscal.trim().isEmpty() ? direccionFiscal.trim() : null);
        clienteRepository.save(cliente);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", cliente.getId());
        res.put("nombre", cliente.getNombre());
        res.put("email", cliente.getEmail());
        res.put("telefono", cliente.getTelefono());
        res.put("dniCif", cliente.getDniCif() != null ? cliente.getDniCif() : "");
        res.put("direccionFiscal", cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : "");
        res.put("rol", "CLIENTE");
        res.put("mensaje", "Perfil y datos fiscales actualizados correctamente.");
        return res;
    }

    /**
     * HU #67: Permite al cliente cambiar su contraseña de acceso verificando su contraseña actual,
     * validando la política mínima de seguridad (HU #64) y almacenando el nuevo hash BCrypt.
     */
    @Transactional
    public Map<String, Object> cambiarPasswordCliente(Long id, String passwordActual,
                                                      String nuevaPassword, String confirmarPassword) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente con ID: " + id));

        if (!cliente.tieneCuenta()) {
            throw new IllegalArgumentException("Este cliente no dispone de cuenta web activa.");
        }
        if (passwordActual == null || passwordActual.isBlank()) {
            throw new IllegalArgumentException("Debes introducir tu contraseña actual.");
        }
        if (!passwordSecurityService.coincide(passwordActual, cliente.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual introducida es incorrecta.");
        }
        if (nuevaPassword == null || nuevaPassword.isBlank()) {
            throw new IllegalArgumentException("Debes introducir la nueva contraseña.");
        }
        if (!nuevaPassword.equals(confirmarPassword)) {
            throw new IllegalArgumentException("La nueva contraseña y su confirmación no coinciden.");
        }

        passwordSecurityService.validarFortaleza(nuevaPassword);
        cliente.setPassword(passwordSecurityService.hashear(nuevaPassword));
        clienteRepository.save(cliente);

        return Map.of("mensaje", "Contraseña actualizada correctamente.");
    }
}
