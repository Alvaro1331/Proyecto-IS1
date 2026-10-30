package com.taller.config;

import com.taller.models.*;
import com.taller.repositories.*;
import com.taller.services.CitaPreviaService;
import com.taller.services.ConfiguracionTallerService;
import com.taller.services.ReparacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Carga datos de prueba en la base de datos al arrancar el servidor.
 * Garantiza también que exista la Configuración del Taller por defecto (Margen: 15%, Mano de obra: 35.00 €/h),
 * datos fiscales en clientes, un presupuesto desglosado de muestra y citas previas en el calendario.
 */
@Component
public class DatosPrueba implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ReparacionRepository reparacionRepository;
    @Autowired private ReparacionMaterialRepository reparacionMaterialRepository;
    @Autowired private IncidenciaReparacionRepository incidenciaRepository;
    @Autowired private FacturaRepository facturaRepository;
    @Autowired private PresupuestoReparacionRepository presupuestoRepository;
    @Autowired private CitaPreviaRepository citaRepository;
    @Autowired private ConfiguracionTallerService configuracionService;
    @Autowired private ReparacionService reparacionService;
    @Autowired private CitaPreviaService citaService;

    @Override
    public void run(String... args) {
        // 1. Asegurar que exista siempre la Configuración del Taller por defecto (15% margen, 35 €/h, 2000 € fondo)
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();

        // 2. Si la base de datos MySQL ya tiene usuarios, normalizar datos existentes
        if (usuarioRepository.count() > 0) {
            normalizarClientesFiscales();
            List<Reparacion> existentes = reparacionRepository.findAll();
            for (Reparacion r : existentes) {
                boolean modificado = false;
                if (r.getDiasEstimados() == null || r.getDiasEstimados() <= 0) {
                    r.setDiasEstimados(3);
                    modificado = true;
                }
                if (r.getFechaEstimadaFin() == null && r.getFechaEntrada() != null) {
                    r.setFechaEstimadaFin(r.getFechaEntrada().plusDays(r.getDiasEstimados()));
                    modificado = true;
                }
                if (r.getHorasTrabajoMecanico() == null || (r.getHorasTrabajoMecanico() == 0.0 && r.getEstado() != EstadoReparacion.PENDIENTE)) {
                    r.setHorasTrabajoMecanico(r.getEstado() == EstadoReparacion.FINALIZADA
                            || r.getEstado() == EstadoReparacion.CONFIRMADA
                            || r.getEstado() == EstadoReparacion.ENTREGADA ? 2.0 : 1.5);
                    modificado = true;
                }
                if (modificado) {
                    reparacionRepository.save(r);
                }
                if ((r.getEstado() == EstadoReparacion.FINALIZADA
                        || r.getEstado() == EstadoReparacion.CONFIRMADA
                        || r.getEstado() == EstadoReparacion.ENTREGADA)
                        && facturaRepository.count() == 0) {
                    reparacionService.generarFactura(r.getId(), r.getHorasTrabajoMecanico());
                }
            }
            inicializarPresupuestoYCitasMuestra(existentes);
            System.out.println("=========================================");
            System.out.println("  BASE DE DATOS MYSQL CONECTADA");
            System.out.println("  Configuración Taller: Margen " + config.getMargenBeneficioPorcentaje()
                    + "% | Mano de obra " + config.getPrecioHoraManoObra() + " €/h");
            System.out.println("=========================================");
            return;
        }

        // ======== USUARIOS (personal del taller) ========
        Usuario admin = new Usuario("Antonio García", "admin@taller.com", "admin123", "600000001", Rol.ADMIN);
        Usuario recep1 = new Usuario("Laura Martínez", "laura@taller.com", "laura123", "600000002", Rol.RECEPCIONISTA);
        Usuario mec1 = new Usuario("Pedro Ruiz", "pedro@taller.com", "pedro123", "600000003", Rol.MECANICO);
        Usuario mec2 = new Usuario("Ana López", "ana@taller.com", "ana123", "600000004", Rol.MECANICO);

        usuarioRepository.save(admin);
        usuarioRepository.save(recep1);
        usuarioRepository.save(mec1);
        usuarioRepository.save(mec2);

        // ======== CLIENTES ========
        Cliente cliente1 = new Cliente("Juan Pérez", "611111111", "juan@email.com", "juan123", "12345678Z", "Calle Alcalá 45, 28014 Madrid");
        Cliente cliente2 = new Cliente("María Sánchez", "622222222", null, null, "45678912M", "Avenida de América 18, 28028 Madrid");
        Cliente cliente3 = new Cliente("Carlos Fernández", "633333333", "carlos@email.com", "carlos123", "87654321X", "Paseo de la Castellana 120, 28046 Madrid");

        clienteRepository.save(cliente1);
        clienteRepository.save(cliente2);
        clienteRepository.save(cliente3);

        // ======== VEHÍCULOS ========
        Vehiculo v1 = new Vehiculo("1234ABC", "Toyota", "Corolla", 2019, cliente1);
        Vehiculo v2 = new Vehiculo("5678DEF", "Ford", "Focus", 2021, cliente1);
        Vehiculo v3 = new Vehiculo("9012GHI", "Seat", "Ibiza", 2018, cliente2);
        Vehiculo v4 = new Vehiculo("3456JKL", "Volkswagen", "Golf", 2022, cliente3);

        vehiculoRepository.save(v1);
        vehiculoRepository.save(v2);
        vehiculoRepository.save(v3);
        vehiculoRepository.save(v4);

        // ======== MATERIALES (inventario) ========
        Material mFiltro = materialRepository.save(new Material("Filtro de aceite", "Compatible con motores gasolina 1.4-2.0", 30, 8.50));
        Material mPastillas = materialRepository.save(new Material("Pastillas de freno", "Juego delantero universal", 15, 35.00));
        Material mAceite = materialRepository.save(new Material("Aceite motor 5W30", "Bidón de 5 litros, sintético", 20, 28.00));
        materialRepository.save(new Material("Batería 60Ah", "12V, arranque en frío 540A", 5, 85.00));
        materialRepository.save(new Material("Líquido refrigerante", "Bidón de 5 litros, -30ºC", 12, 15.00));
        materialRepository.save(new Material("Correa de distribución", "Kit completo con tensor", 3, 120.00));
        materialRepository.save(new Material("Bujías", "Set de 4 unidades, iridio", 25, 22.00));

        // ======== REPARACIONES ========
        // Reparación 1: Pendiente (con presupuesto desglosado pendiente de aceptación por Juan Pérez)
        Reparacion rep1 = new Reparacion(v1, recep1, "El motor hace un ruido extraño al acelerar.", 220.00, 4);
        rep1.setMecanico(mec1);
        reparacionRepository.save(rep1);

        // Reparación 2: En proceso (Pedro Ruiz)
        Reparacion rep2 = new Reparacion(v3, recep1, "Las pastillas de freno están desgastadas, frena mal.", 140.00, 2);
        rep2.setMecanico(mec1);
        rep2.setEstado(EstadoReparacion.EN_PROCESO);
        rep2.setHorasTrabajoMecanico(1.5);
        reparacionRepository.save(rep2);
        reparacionMaterialRepository.save(new ReparacionMaterial(rep2, mPastillas, 1));
        incidenciaRepository.save(new IncidenciaReparacion(
                rep2,
                "Se desmontaron las pinzas delanteras y se verificó desgaste severo en pastillas. Discos en buen estado.",
                0.0,
                null
        ));

        // Reparación 3: Finalizada por Ana López
        Reparacion rep3 = new Reparacion(v4, recep1, "Cambio de aceite y filtro programado (revisión de los 30.000 km).", 120.00, 1);
        rep3.setMecanico(mec2);
        rep3.setEstado(EstadoReparacion.FINALIZADA);
        rep3.setHorasTrabajoMecanico(1.5);
        reparacionRepository.save(rep3);
        reparacionMaterialRepository.save(new ReparacionMaterial(rep3, mFiltro, 1));
        reparacionMaterialRepository.save(new ReparacionMaterial(rep3, mAceite, 1));
        incidenciaRepository.save(new IncidenciaReparacion(
                rep3,
                "Revisión de 30.000 km completada: sustitución de aceite sintético 5W30 y filtro de aceite. Niveles verificados.",
                0.0,
                null
        ));

        // Generar factura inicial de muestra para la reparación 3
        reparacionService.generarFactura(rep3.getId(), 1.5);

        // Vincular reparaciones a las cuentas de los clientes que tienen cuenta
        cliente1.getReparacionesVinculadas().add(rep1);
        clienteRepository.save(cliente1);

        cliente3.getReparacionesVinculadas().add(rep3);
        clienteRepository.save(cliente3);

        inicializarPresupuestoYCitasMuestra(List.of(rep1, rep2, rep3));

        System.out.println("=========================================");
        System.out.println("  DATOS DE PRUEBA CARGADOS CORRECTAMENTE");
        System.out.println("  Configuración: Margen " + config.getMargenBeneficioPorcentaje()
                + "% | Hora M.O. " + config.getPrecioHoraManoObra() + " €/h");
        System.out.println("  Localizador Rep 1 (Toyota):  " + rep1.getLocalizador());
        System.out.println("  Localizador Rep 2 (Seat):    " + rep2.getLocalizador());
        System.out.println("  Localizador Rep 3 (VW Golf): " + rep3.getLocalizador());
        System.out.println("=========================================");
    }

    private void normalizarClientesFiscales() {
        List<Cliente> clientes = clienteRepository.findAll();
        int idx = 1;
        for (Cliente c : clientes) {
            boolean mod = false;
            if (c.getDniCif() == null || c.getDniCif().isBlank()) {
                c.setDniCif(String.format("%08d%c", 10000000 + idx * 1234567, "TRWAGMYFPDXBNJZSQVHLCKE".charAt(idx % 23)));
                mod = true;
            }
            if (c.getDireccionFiscal() == null || c.getDireccionFiscal().isBlank()) {
                c.setDireccionFiscal("Calle Gran Vía " + (10 + idx * 5) + ", 28013 Madrid");
                mod = true;
            }
            if (mod) {
                clienteRepository.save(c);
            }
            idx++;
        }
    }

    private void inicializarPresupuestoYCitasMuestra(List<Reparacion> reparaciones) {
        try {
            if (presupuestoRepository.count() == 0 && !reparaciones.isEmpty()) {
                Reparacion target = reparaciones.stream()
                        .filter(r -> r.getEstado() == EstadoReparacion.PENDIENTE)
                        .findFirst()
                        .orElse(reparaciones.get(0));
                reparacionService.guardarPresupuestoDesglosado(
                        target.getId(),
                        "Diagnóstico electrónico y mecánico: holgura en tensor de distribución y sustitución preventiva de bujías.",
                        15,
                        List.of(
                                Map.of("tipo", "PIEZA", "concepto", "Kit Correa de distribución con tensor", "cantidad", 1, "precioUnitario", 120.00),
                                Map.of("tipo", "PIEZA", "concepto", "Juego de 4 Bujías de iridio", "cantidad", 1, "precioUnitario", 22.00),
                                Map.of("tipo", "MANO_OBRA", "concepto", "Desmontaje, sustitución de distribución y ajuste de encendido", "cantidad", 2.5, "precioUnitario", 35.00)
                        )
                );
            }

            // Garantizar coherencia realista:
            // 1) Si una reparación tiene presupuesto en PENDIENTE_ACEPTACION o RECHAZADO y aún no se habían usado piezas, debe estar en PENDIENTE (no EN_PROCESO).
            // 2) Si una reparación de muestra ya está EN_PROCESO, FINALIZADA, CONFIRMADA o ENTREGADA y no tenía registro de presupuesto, crear su presupuesto inicial como ACEPTADO.
            for (Reparacion r : reparacionRepository.findAll()) {
                var optPres = presupuestoRepository.findByReparacionId(r.getId());
                if (optPres.isPresent()) {
                    PresupuestoReparacion p = optPres.get();
                    if (!"ACEPTADO".equalsIgnoreCase(p.getEstado())
                            && r.getEstado() == EstadoReparacion.EN_PROCESO
                            && reparacionMaterialRepository.findByReparacionId(r.getId()).isEmpty()) {
                        r.setEstado(EstadoReparacion.PENDIENTE);
                        reparacionRepository.save(r);
                    }
                } else if (r.getEstado() != EstadoReparacion.PENDIENTE) {
                    reparacionService.crearPresupuestoAceptadoInicial(
                            r.getId(),
                            "Diagnóstico inicial confirmado con el cliente: " + r.getDescripcionAveria(),
                            List.of(
                                    Map.of("tipo", "PIEZA", "concepto", "Kit de repuestos según intervención (" + r.getVehiculo().getMatricula() + ")", "cantidad", 1, "precioUnitario", 35.00),
                                    Map.of("tipo", "MANO_OBRA", "concepto", "Mano de obra intervención mecánica", "cantidad", r.getHorasTrabajoMecanico() != null && r.getHorasTrabajoMecanico() > 0 ? r.getHorasTrabajoMecanico() : 1.5, "precioUnitario", 35.00)
                            )
                    );
                }
            }
        } catch (Exception ignored) {}

        try {
            if (citaRepository.count() == 0) {
                List<Vehiculo> vehiculos = vehiculoRepository.findAll();
                if (!vehiculos.isEmpty()) {
                    LocalDate dia1 = siguienteDiaLaborable(LocalDate.now().plusDays(1));
                    LocalDate dia2 = siguienteDiaLaborable(dia1.plusDays(1));

                    Vehiculo v1 = vehiculos.get(0);
                    citaRepository.save(new CitaPrevia(
                            v1.getCliente(), v1, dia1, "09:00",
                            "Revisión periódica", "Revisión anual y niveles", "CONFIRMADA"
                    ));
                    if (vehiculos.size() > 1) {
                        Vehiculo v2 = vehiculos.get(1);
                        citaRepository.save(new CitaPrevia(
                                v2.getCliente(), v2, dia1, "11:00",
                                "Cambio de neumáticos", "Sustitución eje delantero y equilibrado", "CONFIRMADA"
                        ));
                    }
                    if (vehiculos.size() > 2) {
                        Vehiculo v3 = vehiculos.get(2);
                        citaRepository.save(new CitaPrevia(
                                v3.getCliente(), v3, dia2, "10:00",
                                "Frenos y suspensión", "Vibración al frenar en autopista", "PENDIENTE"
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private LocalDate siguienteDiaLaborable(LocalDate fecha) {
        LocalDate cursor = fecha;
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY
                || !citaService.esDiaLaborable(cursor)) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
