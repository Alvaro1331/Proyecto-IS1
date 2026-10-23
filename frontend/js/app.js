const API_BASE = 'http://localhost:8080/api';

function getUsuarioActual() {
    const userStr = sessionStorage.getItem('usuario');
    return userStr ? JSON.parse(userStr) : null;
}

function isLoggedIn() {
    return !!getUsuarioActual();
}

// Ruta de login calculada a partir de la ubicación de este script (funciona con file:// y con servidor)
const LOGIN_URL = new URL('../pages/login.html', document.currentScript.src).href;

function logout() {
    sessionStorage.removeItem('usuario');
    window.location.href = LOGIN_URL;
}

function requireAuth(rolesPermitidos = []) {
    const user = getUsuarioActual();
    if (!user) {
        window.location.href = LOGIN_URL;
        return;
    }
    if (rolesPermitidos.length > 0 && !rolesPermitidos.includes(user.rol)) {
        window.location.href = LOGIN_URL;
    }
}

async function apiFetch(endpoint, options = {}) {
    const { silent, ...fetchOptions } = options;
    try {
        const defaultHeaders = {
            'Content-Type': 'application/json'
        };
        const res = await fetch(`${API_BASE}${endpoint}`, {
            ...fetchOptions,
            headers: {
                ...defaultHeaders,
                ...fetchOptions.headers
            }
        });
        
        let data = null;
        if (res.headers.get('content-type')?.includes('application/json')) {
            data = await res.json();
        }

        if (!res.ok) {
            throw new Error(data?.error || data?.message || `Error: ${res.status}`);
        }
        return data;
    } catch (error) {
        console.error('API Error:', error);
        if (error instanceof TypeError) {
            error.message = 'No se puede conectar con el servidor. Comprueba que el backend está arrancado.';
        }
        if (!silent) mostrarAlerta(error.message, 'error');
        throw error;
    }
}

