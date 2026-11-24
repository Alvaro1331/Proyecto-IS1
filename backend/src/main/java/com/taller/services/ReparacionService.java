package com.taller.services;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.events.NotificacionReparacionEvent;
import com.taller.models.*;
import com.taller.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReparacionService {

    public static final double FONDO_INICIAL_TALLER = 2000.00;
    private double gastosComprasStock = 0.0;
    private final List<GastoTesoreria> gastosComprasMemoria = new ArrayList<>();
    private long secuenciaGastoMemoria = 1L;

    @Autowired(required = false)
    private GastoTesoreriaRepository gastoTesoreriaRepository;

    @Autowired(required = false)
    private MaterialRepository materialRepository;

    public synchronized void registrarGastoCompraStock(double importe) {
        if (importe > 0) {
            registrarGastoCompraStock(null, "Compra de stock de materiales", 1, importe, 0, "COMPRA_STOCK");
        }
    }

    public synchronized GastoTesoreria registrarGastoCompraStock(Long materialId, String nombreMaterial,
                                                                 int cantidad, double precioUnitario,
                                                                 Integer stockPrevio, String tipo) {
        double importe = Math.round((cantidad * precioUnitario) * 100.0) / 100.0;
        if (importe <= 0 || cantidad <= 0) {
            return null;
        }
        String tipoNorm = (tipo != null && !tipo.isBlank()) ? tipo.trim().toUpperCase() : "COMPRA_STOCK";
        String nomMat = (nombreMaterial != null && !nombreMaterial.isBlank()) ? nombreMaterial.trim() : "Material";
        String concepto = "ALTA_MATERIAL".equals(tipoNorm)
                ? String.format("Alta de nuevo material: %s (%d uds × %.2f €)", nomMat, cantidad, precioUnitario)
                : String.format("Compra / reposición de stock: %s (+%d uds × %.2f €)", nomMat, cantidad, precioUnitario);

        GastoTesoreria gasto = new GastoTesoreria(
                materialId, nomMat, concepto, tipoNorm, cantidad, precioUnitario, importe, stockPrevio
        );

        if (gastoTesoreriaRepository != null) {
            gasto = gastoTesoreriaRepository.save(gasto);
        } else {
            gasto.setId(secuenciaGastoMemoria++);
            gastosComprasMemoria.add(0, gasto);
        }
        recalcularGastosComprasStockTotal();
        return gasto;
    }

    private List<GastoTesoreria> obtenerEntidadesGastosCompras() {
        if (gastoTesoreriaRepository != null) {
            return gastoTesoreriaRepository.findAllByOrderByFechaDescIdDesc();
        }
        return new ArrayList<>(gastosComprasMemoria);
    }

    private void recalcularGastosComprasStockTotal() {
        this.gastosComprasStock = Math.round(obtenerEntidadesGastosCompras().stream()
                .mapToDouble(g -> g.getImporte() != null ? g.getImporte() : 0.0)
                .sum() * 100.0) / 100.0;
    }

    public double getGastosComprasStock() {
        recalcularGastosComprasStockTotal();
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

    @Autowired
    private IncidenciaReparacionRepository incidenciaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PresupuestoReparacionRepository presupuestoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ConfiguracionTallerService configuracionService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PasswordSecurityService passwordSecurityService = new PasswordSecurityService();

    /**
     * Crea una nueva reparación en Recepción con una estimación inicial orientativa (no bloqueante)
     * y los días estimados de trabajo.
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

        Double estimacionInicial = dto.getEstimacionInicial();
        if (estimacionInicial != null && estimacionInicial <= 0) {
            throw new IllegalArgumentException("La estimación inicial orientativa debe ser mayor que 0 €.");
        }
        if (estimacionInicial == null) {
            estimacionInicial = 150.0;
        }

        Integer diasEstimados = dto.getDiasEstimados();
        if (diasEstimados != null && diasEstimados <= 0) {
            throw new IllegalArgumentException("Los días estimados deben ser al menos 1 día.");
        }
        if (diasEstimados == null) {
            diasEstimados = 3;
        }

        Reparacion reparacion = new Reparacion(
                vehiculo,
                recepcionista,
                dto.getDescripcionAveria(),
                estimacionInicial,
                diasEstimados
        );
        reparacionRepository.save(reparacion);

        if (vehiculo.getCliente() != null) {
            Cliente cli = vehiculo.getCliente();
            if (cli.tieneCuenta() && clienteRepository != null) {
                Cliente cliPersistido = cli.getId() != null
                        ? clienteRepository.findById(cli.getId()).orElse(cli)
                        : cli;
                if (cliPersistido.getReparacionesVinculadas() == null) {
                    cliPersistido.setReparacionesVinculadas(new HashSet<>());
                }
                boolean yaVinculada = cliPersistido.getReparacionesVinculadas().stream()
                        .anyMatch(r -> reparacion.getId() != null && reparacion.getId().equals(r.getId()));
                if (!yaVinculada) {
                    cliPersistido.getReparacionesVinculadas().add(reparacion);
                    clienteRepository.save(cliPersistido);
                }
            }

            if (eventPublisher != null) {
                eventPublisher.publishEvent(new NotificacionReparacionEvent(
                        cli,
                        reparacion,
                        TipoNotificacion.ENTRADA_TALLER,
                        "Vehículo recepcionado en el taller",
                        "Hemos recibido tu vehículo " + vehiculo.getMarca() + " " + vehiculo.getModelo()
                                + " (" + vehiculo.getMatricula() + "). Tu código localizador de seguimiento es: "
                                + reparacion.getLocalizador()
                ));
            }
        }

        return convertirAResponseDTO(reparacion,
                "Reparación creada correctamente con estimación orientativa de "
                        + String.format("%.2f €", estimacionInicial) + " y " + diasEstimados
                        + " día(s) estimados. Localizador: " + reparacion.getLocalizador());
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

        if (reparacion.getEstado() == EstadoReparacion.ENTREGADA || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalArgumentException(
                    "No se puede asignar un mecánico a una reparación ya cerrada (" + reparacion.getEstado() + ").");
        }

        reparacion.setMecanico(mecanico);
        Optional<PresupuestoReparacion> optPres = presupuestoRepository.findByReparacionId(reparacionId);
        boolean presupuestoAceptado = optPres.isPresent() && "ACEPTADO".equalsIgnoreCase(optPres.get().getEstado());
        if (reparacion.getEstado() == EstadoReparacion.PENDIENTE && presupuestoAceptado) {
            reparacion.setEstado(EstadoReparacion.EN_PROCESO);
        }
        reparacionRepository.save(reparacion);

        String msg = presupuestoAceptado
                ? "Mecánico " + mecanico.getNombre() + " asignado. El presupuesto ya está aceptado, por lo que la reparación pasa a EN PROCESO."
                : "Mecánico " + mecanico.getNombre() + " asignado para diagnóstico. La reparación permanece PENDIENTE hasta que se emita el presupuesto y el cliente lo acepte.";
        return convertirAResponseDTO(reparacion, msg);
    }

    public boolean tienePresupuestoAceptado(Long reparacionId) {
        return presupuestoRepository.findByReparacionId(reparacionId)
                .map(p -> "ACEPTADO".equalsIgnoreCase(p.getEstado()))
                .orElse(false);
    }

    /**
     * Actualiza la estimación orientativa de una reparación si se desea ajustar manualmente.
     */
    public ReparacionResponseDTO actualizarPresupuesto(Long reparacionId, Double nuevaEstimacion) {
        if (nuevaEstimacion == null || nuevaEstimacion <= 0) {
            throw new IllegalArgumentException("La estimación debe ser mayor que 0 €.");
        }

        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        reparacion.setEstimacionInicial(nuevaEstimacion);
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                String.format("Estimación orientativa actualizada a %.2f €.", nuevaEstimacion));
    }

    /**
     * El mecánico registra o actualiza las horas reales de mano de obra dedicadas a la reparación.
     */
    public ReparacionResponseDTO actualizarHorasTrabajo(Long reparacionId, Double horas) {
        if (horas == null || horas < 0) {
            throw new IllegalArgumentException("Las horas de trabajo no pueden ser negativas.");
        }

        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        if (esReparacionConcluida(reparacion.getEstado())) {
            throw new IllegalArgumentException(
                    "No se pueden modificar las horas de una reparación en estado " + reparacion.getEstado().name() + ".");
        }

        if (!tienePresupuestoAceptado(reparacionId)) {
            throw new IllegalArgumentException(
                    "Operación bloqueada: no se pueden registrar horas de trabajo hasta que el cliente haya ACEPTADO el presupuesto desglosado.");
        }

        double horasRedondeadas = Math.round(horas * 100.0) / 100.0;
        reparacion.setHorasTrabajoMecanico(horasRedondeadas);
        reparacion.setSegundosAcumuladosTemporizador(Math.round(horasRedondeadas * 3600.0));
        reparacion.setInicioTemporizador(null);
        if ("EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador())) {
            reparacion.setEstadoTemporizador("PAUSADO");
        }
        reparacionRepository.save(reparacion);

        // Si ya existía una factura preliminar, la sincronizamos con las nuevas horas
        if (facturaRepository != null) {
            facturaRepository.findByReparacionId(reparacionId).ifPresent(factura -> {
                ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
                double costeMat = calcularCosteMateriales(reparacionId);
                factura.recalcularImportes(costeMat, horasRedondeadas, config.getPrecioHoraManoObra(),
                        config.getMargenBeneficioPorcentaje(), 21.0);
                facturaRepository.save(factura);
            });
        }

        return convertirAResponseDTO(reparacion,
                String.format("Horas de mano de obra actualizadas a %.2f h.", horasRedondeadas));
    }

    /**
     * HU #19: Inicia o reanuda el temporizador de mano de obra para una reparación EN_PROCESO.
     * Si el mecánico ya tiene otro vehículo con temporizador EN_MARCHA, bloquea la acción.
     */
    public synchronized ReparacionResponseDTO iniciarTemporizador(Long reparacionId, Long mecanicoId) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() != EstadoReparacion.EN_PROCESO) {
            throw new IllegalArgumentException(
                    "Solo se puede iniciar el temporizador en reparaciones en estado EN_PROCESO (estado actual: "
                            + reparacion.getEstado().name() + ").");
        }

        if (presupuestoRepository != null && !tienePresupuestoAceptado(reparacionId)) {
            throw new IllegalArgumentException(
                    "Operación bloqueada: el presupuesto debe estar aceptado por el cliente antes de iniciar el temporizador.");
        }

        Long mecIdEfectivo = mecanicoId != null
                ? mecanicoId
                : (reparacion.getMecanico() != null ? reparacion.getMecanico().getId() : null);

        if (mecIdEfectivo != null) {
            List<Reparacion> delMecanico = reparacionRepository.findByMecanicoId(mecIdEfectivo);
            if (delMecanico != null) {
                for (Reparacion otra : delMecanico) {
                    if (otra.getId() != null && !otra.getId().equals(reparacionId)
                            && "EN_MARCHA".equalsIgnoreCase(otra.getEstadoTemporizador())) {
                        String matriculaOtra = (otra.getVehiculo() != null && otra.getVehiculo().getMatricula() != null)
                                ? otra.getVehiculo().getMatricula() : ("Orden #" + otra.getId());
                        throw new IllegalStateException(
                                "Ya tienes un temporizador en marcha en el vehículo " + matriculaOtra
                                        + " (Orden #" + otra.getId() + "). Pausa la tarea activa antes de comenzar una nueva.");
                    }
                }
            }
        }

        if ("EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador())) {
            return convertirAResponseDTO(reparacion, "El temporizador ya se encuentra en marcha.");
        }

        if (reparacion.getSegundosAcumuladosTemporizador() == 0L && reparacion.getHorasTrabajoMecanico() > 0.0) {
            reparacion.setSegundosAcumuladosTemporizador(Math.round(reparacion.getHorasTrabajoMecanico() * 3600.0));
        }

        reparacion.setEstadoTemporizador("EN_MARCHA");
        reparacion.setInicioTemporizador(LocalDateTime.now());
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion, "Temporizador de mano de obra iniciado.");
    }

    /**
     * HU #19: Pausa el temporizador de mano de obra guardando los segundos y horas acumulados.
     */
    public synchronized ReparacionResponseDTO pausarTemporizador(Long reparacionId) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (!"EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador())) {
            throw new IllegalStateException("El temporizador no está en marcha actualmente para esta reparación.");
        }

        long transcurrido = calcularSegundosDesdeInicio(reparacion.getInicioTemporizador());
        long totalSegundos = reparacion.getSegundosAcumuladosTemporizador() + transcurrido;
        double horas = Math.round((totalSegundos / 3600.0) * 100.0) / 100.0;

        reparacion.setSegundosAcumuladosTemporizador(totalSegundos);
        reparacion.setInicioTemporizador(null);
        reparacion.setEstadoTemporizador("PAUSADO");
        reparacion.setHorasTrabajoMecanico(horas);
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Temporizador pausado. Tiempo acumulado: " + formatearDuracionHorasMinutos(totalSegundos) + ".");
    }

    /**
     * HU #19: Detiene definitivamente el temporizador y consolida las horas invertidas en la ficha.
     */
    public synchronized ReparacionResponseDTO terminarTemporizador(Long reparacionId) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (esReparacionConcluida(reparacion.getEstado())) {
            throw new IllegalArgumentException(
                    "La reparación ya está en estado " + reparacion.getEstado().name() + " y su tiempo no puede modificarse.");
        }

        long transcurrido = "EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador())
                ? calcularSegundosDesdeInicio(reparacion.getInicioTemporizador())
                : 0L;
        long totalSegundos = reparacion.getSegundosAcumuladosTemporizador() + transcurrido;
        if (totalSegundos == 0L && reparacion.getHorasTrabajoMecanico() > 0.0) {
            totalSegundos = Math.round(reparacion.getHorasTrabajoMecanico() * 3600.0);
        }
        double horas = Math.round((totalSegundos / 3600.0) * 100.0) / 100.0;

        reparacion.setSegundosAcumuladosTemporizador(totalSegundos);
        reparacion.setInicioTemporizador(null);
        reparacion.setEstadoTemporizador("DETENIDO");
        reparacion.setHorasTrabajoMecanico(horas);
        reparacionRepository.save(reparacion);

        if (facturaRepository != null) {
            facturaRepository.findByReparacionId(reparacionId).ifPresent(factura -> {
                ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
                double costeMat = calcularCosteMateriales(reparacionId);
                factura.recalcularImportes(costeMat, horas, config.getPrecioHoraManoObra(),
                        config.getMargenBeneficioPorcentaje(), 21.0);
                facturaRepository.save(factura);
            });
        }

        return convertirAResponseDTO(reparacion,
                "Tiempo total registrado en la reparación: " + formatearDuracionHorasMinutos(totalSegundos)
                        + " (" + String.format(Locale.US, "%.2f", horas) + " h).");
    }

    private long calcularSegundosDesdeInicio(LocalDateTime inicio) {
        if (inicio == null) return 0L;
        return Math.max(0L, Duration.between(inicio, LocalDateTime.now()).getSeconds());
    }

    private boolean esReparacionConcluida(EstadoReparacion estado) {
        return estado == EstadoReparacion.FINALIZADA
                || estado == EstadoReparacion.CONFIRMADA
                || estado == EstadoReparacion.ENTREGADA
                || estado == EstadoReparacion.CANCELADA;
    }

    /**
     * El mecánico o recepcionista modifica los días estimados de la reparación.
     */
    public ReparacionResponseDTO actualizarDiasEstimados(Long reparacionId, Integer dias) {
        if (dias == null || dias < 1) {
            throw new IllegalArgumentException("Los días estimados deben ser al menos 1 día.");
        }

        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        reparacion.setDiasEstimados(dias);
        reparacionRepository.save(reparacion);

        if (eventPublisher != null && reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null) {
            eventPublisher.publishEvent(new NotificacionReparacionEvent(
                    reparacion.getVehiculo().getCliente(),
                    reparacion,
                    TipoNotificacion.CAMBIO_FECHA,
                    "Actualización en la fecha de entrega",
                    "Se ha actualizado el plazo de tu vehículo " + reparacion.getVehiculo().getMatricula()
                            + " a " + dias + " día(s). Nueva fecha prevista de entrega: " + reparacion.getFechaEstimadaFin() + "."
            ));
        }

        return convertirAResponseDTO(reparacion,
                "Plazo estimado actualizado a " + dias + " día(s) (entrega prevista: "
                        + reparacion.getFechaEstimadaFin() + ").");
    }

    /**
     * Registra una incidencia / nota explicativa del mecánico con impacto en precio opcional y foto subida.
     */
    public Map<String, Object> registrarIncidencia(Long reparacionId, String descripcion,
                                                   Double impactoPrecio, MultipartFile archivoFoto) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        if (esReparacionConcluida(reparacion.getEstado())) {
            throw new IllegalArgumentException(
                    "No se pueden añadir incidencias ni fotografías a una reparación ya concluida ("
                            + reparacion.getEstado().name() + ").");
        }

        if (descripcion == null || descripcion.isBlank()) {
            throw new IllegalArgumentException("La descripción de la incidencia no puede estar vacía.");
        }

        String rutaFotoPublica = null;
        if (archivoFoto != null && !archivoFoto.isEmpty()) {
            try {
                Path directorioUploads = Paths.get("uploads").toAbsolutePath().normalize();
                Files.createDirectories(directorioUploads);

                String nombreOriginal = archivoFoto.getOriginalFilename() != null
                        ? archivoFoto.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "foto.jpg";
                String nombreArchivo = "incidencia_" + reparacionId + "_" + System.currentTimeMillis() + "_" + nombreOriginal;
                Path destino = directorioUploads.resolve(nombreArchivo);
                Files.copy(archivoFoto.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
                rutaFotoPublica = "/uploads/" + nombreArchivo;
            } catch (IOException e) {
                throw new IllegalArgumentException("No se pudo guardar la imagen de la incidencia: " + e.getMessage());
            }
        }

        double impacto = impactoPrecio != null ? Math.round(impactoPrecio * 100.0) / 100.0 : 0.0;
        IncidenciaReparacion incidencia = new IncidenciaReparacion(reparacion, descripcion.trim(), impacto, rutaFotoPublica);
        incidenciaRepository.save(incidencia);

        if (eventPublisher != null && reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null) {
            String extraImpacto = (impacto > 0)
                    ? " (Impacto económico estimado: +" + String.format(Locale.US, "%.2f", impacto) + " €)"
                    : "";
            eventPublisher.publishEvent(new NotificacionReparacionEvent(
                    reparacion.getVehiculo().getCliente(),
                    reparacion,
                    TipoNotificacion.INCIDENCIA,
                    "Nueva incidencia técnica registrada",
                    "El mecánico ha registrado un hallazgo en tu vehículo " + reparacion.getVehiculo().getMatricula()
                            + ": \"" + descripcion.trim() + "\"" + extraImpacto + ". Puedes consultar las fotos y detalles en el seguimiento."
            ));
        }

        Map<String, Object> respuesta = mapearIncidencia(incidencia);
        respuesta.put("mensaje", "Incidencia registrada correctamente en la línea temporal del cliente.");
        return respuesta;
    }

    /**
     * Obtiene las incidencias ordenadas cronológicamente para una reparación.
     */
    public List<Map<String, Object>> obtenerIncidencias(Long reparacionId) {
        if (!reparacionRepository.existsById(reparacionId)) {
            throw new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId);
        }
        return incidenciaRepository.findByReparacionIdOrderByFechaAsc(reparacionId).stream()
                .map(this::mapearIncidencia)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapearIncidencia(IncidenciaReparacion inc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", inc.getId());
        m.put("reparacionId", inc.getReparacion().getId());
        m.put("fecha", inc.getFecha() != null ? inc.getFecha().toString() : LocalDate.now().atStartOfDay().toString());
        m.put("descripcion", inc.getDescripcion());
        m.put("impactoPrecio", inc.getImpactoPrecio() != null ? inc.getImpactoPrecio() : 0.0);
        m.put("rutaFoto", inc.getRutaFoto());
        return m;
    }

    /**
     * Elimina únicamente la foto asociada a una incidencia (manteniendo el texto de la nota)
     * y borra el archivo de imagen físico de la carpeta uploads/.
     */
    public Map<String, Object> eliminarFotoIncidencia(Long reparacionId, Long incidenciaId) {
        IncidenciaReparacion incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la incidencia con ID: " + incidenciaId));

        if (!incidencia.getReparacion().getId().equals(reparacionId)) {
            throw new IllegalArgumentException("La incidencia no pertenece a la reparación #" + reparacionId);
        }

        if (esReparacionConcluida(incidencia.getReparacion().getEstado())) {
            throw new IllegalArgumentException(
                    "El historial de reparaciones concluidas es de solo lectura: no se pueden eliminar fotografías.");
        }

        if (incidencia.getRutaFoto() == null || incidencia.getRutaFoto().isBlank()) {
            throw new IllegalArgumentException("Esta incidencia no tiene ninguna foto adjunta para eliminar.");
        }

        borrarArchivoFotoFisico(incidencia.getRutaFoto());
        incidencia.setRutaFoto(null);
        incidenciaRepository.save(incidencia);

        Map<String, Object> respuesta = mapearIncidencia(incidencia);
        respuesta.put("mensaje", "La fotografía de la incidencia se ha eliminado correctamente.");
        return respuesta;
    }

    /**
     * Elimina por completo una incidencia y su fotografía física del disco si la tuviera.
     */
    public Map<String, Object> eliminarIncidencia(Long reparacionId, Long incidenciaId) {
        IncidenciaReparacion incidencia = incidenciaRepository.findById(incidenciaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la incidencia con ID: " + incidenciaId));

        if (!incidencia.getReparacion().getId().equals(reparacionId)) {
            throw new IllegalArgumentException("La incidencia no pertenece a la reparación #" + reparacionId);
        }

        if (esReparacionConcluida(incidencia.getReparacion().getEstado())) {
            throw new IllegalArgumentException(
                    "El historial de reparaciones concluidas es de solo lectura: no se pueden borrar notas pasadas.");
        }

        if (incidencia.getRutaFoto() != null && !incidencia.getRutaFoto().isBlank()) {
            borrarArchivoFotoFisico(incidencia.getRutaFoto());
        }

        incidenciaRepository.delete(incidencia);

        return Map.of(
                "id", incidenciaId,
                "reparacionId", reparacionId,
                "mensaje", "Incidencia y fotografía eliminadas correctamente."
        );
    }

    private void borrarArchivoFotoFisico(String rutaFotoPublica) {
        try {
            String nombreArchivo = rutaFotoPublica.replaceFirst("^/uploads/", "");
            Path ruta1 = Paths.get("uploads").toAbsolutePath().normalize().resolve(nombreArchivo);
            Files.deleteIfExists(ruta1);

            Path ruta2 = Paths.get("backend", "uploads").toAbsolutePath().normalize().resolve(nombreArchivo);
            Files.deleteIfExists(ruta2);
        } catch (IOException ignored) {
        }
    }

    /**
     * Genera o actualiza la Factura formal de una reparación basándose en:
     * - Coste real de piezas usadas
     * - Horas de mano de obra trabajadas × precioHora (configurado por el Admin)
     * - Margen de beneficio del taller (% configurado por el Admin sobre materiales + mano de obra)
     * - IVA (21%)
     */
    public synchronized Map<String, Object> generarFactura(Long reparacionId, Double horasManoObraOpcional) {
        return generarFactura(reparacionId, horasManoObraOpcional, null, null, false);
    }

    public synchronized Map<String, Object> generarFactura(Long reparacionId, Double horasManoObraOpcional,
                                                           String dniCif, String direccionFiscal,
                                                           boolean validarDatosFiscales) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.PENDIENTE
                || reparacion.getEstado() == EstadoReparacion.EN_PROCESO
                || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalArgumentException(
                    "No se puede generar la factura si la reparación no está en estado FINALIZADA o CONFIRMADA (estado actual: "
                            + reparacion.getEstado().name() + ").");
        }

        Cliente cliente = reparacion.getVehiculo().getCliente();
        if (dniCif != null && !dniCif.trim().isEmpty()) {
            cliente.setDniCif(dniCif.trim().toUpperCase());
        }
        if (direccionFiscal != null && !direccionFiscal.trim().isEmpty()) {
            cliente.setDireccionFiscal(direccionFiscal.trim());
        }
        if ((dniCif != null && !dniCif.trim().isEmpty()) || (direccionFiscal != null && !direccionFiscal.trim().isEmpty())) {
            clienteRepository.save(cliente);
        }

        if (validarDatosFiscales) {
            if (cliente.getDniCif() == null || cliente.getDniCif().trim().isEmpty()
                    || cliente.getDireccionFiscal() == null || cliente.getDireccionFiscal().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "No se puede generar la factura: el registro carece de datos fiscales obligatorios del cliente (DNI/CIF y Dirección Fiscal).");
            }
        }

        if (horasManoObraOpcional != null && horasManoObraOpcional > 0) {
            reparacion.setHorasTrabajoMecanico(Math.round(horasManoObraOpcional * 100.0) / 100.0);
            reparacionRepository.save(reparacion);
        } else if (reparacion.getHorasTrabajoMecanico() == null || reparacion.getHorasTrabajoMecanico() <= 0) {
            reparacion.setHorasTrabajoMecanico(1.0);
            reparacionRepository.save(reparacion);
        }

        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
        double costeMateriales = calcularCosteMateriales(reparacionId);
        double horas = reparacion.getHorasTrabajoMecanico();
        double precioHora = config.getPrecioHoraManoObra();
        double porcentajeMargen = config.getMargenBeneficioPorcentaje();

        Optional<Factura> existente = facturaRepository.findByReparacionId(reparacionId);
        Factura factura;
        if (existente.isPresent()) {
            factura = existente.get();
            if (reparacion.getEstado() == EstadoReparacion.ENTREGADA) {
                throw new IllegalArgumentException(
                        "La factura " + factura.getNumeroFactura()
                                + " ya está cerrada y entregada; sus importes son inmutables y no pueden modificarse.");
            }
            if (reparacion.getEstado() != EstadoReparacion.CONFIRMADA) {
                factura.setFechaEmision(LocalDate.now());
                factura.recalcularImportes(costeMateriales, horas, precioHora, porcentajeMargen, 21.0);
            }
        } else {
            String numeroFactura = generarNumeroFacturaUnico();
            factura = new Factura(numeroFactura, reparacion, costeMateriales, horas, precioHora, porcentajeMargen, 21.0);
        }

        factura.setDniCifCliente(cliente.getDniCif() != null ? cliente.getDniCif() : "");
        factura.setDireccionFiscalCliente(cliente.getDireccionFiscal() != null ? cliente.getDireccionFiscal() : "");

        facturaRepository.save(factura);
        reparacion.setFactura(factura);

        Map<String, Object> dto = mapearFacturaCompleta(factura);
        dto.put("mensaje", "Factura " + factura.getNumeroFactura() + " generada y registrada correctamente.");
        return dto;
    }

    // ======== HISTORIA DE USUARIO 1 Y 3: PRESUPUESTO DESGLOSADO Y ACEPTACIÓN DEL CLIENTE ========

    /**
     * Genera o actualiza un Presupuesto Desglosado tras el diagnóstico técnico (Historia 1).
     * Diferencia líneas de tipo "PIEZA" (Pieza/Repuesto) y "MANO_OBRA" (Mano de obra).
     */
    public synchronized Map<String, Object> guardarPresupuestoDesglosado(Long reparacionId,
                                                                         String diagnosticoPrevio,
                                                                         Integer diasValidez,
                                                                         List<Map<String, Object>> lineasRaw) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.ENTREGADA || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalStateException("No se puede emitir ni modificar el presupuesto de una reparación ya "
                    + reparacion.getEstado().name() + ".");
        }

        if (lineasRaw == null || lineasRaw.isEmpty()) {
            throw new IllegalArgumentException("El presupuesto debe contener al menos un concepto (pieza o mano de obra).");
        }

        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
        int validez = (diasValidez != null && diasValidez > 0) ? diasValidez : 15;

        Optional<PresupuestoReparacion> optExistente = presupuestoRepository.findByReparacionId(reparacionId);
        PresupuestoReparacion presupuesto;
        if (optExistente.isPresent()) {
            presupuesto = optExistente.get();
            presupuesto.setDiagnosticoPrevio(diagnosticoPrevio != null ? diagnosticoPrevio.trim() : reparacion.getDescripcionAveria());
            presupuesto.setFechaEmision(LocalDate.now());
            presupuesto.setFechaValidez(LocalDate.now().plusDays(validez));
            presupuesto.setEstado("PENDIENTE_ACEPTACION");
            presupuesto.setMotivoRechazo(null);
            presupuesto.setFechaRespuestaCliente(null);
            presupuesto.setPorcentajeMargen(config.getMargenBeneficioPorcentaje());
            presupuesto.setPorcentajeIva(21.0);
            presupuesto.getLineas().clear();
        } else {
            String numero = generarNumeroPresupuestoUnico();
            presupuesto = new PresupuestoReparacion(
                    numero,
                    reparacion,
                    diagnosticoPrevio != null ? diagnosticoPrevio.trim() : reparacion.getDescripcionAveria(),
                    LocalDate.now().plusDays(validez),
                    config.getMargenBeneficioPorcentaje(),
                    21.0
            );
        }

        double horasTotalesEstimadas = 0.0;
        for (Map<String, Object> item : lineasRaw) {
            String tipo = item.get("tipo") != null ? item.get("tipo").toString().trim().toUpperCase() : "PIEZA";
            if (!"PIEZA".equals(tipo) && !"MANO_OBRA".equals(tipo)) {
                tipo = "PIEZA";
            }
            String concepto = item.get("concepto") != null ? item.get("concepto").toString().trim() : "";
            if (concepto.isEmpty()) {
                throw new IllegalArgumentException("Todas las líneas del presupuesto deben incluir un concepto descriptivo.");
            }
            double cantidad = item.get("cantidad") != null ? Double.parseDouble(item.get("cantidad").toString()) : 0.0;
            double precioUnitario = item.get("precioUnitario") != null ? Double.parseDouble(item.get("precioUnitario").toString()) : 0.0;

            if (cantidad <= 0 || precioUnitario <= 0) {
                throw new IllegalArgumentException("No se permite guardar el presupuesto: las cantidades, horas e importes unitarios deben ser mayores que cero (> 0).");
            }

            Long materialId = item.get("materialId") != null && !item.get("materialId").toString().isBlank()
                    ? Long.valueOf(item.get("materialId").toString()) : null;

            if ("MANO_OBRA".equals(tipo)) {
                horasTotalesEstimadas += cantidad;
            }

            LineaPresupuesto linea = new LineaPresupuesto(presupuesto, tipo, concepto, cantidad, precioUnitario, materialId);
            presupuesto.getLineas().add(linea);
        }

        presupuesto.recalcularTotales();
        presupuestoRepository.save(presupuesto);

        // Actualizar el presupuesto de referencia en la reparación y horas estimadas si aún era 0
        reparacion.setPresupuesto(presupuesto.getTotalPresupuesto());
        reparacion.setPresupuestoEstimado(presupuesto.getTotalPresupuesto());
        if ((reparacion.getHorasTrabajoMecanico() == null || reparacion.getHorasTrabajoMecanico() <= 0) && horasTotalesEstimadas > 0) {
            reparacion.setHorasTrabajoMecanico(Math.round(horasTotalesEstimadas * 100.0) / 100.0);
        }
        reparacionRepository.save(reparacion);

        boolean eraAmpliacion = optExistente.isPresent() && reparacion.getEstado() == EstadoReparacion.EN_PROCESO;
        // Registrar hito en la línea temporal de la reparación
        incidenciaRepository.save(new IncidenciaReparacion(
                reparacion,
                (eraAmpliacion
                        ? "⚠️ Ampliación de Presupuesto " + presupuesto.getNumeroPresupuesto() + " por avería adicional/imprevisto emitida por "
                        : "📋 Presupuesto desglosado " + presupuesto.getNumeroPresupuesto() + " emitido por ")
                        + String.format("%.2f €", presupuesto.getTotalPresupuesto())
                        + " (Piezas: " + String.format("%.2f €", presupuesto.getSubtotalPiezas())
                        + " | Mano de obra: " + String.format("%.2f €", presupuesto.getSubtotalManoObra())
                        + " + Margen/IVA). Estado: PENDIENTE DE ACEPTACIÓN DEL CLIENTE (reparación bloqueada hasta confirmación).",
                0.0,
                null
        ));

        Map<String, Object> dto = mapearPresupuestoCompleto(presupuesto);
        dto.put("mensaje", eraAmpliacion
                ? "Ampliación de presupuesto " + presupuesto.getNumeroPresupuesto() + " enviada al cliente. La reparación queda en pausa hasta su aceptación."
                : "Presupuesto " + presupuesto.getNumeroPresupuesto() + " guardado y enviado al cliente (PENDIENTE_ACEPTACION).");

        if (eventPublisher != null && reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null) {
            String tituloNotif = eraAmpliacion ? "Ampliación de presupuesto disponible" : "Presupuesto desglosado disponible";
            eventPublisher.publishEvent(new NotificacionReparacionEvent(
                    reparacion.getVehiculo().getCliente(),
                    reparacion,
                    TipoNotificacion.PRESUPUESTO_DISPONIBLE,
                    tituloNotif,
                    "Se ha emitido el presupuesto desglosado " + presupuesto.getNumeroPresupuesto()
                            + " para tu vehículo " + reparacion.getVehiculo().getMatricula()
                            + " por un total de " + String.format(Locale.US, "%.2f", presupuesto.getTotalPresupuesto())
                            + " €. Ya puedes revisarlo y autorizarlo desde tu panel de cliente."
            ));
        }

        return dto;
    }

    /**
     * Crea un presupuesto inicialmente ACEPTADO (para reparaciones de muestra que ya están en taller o aceptadas en recepción).
     */
    public synchronized void crearPresupuestoAceptadoInicial(Long reparacionId,
                                                             String diagnosticoPrevio,
                                                             List<Map<String, Object>> lineasRaw) {
        if (presupuestoRepository.findByReparacionId(reparacionId).isPresent()) {
            return;
        }
        guardarPresupuestoDesglosado(reparacionId, diagnosticoPrevio, 15, lineasRaw);
        presupuestoRepository.findByReparacionId(reparacionId).ifPresent(p -> {
            p.setEstado("ACEPTADO");
            p.setFechaRespuestaCliente(java.time.LocalDateTime.now());
            presupuestoRepository.save(p);
        });
    }

    private String generarNumeroPresupuestoUnico() {
        int anio = LocalDate.now().getYear();
        long total = presupuestoRepository.count() + 1;
        String candidato = String.format("PRES-%d-%03d", anio, total);
        while (presupuestoRepository.findByNumeroPresupuesto(candidato).isPresent()) {
            total++;
            candidato = String.format("PRES-%d-%03d", anio, total);
        }
        return candidato;
    }

    public Map<String, Object> obtenerPresupuestoPorReparacion(Long reparacionId) {
        PresupuestoReparacion p = presupuestoRepository.findByReparacionId(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Todavía no se ha emitido un presupuesto desglosado para la reparación #" + reparacionId));
        return mapearPresupuestoCompleto(p);
    }

    /**
     * Historia de Usuario 3: Permite al cliente ACEPTAR o RECHAZAR un presupuesto en estado PENDIENTE_ACEPTACION
     * verificando obligatoriamente la contraseña de su cuenta.
     */
    public synchronized Map<String, Object> responderPresupuestoCliente(Long reparacionId,
                                                                        String accion,
                                                                        String password,
                                                                        String motivoRechazo) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.ENTREGADA || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalStateException("No se puede responder al presupuesto porque la reparación ya está "
                    + reparacion.getEstado().name() + ".");
        }

        PresupuestoReparacion presupuesto = presupuestoRepository.findByReparacionId(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("Esta reparación no tiene un presupuesto formal pendiente."));

        if (!"PENDIENTE_ACEPTACION".equalsIgnoreCase(presupuesto.getEstado())) {
            throw new IllegalStateException("No se puede modificar este presupuesto porque ya se encuentra en estado "
                    + presupuesto.getEstado() + ".");
        }

        if (presupuesto.estaCaducado()) {
            throw new IllegalStateException("Este presupuesto ha superado su fecha de validez ("
                    + presupuesto.getFechaValidez() + ") y ya no puede ser aceptado ni rechazado.");
        }

        if (password == null || password.isBlank()) {
            throw new SecurityException("Debes introducir tu contraseña para confirmar la operación.");
        }

        Cliente cliente = reparacion.getVehiculo().getCliente();
        boolean passwordValida = false;
        if (cliente.getPassword() != null && passwordSecurityService.coincide(password, cliente.getPassword())) {
            passwordValida = true;
            if (!passwordSecurityService.estaHasheada(cliente.getPassword())) {
                cliente.setPassword(passwordSecurityService.hashear(password));
                clienteRepository.save(cliente);
            }
        } else {
            // También comprobar si la reparación está vinculada a otro perfil de cliente con cuenta
            for (Cliente c : clienteRepository.findAll()) {
                if (c.getReparacionesVinculadas() != null
                        && c.getReparacionesVinculadas().stream().anyMatch(r -> r.getId().equals(reparacionId))
                        && c.getPassword() != null && passwordSecurityService.coincide(password, c.getPassword())) {
                    passwordValida = true;
                    if (!passwordSecurityService.estaHasheada(c.getPassword())) {
                        c.setPassword(passwordSecurityService.hashear(password));
                        clienteRepository.save(c);
                    }
                    break;
                }
            }
        }

        if (!passwordValida) {
            throw new SecurityException("Contraseña incorrecta");
        }

        String accionNorm = accion != null ? accion.trim().toUpperCase() : "";
        if ("ACEPTAR".equals(accionNorm) || "ACEPTADO".equals(accionNorm)) {
            presupuesto.setEstado("ACEPTADO");
            presupuesto.setFechaRespuestaCliente(java.time.LocalDateTime.now());
            presupuesto.setMotivoRechazo(null);
            presupuestoRepository.save(presupuesto);

            if (reparacion.getEstado() == EstadoReparacion.PENDIENTE) {
                reparacion.setEstado(EstadoReparacion.EN_PROCESO);
            }
            reparacion.setPresupuesto(presupuesto.getTotalPresupuesto());
            reparacionRepository.save(reparacion);

            incidenciaRepository.save(new IncidenciaReparacion(
                    reparacion,
                    "✅ Presupuesto " + presupuesto.getNumeroPresupuesto() + " ACEPTADO por el cliente. Vehículo autorizado y listo para reparar.",
                    0.0,
                    null
            ));

            if (eventPublisher != null && cliente != null) {
                eventPublisher.publishEvent(new NotificacionReparacionEvent(
                        cliente,
                        reparacion,
                        TipoNotificacion.EN_PROCESO,
                        "Presupuesto aceptado - Reparación en marcha",
                        "Has autorizado el presupuesto " + presupuesto.getNumeroPresupuesto()
                                + " (" + String.format(Locale.US, "%.2f", presupuesto.getTotalPresupuesto()) + " €). Tu vehículo ("
                                + reparacion.getVehiculo().getMatricula() + ") ya está listo para que el mecánico inicie las reparaciones."
                ));
            }
        } else if ("RECHAZAR".equals(accionNorm) || "RECHAZADO".equals(accionNorm)) {
            presupuesto.setEstado("RECHAZADO");
            presupuesto.setFechaRespuestaCliente(java.time.LocalDateTime.now());
            String motivo = (motivoRechazo != null && !motivoRechazo.trim().isEmpty())
                    ? motivoRechazo.trim() : "Sin motivo especificado";
            presupuesto.setMotivoRechazo(motivo);
            presupuestoRepository.save(presupuesto);

            incidenciaRepository.save(new IncidenciaReparacion(
                    reparacion,
                    "❌ Presupuesto " + presupuesto.getNumeroPresupuesto() + " RECHAZADO por el cliente. Motivo: " + motivo,
                    0.0,
                    null
            ));
        } else {
            throw new IllegalArgumentException("Acción no válida. Debe ser ACEPTAR o RECHAZAR.");
        }

        Map<String, Object> dto = mapearPresupuestoCompleto(presupuesto);
        dto.put("estadoReparacion", reparacion.getEstado().name());
        dto.put("mensaje", "ACEPTADO".equals(presupuesto.getEstado())
                ? "Has aceptado el presupuesto correctamente. El taller ya puede iniciar la reparación."
                : "Has rechazado el presupuesto. Se ha notificado al taller tu decisión.");
        return dto;
    }

    /**
     * Punto 2: Permite a Recepción o Administración registrar la ACEPTACIÓN o RECHAZO
     * presencial/telefónico de un presupuesto cuando el cliente no tiene cuenta web.
     */
    @Transactional
    public synchronized Map<String, Object> responderPresupuestoPersonal(Long reparacionId,
                                                                         String accion,
                                                                         String canalConfirmacion,
                                                                         String motivoRechazo,
                                                                         Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("Solo Recepción o Administración puede registrar la aceptación o rechazo presencial/telefónico de un presupuesto.");
        }
        Usuario empleado = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario del taller con ID: " + usuarioId));

        if (empleado.getRol() != Rol.RECEPCIONISTA && empleado.getRol() != Rol.ADMIN) {
            throw new IllegalArgumentException("Solo Recepción o Administración puede registrar la aceptación o rechazo presencial/telefónico de un presupuesto.");
        }

        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.ENTREGADA || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalStateException("No se puede responder al presupuesto porque la reparación ya está "
                    + reparacion.getEstado().name() + ".");
        }

        PresupuestoReparacion presupuesto = presupuestoRepository.findByReparacionId(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("Esta reparación no tiene un presupuesto formal pendiente."));

        if (!"PENDIENTE_ACEPTACION".equalsIgnoreCase(presupuesto.getEstado())) {
            throw new IllegalStateException("No se puede modificar este presupuesto porque ya se encuentra en estado "
                    + presupuesto.getEstado() + ".");
        }

        if (presupuesto.estaCaducado()) {
            throw new IllegalStateException("Este presupuesto ha superado su fecha de validez ("
                    + presupuesto.getFechaValidez() + ") y ya no puede ser aceptado ni rechazado.");
        }

        String canal = (canalConfirmacion != null && !canalConfirmacion.trim().isEmpty())
                ? canalConfirmacion.trim() : "Presencial / Teléfono";
        String accionNorm = accion != null ? accion.trim().toUpperCase() : "";
        Cliente cliente = reparacion.getVehiculo() != null ? reparacion.getVehiculo().getCliente() : null;
        String matricula = reparacion.getVehiculo() != null ? reparacion.getVehiculo().getMatricula() : "";

        if ("ACEPTAR".equals(accionNorm) || "ACEPTADO".equals(accionNorm)) {
            presupuesto.setEstado("ACEPTADO");
            presupuesto.setFechaRespuestaCliente(java.time.LocalDateTime.now());
            presupuesto.setMotivoRechazo(null);
            presupuestoRepository.save(presupuesto);

            if (reparacion.getEstado() == EstadoReparacion.PENDIENTE && reparacion.getMecanico() != null) {
                reparacion.setEstado(EstadoReparacion.EN_PROCESO);
            }
            reparacion.setPresupuesto(presupuesto.getTotalPresupuesto());
            reparacionRepository.save(reparacion);

            if (incidenciaRepository != null) {
                incidenciaRepository.save(new IncidenciaReparacion(
                        reparacion,
                        "✅ Presupuesto " + presupuesto.getNumeroPresupuesto() + " ACEPTADO (" + canal
                                + ") registrado por " + empleado.getNombre() + ". Vehículo autorizado para reparar.",
                        0.0,
                        null
                ));
            }

            if (eventPublisher != null && cliente != null) {
                eventPublisher.publishEvent(new NotificacionReparacionEvent(
                        cliente,
                        reparacion,
                        TipoNotificacion.EN_PROCESO,
                        "Presupuesto aceptado - Reparación en marcha",
                        "Se ha registrado la autorización (" + canal + ") del presupuesto "
                                + presupuesto.getNumeroPresupuesto() + " ("
                                + String.format(Locale.US, "%.2f", presupuesto.getTotalPresupuesto()) + " €) para tu vehículo ("
                                + matricula + ")."
                ));
            }
        } else if ("RECHAZAR".equals(accionNorm) || "RECHAZADO".equals(accionNorm)) {
            presupuesto.setEstado("RECHAZADO");
            presupuesto.setFechaRespuestaCliente(java.time.LocalDateTime.now());
            String motivo = (motivoRechazo != null && !motivoRechazo.trim().isEmpty())
                    ? motivoRechazo.trim() : "Rechazado por el cliente (" + canal + ")";
            presupuesto.setMotivoRechazo(motivo);
            presupuestoRepository.save(presupuesto);

            if (incidenciaRepository != null) {
                incidenciaRepository.save(new IncidenciaReparacion(
                        reparacion,
                        "❌ Presupuesto " + presupuesto.getNumeroPresupuesto() + " RECHAZADO (" + canal
                                + ") registrado por " + empleado.getNombre() + ". Motivo: " + motivo
                                + ". La orden queda abierta para nuevo presupuesto o cancelación.",
                        0.0,
                        null
                ));
            }

            if (eventPublisher != null && cliente != null) {
                eventPublisher.publishEvent(new NotificacionReparacionEvent(
                        cliente,
                        reparacion,
                        TipoNotificacion.INFO,
                        "Presupuesto rechazado (" + canal + ")",
                        "Se ha registrado el rechazo del presupuesto " + presupuesto.getNumeroPresupuesto()
                                + " para tu vehículo (" + matricula + "). Motivo: " + motivo
                                + ". Puedes solicitar un nuevo presupuesto o retirar el vehículo sin reparar."
                ));
            }
        } else {
            throw new IllegalArgumentException("Acción no válida. Debe ser ACEPTAR o RECHAZAR.");
        }

        Map<String, Object> dto = mapearPresupuestoCompleto(presupuesto);
        dto.put("estadoReparacion", reparacion.getEstado().name());
        dto.put("canalConfirmacion", canal);
        dto.put("mensaje", "ACEPTADO".equals(presupuesto.getEstado())
                ? "Presupuesto marcado como ACEPTADO (" + canal + "). El taller ya puede iniciar la reparación."
                : "Presupuesto marcado como RECHAZADO (" + canal + "). La reparación sigue abierta a un nuevo presupuesto o a su cancelación definitiva.");
        return dto;
    }

    /**
     * Punto 3: Cancela definitivamente una reparación tras haberse rechazado el presupuesto
     * y devuelve el vehículo sin reparar, retirándolo de las listas activas del taller.
     * Puede invocarlo el cliente propietario/vinculado o un Recepcionista/Admin.
     */
    @Transactional
    public synchronized ReparacionResponseDTO cancelarReparacionPorRechazo(Long reparacionId,
                                                                           String motivo,
                                                                           Long clienteId,
                                                                           Long usuarioId) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.ENTREGADA || reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            throw new IllegalStateException("No se puede cancelar la reparación porque ya se encuentra en estado "
                    + reparacion.getEstado().name() + ".");
        }
        if (reparacion.getEstado() == EstadoReparacion.FINALIZADA || reparacion.getEstado() == EstadoReparacion.CONFIRMADA) {
            throw new IllegalStateException("No se puede cancelar una reparación que ya ha sido finalizada o confirmada.");
        }

        PresupuestoReparacion presupuesto = presupuestoRepository.findByReparacionId(reparacionId)
                .orElseThrow(() -> new IllegalStateException(
                        "Solo se puede cancelar una reparación y retirar el vehículo sin reparar cuando el presupuesto haya sido rechazado previamente."));

        if (!"RECHAZADO".equalsIgnoreCase(presupuesto.getEstado())) {
            throw new IllegalStateException(
                    "Solo se puede cancelar una reparación y retirar el vehículo sin reparar cuando el presupuesto haya sido rechazado previamente.");
        }

        String origen;
        if (usuarioId != null) {
            Usuario empleado = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario con ID: " + usuarioId));
            if (empleado.getRol() != Rol.RECEPCIONISTA && empleado.getRol() != Rol.ADMIN) {
                throw new IllegalArgumentException("Solo el cliente, Recepción o Administración pueden cancelar la reparación.");
            }
            origen = "Recepción/Admin (" + empleado.getNombre() + ")";
        } else if (clienteId != null) {
            Cliente cli = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + clienteId));
            boolean esPropietario = reparacion.getVehiculo() != null
                    && reparacion.getVehiculo().getCliente() != null
                    && clienteId.equals(reparacion.getVehiculo().getCliente().getId());
            boolean estaVinculado = cli.getReparacionesVinculadas() != null
                    && cli.getReparacionesVinculadas().stream().anyMatch(r -> reparacionId.equals(r.getId()));
            if (!esPropietario && !estaVinculado) {
                throw new IllegalArgumentException("No tienes permiso para cancelar esta reparación.");
            }
            origen = "Cliente (" + cli.getNombre() + ")";
        } else {
            throw new IllegalArgumentException("Debes identificarte como cliente, recepcionista o administrador para cancelar la reparación.");
        }

        reparacion.setEstado(EstadoReparacion.CANCELADA);
        reparacion.setFechaSalida(LocalDate.now());
        reparacionRepository.save(reparacion);

        String motivoLimpio = (motivo != null && !motivo.trim().isEmpty()) ? motivo.trim() : null;
        if (incidenciaRepository != null) {
            incidenciaRepository.save(new IncidenciaReparacion(
                    reparacion,
                    "🚫 Reparación CANCELADA por " + origen
                            + ". El vehículo ha sido devuelto/retirado sin reparar tras rechazar el presupuesto."
                            + (motivoLimpio != null ? " Observaciones: " + motivoLimpio : ""),
                    0.0,
                    null
            ));
        }

        Cliente clienteNotif = reparacion.getVehiculo() != null ? reparacion.getVehiculo().getCliente() : null;
        String matricula = reparacion.getVehiculo() != null ? reparacion.getVehiculo().getMatricula() : "";
        if (eventPublisher != null && clienteNotif != null) {
            eventPublisher.publishEvent(new NotificacionReparacionEvent(
                    clienteNotif,
                    reparacion,
                    TipoNotificacion.CANCELADA,
                    "Reparación cancelada - Vehículo devuelto sin reparar",
                    "La reparación de tu vehículo (" + matricula + ") ha sido cancelada tras el rechazo del presupuesto. "
                            + "El vehículo ha sido retirado sin coste de reparación."
            ));
        }

        return convertirAResponseDTO(
                reparacion,
                "Reparación cancelada correctamente. El vehículo ha sido retirado de la lista activa del taller y registrado en el historial como CANCELADA."
        );
    }

    public Map<String, Object> mapearPresupuestoCompleto(PresupuestoReparacion p) {
        Reparacion r = p.getReparacion();
        List<Map<String, Object>> lineasDto = p.getLineas().stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", l.getId());
            m.put("tipo", l.getTipo());
            m.put("concepto", l.getConcepto());
            m.put("cantidad", l.getCantidad());
            m.put("precioUnitario", l.getPrecioUnitario());
            m.put("subtotal", l.getSubtotal());
            m.put("materialId", l.getMaterialId());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("numeroPresupuesto", p.getNumeroPresupuesto());
        map.put("reparacionId", r.getId());
        map.put("localizador", r.getLocalizador());
        map.put("diagnosticoPrevio", p.getDiagnosticoPrevio());
        map.put("fechaEmision", p.getFechaEmision() != null ? p.getFechaEmision().toString() : LocalDate.now().toString());
        map.put("fechaValidez", p.getFechaValidez() != null ? p.getFechaValidez().toString() : LocalDate.now().plusDays(15).toString());
        map.put("caducado", p.estaCaducado());
        map.put("estado", p.getEstado());
        map.put("motivoRechazo", p.getMotivoRechazo());
        map.put("fechaRespuestaCliente", p.getFechaRespuestaCliente() != null ? p.getFechaRespuestaCliente().toString() : null);
        map.put("matriculaVehiculo", r.getVehiculo().getMatricula());
        map.put("marcaVehiculo", r.getVehiculo().getMarca());
        map.put("modeloVehiculo", r.getVehiculo().getModelo());
        map.put("nombreCliente", r.getVehiculo().getCliente().getNombre());
        map.put("telefonoCliente", r.getVehiculo().getCliente().getTelefono());
        map.put("dniCifCliente", r.getVehiculo().getCliente().getDniCif() != null ? r.getVehiculo().getCliente().getDniCif() : "");
        map.put("direccionFiscalCliente", r.getVehiculo().getCliente().getDireccionFiscal() != null ? r.getVehiculo().getCliente().getDireccionFiscal() : "");
        map.put("nombreMecanico", r.getMecanico() != null ? r.getMecanico().getNombre() : "Mecánico Jefe");
        map.put("subtotalPiezas", p.getSubtotalPiezas());
        map.put("subtotalManoObra", p.getSubtotalManoObra());
        map.put("subtotalBase", p.getSubtotalBase());
        map.put("porcentajeMargen", p.getPorcentajeMargen());
        map.put("importeMargen", p.getImporteMargen());
        map.put("baseImponible", p.getBaseImponible());
        map.put("porcentajeIva", p.getPorcentajeIva());
        map.put("importeIva", p.getImporteIva());
        map.put("totalPresupuesto", p.getTotalPresupuesto());
        map.put("lineas", lineasDto);
        return map;
    }

    private String generarNumeroFacturaUnico() {
        int anio = LocalDate.now().getYear();
        long total = facturaRepository.count() + 1;
        String candidato = String.format("FACT-%d-%03d", anio, total);
        while (facturaRepository.findByNumeroFactura(candidato).isPresent()) {
            total++;
            candidato = String.format("FACT-%d-%03d", anio, total);
        }
        return candidato;
    }

    /**
     * Obtiene la factura asociada a una reparación.
     */
    public Map<String, Object> obtenerFacturaPorReparacion(Long reparacionId) {
        Factura factura = facturaRepository.findByReparacionId(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Todavía no se ha emitido factura para la reparación #" + reparacionId));
        return mapearFacturaCompleta(factura);
    }

    /**
     * Devuelve todas las facturas emitidas para el Archivo de Facturas de Admin y Recepcionista.
     */
    public List<Map<String, Object>> obtenerTodasLasFacturas() {
        return facturaRepository.findAllByOrderByFechaEmisionDescIdDesc().stream()
                .map(this::mapearFacturaCompleta)
                .collect(Collectors.toList());
    }

    private Map<String, Object> mapearFacturaCompleta(Factura f) {
        Reparacion r = f.getReparacion();
        List<ReparacionMaterial> usados = reparacionMaterialRepository.findByReparacionId(r.getId());
        List<String> materialesDetalle = usados.stream()
                .map(u -> String.format("%dx %s (%.2f €)",
                        u.getCantidad(),
                        u.getMaterial().getNombre(),
                        u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0)))
                .collect(Collectors.toList());

        double beneficioNetoTaller = Math.round((f.getSubtotalManoObra() + f.getImporteMargen()) * 100.0) / 100.0;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", f.getId());
        map.put("numeroFactura", f.getNumeroFactura());
        map.put("reparacionId", r.getId());
        map.put("localizador", r.getLocalizador());
        map.put("fechaEmision", f.getFechaEmision() != null ? f.getFechaEmision().toString() : LocalDate.now().toString());
        map.put("fechaEntrada", r.getFechaEntrada() != null ? r.getFechaEntrada().toString() : "");
        map.put("fechaSalida", r.getFechaSalida() != null ? r.getFechaSalida().toString() : "");
        map.put("estadoReparacion", r.getEstado().name());
        map.put("matriculaVehiculo", r.getVehiculo().getMatricula());
        map.put("marcaVehiculo", r.getVehiculo().getMarca());
        map.put("modeloVehiculo", r.getVehiculo().getModelo());
        map.put("nombreCliente", r.getVehiculo().getCliente().getNombre());
        map.put("telefonoCliente", r.getVehiculo().getCliente().getTelefono());
        map.put("dniCifCliente", f.getDniCifCliente() != null && !f.getDniCifCliente().isEmpty()
                ? f.getDniCifCliente()
                : (r.getVehiculo().getCliente().getDniCif() != null ? r.getVehiculo().getCliente().getDniCif() : ""));
        map.put("direccionFiscalCliente", f.getDireccionFiscalCliente() != null && !f.getDireccionFiscalCliente().isEmpty()
                ? f.getDireccionFiscalCliente()
                : (r.getVehiculo().getCliente().getDireccionFiscal() != null ? r.getVehiculo().getCliente().getDireccionFiscal() : ""));
        map.put("nombreMecanico", r.getMecanico() != null ? r.getMecanico().getNombre() : "Sin asignar");
        map.put("nombreRecepcionista", r.getRecepcionista() != null ? r.getRecepcionista().getNombre() : "-");
        map.put("descripcionAveria", r.getDescripcionAveria());
        map.put("estimacionInicial", r.getEstimacionInicial());
        map.put("costeMateriales", f.getCosteMateriales());
        map.put("materialesDetalle", materialesDetalle);
        map.put("horasManoObra", f.getHorasManoObra());
        map.put("precioHora", f.getPrecioHora());
        map.put("subtotalManoObra", f.getSubtotalManoObra());
        map.put("subtotalBase", f.getSubtotalBase());
        map.put("porcentajeMargen", f.getPorcentajeMargen());
        map.put("importeMargen", f.getImporteMargen());
        map.put("baseImponible", f.getBaseImponible());
        map.put("porcentajeIva", f.getPorcentajeIva());
        map.put("importeIva", f.getImporteIva());
        map.put("totalFactura", f.getTotalFactura());
        map.put("beneficioNetoTaller", beneficioNetoTaller);
        return map;
    }

    /**
     * Buscador de historial de precios de reparaciones anteriores para orientar a Recepción
     * al dar una estimación inicial al cliente.
     */
    public List<Map<String, Object>> buscarHistorialPrecios(String query) {
        String filtro = query != null ? query.trim().toLowerCase() : "";
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();

        return reparacionRepository.findAll().stream()
                .filter(r -> r.getEstado() != EstadoReparacion.CANCELADA)
                .filter(r -> {
                    if (filtro.isEmpty()) return true;
                    String desc = r.getDescripcionAveria() != null ? r.getDescripcionAveria().toLowerCase() : "";
                    String marca = r.getVehiculo() != null && r.getVehiculo().getMarca() != null ? r.getVehiculo().getMarca().toLowerCase() : "";
                    String modelo = r.getVehiculo() != null && r.getVehiculo().getModelo() != null ? r.getVehiculo().getModelo().toLowerCase() : "";
                    String matricula = r.getVehiculo() != null && r.getVehiculo().getMatricula() != null ? r.getVehiculo().getMatricula().toLowerCase() : "";
                    return desc.contains(filtro) || marca.contains(filtro) || modelo.contains(filtro) || matricula.contains(filtro);
                })
                .sorted(Comparator.comparing(Reparacion::getId).reversed())
                .map(r -> {
                    double costeMat = calcularCosteMateriales(r.getId());
                    double horas = r.getHorasTrabajoMecanico() != null && r.getHorasTrabajoMecanico() > 0
                            ? r.getHorasTrabajoMecanico() : 1.5;
                    Optional<Factura> optFact = facturaRepository.findByReparacionId(r.getId());

                    double manoObra = Math.round(horas * config.getPrecioHoraManoObra() * 100.0) / 100.0;
                    double base = costeMat + manoObra;
                    double margen = Math.round(base * (config.getMargenBeneficioPorcentaje() / 100.0) * 100.0) / 100.0;
                    double baseImp = base + margen;
                    double totalCalculado = Math.round(baseImp * 1.21 * 100.0) / 100.0;

                    double precioReferencia = optFact.map(Factura::getTotalFactura)
                            .orElse(costeMat > 0 || r.getHorasTrabajoMecanico() > 0 ? totalCalculado : r.getEstimacionInicial());

                    List<String> piezas = reparacionMaterialRepository.findByReparacionId(r.getId()).stream()
                            .map(u -> u.getCantidad() + "x " + u.getMaterial().getNombre())
                            .collect(Collectors.toList());

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", r.getId());
                    item.put("localizador", r.getLocalizador());
                    item.put("vehiculo", r.getVehiculo().getMarca() + " " + r.getVehiculo().getModelo() + " (" + r.getVehiculo().getMatricula() + ")");
                    item.put("descripcionAveria", r.getDescripcionAveria());
                    item.put("estado", r.getEstado().name());
                    item.put("fechaEntrada", r.getFechaEntrada() != null ? r.getFechaEntrada().toString() : "");
                    item.put("diasEstimados", r.getDiasEstimados());
                    item.put("horasTrabajo", r.getHorasTrabajoMecanico());
                    item.put("costeMateriales", Math.round(costeMat * 100.0) / 100.0);
                    item.put("piezasUsadas", piezas);
                    item.put("estimacionInicial", r.getEstimacionInicial());
                    item.put("tieneFactura", optFact.isPresent());
                    item.put("numeroFactura", optFact.map(Factura::getNumeroFactura).orElse(null));
                    item.put("totalFinal", Math.round(precioReferencia * 100.0) / 100.0);
                    return item;
                })
                .collect(Collectors.toList());
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
                    + ". Los estados válidos son: PENDIENTE, EN_PROCESO, FINALIZADA, CONFIRMADA, ENTREGADA, CANCELADA");
        }

        validarTransicionEstado(reparacion.getEstado(), nuevoEstado);

        if (nuevoEstado != EstadoReparacion.PENDIENTE
                && nuevoEstado != EstadoReparacion.CANCELADA
                && reparacion.getMecanico() == null) {
            throw new IllegalArgumentException(
                    "No se puede cambiar el estado a " + nuevoEstado + " porque la reparación no tiene ningún mecánico asignado.");
        }

        if (nuevoEstado == EstadoReparacion.EN_PROCESO || nuevoEstado == EstadoReparacion.FINALIZADA) {
            Optional<PresupuestoReparacion> optPres = presupuestoRepository.findByReparacionId(reparacionId);
            if (optPres.isEmpty()) {
                throw new IllegalArgumentException(
                        "Operación bloqueada: no se puede iniciar ni finalizar la reparación sin haber generado antes el presupuesto desglosado y contar con la aceptación del cliente.");
            }
            String estadoPres = optPres.get().getEstado();
            if (!"ACEPTADO".equalsIgnoreCase(estadoPres)) {
                throw new IllegalArgumentException(
                        "Operación bloqueada: el presupuesto (" + optPres.get().getNumeroPresupuesto()
                                + ") se encuentra en estado " + estadoPres
                                + ". El cliente debe aceptar el presupuesto antes de iniciar o finalizar la reparación.");
            }
        }

        reparacion.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoReparacion.FINALIZADA
                && "EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador())) {
            long transcurrido = calcularSegundosDesdeInicio(reparacion.getInicioTemporizador());
            long totalSegundos = reparacion.getSegundosAcumuladosTemporizador() + transcurrido;
            reparacion.setSegundosAcumuladosTemporizador(totalSegundos);
            reparacion.setInicioTemporizador(null);
            reparacion.setEstadoTemporizador("DETENIDO");
            reparacion.setHorasTrabajoMecanico(Math.round((totalSegundos / 3600.0) * 100.0) / 100.0);
        }

        if (nuevoEstado == EstadoReparacion.FINALIZADA
                || nuevoEstado == EstadoReparacion.CONFIRMADA
                || nuevoEstado == EstadoReparacion.ENTREGADA
                || nuevoEstado == EstadoReparacion.CANCELADA) {
            reparacion.setFechaSalida(LocalDate.now());
        }

        reparacionRepository.save(reparacion);

        // Si pasa a ENTREGADA y aún no tenía factura formalizada, generamos su factura automáticamente
        if (nuevoEstado == EstadoReparacion.ENTREGADA && facturaRepository.findByReparacionId(reparacionId).isEmpty()) {
            generarFactura(reparacionId, reparacion.getHorasTrabajoMecanico());
        }

        if (eventPublisher != null && reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null) {
            Cliente cliente = reparacion.getVehiculo().getCliente();
            String matricula = reparacion.getVehiculo().getMatricula();
            switch (nuevoEstado) {
                case EN_PROCESO:
                    eventPublisher.publishEvent(new NotificacionReparacionEvent(
                            cliente, reparacion, TipoNotificacion.EN_PROCESO,
                            "Reparación en proceso",
                            "El mecánico ha iniciado los trabajos de reparación en tu vehículo (" + matricula + ")."
                    ));
                    break;
                case FINALIZADA:
                    eventPublisher.publishEvent(new NotificacionReparacionEvent(
                            cliente, reparacion, TipoNotificacion.INFO,
                            "Trabajos finalizados - En revisión",
                            "El mecánico ha finalizado los trabajos en tu vehículo (" + matricula + "). El taller está verificando la reparación para dar el visto bueno."
                    ));
                    break;
                case CONFIRMADA:
                    eventPublisher.publishEvent(new NotificacionReparacionEvent(
                            cliente, reparacion, TipoNotificacion.LISTO_PARA_RECOGER,
                            "¡Tu vehículo está listo para recoger!",
                            "La reparación de tu vehículo (" + matricula + ") ha sido revisada y confirmada con éxito. Ya puedes pasar a recogerlo por nuestro taller."
                    ));
                    break;
                case ENTREGADA:
                    eventPublisher.publishEvent(new NotificacionReparacionEvent(
                            cliente, reparacion, TipoNotificacion.ENTREGADA,
                            "Vehículo entregado - Factura disponible",
                            "Tu vehículo (" + matricula + ") ha sido entregado. Muchas gracias por confiar en MotorFlow. Tu factura ya está disponible para descargar."
                    ));
                    break;
                case CANCELADA:
                    eventPublisher.publishEvent(new NotificacionReparacionEvent(
                            cliente, reparacion, TipoNotificacion.CANCELADA,
                            "Reparación cancelada - Vehículo devuelto sin reparar",
                            "La reparación de tu vehículo (" + matricula + ") ha sido cancelada tras rechazar el presupuesto."
                    ));
                    break;
                default:
                    break;
            }
        }

        return convertirAResponseDTO(reparacion,
                "Estado actualizado a " + nuevoEstado.name() + ".");
    }

    /**
     * Devuelve el resumen financiero de la Cuenta General del Taller (Tesorería del Admin),
     * calculado a partir de las facturas reales emitidas y los costes reales del taller.
     */
    public Map<String, Object> obtenerCuentaTaller() {
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
        double fondoInicial = config.getFondoInicial() != null ? config.getFondoInicial() : FONDO_INICIAL_TALLER;

        List<Reparacion> todas = reparacionRepository.findAll();
        List<ReparacionMaterial> todosUsos = reparacionMaterialRepository.findAll();
        List<Factura> facturas = facturaRepository.findAll();

        List<GastoTesoreria> comprasStock = obtenerEntidadesGastosCompras();
        double totalComprasStock = Math.round(comprasStock.stream()
                .mapToDouble(g -> g.getImporte() != null ? g.getImporte() : 0.0)
                .sum() * 100.0) / 100.0;
        this.gastosComprasStock = totalComprasStock;

        // Coste analítico/imputado de piezas instaladas en reparaciones (informativo: no vuelve a restar de la caja)
        double gastosPiezasUsadas = Math.round(todosUsos.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum() * 100.0) / 100.0;

        // El flujo de salida de caja real corresponde únicamente a las compras/reposiciones de stock del almacén
        double gastosMateriales = totalComprasStock;

        // Ingresos cobrados: suma del totalFactura de reparaciones entregadas o facturadas
        double ingresosCobrados = facturas.stream()
                .filter(f -> f.getReparacion().getEstado() == EstadoReparacion.ENTREGADA
                        || f.getReparacion().getEstado() == EstadoReparacion.CONFIRMADA
                        || f.getReparacion().getEstado() == EstadoReparacion.FINALIZADA)
                .mapToDouble(Factura::getTotalFactura)
                .sum();

        // Previsión de ingresos en curso (reparaciones activas sin factura emitida todavía, excluyendo canceladas)
        double ingresosEnCurso = todas.stream()
                .filter(r -> r.getEstado() != EstadoReparacion.CANCELADA)
                .filter(r -> facturaRepository.findByReparacionId(r.getId()).isEmpty())
                .mapToDouble(this::calcularTotalPrevistoReparacion)
                .sum();

        double totalFacturado = facturas.stream()
                .mapToDouble(Factura::getTotalFactura)
                .sum();

        double beneficioManoObra = facturas.stream()
                .mapToDouble(Factura::getSubtotalManoObra)
                .sum();

        double beneficioMargen = facturas.stream()
                .mapToDouble(Factura::getImporteMargen)
                .sum();

        double ivaTotalFacturas = facturas.stream()
                .mapToDouble(Factura::getImporteIva)
                .sum();

        // Beneficio neto real del taller (Mano de Obra + Margen comercial de facturas emitidas)
        double beneficioTotal = Math.round((beneficioManoObra + beneficioMargen) * 100.0) / 100.0;
        double saldoActual = Math.round((fondoInicial - gastosMateriales + ingresosCobrados) * 100.0) / 100.0;

        List<Map<String, Object>> historialGastos = construirListaHistorialGastos(comprasStock, todosUsos);

        Map<String, Object> cuenta = new LinkedHashMap<>();
        cuenta.put("fondoInicial", fondoInicial);
        cuenta.put("gastosMateriales", gastosMateriales);
        cuenta.put("gastosComprasStock", totalComprasStock);
        cuenta.put("gastosPiezasUsadas", gastosPiezasUsadas);
        cuenta.put("historialGastos", historialGastos);
        cuenta.put("ingresosCobrados", Math.round(ingresosCobrados * 100.0) / 100.0);
        cuenta.put("ingresosEnCurso", Math.round(ingresosEnCurso * 100.0) / 100.0);
        cuenta.put("totalFacturado", Math.round(totalFacturado * 100.0) / 100.0);
        cuenta.put("beneficioManoObra", Math.round(beneficioManoObra * 100.0) / 100.0);
        cuenta.put("beneficioMargen", Math.round(beneficioMargen * 100.0) / 100.0);
        cuenta.put("ivaTotalFacturas", Math.round(ivaTotalFacturas * 100.0) / 100.0);
        cuenta.put("totalFacturasEmitidas", facturas.size());
        cuenta.put("beneficioTotal", beneficioTotal);
        cuenta.put("saldoActual", saldoActual);
        cuenta.put("margenBeneficioPorcentaje", config.getMargenBeneficioPorcentaje());
        cuenta.put("precioHoraManoObra", config.getPrecioHoraManoObra());
        return cuenta;
    }

    /**
     * Devuelve el historial completo de gastos registrados en la Tesorería (compras de stock y piezas empleadas).
     */
    public List<Map<String, Object>> obtenerHistorialGastos() {
        List<GastoTesoreria> comprasStock = obtenerEntidadesGastosCompras();
        List<ReparacionMaterial> todosUsos = reparacionMaterialRepository != null
                ? reparacionMaterialRepository.findAll()
                : Collections.emptyList();
        return construirListaHistorialGastos(comprasStock, todosUsos);
    }

    private List<Map<String, Object>> construirListaHistorialGastos(List<GastoTesoreria> comprasStock,
                                                                    List<ReparacionMaterial> todosUsos) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (GastoTesoreria g : comprasStock) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "COMPRA-" + g.getId());
            item.put("registroId", g.getId());
            item.put("origen", "COMPRA");
            item.put("tipo", g.getTipo());
            item.put("fecha", g.getFecha() != null ? g.getFecha().toString() : LocalDate.now().atStartOfDay().toString());
            item.put("materialId", g.getMaterialId());
            item.put("nombreMaterial", g.getNombreMaterial());
            item.put("concepto", g.getConcepto());
            item.put("cantidad", g.getCantidad());
            item.put("precioUnitario", g.getPrecioUnitario());
            item.put("importe", Math.round((g.getImporte() != null ? g.getImporte() : 0.0) * 100.0) / 100.0);
            item.put("descuentaCaja", true);
            item.put("eliminable", true);
            resultado.add(item);
        }

        List<ReparacionMaterial> usosOrdenados = new ArrayList<>(todosUsos);
        usosOrdenados.sort(Comparator.comparing(ReparacionMaterial::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        for (ReparacionMaterial u : usosOrdenados) {
            double precioUnit = (u.getMaterial() != null && u.getMaterial().getPrecioUnitario() != null)
                    ? u.getMaterial().getPrecioUnitario() : 0.0;
            double imp = Math.round((u.getCantidad() * precioUnit) * 100.0) / 100.0;
            String nomMat = (u.getMaterial() != null && u.getMaterial().getNombre() != null)
                    ? u.getMaterial().getNombre() : "Material";
            Reparacion rep = u.getReparacion();
            String matVeh = (rep != null && rep.getVehiculo() != null && rep.getVehiculo().getMatricula() != null)
                    ? rep.getVehiculo().getMatricula() : "";
            String concepto = rep != null
                    ? String.format("Material empleado en Reparación #%d%s: %s (%d uds × %.2f €)",
                            rep.getId(), matVeh.isEmpty() ? "" : " (" + matVeh + ")", nomMat, u.getCantidad(), precioUnit)
                    : String.format("Material empleado: %s (%d uds × %.2f €)", nomMat, u.getCantidad(), precioUnit);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "USO-" + u.getId());
            item.put("registroId", u.getId());
            item.put("origen", "USO_REPARACION");
            item.put("tipo", "USO_REPARACION");
            item.put("fecha", (rep != null && rep.getFechaEntrada() != null)
                    ? rep.getFechaEntrada().atStartOfDay().toString()
                    : LocalDate.now().atStartOfDay().toString());
            item.put("materialId", u.getMaterial() != null ? u.getMaterial().getId() : null);
            item.put("reparacionId", rep != null ? rep.getId() : null);
            item.put("nombreMaterial", nomMat);
            item.put("concepto", concepto);
            item.put("cantidad", u.getCantidad());
            item.put("precioUnitario", precioUnit);
            item.put("importe", imp);
            item.put("descuentaCaja", false);
            item.put("eliminable", true);
            resultado.add(item);
        }

        return resultado;
    }

    /**
     * Elimina un gasto del historial de Tesorería (compra de stock por error o consumo en reparación),
     * reembolsando su importe al saldo de Tesorería y ajustando el stock del inventario en consecuencia.
     */
    public synchronized Map<String, Object> eliminarGastoTesoreria(String gastoKey) {
        if (gastoKey == null || gastoKey.isBlank()) {
            throw new IllegalArgumentException("El identificador del gasto es obligatorio.");
        }
        String cleanKey = gastoKey.trim().toUpperCase();

        if (cleanKey.startsWith("USO-")) {
            Long usoId;
            try {
                usoId = Long.parseLong(cleanKey.substring(4).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Identificador de gasto no válido: " + gastoKey);
            }
            ReparacionMaterial uso = reparacionMaterialRepository.findById(usoId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el gasto con ID: " + gastoKey));

            double precioUnit = (uso.getMaterial() != null && uso.getMaterial().getPrecioUnitario() != null)
                    ? uso.getMaterial().getPrecioUnitario() : 0.0;
            double importe = Math.round((uso.getCantidad() * precioUnit) * 100.0) / 100.0;
            String nomMat = uso.getMaterial() != null ? uso.getMaterial().getNombre() : "Material";
            int cantidad = uso.getCantidad() != null ? uso.getCantidad() : 0;

            if (materialRepository != null && uso.getMaterial() != null && uso.getMaterial().getId() != null) {
                materialRepository.findById(uso.getMaterial().getId()).ifPresent(mat -> {
                    int stockActual = mat.getStockDisponible() != null ? mat.getStockDisponible() : 0;
                    mat.setStockDisponible(stockActual + cantidad);
                    materialRepository.save(mat);
                });
            }

            Long repId = uso.getReparacion() != null ? uso.getReparacion().getId() : null;
            reparacionMaterialRepository.delete(uso);

            if (repId != null && facturaRepository != null && configuracionService != null) {
                facturaRepository.findByReparacionId(repId).ifPresent(factura -> {
                    ConfiguracionTaller cfg = configuracionService.obtenerConfiguracion();
                    double nuevoCosteMat = calcularCosteMateriales(repId);
                    double horas = factura.getHorasManoObra() != null ? factura.getHorasManoObra() : 1.5;
                    factura.recalcularImportes(nuevoCosteMat, horas, cfg.getPrecioHoraManoObra(),
                            cfg.getMargenBeneficioPorcentaje(), 21.0);
                    facturaRepository.save(factura);
                });
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("id", "USO-" + usoId);
            res.put("importeEliminado", importe);
            res.put("unidadesRevertidas", cantidad);
            res.put("nombreMaterial", nomMat);
            res.put("mensaje", String.format("Gasto de %.2f € (%dx %s) eliminado de la Tesorería y unidades devueltas al almacén.",
                    importe, cantidad, nomMat));
            return res;
        }

        String numericPart = cleanKey.startsWith("COMPRA-") ? cleanKey.substring(7).trim() : cleanKey;
        Long compraId;
        try {
            compraId = Long.parseLong(numericPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Identificador de compra no válido: " + gastoKey);
        }

        GastoTesoreria gasto = null;
        if (gastoTesoreriaRepository != null) {
            gasto = gastoTesoreriaRepository.findById(compraId).orElse(null);
        } else {
            for (GastoTesoreria item : gastosComprasMemoria) {
                if (compraId.equals(item.getId())) {
                    gasto = item;
                    break;
                }
            }
        }

        if (gasto == null) {
            throw new IllegalArgumentException("No se encontró el registro de compra con ID: " + gastoKey);
        }

        double importe = gasto.getImporte() != null ? gasto.getImporte() : 0.0;
        int cantidadComprada = gasto.getCantidad() != null ? gasto.getCantidad() : 0;
        String nomMat = gasto.getNombreMaterial();
        int unidadesDescontadas = 0;

        if (materialRepository != null && gasto.getMaterialId() != null && cantidadComprada > 0) {
            Optional<Material> optMat = materialRepository.findById(gasto.getMaterialId());
            if (optMat.isPresent()) {
                Material mat = optMat.get();
                int stockActual = mat.getStockDisponible() != null ? mat.getStockDisponible() : 0;
                int stockPrevio = gasto.getStockPrevio() != null ? Math.max(0, gasto.getStockPrevio()) : 0;
                if (stockActual > stockPrevio) {
                    int nuevoStock = Math.max(stockPrevio, stockActual - cantidadComprada);
                    unidadesDescontadas = stockActual - nuevoStock;
                    mat.setStockDisponible(nuevoStock);
                    materialRepository.save(mat);
                }
            }
        }

        if (gastoTesoreriaRepository != null) {
            gastoTesoreriaRepository.delete(gasto);
        } else {
            gastosComprasMemoria.removeIf(item -> compraId.equals(item.getId()));
        }
        recalcularGastosComprasStockTotal();

        String detalleStock = unidadesDescontadas > 0
                ? String.format(" y se han revertido %d ud(s) del stock de \"%s\"", unidadesDescontadas, nomMat)
                : "";

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", "COMPRA-" + compraId);
        res.put("importeEliminado", importe);
        res.put("unidadesRevertidas", unidadesDescontadas);
        res.put("nombreMaterial", nomMat);
        res.put("mensaje", String.format("Compra anulada correctamente: se han reembolsado %.2f € a la Tesorería%s.",
                importe, detalleStock));
        return res;
    }

    public double calcularCosteMateriales(Long reparacionId) {
        if (reparacionId == null || reparacionMaterialRepository == null) return 0.0;
        return reparacionMaterialRepository.findByReparacionId(reparacionId).stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
    }

    public double calcularTotalPrevistoReparacion(Reparacion r) {
        if (r == null || r.getEstado() == EstadoReparacion.CANCELADA) {
            return 0.0;
        }
        ConfiguracionTaller config = configuracionService != null
                ? configuracionService.obtenerConfiguracion()
                : new ConfiguracionTaller();
        double costeMat = calcularCosteMateriales(r.getId());
        double horas = r.getHorasTrabajoMecanico() != null ? r.getHorasTrabajoMecanico() : 0.0;
        if (costeMat == 0.0 && horas == 0.0) {
            double impacto = (incidenciaRepository != null && r.getId() != null)
                    ? incidenciaRepository.findByReparacionIdOrderByFechaAsc(r.getId()).stream()
                    .mapToDouble(i -> i.getImpactoPrecio() != null ? i.getImpactoPrecio() : 0.0)
                    .sum()
                    : 0.0;
            return Math.round(((r.getEstimacionInicial() != null ? r.getEstimacionInicial() : 150.0) + impacto) * 100.0) / 100.0;
        }
        double manoObra = horas * config.getPrecioHoraManoObra();
        double subtotalBase = costeMat + manoObra;
        double margen = subtotalBase * (config.getMargenBeneficioPorcentaje() / 100.0);
        double baseImponible = subtotalBase + margen;
        double totalConIva = baseImponible * 1.21;
        return Math.round(totalConIva * 100.0) / 100.0;
    }

    public List<ReparacionResponseDTO> obtenerTodas() {
        return obtenerTodas(null);
    }

    /**
     * HU #7: Devuelve todas las reparaciones con ordenación dinámica opcional por categorías operativas.
     */
    public List<ReparacionResponseDTO> obtenerTodas(String orden) {
        List<ReparacionResponseDTO> lista = reparacionRepository.findAll().stream()
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
        return ordenarReparacionesPorCriterio(lista, orden);
    }

    /**
     * HU #7: Ordena una lista de reparaciones según criterios operativos del Mecánico Jefe:
     * LLEGADA_ASC (orden de llegada), LLEGADA_DESC, TIEMPO_ASC, TIEMPO_DESC, PIEZAS_DESC, PIEZAS_ASC.
     */
    public List<ReparacionResponseDTO> ordenarReparacionesPorCriterio(List<ReparacionResponseDTO> lista, String criterio) {
        if (lista == null || lista.isEmpty() || criterio == null || criterio.isBlank()) {
            return lista;
        }
        String norm = criterio.trim().toUpperCase(Locale.ROOT);
        List<ReparacionResponseDTO> copia = new ArrayList<>(lista);

        switch (norm) {
            case "LLEGADA_ASC":
            case "ORDEN_LLEGADA":
                copia.sort(Comparator
                        .comparing((ReparacionResponseDTO r) -> r.getFechaHoraEntrada() != null ? r.getFechaHoraEntrada() : "")
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L));
                break;
            case "LLEGADA_DESC":
                copia.sort(Comparator
                        .comparing((ReparacionResponseDTO r) -> r.getFechaHoraEntrada() != null ? r.getFechaHoraEntrada() : "")
                        .reversed()
                        .thenComparing(Comparator.comparing((ReparacionResponseDTO r) -> r.getId() != null ? r.getId() : 0L).reversed()));
                break;
            case "TIEMPO_ASC":
            case "TIEMPO_ESTIMADO":
                copia.sort(Comparator
                        .comparing((ReparacionResponseDTO r) -> r.getDiasEstimados() != null ? r.getDiasEstimados() : Integer.MAX_VALUE)
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L));
                break;
            case "TIEMPO_DESC":
                copia.sort(Comparator
                        .comparing((ReparacionResponseDTO r) -> r.getDiasEstimados() != null ? r.getDiasEstimados() : 0)
                        .reversed()
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L));
                break;
            case "PIEZAS_DESC":
            case "USO_PIEZAS":
                copia.sort(Comparator
                        .comparingInt((ReparacionResponseDTO r) -> r.getTotalUnidadesPiezas() != null ? r.getTotalUnidadesPiezas() : 0)
                        .reversed()
                        .thenComparing(Comparator.comparingDouble((ReparacionResponseDTO r) -> r.getCosteMateriales() != null ? r.getCosteMateriales() : 0.0).reversed())
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L));
                break;
            case "PIEZAS_ASC":
                copia.sort(Comparator
                        .comparingInt((ReparacionResponseDTO r) -> r.getTotalUnidadesPiezas() != null ? r.getTotalUnidadesPiezas() : 0)
                        .thenComparingDouble(r -> r.getCosteMateriales() != null ? r.getCosteMateriales() : 0.0)
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L));
                break;
            default:
                break;
        }
        return copia;
    }

    /**
     * HU #8: Registro cronológico de vehículos pendientes de entrar a reparación,
     * ordenados automáticamente del que lleva más tiempo esperando en el taller al más reciente.
     */
    public List<ReparacionResponseDTO> obtenerRegistroCronologicoEspera() {
        return reparacionRepository.findAll().stream()
                .filter(r -> r.getEstado() == EstadoReparacion.PENDIENTE)
                .sorted(Comparator
                        .comparing((Reparacion r) -> r.getFechaHoraEntrada() != null
                                ? r.getFechaHoraEntrada()
                                : (r.getFechaEntrada() != null ? r.getFechaEntrada().atTime(9, 0) : LocalDateTime.now()))
                        .thenComparing(r -> r.getId() != null ? r.getId() : 0L))
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    /**
     * HU #20: Supervisión del trabajo de los mecánicos (resumen de horas y desglose por coche).
     * Protegido por RBAC: bloquea el acceso si solicita un rol MECANICO o CLIENTE.
     */
    public Map<String, Object> obtenerSupervisionTiemposMecanicos(Long mecanicoId, String periodo, String rolSolicitante) {
        if (rolSolicitante != null && !rolSolicitante.isBlank() && !"ADMIN".equalsIgnoreCase(rolSolicitante.trim())) {
            throw new SecurityException(
                    "Acceso denegado: la supervisión de tiempos del equipo solo está disponible para el administrador.");
        }

        String periodoNorm = (periodo != null && !periodo.isBlank())
                ? periodo.trim().toUpperCase(Locale.ROOT)
                : "TODOS";
        LocalDate hoy = LocalDate.now();
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate inicioMes = hoy.withDayOfMonth(1);

        List<Usuario> mecanicos = usuarioRepository.findByRol(Rol.MECANICO);
        if (mecanicos == null) {
            mecanicos = Collections.emptyList();
        }
        if (mecanicoId != null) {
            mecanicos = mecanicos.stream()
                    .filter(m -> mecanicoId.equals(m.getId()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> resumenMecanicos = new ArrayList<>();
        long totalSegundosGlobal = 0L;
        int totalReparacionesGlobal = 0;

        for (Usuario mec : mecanicos) {
            List<Reparacion> repsMec = reparacionRepository.findByMecanicoId(mec.getId());
            if (repsMec == null) {
                repsMec = Collections.emptyList();
            }

            List<Map<String, Object>> desglose = new ArrayList<>();
            long totalSegMec = 0L;

            List<Reparacion> ordenadas = new ArrayList<>(repsMec);
            ordenadas.sort(Comparator
                    .comparing((Reparacion r) -> r.getFechaEntrada(), Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(r -> r.getId(), Comparator.nullsLast(Comparator.reverseOrder())));

            for (Reparacion r : ordenadas) {
                if (r.getEstado() == EstadoReparacion.CANCELADA) {
                    continue;
                }
                LocalDate fechaRef = r.getFechaSalida() != null ? r.getFechaSalida() : r.getFechaEntrada();
                if (!cumpleFiltroPeriodo(fechaRef, periodoNorm, hoy, inicioSemana, inicioMes)) {
                    continue;
                }

                long seg = calcularSegundosInvertidosReparacion(r);
                if (seg <= 0L && !"EN_MARCHA".equalsIgnoreCase(r.getEstadoTemporizador())) {
                    continue;
                }

                double horasDec = Math.round((seg / 3600.0) * 100.0) / 100.0;
                totalSegMec += seg;

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("reparacionId", r.getId());
                item.put("localizador", r.getLocalizador());
                item.put("matriculaVehiculo", r.getVehiculo() != null ? r.getVehiculo().getMatricula() : "-");
                item.put("marcaVehiculo", r.getVehiculo() != null ? r.getVehiculo().getMarca() : "");
                item.put("modeloVehiculo", r.getVehiculo() != null ? r.getVehiculo().getModelo() : "");
                item.put("descripcionAveria", r.getDescripcionAveria());
                item.put("estado", r.getEstado() != null ? r.getEstado().name() : "PENDIENTE");
                item.put("estadoTemporizador", r.getEstadoTemporizador());
                item.put("fechaEntrada", r.getFechaEntrada() != null ? r.getFechaEntrada().toString() : "");
                item.put("fechaSalida", r.getFechaSalida() != null ? r.getFechaSalida().toString() : "");
                item.put("segundosInvertidos", seg);
                item.put("horasTrabajo", horasDec);
                item.put("tiempoFormateado", formatearDuracionHorasMinutos(seg));
                desglose.add(item);
            }

            totalSegundosGlobal += totalSegMec;
            totalReparacionesGlobal += desglose.size();

            Map<String, Object> mecDto = new LinkedHashMap<>();
            mecDto.put("mecanicoId", mec.getId());
            mecDto.put("nombreMecanico", mec.getNombre());
            mecDto.put("emailMecanico", mec.getEmail());
            mecDto.put("numeroReparaciones", desglose.size());
            mecDto.put("totalSegundos", totalSegMec);
            mecDto.put("totalHoras", Math.round((totalSegMec / 3600.0) * 100.0) / 100.0);
            mecDto.put("tiempoTotalFormateado", formatearDuracionHorasMinutos(totalSegMec));
            mecDto.put("desgloseReparaciones", desglose);
            resumenMecanicos.add(mecDto);
        }

        boolean sinRegistros = totalReparacionesGlobal == 0;
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("periodo", periodoNorm);
        resultado.put("mecanicoIdFiltro", mecanicoId);
        resultado.put("mecanicos", resumenMecanicos);
        resultado.put("totalReparacionesGlobal", totalReparacionesGlobal);
        resultado.put("totalSegundosGlobal", totalSegundosGlobal);
        resultado.put("totalHorasGlobal", Math.round((totalSegundosGlobal / 3600.0) * 100.0) / 100.0);
        resultado.put("tiempoTotalGlobalFormateado", formatearDuracionHorasMinutos(totalSegundosGlobal));
        resultado.put("sinRegistros", sinRegistros);
        resultado.put("mensaje", sinRegistros
                ? "No existen registros de trabajo para ese periodo."
                : "Supervisión de tiempos cargada correctamente.");
        return resultado;
    }

    private boolean cumpleFiltroPeriodo(LocalDate fechaRef, String periodoNorm,
                                        LocalDate hoy, LocalDate inicioSemana, LocalDate inicioMes) {
        if ("TODOS".equals(periodoNorm) || fechaRef == null) {
            return true;
        }
        switch (periodoNorm) {
            case "HOY":
                return fechaRef.equals(hoy);
            case "SEMANA":
                return !fechaRef.isBefore(inicioSemana) && !fechaRef.isAfter(hoy);
            case "MES":
                return !fechaRef.isBefore(inicioMes) && !fechaRef.isAfter(hoy);
            default:
                return true;
        }
    }

    private long calcularSegundosInvertidosReparacion(Reparacion r) {
        long base = r.getSegundosAcumuladosTemporizador();
        if (base == 0L && r.getHorasTrabajoMecanico() > 0.0) {
            base = Math.round(r.getHorasTrabajoMecanico() * 3600.0);
        }
        if ("EN_MARCHA".equalsIgnoreCase(r.getEstadoTemporizador()) && r.getInicioTemporizador() != null) {
            base += calcularSegundosDesdeInicio(r.getInicioTemporizador());
        }
        return base;
    }

    /**
     * HU #21: Historial de reparaciones anteriores concluidas (FINALIZADA, CONFIRMADA, ENTREGADA)
     * de un vehículo para consulta técnica en modo solo lectura por los mecánicos.
     */
    public List<ReparacionResponseDTO> obtenerHistorialVehiculoParaMecanico(Long vehiculoId, Long excluirReparacionId) {
        if (vehiculoId == null) {
            return Collections.emptyList();
        }
        return reparacionRepository.findByVehiculoId(vehiculoId).stream()
                .filter(r -> r.getEstado() == EstadoReparacion.FINALIZADA
                        || r.getEstado() == EstadoReparacion.CONFIRMADA
                        || r.getEstado() == EstadoReparacion.ENTREGADA)
                .filter(r -> excluirReparacionId == null || !excluirReparacionId.equals(r.getId()))
                .sorted(Comparator
                        .comparing((Reparacion r) -> r.getFechaSalida() != null ? r.getFechaSalida() : r.getFechaEntrada(),
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(r -> r.getId(), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(r -> convertirAResponseDTO(r, null))
                .collect(Collectors.toList());
    }

    private String formatearEstanciaTexto(long minutosTotales) {
        if (minutosTotales < 60) {
            return Math.max(1L, minutosTotales) + " min";
        }
        long dias = minutosTotales / (24 * 60);
        long horas = (minutosTotales % (24 * 60)) / 60;
        long mins = minutosTotales % 60;
        if (dias > 0) {
            return dias + (dias == 1 ? " día" : " días") + " y " + horas + (horas == 1 ? " hora" : " horas");
        }
        return horas + (horas == 1 ? " hora" : " horas") + " y " + mins + " min";
    }

    private String formatearDuracionHorasMinutos(long segundosTotales) {
        long minsTotales = Math.max(0L, Math.round(segundosTotales / 60.0));
        long horas = minsTotales / 60;
        long mins = minsTotales % 60;
        return horas + " h " + mins + " min";
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
        if (actual == EstadoReparacion.CANCELADA) {
            throw new IllegalArgumentException(
                    "La reparación ya está CANCELADA, no se puede modificar su estado.");
        }
        if (nuevo == EstadoReparacion.CANCELADA) {
            if (actual == EstadoReparacion.FINALIZADA || actual == EstadoReparacion.CONFIRMADA) {
                throw new IllegalArgumentException(
                        "No se puede cancelar una reparación que ya ha sido finalizada o confirmada.");
            }
            return;
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

    public ReparacionResponseDTO convertirAResponseDTO(Reparacion reparacion, String mensaje) {
        ConfiguracionTaller config = configuracionService != null
                ? configuracionService.obtenerConfiguracion()
                : new ConfiguracionTaller();

        List<ReparacionMaterial> usados = (reparacionMaterialRepository != null && reparacion.getId() != null)
                ? reparacionMaterialRepository.findByReparacionId(reparacion.getId())
                : Collections.emptyList();
        double costeMateriales = Math.round(usados.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum() * 100.0) / 100.0;

        double estimacionInicial = reparacion.getEstimacionInicial() != null ? reparacion.getEstimacionInicial() : 150.0;
        int diasEstimados = reparacion.getDiasEstimados();
        LocalDate fechaEstimadaFin = reparacion.getFechaEstimadaFin();
        long diasRestantes = 0L;
        if (reparacion.getEstado() != EstadoReparacion.ENTREGADA
                && reparacion.getEstado() != EstadoReparacion.CANCELADA
                && fechaEstimadaFin != null) {
            diasRestantes = Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), fechaEstimadaFin));
        }

        double horasTrabajo = reparacion.getHorasTrabajoMecanico();
        double precioHora = config.getPrecioHoraManoObra();
        double subtotalManoObra = Math.round(horasTrabajo * precioHora * 100.0) / 100.0;
        double porcentajeMargen = config.getMargenBeneficioPorcentaje();

        List<Map<String, Object>> incidenciasDto = (incidenciaRepository != null && reparacion.getId() != null)
                ? incidenciaRepository.findByReparacionIdOrderByFechaAsc(reparacion.getId())
                    .stream()
                    .map(this::mapearIncidencia)
                    .collect(Collectors.toList())
                : Collections.emptyList();

        double impactoIncidencias = Math.round(incidenciasDto.stream()
                .mapToDouble(m -> m.get("impactoPrecio") != null ? ((Number) m.get("impactoPrecio")).doubleValue() : 0.0)
                .sum() * 100.0) / 100.0;

        Optional<Factura> optFactura = (facturaRepository != null && reparacion.getId() != null)
                ? facturaRepository.findByReparacionId(reparacion.getId())
                : Optional.empty();
        Map<String, Object> facturaMap = optFactura.map(this::mapearFacturaCompleta).orElse(null);

        Optional<PresupuestoReparacion> optPresupuesto = (presupuestoRepository != null && reparacion.getId() != null)
                ? presupuestoRepository.findByReparacionId(reparacion.getId())
                : Optional.empty();
        Map<String, Object> presupuestoMap = optPresupuesto.map(this::mapearPresupuestoCompleto).orElse(null);
        String estadoPresupuesto = optPresupuesto.map(PresupuestoReparacion::getEstado).orElse("SIN_PRESUPUESTO");

        double totalPrevisto = calcularTotalPrevistoReparacion(reparacion);
        Double totalFinalFactura = optFactura.map(Factura::getTotalFactura).orElse(null);

        // Beneficio neto del taller en esta reparación (mano de obra + margen sobre base, o estimación - materiales si aún está pendiente)
        double beneficio;
        if (reparacion.getEstado() == EstadoReparacion.CANCELADA) {
            beneficio = 0.0;
        } else if (optFactura.isPresent()) {
            Factura f = optFactura.get();
            beneficio = Math.round((f.getSubtotalManoObra() + f.getImporteMargen()) * 100.0) / 100.0;
        } else if (costeMateriales > 0 || horasTrabajo > 0) {
            double base = costeMateriales + subtotalManoObra;
            double margen = base * (porcentajeMargen / 100.0);
            beneficio = Math.round((subtotalManoObra + margen) * 100.0) / 100.0;
        } else {
            beneficio = Math.round((estimacionInicial - costeMateriales) * 100.0) / 100.0;
        }

        List<String> materialesDetalle = usados.stream()
                .map(u -> String.format("%dx %s (%.2f €)",
                        u.getCantidad(),
                        u.getMaterial().getNombre(),
                        u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0)))
                .collect(Collectors.toList());

        ReparacionResponseDTO dto = new ReparacionResponseDTO();
        dto.setId(reparacion.getId());
        dto.setLocalizador(reparacion.getLocalizador());
        dto.setVehiculoId(reparacion.getVehiculo() != null ? reparacion.getVehiculo().getId() : null);
        dto.setMatriculaVehiculo(reparacion.getVehiculo() != null ? reparacion.getVehiculo().getMatricula() : "-");
        dto.setMarcaVehiculo(reparacion.getVehiculo() != null ? reparacion.getVehiculo().getMarca() : "");
        dto.setModeloVehiculo(reparacion.getVehiculo() != null ? reparacion.getVehiculo().getModelo() : "");
        dto.setNombreCliente(reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null
                ? reparacion.getVehiculo().getCliente().getNombre() : "-");
        dto.setTelefonoCliente(reparacion.getVehiculo() != null && reparacion.getVehiculo().getCliente() != null
                ? reparacion.getVehiculo().getCliente().getTelefono() : "");
        dto.setMecanicoId(reparacion.getMecanico() != null ? reparacion.getMecanico().getId() : null);
        dto.setNombreMecanico(reparacion.getMecanico() != null ? reparacion.getMecanico().getNombre() : "Sin asignar");
        dto.setNombreRecepcionista(reparacion.getRecepcionista() != null ? reparacion.getRecepcionista().getNombre() : "-");
        dto.setFechaEntrada(reparacion.getFechaEntrada());

        LocalDateTime fhEntrada = reparacion.getFechaHoraEntrada() != null
                ? reparacion.getFechaHoraEntrada()
                : (reparacion.getFechaEntrada() != null ? reparacion.getFechaEntrada().atTime(9, 0) : LocalDateTime.now());
        long minutosEstancia = Math.max(0L, Duration.between(fhEntrada, LocalDateTime.now()).toMinutes());
        dto.setFechaHoraEntrada(fhEntrada.toString());
        dto.setMinutosEstancia(minutosEstancia);
        dto.setTiempoEstanciaTexto(formatearEstanciaTexto(minutosEstancia));

        int totalUnidadesPiezas = usados.stream()
                .mapToInt(u -> u.getCantidad() != null ? u.getCantidad() : 0)
                .sum();
        dto.setTotalUnidadesPiezas(totalUnidadesPiezas);

        long segundosAcum = reparacion.getSegundosAcumuladosTemporizador();
        if (segundosAcum == 0L && horasTrabajo > 0.0) {
            segundosAcum = Math.round(horasTrabajo * 3600.0);
        }
        long segundosEfectivos = segundosAcum;
        if ("EN_MARCHA".equalsIgnoreCase(reparacion.getEstadoTemporizador()) && reparacion.getInicioTemporizador() != null) {
            segundosEfectivos += calcularSegundosDesdeInicio(reparacion.getInicioTemporizador());
        }
        dto.setEstadoTemporizador(reparacion.getEstadoTemporizador());
        dto.setInicioTemporizador(reparacion.getInicioTemporizador() != null ? reparacion.getInicioTemporizador().toString() : null);
        dto.setSegundosAcumuladosTemporizador(segundosAcum);
        dto.setTiempoTrabajoFormateado(formatearDuracionHorasMinutos(segundosEfectivos));

        dto.setFechaSalida(reparacion.getFechaSalida());
        dto.setEstado(reparacion.getEstado().name());
        dto.setDescripcionAveria(reparacion.getDescripcionAveria());
        dto.setPresupuesto(estimacionInicial);
        dto.setEstimacionInicial(estimacionInicial);
        dto.setDiasEstimados(diasEstimados);
        dto.setFechaEstimadaFin(fechaEstimadaFin);
        dto.setDiasRestantes(diasRestantes);
        dto.setHorasTrabajoMecanico(horasTrabajo);
        dto.setPrecioHoraManoObra(precioHora);
        dto.setSubtotalManoObra(subtotalManoObra);
        dto.setPorcentajeMargen(porcentajeMargen);
        dto.setCosteMateriales(costeMateriales);
        dto.setImpactoIncidencias(impactoIncidencias);
        dto.setTotalCalculadoPrevisto(totalPrevisto);
        dto.setTotalFinalFactura(totalFinalFactura);
        dto.setBeneficio(beneficio);
        dto.setPresupuestoEstimado(estimacionInicial);
        dto.setPresupuestoMecanico(0.0);
        dto.setPresupuestoRestante(0.0);
        dto.setMaterialesDetalle(materialesDetalle);
        dto.setIncidencias(incidenciasDto);
        dto.setFactura(facturaMap);
        dto.setPresupuestoFormal(presupuestoMap);
        dto.setEstadoPresupuesto(estadoPresupuesto);
        dto.setDniCifCliente(reparacion.getVehiculo().getCliente().getDniCif() != null ? reparacion.getVehiculo().getCliente().getDniCif() : "");
        dto.setDireccionFiscalCliente(reparacion.getVehiculo().getCliente().getDireccionFiscal() != null ? reparacion.getVehiculo().getCliente().getDireccionFiscal() : "");
        dto.setClienteTieneCuenta(reparacion.getVehiculo().getCliente().tieneCuenta());
        dto.setMensaje(mensaje);
        return dto;
    }
}
