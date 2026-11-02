package com.taller;

import com.taller.dtos.UsuarioResponseDTO;
import com.taller.models.*;
import com.taller.repositories.UsuarioRepository;
import com.taller.services.UsuarioService;
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
}
