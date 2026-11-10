package com.taller;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.MaterialRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.UsuarioRepository;
import com.taller.repositories.VehiculoRepository;
import com.taller.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================================
 * PRUEBAS UNITARIAS DEL SISTEMA MOTORFLOW
 * ============================================================================
 * Este archivo contiene pruebas unitarias implementadas con JUnit 5 para
 * verificar el comportamiento de los métodos y clases clave del sistema.
 */
public class PruebasUnitariasTest {

    /**
     * PRUEBA 1:
     * Verifica que el método recalcularImportes() de la clase Factura calcula
     * correctamente el desglose económico (mano de obra, margen de taller,
     * base imponible, IVA y total final).
     */
    @Test
    @DisplayName("Prueba 1: Cálculo del desglose e importe total de una Factura")
    void test1_CalcularTotalFacturaConMaterialesYManoDeObra() {
        // 1. Inicializar
        Factura factura = new Factura();
        double costeMateriales = 100.00;
        double horasManoObra = 2.0;
        double precioHora = 35.00;       // 2h * 35€ = 70.00 €
        double porcentajeMargen = 15.0;  // 15% de (100 + 70) = 25.50 €
        double porcentajeIva = 21.0;     // 21% de 195.50 = 41.06 €

        // 2. Ejecutar método
        factura.recalcularImportes(costeMateriales, horasManoObra, precioHora, porcentajeMargen, porcentajeIva);

        // 3. Verificar resultados
        assertEquals(70.00, factura.getSubtotalManoObra(), 0.001, "Mano de obra debe ser 70.00 €");
        assertEquals(170.00, factura.getSubtotalBase(), 0.001, "Subtotal base debe ser 170.00 €");
        assertEquals(25.50, factura.getImporteMargen(), 0.001, "Margen (15%) debe ser 25.50 €");
        assertEquals(195.50, factura.getBaseImponible(), 0.001, "Base imponible debe ser 195.50 €");
        assertEquals(41.06, factura.getImporteIva(), 0.001, "IVA (21%) debe ser 41.06 €");
        assertEquals(236.56, factura.getTotalFactura(), 0.001, "Total factura debe ser 236.56 €");
    }

    /**
     * PRUEBA 2:
     * Verifica el cálculo de una factura cuando el coste de materiales es 0.0 €
     * (solo mano de obra), asegurando que los cálculos no fallen y reflejen el importe correcto.
     */
    @Test
    @DisplayName("Prueba 2: Cálculo de Factura únicamente con Mano de Obra (sin materiales)")
    void test2_CalcularFacturaSinMateriales() {
        Factura factura = new Factura();

        // 1 hora de mano de obra a 35€/h, sin piezas gastadas
        factura.recalcularImportes(0.0, 1.0, 35.0, 15.0, 21.0);

        assertEquals(0.0, factura.getCosteMateriales(), 0.001);
        assertEquals(35.0, factura.getSubtotalBase(), 0.001);
        assertEquals(5.25, factura.getImporteMargen(), 0.001);
        assertEquals(40.25, factura.getBaseImponible(), 0.001);
        assertEquals(8.45, factura.getImporteIva(), 0.001);
        assertEquals(48.70, factura.getTotalFactura(), 0.001);
    }

    /**
     * PRUEBA 3:
     * Verifica que el método recalcularTotales() de la clase PresupuestoReparacion
     * suma correctamente las líneas agregadas (piezas y mano de obra) y calcula el total.
     */
    @Test
    @DisplayName("Prueba 3: Cálculo y suma de subtotales en un Presupuesto de Reparación")
    void test3_RecalcularTotalesPresupuesto() {
        PresupuestoReparacion presupuesto = new PresupuestoReparacion(
                "PRE-2026-001", null, "Cambio de pastillas y líquido",
                LocalDate.now().plusDays(15), 15.0, 21.0
        );

        // Añadimos línea de pieza: 2 unidades a 25.00 € = 50.00 €
        LineaPresupuesto lPieza = new LineaPresupuesto(presupuesto, "PIEZA", "Pastillas de freno", 2.0, 25.0, 1L);
        // Añadimos línea de mano de obra: 1.5 horas a 40.00 € = 60.00 €
        LineaPresupuesto lManoObra = new LineaPresupuesto(presupuesto, "MANO_OBRA", "Mano de obra", 1.5, 40.0, null);

        presupuesto.getLineas().add(lPieza);
        presupuesto.getLineas().add(lManoObra);

        presupuesto.recalcularTotales();

        assertEquals(50.00, presupuesto.getSubtotalPiezas(), 0.001);
        assertEquals(60.00, presupuesto.getSubtotalManoObra(), 0.001);
        assertEquals(110.00, presupuesto.getSubtotalBase(), 0.001);
        assertEquals(153.07, presupuesto.getTotalPresupuesto(), 0.001);
    }

