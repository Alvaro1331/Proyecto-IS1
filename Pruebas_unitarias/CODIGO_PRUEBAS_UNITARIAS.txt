package com.taller;

import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.dtos.VehiculoRequestDTO;
import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.CitaPreviaRepository;
import com.taller.repositories.FacturaRepository;
import com.taller.repositories.IncidenciaReparacionRepository;
import com.taller.repositories.MaterialRepository;
import com.taller.repositories.NotificacionClienteRepository;
import com.taller.repositories.PresupuestoReparacionRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.ReparacionMaterialRepository;
import com.taller.repositories.UsuarioRepository;
import com.taller.repositories.VehiculoRepository;
import com.taller.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    /**
     * PRUEBA 21:
     * Verifica la creación de una NotificacionCliente: que el objeto se inicializa
     * con el tipo correcto, el título y mensaje asignados, y que nace en estado no leída.
     * También verifica que el método setLeida(true) la marca como leída correctamente.
     */
    @Test
    @DisplayName("Prueba 21: Creación y marcado de lectura de NotificacionCliente")
    void test21_CreacionYLecturaNotificacionCliente() {
        Cliente cliente = new Cliente("Juan Pérez", "611111111", "juan@email.com", "juan123");
        NotificacionCliente notificacion = new NotificacionCliente(
                cliente,
                null,
                TipoNotificacion.LISTO_PARA_RECOGER,
                "¡Tu coche está listo!",
                "La reparación ha finalizado y ya puedes pasar a recogerlo."
        );

        assertNotNull(notificacion, "La notificación no debe ser nula");
        assertEquals(TipoNotificacion.LISTO_PARA_RECOGER, notificacion.getTipo(), "El tipo debe coincidir con LISTO_PARA_RECOGER");
        assertEquals("¡Tu coche está listo!", notificacion.getTitulo(), "El título debe coincidir");
        assertFalse(notificacion.isLeida(), "La notificación debe nacer como no leída");

        notificacion.setLeida(true);
        assertTrue(notificacion.isLeida(), "Tras marcarla, la notificación debe quedar leída");
    }

    /**
     * PRUEBA 22:
     * Verifica que NotificacionService persiste correctamente una notificación
     * en el repositorio mediante un mock, y que el objeto devuelto contiene
     * el ID, el tipo y el cliente esperados.
     */
    @Test
    @DisplayName("Prueba 22: Persistencia de NotificacionCliente a través de NotificacionService")
    void test22_PersistenciaNotificacionEnServicio() {
        NotificacionClienteRepository repoMock = mock(NotificacionClienteRepository.class);
        NotificacionService notifService = new NotificacionService();
        ReflectionTestUtils.setField(notifService, "notificacionRepository", repoMock);

        Cliente cliente = new Cliente("María Sánchez", "622222222", "maria@email.com", "maria123");
        NotificacionCliente guardadaMock = new NotificacionCliente(
                cliente, null, TipoNotificacion.EN_PROCESO, "En proceso", "Tu vehículo ha empezado a repararse"
        );
        guardadaMock.setId(101L);

        when(repoMock.save(any(NotificacionCliente.class))).thenReturn(guardadaMock);

        NotificacionCliente resultado = notifService.crearYProcesarNotificacion(
                cliente, null, TipoNotificacion.EN_PROCESO, "En proceso", "Tu vehículo ha empezado a repararse"
        );

        assertNotNull(resultado, "El resultado no debe ser nulo");
        assertEquals(101L, resultado.getId(), "El ID debe ser 101");
        assertEquals(TipoNotificacion.EN_PROCESO, resultado.getTipo(), "El tipo debe ser EN_PROCESO");
        verify(repoMock, times(1)).save(any(NotificacionCliente.class));
    }

    // =========================================================================
    //  NUEVAS PRUEBAS UNITARIAS (23 – 33) — NOVIEMBRE 2026
    //  Cobertura extendida: VehiculoService, CitaPreviaService, MaterialService,
    //  ReparacionService (máquina de estados) y ClienteService.
    // =========================================================================

    /**
     * PRUEBA 23 (VehiculoService - Validación de matrícula vacía):
     * Verifica que el registro de un vehículo falla con IllegalArgumentException
     * cuando la matrícula proporcionada está vacía o es nula, protegiendo la integridad
     * de los datos del inventario de vehículos.
     */
    @Test
    @DisplayName("Prueba 23: Rechazo de registro de vehículo con matrícula vacía")
    void test23_RegistroVehiculoConMatriculaVacia() {
        VehiculoRepository vehiculoRepo = mock(VehiculoRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        VehiculoService vehiculoService = new VehiculoService();
        ReflectionTestUtils.setField(vehiculoService, "vehiculoRepository", vehiculoRepo);
        ReflectionTestUtils.setField(vehiculoService, "clienteRepository", clienteRepo);

        // Caso 1: Matrícula vacía (cadena en blanco)
        VehiculoRequestDTO dto = new VehiculoRequestDTO();
        dto.setMatricula("   ");
        dto.setMarca("Toyota");
        dto.setModelo("Corolla");
        dto.setAno(2020);
        dto.setClienteNombre("Juan García");
        dto.setClienteTelefono("600123456");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            vehiculoService.registrarVehiculo(dto);
        });

        assertEquals("La matrícula no puede estar vacía.", ex.getMessage());
    }

    /**
     * PRUEBA 24 (VehiculoService - Validación de matrícula duplicada):
     * Verifica que el sistema rechaza el registro de un vehículo cuando ya existe
     * otro vehículo con la misma matrícula en la base de datos, evitando duplicados.
     */
    @Test
    @DisplayName("Prueba 24: Rechazo de registro de vehículo con matrícula duplicada")
    void test24_RegistroVehiculoConMatriculaDuplicada() {
        VehiculoRepository vehiculoRepo = mock(VehiculoRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        VehiculoService vehiculoService = new VehiculoService();
        ReflectionTestUtils.setField(vehiculoService, "vehiculoRepository", vehiculoRepo);
        ReflectionTestUtils.setField(vehiculoService, "clienteRepository", clienteRepo);

        // Simulamos que la matrícula ya existe en la base de datos
        when(vehiculoRepo.existsByMatricula("1234ABC")).thenReturn(true);

        VehiculoRequestDTO dto = new VehiculoRequestDTO();
        dto.setMatricula("1234ABC");
        dto.setMarca("Toyota");
        dto.setModelo("Corolla");
        dto.setAno(2020);
        dto.setClienteNombre("Juan García");
        dto.setClienteTelefono("600123456");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            vehiculoService.registrarVehiculo(dto);
        });

        assertTrue(ex.getMessage().contains("Ya existe un vehículo con la matrícula"),
                "Debe indicar que la matrícula ya está registrada");
    }

    /**
     * PRUEBA 25 (VehiculoService - Validación de año inválido):
     * Verifica que el registro de un vehículo falla cuando el año proporcionado
     * es anterior a 1900 o nulo, garantizando que solo se registran fechas lógicas.
     */
    @Test
    @DisplayName("Prueba 25: Rechazo de registro de vehículo con año anterior a 1900")
    void test25_RegistroVehiculoConAnoInvalido() {
        VehiculoRepository vehiculoRepo = mock(VehiculoRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        VehiculoService vehiculoService = new VehiculoService();
        ReflectionTestUtils.setField(vehiculoService, "vehiculoRepository", vehiculoRepo);
        ReflectionTestUtils.setField(vehiculoService, "clienteRepository", clienteRepo);

        when(vehiculoRepo.existsByMatricula("9999ZZZ")).thenReturn(false);

        VehiculoRequestDTO dto = new VehiculoRequestDTO();
        dto.setMatricula("9999ZZZ");
        dto.setMarca("Seat");
        dto.setModelo("Ibiza");
        dto.setAno(1850);  // Año anterior a 1900 (inválido)
        dto.setClienteNombre("Pedro López");
        dto.setClienteTelefono("611222333");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            vehiculoService.registrarVehiculo(dto);
        });

        assertEquals("El año del vehículo debe ser válido y posterior a 1900.", ex.getMessage());
    }

    /**
     * PRUEBA 26 (CitaPreviaService - Detección de fines de semana):
     * Verifica que el método esFinDeSemana() reconoce correctamente un sábado
     * y un domingo como fin de semana, y un día laborable como día entre semana.
     */
    @Test
    @DisplayName("Prueba 26: Detección correcta de fines de semana en CitaPreviaService")
    void test26_DeteccionFinDeSemana() {
        CitaPreviaService citaService = new CitaPreviaService();

        // Buscamos el próximo sábado y domingo a partir de hoy
        LocalDate hoy = LocalDate.now();
        LocalDate proximoSabado = hoy.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY));
        LocalDate proximoDomingo = proximoSabado.plusDays(1);
        LocalDate proximoLunes = proximoDomingo.plusDays(1);

        assertTrue(citaService.esFinDeSemana(proximoSabado),
                "Un sábado debe ser detectado como fin de semana");
        assertTrue(citaService.esFinDeSemana(proximoDomingo),
                "Un domingo debe ser detectado como fin de semana");
        assertFalse(citaService.esFinDeSemana(proximoLunes),
                "Un lunes NO debe ser detectado como fin de semana");
    }

    /**
     * PRUEBA 27 (CitaPreviaService - Detección de festivos nacionales):
     * Verifica que el método esFestivo() identifica correctamente las fechas
     * festivas oficiales (1 de enero, 25 de diciembre) y rechaza días laborables.
     */
    @Test
    @DisplayName("Prueba 27: Detección correcta de festivos nacionales en CitaPreviaService")
    void test27_DeteccionFestivosNacionales() {
        CitaPreviaService citaService = new CitaPreviaService();

        // Festivos conocidos (año futuro para evitar problemas con fechas pasadas)
        LocalDate anoNuevo = LocalDate.of(2027, 1, 1);
        LocalDate navidad = LocalDate.of(2027, 12, 25);
        LocalDate diaConstitucion = LocalDate.of(2027, 12, 6);

        // Día laborable normal (15 de marzo de 2027)
        LocalDate diaLaboral = LocalDate.of(2027, 3, 15);

        assertTrue(citaService.esFestivo(anoNuevo), "El 1 de enero debe ser festivo");
        assertTrue(citaService.esFestivo(navidad), "El 25 de diciembre debe ser festivo");
        assertTrue(citaService.esFestivo(diaConstitucion), "El 6 de diciembre debe ser festivo");
        assertFalse(citaService.esFestivo(diaLaboral), "El 15 de marzo no es festivo");
    }

    /**
     * PRUEBA 28 (CitaPreviaService - Validación de día laborable):
     * Verifica que el método esDiaLaborable() combina correctamente las comprobaciones
     * de fin de semana y festivo, devolviendo true solo para días hábiles.
     */
    @Test
    @DisplayName("Prueba 28: Validación combinada de día laborable en CitaPreviaService")
    void test28_ValidacionDiaLaborable() {
        CitaPreviaService citaService = new CitaPreviaService();

        // Un martes 15 de marzo de 2027 (ni festivo ni fin de semana) → laborable
        LocalDate martes = LocalDate.of(2027, 3, 16); // 16 marzo 2027 es martes
        assertTrue(citaService.esDiaLaborable(martes),
                "Un martes que no es festivo debe ser día laborable");

        // Navidad 2027 (25 de diciembre, sábado) → no laborable por festivo
        LocalDate navidad2027 = LocalDate.of(2027, 12, 25);
        assertFalse(citaService.esDiaLaborable(navidad2027),
                "Navidad no es día laborable aunque caiga en día entre semana");

        // Un domingo cualquiera → no laborable
        LocalDate hoy = LocalDate.now();
        LocalDate proximoDomingo = hoy.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SUNDAY));
        assertFalse(citaService.esDiaLaborable(proximoDomingo),
                "Un domingo no debe ser día laborable");
    }

    /**
     * PRUEBA 29 (MaterialService - Rechazo por stock insuficiente):
     * Verifica que al intentar usar más unidades de un material de las que hay
     * en stock, el sistema lanza IllegalArgumentException indicando la falta de stock.
     */
    @Test
    @DisplayName("Prueba 29: Rechazo de uso de material cuando el stock es insuficiente")
    void test29_UsarMaterialConStockInsuficiente() {
        MaterialRepository materialRepo = mock(MaterialRepository.class);
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionMaterialRepository reparacionMaterialRepo = mock(ReparacionMaterialRepository.class);
        ReparacionService reparacionServiceMock = mock(ReparacionService.class);

        MaterialService materialService = new MaterialService();
        ReflectionTestUtils.setField(materialService, "materialRepository", materialRepo);
        ReflectionTestUtils.setField(materialService, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(materialService, "reparacionMaterialRepository", reparacionMaterialRepo);
        ReflectionTestUtils.setField(materialService, "reparacionService", reparacionServiceMock);

        // Mock de la reparación y del presupuesto aceptado
        Reparacion reparacion = new Reparacion();
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(reparacion));
        when(reparacionServiceMock.tienePresupuestoAceptado(1L)).thenReturn(true);

        // Material con solo 2 unidades de stock
        Material material = new Material("Pastillas de freno", "Pastillas delanteras", 2, 35.00);
        when(materialRepo.findById(10L)).thenReturn(Optional.of(material));

        // Intentar usar 5 unidades (más de las disponibles)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            materialService.usarMaterialEnReparacion(1L, 10L, 5);
        });

        assertTrue(ex.getMessage().contains("Stock insuficiente"),
                "Debe indicar que no hay stock suficiente");
    }

    /**
     * PRUEBA 30 (MaterialService - Bloqueo sin presupuesto aceptado):
     * Verifica que el sistema impide consumir materiales del almacén si el presupuesto
     * de la reparación aún no ha sido ACEPTADO por el cliente.
     */
    @Test
    @DisplayName("Prueba 30: Bloqueo de uso de material sin presupuesto aceptado por el cliente")
    void test30_UsarMaterialSinPresupuestoAceptado() {
        MaterialRepository materialRepo = mock(MaterialRepository.class);
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionMaterialRepository reparacionMaterialRepo = mock(ReparacionMaterialRepository.class);
        ReparacionService reparacionServiceMock = mock(ReparacionService.class);

        MaterialService materialService = new MaterialService();
        ReflectionTestUtils.setField(materialService, "materialRepository", materialRepo);
        ReflectionTestUtils.setField(materialService, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(materialService, "reparacionMaterialRepository", reparacionMaterialRepo);
        ReflectionTestUtils.setField(materialService, "reparacionService", reparacionServiceMock);

        // Mock de la reparación existente
        Reparacion reparacion = new Reparacion();
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(reparacion));

        // El presupuesto NO está aceptado
        when(reparacionServiceMock.tienePresupuestoAceptado(1L)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            materialService.usarMaterialEnReparacion(1L, 10L, 2);
        });

        assertTrue(ex.getMessage().contains("Operación bloqueada"),
                "Debe indicar que la operación está bloqueada hasta que el presupuesto sea aceptado");
    }

    /**
     * PRUEBA 31 (ReparacionService - Máquina de estados: Rechazo de entrega directa desde EN_PROCESO):
     * Verifica que la transición directa de EN_PROCESO a ENTREGADA es rechazada, ya que primero
     * el mecánico debe finalizar la reparación y el administrador debe confirmarla.
     */
    @Test
    @DisplayName("Prueba 31: Rechazo de transición directa de EN_PROCESO a ENTREGADA")
    void test31_RechazoTransicionDirectaEnProcesoAEntregada() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService reparacionService = new ReparacionService();
        ReflectionTestUtils.setField(reparacionService, "reparacionRepository", reparacionRepo);

        // Reparación actualmente EN_PROCESO
        Reparacion reparacion = new Reparacion();
        reparacion.setEstado(EstadoReparacion.EN_PROCESO);
        reparacion.setMecanico(new Usuario("Mec", "mec@t.com", "pass", "600", Rol.MECANICO));
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(reparacion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reparacionService.actualizarEstado(1L, "ENTREGADA");
        });

        assertTrue(ex.getMessage().contains("No se puede entregar el vehículo directamente desde EN_PROCESO"),
                "Debe prohibir saltar de EN_PROCESO directamente a ENTREGADA");
    }

    /**
     * PRUEBA 32 (ReparacionService - Máquina de estados: Rechazo de modificación tras ENTREGADA):
     * Verifica que una vez que la reparación está en estado ENTREGADA, no se puede
     * modificar su estado a ningún otro valor, protegiendo la integridad de reparaciones cerradas.
     */
    @Test
    @DisplayName("Prueba 32: Rechazo de cambio de estado en reparación ya ENTREGADA")
    void test32_RechazoModificacionEstadoEntregada() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService reparacionService = new ReparacionService();
        ReflectionTestUtils.setField(reparacionService, "reparacionRepository", reparacionRepo);

        // Reparación ya ENTREGADA (operación cerrada)
        Reparacion reparacion = new Reparacion();
        reparacion.setEstado(EstadoReparacion.ENTREGADA);
        reparacion.setMecanico(new Usuario("Mec", "mec@t.com", "pass", "600", Rol.MECANICO));
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(reparacion));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reparacionService.actualizarEstado(1L, "EN_PROCESO");
        });

        assertTrue(ex.getMessage().contains("ya está ENTREGADA"),
                "Debe indicar que una reparación entregada no se puede modificar");
    }

    /**
     * PRUEBA 33 (ReparacionService - Máquina de estados: Rechazo de retroceso en el flujo):
     * Verifica que no se permite retroceder en la cadena de estados (por ejemplo, de FINALIZADA
     * a PENDIENTE), garantizando el flujo unidireccional de la reparación.
     */
    @Test
    @DisplayName("Prueba 33: Rechazo de retroceso en el flujo de estados de reparación")
    void test33_RechazoRetrocesoEstadoReparacion() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService reparacionService = new ReparacionService();
        ReflectionTestUtils.setField(reparacionService, "reparacionRepository", reparacionRepo);

        // Reparación actualmente FINALIZADA
        Reparacion reparacion = new Reparacion();
        reparacion.setEstado(EstadoReparacion.FINALIZADA);
        reparacion.setMecanico(new Usuario("Mec", "mec@t.com", "pass", "600", Rol.MECANICO));
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(reparacion));

        // Intentar retroceder a PENDIENTE (ordinal 0 < ordinal 2 de FINALIZADA)
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reparacionService.actualizarEstado(1L, "PENDIENTE");
        });

        assertTrue(ex.getMessage().contains("No se puede cambiar de"),
                "Debe indicar que no se puede retroceder en el flujo de estados");
    }

    // =========================================================================
    //  NUEVAS PRUEBAS UNITARIAS (34 – 38)
    // =========================================================================

    /**
     * PRUEBA 34 (CitaPreviaService - Solicitud pública sin sesión):
     * Verifica que una solicitud pública sin iniciar sesión exige correo electrónico válido
     * y se crea siempre en estado PENDIENTE guardando el emailContacto en la cita sin crear cuenta.
     */
    @Test
    @DisplayName("Prueba 34: Cita previa sin sesión exige correo y se crea como PENDIENTE")
    void test34_CitaPublicaSinSesionCreaPendienteYExigeEmail() {
        CitaPreviaRepository citaRepo = mock(CitaPreviaRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        VehiculoRepository vehiculoRepo = mock(VehiculoRepository.class);

        CitaPreviaService citaService = new CitaPreviaService();
        ReflectionTestUtils.setField(citaService, "citaRepository", citaRepo);
        ReflectionTestUtils.setField(citaService, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(citaService, "vehiculoRepository", vehiculoRepo);

        LocalDate diaLaborable = LocalDate.of(2027, 3, 16);

        // 1) Sin email debe rechazarse
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            citaService.crearCita(
                    null, null, "4321XYZ", "Sofía Navarro", "699112233",
                    "", "Seat", "Ibiza", 2020,
                    diaLaborable, "10:00", "Revisión periódica", "Sin observaciones",
                    "CONFIRMADA", true
            );
        });
        assertTrue(ex.getMessage().contains("correo electrónico válido"));

        // 2) Con email válido se crea en estado PENDIENTE aunque se pase CONFIRMADA
        when(vehiculoRepo.findByMatricula("4321XYZ")).thenReturn(Optional.empty());
        when(clienteRepo.findByTelefono("699112233")).thenReturn(Optional.empty());
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(50L);
            return c;
        });
        when(vehiculoRepo.save(any(Vehiculo.class))).thenAnswer(inv -> {
            Vehiculo v = inv.getArgument(0);
            v.setId(70L);
            return v;
        });
        when(citaRepo.findByFechaOrderByFranjaHorariaAsc(diaLaborable)).thenReturn(Collections.emptyList());
        when(citaRepo.findByCodigoCita(anyString())).thenReturn(Optional.empty());
        when(citaRepo.save(any(CitaPrevia.class))).thenAnswer(inv -> {
            CitaPrevia cp = inv.getArgument(0);
            cp.setId(90L);
            return cp;
        });

        Map<String, Object> creada = citaService.crearCita(
                null, null, "4321XYZ", "Sofía Navarro", "699112233",
                "sofia.navarro@ejemplo.com", "Seat", "Ibiza", 2020,
                diaLaborable, "10:00", "Revisión periódica", "Revisión anual",
                "CONFIRMADA", true
        );

        assertEquals("PENDIENTE", creada.get("estado"));
        assertEquals("sofia.navarro@ejemplo.com", creada.get("emailContacto"));
    }

    /**
     * PRUEBA 35 (CitaPreviaService + NotificacionService - Notificación al rechazar cita):
     * Verifica que al cambiar el estado de una cita PENDIENTE a CANCELADA (rechazo),
     * se invoca a NotificacionService para notificar al correo de contacto del cliente.
     */
    @Test
    @DisplayName("Prueba 35: Rechazar una cita previa notifica por correo al cliente")
    void test35_RechazarCitaNotificaPorEmail() {
        CitaPreviaRepository citaRepo = mock(CitaPreviaRepository.class);
        NotificacionService notifService = mock(NotificacionService.class);

        CitaPreviaService citaService = new CitaPreviaService();
        ReflectionTestUtils.setField(citaService, "citaRepository", citaRepo);
        ReflectionTestUtils.setField(citaService, "notificacionService", notifService);

        Cliente cliente = new Cliente("Sofía Navarro", "699112233");
        Vehiculo vehiculo = new Vehiculo("4321XYZ", "Seat", "Ibiza", 2020, cliente);
        CitaPrevia cita = new CitaPrevia(
                cliente, vehiculo, LocalDate.of(2027, 3, 16), "10:00",
                "Revisión periódica", "", "PENDIENTE"
        );
        cita.setId(90L);
        cita.setEmailContacto("sofia.navarro@ejemplo.com");

        when(citaRepo.findById(90L)).thenReturn(Optional.of(cita));
        when(citaRepo.save(any(CitaPrevia.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> res = citaService.cambiarEstadoCita(90L, "CANCELADA", "Elevador ocupado en esa franja");

        assertEquals("CANCELADA", res.get("estado"));
        assertEquals(true, res.get("notificacionEnviada"));
        verify(notifService, times(1)).notificarCambioEstadoCita(
                argThat(c -> "CANCELADA".equals(c.getEstado()) && "sofia.navarro@ejemplo.com".equals(c.getEmailContacto())),
                eq("PENDIENTE"),
                eq("Elevador ocupado en esa franja")
        );
    }

    /**
     * PRUEBA 36 (ReparacionService - Respuesta presencial/telefónica a presupuesto sin cuenta y RBAC):
     * Verifica que un recepcionista puede marcar un presupuesto como ACEPTADO o RECHAZADO sin contraseña
     * del cliente, que un mecánico no está autorizado, y que el rechazo mantiene la reparación abierta.
     */
    @Test
    @DisplayName("Prueba 36: Aceptar/Rechazar presupuesto presencialmente sin cuenta y validación RBAC")
    void test36_ResponderPresupuestoPresencialAceptarRechazarYRBAC() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        PresupuestoReparacionRepository presupuestoRepo = mock(PresupuestoReparacionRepository.class);
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        IncidenciaReparacionRepository incidenciaRepo = mock(IncidenciaReparacionRepository.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "presupuestoRepository", presupuestoRepo);
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepo);
        ReflectionTestUtils.setField(service, "incidenciaRepository", incidenciaRepo);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);

        Cliente clienteSinCuenta = new Cliente("María Sin Cuenta", "622222222");
        Vehiculo vehiculo = new Vehiculo("9012GHI", "Seat", "Ibiza", 2018, clienteSinCuenta);
        Usuario recepcionista = new Usuario("Laura", "laura@taller.com", "1234", "600000002", Rol.RECEPCIONISTA);
        recepcionista.setId(2L);
        Usuario mecanico = new Usuario("Pedro", "pedro@taller.com", "1234", "600000003", Rol.MECANICO);
        mecanico.setId(3L);

        Reparacion reparacion = new Reparacion(vehiculo, recepcionista, "Frenos", 150.0, 2);
        reparacion.setId(10L);
        reparacion.setMecanico(mecanico);

        PresupuestoReparacion presupuesto = new PresupuestoReparacion(
                "PRES-2026-010", reparacion, "Cambio de pastillas", LocalDate.now().plusDays(15), 15.0, 21.0
        );
        presupuesto.getLineas().add(new LineaPresupuesto(presupuesto, "PIEZA", "Pastillas", 1.0, 50.0, null));
        presupuesto.recalcularTotales();

        when(reparacionRepo.findById(10L)).thenReturn(Optional.of(reparacion));
        when(presupuestoRepo.findByReparacionId(10L)).thenReturn(Optional.of(presupuesto));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(recepcionista));
        when(usuarioRepo.findById(3L)).thenReturn(Optional.of(mecanico));

        // 1) Mecánico bloqueado por RBAC
        assertThrows(IllegalArgumentException.class, () ->
                service.responderPresupuestoPersonal(10L, "ACEPTAR", "Presencial", null, 3L)
        );

        // 2) Recepcionista rechaza presencialmente -> presupuesto RECHAZADO pero reparación sigue PENDIENTE (abierta a nuevo presupuesto)
        Map<String, Object> resRechazo = service.responderPresupuestoPersonal(10L, "RECHAZAR", "Por teléfono", "Demasiado caro", 2L);
        assertEquals("RECHAZADO", resRechazo.get("estado"));
        assertEquals("PENDIENTE", resRechazo.get("estadoReparacion"));
        assertEquals(EstadoReparacion.PENDIENTE, reparacion.getEstado());

        // 3) Si se emite nuevo presupuesto y Recepción lo marca ACEPTADO, pasa a EN_PROCESO
        presupuesto.setEstado("PENDIENTE_ACEPTACION");
        Map<String, Object> resAceptado = service.responderPresupuestoPersonal(10L, "ACEPTAR", "Presencial en recepción", null, 2L);
        assertEquals("ACEPTADO", resAceptado.get("estado"));
        assertEquals("EN_PROCESO", resAceptado.get("estadoReparacion"));
        assertEquals(EstadoReparacion.EN_PROCESO, reparacion.getEstado());
    }

    /**
     * PRUEBA 37 (ReparacionService - Cancelación tras rechazo del presupuesto y bloqueo posterior):
     * Verifica que solo se puede cancelar cuando el presupuesto está RECHAZADO, que cambia el estado
     * a CANCELADA con fechaSalida y sin generar factura, y bloquea cambios posteriores.
     */
    @Test
    @DisplayName("Prueba 37: Cancelar reparación tras rechazo de presupuesto y bloquear cambios posteriores")
    void test37_CancelarReparacionTrasRechazoYBloqueoModificacion() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        PresupuestoReparacionRepository presupuestoRepo = mock(PresupuestoReparacionRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        IncidenciaReparacionRepository incidenciaRepo = mock(IncidenciaReparacionRepository.class);
        FacturaRepository facturaRepo = mock(FacturaRepository.class);
        ReparacionMaterialRepository repMatRepo = mock(ReparacionMaterialRepository.class);
        ConfiguracionTallerService configService = mock(ConfiguracionTallerService.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);

        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "presupuestoRepository", presupuestoRepo);
        ReflectionTestUtils.setField(service, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(service, "incidenciaRepository", incidenciaRepo);
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepo);
        ReflectionTestUtils.setField(service, "reparacionMaterialRepository", repMatRepo);
        ReflectionTestUtils.setField(service, "configuracionService", configService);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);

        Cliente cliente = new Cliente("Carlos", "633333333", "carlos@email.com", "pass");
        cliente.setId(5L);
        Vehiculo vehiculo = new Vehiculo("3456JKL", "VW", "Golf", 2022, cliente);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "1234", "600", Rol.RECEPCIONISTA);
        Reparacion reparacion = new Reparacion(vehiculo, recep, "Avería motor", 300.0, 4);
        reparacion.setId(20L);

        PresupuestoReparacion presupuesto = new PresupuestoReparacion(
                "PRES-2026-020", reparacion, "Diagnóstico motor", LocalDate.now().plusDays(15), 15.0, 21.0
        );

        when(reparacionRepo.findById(20L)).thenReturn(Optional.of(reparacion));
        when(presupuestoRepo.findByReparacionId(20L)).thenReturn(Optional.of(presupuesto));
        when(clienteRepo.findById(5L)).thenReturn(Optional.of(cliente));
        when(configService.obtenerConfiguracion()).thenReturn(new ConfiguracionTaller());
        when(repMatRepo.findByReparacionId(20L)).thenReturn(Collections.emptyList());
        when(incidenciaRepo.findByReparacionIdOrderByFechaAsc(20L)).thenReturn(Collections.emptyList());
        when(facturaRepo.findByReparacionId(20L)).thenReturn(Optional.empty());

        // No permite cancelar mientras el presupuesto siga PENDIENTE_ACEPTACION
        assertThrows(IllegalStateException.class, () ->
                service.cancelarReparacionPorRechazo(20L, "Retira coche", 5L, null)
        );

        // Tras rechazar el presupuesto, el cliente sí puede cancelar y retirar el vehículo
        presupuesto.setEstado("RECHAZADO");
        var resp = service.cancelarReparacionPorRechazo(20L, "Prefiere no reparar", 5L, null);

        assertEquals("CANCELADA", resp.getEstado());
        assertNotNull(resp.getFechaSalida());
        assertNull(resp.getFactura());
        verify(facturaRepo, never()).save(any(Factura.class));

        // Una vez CANCELADA, no permite transicionar a otro estado
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.actualizarEstado(20L, "EN_PROCESO")
        );
        assertTrue(ex.getMessage().contains("CANCELADA"));
    }

    /**
     * PRUEBA 38 (ClienteService - Auto-vinculación de reparaciones al registrarse):
     * Verifica que cuando un cliente ya existente en taller (sin cuenta) se registra
     * con su teléfono, ClienteService vincula automáticamente todas las reparaciones
     * de sus vehículos a reparacionesVinculadas sin necesidad de pegar el localizador.
     */
    @Test
    @DisplayName("Prueba 38: Auto-vinculación de reparaciones existentes al registrarse el cliente por teléfono")
    void test38_RegistroClienteAutoVinculaReparacionesExistentes() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService reparacionService = mock(ReparacionService.class);

        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(clienteService, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(clienteService, "reparacionService", reparacionService);

        Cliente clienteExistente = new Cliente("María Sánchez", "622222222");
        clienteExistente.setId(2L);

        Vehiculo vehiculo = new Vehiculo("9012GHI", "Seat", "Ibiza", 2018, clienteExistente);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "1234", "600", Rol.RECEPCIONISTA);
        Reparacion rep1 = new Reparacion(vehiculo, recep, "Pastillas de freno", 140.0, 2);
        rep1.setId(101L);
        Reparacion rep2 = new Reparacion(vehiculo, recep, "Revisión aceite", 90.0, 1);
        rep2.setId(102L);

        when(clienteRepo.findByEmail("maria@email.com")).thenReturn(Optional.empty());
        when(clienteRepo.findByTelefono("622222222")).thenReturn(Optional.of(clienteExistente));
        when(reparacionRepo.findByVehiculoClienteId(2L)).thenReturn(List.of(rep1, rep2));
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> resultado = clienteService.registrarCliente(
                "María Sánchez", "622222222", "maria@email.com", "maria123"
        );

        assertEquals(2, resultado.get("reparacionesVinculadas"));
        assertEquals(2, clienteExistente.getReparacionesVinculadas().size());
        assertTrue(clienteExistente.getReparacionesVinculadas().stream().anyMatch(r -> r.getId().equals(101L)));
        assertTrue(clienteExistente.getReparacionesVinculadas().stream().anyMatch(r -> r.getId().equals(102L)));
        verify(clienteRepo, times(1)).save(clienteExistente);
    }

    // =========================================================================
    //  NUEVAS PRUEBAS UNITARIAS PARA HHU #7, #8, #19, #20 Y #21 (39 – 43)
    // =========================================================================

    /**
     * PRUEBA 39 (HU #7 - Ordenación de prioridad de vehículos según categorías):
     * Verifica que ordenarReparacionesPorCriterio ordena correctamente por orden de llegada,
     * tiempo estimado de reparación y uso de piezas.
     */
    @Test
    @DisplayName("Prueba 39 (HU #7): Ordenación por orden de llegada, tiempo estimado y uso de piezas")
    void test39_OrdenacionPrioridadVehiculosPorCategorias() {
        ReparacionService service = new ReparacionService();

        ReparacionResponseDTO r1 = new ReparacionResponseDTO();
        r1.setId(1L);
        r1.setFechaHoraEntrada("2026-11-18T10:00:00");
        r1.setDiasEstimados(5);
        r1.setTotalUnidadesPiezas(1);
        r1.setCosteMateriales(15.0);

        ReparacionResponseDTO r2 = new ReparacionResponseDTO();
        r2.setId(2L);
        r2.setFechaHoraEntrada("2026-11-16T08:30:00");
        r2.setDiasEstimados(2);
        r2.setTotalUnidadesPiezas(6);
        r2.setCosteMateriales(180.0);

        ReparacionResponseDTO r3 = new ReparacionResponseDTO();
        r3.setId(3L);
        r3.setFechaHoraEntrada("2026-11-19T12:15:00");
        r3.setDiasEstimados(1);
        r3.setTotalUnidadesPiezas(3);
        r3.setCosteMateriales(65.0);

        List<ReparacionResponseDTO> entrada = List.of(r1, r2, r3);

        // 1) Orden de llegada (más antiguo primero)
        List<ReparacionResponseDTO> porLlegada = service.ordenarReparacionesPorCriterio(entrada, "LLEGADA_ASC");
        assertEquals(List.of(2L, 1L, 3L), porLlegada.stream().map(ReparacionResponseDTO::getId).toList());

        // 2) Tiempo estimado (menor duración primero)
        List<ReparacionResponseDTO> porTiempo = service.ordenarReparacionesPorCriterio(entrada, "TIEMPO_ASC");
        assertEquals(List.of(3L, 2L, 1L), porTiempo.stream().map(ReparacionResponseDTO::getId).toList());

        // 3) Uso de piezas (mayor consumo primero)
        List<ReparacionResponseDTO> porPiezas = service.ordenarReparacionesPorCriterio(entrada, "PIEZAS_DESC");
        assertEquals(List.of(2L, 3L, 1L), porPiezas.stream().map(ReparacionResponseDTO::getId).toList());
    }

    /**
     * PRUEBA 40 (HU #8 - Registro cronológico de espera):
     * Verifica que obtenerRegistroCronologicoEspera devuelve únicamente reparaciones PENDIENTE
     * ordenadas de la más antigua a la más reciente y calcula su tiempo de estancia.
     */
    @Test
    @DisplayName("Prueba 40 (HU #8): Registro cronológico ordena pendientes por antigüedad y excluye EN_PROCESO")
    void test40_RegistroCronologicoEsperaYExclusionEnProceso() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);

        Cliente cliente = new Cliente("Carlos", "600111222");
        Usuario recep = new Usuario("Laura", "laura@taller.com", "123", "600", Rol.RECEPCIONISTA);
        Vehiculo v1 = new Vehiculo("1111AAA", "Seat", "Leon", 2020, cliente);
        Vehiculo v2 = new Vehiculo("2222BBB", "Ford", "Fiesta", 2019, cliente);
        Vehiculo v3 = new Vehiculo("3333CCC", "Opel", "Corsa", 2021, cliente);

        Reparacion repMasReciente = new Reparacion(v1, recep, "Ruido puerta", 100.0, 2);
        repMasReciente.setId(10L);
        repMasReciente.setFechaHoraEntrada(LocalDateTime.now().minusHours(5));
        repMasReciente.setEstado(EstadoReparacion.PENDIENTE);

        Reparacion repMasAntigua = new Reparacion(v2, recep, "Fallo motor", 250.0, 4);
        repMasAntigua.setId(11L);
        repMasAntigua.setFechaHoraEntrada(LocalDateTime.now().minusDays(2).minusHours(4));
        repMasAntigua.setEstado(EstadoReparacion.PENDIENTE);

        Reparacion repEnProceso = new Reparacion(v3, recep, "Cambio embrague", 400.0, 3);
        repEnProceso.setId(12L);
        repEnProceso.setFechaHoraEntrada(LocalDateTime.now().minusDays(4));
        repEnProceso.setEstado(EstadoReparacion.EN_PROCESO);

        when(reparacionRepo.findAll()).thenReturn(List.of(repMasReciente, repMasAntigua, repEnProceso));

        List<ReparacionResponseDTO> cola = service.obtenerRegistroCronologicoEspera();

        assertEquals(2, cola.size(), "Solo deben figurar los vehículos pendientes de iniciar reparación");
        assertEquals(11L, cola.get(0).getId(), "El vehículo con mayor estancia debe aparecer primero");
        assertEquals(10L, cola.get(1).getId(), "El vehículo recién llegado debe ir después");
        assertTrue(cola.get(0).getTiempoEstanciaTexto().contains("días"), "Debe incluir la estancia en días y horas");
    }

    /**
     * PRUEBA 41 (HU #19 - Temporizador de mano de obra):
     * Verifica iniciar, pausar y terminar el temporizador, y el bloqueo si el mecánico
     * intenta iniciar otro contador mientras ya tiene uno en marcha.
     */
    @Test
    @DisplayName("Prueba 41 (HU #19): Ciclo Iniciar/Pausar/Terminar temporizador y bloqueo de simultaneidad")
    void test41_TemporizadorManoDeObraYBloqueoSimultaneo() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        PresupuestoReparacionRepository presupuestoRepo = mock(PresupuestoReparacionRepository.class);
        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "presupuestoRepository", presupuestoRepo);

        Cliente cliente = new Cliente("Juan", "611111111");
        Vehiculo v1 = new Vehiculo("1234ABC", "Toyota", "Corolla", 2019, cliente);
        Vehiculo v2 = new Vehiculo("5678DEF", "Ford", "Focus", 2021, cliente);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "123", "600", Rol.RECEPCIONISTA);
        Usuario mecanico = new Usuario("Pedro", "pedro@taller.com", "123", "600", Rol.MECANICO);
        mecanico.setId(3L);

        Reparacion rep1 = new Reparacion(v1, recep, "Frenos", 150.0, 2);
        rep1.setId(1L);
        rep1.setMecanico(mecanico);
        rep1.setEstado(EstadoReparacion.EN_PROCESO);

        Reparacion rep2 = new Reparacion(v2, recep, "Aceite", 100.0, 1);
        rep2.setId(2L);
        rep2.setMecanico(mecanico);
        rep2.setEstado(EstadoReparacion.EN_PROCESO);

        PresupuestoReparacion presAceptado = new PresupuestoReparacion(
                "PRES-2026-100", rep1, "OK", LocalDate.now().plusDays(10), 15.0, 21.0);
        presAceptado.setEstado("ACEPTADO");

        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(rep1));
        when(reparacionRepo.findById(2L)).thenReturn(Optional.of(rep2));
        when(reparacionRepo.findByMecanicoId(3L)).thenReturn(List.of(rep1, rep2));
        when(presupuestoRepo.findByReparacionId(1L)).thenReturn(Optional.of(presAceptado));
        when(presupuestoRepo.findByReparacionId(2L)).thenReturn(Optional.of(presAceptado));
        when(reparacionRepo.save(any(Reparacion.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1) Iniciar en rep1
        ReparacionResponseDTO iniciado = service.iniciarTemporizador(1L, 3L);
        assertEquals("EN_MARCHA", iniciado.getEstadoTemporizador());
        assertNotNull(iniciado.getInicioTemporizador());

        // 2) Intentar iniciar rep2 mientras rep1 está EN_MARCHA -> debe bloquearse
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.iniciarTemporizador(2L, 3L)
        );
        assertTrue(ex.getMessage().contains("Pausa la tarea activa"));

        // 3) Simular 30 minutos trabajados, pausar y luego terminar
        rep1.setInicioTemporizador(LocalDateTime.now().minusMinutes(30));
        ReparacionResponseDTO pausado = service.pausarTemporizador(1L);
        assertEquals("PAUSADO", pausado.getEstadoTemporizador());
        assertTrue(pausado.getSegundosAcumuladosTemporizador() >= 1800L);
        assertEquals(0.50, pausado.getHorasTrabajoMecanico(), 0.02);

        ReparacionResponseDTO terminado = service.terminarTemporizador(1L);
        assertEquals("DETENIDO", terminado.getEstadoTemporizador());
        assertNull(terminado.getInicioTemporizador());
        assertEquals(0.50, terminado.getHorasTrabajoMecanico(), 0.02);
    }

    /**
     * PRUEBA 42 (HU #20 - Supervisión del trabajo de los mecánicos + RBAC):
     * Verifica la suma de tiempos y desglose por mecánico, el mensaje cuando no hay registros,
     * y el bloqueo de seguridad RBAC cuando intenta acceder un MECANICO o CLIENTE.
     */
    @Test
    @DisplayName("Prueba 42 (HU #20): Supervisión de tiempos por mecánico, desglose y seguridad RBAC")
    void test42_SupervisionTiemposMecanicosYSeguridadRBAC() {
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "usuarioRepository", usuarioRepo);
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);

        Usuario mecPedro = new Usuario("Pedro Ruiz", "pedro@taller.com", "123", "600", Rol.MECANICO);
        mecPedro.setId(3L);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "123", "600", Rol.RECEPCIONISTA);
        Cliente cli = new Cliente("Ana", "612345678");
        Vehiculo v1 = new Vehiculo("1234ABC", "Toyota", "Corolla", 2019, cli);

        Reparacion rep1 = new Reparacion(v1, recep, "Cambio distribución", 300.0, 3);
        rep1.setId(50L);
        rep1.setMecanico(mecPedro);
        rep1.setEstado(EstadoReparacion.FINALIZADA);
        rep1.setHorasTrabajoMecanico(2.5);
        rep1.setSegundosAcumuladosTemporizador(9000L); // 2 h 30 min

        when(usuarioRepo.findByRol(Rol.MECANICO)).thenReturn(List.of(mecPedro));
        when(reparacionRepo.findByMecanicoId(3L)).thenReturn(List.of(rep1));

        // 1) Acceso denegado a MECANICO y CLIENTE
        assertThrows(SecurityException.class, () ->
                service.obtenerSupervisionTiemposMecanicos(null, "TODOS", "MECANICO")
        );
        assertThrows(SecurityException.class, () ->
                service.obtenerSupervisionTiemposMecanicos(null, "TODOS", "CLIENTE")
        );

        // 2) Consulta válida como ADMIN
        Map<String, Object> supervision = service.obtenerSupervisionTiemposMecanicos(3L, "MES", "ADMIN");
        assertEquals(false, supervision.get("sinRegistros"));
        assertEquals("2 h 30 min", supervision.get("tiempoTotalGlobalFormateado"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> mecs = (List<Map<String, Object>>) supervision.get("mecanicos");
        assertEquals(1, mecs.size());
        assertEquals(1, mecs.get(0).get("numeroReparaciones"));
        assertEquals("2 h 30 min", mecs.get(0).get("tiempoTotalFormateado"));

        // 3) Si el mecánico no tiene reparaciones en el periodo -> sinRegistros = true
        when(reparacionRepo.findByMecanicoId(3L)).thenReturn(Collections.emptyList());
        Map<String, Object> vacio = service.obtenerSupervisionTiemposMecanicos(3L, "HOY", "ADMIN");
        assertEquals(true, vacio.get("sinRegistros"));
        assertTrue(vacio.get("mensaje").toString().contains("No existen registros"));
    }

    /**
     * PRUEBA 43 (HU #21 - Historial de reparaciones para mecánicos y modo solo lectura):
     * Verifica que solo se devuelven reparaciones pasadas concluidas ordenadas de más reciente
     * a más antigua, y que se bloquea modificar incidencias/horas en reparaciones ya concluidas.
     */
    @Test
    @DisplayName("Prueba 43 (HU #21): Historial técnico para mecánicos ordenado y protegido en solo lectura")
    void test43_HistorialVehiculoParaMecanicoYSoloLectura() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        IncidenciaReparacionRepository incidenciaRepo = mock(IncidenciaReparacionRepository.class);
        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "incidenciaRepository", incidenciaRepo);

        Cliente cli = new Cliente("María", "622222222");
        Vehiculo veh = new Vehiculo("9012GHI", "Seat", "Ibiza", 2018, cli);
        veh.setId(5L);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "123", "600", Rol.RECEPCIONISTA);
        Usuario mec = new Usuario("Ana", "ana@taller.com", "123", "600", Rol.MECANICO);

        Reparacion antigua = new Reparacion(veh, recep, "Cambio batería", 120.0, 1);
        antigua.setId(1L);
        antigua.setMecanico(mec);
        antigua.setEstado(EstadoReparacion.ENTREGADA);
        antigua.setFechaEntrada(LocalDate.now().minusDays(60));
        antigua.setFechaSalida(LocalDate.now().minusDays(59));

        Reparacion reciente = new Reparacion(veh, recep, "Cambio pastillas", 140.0, 2);
        reciente.setId(2L);
        reciente.setMecanico(mec);
        reciente.setEstado(EstadoReparacion.FINALIZADA);
        reciente.setFechaEntrada(LocalDate.now().minusDays(10));
        reciente.setFechaSalida(LocalDate.now().minusDays(9));

        Reparacion activa = new Reparacion(veh, recep, "Revisión actual", 100.0, 1);
        activa.setId(3L);
        activa.setEstado(EstadoReparacion.EN_PROCESO);

        when(reparacionRepo.findByVehiculoId(5L)).thenReturn(List.of(antigua, reciente, activa));
        when(reparacionRepo.findById(1L)).thenReturn(Optional.of(antigua));

        List<ReparacionResponseDTO> historial = service.obtenerHistorialVehiculoParaMecanico(5L, 3L);

        assertEquals(2, historial.size(), "Solo deben aparecer las reparaciones concluidas excluyendo la activa");
        assertEquals(2L, historial.get(0).getId(), "La reparación más reciente debe ir primero");
        assertEquals(1L, historial.get(1).getId(), "La reparación más antigua debe ir después");

        // Verificar protección de solo lectura: impedir añadir notas o modificar horas sobre una reparación ENTREGADA
        IllegalArgumentException exInc = assertThrows(IllegalArgumentException.class, () ->
                service.registrarIncidencia(1L, "Intento editar pasado", 0.0, null)
        );
        assertTrue(exInc.getMessage().contains("concluida"));

        IllegalArgumentException exHoras = assertThrows(IllegalArgumentException.class, () ->
                service.actualizarHorasTrabajo(1L, 5.0)
        );
        assertTrue(exHoras.getMessage().contains("ENTREGADA"));
    }
}
