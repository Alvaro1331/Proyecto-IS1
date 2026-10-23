package com.taller.config;

import com.taller.models.*;
import com.taller.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Carga datos de prueba en la base de datos H2 en memoria al arrancar el servidor.
 */
@Component
public class DatosPrueba implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ReparacionRepository reparacionRepository;
    @Autowired private ReparacionMaterialRepository reparacionMaterialRepository;

    @Override
    public void run(String... args) {

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
        // Reparación 1: Pendiente (sin mecánico asignado)
        // Precio acordado cliente: 250 € | Pensado por Laura (Recepción): 120 € | Presupuesto mecánico (a decidir por Admin): 120 €
        Reparacion rep1 = new Reparacion(v1, recep1, "El motor hace un ruido extraño al acelerar.", 250.00, 120.00, 120.00);
        reparacionRepository.save(rep1);

        // Reparación 2: En proceso (Pedro Ruiz)
        // Precio acordado cliente: 180 € | Pensado por Laura: 90 € | Presupuesto fijado por Admin para mecánico: 80 € (1x Pastillas = 35 € usadas -> quedan 45 €)
        Reparacion rep2 = new Reparacion(v3, recep1, "Las pastillas de freno están desgastadas, frena mal.", 180.00, 90.00, 80.00);
        rep2.setMecanico(mec1);
        rep2.setEstado(EstadoReparacion.EN_PROCESO);
        reparacionRepository.save(rep2);
        reparacionMaterialRepository.save(new ReparacionMaterial(rep2, mPastillas, 1));

        // Reparación 3: Finalizada por el mecánico Ana López (pendiente de confirmación por el Admin antes de que Laura pueda entregar y cobrar)
        // Precio acordado cliente: 150 € | Pensado por Laura: 75 € | Presupuesto fijado por Admin para mecánico: 60 € (Filtro 8.50 € + Aceite 28.00 € = 36.50 € usados)
        Reparacion rep3 = new Reparacion(v4, recep1, "Cambio de aceite y filtro programado (revisión de los 30.000 km).", 150.00, 75.00, 60.00);
        rep3.setMecanico(mec2);
        rep3.setEstado(EstadoReparacion.FINALIZADA);
        reparacionRepository.save(rep3);
        reparacionMaterialRepository.save(new ReparacionMaterial(rep3, mFiltro, 1));
        reparacionMaterialRepository.save(new ReparacionMaterial(rep3, mAceite, 1));

        // Vincular reparaciones a las cuentas de los clientes que tienen cuenta
        cliente1.getReparacionesVinculadas().add(rep1);
        clienteRepository.save(cliente1);

        cliente3.getReparacionesVinculadas().add(rep3);
        clienteRepository.save(cliente3);

        System.out.println("=========================================");
        System.out.println("  DATOS DE PRUEBA CARGADOS CORRECTAMENTE");
        System.out.println("  Usuarios: 4 | Clientes: 3 | Vehículos: 4");
        System.out.println("  Materiales: 7 | Reparaciones: 3");
        System.out.println("  Localizador Rep 1 (Toyota):  " + rep1.getLocalizador());
        System.out.println("  Localizador Rep 2 (Seat):    " + rep2.getLocalizador());
        System.out.println("  Localizador Rep 3 (VW Golf): " + rep3.getLocalizador());
        System.out.println("=========================================");
    }
}
