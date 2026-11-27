package com.taller;

import com.taller.dtos.MaterialResponseDTO;
import com.taller.dtos.ReparacionRequestDTO;
import com.taller.dtos.ReparacionResponseDTO;
import com.taller.dtos.UsuarioResponseDTO;
import com.taller.dtos.VehiculoRequestDTO;
import com.taller.dtos.VehiculoResponseDTO;
import com.taller.models.*;
import com.taller.repositories.ClienteRepository;
import com.taller.repositories.CitaPreviaRepository;
import com.taller.repositories.ConfiguracionTallerRepository;
import com.taller.repositories.FacturaRepository;
import com.taller.repositories.IncidenciaReparacionRepository;
import com.taller.repositories.MaterialRepository;
import com.taller.repositories.NotificacionClienteRepository;
import com.taller.repositories.PresupuestoReparacionRepository;
import com.taller.repositories.ReparacionRepository;
import com.taller.repositories.ReparacionMaterialRepository;
import com.taller.repositories.UsuarioRepository;
import com.taller.repositories.VehiculoRepository;
import com.taller.config.WebConfig;
import com.taller.services.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

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

    // =========================================================================
    //  HU #64 — SEGURIDAD EN CONTRASEÑAS (BCrypt + política mínima) — PRUEBAS 44 – 51
    //  Escritas siguiendo TDD (Red → Green → Refactor) antes de la implementación.
    // =========================================================================

    /**
     * PRUEBA 44 (PasswordSecurityService - Hash BCrypt irreversible con salt):
     * Verifica que la contraseña nunca se guarda en texto plano, que el hash tiene formato BCrypt,
     * que la verificación funciona y que dos contraseñas iguales producen hashes distintos (salt único).
     */
    @Test
    @DisplayName("Prueba 44 (HU #64): Hash BCrypt irreversible con salt único y verificación segura")
    void test44_HashBCryptConSaltUnicoYVerificacion() {
        PasswordSecurityService seguridad = new PasswordSecurityService();

        String hash1 = seguridad.hashear("Segura2026");
        String hash2 = seguridad.hashear("Segura2026");

        assertNotEquals("Segura2026", hash1, "La contraseña nunca debe almacenarse en texto plano");
        assertTrue(seguridad.estaHasheada(hash1), "El valor almacenado debe tener formato BCrypt ($2a$/$2b$/$2y$)");
        assertNotEquals(hash1, hash2, "Dos contraseñas iguales deben generar hashes distintos gracias al salt");
        assertTrue(seguridad.coincide("Segura2026", hash1));
        assertTrue(seguridad.coincide("Segura2026", hash2));
        assertFalse(seguridad.coincide("Incorrecta99", hash1));
        assertFalse(seguridad.coincide(null, hash1));
        assertFalse(seguridad.coincide("Segura2026", null));
    }

    /**
     * PRUEBA 45 (PasswordSecurityService - Política mínima de fortaleza):
     * Verifica que se rechazan contraseñas vacías, de menos de 8 caracteres, sin números o sin letras,
     * y que se acepta una contraseña que cumple la política.
     */
    @Test
    @DisplayName("Prueba 45 (HU #64): Política mínima de contraseña (8 caracteres, letras y números)")
    void test45_PoliticaMinimaFortalezaContrasena() {
        PasswordSecurityService seguridad = new PasswordSecurityService();

        assertThrows(IllegalArgumentException.class, () -> seguridad.validarFortaleza(null));
        assertThrows(IllegalArgumentException.class, () -> seguridad.validarFortaleza("   "));
        IllegalArgumentException corta = assertThrows(IllegalArgumentException.class,
                () -> seguridad.validarFortaleza("abc12"));
        assertTrue(corta.getMessage().contains("8 caracteres"));
        assertThrows(IllegalArgumentException.class, () -> seguridad.validarFortaleza("soloLetras"));
        assertThrows(IllegalArgumentException.class, () -> seguridad.validarFortaleza("12345678"));

        assertDoesNotThrow(() -> seguridad.validarFortaleza("Segura2026"));
    }

    /**
     * PRUEBA 46 (UsuarioService - Alta de empleado con hash y login contra el hash):
     * Verifica que al crear un empleado la contraseña se persiste como hash BCrypt, que se rechazan
     * contraseñas débiles y que el login valida correctamente contra el hash almacenado.
     */
    @Test
    @DisplayName("Prueba 46 (HU #64): Alta de empleado guarda hash BCrypt y login valida contra el hash")
    void test46_CrearEmpleadoGuardaHashYLoginContraHash() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(usuarioService, "passwordSecurityService", new PasswordSecurityService());

        when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // Contraseña débil -> rechazada
        com.taller.dtos.UsuarioRequestDTO debil = new com.taller.dtos.UsuarioRequestDTO();
        debil.setNombre("Nuevo Mecánico");
        debil.setEmail("nuevo@taller.com");
        debil.setPassword("1234");
        debil.setTelefono("600999999");
        debil.setRol("MECANICO");
        assertThrows(IllegalArgumentException.class, () -> usuarioService.crearUsuario(debil));
        verify(usuarioRepository, never()).save(any(Usuario.class));

        // Contraseña robusta -> guardada como hash
        com.taller.dtos.UsuarioRequestDTO dto = new com.taller.dtos.UsuarioRequestDTO();
        dto.setNombre("Nuevo Mecánico");
        dto.setEmail("nuevo@taller.com");
        dto.setPassword("Taller2026");
        dto.setTelefono("600999999");
        dto.setRol("MECANICO");
        usuarioService.crearUsuario(dto);

        org.mockito.ArgumentCaptor<Usuario> captor = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(1)).save(captor.capture());
        Usuario guardado = captor.getValue();
        assertNotEquals("Taller2026", guardado.getPassword(), "La contraseña no puede guardarse en texto plano");
        assertTrue(guardado.getPassword().startsWith("$2"), "Debe almacenarse como hash BCrypt");

        // Login contra el hash almacenado
        when(usuarioRepository.findByEmail("nuevo@taller.com")).thenReturn(Optional.of(guardado));
        UsuarioResponseDTO ok = usuarioService.login("nuevo@taller.com", "Taller2026");
        assertEquals("MECANICO", ok.getRol());
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> usuarioService.login("nuevo@taller.com", "Otra2026"));
        assertEquals("Contraseña incorrecta.", ex.getMessage());
    }

    /**
     * PRUEBA 47 (ClienteService - Registro de cliente con hash y login por email/teléfono):
     * Verifica que el registro de un cliente guarda la contraseña hasheada, rechaza contraseñas débiles
     * y permite iniciar sesión por correo o teléfono comparando contra el hash.
     */
    @Test
    @DisplayName("Prueba 47 (HU #64): Registro de cliente guarda hash BCrypt y login por email/teléfono")
    void test47_RegistroClienteGuardaHashYLogin() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(clienteService, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(clienteService, "reparacionService", mock(ReparacionService.class));
        ReflectionTestUtils.setField(clienteService, "passwordSecurityService", new PasswordSecurityService());

        when(clienteRepo.findByEmail("lucia@email.com")).thenReturn(Optional.empty());
        when(clienteRepo.findByTelefono("677000111")).thenReturn(Optional.empty());
        when(clienteRepo.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            if (c.getId() == null) c.setId(99L);
            return c;
        });

        // Contraseña débil -> rechazada
        IllegalArgumentException exDebil = assertThrows(IllegalArgumentException.class, () ->
                clienteService.registrarCliente("Lucía Gómez", "677000111", "lucia@email.com", "lucia")
        );
        assertTrue(exDebil.getMessage().contains("8 caracteres"));

        clienteService.registrarCliente("Lucía Gómez", "677000111", "lucia@email.com", "Lucia2026");

        org.mockito.ArgumentCaptor<Cliente> captor = org.mockito.ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepo, atLeastOnce()).save(captor.capture());
        Cliente guardado = captor.getValue();
        assertNotEquals("Lucia2026", guardado.getPassword());
        assertTrue(guardado.getPassword().startsWith("$2"));

        when(clienteRepo.findByEmail("lucia@email.com")).thenReturn(Optional.of(guardado));
        when(clienteRepo.findByTelefono("677000111")).thenReturn(Optional.of(guardado));
        assertNotNull(clienteService.loginCliente("lucia@email.com", "Lucia2026"));
        assertNotNull(clienteService.loginCliente("677000111", "Lucia2026"));
        IllegalArgumentException exMal = assertThrows(IllegalArgumentException.class,
                () -> clienteService.loginCliente("lucia@email.com", "Erronea2026"));
        assertEquals("Contraseña incorrecta.", exMal.getMessage());
    }

    /**
     * PRUEBA 48 (RecuperacionPasswordService - Restablecimiento con política y hash):
     * Verifica que la nueva contraseña del flujo de recuperación con código de 6 dígitos
     * debe cumplir la política mínima y se persiste hasheada con BCrypt.
     */
    @Test
    @DisplayName("Prueba 48 (HU #64): Restablecer contraseña exige política mínima y guarda hash BCrypt")
    @SuppressWarnings("unchecked")
    void test48_RestablecerPasswordConPoliticaYHash() {
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        RecuperacionPasswordService recuperacion = new RecuperacionPasswordService();
        ReflectionTestUtils.setField(recuperacion, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(recuperacion, "passwordSecurityService", new PasswordSecurityService());

        PasswordSecurityService seguridad = new PasswordSecurityService();
        Cliente cliente = new Cliente("Juan Pérez", "611111111", "juan@email.com", seguridad.hashear("juan123"));
        when(clienteRepo.findByEmail("juan@email.com")).thenReturn(Optional.of(cliente));

        recuperacion.solicitarCodigo("juan@email.com");
        Map<String, Object> pendientes = (Map<String, Object>) ReflectionTestUtils.getField(recuperacion, "codigosPendientes");
        String codigo = (String) ReflectionTestUtils.invokeMethod(pendientes.get("juan@email.com"), "getCodigo");

        // Contraseña débil (antes bastaban 4 caracteres) -> rechazada
        assertThrows(IllegalArgumentException.class, () ->
                recuperacion.restablecerPassword("juan@email.com", codigo, "abcd", "abcd")
        );

        recuperacion.restablecerPassword("juan@email.com", codigo, "NuevaClave2026", "NuevaClave2026");

        assertNotEquals("NuevaClave2026", cliente.getPassword());
        assertTrue(seguridad.estaHasheada(cliente.getPassword()));
        assertTrue(seguridad.coincide("NuevaClave2026", cliente.getPassword()));
        verify(clienteRepo, times(1)).save(cliente);
    }

    /**
     * PRUEBA 49 (ReparacionService - Confirmación de presupuesto validando contra el hash):
     * Verifica que la aceptación/rechazo de un presupuesto por el cliente valida la contraseña
     * contra el hash BCrypt almacenado, rechazando contraseñas incorrectas.
     */
    @Test
    @DisplayName("Prueba 49 (HU #64): Confirmar presupuesto con contraseña valida contra el hash BCrypt")
    void test49_ConfirmarPresupuestoValidaContraHash() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        PresupuestoReparacionRepository presupuestoRepo = mock(PresupuestoReparacionRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        IncidenciaReparacionRepository incidenciaRepo = mock(IncidenciaReparacionRepository.class);

        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "presupuestoRepository", presupuestoRepo);
        ReflectionTestUtils.setField(service, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(service, "incidenciaRepository", incidenciaRepo);
        ReflectionTestUtils.setField(service, "eventPublisher", mock(ApplicationEventPublisher.class));
        ReflectionTestUtils.setField(service, "passwordSecurityService", new PasswordSecurityService());

        PasswordSecurityService seguridad = new PasswordSecurityService();
        Cliente cliente = new Cliente("Juan Pérez", "611111111", "juan@email.com", seguridad.hashear("Juan2026"));
        Vehiculo vehiculo = new Vehiculo("1234ABC", "Toyota", "Corolla", 2019, cliente);
        Usuario recep = new Usuario("Laura", "laura@taller.com", "x", "600", Rol.RECEPCIONISTA);
        Reparacion reparacion = new Reparacion(vehiculo, recep, "Ruido motor", 200.0, 3);
        reparacion.setId(30L);

        PresupuestoReparacion presupuesto = new PresupuestoReparacion(
                "PRES-2026-030", reparacion, "Distribución", LocalDate.now().plusDays(10), 15.0, 21.0
        );
        presupuesto.getLineas().add(new LineaPresupuesto(presupuesto, "PIEZA", "Kit correa", 1.0, 120.0, null));
        presupuesto.recalcularTotales();

        when(reparacionRepo.findById(30L)).thenReturn(Optional.of(reparacion));
        when(presupuestoRepo.findByReparacionId(30L)).thenReturn(Optional.of(presupuesto));
        when(clienteRepo.findAll()).thenReturn(List.of(cliente));

        // Contraseña incorrecta -> bloqueado, presupuesto sin cambios
        assertThrows(SecurityException.class, () ->
                service.responderPresupuestoCliente(30L, "ACEPTAR", "Juan2025", null)
        );
        assertEquals("PENDIENTE_ACEPTACION", presupuesto.getEstado());

        // Contraseña correcta comparada contra el hash -> ACEPTADO
        Map<String, Object> res = service.responderPresupuestoCliente(30L, "ACEPTAR", "Juan2026", null);
        assertEquals("ACEPTADO", res.get("estado"));
    }

    /**
     * PRUEBA 50 (Migración de contraseñas heredadas en texto plano):
     * Verifica que las cuentas antiguas (p. ej. juan123 / admin123) se migran a hash BCrypt al arrancar
     * sin dejar de ser válidas, y que un login con contraseña heredada la re-hashea de forma transparente.
     */
    @Test
    @DisplayName("Prueba 50 (HU #64): Migración de contraseñas heredadas a BCrypt sin perder el acceso")
    void test50_MigracionPasswordsHeredadasSinPerderAcceso() {
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        ClienteRepository clienteRepo = mock(ClienteRepository.class);
        PasswordSecurityService seguridad = new PasswordSecurityService();

        Usuario admin = new Usuario("Antonio", "admin@taller.com", "admin123", "600000001", Rol.ADMIN);
        Cliente juan = new Cliente("Juan Pérez", "611111111", "juan@email.com", "juan123");
        Cliente sinCuenta = new Cliente("María", "622222222");
        Cliente yaHasheado = new Cliente("Carlos", "633333333", "carlos@email.com", seguridad.hashear("carlos123"));
        String hashPrevioCarlos = yaHasheado.getPassword();

        when(usuarioRepo.findAll()).thenReturn(List.of(admin));
        when(clienteRepo.findAll()).thenReturn(List.of(juan, sinCuenta, yaHasheado));

        com.taller.config.DatosPrueba datos = new com.taller.config.DatosPrueba();
        ReflectionTestUtils.setField(datos, "usuarioRepository", usuarioRepo);
        ReflectionTestUtils.setField(datos, "clienteRepository", clienteRepo);
        ReflectionTestUtils.setField(datos, "passwordSecurityService", seguridad);

        int migradas = datos.migrarPasswordsTextoPlano();

        assertEquals(2, migradas, "Solo deben migrarse las 2 contraseñas que estaban en texto plano");
        assertTrue(seguridad.estaHasheada(admin.getPassword()));
        assertTrue(seguridad.estaHasheada(juan.getPassword()));
        assertTrue(seguridad.coincide("admin123", admin.getPassword()), "admin123 debe seguir siendo válida");
        assertTrue(seguridad.coincide("juan123", juan.getPassword()), "juan123 debe seguir siendo válida");
        assertNull(sinCuenta.getPassword(), "Los clientes sin cuenta no deben recibir contraseña");
        assertEquals(hashPrevioCarlos, yaHasheado.getPassword(), "Un hash existente no debe re-hashearse");
        verify(usuarioRepo, times(1)).save(admin);
        verify(clienteRepo, times(1)).save(juan);
        verify(clienteRepo, never()).save(yaHasheado);

        // Login transparente de una cuenta heredada aún en texto plano: entra y queda re-hasheada
        UsuarioRepository repoLogin = mock(UsuarioRepository.class);
        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", repoLogin);
        ReflectionTestUtils.setField(usuarioService, "passwordSecurityService", seguridad);
        Usuario pedro = new Usuario("Pedro", "pedro@taller.com", "pedro123", "600000003", Rol.MECANICO);
        when(repoLogin.findByEmail("pedro@taller.com")).thenReturn(Optional.of(pedro));

        assertNotNull(usuarioService.login("pedro@taller.com", "pedro123"));
        assertTrue(seguridad.estaHasheada(pedro.getPassword()), "Tras el login la contraseña heredada queda hasheada");
        verify(repoLogin, times(1)).save(pedro);
    }

    /**
     * PRUEBA 51 (Privacidad en la API - JSON sin contraseñas ni hashes):
     * Verifica que al serializar a JSON las entidades Usuario y Cliente (respuestas de la API),
     * nunca se incluye la contraseña en texto plano ni su hash almacenado.
     */
    @Test
    @DisplayName("Prueba 51 (HU #64): Las respuestas JSON nunca exponen contraseñas ni hashes")
    void test51_JsonNoExponePasswordNiHash() throws Exception {
        PasswordSecurityService seguridad = new PasswordSecurityService();
        String hash = seguridad.hashear("Segura2026");

        Usuario usuario = new Usuario("Antonio", "admin@taller.com", hash, "600000001", Rol.ADMIN);
        Cliente cliente = new Cliente("Juan Pérez", "611111111", "juan@email.com", hash);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.findAndRegisterModules();
        String jsonUsuario = mapper.writeValueAsString(usuario);
        String jsonCliente = mapper.writeValueAsString(cliente);

        assertFalse(jsonUsuario.contains("password"), "El JSON del empleado no debe incluir el campo password");
        assertFalse(jsonUsuario.contains(hash), "El JSON del empleado no debe incluir el hash");
        assertFalse(jsonCliente.contains("password"), "El JSON del cliente no debe incluir el campo password");
        assertFalse(jsonCliente.contains(hash), "El JSON del cliente no debe incluir el hash");
        assertFalse(jsonCliente.contains("Segura2026"));
    }

    /**
     * PRUEBA 52 (Tesorería - Prevención de doble contabilidad de piezas en caja):
     * Verifica que obtenerCuentaTaller() descuenta del saldo de caja únicamente las compras
     * de stock del almacén (gastosComprasStock) y no vuelve a restar de la caja el coste de
     * las piezas del almacén instaladas en reparaciones (gastosPiezasUsadas).
     */
    @Test
    @DisplayName("Prueba 52: El saldo de Tesorería solo descuenta compras de stock y no cobra dos veces las piezas usadas")
    void test52_TesoreriaNoDescuentaDobleLasPiezasUsadasEnReparaciones() {
        ReparacionRepository reparacionRepo = mock(ReparacionRepository.class);
        ReparacionMaterialRepository repMatRepo = mock(ReparacionMaterialRepository.class);
        FacturaRepository facturaRepo = mock(FacturaRepository.class);
        ConfiguracionTallerService configService = mock(ConfiguracionTallerService.class);

        ReparacionService service = new ReparacionService();
        ReflectionTestUtils.setField(service, "reparacionRepository", reparacionRepo);
        ReflectionTestUtils.setField(service, "reparacionMaterialRepository", repMatRepo);
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepo);
        ReflectionTestUtils.setField(service, "configuracionService", configService);

        when(configService.obtenerConfiguracion()).thenReturn(new ConfiguracionTaller(15.0, 35.0, 2000.0));
        when(reparacionRepo.findAll()).thenReturn(Collections.emptyList());
        when(facturaRepo.findAll()).thenReturn(Collections.emptyList());

        // 1) El administrador compra 10 filtros a 10.00 € = 100.00 € de salida real de caja
        service.registrarGastoCompraStock(1L, "Filtro de aceite", 10, 10.00, 0, "COMPRA_STOCK");

        // 2) El mecánico instala 2 de esos filtros del almacén (20.00 €) en una reparación
        Material filtro = new Material("Filtro de aceite", "Filtro motor", 8, 10.00);
        filtro.setId(1L);
        Reparacion rep = new Reparacion();
        rep.setId(1L);
        ReparacionMaterial uso = new ReparacionMaterial(rep, filtro, 2);
        uso.setId(1L);
        when(repMatRepo.findAll()).thenReturn(List.of(uso));

        Map<String, Object> cuenta = service.obtenerCuentaTaller();

        assertEquals(2000.00, (Double) cuenta.get("fondoInicial"), 0.001);
        assertEquals(100.00, (Double) cuenta.get("gastosComprasStock"), 0.001, "Las compras de stock son 100.00 €");
        assertEquals(20.00, (Double) cuenta.get("gastosPiezasUsadas"), 0.001, "El coste analítico de piezas usadas es 20.00 €");
        assertEquals(100.00, (Double) cuenta.get("gastosMateriales"), 0.001, "El gasto de caja en materiales solo debe ser la compra de stock (100.00 €)");
        assertEquals(1900.00, (Double) cuenta.get("saldoActual"), 0.001, "El saldo de caja debe ser 2000 - 100 = 1900.00 € (sin restar otra vez los 20 €)");
    }

    /**
     * PRUEBA 53 (Bug 2.2 - Inmutabilidad de facturas históricas al cambiar tarifas):
     * Verifica que actualizar la configuración del taller (precio/hora y margen %) no recalcula
     * facturas ya emitidas, que generarFactura() bloquea modificaciones en reparaciones ENTREGADAS
     * y que en estado CONFIRMADA congela los importes permitiendo solo completar datos fiscales.
     */
    @Test
    @DisplayName("Prueba 53 (Bug 2.2): El cambio de tarifas no recalcula facturas históricas y las cerradas son inmutables")
    void test53_CambioDeTarifasNoRecalculaFacturasHistoricasYEntregadaEsInmutable() {
        // 1) ConfiguracionTallerService actualiza tarifas sin tocar facturas históricas
        ConfiguracionTallerRepository configRepo = mock(ConfiguracionTallerRepository.class);
        ConfiguracionTallerService configService = new ConfiguracionTallerService();
        ReflectionTestUtils.setField(configService, "configuracionRepository", configRepo);

        ConfiguracionTaller configInicial = new ConfiguracionTaller(15.0, 35.0, 2000.0);
        when(configRepo.findAll()).thenReturn(List.of(configInicial));
        when(configRepo.save(any(ConfiguracionTaller.class))).thenAnswer(inv -> inv.getArgument(0));

        ConfiguracionTaller actualizada = configService.actualizarConfiguracion(25.0, 50.0, 2500.0);
        assertEquals(25.0, actualizada.getMargenBeneficioPorcentaje(), 0.001);
        assertEquals(50.0, actualizada.getPrecioHoraManoObra(), 0.001);
        assertEquals(2500.0, actualizada.getFondoInicial(), 0.001);

        // 2) En ReparacionService.generarFactura: ENTREGADA bloquea modificación; CONFIRMADA congela importes
        ReparacionRepository repRepo = mock(ReparacionRepository.class);
        FacturaRepository factRepo = mock(FacturaRepository.class);
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        ReparacionMaterialRepository repMatRepo = mock(ReparacionMaterialRepository.class);
        ConfiguracionTallerService cfgSvcMock = mock(ConfiguracionTallerService.class);

        ReparacionService repService = new ReparacionService();
        ReflectionTestUtils.setField(repService, "reparacionRepository", repRepo);
        ReflectionTestUtils.setField(repService, "facturaRepository", factRepo);
        ReflectionTestUtils.setField(repService, "clienteRepository", cliRepo);
        ReflectionTestUtils.setField(repService, "reparacionMaterialRepository", repMatRepo);
        ReflectionTestUtils.setField(repService, "configuracionService", cfgSvcMock);

        Cliente cliente = new Cliente("Laura Gómez", "600112233");
        cliente.setId(1L);
        Vehiculo vehiculo = new Vehiculo("5555FFF", "Seat", "León", 2021, cliente);
        vehiculo.setId(1L);
        Reparacion rep = new Reparacion();
        rep.setId(10L);
        rep.setVehiculo(vehiculo);
        rep.setHorasTrabajoMecanico(2.0);
        rep.setEstado(EstadoReparacion.ENTREGADA);

        // Factura histórica emitida con las tarifas antiguas (35 €/h, 15% margen)
        Factura facturaHistorica = new Factura("FAC-2026-0001", rep, 100.0, 2.0, 35.0, 15.0, 21.0);
        facturaHistorica.setId(100L);
        double totalOriginal = facturaHistorica.getTotalFactura();

        when(repRepo.findById(10L)).thenReturn(Optional.of(rep));
        when(factRepo.findByReparacionId(10L)).thenReturn(Optional.of(facturaHistorica));
        when(repMatRepo.findByReparacionId(10L)).thenReturn(Collections.emptyList());
        when(cfgSvcMock.obtenerConfiguracion()).thenReturn(actualizada); // Tarifas nuevas: 50 €/h y 25%
        when(factRepo.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cliRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // 2a) ENTREGADA: prohibido modificar/recalcular la factura
        IllegalArgumentException exEntregada = assertThrows(IllegalArgumentException.class,
                () -> repService.generarFactura(10L, null, "12345678Z", "Calle Mayor 1", false));
        assertTrue(exEntregada.getMessage().contains("inmutables"));
        assertEquals(totalOriginal, facturaHistorica.getTotalFactura(), 0.001);

        // 2b) CONFIRMADA: se permiten completar datos fiscales pero las tarifas/totales permanecen congelados
        rep.setEstado(EstadoReparacion.CONFIRMADA);
        repService.generarFactura(10L, null, "12345678z", "Calle Mayor 1, Madrid", true);
        assertEquals(totalOriginal, facturaHistorica.getTotalFactura(), 0.001, "Los importes deben permanecer congelados");
        assertEquals(35.0, facturaHistorica.getPrecioHora(), 0.001, "La tarifa por hora original no debe alterarse");
        assertEquals("12345678Z", facturaHistorica.getDniCifCliente());
        assertEquals("Calle Mayor 1, Madrid", facturaHistorica.getDireccionFiscalCliente());
    }

    /**
     * PRUEBA 54 (HU #58 - Recepción directa de cita previa y apertura automática de reparación):
     * Verifica que recepcionarYAbrirReparacion() convierte una cita CONFIRMADA en RECEPCIONADA,
     * crea la reparación autocompletando avería (motivo + observaciones), guarda reparacionId,
     * y bloquea duplicidades (ya recepcionada) y citas CANCELADAS.
     */
    @Test
    @DisplayName("Prueba 54 (HU #58): Recepción de cita previa abre reparación, vincula reparacionId y bloquea duplicados/canceladas")
    void test54_RecepcionCitaPreviaAbreReparacionYVinculaBloqueandoDuplicadosYCanceladas() {
        CitaPreviaRepository citaRepo = mock(CitaPreviaRepository.class);
        ReparacionService repService = mock(ReparacionService.class);

        CitaPreviaService citaService = new CitaPreviaService();
        ReflectionTestUtils.setField(citaService, "citaRepository", citaRepo);
        ReflectionTestUtils.setField(citaService, "reparacionService", repService);

        Cliente cli = new Cliente("Carlos Ruiz", "611223344");
        cli.setId(5L);
        Vehiculo veh = new Vehiculo("7777GGG", "Ford", "Focus", 2020, cli);
        veh.setId(8L);
        CitaPrevia cita = new CitaPrevia(cli, veh, LocalDate.now().plusDays(1), "10:00",
                "Frenos y suspensión", "Chirrido al frenar en frío", "CONFIRMADA");
        cita.setId(30L);

        when(citaRepo.findById(30L)).thenReturn(Optional.of(cita));
        when(citaRepo.save(any(CitaPrevia.class))).thenAnswer(inv -> inv.getArgument(0));

        ReparacionResponseDTO repMock = new ReparacionResponseDTO();
        repMock.setId(99L);
        repMock.setLocalizador("AB12CD34");
        when(repService.crearReparacion(any(ReparacionRequestDTO.class))).thenAnswer(inv -> {
            ReparacionRequestDTO dto = inv.getArgument(0);
            assertEquals(8L, dto.getVehiculoId());
            assertEquals(2L, dto.getRecepcionistaId());
            assertTrue(dto.getDescripcionAveria().contains("Frenos y suspensión"));
            assertTrue(dto.getDescripcionAveria().contains("Chirrido al frenar en frío"));
            return repMock;
        });

        Map<String, Object> res = citaService.recepcionarYAbrirReparacion(30L, 2L, 180.0, 2, null);

        assertEquals("RECEPCIONADA", cita.getEstado());
        assertEquals(99L, cita.getReparacionId());
        assertEquals("AB12CD34", res.get("localizador"));
        verify(repService, times(1)).crearReparacion(any(ReparacionRequestDTO.class));

        // Bloqueo contra duplicidad: volver a recepcionar la misma cita debe fallar
        assertThrows(IllegalStateException.class,
                () -> citaService.recepcionarYAbrirReparacion(30L, 2L, 180.0, 2, null));

        // Restricción en citas canceladas
        CitaPrevia cancelada = new CitaPrevia(cli, veh, LocalDate.now().plusDays(2), "11:00", "Pre-ITV", "", "CANCELADA");
        cancelada.setId(31L);
        when(citaRepo.findById(31L)).thenReturn(Optional.of(cancelada));
        assertThrows(IllegalStateException.class,
                () -> citaService.recepcionarYAbrirReparacion(31L, 2L, 150.0, 1, null));
    }

    /**
     * PRUEBA 55 (HU #59 - Edición, cambio de rol y baja segura de empleados):
     * Verifica la edición de datos/rol/contraseña (BCrypt), rechazo de email duplicado,
     * protección contra auto-degradación y autoborrado del administrador, y desasignación
     * automática de reparaciones al eliminar un mecánico.
     */
    @Test
    @DisplayName("Prueba 55 (HU #59): Edición y baja de empleados con protección admin y desasignación de mecánico")
    void test55_EdicionYBajaEmpleadosConProteccionAdminYDesasignacionMecanico() {
        UsuarioRepository usuarioRepo = mock(UsuarioRepository.class);
        ReparacionRepository repRepo = mock(ReparacionRepository.class);
        PasswordSecurityService seguridad = new PasswordSecurityService();

        UsuarioService usuarioService = new UsuarioService();
        ReflectionTestUtils.setField(usuarioService, "usuarioRepository", usuarioRepo);
        ReflectionTestUtils.setField(usuarioService, "reparacionRepository", repRepo);
        ReflectionTestUtils.setField(usuarioService, "passwordSecurityService", seguridad);

        Usuario admin = new Usuario("Admin Principal", "admin@taller.com", seguridad.hashear("Admin2026"), "600000001", Rol.ADMIN);
        admin.setId(1L);
        Usuario mecanico = new Usuario("Pedro Mecánico", "pedro@taller.com", seguridad.hashear("Pedro2026"), "600000002", Rol.MECANICO);
        mecanico.setId(2L);

        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(mecanico));
        when(usuarioRepo.findByEmail("admin@taller.com")).thenReturn(Optional.of(admin));
        when(usuarioRepo.findByEmail("pedro.nuevo@taller.com")).thenReturn(Optional.empty());
        when(usuarioRepo.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1) Email duplicado rechazado
        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(2L, "Pedro", "admin@taller.com", "600000002", "MECANICO", null, 1L));

        // 2) Admin no puede quitarse a sí mismo el rol ADMIN ni autoborrarse
        assertThrows(IllegalStateException.class,
                () -> usuarioService.actualizarUsuario(1L, "Admin", "admin@taller.com", "600000001", "MECANICO", null, 1L));
        assertThrows(IllegalStateException.class,
                () -> usuarioService.eliminarUsuario(1L, 1L));

        // 3) Edición válida con cambio de email y nueva contraseña hasheada con BCrypt
        UsuarioResponseDTO editado = usuarioService.actualizarUsuario(
                2L, "Pedro Sánchez", "pedro.nuevo@taller.com", "699888777", "MECANICO", "NuevaClave2026", 1L);
        assertEquals("Pedro Sánchez", editado.getNombre());
        assertEquals("pedro.nuevo@taller.com", editado.getEmail());
        assertTrue(seguridad.estaHasheada(mecanico.getPassword()));
        assertTrue(seguridad.coincide("NuevaClave2026", mecanico.getPassword()));

        // 4) Baja de mecánico desasigna sus reparaciones activas (mecanico = null)
        Reparacion repAsignada = new Reparacion();
        repAsignada.setId(50L);
        repAsignada.setMecanico(mecanico);
        when(repRepo.findByRecepcionistaId(2L)).thenReturn(Collections.emptyList());
        when(repRepo.findByMecanicoId(2L)).thenReturn(List.of(repAsignada));

        Map<String, Object> baja = usuarioService.eliminarUsuario(2L, 1L);
        assertNull(repAsignada.getMecanico(), "La reparación debe quedar sin mecánico asignado");
        assertEquals(1, baja.get("reparacionesDesasignadas"));
        verify(usuarioRepo, times(1)).delete(mecanico);
    }

    /**
     * PRUEBA 56 (HU #60 - Edición comercial y baja protegida de materiales):
     * Verifica que actualizar nombre/descripción/precio rechaza valores inválidos,
     * no registra ningún gasto de stock en Tesorería, y que eliminar un material
     * con historial en reparacion_materiales se bloquea con IllegalStateException.
     */
    @Test
    @DisplayName("Prueba 56 (HU #60): Edición comercial de material sin gasto en Tesorería y baja protegida por historial")
    void test56_EdicionComercialMaterialSinGastoTesoreriaYBajaProtegidaPorHistorial() {
        MaterialRepository matRepo = mock(MaterialRepository.class);
        ReparacionMaterialRepository repMatRepo = mock(ReparacionMaterialRepository.class);
        ReparacionService repService = mock(ReparacionService.class);

        MaterialService materialService = new MaterialService();
        ReflectionTestUtils.setField(materialService, "materialRepository", matRepo);
        ReflectionTestUtils.setField(materialService, "reparacionMaterialRepository", repMatRepo);
        ReflectionTestUtils.setField(materialService, "reparacionService", repService);

        Material bateria = new Material("Batería 12V", "Batería estándar", 6, 85.00);
        bateria.setId(4L);
        when(matRepo.findById(4L)).thenReturn(Optional.of(bateria));
        when(matRepo.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1) Validaciones de nombre vacío y precio <= 0
        assertThrows(IllegalArgumentException.class,
                () -> materialService.actualizarMaterial(4L, "   ", "Desc", 90.0));
        assertThrows(IllegalArgumentException.class,
                () -> materialService.actualizarMaterial(4L, "Batería 12V", "Desc", 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> materialService.actualizarMaterial(4L, "Batería 12V", "Desc", -10.0));

        // 2) Edición válida sin impacto en Tesorería
        MaterialResponseDTO actualizado = materialService.actualizarMaterial(4L, "Batería AGM 70Ah", "Alta capacidad", 112.50);
        assertEquals("Batería AGM 70Ah", actualizado.getNombre());
        assertEquals(112.50, actualizado.getPrecioUnitario(), 0.001);
        verifyNoInteractions(repService);

        // 3) Baja bloqueada si tiene historial en reparacion_materiales
        ReparacionMaterial usoHistorico = new ReparacionMaterial(new Reparacion(), bateria, 1);
        when(repMatRepo.findByMaterialId(4L)).thenReturn(List.of(usoHistorico));
        assertThrows(IllegalStateException.class, () -> materialService.eliminarMaterial(4L));
        verify(matRepo, never()).delete(bateria);

        // 4) Baja permitida si no tiene usos vinculados
        when(repMatRepo.findByMaterialId(4L)).thenReturn(Collections.emptyList());
        materialService.eliminarMaterial(4L);
        verify(matRepo, times(1)).delete(bateria);
    }

    /**
     * PRUEBA 57 (HU #61 - Edición y baja de clientes y vehículos con unicidad e integridad):
     * Verifica validaciones de teléfono/email único en cliente, matrícula única y año válido
     * en vehículo, y bloqueo de borrado cuando existen reparaciones o citas asociadas.
     */
    @Test
    @DisplayName("Prueba 57 (HU #61): Edición y baja de clientes y vehículos con unicidad e integridad histórica")
    void test57_EdicionYBajaClientesYVehiculosConUnicidadEIntegridad() {
        // --- Parte A: ClienteService ---
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        ReparacionRepository repRepo = mock(ReparacionRepository.class);
        VehiculoRepository vehRepo = mock(VehiculoRepository.class);
        CitaPreviaRepository citaRepo = mock(CitaPreviaRepository.class);

        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", cliRepo);
        ReflectionTestUtils.setField(clienteService, "reparacionRepository", repRepo);
        ReflectionTestUtils.setField(clienteService, "vehiculoRepository", vehRepo);
        ReflectionTestUtils.setField(clienteService, "citaPreviaRepository", citaRepo);

        Cliente c1 = new Cliente("Elena Torres", "611000111");
        c1.setId(1L);
        Cliente c2 = new Cliente("Otro Cliente", "622000222");
        c2.setId(2L);

        when(cliRepo.findById(1L)).thenReturn(Optional.of(c1));
        when(cliRepo.findByTelefono("622000222")).thenReturn(Optional.of(c2));
        when(cliRepo.findByTelefono("611999888")).thenReturn(Optional.empty());
        when(cliRepo.findByEmail("elena@correo.com")).thenReturn(Optional.empty());
        when(cliRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // Teléfono duplicado de otro cliente -> error
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.actualizarCliente(1L, "Elena", "622000222", "elena@correo.com", null, null));

        // Edición válida
        Map<String, Object> cliAct = clienteService.actualizarCliente(
                1L, "Elena Torres Gil", "611999888", "elena@correo.com", "87654321x", "Av. Europa 10");
        assertEquals("87654321X", cliAct.get("dniCif"));
        assertEquals("Av. Europa 10", cliAct.get("direccionFiscal"));

        // Baja bloqueada si tiene reparaciones
        when(repRepo.findByVehiculoClienteId(1L)).thenReturn(List.of(new Reparacion()));
        assertThrows(IllegalStateException.class, () -> clienteService.eliminarCliente(1L));

        // Baja permitida sin historial ni vehículos ni citas
        when(repRepo.findByVehiculoClienteId(1L)).thenReturn(Collections.emptyList());
        when(citaRepo.findByClienteIdOrderByFechaDescFranjaHorariaAsc(1L)).thenReturn(Collections.emptyList());
        when(vehRepo.findByClienteId(1L)).thenReturn(Collections.emptyList());
        clienteService.eliminarCliente(1L);
        verify(cliRepo, times(1)).delete(c1);

        // --- Parte B: VehiculoService ---
        VehiculoService vehiculoService = new VehiculoService();
        ReflectionTestUtils.setField(vehiculoService, "vehiculoRepository", vehRepo);
        ReflectionTestUtils.setField(vehiculoService, "clienteRepository", cliRepo);
        ReflectionTestUtils.setField(vehiculoService, "reparacionRepository", repRepo);
        ReflectionTestUtils.setField(vehiculoService, "citaPreviaRepository", citaRepo);

        Vehiculo v1 = new Vehiculo("1111AAA", "Renault", "Clio", 2019, c1);
        v1.setId(10L);
        Vehiculo v2 = new Vehiculo("2222BBB", "Opel", "Corsa", 2020, c2);
        v2.setId(11L);

        when(vehRepo.findById(10L)).thenReturn(Optional.of(v1));
        when(vehRepo.findByMatricula("2222BBB")).thenReturn(Optional.of(v2));
        when(vehRepo.findByMatricula("3333CCC")).thenReturn(Optional.empty());
        when(vehRepo.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

        // Matrícula duplicada y año inválido (< 1900)
        assertThrows(IllegalArgumentException.class,
                () -> vehiculoService.actualizarVehiculo(10L, "2222BBB", "Renault", "Clio", 2019));
        assertThrows(IllegalArgumentException.class,
                () -> vehiculoService.actualizarVehiculo(10L, "3333CCC", "Renault", "Clio", 1850));

        // Edición válida
        VehiculoResponseDTO vehAct = vehiculoService.actualizarVehiculo(10L, "3333ccc", "Renault", "Megane", 2022);
        assertEquals("3333CCC", vehAct.getMatricula());
        assertEquals("Megane", vehAct.getModelo());

        // Baja bloqueada si tiene reparaciones y permitida si está limpio
        when(repRepo.findByVehiculoId(10L)).thenReturn(List.of(new Reparacion()));
        assertThrows(IllegalStateException.class, () -> vehiculoService.eliminarVehiculo(10L));

        when(repRepo.findByVehiculoId(10L)).thenReturn(Collections.emptyList());
        when(citaRepo.findByVehiculoId(10L)).thenReturn(Collections.emptyList());
        vehiculoService.eliminarVehiculo(10L);
        verify(vehRepo, times(1)).delete(v1);
    }

    /**
     * PRUEBA 58 (HU #67 - Perfil personal, datos fiscales y cambio seguro de contraseña del cliente):
     * Verifica que el cliente puede actualizar su contacto y datos fiscales (dniCif en mayúsculas),
     * que se rechazan emails/teléfonos duplicados o inválidos, y que el cambio de contraseña
     * exige verificar passwordActual, confirmación coincidente y fortaleza mínima (BCrypt).
     */
    @Test
    @DisplayName("Prueba 58 (HU #67): Edición de perfil/datos fiscales y cambio seguro de contraseña del cliente")
    void test58_PerfilClienteDatosFiscalesYCambioSeguroPassword() {
        ClienteRepository cliRepo = mock(ClienteRepository.class);
        PasswordSecurityService seguridad = new PasswordSecurityService();

        ClienteService clienteService = new ClienteService();
        ReflectionTestUtils.setField(clienteService, "clienteRepository", cliRepo);
        ReflectionTestUtils.setField(clienteService, "passwordSecurityService", seguridad);

        Cliente cliente = new Cliente("Sofía Martín", "633111222", "sofia@email.com", seguridad.hashear("ClaveActual2026"));
        cliente.setId(7L);
        Cliente otro = new Cliente("Mario", "644999888", "mario@email.com", seguridad.hashear("Mario2026"));
        otro.setId(8L);

        when(cliRepo.findById(7L)).thenReturn(Optional.of(cliente));
        when(cliRepo.findByEmail("mario@email.com")).thenReturn(Optional.of(otro));
        when(cliRepo.findByEmail("sofia.nueva@email.com")).thenReturn(Optional.empty());
        when(cliRepo.findByTelefono("633999000")).thenReturn(Optional.empty());
        when(cliRepo.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1) Rechaza correo de otro cliente y correo con formato inválido
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.actualizarPerfilCliente(7L, "Sofía", "mario@email.com", "633111222", null, null));
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.actualizarPerfilCliente(7L, "Sofía", "correo-invalido", "633111222", null, null));

        // 2) Actualización válida de perfil y datos fiscales
        Map<String, Object> perfil = clienteService.actualizarPerfilCliente(
                7L, "Sofía Martín Ruiz", "sofia.nueva@email.com", "633999000", "45678912a", "Plaza España 3, Sevilla");
        assertEquals("Sofía Martín Ruiz", perfil.get("nombre"));
        assertEquals("45678912A", perfil.get("dniCif"));
        assertEquals("Plaza España 3, Sevilla", perfil.get("direccionFiscal"));

        // 3) Cambio de contraseña: falla si actual incorrecta, si no coinciden o si es débil
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.cambiarPasswordCliente(7L, "Incorrecta1", "NuevaSegura2026", "NuevaSegura2026"));
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.cambiarPasswordCliente(7L, "ClaveActual2026", "NuevaSegura2026", "OtraDistinta2026"));
        assertThrows(IllegalArgumentException.class,
                () -> clienteService.cambiarPasswordCliente(7L, "ClaveActual2026", "1234", "1234"));

        // 4) Cambio de contraseña válido: almacena nuevo hash BCrypt
        clienteService.cambiarPasswordCliente(7L, "ClaveActual2026", "NuevaSegura2026", "NuevaSegura2026");
        assertTrue(seguridad.estaHasheada(cliente.getPassword()));
        assertTrue(seguridad.coincide("NuevaSegura2026", cliente.getPassword()));
        assertFalse(seguridad.coincide("ClaveActual2026", cliente.getPassword()));
    }

    /**
     * PRUEBA 59 (NotificacionService - Resolución de URLs para emails sin localhost hardcodeado):
     * Verifica que NotificacionService resuelve la URL base configurada para los correos,
     * permitiendo enlaces accesibles desde producción (Azure) o configurables por propiedad/entorno.
     */
    @Test
    @DisplayName("Prueba 59: NotificacionService resuelve correctamente la URL base sin localhost hardcodeado")
    void test59_NotificacionServiceResuelveUrlBaseCorrectamente() {
        NotificacionService notifService = new NotificacionService();

        // 1. Con appBaseUrl configurado (por ejemplo, dominio de producción o personalizado)
        notifService.setAppBaseUrl("https://motorflow.midominio.com");
        assertEquals("https://motorflow.midominio.com", notifService.getBaseUrl());

        // Con trailing slash debe normalizarlo
        notifService.setAppBaseUrl("https://motorflow.midominio.com/");
        assertEquals("https://motorflow.midominio.com", notifService.getBaseUrl());

        // 2. Si se configura servidor de correo (modo producción/envío real), debe apuntar a la URL pública de producción
        notifService.setAppBaseUrl(null);
        ReflectionTestUtils.setField(notifService, "mailHost", "smtp.gmail.com");
        assertEquals(NotificacionService.DEFAULT_PROD_URL, notifService.getBaseUrl());

        // 3. En desarrollo local puro (sin servidor de correo ni URL configurada), utiliza localhost
        ReflectionTestUtils.setField(notifService, "mailHost", null);
        assertEquals(NotificacionService.DEFAULT_LOCAL_URL, notifService.getBaseUrl());
    }

    /**
     * PRUEBA 60 (WebConfig - Seguridad CORS, Enrutamiento y Recursos Estáticos sin Duplicación):
     * Verifica la configuración de WebConfig:
     * 1) Política CORS global para /** con métodos HTTP autorizados (GET, POST, PUT, DELETE, PATCH, OPTIONS) y credenciales.
     * 2) Registro de manejadores de recursos para /uploads/** y /** mapeando al frontend local/classpath sin duplicar archivos.
     * 3) Redirección segura de la raíz "/" hacia "/index.html".
     */
    @Test
    @DisplayName("Prueba 60: WebConfig configura correctamente la seguridad CORS, redirección a index.html y recursos estáticos")
    void test60_WebConfigSeguridadCorsYRecursosEstaticos() {
        WebConfig webConfig = new WebConfig();

        // 1. Validar política CORS (Arrange, Act & Assert)
        CorsRegistry corsRegistry = mock(CorsRegistry.class);
        CorsRegistration corsReg = mock(CorsRegistration.class);
        when(corsRegistry.addMapping("/**")).thenReturn(corsReg);
        when(corsReg.allowedOriginPatterns("*")).thenReturn(corsReg);
        when(corsReg.allowedMethods(any(String[].class))).thenReturn(corsReg);
        when(corsReg.allowedHeaders("*")).thenReturn(corsReg);
        when(corsReg.allowCredentials(true)).thenReturn(corsReg);

        webConfig.addCorsMappings(corsRegistry);

        verify(corsRegistry, times(1)).addMapping("/**");
        verify(corsReg, times(1)).allowedOriginPatterns("*");
        verify(corsReg, times(1)).allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        verify(corsReg, times(1)).allowedHeaders("*");
        verify(corsReg, times(1)).allowCredentials(true);

        // 2. Validar redirección de raíz a index.html (Arrange, Act & Assert)
        ViewControllerRegistry viewRegistry = mock(ViewControllerRegistry.class);
        ViewControllerRegistration viewReg = mock(ViewControllerRegistration.class);
        when(viewRegistry.addViewController("/")).thenReturn(viewReg);

        webConfig.addViewControllers(viewRegistry);
        verify(viewRegistry, times(1)).addViewController("/");
        verify(viewReg, times(1)).setViewName("forward:/index.html");

        // 3. Validar registro de recursos estáticos (/uploads/** y /**)
        ResourceHandlerRegistry resourceRegistry = mock(ResourceHandlerRegistry.class);
        ResourceHandlerRegistration uploadReg = mock(ResourceHandlerRegistration.class);
        ResourceHandlerRegistration frontendReg = mock(ResourceHandlerRegistration.class);

        when(resourceRegistry.addResourceHandler("/uploads/**")).thenReturn(uploadReg);
        when(resourceRegistry.addResourceHandler("/**")).thenReturn(frontendReg);
        when(uploadReg.addResourceLocations(any(String[].class))).thenReturn(uploadReg);
        when(frontendReg.addResourceLocations(any(String[].class))).thenReturn(frontendReg);

        webConfig.addResourceHandlers(resourceRegistry);
        verify(resourceRegistry, times(1)).addResourceHandler("/uploads/**");
        verify(resourceRegistry, times(1)).addResourceHandler("/**");
    }
}
