package com.taller.config;

import com.taller.models.*;
import com.taller.repositories.*;
import com.taller.services.ConfiguracionTallerService;
import com.taller.services.ReparacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Carga datos de prueba en la base de datos al arrancar el servidor.
 * Garantiza también que exista la Configuración del Taller por defecto (Margen: 15%, Mano de obra: 35.00 €/h)
 * y que las reparaciones existentes en MySQL tengan días estimados y horas inicializadas.
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
    @Autowired private ConfiguracionTallerService configuracionService;
    @Autowired private ReparacionService reparacionService;

    @Override
    public void run(String... args) {
        // 1. Asegurar que exista siempre la Configuración del Taller por defecto (15% margen, 35 €/h, 2000 € fondo)
        ConfiguracionTaller config = configuracionService.obtenerConfiguracion();

        // 2. Si la base de datos MySQL ya tiene usuarios, normalizar reparaciones existentes que no tengan los nuevos campos
        if (usuarioRepository.count() > 0) {
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
                // Si hay una reparación finalizada/confirmada/entregada de prueba y no hay ninguna factura aún, generamos una de muestra
                if ((r.getEstado() == EstadoReparacion.FINALIZADA
                        || r.getEstado() == EstadoReparacion.CONFIRMADA
                        || r.getEstado() == EstadoReparacion.ENTREGADA)
                        && facturaRepository.count() == 0) {
                    reparacionService.generarFactura(r.getId(), r.getHorasTrabajoMecanico());
                }
            }
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
        Cliente cliente1 = new Cliente("Juan Pérez", "611111111", "juan@email.com", "juan123");
        Cliente cliente2 = new Cliente("María Sánchez", "622222222");
        Cliente cliente3 = new Cliente("Carlos Fernández", "633333333", "carlos@email.com", "carlos123");

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
        // Reparación 1: Pendiente (sin mecánico asignado) - Estimación inicial: 220 €, 4 días estimados
        Reparacion rep1 = new Reparacion(v1, recep1, "El motor hace un ruido extraño al acelerar.", 220.00, 4);
        reparacionRepository.save(rep1);

        // Reparación 2: En proceso (Pedro Ruiz) - Estimación inicial: 140 €, 2 días estimados, 1.5h trabajadas
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

        // Reparación 3: Finalizada por Ana López - Estimación inicial: 120 €, 1 día estimado, 1.5h trabajadas
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

        System.out.println("=========================================");
        System.out.println("  DATOS DE PRUEBA CARGADOS CORRECTAMENTE");
        System.out.println("  Configuración: Margen " + config.getMargenBeneficioPorcentaje()
                + "% | Hora M.O. " + config.getPrecioHoraManoObra() + " €/h");
        System.out.println("  Localizador Rep 1 (Toyota):  " + rep1.getLocalizador());
        System.out.println("  Localizador Rep 2 (Seat):    " + rep2.getLocalizador());
        System.out.println("  Localizador Rep 3 (VW Golf): " + rep3.getLocalizador());
        System.out.println("=========================================");
    }
}
