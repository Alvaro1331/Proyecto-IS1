package com.taller.services;

import com.taller.models.*;
import com.taller.repositories.CitaPreviaRepository;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de Citas Previas de clientes y el Calendario de Capacidad del Taller
 * (Historias de Usuario 4 y 5).
 */
@Service
public class CitaPreviaService {

    public static final int CAPACIDAD_MAXIMA_DIARIA = 8;
    public static final int CAPACIDAD_MAXIMA_FRANJA = 2;

    public static final List<String> FRANJAS_HORARIAS = List.of(
            "09:00", "10:00", "11:00", "12:00", "13:00", "16:00", "17:00", "18:00"
    );

    private static final Set<MonthDay> FESTIVOS_NACIONALES = Set.of(
            MonthDay.of(1, 1),   // Año Nuevo
            MonthDay.of(1, 6),   // Reyes
            MonthDay.of(5, 1),   // Día del Trabajo
            MonthDay.of(8, 15),  // Asunción
            MonthDay.of(10, 12), // Fiesta Nacional
            MonthDay.of(11, 1),  // Todos los Santos
            MonthDay.of(12, 6),  // Día de la Constitución
            MonthDay.of(12, 8),  // Inmaculada Concepción
            MonthDay.of(12, 25)  // Navidad
    );

    @Autowired
    private CitaPreviaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    public boolean esFinDeSemana(LocalDate fecha) {
        DayOfWeek dow = fecha.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    public boolean esFestivo(LocalDate fecha) {
        return FESTIVOS_NACIONALES.contains(MonthDay.from(fecha));
    }

    public boolean esDiaLaborable(LocalDate fecha) {
        return !esFinDeSemana(fecha) && !esFestivo(fecha);
    }

    /**
     * Devuelve la disponibilidad detallada de un día concreto (franjas horarias y aforo).
     */
    public Map<String, Object> obtenerDisponibilidadDia(LocalDate fecha) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("fecha", fecha.toString());
        res.put("capacidadMaximaDiaria", CAPACIDAD_MAXIMA_DIARIA);
        res.put("capacidadMaximaFranja", CAPACIDAD_MAXIMA_FRANJA);

        boolean pasada = fecha.isBefore(LocalDate.now());
        boolean finSemana = esFinDeSemana(fecha);
        boolean festivo = esFestivo(fecha);
        boolean habilitado = !pasada && !finSemana && !festivo;

        String motivoBloqueo = null;
        if (pasada) motivoBloqueo = "No se pueden agendar citas en fechas pasadas.";
        else if (finSemana) motivoBloqueo = "El taller permanece cerrado los fines de semana (sábados y domingos).";
        else if (festivo) motivoBloqueo = "El día seleccionado es festivo oficial.";

        List<CitaPrevia> citasDia = citaRepository.findByFechaOrderByFranjaHorariaAsc(fecha);
        List<CitaPrevia> citasActivas = citasDia.stream()
                .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        int ocupadasDia = citasActivas.size();
        boolean diaCompleto = ocupadasDia >= CAPACIDAD_MAXIMA_DIARIA;
        if (habilitado && diaCompleto) {
            motivoBloqueo = "Capacidad máxima superada: el taller tiene el aforo completo (" + ocupadasDia + "/" + CAPACIDAD_MAXIMA_DIARIA + " vehículos) para este día.";
        }

        List<Map<String, Object>> franjas = new ArrayList<>();
        LocalTime ahora = LocalTime.now();
        boolean esHoy = fecha.equals(LocalDate.now());

        for (String horaStr : FRANJAS_HORARIAS) {
            long ocupadasFranja = citasActivas.stream()
                    .filter(c -> horaStr.equals(c.getFranjaHoraria()))
                    .count();
            LocalTime horaFranja = LocalTime.parse(horaStr);
            boolean horaPasada = esHoy && horaFranja.isBefore(ahora);
            boolean franjaCompleta = ocupadasFranja >= CAPACIDAD_MAXIMA_FRANJA || diaCompleto;
            boolean disponible = habilitado && !horaPasada && !franjaCompleta;

            Map<String, Object> f = new LinkedHashMap<>();
            f.put("franja", horaStr);
            f.put("ocupadas", ocupadasFranja);
            f.put("capacidad", CAPACIDAD_MAXIMA_FRANJA);
            f.put("plazasLibres", Math.max(0, CAPACIDAD_MAXIMA_FRANJA - (int) ocupadasFranja));
            f.put("horaPasada", horaPasada);
            f.put("completo", franjaCompleta);
            f.put("disponible", disponible);
            franjas.add(f);
        }

        res.put("habilitado", habilitado && !diaCompleto);
        res.put("esLaborable", !finSemana && !festivo);
        res.put("motivoBloqueo", motivoBloqueo);
        res.put("ocupadasDia", ocupadasDia);
        res.put("plazasLibresDia", Math.max(0, CAPACIDAD_MAXIMA_DIARIA - ocupadasDia));
        res.put("nivelOcupacion", calcularNivelColor(ocupadasDia, CAPACIDAD_MAXIMA_DIARIA));
        res.put("franjas", franjas);
        return res;
    }