    /**
     * PRUEBA 4:
     * Verifica el método estaCaducado() de la clase PresupuestoReparacion
     * para asegurar que detecta cuándo un presupuesto ha superado su fecha de validez.
     */
    @Test
    @DisplayName("Prueba 4: Verificación del estado de caducidad de un Presupuesto")
    void test4_VerificarCaducidadPresupuesto() {
        PresupuestoReparacion presupuesto = new PresupuestoReparacion();
        presupuesto.setEstado("PENDIENTE_ACEPTACION");

        // Caso 1: Fecha de validez dentro de 5 días -> No debe estar caducado
        presupuesto.setFechaValidez(LocalDate.now().plusDays(5));
        assertFalse(presupuesto.estaCaducado(), "Un presupuesto con fecha futura no debe estar caducado");

        // Caso 2: Fecha de validez de ayer -> Sí debe estar caducado
        presupuesto.setFechaValidez(LocalDate.now().minusDays(1));
        assertTrue(presupuesto.estaCaducado(), "Un presupuesto cuya fecha ya pasó debe estar caducado");
    }

    /**
     * PRUEBA 5:
     * Verifica que el método estático generarLocalizador() de la clase Reparacion
     * genera un código no nulo y con una longitud exacta de 8 caracteres.
     */
    @Test
    @DisplayName("Prueba 5: Generación del código localizador de una Reparación")
    void test5_GenerarLocalizadorReparacion() {
        String localizador = Reparacion.generarLocalizador();

        assertNotNull(localizador, "El localizador no debe ser nulo");
        assertEquals(8, localizador.length(), "El localizador debe tener exactamente 8 caracteres");
    }

    /**
     * PRUEBA 6:
     * Verifica la correcta inicialización de los atributos de un Material del inventario
     * (nombre, descripción, stock disponible y precio unitario).
     */
    @Test
    @DisplayName("Prueba 6: Inicialización y consistencia de datos de un Material")
    void test6_InicializacionMaterialInventario() {
        Material material = new Material("Filtro de aire", "Filtro para motor 1.6", 15, 18.50);

        assertEquals("Filtro de aire", material.getNombre());
        assertEquals("Filtro para motor 1.6", material.getDescripcion());
        assertEquals(15, material.getStockDisponible());
        assertEquals(18.50, material.getPrecioUnitario(), 0.001);
    }

