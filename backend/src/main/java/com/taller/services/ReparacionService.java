package com.taller.services;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ReparacionMaterialRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.UsuarioRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReparacionService {

    public static final double FONDO_INICIAL_TALLER = 2000.00;
    private double gastosComprasStock = 0.0;

    public synchronized void registrarGastoCompraStock(double importe) {
        if (importe > 0) {
            this.gastosComprasStock += importe;
        }
    }

    public double getGastosComprasStock() {
        return gastosComprasStock;
    }

    @Autowired
    private ReparacionRepository reparacionRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ReparacionMaterialRepository reparacionMaterialRepository;

    /**
     * Crea una nueva reparación para un vehículo.
     * Se crea en estado PENDIENTE y sin mecánico asignado, con el precio acordado con el cliente
     * y el presupuesto estimado pensado por recepción para arreglarlo.
     */
    public ReparacionResponseDTO crearReparacion(ReparacionRequestDTO dto) {
        Vehiculo vehiculo = vehiculoRepository.findById(dto.getVehiculoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el vehículo con ID: " + dto.getVehiculoId()));

        Usuario recepcionista = usuarioRepository.findById(dto.getRecepcionistaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró el usuario con ID: " + dto.getRecepcionistaId()));

        if (recepcionista.getRol() != Rol.RECEPCIONISTA && recepcionista.getRol() != Rol.ADMIN) {
            throw new IllegalArgumentException(
                    "El usuario " + recepcionista.getNombre() + " no tiene permisos de recepcionista.");
        }

        Double precioCliente = dto.getPresupuesto();
        if (precioCliente != null && precioCliente <= 0) {
            throw new IllegalArgumentException("El precio acordado con el cliente debe ser mayor que 0 €.");
        }
        if (precioCliente == null) {
            precioCliente = 200.0;
        }

        Double presupuestoEstimado = dto.getPresupuestoEstimado();
        if (presupuestoEstimado == null) {
            presupuestoEstimado = Math.round(precioCliente * 50.0) / 100.0;
        }
        if (presupuestoEstimado <= 0) {
            throw new IllegalArgumentException("El presupuesto estimado para arreglarlo debe ser mayor que 0 €.");
        }
        if (presupuestoEstimado >= precioCliente) {
            throw new IllegalArgumentException(String.format(
                    "El presupuesto estimado para el arreglo (%.2f €) debe ser menor que el precio cobrado al cliente (%.2f €) para que el taller tenga ganancias.",
                    presupuestoEstimado, precioCliente));
        }

        Double presupuestoMecanico = dto.getPresupuestoMecanico() != null
                ? dto.getPresupuestoMecanico()
                : presupuestoEstimado;

        Reparacion reparacion = new Reparacion(
                vehiculo,
                recepcionista,
                dto.getDescripcionAveria(),
                precioCliente,
                presupuestoEstimado,
                presupuestoMecanico
        );
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Reparación creada correctamente. Localizador: " + reparacion.getLocalizador());
    }

    /**
     * Busca una reparación por su código localizador.
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
     * El Administrador establece o modifica el presupuesto máximo que el mecánico puede gastar en la reparación.
     */
    public ReparacionResponseDTO actualizarPresupuesto(Long reparacionId, Double nuevoPresupuestoMecanico) {
        if (nuevoPresupuestoMecanico == null || nuevoPresupuestoMecanico <= 0) {
            throw new IllegalArgumentException("El presupuesto para el mecánico debe ser mayor que 0 €.");
        }

        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        double precioCliente = reparacion.getPresupuesto() != null ? reparacion.getPresupuesto() : 200.0;
        if (nuevoPresupuestoMecanico >= precioCliente) {
            throw new IllegalArgumentException(String.format(
                    "El presupuesto del mecánico (%.2f €) debe ser menor que el precio acordado con el cliente (%.2f €) para asegurar ganancia al taller.",
                    nuevoPresupuestoMecanico, precioCliente));
        }

        double costeMateriales = calcularCosteMateriales(reparacionId);
        if (nuevoPresupuestoMecanico + 0.001 < costeMateriales) {
            throw new IllegalArgumentException(String.format(
                    "El nuevo presupuesto para el mecánico (%.2f €) no puede ser inferior a lo ya gastado en materiales (%.2f €).",
                    nuevoPresupuestoMecanico, costeMateriales));
        }

        reparacion.setPresupuestoMecanico(nuevoPresupuestoMecanico);
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                String.format("Presupuesto del mecánico fijado por el Administrador en %.2f €.", nuevoPresupuestoMecanico));
    }

    /**
     * Actualiza el estado de una reparación siguiendo el flujo:
     * PENDIENTE → EN_PROCESO → FINALIZADA (mecánico acaba) → CONFIRMADA (admin confirma) → ENTREGADA (recepcionista entrega y cobra).
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
                    + ". Los estados válidos son: PENDIENTE, EN_PROCESO, FINALIZADA, CONFIRMADA, ENTREGADA");
        }

        validarTransicionEstado(reparacion.getEstado(), nuevoEstado);

        if (nuevoEstado != EstadoReparacion.PENDIENTE && reparacion.getMecanico() == null) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el estado a " + nuevoEstado + " porque la reparación no tiene ningún mecánico asignado.");
        }

        reparacion.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoReparacion.FINALIZADA
                || nuevoEstado == EstadoReparacion.CONFIRMADA
                || nuevoEstado == EstadoReparacion.ENTREGADA) {
            reparacion.setFechaSalida(LocalDate.now());
        }

        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Estado actualizado a " + nuevoEstado.name() + ".");
    }

    /**
     * Devuelve el resumen financiero de la Cuenta General del Taller (Tesorería del Admin).
     */
    public Map<String, Object> obtenerCuentaTaller() {
        List<Reparacion> todas = reparacionRepository.findAll();
        List<ReparacionMaterial> todosUsos = reparacionMaterialRepository.findAll();

        double gastosPiezasUsadas = todosUsos.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
        double gastosMateriales = gastosPiezasUsadas + gastosComprasStock;

        double ingresosCobrados = todas.stream()
                .filter(r -> r.getEstado() == EstadoReparacion.ENTREGADA)
                .mapToDouble(r -> r.getPresupuesto() != null ? r.getPresupuesto() : 200.0)
                .sum();

        double ingresosEnCurso = todas.stream()
                .filter(r -> r.getEstado() != EstadoReparacion.ENTREGADA)
                .mapToDouble(r -> r.getPresupuesto() != null ? r.getPresupuesto() : 200.0)
                .sum();

        double beneficioTotal = todas.stream()
                .mapToDouble(r -> {
                    double pres = r.getPresupuesto() != null ? r.getPresupuesto() : 200.0;
                    return pres - calcularCosteMateriales(r.getId());
                })
                .sum();

        double saldoActual = FONDO_INICIAL_TALLER - gastosMateriales + ingresosCobrados;

        Map<String, Object> cuenta = new LinkedHashMap<>();
        cuenta.put("fondoInicial", FONDO_INICIAL_TALLER);
        cuenta.put("gastosMateriales", gastosMateriales);
        cuenta.put("ingresosCobrados", ingresosCobrados);
        cuenta.put("ingresosEnCurso", ingresosEnCurso);
        cuenta.put("beneficioTotal", beneficioTotal);
        cuenta.put("saldoActual", saldoActual);
        return cuenta;
    }

    public double calcularCosteMateriales(Long reparacionId) {
        if (reparacionId == null) return 0.0;
        return reparacionMaterialRepository.findByReparacionId(reparacionId).stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
    }

    public List<ReparacionResponseDTO> obtenerTodas() {
        return reparacionRepository.findAll().stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    public ReparacionResponseDTO obtenerPorId(Long id) {
        Reparacion reparacion = reparacionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + id));
        return convertirAResponseDTO(reparacion, null);
    }

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

    public List<ReparacionResponseDTO> obtenerPorMecanico(Long mecanicoId) {
        return reparacionRepository.findByMecanicoId(mecanicoId).stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    public List<ReparacionResponseDTO> obtenerPorVehiculo(Long vehiculoId) {
        return reparacionRepository.findByVehiculoId(vehiculoId).stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    private void validarTransicionEstado(EstadoReparacion actual, EstadoReparacion nuevo) {
        if (actual == EstadoReparacion.ENTREGADA) {
            throw new IllegalArgumentException(
                    "La reparación ya está ENTREGADA, no se puede modificar su estado.");
        }
        if (actual.ordinal() > nuevo.ordinal()) {
            throw new IllegalArgumentException(
                    "No se puede cambiar de " + actual + " a " + nuevo
                            + ". El flujo es: PENDIENTE → EN_PROCESO → FINALIZADA → CONFIRMADA → ENTREGADA");
        }
        if (actual == EstadoReparacion.EN_PROCESO && nuevo == EstadoReparacion.CONFIRMADA) {
            throw new IllegalArgumentException(
                    "Primero el mecánico debe dar por finalizada la reparación (FINALIZADA) antes de que el Administrador la confirme.");
        }
        if (actual == EstadoReparacion.EN_PROCESO && nuevo == EstadoReparacion.ENTREGADA) {
            throw new IllegalArgumentException(
                    "No se puede entregar el vehículo directamente desde EN_PROCESO: primero el mecánico debe finalizarla y el Administrador confirmarla.");
        }
        if (actual == EstadoReparacion.FINALIZADA && nuevo == EstadoReparacion.ENTREGADA) {
            throw new IllegalArgumentException(
                    "El Administrador debe confirmar primero que la reparación es correcta (estado CONFIRMADA) antes de que Recepción pueda entregar el vehículo y cobrar.");
        }
    }

    private ReparacionResponseDTO convertirAResponseDTO(Reparacion reparacion, String mensaje) {
        List<ReparacionMaterial> usados = reparacionMaterialRepository.findByReparacionId(reparacion.getId());
        double costeMateriales = usados.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
        double precioCliente = reparacion.getPresupuesto() != null ? reparacion.getPresupuesto() : 200.0;
        double presupuestoEstimado = reparacion.getPresupuestoEstimado() != null
                ? reparacion.getPresupuestoEstimado()
                : Math.round(precioCliente * 50.0) / 100.0;
        double presupuestoMecanico = reparacion.getPresupuestoMecanico() != null
                ? reparacion.getPresupuestoMecanico()
                : presupuestoEstimado;
        double presupuestoRestante = Math.max(0.0, presupuestoMecanico - costeMateriales);
        double beneficio = precioCliente - costeMateriales;
        List<String> materialesDetalle = usados.stream()
                .map(u -> String.format("%dx %s (%.2f €)",
                        u.getCantidad(),
                        u.getMaterial().getNombre(),
                        u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0)))
                .collect(Collectors.toList());

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
                precioCliente,
                presupuestoEstimado,
                presupuestoMecanico,
                costeMateriales,
                presupuestoRestante,
                beneficio,
                materialesDetalle,
                mensaje
        );
    }
}