    /**
     * Crea una nueva cita previa con control de concurrencia (synchronized)
     * para impedir sobrepasar el aforo por franja o diario.
     */
    public synchronized Map<String, Object> crearCita(Long clienteId, Long vehiculoId, String matriculaManual,
                                                      String nombreClienteManual, String telefonoClienteManual,
                                                      String marcaManual, String modeloManual,
                                                      LocalDate fecha, String franjaHoraria,
                                                      String motivo, String observaciones, String estadoInicial) {
        if (fecha == null) {
            throw new IllegalArgumentException("Debes seleccionar una fecha válida para la cita.");
        }
        if (franjaHoraria == null || !FRANJAS_HORARIAS.contains(franjaHoraria.trim())) {
            throw new IllegalArgumentException("Debes seleccionar un tramo horario válido (" + String.join(", ", FRANJAS_HORARIAS) + ").");
        }
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new IllegalArgumentException("Debes especificar el motivo de la visita.");
        }

        // Validación de fecha u hora pasada
        LocalDate hoy = LocalDate.now();
        if (fecha.isBefore(hoy)) {
            throw new IllegalArgumentException("No se permite agendar citas en fechas pasadas.");
        }
        if (fecha.equals(hoy)) {
            LocalTime horaCita = LocalTime.parse(franjaHoraria.trim());
            if (horaCita.isBefore(LocalTime.now())) {
                throw new IllegalArgumentException("No se permite agendar citas en una franja horaria que ya ha pasado hoy.");
            }
        }

        // Validación de días laborables
        if (esFinDeSemana(fecha)) {
            throw new IllegalArgumentException("No se pueden agendar citas en fines de semana (sábados ni domingos).");
        }
        if (esFestivo(fecha)) {
            throw new IllegalArgumentException("No se pueden agendar citas en días festivos.");
        }

        // Resolver cliente y vehículo
        Vehiculo vehiculo = null;
        Cliente cliente = null;