    /**
     * PRUEBA 7:
     * Verifica que el inicio de sesión de un empleado (UsuarioService.login)
     * autentica con éxito cuando el email y la contraseña coinciden.
     */
    @Test
    @DisplayName("Prueba 7: Inicio de sesión exitoso de un empleado con credenciales válidas")
    void test7_LoginExitosoEmpleado() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);

        Usuario empleado = new Usuario("Pedro Ruiz", "pedro@taller.com", "pedro123", "600000003", Rol.MECANICO);
        when(usuarioRepository.findByEmail("pedro@taller.com")).thenReturn(Optional.of(empleado));

        UsuarioResponseDTO response = usuarioService.login("pedro@taller.com", "pedro123");

        assertNotNull(response, "La respuesta de login no debe ser nula");
        assertEquals("Pedro Ruiz", response.getNombre());
        assertEquals("MECANICO", response.getRol());
        assertTrue(response.getMensaje().contains("Login correcto"), "El mensaje debe confirmar el login correcto");
    }

    /**
     * PRUEBA 8:
     * Verifica que el inicio de sesión falla y lanza IllegalArgumentException
     * cuando la contraseña introducida por el usuario es incorrecta.
     */
    @Test
    @DisplayName("Prueba 8: Rechazo de inicio de sesión ante contraseña incorrecta")
    void test8_LoginContrasenaIncorrecta() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);

        Usuario empleado = new Usuario("Antonio", "admin@taller.com", "admin123", "600000001", Rol.ADMIN);
        when(usuarioRepository.findByEmail("admin@taller.com")).thenReturn(Optional.of(empleado));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.login("admin@taller.com", "passwordErronea");
        });

        assertEquals("Contraseña incorrecta.", ex.getMessage());
    }

    /**
     * PRUEBA 9:
     * Verifica que el método tieneCuenta() de la clase Cliente distingue correctamente
     * entre un cliente con credenciales de login (email y password) y uno sin cuenta registrada.
     */
    @Test
    @DisplayName("Prueba 9: Validación de clientes con cuenta de acceso vs clientes sin cuenta")
    void test9_ClienteTieneCuentaOno() {
        // Caso 1: Cliente sin cuenta (solo registrado al dejar el vehículo en recepción)
        Cliente clienteSinCuenta = new Cliente("Marta Gómez", "655555555");
        assertFalse(clienteSinCuenta.tieneCuenta(), "Un cliente sin email ni contraseña no debe tener cuenta de acceso");

        // Caso 2: Cliente con cuenta completa para iniciar sesión
        Cliente clienteConCuenta = new Cliente("Juan Pérez", "611111111", "juan@email.com", "juan123");
        assertTrue(clienteConCuenta.tieneCuenta(), "Un cliente con email y contraseña debe tener cuenta de acceso válida");
    }

    /**
     * PRUEBA 10:
     * Verifica que el método estático generarCodigoCita() de la clase CitaPrevia
     * genera un código no nulo con el prefijo "CITA-" y una longitud total de 9 caracteres.
     */
    @Test
    @DisplayName("Prueba 10: Generación del código identificativo de Cita Previa")
    void test10_GenerarCodigoCitaPrevia() {
        String codigo = CitaPrevia.generarCodigoCita();

        assertNotNull(codigo, "El código de cita no debe ser nulo");
        assertTrue(codigo.startsWith("CITA-"), "El código debe comenzar con el prefijo 'CITA-'");
        assertEquals(9, codigo.length(), "El código de cita debe tener exactamente 9 caracteres (CITA- + 4 alfanuméricos)");
    }

    /**
     * PRUEBA 11:
     * Verifica que el método actualizarStock de MaterialService valida que el stock no sea
     * negativo, lanzando IllegalArgumentException si se introduce un valor menor que cero.
     */
    @Test
    @DisplayName("Prueba 11: Rechazo de actualización con stock negativo en MaterialService")
    void test11_ActualizarStockNegativoLanzaExcepcion() {
        MaterialRepository materialRepo = mock(MaterialRepository.class);
        MaterialService materialService = new MaterialService();
        ReflectionTestUtils.setField(materialService, "materialRepository", materialRepo);

        Material material = new Material("Batería 12V 70Ah", "Batería de arranque", 5, 85.0);
        when(materialRepo.findById(1L)).thenReturn(Optional.of(material));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            materialService.actualizarStock(1L, -3);
        });

        assertEquals("El stock no puede ser negativo.", ex.getMessage());
    }

    /**
     * PRUEBA 12:
     * Verifica que el registro de un nuevo cliente en ClienteService falla
     * si el correo electrónico ya se encuentra registrado en el sistema.
     */
    @Test
    @DisplayName("Prueba 12: Rechazo de registro de cliente con email duplicado")
    void test12_RegistroClienteEmailDuplicado() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", clienteRepo);

        Cliente clienteExistente = new Cliente("Carlos López", "622222222", "carlos@email.com", "pass123");
        when(clienteRepo.findByEmail("carlos@email.com")).thenReturn(Optional.of(clienteExistente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clienteService.registrarCliente("Carlos López", "622222222", "carlos@email.com", "nuevaPass");
        });

        assertTrue(ex.getMessage().contains("Ya existe una cuenta con el correo"), "Debe indicar que el email ya está en uso");
    }

    /**
     * PRUEBA 13:
     * Verifica que al crear una Reparación con días estimados definidos,
     * el estado inicial es PENDIENTE y la fecha estimada de fin se calcula
     * automáticamente sumando dichos días a la fecha de entrada actual.
     */
    @Test
    @DisplayName("Prueba 13: Inicialización y cálculo de fecha estimada en Reparación")
    void test13_InicializacionReparacionYFechaEstimadaFin() {
        Vehiculo vehiculo = new Vehiculo("1234-XYZ", "Toyota", "Corolla", 2020, null);
        Usuario recepcionista = new Usuario("Laura", "laura@taller.com", "laura123", "600000002", Rol.RECEPCIONISTA);

        Reparacion reparacion = new Reparacion(vehiculo, recepcionista, "Revisión 50.000 km", 250.0, 5);

        assertEquals(EstadoReparacion.PENDIENTE, reparacion.getEstado(), "El estado inicial debe ser PENDIENTE");
        assertEquals(250.0, reparacion.getPresupuesto(), 0.001, "El presupuesto estimado inicial debe ser 250.0 €");
        assertEquals(5, reparacion.getDiasEstimados(), "Los días estimados deben ser 5");
        assertEquals(LocalDate.now().plusDays(5), reparacion.getFechaEstimadaFin(), "La fecha estimada de fin debe ser hoy + 5 días");
    }

    /**
     * PRUEBA 14:
     * Verifica la correcta vinculación e integridad de datos entre un Vehículo
     * y su Cliente propietario registrado en el sistema.
     */
    @Test
    @DisplayName("Prueba 14: Vinculación e integridad de Vehículo con su Cliente propietario")
    void test14_VinculacionVehiculoCliente() {
        Cliente cliente = new Cliente("Ana Martínez", "644444444", "ana@email.com", "ana123");
        Vehiculo vehiculo = new Vehiculo("5678-BBB", "Ford", "Focus", 2018, cliente);

        assertNotNull(vehiculo.getCliente(), "El vehículo debe tener un cliente asignado");
        assertEquals("Ana Martínez", vehiculo.getCliente().getNombre());
        assertEquals("5678-BBB", vehiculo.getMatricula());
        assertEquals("Ford", vehiculo.getMarca());
        assertEquals("Focus", vehiculo.getModelo());
        assertEquals(2018, vehiculo.getAno());
    }

    /**
     * PRUEBA 15:
     * Verifica los valores de configuración por defecto del Singleton de Configuración del Taller
     * (margen de beneficio 15%, tarifa de mano de obra 35€/h y fondo inicial de tesorería de 2000€).
     */
    @Test
    @DisplayName("Prueba 15: Verificación de valores por defecto de la Configuración del Taller")
    void test15_ValoresPorDefectoConfiguracionTaller() {
        ConfiguracionTaller config = new ConfiguracionTaller();

        assertEquals(15.0, config.getMargenBeneficioPorcentaje(), 0.001, "El margen por defecto debe ser 15%");
        assertEquals(35.0, config.getPrecioHoraManoObra(), 0.001, "El precio de mano de obra debe ser 35.0 €/h");
        assertEquals(2000.0, config.getFondoInicial(), 0.001, "El fondo inicial de tesorería debe ser 2000.0 €");
    }

    /**
     * PRUEBA 16 (SEGURIDAD - AUTENTICACIÓN):
     * Verifica que el intento de inicio de sesión con un usuario que no existe en el sistema
     * es rechazado inmediatamente con una excepción de seguridad (IllegalArgumentException),
     * impidiendo el acceso a cuentas inexistentes.
     */
    @Test
    @DisplayName("Prueba 16: Seguridad - Rechazo de autenticación para usuario inexistente")
    void test16_Seguridad_LoginUsuarioInexistenteRechazado() {
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepo);

        when(usuarioRepo.findByEmail("atacante@externo.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            usuarioService.login("atacante@externo.com", "clave1234");
        });

        assertTrue(ex.getMessage().contains("Usuario no encontrado"), "El sistema debe rechazar el login de emails inexistentes");
    }

    /**
     * PRUEBA 17 (SEGURIDAD - CONTROL DE ACCESO BASADO EN ROLES / RBAC):
     * Verifica que un usuario sin rol de RECEPCIONISTA ni ADMIN (por ejemplo, un MECANICO)
     * no puede crear ni abrir órdenes de reparación en el sistema, haciendo cumplir el principio
     * de mínimo privilegio.
     */
    @Test
    @DisplayName("Prueba 17: Seguridad (RBAC) - Bloqueo de creación de reparación a roles no autorizados")
    void test17_Seguridad_CrearReparacionDenegadaParaRolesSinPermiso() {
        VehiculoRepository vehiculoRepo = mock(VehiculoRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        ReparacionService reparacionService = new ReparacionService();
        ReflectionTestUtils.setField(reparacionService, "vehiculoRepository", vehiculoRepo);
        ReflectionTestUtils.setField(reparacionService, "usuarioRepository", usuarioRepo);

        Vehiculo vehiculo = new Vehiculo("9999-ZZZ", "Seat", "León", 2021, null);
        when(vehiculoRepo.findById(1L)).thenReturn(Optional.of(vehiculo));

        // Un mecánico intenta ejecutar la acción administrativa de recepción
        Usuario mecanico = new Usuario("Pedro Mecanico", "pedro@taller.com", "pass", "600000003", Rol.MECANICO);
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(mecanico));

        ReparacionRequestDTO dto = new ReparacionRequestDTO();
        dto.setVehiculoId(1L);
        dto.setRecepcionistaId(2L);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reparacionService.crearReparacion(dto);
        });

        assertTrue(ex.getMessage().contains("no tiene permisos de recepcionista"), "Debe impedir que roles ajenos a recepción creen reparaciones");
    }

    /**
     * PRUEBA 18 (SEGURIDAD - CONTROL DE ACCESO BASADO EN ROLES / RBAC):
     * Verifica que el sistema impide asignar tareas de taller a un usuario que no tiene
     * el rol de MECANICO o ADMIN (por ejemplo, asignar el coche a un recepcionista).
     */
    @Test
    @DisplayName("Prueba 18: Seguridad (RBAC) - Bloqueo de asignación técnica a usuarios sin rol mecánico")
    void test18_Seguridad_AsignacionMecanicoDenegadaARolesInadecuados() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        ReparacionService reparacionService = new ReparacionService();
        ReflectionTestUtils.setField(reparacionService, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(reparacionService, "usuarioRepository", usuarioRepo);

        Reparacion reparacion = new Reparacion();
        when(reparacionRepo.findById(5L)).thenReturn(Optional.of(reparacion));

        // Recepcionista intentando ser asignado como operario técnico
        Usuario recepcionista = new Usuario("Laura Recepcion", "laura@taller.com", "pass", "600000002", Rol.RECEPCIONISTA);
        when(usuarioRepo.findById(8L)).thenReturn(Optional.of(recepcionista));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reparacionService.asignarMecanico(5L, 8L);
        });

        assertTrue(ex.getMessage().contains("no tiene rol de mecánico"), "Debe impedir que empleados sin cualificación técnica asuman reparaciones");
    }

    /**
     * PRUEBA 19 (SEGURIDAD - PROTECCIÓN DE CUENTAS):
     * Verifica que un cliente registrado de forma presencial en el taller físico (que solo tiene
     * nombre y teléfono, pero carece de credenciales de acceso) no puede iniciar sesión por la web,
     * protegiendo el sistema contra accesos no autorizados a fichas sin cuenta.
     */
    @Test
    @DisplayName("Prueba 19: Seguridad - Bloqueo de inicio de sesión a clientes sin credenciales registradas")
    void test19_Seguridad_LoginDenegadoAClientesSinCuentaWeb() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", clienteRepo);

        // Cliente presencial que no se ha registrado en la plataforma
        Cliente clienteSinCuenta = new Cliente("Cliente Presencial", "688112233");
        when(clienteRepo.findByTelefono("688112233")).thenReturn(Optional.of(clienteSinCuenta));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            clienteService.loginCliente("688112233", "passwordIntento");
        });

        assertEquals("Este cliente no tiene cuenta. Regístrate primero.", ex.getMessage());
    }

    /**
     * PRUEBA 20 (SEGURIDAD - FLUJO ANTIFRAUDE EN RECUPERACIÓN DE CONTRASEÑA):
     * Verifica que el servicio de recuperación de credenciales rechaza intentos de verificación
     * de códigos si no existe una solicitud legítima previa en memoria para dicho correo,
     * impidiendo ataques de fuerza bruta o inyecciones de código.
     */
    @Test
    @DisplayName("Prueba 20: Seguridad - Rechazo de verificación de código sin solicitud previa activa")
    void test20_Seguridad_RechazoCodigoRecuperacionSinSolicitudPrevia() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        RecuperacionPasswordService recService = new RecuperacionPasswordService();
        ReflectionTestUtils.setField(recService, "clienteRepository", clienteRepo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            recService.verificarCodigo("victima@email.com", "123456");
        });

        assertEquals("No hay ninguna solicitud de recuperación activa para este correo.", ex.getMessage());
    }
}
