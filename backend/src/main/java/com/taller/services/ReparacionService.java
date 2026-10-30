package com.taller.services;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.models.*;
import com.taller.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    @Autowired
    private IncidenciaReparacionRepository incidenciaRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ConfiguracionTallerService configuracionService;

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

        reparacion.setMecanico(mecanico);
        if (reparacion.getEstado() == EstadoReparacion.PENDIENTE) {
            reparacion.setEstado(EstadoReparacion.EN_PROCESO);
        }
        reparacionRepository.save(reparacion);

        return convertirAResponseDTO(reparacion,
                "Mecánico " + mecanico.getNombre() + " asignado correctamente.");
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

        double horasRedondeadas = Math.round(horas * 100.0) / 100.0;
        reparacion.setHorasTrabajoMecanico(horasRedondeadas);
        reparacionRepository.save(reparacion);

        // Si ya existía una factura preliminar, la sincronizamos con las nuevas horas
        facturaRepository.findByReparacionId(reparacionId).ifPresent(factura -> {
            ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
            double costeMat = calcularCosteMateriales(reparacionId);
            factura.recalcularImportes(costeMat, horasRedondeadas, config.getPrecioHoraManoObra(),
                    config.getMargenBeneficioPorcentaje(), 21.0);
            facturaRepository.save(factura);
        });

        return convertirAResponseDTO(reparacion,
                String.format("Horas de mano de obra actualizadas a %.2f h.", horasRedondeadas));
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
     * Genera o actualiza la Factura formal de una reparación basándose en:
     * - Coste real de piezas usadas
     * - Horas de mano de obra trabajadas × precioHora (configurado por el Admin)
     * - Margen de beneficio del taller (% configurado por el Admin sobre materiales + mano de obra)
     * - IVA (21%)
     */
    public synchronized Map<String, Object> generarFactura(Long reparacionId, Double horasManoObraOpcional) {
        Reparacion reparacion = reparacionRepository.findById(reparacionId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la reparación con ID: " + reparacionId));

        if (reparacion.getEstado() == EstadoReparacion.PENDIENTE) {
            throw new IllegalArgumentException("No se puede facturar una reparación que aún está PENDIENTE de iniciar.");
        }

        if (horasManoObraOpcional != null && horasManoObraOpcional >= 0) {
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
            factura.setFechaEmision(LocalDate.now());
            factura.recalcularImportes(costeMateriales, horas, precioHora, porcentajeMargen, 21.0);
        } else {
            String numeroFactura = generarNumeroFacturaUnico();
            factura = new Factura(numeroFactura, reparacion, costeMateriales, horas, precioHora, porcentajeMargen, 21.0);
        }

        facturaRepository.save(factura);
        reparacion.setFactura(factura);

        Map<String, Object> dto = mapearFacturaCompleta(factura);
        dto.put("mensaje", "Factura " + factura.getNumeroFactura() + " formalizada correctamente.");
        return dto;
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

        // Si pasa a ENTREGADA y aún no tenía factura formalizada, generamos su factura automáticamente
        if (nuevoEstado == EstadoReparacion.ENTREGADA && facturaRepository.findByReparacionId(reparacionId).isEmpty()) {
            generarFactura(reparacionId, reparacion.getHorasTrabajoMecanico());
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

        double gastosPiezasUsadas = todosUsos.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
        double gastosMateriales = Math.round((gastosPiezasUsadas + gastosComprasStock) * 100.0) / 100.0;

        // Ingresos cobrados: suma del totalFactura de reparaciones entregadas o facturadas
        double ingresosCobrados = facturas.stream()
                .filter(f -> f.getReparacion().getEstado() == EstadoReparacion.ENTREGADA
                        || f.getReparacion().getEstado() == EstadoReparacion.CONFIRMADA
                        || f.getReparacion().getEstado() == EstadoReparacion.FINALIZADA)
                .mapToDouble(Factura::getTotalFactura)
                .sum();

        // Previsión de ingresos en curso (reparaciones sin factura emitida todavía)
        double ingresosEnCurso = todas.stream()
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

        Map<String, Object> cuenta = new LinkedHashMap<>();
        cuenta.put("fondoInicial", fondoInicial);
        cuenta.put("gastosMateriales", gastosMateriales);
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

    public double calcularCosteMateriales(Long reparacionId) {
        if (reparacionId == null) return 0.0;
        return reparacionMaterialRepository.findByReparacionId(reparacionId).stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum();
    }

    public double calcularTotalPrevistoReparacion(Reparacion r) {
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();
        double costeMat = calcularCosteMateriales(r.getId());
        double horas = r.getHorasTrabajoMecanico() != null ? r.getHorasTrabajoMecanico() : 0.0;
        if (costeMat == 0.0 && horas == 0.0) {
            double impacto = incidenciaRepository.findByReparacionIdOrderByFechaAsc(r.getId()).stream()
                    .mapToDouble(i -> i.getImpactoPrecio() != null ? i.getImpactoPrecio() : 0.0)
                    .sum();
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

    public ReparacionResponseDTO convertirAResponseDTO(Reparacion reparacion, String mensaje) {
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();

        List<ReparacionMaterial> usados = reparacionMaterialRepository.findByReparacionId(reparacion.getId());
        double costeMateriales = Math.round(usados.stream()
                .mapToDouble(u -> u.getCantidad() * (u.getMaterial().getPrecioUnitario() != null ? u.getMaterial().getPrecioUnitario() : 0.0))
                .sum() * 100.0) / 100.0;

        double estimacionInicial = reparacion.getEstimacionInicial() != null ? reparacion.getEstimacionInicial() : 150.0;
        int diasEstimados = reparacion.getDiasEstimados();
        LocalDate fechaEstimadaFin = reparacion.getFechaEstimadaFin();
        long diasRestantes = 0L;
        if (reparacion.getEstado() != EstadoReparacion.ENTREGADA && fechaEstimadaFin != null) {
            diasRestantes = Math.max(0L, ChronoUnit.DAYS.between(LocalDate.now(), fechaEstimadaFin));
        }

        double horasTrabajo = reparacion.getHorasTrabajoMecanico();
        double precioHora = config.getPrecioHoraManoObra();
        double subtotalManoObra = Math.round(horasTrabajo * precioHora * 100.0) / 100.0;
        double porcentajeMargen = config.getMargenBeneficioPorcentaje();

        List<Map<String, Object>> incidenciasDto = incidenciaRepository
                .findByReparacionIdOrderByFechaAsc(reparacion.getId())
                .stream()
                .map(this::mapearIncidencia)
                .collect(Collectors.toList());

        double impactoIncidencias = Math.round(incidenciasDto.stream()
                .mapToDouble(m -> m.get("impactoPrecio") != null ? ((Number) m.get("impactoPrecio")).doubleValue() : 0.0)
                .sum() * 100.0) / 100.0;

        Optional<Factura> optFactura = facturaRepository.findByReparacionId(reparacion.getId());
        Map<String, Object> facturaMap = optFactura.map(this::mapearFacturaCompleta).orElse(null);

        double totalPrevisto = calcularTotalPrevistoReparacion(reparacion);
        Double totalFinalFactura = optFactura.map(Factura::getTotalFactura).orElse(null);

        // Beneficio neto del taller en esta reparación (mano de obra + margen sobre base, o estimación - materiales si aún está pendiente)
        double beneficio;
        if (optFactura.isPresent()) {
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
        dto.setMatriculaVehiculo(reparacion.getVehiculo().getMatricula());
        dto.setMarcaVehiculo(reparacion.getVehiculo().getMarca());
        dto.setModeloVehiculo(reparacion.getVehiculo().getModelo());
        dto.setNombreCliente(reparacion.getVehiculo().getCliente().getNombre());
        dto.setTelefonoCliente(reparacion.getVehiculo().getCliente().getTelefono());
        dto.setNombreMecanico(reparacion.getMecanico() != null ? reparacion.getMecanico().getNombre() : "Sin asignar");
        dto.setNombreRecepcionista(reparacion.getRecepcionista().getNombre());
        dto.setFechaEntrada(reparacion.getFechaEntrada());
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
        dto.setMensaje(mensaje);
        return dto;
    }
}