        if (vehiculoId != null) {
            vehiculo = vehiculoRepository.findById(vehiculoId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el vehículo seleccionado."));
            cliente = vehiculo.getCliente();
        } else if (matriculaManual != null && !matriculaManual.trim().isEmpty()) {
            String mat = matriculaManual.trim().toUpperCase();
            Optional<Vehiculo> optV = vehiculoRepository.findByMatricula(mat);
            if (optV.isPresent()) {
                vehiculo = optV.get();
                cliente = vehiculo.getCliente();
            } else {
                if (clienteId != null) {
                    cliente = clienteRepository.findById(clienteId)
                            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));
                } else if (telefonoClienteManual != null && !telefonoClienteManual.trim().isEmpty()) {
                    String tel = telefonoClienteManual.trim();
                    cliente = clienteRepository.findByTelefono(tel).orElseGet(() -> {
                        String nom = (nombreClienteManual != null && !nombreClienteManual.trim().isEmpty())
                                ? nombreClienteManual.trim() : "Cliente Cita " + tel;
                        return clienteRepository.save(new Cliente(nom, tel));
                    });
                } else {
                    throw new IllegalArgumentException("Debes seleccionar un vehículo registrado o indicar matrícula y teléfono del cliente.");
                }
                String marca = (marcaManual != null && !marcaManual.trim().isEmpty()) ? marcaManual.trim() : "Pendiente";
                String modelo = (modeloManual != null && !modeloManual.trim().isEmpty()) ? modeloManual.trim() : "Cita Previa";
                vehiculo = vehiculoRepository.save(new Vehiculo(mat, marca, modelo, LocalDate.now().getYear(), cliente));
            }
        } else {
            throw new IllegalArgumentException("Debes seleccionar un vehículo para agendar la cita.");
        }

        if (clienteId != null && cliente == null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException("No se encontró el cliente."));
        }

        // Control de concurrencia y capacidad máxima diaria y por franja
        List<CitaPrevia> citasActivasDia = citaRepository.findByFechaOrderByFranjaHorariaAsc(fecha).stream()
                .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.toList());

        if (citasActivasDia.size() >= CAPACIDAD_MAXIMA_DIARIA) {
            throw new IllegalStateException("Capacidad máxima superada (" + CAPACIDAD_MAXIMA_DIARIA + "/" + CAPACIDAD_MAXIMA_DIARIA
                    + " vehículos): el día seleccionado ya tiene el aforo completo. Por favor, elige otro día.");
        }

        long citasEnFranja = citasActivasDia.stream()
                .filter(c -> franjaHoraria.trim().equals(c.getFranjaHoraria()))
                .count();

        if (citasEnFranja >= CAPACIDAD_MAXIMA_FRANJA) {
            throw new IllegalStateException("El horario " + franjaHoraria + " del día " + fecha
                    + " ya no está disponible (aforo de recepción completo). Por favor, elige un nuevo tramo horario.");
        }

        // Evitar que el mismo vehículo agende dos citas activas a la misma hora el mismo día
        final Long vId = vehiculo.getId();
        boolean duplicada = citasActivasDia.stream()
                .anyMatch(c -> c.getVehiculo().getId().equals(vId) && franjaHoraria.trim().equals(c.getFranjaHoraria()));
        if (duplicada) {
            throw new IllegalArgumentException("Este vehículo ya tiene una cita activa reservada en ese mismo día y tramo horario.");
        }

        String codigo = CitaPrevia.generarCodigoCita();
        while (citaRepository.findByCodigoCita(codigo).isPresent()) {
            codigo = CitaPrevia.generarCodigoCita();
        }

        CitaPrevia cita = new CitaPrevia(
                cliente,
                vehiculo,
                fecha,
                franjaHoraria.trim(),
                motivo.trim(),
                observaciones != null ? observaciones.trim() : "",
                estadoInicial != null ? estadoInicial : "CONFIRMADA"
        );
        cita.setCodigoCita(codigo);
        citaRepository.save(cita);

        Map<String, Object> dto = mapearCita(cita);
        dto.put("mensaje", "Cita previa " + cita.getCodigoCita() + " confirmada para el " + fecha + " a las " + franjaHoraria + ".");
        return dto;
    }

    /**
     * Obtiene las citas de un cliente concreto (y de todos sus vehículos asociados).
     */
    public List<Map<String, Object>> obtenerCitasCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado."));

        List<CitaPrevia> citas = citaRepository.findByClienteIdOrderByFechaDescFranjaHorariaAsc(cliente.getId());
        return citas.stream().map(this::mapearCita).collect(Collectors.toList());
    }

    /**
     * Obtiene el calendario completo de citas en un rango de fechas con métricas de capacidad diaria.
     */
    public Map<String, Object> obtenerCalendarioTaller(LocalDate desde, LocalDate hasta, String estadoFiltro) {
        LocalDate inicio = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate fin = hasta != null ? hasta : inicio.plusMonths(1).minusDays(1);

        List<CitaPrevia> todasRango = citaRepository.findByFechaBetweenOrderByFechaAscFranjaHorariaAsc(inicio, fin);

        // Agrupar ocupación real (todas las citas no canceladas cuentan para el aforo del taller)
        Map<String, List<CitaPrevia>> activasPorDia = todasRango.stream()
                .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado()))
                .collect(Collectors.groupingBy(c -> c.getFecha().toString()));

        Map<String, Map<String, Object>> resumenDias = new LinkedHashMap<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fin)) {
            String key = cursor.toString();
            List<CitaPrevia> activas = activasPorDia.getOrDefault(key, Collections.emptyList());
            int ocupadas = activas.size();
            boolean laborable = esDiaLaborable(cursor);

            Map<String, Integer> porFranja = new LinkedHashMap<>();
            for (String f : FRANJAS_HORARIAS) {
                int countF = (int) activas.stream().filter(c -> f.equals(c.getFranjaHoraria())).count();
                porFranja.put(f, countF);
            }

            Map<String, Object> infoDia = new LinkedHashMap<>();
            infoDia.put("fecha", key);
            infoDia.put("esLaborable", laborable);
            infoDia.put("esFinDeSemana", esFinDeSemana(cursor));
            infoDia.put("esFestivo", esFestivo(cursor));
            infoDia.put("ocupadas", ocupadas);
            infoDia.put("capacidadMaxima", CAPACIDAD_MAXIMA_DIARIA);
            infoDia.put("porcentaje", Math.min(100, Math.round((ocupadas * 100.0f) / CAPACIDAD_MAXIMA_DIARIA)));
            infoDia.put("completo", ocupadas >= CAPACIDAD_MAXIMA_DIARIA);
            infoDia.put("nivelColor", calcularNivelColor(ocupadas, CAPACIDAD_MAXIMA_DIARIA));
            infoDia.put("etiquetaCapacidad", ocupadas + "/" + CAPACIDAD_MAXIMA_DIARIA + " vehículos aceptados");
            infoDia.put("ocupacionPorFranja", porFranja);
            resumenDias.put(key, infoDia);

            cursor = cursor.plusDays(1);
        }

        // Aplicar filtro de estado para el listado de citas mostradas
        String filtro = (estadoFiltro != null && !estadoFiltro.isBlank() && !"TODOS".equalsIgnoreCase(estadoFiltro))
                ? estadoFiltro.trim().toUpperCase() : null;

        List<Map<String, Object>> citasFiltradas = todasRango.stream()
                .filter(c -> filtro == null || filtro.equalsIgnoreCase(c.getEstado()))
                .map(this::mapearCita)
                .collect(Collectors.toList());

        List<Map<String, Object>> todasLasCitas = citaRepository.findAll().stream()
                .filter(c -> filtro == null || filtro.equalsIgnoreCase(c.getEstado()))
                .sorted(Comparator.comparing(CitaPrevia::getFecha).thenComparing(CitaPrevia::getFranjaHoraria))
                .map(this::mapearCita)
                .collect(Collectors.toList());

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("desde", inicio.toString());
        respuesta.put("hasta", fin.toString());
        respuesta.put("capacidadMaximaDiaria", CAPACIDAD_MAXIMA_DIARIA);
        respuesta.put("capacidadMaximaFranja", CAPACIDAD_MAXIMA_FRANJA);
        respuesta.put("franjasHorarias", FRANJAS_HORARIAS);
        respuesta.put("resumenDias", resumenDias);
        respuesta.put("citas", citasFiltradas);
        respuesta.put("todasLasCitas", todasLasCitas);
        return respuesta;
    }

    public synchronized Map<String, Object> cambiarEstadoCita(Long citaId, String nuevoEstado) {
        CitaPrevia cita = citaRepository.findById(citaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la cita con ID: " + citaId));

        if (nuevoEstado == null || nuevoEstado.isBlank()) {
            throw new IllegalArgumentException("Debes indicar el nuevo estado de la cita.");
        }
        String estadoNorm = nuevoEstado.trim().toUpperCase();
        if (!List.of("PENDIENTE", "CONFIRMADA", "RECEPCIONADA", "CANCELADA").contains(estadoNorm)) {
            throw new IllegalArgumentException("Estado de cita no válido: " + nuevoEstado);
        }

        // Si se reactiva una cita cancelada, comprobar que quede cupo
        if ("CANCELADA".equalsIgnoreCase(cita.getEstado()) && !"CANCELADA".equals(estadoNorm)) {
            List<CitaPrevia> activas = citaRepository.findByFechaOrderByFranjaHorariaAsc(cita.getFecha()).stream()
                    .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado()))
                    .collect(Collectors.toList());
            if (activas.size() >= CAPACIDAD_MAXIMA_DIARIA) {
                throw new IllegalStateException("Capacidad máxima superada: no se puede reactivar la cita porque el día ya está completo.");
            }
        }

        cita.setEstado(estadoNorm);
        citaRepository.save(cita);

        Map<String, Object> dto = mapearCita(cita);
        dto.put("mensaje", "Estado de la cita " + cita.getCodigoCita() + " actualizado a " + estadoNorm + ".");
        return dto;
    }

    private String calcularNivelColor(int ocupadas, int capacidad) {
        if (ocupadas >= capacidad) return "ROJO";
        double ratio = (double) ocupadas / capacidad;
        if (ratio >= 0.60) return "AMARILLO";
        return "VERDE";
    }

    public Map<String, Object> mapearCita(CitaPrevia c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("codigoCita", c.getCodigoCita());
        m.put("fecha", c.getFecha().toString());
        m.put("franjaHoraria", c.getFranjaHoraria());
        m.put("motivo", c.getMotivo());
        m.put("observaciones", c.getObservaciones() != null ? c.getObservaciones() : "");
        m.put("estado", c.getEstado());
        m.put("reparacionId", c.getReparacionId());
        m.put("clienteId", c.getCliente() != null ? c.getCliente().getId() : null);
        m.put("nombreCliente", c.getCliente() != null ? c.getCliente().getNombre() : "-");
        m.put("telefonoCliente", c.getCliente() != null ? c.getCliente().getTelefono() : "-");
        m.put("vehiculoId", c.getVehiculo() != null ? c.getVehiculo().getId() : null);
        m.put("matriculaVehiculo", c.getVehiculo() != null ? c.getVehiculo().getMatricula() : "-");
        m.put("marcaVehiculo", c.getVehiculo() != null ? c.getVehiculo().getMarca() : "");
        m.put("modeloVehiculo", c.getVehiculo() != null ? c.getVehiculo().getModelo() : "");
        return m;
    }
}
