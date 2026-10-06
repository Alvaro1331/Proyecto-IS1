package com.taller.config;

import com.taller.models.*;
import com.taller.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Este archivo carga datos de prueba en la base de datos
 * cada vez que se arranca el servidor.
 *
 * SOLO FUNCIONA CON H2 (base de datos en memoria).
 * Cuando cambiéis a MySQL, comentad o borrad esta clase
 * para que no se dupliquen los datos cada vez que arranquéis.
 *
 * Los datos son inventados para poder probar los endpoints
 * sin tener que registrar todo a mano cada vez.
 */
@Component
public class DatosPrueba implements CommandLineRunner {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private VehiculoRepository vehiculoRepository;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private ReparacionRepository reparacionRepository;

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
        Cliente cliente1 = new Cliente("Juan Pérez", "611111111");
        Cliente cliente2 = new Cliente("María Sánchez", "622222222");
        Cliente cliente3 = new Cliente("Carlos Fernández", "633333333");

        clienteRepository.save(cliente1);
        clienteRepository.save(cliente2);
        clienteRepository.save(cliente3);

        // ======== VEHÍCULOS ========
        Vehiculo v1 = new Vehiculo("1234ABC", "Toyota", "Corolla", 2019, cliente1);
        Vehiculo v2 = new Vehiculo("5678DEF", "Ford", "Focus", 2021, cliente1);  // Juan tiene 2 coches
        Vehiculo v3 = new Vehiculo("9012GHI", "Seat", "Ibiza", 2018, cliente2);
        Vehiculo v4 = new Vehiculo("3456JKL", "Volkswagen", "Golf", 2022, cliente3);

        vehiculoRepository.save(v1);
        vehiculoRepository.save(v2);
        vehiculoRepository.save(v3);
        vehiculoRepository.save(v4);

        // ======== MATERIALES (inventario) ========
        materialRepository.save(new Material("Filtro de aceite", "Compatible con motores gasolina 1.4-2.0", 30, 8.50));
        materialRepository.save(new Material("Pastillas de freno", "Juego delantero universal", 15, 35.00));
        materialRepository.save(new Material("Aceite motor 5W30", "Bidón de 5 litros, sintético", 20, 28.00));
        materialRepository.save(new Material("Batería 60Ah", "12V, arranque en frío 540A", 5, 85.00));
        materialRepository.save(new Material("Líquido refrigerante", "Bidón de 5 litros, -30ºC", 12, 15.00));
        materialRepository.save(new Material("Correa de distribución", "Kit completo con tensor", 3, 120.00));
        materialRepository.save(new Material("Bujías", "Set de 4 unidades, iridio", 25, 22.00));

        // ======== REPARACIONES ========
        // Reparación 1: Pendiente (sin mecánico asignado)
        Reparacion rep1 = new Reparacion(v1, recep1, "El motor hace un ruido extraño al acelerar.");
        reparacionRepository.save(rep1);

        // Reparación 2: En proceso (con mecánico asignado)
        Reparacion rep2 = new Reparacion(v3, recep1, "Las pastillas de freno están desgastadas, frena mal.");
        rep2.setMecanico(mec1);
        rep2.setEstado(EstadoReparacion.EN_PROCESO);
        reparacionRepository.save(rep2);

        // Reparación 3: Finalizada (esperando recogida)
        Reparacion rep3 = new Reparacion(v4, recep1, "Cambio de aceite y filtro programado (revisión de los 30.000 km).");
        rep3.setMecanico(mec2);
        rep3.setEstado(EstadoReparacion.FINALIZADA);
        reparacionRepository.save(rep3);

        System.out.println("=========================================");
        System.out.println("  DATOS DE PRUEBA CARGADOS CORRECTAMENTE");
        System.out.println("  4 usuarios, 3 clientes, 4 vehículos,");
        System.out.println("  7 materiales, 3 reparaciones");
        System.out.println("=========================================");
    }
}
