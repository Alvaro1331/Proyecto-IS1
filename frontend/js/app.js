const API_BASE = 'http://localhost:8080/api';
const BACKEND_ORIGIN = 'http://localhost:8080';

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
        const isFormData = fetchOptions.body instanceof FormData;
        const defaultHeaders = isFormData ? {} : { 'Content-Type': 'application/json' };
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
    if (isNaN(date.getTime())) return dateString;
    return date.toLocaleDateString('es-ES');
}

function formatearFechaHora(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;
    return date.toLocaleString('es-ES', {
        day: '2-digit', month: '2-digit', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
    });
}

function resolverUrlFoto(rutaFoto) {
    if (!rutaFoto) return null;
    if (rutaFoto.startsWith('http://') || rutaFoto.startsWith('https://') || rutaFoto.startsWith('data:')) {
        return rutaFoto;
    }
    return `${BACKEND_ORIGIN}${rutaFoto.startsWith('/') ? '' : '/'}${rutaFoto}`;
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
// GESTIÓN ECONÓMICA, LÍNEA TEMPORAL Y FACTURACIÓN
// =====================================================
const FONDO_INICIAL_TALLER = 2000.00;
const FINANZAS_STORAGE_KEY = 'motorflow_finanzas_v3';

function obtenerEstadoFinanzasLocal() {
    try {
        const raw = localStorage.getItem(FINANZAS_STORAGE_KEY);
        if (raw) return JSON.parse(raw);
    } catch (e) {}
    const inicial = {
        presupuestos: {},
        confirmadasPorAdmin: {},
        usosPorReparacion: {},
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

function guardarPresupuestoCreacionLocal(reparacionId, estimacionInicial) {
    const est = obtenerEstadoFinanzasLocal();
    est.presupuestos[reparacionId] = Number(estimacionInicial) || 150.0;
    guardarEstadoFinanzasLocal(est);
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

function registrarCompraStockLocal(importeCompra) {
    const est = obtenerEstadoFinanzasLocal();
    est.gastosComprasStock = Number(est.gastosComprasStock || 0) + Number(importeCompra || 0);
    guardarEstadoFinanzasLocal(est);
}

function enriquecerReparacionFinanzas(r) {
    if (!r) return r;
    const estimacionInicial = Number(r.estimacionInicial ?? r.presupuesto ?? 150.0);
    const costeMateriales = Number(r.costeMateriales ?? 0.0);
    const horasTrabajoMecanico = Number(r.horasTrabajoMecanico ?? 0.0);
    const precioHoraManoObra = Number(r.precioHoraManoObra ?? 35.0);
    const subtotalManoObra = Number(r.subtotalManoObra ?? (horasTrabajoMecanico * precioHoraManoObra));
    const porcentajeMargen = Number(r.porcentajeMargen ?? 15.0);
    const diasEstimados = Number(r.diasEstimados ?? 3);
    const diasRestantes = Number(r.diasRestantes ?? diasEstimados);
    const materialesDetalle = Array.isArray(r.materialesDetalle) ? r.materialesDetalle : [];
    const incidencias = Array.isArray(r.incidencias) ? r.incidencias : [];
    const impactoIncidencias = Number(r.impactoIncidencias ?? 0.0);
    const totalCalculadoPrevisto = Number(r.totalCalculadoPrevisto ?? (estimacionInicial + impactoIncidencias));
    const beneficio = Number(r.beneficio ?? (estimacionInicial - costeMateriales));

    return {
        ...r,
        presupuesto: estimacionInicial,
        estimacionInicial,
        costeMateriales,
        horasTrabajoMecanico,
        precioHoraManoObra,
        subtotalManoObra,
        porcentajeMargen,
        diasEstimados,
        diasRestantes,
        materialesDetalle,
        incidencias,
        impactoIncidencias,
        totalCalculadoPrevisto,
        beneficio
    };
}

function formatearEuros(valor) {
    const num = Number(valor) || 0;
    return num.toLocaleString('es-ES', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' €';
}

// =====================================================
// COMPONENTES VISUALES: LÍNEA TEMPORAL, INCIDENCIAS Y MODAL DE FACTURA
// =====================================================
const PASOS_REPARACION = [
    { codigo: 'PENDIENTE',   icono: '📋', titulo: 'Recibido en Recepción' },
    { codigo: 'EN_PROCESO',  icono: '🔧', titulo: 'En Reparación (Taller)' },
    { codigo: 'FINALIZADA',  icono: '🏁', titulo: 'Trabajo Terminado' },
    { codigo: 'CONFIRMADA',  icono: '✅', titulo: 'Verificado por Taller' },
    { codigo: 'ENTREGADA',   icono: '🚗', titulo: 'Vehículo Entregado' }
];

function renderizarLineaTemporalHTML(rep) {
    const estadoActual = rep.estado || 'PENDIENTE';
    const idxActual = PASOS_REPARACION.findIndex(p => p.codigo === estadoActual);
    const diasEstimados = rep.diasEstimados ?? 3;
    const diasRestantes = rep.diasRestantes ?? diasEstimados;
    const fechaEstimada = rep.fechaEstimadaFin ? formatearFecha(rep.fechaEstimadaFin) : 'Por determinar';

    let badgePlazo = '';
    if (estadoActual === 'ENTREGADA') {
        badgePlazo = `<span class="badge badge-confirmed">🎉 Vehículo ya entregado al cliente</span>`;
    } else if (estadoActual === 'CONFIRMADA' || estadoActual === 'FINALIZADA') {
        badgePlazo = `<span class="badge badge-confirmed">✨ Reparación finalizada · Listo para entrega</span>`;
    } else if (diasRestantes === 0) {
        badgePlazo = `<span class="badge badge-pending">⏰ Entrega prevista para hoy (${fechaEstimada})</span>`;
    } else {
        badgePlazo = `<span class="badge badge-process">⏳ Tiempo estimado: ${diasEstimados} día(s) · Faltan ~${diasRestantes} día(s) (Previsto: ${fechaEstimada})</span>`;
    }

    const pasosHtml = PASOS_REPARACION.map((paso, index) => {
        let estadoClase = '';
        if (index < idxActual || estadoActual === 'ENTREGADA') {
            estadoClase = 'completed';
        } else if (index === idxActual) {
            estadoClase = 'active';
        }
        const iconoMostrar = (index < idxActual || estadoActual === 'ENTREGADA') ? '✓' : paso.icono;
        return `
            <div class="timeline-step ${estadoClase}">
                <div class="timeline-step-circle">${iconoMostrar}</div>
                <div class="timeline-step-label">${paso.titulo}</div>
            </div>
        `;
    }).join('');

    return `
        <div class="timeline-wrapper">
            <div class="timeline-header-meta">
                <div style="font-weight: 600; font-size: 0.92rem; color: var(--text-light);">
                    📍 Seguimiento en Tiempo Real
                </div>
                <div>${badgePlazo}</div>
            </div>
            <div class="timeline-stepper">
                ${pasosHtml}
            </div>
        </div>
    `;
}

function renderizarIncidenciasHTML(incidencias = []) {
    if (!incidencias || incidencias.length === 0) {
        return `
            <div class="incidencias-section">
                <div style="font-size: 0.88rem; color: var(--text-muted);">
                    📝 <em>El mecánico no ha registrado incidencias ni variaciones adicionales por el momento.</em>
                </div>
            </div>
        `;
    }

    const itemsHtml = incidencias.map(inc => {
        const urlFoto = resolverUrlFoto(inc.rutaFoto);
        const impacto = Number(inc.impactoPrecio || 0);
        const badgeImpacto = impacto > 0
            ? `<span class="badge badge-pending" style="margin-left: 0.5rem;">Impacto orientativo: +${formatearEuros(impacto)}</span>`
            : `<span class="badge" style="background: rgba(255,255,255,0.08); color: var(--text-muted); margin-left: 0.5rem;">Nota informativa (0,00 €)</span>`;

        const fotoHtml = urlFoto
            ? `<a href="${urlFoto}" target="_blank" title="Pulsa para ampliar imagen">
                   <img src="${urlFoto}" alt="Foto de la incidencia" class="incidencia-foto-thumb">
               </a>`
            : '';

        return `
            <div class="incidencia-item">
                ${fotoHtml}
                <div style="flex: 1; min-width: 200px;">
                    <div style="display: flex; align-items: center; flex-wrap: wrap; gap: 0.35rem; margin-bottom: 0.35rem;">
                        <span style="font-size: 0.78rem; color: var(--text-muted);">🕒 ${formatearFechaHora(inc.fecha)}</span>
                        ${badgeImpacto}
                    </div>
                    <div style="font-size: 0.92rem; color: var(--text-light);">${inc.descripcion || ''}</div>
                </div>
            </div>
        `;
    }).join('');

    return `
        <div class="incidencias-section">
            <h4 style="font-size: 0.95rem; margin-bottom: 0.75rem; color: var(--primary-light);">
                📸 Historial de Incidencias y Fotos del Mecánico (${incidencias.length})
            </h4>
            ${itemsHtml}
        </div>
    `;
}

function renderizarFacturaDesglosadaHTML(f) {
    if (!f) return '';
    const piezas = (f.materialesDetalle && f.materialesDetalle.length > 0)
        ? f.materialesDetalle.map(p => `<li style="margin-left: 1.2rem; font-size: 0.85rem; color: var(--text-muted);">${p}</li>`).join('')
        : '<li style="margin-left: 1.2rem; font-size: 0.85rem; color: var(--text-muted);">Sin recambios adicionales</li>';

    return `
        <div class="factura-card-view">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 1rem; margin-bottom: 1rem;">
                <div>
                    <div style="font-size: 0.8rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px;">Factura Oficial Taller MotorFlow</div>
                    <h3 style="margin: 0.2rem 0; color: var(--primary); font-family: monospace; font-size: 1.4rem;">${f.numeroFactura}</h3>
                    <div style="font-size: 0.85rem; color: var(--text-muted);">Fecha de emisión: <strong>${formatearFecha(f.fechaEmision)}</strong> · Localizador: <strong>${f.localizador || '-'}</strong></div>
                </div>
                <div style="text-align: right; font-size: 0.86rem;">
                    <div><strong>Cliente:</strong> ${f.nombreCliente || '-'} (${f.telefonoCliente || '-'})</div>
                    <div><strong>Vehículo:</strong> ${f.marcaVehiculo || ''} ${f.modeloVehiculo || ''} (${f.matriculaVehiculo || '-'})</div>
                    <div><strong>Mecánico:</strong> ${f.nombreMecanico || '-'}</div>
                </div>
            </div>

            <div style="margin-bottom: 0.85rem; font-size: 0.88rem;">
                <strong>Trabajo realizado:</strong> <span style="color: var(--text-muted);">${f.descripcionAveria || '-'}</span>
            </div>

            <div class="factura-row">
                <div>
                    <strong>1. Coste de Materiales y Piezas Usadas</strong>
                    <ul style="margin-top: 0.25rem;">${piezas}</ul>
                </div>
                <strong>${formatearEuros(f.costeMateriales)}</strong>
            </div>

            <div class="factura-row">
                <span><strong>2. Mano de Obra</strong> (${Number(f.horasManoObra || 0).toFixed(2)} h × ${formatearEuros(f.precioHora)}/h)</span>
                <strong>${formatearEuros(f.subtotalManoObra)}</strong>
            </div>

            <div class="factura-row" style="color: var(--text-muted);">
                <span>Subtotal Base (Materiales + Mano de Obra)</span>
                <span>${formatearEuros(f.subtotalBase)}</span>
            </div>

            <div class="factura-row">
                <span><strong>3. Margen Operativo del Taller</strong> (${Number(f.porcentajeMargen || 0).toFixed(1)}%)</span>
                <strong>${formatearEuros(f.importeMargen)}</strong>
            </div>

            <div class="factura-row" style="background: rgba(255,255,255,0.03); padding: 0.5rem 0.4rem; font-weight: 600;">
                <span>Base Imponible</span>
                <span>${formatearEuros(f.baseImponible)}</span>
            </div>

            <div class="factura-row">
                <span><strong>4. IVA Aplicado</strong> (${Number(f.porcentajeIva || 21).toFixed(0)}%)</span>
                <strong>${formatearEuros(f.importeIva)}</strong>
            </div>

            <div class="factura-row-total">
                <span>TOTAL FACTURA A PAGAR</span>
                <span>${formatearEuros(f.totalFactura)}</span>
            </div>
        </div>
    `;
}

function abrirModalFacturaObjeto(factura) {
    let overlay = document.getElementById('modal-factura-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'modal-factura-overlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal card" style="max-width: 680px; width: 95%; max-height: 90vh; overflow-y: auto;">
                <div id="modal-factura-body"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="window.print()">🖨️ Imprimir / Guardar PDF</button>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('modal-factura-overlay').style.display='none'">Cerrar</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    document.getElementById('modal-factura-body').innerHTML = renderizarFacturaDesglosadaHTML(factura);
    overlay.style.display = 'flex';
}