function mostrarAlerta(mensaje, tipo, contenedorOrId = 'alert-container') {
    let contenedor = (contenedorOrId instanceof HTMLElement)
        ? contenedorOrId
        : document.getElementById(contenedorOrId);

    if (!contenedor) {
        contenedor = document.createElement('div');
        contenedor.id = 'alert-container';
        contenedor.style.position = 'fixed';
        contenedor.style.top = '1rem';
        contenedor.style.right = '1rem';
        contenedor.style.zIndex = '9999';
        document.body.appendChild(contenedor);
    }

    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${tipo}`;
    const icono = tipo === 'error' ? '⚠️' : (tipo === 'success' ? '✅' : 'ℹ️');
    alertDiv.innerHTML = `
        <span class="alert-text"><strong class="alert-icon">${icono}</strong> ${mensaje}</span>
        <button type="button" class="close-alert" onclick="cerrarAlerta(this.parentElement)">&times;</button>
    `;
    contenedor.appendChild(alertDiv);
    setTimeout(() => cerrarAlerta(alertDiv), 6000);
}

function cerrarAlerta(element) {
    if (element && element.parentNode) {
        element.parentNode.removeChild(element);
    }
}

function formatearFecha(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('es-ES');
}

function crearBadgeEstado(estado) {
    let clase = 'badge';
    let texto = (estado || '').replace('_', ' ');
    switch (estado) {
        case 'PENDIENTE':
            clase += ' badge-pending';
            texto = 'PENDIENTE';
            break;
        case 'EN_PROCESO':
            clase += ' badge-process';
            texto = 'EN PROCESO';
            break;
        case 'FINALIZADA':
            clase += ' badge-finished';
            texto = 'FINALIZADA · PEND. ADMIN';
            break;
        case 'CONFIRMADA':
            clase += ' badge-confirmed';
            texto = 'CONFIRMADA ✓';
            break;
        case 'ENTREGADA':
            clase += ' badge-delivered';
            texto = 'ENTREGADA';
            break;
    }
    return `<span class="${clase}">${texto}</span>`;
}

function toggleLoading(button, isLoading) {
    if (!button) return;
    if (isLoading) {
        button.disabled = true;
        button.classList.add('btn-loading');
        button.dataset.originalText = button.innerHTML;
        button.innerHTML = '<span class="spinner"></span> Cargando...';
    } else {
        button.disabled = false;
        button.classList.remove('btn-loading');
        button.innerHTML = button.dataset.originalText || button.innerHTML;
    }
}

let modalCallback = null;

function mostrarModal(titulo, mensaje, onConfirm) {
    let overlay = document.getElementById('modal-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'modal-overlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal card">
                <h3 id="modal-title"></h3>
                <p id="modal-message"></p>
                <div class="modal-actions">
                    <button class="btn btn-secondary" onclick="cerrarModal()">Cancelar</button>
                    <button class="btn btn-primary" id="modal-confirm">Confirmar</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    
    document.getElementById('modal-title').innerText = titulo;
    document.getElementById('modal-message').innerText = mensaje;
    
    modalCallback = onConfirm;
    document.getElementById('modal-confirm').onclick = () => {
        if (modalCallback) modalCallback();
        cerrarModal();
    };
    
    overlay.style.display = 'flex';
}

function cerrarModal() {
    const overlay = document.getElementById('modal-overlay');
    if (overlay) {
        overlay.style.display = 'none';
        modalCallback = null;
    }
}


// =====================================================
// CABECERA COMÚN DE LOS PANELES
// Rellena la barra superior (#user-info) y la tarjeta de bienvenida
// (#bienvenida-avatar, #bienvenida-nombre, #bienvenida-rol, #bienvenida-fecha)
// con los datos del usuario que ha iniciado sesión.
// =====================================================
const NOMBRES_ROL = {
    ADMIN: 'ADMINISTRADOR',
    RECEPCIONISTA: 'RECEPCIONISTA',
    MECANICO: 'MECÁNICO',
    CLIENTE: 'CLIENTE'
};

function obtenerIniciales(nombre) {
    return (nombre || '?')
        .split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map(p => p[0].toUpperCase())
        .join('');
}

function pintarCabeceraUsuario(user, rolForzado) {
    if (!user) return;
    // Los clientes no tienen campo "rol" en la sesión
    const rol = rolForzado || user.rol || 'CLIENTE';
    const rolTexto = NOMBRES_ROL[rol] || rol;

    const navInfo = document.getElementById('user-info');
    if (navInfo) {
        navInfo.innerHTML = `
            <span class="nav-user-name">${user.nombre || ''}</span>
            <span class="nav-user-rol rol-${rol}">${rolTexto}</span>
        `;
    }

    const set = (id, valor) => {
        const el = document.getElementById(id);
        if (el) el.textContent = valor;
    };
    set('bienvenida-avatar', obtenerIniciales(user.nombre));
    set('bienvenida-nombre', user.nombre || '');
    set('bienvenida-fecha', new Date().toLocaleDateString('es-ES', {
        weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
    }));

    const rolEl = document.getElementById('bienvenida-rol');
    if (rolEl) {
        rolEl.textContent = rolTexto;
        rolEl.className = `bienvenida-rol rol-${rol}`;
    }
}


// =====================================================
// GESTIÓN ECONÓMICA Y PRESUPUESTOS DEL TALLER
// Sincroniza:
//  - presupuesto (precio cobrado al cliente, ej. 200 €)
//  - presupuestoEstimado (lo pensado por recepción para arreglarlo, ej. 100 €)
//  - presupuestoMecanico (límite real decidido por el Admin para el mecánico, ej. 100 €, 150 € o 75 €)
//  - confirmadasPorAdmin (paso intermedio FINALIZADA -> CONFIRMADA -> ENTREGADA)
// =====================================================
const FONDO_INICIAL_TALLER = 2000.00;
const FINANZAS_STORAGE_KEY = 'motorflow_finanzas_v2';

function obtenerEstadoFinanzasLocal() {
    try {
        const raw = localStorage.getItem(FINANZAS_STORAGE_KEY);
        if (raw) return JSON.parse(raw);
    } catch (e) {}
    const inicial = {
        presupuestos: { 1: 250.00, 2: 180.00, 3: 150.00 },
        presupuestosEstimados: { 1: 120.00, 2: 90.00, 3: 75.00 },
        presupuestosMecanico: { 1: 120.00, 2: 80.00, 3: 60.00 },
        confirmadasPorAdmin: {},
        usosPorReparacion: {
            2: [{ nombre: 'Pastillas de freno', cantidad: 1, precioUnitario: 35.00 }],
            3: [
                { nombre: 'Filtro de aceite', cantidad: 1, precioUnitario: 8.50 },
                { nombre: 'Aceite motor 5W30', cantidad: 1, precioUnitario: 28.00 }
            ]
        },
        gastosComprasStock: 0
    };
    localStorage.setItem(FINANZAS_STORAGE_KEY, JSON.stringify(inicial));
    return inicial;
}

function guardarEstadoFinanzasLocal(estado) {
    try {
        localStorage.setItem(FINANZAS_STORAGE_KEY, JSON.stringify(estado));
    } catch (e) {}
}

function guardarPresupuestoCreacionLocal(reparacionId, precioCliente, presupuestoEstimado) {
    const est = obtenerEstadoFinanzasLocal();
    const pCli = Number(precioCliente) || 200.0;
    const pEst = Number(presupuestoEstimado) || Math.round(pCli * 50) / 100;
    est.presupuestos[reparacionId] = pCli;
    if (!est.presupuestosEstimados) est.presupuestosEstimados = {};
    if (!est.presupuestosMecanico) est.presupuestosMecanico = {};
    est.presupuestosEstimados[reparacionId] = pEst;
    est.presupuestosMecanico[reparacionId] = pEst;
    guardarEstadoFinanzasLocal(est);
}

function guardarPresupuestoMecanicoLocal(reparacionId, presupuestoMecanico) {
    const est = obtenerEstadoFinanzasLocal();
    if (!est.presupuestosMecanico) est.presupuestosMecanico = {};
    est.presupuestosMecanico[reparacionId] = Number(presupuestoMecanico);
    guardarEstadoFinanzasLocal(est);
}

function guardarPresupuestoLocal(reparacionId, presupuestoMecanico) {
    guardarPresupuestoMecanicoLocal(reparacionId, presupuestoMecanico);
}

function marcarConfirmadaPorAdminLocal(reparacionId, confirmada = true) {
    const est = obtenerEstadoFinanzasLocal();
    if (!est.confirmadasPorAdmin) est.confirmadasPorAdmin = {};
    if (confirmada) {
        est.confirmadasPorAdmin[reparacionId] = true;
    } else {
        delete est.confirmadasPorAdmin[reparacionId];
    }
    guardarEstadoFinanzasLocal(est);
}

function registrarUsoMaterialLocal(reparacionId, nombreMaterial, cantidad, precioUnitario) {
    const est = obtenerEstadoFinanzasLocal();
    if (!est.usosPorReparacion[reparacionId]) {
        est.usosPorReparacion[reparacionId] = [];
    }
    est.usosPorReparacion[reparacionId].push({
        nombre: nombreMaterial,
        cantidad: Number(cantidad),
        precioUnitario: Number(precioUnitario)
    });
    guardarEstadoFinanzasLocal(est);
}

function enriquecerReparacionFinanzas(r) {
    if (!r) return r;
    const est = obtenerEstadoFinanzasLocal();
    const tieneBackendNuevo = typeof r.presupuestoMecanico === 'number' && typeof r.costeMateriales === 'number';
    const tieneBackendPrevio = typeof r.presupuesto === 'number' && typeof r.costeMateriales === 'number';

    const presupuesto = (typeof r.presupuesto === 'number')
        ? r.presupuesto
        : (est.presupuestos?.[r.id] ?? 200.00);

    const presupuestoEstimado = tieneBackendNuevo
        ? r.presupuestoEstimado
        : (est.presupuestosEstimados?.[r.id] ?? Math.round(presupuesto * 50) / 100);

    const presupuestoMecanico = tieneBackendNuevo
        ? r.presupuestoMecanico
        : (est.presupuestosMecanico?.[r.id] ?? presupuestoEstimado);

    let costeMateriales = 0;
    let materialesDetalle = [];

    if (tieneBackendNuevo || tieneBackendPrevio) {
        costeMateriales = r.costeMateriales;
        materialesDetalle = Array.isArray(r.materialesDetalle) ? r.materialesDetalle : [];
    } else {
        const usos = est.usosPorReparacion?.[r.id] || [];
        costeMateriales = usos.reduce((acc, u) => acc + (u.cantidad * u.precioUnitario), 0);
        materialesDetalle = usos.map(u => `${u.cantidad}x ${u.nombre} (${(u.cantidad * u.precioUnitario).toFixed(2)} €)`);
    }

    let estadoEfectivo = r.estado;
    if (estadoEfectivo === 'FINALIZADA' && est.confirmadasPorAdmin?.[r.id]) {
        estadoEfectivo = 'CONFIRMADA';
    }

    const presupuestoRestante = Math.max(0, presupuestoMecanico - costeMateriales);
    const beneficio = presupuesto - costeMateriales;

    return {
        ...r,
        estado: estadoEfectivo,
        presupuesto,
        presupuestoEstimado,
        presupuestoMecanico,
        costeMateriales,
        presupuestoRestante,
        beneficio,
        materialesDetalle
    };
}

function registrarCompraStockLocal(importeCompra) {
    const est = obtenerEstadoFinanzasLocal();
    est.gastosComprasStock = Number(est.gastosComprasStock || 0) + Number(importeCompra || 0);
    guardarEstadoFinanzasLocal(est);
}

function calcularCuentaTallerDesdeReparaciones(todasReparaciones = []) {
    const est = obtenerEstadoFinanzasLocal();
    const enriquecidas = (todasReparaciones || []).map(enriquecerReparacionFinanzas);
    const gastosPiezasUsadas = enriquecidas.reduce((acc, r) => acc + (r.costeMateriales || 0), 0);
    const gastosComprasStock = Number(est.gastosComprasStock || 0);
    const gastosMateriales = gastosPiezasUsadas + gastosComprasStock;
    const ingresosCobrados = enriquecidas
        .filter(r => r.estado === 'ENTREGADA')
        .reduce((acc, r) => acc + (r.presupuesto || 0), 0);
    const ingresosEnCurso = enriquecidas
        .filter(r => r.estado !== 'ENTREGADA')
        .reduce((acc, r) => acc + (r.presupuesto || 0), 0);
    const beneficioTotal = enriquecidas.reduce((acc, r) => acc + (r.beneficio || 0), 0);
    const saldoActual = FONDO_INICIAL_TALLER - gastosMateriales + ingresosCobrados;

    return {
        fondoInicial: FONDO_INICIAL_TALLER,
        gastosPiezasUsadas,
        gastosComprasStock,
        gastosMateriales,
        ingresosCobrados,
        ingresosEnCurso,
        beneficioTotal,
        saldoActual
    };
}

function formatearEuros(valor) {
    const num = Number(valor) || 0;
    return num.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
}
