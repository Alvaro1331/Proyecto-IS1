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

    const dniHtml = f.dniCifCliente ? `<div><strong>DNI/CIF:</strong> ${f.dniCifCliente}</div>` : '';
    const dirHtml = f.direccionFiscalCliente ? `<div><strong>Dirección Fiscal:</strong> ${f.direccionFiscalCliente}</div>` : '';

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
                    ${dniHtml}
                    ${dirHtml}
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

let ultimaFacturaModal = null;

function abrirModalFacturaObjeto(factura) {
    ultimaFacturaModal = factura;
    let overlay = document.getElementById('modal-factura-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'modal-factura-overlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal card" style="max-width: 680px; width: 95%; max-height: 90vh; overflow-y: auto;">
                <div id="modal-factura-body"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="exportarFacturaActualPDF()">🖨️ Descargar / Imprimir PDF</button>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('modal-factura-overlay').style.display='none'">Cerrar</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    document.getElementById('modal-factura-body').innerHTML = renderizarFacturaDesglosadaHTML(factura);
    overlay.style.display = 'flex';
}

function exportarFacturaActualPDF() {
    if (!ultimaFacturaModal) {
        window.print();
        return;
    }
    const f = ultimaFacturaModal;
    const piezas = (f.materialesDetalle && f.materialesDetalle.length > 0)
        ? f.materialesDetalle.map(p => `<li>${p}</li>`).join('')
        : '<li>Sin recambios adicionales</li>';
    const win = window.open('', '_blank', 'width=850,height=900');
    if (!win) {
        window.print();
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <title>Factura ${f.numeroFactura} - MotorFlow</title>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; color: #1e293b; padding: 32px; max-width: 780px; margin: 0 auto; }
                .header { display: flex; justify-content: space-between; border-bottom: 3px solid #E85D04; padding-bottom: 16px; margin-bottom: 20px; }
                .title { color: #E85D04; font-size: 24px; font-weight: 800; margin: 4px 0; }
                .row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #cbd5e1; }
                .total { display: flex; justify-content: space-between; padding: 14px 0; border-top: 2px solid #E85D04; font-size: 20px; font-weight: 800; color: #E85D04; margin-top: 12px; }
                .box { background: #f8fafc; border: 1px solid #e2e8f0; padding: 12px; border-radius: 6px; margin-bottom: 16px; font-size: 14px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div>
                    <div style="font-size:12px; text-transform:uppercase; color:#64748b;">Taller Mecánico Oficial MotorFlow · CIF B-88990011</div>
                    <div class="title">FACTURA ${f.numeroFactura}</div>
                    <div>Fecha Emisión: <strong>${formatearFecha(f.fechaEmision)}</strong> · Localizador: <strong>${f.localizador || '-'}</strong></div>
                </div>
                <div style="text-align:right; font-size:14px;">
                    <div><strong>Cliente:</strong> ${f.nombreCliente || '-'}</div>
                    <div><strong>DNI/CIF:</strong> ${f.dniCifCliente || 'No especificado'}</div>
                    <div><strong>Dirección Fiscal:</strong> ${f.direccionFiscalCliente || 'No especificada'}</div>
                    <div><strong>Teléfono:</strong> ${f.telefonoCliente || '-'}</div>
                    <div><strong>Vehículo:</strong> ${f.marcaVehiculo || ''} ${f.modeloVehiculo || ''} (${f.matriculaVehiculo || '-'})</div>
                </div>
            </div>
            <div class="box">
                <strong>Concepto / Intervención realizada:</strong><br>${f.descripcionAveria || '-'}
            </div>
            <div class="row">
                <div><strong>1. Piezas y Materiales Empleados</strong><ul>${piezas}</ul></div>
                <strong>${formatearEuros(f.costeMateriales)}</strong>
            </div>
            <div class="row">
                <span><strong>2. Mano de Obra</strong> (${Number(f.horasManoObra || 0).toFixed(2)} h × ${formatearEuros(f.precioHora)}/h)</span>
                <strong>${formatearEuros(f.subtotalManoObra)}</strong>
            </div>
            <div class="row">
                <span><strong>3. Margen Operativo Taller</strong> (${Number(f.porcentajeMargen || 0).toFixed(1)}%)</span>
                <strong>${formatearEuros(f.importeMargen)}</strong>
            </div>
            <div class="row" style="background:#f1f5f9; padding:10px 8px; font-weight:700;">
                <span>Base Imponible</span>
                <span>${formatearEuros(f.baseImponible)}</span>
            </div>
            <div class="row">
                <span><strong>4. Cuota IVA</strong> (${Number(f.porcentajeIva || 21).toFixed(0)}%)</span>
                <strong>${formatearEuros(f.importeIva)}</strong>
            </div>
            <div class="total">
                <span>IMPORTE TOTAL A ABONAR</span>
                <span>${formatearEuros(f.totalFactura)}</span>
            </div>
            <script>window.onload = function() { window.print(); };</script>
        </body>
        </html>
    `);
    win.document.close();
}

// =====================================================
// PRESUPUESTO DESGLOSADO (HISTORIAS DE USUARIO 1 Y 3)
// =====================================================
function renderizarBadgePresupuesto(estadoPresupuesto) {
    if (!estadoPresupuesto || estadoPresupuesto === 'SIN_PRESUPUESTO') {
        return `<span class="badge" style="background: rgba(255,255,255,0.08); color: var(--text-muted);">Sin presupuesto formal</span>`;
    }
    if (estadoPresupuesto === 'PENDIENTE_ACEPTACION') {
        return `<span class="badge badge-pending">📋 Presupuesto Pendiente Cliente</span>`;
    }
    if (estadoPresupuesto === 'ACEPTADO') {
        return `<span class="badge badge-confirmed">✅ Presupuesto Aceptado</span>`;
    }
    if (estadoPresupuesto === 'RECHAZADO') {
        return `<span class="badge" style="background: rgba(220,38,38,0.2); color: #FCA5A5; border: 1px solid var(--accent);">❌ Presupuesto Rechazado</span>`;
    }
    return `<span class="badge">${estadoPresupuesto}</span>`;
}

function renderizarPresupuestoDesglosadoHTML(p) {
    if (!p) return '';
    const lineas = Array.isArray(p.lineas) ? p.lineas : [];
    const filasPiezas = lineas.filter(l => l.tipo === 'PIEZA').map(l => `
        <tr>
            <td><span class="badge badge-process" style="font-size: 0.7rem;">Pieza/Repuesto</span></td>
            <td>${l.concepto}</td>
            <td style="text-align: center;">${Number(l.cantidad).toFixed(2)} ud.</td>
            <td style="text-align: right;">${formatearEuros(l.precioUnitario)}</td>
            <td style="text-align: right; font-weight: 600;">${formatearEuros(l.subtotal)}</td>
        </tr>
    `).join('');

    const filasManoObra = lineas.filter(l => l.tipo === 'MANO_OBRA').map(l => `
        <tr>
            <td><span class="badge badge-pending" style="font-size: 0.7rem;">Mano de obra</span></td>
            <td>${l.concepto}</td>
            <td style="text-align: center;">${Number(l.cantidad).toFixed(2)} h</td>
            <td style="text-align: right;">${formatearEuros(l.precioUnitario)}/h</td>
            <td style="text-align: right; font-weight: 600;">${formatearEuros(l.subtotal)}</td>
        </tr>
    `).join('');

    const motivoHtml = (p.estado === 'RECHAZADO' && p.motivoRechazo)
        ? `<div class="alert alert-error" style="margin-top: 0.75rem;"><span><strong>Motivo del rechazo del cliente:</strong> ${p.motivoRechazo}</span></div>`
        : '';

    return `
        <div class="factura-card-view" style="border-top-color: var(--info);">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 1rem; border-bottom: 1px solid var(--border); padding-bottom: 1rem; margin-bottom: 1rem;">
                <div>
                    <div style="font-size: 0.78rem; color: var(--text-muted); text-transform: uppercase; letter-spacing: 1px;">Presupuesto Previo Desglosado · MotorFlow</div>
                    <h3 style="margin: 0.2rem 0; color: var(--info); font-family: monospace; font-size: 1.35rem;">${p.numeroPresupuesto}</h3>
                    <div style="font-size: 0.84rem; color: var(--text-muted);">
                        Emisión: <strong>${formatearFecha(p.fechaEmision)}</strong> · Válido hasta: <strong>${formatearFecha(p.fechaValidez)}</strong>
                    </div>
                </div>
                <div style="text-align: right; font-size: 0.85rem;">
                    <div style="margin-bottom: 0.35rem;">${renderizarBadgePresupuesto(p.estado)}</div>
                    <div><strong>Cliente:</strong> ${p.nombreCliente || '-'} (${p.telefonoCliente || '-'})</div>
                    <div><strong>Vehículo:</strong> ${p.marcaVehiculo || ''} ${p.modeloVehiculo || ''} (${p.matriculaVehiculo || '-'})</div>
                </div>
            </div>

            <div style="margin-bottom: 0.9rem; padding: 0.65rem 0.85rem; background: rgba(255,255,255,0.03); border-radius: 6px; border: 1px solid var(--border); font-size: 0.88rem;">
                <strong>Diagnóstico Técnico Previo:</strong>
                <div style="color: var(--text-muted); margin-top: 0.2rem;">${p.diagnosticoPrevio || '-'}</div>
            </div>

            <div class="table-responsive" style="margin-bottom: 1rem;">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Tipo</th>
                            <th>Concepto</th>
                            <th style="text-align: center;">Cant. / Horas</th>
                            <th style="text-align: right;">P. Unitario</th>
                            <th style="text-align: right;">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${filasPiezas}
                        ${filasManoObra}
                    </tbody>
                </table>
            </div>

            <div class="factura-row">
                <span>Subtotal Piezas / Repuestos</span>
                <strong>${formatearEuros(p.subtotalPiezas)}</strong>
            </div>
            <div class="factura-row">
                <span>Subtotal Mano de Obra</span>
                <strong>${formatearEuros(p.subtotalManoObra)}</strong>
            </div>
            <div class="factura-row">
                <span>Margen Operativo Taller (${Number(p.porcentajeMargen || 0).toFixed(1)}%)</span>
                <strong>${formatearEuros(p.importeMargen)}</strong>
            </div>
            <div class="factura-row" style="background: rgba(255,255,255,0.03); padding: 0.45rem 0.4rem; font-weight: 600;">
                <span>Base Imponible</span>
                <span>${formatearEuros(p.baseImponible)}</span>
            </div>
            <div class="factura-row">
                <span>IVA Aplicado (${Number(p.porcentajeIva || 21).toFixed(0)}%)</span>
                <strong>${formatearEuros(p.importeIva)}</strong>
            </div>
            <div class="factura-row-total" style="border-top-color: var(--info); color: #93C5FD;">
                <span>TOTAL PRESUPUESTO (CON IMPUESTOS)</span>
                <span>${formatearEuros(p.totalPresupuesto)}</span>
            </div>
            ${motivoHtml}
        </div>
    `;
}

let ultimoPresupuestoModal = null;

function abrirModalPresupuestoObjeto(presupuesto) {
    ultimoPresupuestoModal = presupuesto;
    let overlay = document.getElementById('modal-presupuesto-vista-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.id = 'modal-presupuesto-vista-overlay';
        overlay.className = 'modal-overlay';
        overlay.innerHTML = `
            <div class="modal card" style="max-width: 720px; width: 95%; max-height: 90vh; overflow-y: auto;">
                <div id="modal-presupuesto-vista-body"></div>
                <div class="modal-actions">
                    <button type="button" class="btn btn-secondary" onclick="exportarPresupuestoActualPDF()">🖨️ Exportar / Imprimir PDF</button>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('modal-presupuesto-vista-overlay').style.display='none'">Cerrar</button>
                </div>
            </div>
        `;
        document.body.appendChild(overlay);
    }
    document.getElementById('modal-presupuesto-vista-body').innerHTML = renderizarPresupuestoDesglosadoHTML(presupuesto);
    overlay.style.display = 'flex';
}

function exportarPresupuestoActualPDF() {
    if (!ultimoPresupuestoModal) {
        window.print();
        return;
    }
    const p = ultimoPresupuestoModal;
    const filas = (p.lineas || []).map(l => `
        <tr>
            <td style="padding:8px; border-bottom:1px solid #cbd5e1;">${l.tipo === 'MANO_OBRA' ? 'Mano de obra' : 'Pieza/Repuesto'}</td>
            <td style="padding:8px; border-bottom:1px solid #cbd5e1;">${l.concepto}</td>
            <td style="padding:8px; border-bottom:1px solid #cbd5e1; text-align:center;">${Number(l.cantidad).toFixed(2)} ${l.tipo === 'MANO_OBRA' ? 'h' : 'ud.'}</td>
            <td style="padding:8px; border-bottom:1px solid #cbd5e1; text-align:right;">${formatearEuros(l.precioUnitario)}</td>
            <td style="padding:8px; border-bottom:1px solid #cbd5e1; text-align:right; font-weight:700;">${formatearEuros(l.subtotal)}</td>
        </tr>
    `).join('');

    const win = window.open('', '_blank', 'width=850,height=900');
    if (!win) {
        window.print();
        return;
    }
    win.document.write(`
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <title>Presupuesto ${p.numeroPresupuesto} - MotorFlow</title>
            <style>
                body { font-family: 'Segoe UI', Arial, sans-serif; color: #1e293b; padding: 32px; max-width: 780px; margin: 0 auto; }
                .header { display: flex; justify-content: space-between; border-bottom: 3px solid #2563EB; padding-bottom: 16px; margin-bottom: 20px; }
                .title { color: #2563EB; font-size: 22px; font-weight: 800; margin: 4px 0; }
                table { width: 100%; border-collapse: collapse; margin: 16px 0; font-size: 14px; }
                th { background: #f1f5f9; padding: 10px 8px; text-align: left; border-bottom: 2px solid #94a3b8; }
                .row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #e2e8f0; }
                .total { display: flex; justify-content: space-between; padding: 14px 0; border-top: 2px solid #2563EB; font-size: 20px; font-weight: 800; color: #2563EB; margin-top: 12px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div>
                    <div style="font-size:12px; text-transform:uppercase; color:#64748b;">Taller Mecánico MotorFlow</div>
                    <div class="title">PRESUPUESTO ${p.numeroPresupuesto}</div>
                    <div>Emisión: <strong>${formatearFecha(p.fechaEmision)}</strong> · Válido hasta: <strong>${formatearFecha(p.fechaValidez)}</strong></div>
                    <div>Estado: <strong>${p.estado}</strong></div>
                </div>
                <div style="text-align:right; font-size:14px;">
                    <div><strong>Cliente:</strong> ${p.nombreCliente || '-'} (${p.telefonoCliente || '-'})</div>
                    <div><strong>Vehículo:</strong> ${p.marcaVehiculo || ''} ${p.modeloVehiculo || ''} (${p.matriculaVehiculo || '-'})</div>
                    <div><strong>Localizador:</strong> ${p.localizador || '-'}</div>
                </div>
            </div>
            <div style="background:#f8fafc; border:1px solid #cbd5e1; padding:12px; border-radius:6px; margin-bottom:16px;">
                <strong>Diagnóstico Técnico Previo:</strong><br>${p.diagnosticoPrevio || '-'}
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Tipo</th>
                        <th>Concepto</th>
                        <th style="text-align:center;">Cant. / Horas</th>
                        <th style="text-align:right;">P. Unitario</th>
                        <th style="text-align:right;">Subtotal</th>
                    </tr>
                </thead>
                <tbody>${filas}</tbody>
            </table>
            <div class="row"><span>Subtotal Piezas / Repuestos</span><strong>${formatearEuros(p.subtotalPiezas)}</strong></div>
            <div class="row"><span>Subtotal Mano de Obra</span><strong>${formatearEuros(p.subtotalManoObra)}</strong></div>
            <div class="row"><span>Margen Operativo Taller (${Number(p.porcentajeMargen || 0).toFixed(1)}%)</span><strong>${formatearEuros(p.importeMargen)}</strong></div>
            <div class="row" style="font-weight:700;"><span>Base Imponible</span><span>${formatearEuros(p.baseImponible)}</span></div>
            <div class="row"><span>IVA (${Number(p.porcentajeIva || 21).toFixed(0)}%)</span><strong>${formatearEuros(p.importeIva)}</strong></div>
            <div class="total"><span>TOTAL PRESUPUESTO CON IMPUESTOS</span><span>${formatearEuros(p.totalPresupuesto)}</span></div>
            <script>window.onload = function() { window.print(); };</script>
        </body>
        </html>
    `);
    win.document.close();
}

// =====================================================
// MODAL GENERADOR DE PRESUPUESTO DESGLOSADO (HISTORIA DE USUARIO 1)
// =====================================================
let estadoEditorPresupuesto = {
    reparacion: null,
    lineas: [],
    catalogoMateriales: [],
    precioHoraTaller: 35.0,
    porcentajeMargenTaller: 15.0,
    onSavedCallback: null
};

function asegurarModalEditorPresupuestoDOM() {
    let overlay = document.getElementById('modal-editor-presupuesto-overlay');
    if (overlay) return overlay;

    overlay = document.createElement('div');
    overlay.id = 'modal-editor-presupuesto-overlay';
    overlay.className = 'modal-overlay';
    overlay.innerHTML = `
        <div class="modal card" style="max-width: 820px; width: 96%; max-height: 92vh; overflow-y: auto; border-top: 4px solid var(--info);">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; margin-bottom: 0.75rem;">
                <div>
                    <h3 id="editor-pres-titulo" style="margin: 0; color: var(--primary-light);">📋 Generar Presupuesto Desglosado tras Diagnóstico</h3>
                    <p id="editor-pres-subtitulo" style="margin: 0.2rem 0 0 0; font-size: 0.85rem; color: var(--text-muted);"></p>
                </div>
                <button type="button" class="btn btn-secondary" style="padding: 0.25rem 0.6rem; font-size: 0.8rem;" onclick="cerrarModalEditorPresupuesto()">✕</button>
            </div>

            <div id="editor-pres-alert"></div>

            <div style="display: grid; grid-template-columns: 3fr 1fr; gap: 0.85rem; margin-bottom: 1rem;">
                <div class="form-group" style="margin-bottom: 0;">
                    <label style="font-size: 0.84rem;">Diagnóstico Técnico Previo</label>
                    <textarea id="editor-pres-diagnostico" rows="2" placeholder="Describe el resultado del diagnóstico previo que justifica las piezas y mano de obra..."></textarea>
                </div>
                <div class="form-group" style="margin-bottom: 0;">
                    <label style="font-size: 0.84rem;">Validez (días)</label>
                    <input type="number" id="editor-pres-validez" min="1" max="90" value="15">
                </div>
            </div>

            <!-- Formulario para añadir nueva línea de coste -->
            <div style="background: rgba(0,0,0,0.3); border: 1px solid var(--border); border-radius: 8px; padding: 0.9rem; margin-bottom: 1rem;">
                <div style="font-weight: 600; font-size: 0.88rem; margin-bottom: 0.65rem; color: var(--text-light);">
                    ➕ Añadir Línea de Coste al Presupuesto
                </div>
                <div style="display: grid; grid-template-columns: 1.2fr 1.6fr 2fr 0.9fr 1fr auto; gap: 0.55rem; align-items: end;">
                    <div class="form-group" style="margin-bottom: 0;">
                        <label style="font-size: 0.76rem;">Tipo de Línea</label>
                        <select id="linea-pres-tipo" onchange="onCambioTipoLineaPresupuesto()">
                            <option value="PIEZA">🔧 Pieza / Repuesto</option>
                            <option value="MANO_OBRA">⏱️ Mano de obra</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin-bottom: 0;" id="wrap-linea-catalogo">
                        <label style="font-size: 0.76rem;">Catálogo Almacén (Opcional)</label>
                        <select id="linea-pres-material" onchange="onSeleccionarMaterialPresupuesto()">
                            <option value="">-- Escribir manual --</option>
                        </select>
                    </div>
                    <div class="form-group" style="margin-bottom: 0;">
                        <label style="font-size: 0.76rem;" id="lbl-linea-concepto">Concepto / Descripción</label>
                        <input type="text" id="linea-pres-concepto" placeholder="Ej. Kit correa distribución">
                    </div>
                    <div class="form-group" style="margin-bottom: 0;">
                        <label style="font-size: 0.76rem;" id="lbl-linea-cantidad">Cantidad (ud.)</label>
                        <input type="number" id="linea-pres-cantidad" min="0.1" step="0.5" value="1">
                    </div>
                    <div class="form-group" style="margin-bottom: 0;">
                        <label style="font-size: 0.76rem;" id="lbl-linea-precio">Precio Unit. (€)</label>
                        <input type="number" id="linea-pres-precio" min="0.01" step="0.50" value="25.00">
                    </div>
                    <div>
                        <button type="button" class="btn btn-primary" style="padding: 0.68rem 0.85rem; font-size: 0.84rem;" onclick="agregarLineaEditorPresupuesto()">
                            + Añadir
                        </button>
                    </div>
                </div>
            </div>

            <!-- Tabla de líneas añadidas -->
            <div class="table-responsive" style="margin-bottom: 1rem; max-height: 220px; overflow-y: auto; border: 1px solid var(--border); border-radius: 6px;">
                <table class="table" style="margin-bottom: 0;">
                    <thead>
                        <tr>
                            <th>Tipo</th>
                            <th>Concepto</th>
                            <th style="text-align: center;">Cant. / Horas</th>
                            <th style="text-align: right;">Precio Unit.</th>
                            <th style="text-align: right;">Subtotal</th>
                            <th style="text-align: center;">Quitar</th>
                        </tr>
                    </thead>
                    <tbody id="tabla-lineas-editor-presupuesto"></tbody>
                </table>
            </div>

            <!-- Resumen automático de importes -->
            <div style="background: rgba(0,0,0,0.28); border: 1px solid var(--border); border-radius: 8px; padding: 0.9rem 1.1rem;">
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(145px, 1fr)); gap: 0.75rem; font-size: 0.86rem;">
                    <div>
                        <div style="color: var(--text-muted); font-size: 0.76rem;">Subtotal Piezas</div>
                        <strong id="res-pres-piezas" style="font-size: 1.05rem;">0,00 €</strong>
                    </div>
                    <div>
                        <div style="color: var(--text-muted); font-size: 0.76rem;">Subtotal Mano de Obra</div>
                        <strong id="res-pres-mo" style="font-size: 1.05rem;">0,00 €</strong>
                    </div>
                    <div>
                        <div style="color: var(--text-muted); font-size: 0.76rem;">Margen Taller (<span id="res-pres-margen-pct">15</span>%)</div>
                        <strong id="res-pres-margen" style="font-size: 1.05rem; color: var(--primary-light);">0,00 €</strong>
                    </div>
                    <div>
                        <div style="color: var(--text-muted); font-size: 0.76rem;">Base Imponible + IVA (21%)</div>
                        <strong id="res-pres-iva" style="font-size: 0.95rem;">0,00 € + 0,00 €</strong>
                    </div>
                    <div style="border-left: 2px solid var(--info); padding-left: 0.75rem;">
                        <div style="color: #93C5FD; font-size: 0.76rem; font-weight: 700;">TOTAL CON IMPUESTOS</div>
                        <strong id="res-pres-total" style="font-size: 1.25rem; color: var(--success);">0,00 €</strong>
                    </div>
                </div>
            </div>

            <div class="modal-actions" style="margin-top: 1.1rem; flex-wrap: wrap;">
                <button type="button" class="btn btn-secondary" onclick="cerrarModalEditorPresupuesto()">Cancelar</button>
                <button type="button" class="btn btn-secondary" id="btn-previsualizar-presupuesto" style="display: none;" onclick="previsualizarPresupuestoGuardado()">
                    🖨️ Previsualizar / Exportar PDF
                </button>
                <button type="button" class="btn btn-primary" id="btn-confirmar-guardar-presupuesto" onclick="confirmarYGuardarPresupuesto(this)">
                    💾 Confirmar y Enviar al Cliente (PENDIENTE_ACEPTACION)
                </button>
            </div>
        </div>
    `;
    document.body.appendChild(overlay);
    return overlay;
}

async function abrirModalGenerarPresupuesto(rep, callbackRecargar) {
    const overlay = asegurarModalEditorPresupuestoDOM();
    estadoEditorPresupuesto.reparacion = rep;
    estadoEditorPresupuesto.onSavedCallback = callbackRecargar || null;
    document.getElementById('editor-pres-alert').innerHTML = '';

    // Cargar configuración y catálogo
    try {
        const [cfg, mats] = await Promise.all([
            apiFetch('/configuracion').catch(() => null),
            apiFetch('/materiales').catch(() => [])
        ]);
        if (cfg) {
            estadoEditorPresupuesto.precioHoraTaller = Number(cfg.precioHoraManoObra || 35);
            estadoEditorPresupuesto.porcentajeMargenTaller = Number(cfg.margenBeneficioPorcentaje || 15);
        }
        estadoEditorPresupuesto.catalogoMateriales = mats || [];
    } catch (e) {}

    document.getElementById('editor-pres-titulo').textContent = `📋 Presupuesto Desglosado · Reparación #${rep.id} (${rep.localizador || ''})`;
    document.getElementById('editor-pres-subtitulo').textContent = `Vehículo: ${rep.marcaVehiculo || ''} ${rep.modeloVehiculo || ''} (${rep.matriculaVehiculo || ''}) · Cliente: ${rep.nombreCliente || '-'}`;
    document.getElementById('res-pres-margen-pct').textContent = estadoEditorPresupuesto.porcentajeMargenTaller;

    // Poblar selector de materiales
    const selMat = document.getElementById('linea-pres-material');
    selMat.innerHTML = '<option value="">-- Escribir repuesto manual --</option>' +
        estadoEditorPresupuesto.catalogoMateriales.map(m =>
            `<option value="${m.id}" data-nombre="${m.nombre}" data-precio="${m.precioUnitario}">${m.nombre} (${formatearEuros(m.precioUnitario)})</option>`
        ).join('');

    // Si ya tenía presupuesto formal emitido, precargar sus líneas; si no, precargar diagnóstico
    const presExistente = rep.presupuestoFormal;
    if (presExistente && Array.isArray(presExistente.lineas) && presExistente.lineas.length > 0) {
        document.getElementById('editor-pres-diagnostico').value = presExistente.diagnosticoPrevio || rep.descripcionAveria || '';
        estadoEditorPresupuesto.lineas = presExistente.lineas.map(l => ({
            tipo: l.tipo,
            concepto: l.concepto,
            cantidad: Number(l.cantidad),
            precioUnitario: Number(l.precioUnitario),
            materialId: l.materialId || null
        }));
        ultimoPresupuestoModal = presExistente;
        document.getElementById('btn-previsualizar-presupuesto').style.display = 'inline-block';
    } else {
        document.getElementById('editor-pres-diagnostico').value = rep.descripcionAveria || '';
        estadoEditorPresupuesto.lineas = [];
        if (rep.horasTrabajoMecanico && rep.horasTrabajoMecanico > 0) {
            estadoEditorPresupuesto.lineas.push({
                tipo: 'MANO_OBRA',
                concepto: 'Mano de obra de diagnóstico e intervención mecánica',
                cantidad: Number(rep.horasTrabajoMecanico),
                precioUnitario: estadoEditorPresupuesto.precioHoraTaller,
                materialId: null
            });
        }
        document.getElementById('btn-previsualizar-presupuesto').style.display = 'none';
    }

    document.getElementById('linea-pres-tipo').value = 'PIEZA';
    onCambioTipoLineaPresupuesto();
    renderizarTablaEditorPresupuesto();
    overlay.style.display = 'flex';
}

function cerrarModalEditorPresupuesto() {
    const overlay = document.getElementById('modal-editor-presupuesto-overlay');
    if (overlay) overlay.style.display = 'none';
}

function onCambioTipoLineaPresupuesto() {
    const tipo = document.getElementById('linea-pres-tipo').value;
    const wrapCatalogo = document.getElementById('wrap-linea-catalogo');
    const lblCantidad = document.getElementById('lbl-linea-cantidad');
    const lblPrecio = document.getElementById('lbl-linea-precio');
    const inputConcepto = document.getElementById('linea-pres-concepto');
    const inputPrecio = document.getElementById('linea-pres-precio');
    const inputCant = document.getElementById('linea-pres-cantidad');

    if (tipo === 'MANO_OBRA') {
        wrapCatalogo.style.display = 'none';
        lblCantidad.textContent = 'Horas Estimadas (h)';
        lblPrecio.textContent = 'Tarifa Hora (€/h)';
        inputConcepto.placeholder = 'Ej. Desmontaje y sustitución de embrague';
        inputPrecio.value = estadoEditorPresupuesto.precioHoraTaller.toFixed(2);
        inputCant.step = '0.5';
        inputCant.value = '1.5';
    } else {
        wrapCatalogo.style.display = 'block';
        lblCantidad.textContent = 'Cantidad (ud.)';
        lblPrecio.textContent = 'Precio Unit. (€)';
        inputConcepto.placeholder = 'Ej. Pastillas de freno delanteras';
        inputCant.step = '1';
        inputCant.value = '1';
    }
}

function onSeleccionarMaterialPresupuesto() {
    const sel = document.getElementById('linea-pres-material');
    const opt = sel.options[sel.selectedIndex];
    if (opt && opt.value) {
        document.getElementById('linea-pres-concepto').value = opt.getAttribute('data-nombre') || '';
        document.getElementById('linea-pres-precio').value = Number(opt.getAttribute('data-precio') || 0).toFixed(2);
    }
}

function agregarLineaEditorPresupuesto() {
    const alertBox = document.getElementById('editor-pres-alert');
    alertBox.innerHTML = '';

    const tipo = document.getElementById('linea-pres-tipo').value;
    const selMat = document.getElementById('linea-pres-material');
    const materialId = (tipo === 'PIEZA' && selMat.value) ? Number(selMat.value) : null;
    const concepto = document.getElementById('linea-pres-concepto').value.trim();
    const cantidad = parseFloat(document.getElementById('linea-pres-cantidad').value);
    const precioUnitario = parseFloat(document.getElementById('linea-pres-precio').value);

    if (!concepto) {
        mostrarAlerta('Debes indicar el concepto o descripción de la línea.', 'error', alertBox);
        return;
    }
    if (isNaN(cantidad) || cantidad <= 0 || isNaN(precioUnitario) || precioUnitario <= 0) {
        mostrarAlerta('La cantidad/horas y el precio unitario deben ser mayores que cero (> 0).', 'error', alertBox);
        return;
    }

    estadoEditorPresupuesto.lineas.push({
        tipo,
        concepto,
        cantidad: Math.round(cantidad * 100) / 100,
        precioUnitario: Math.round(precioUnitario * 100) / 100,
        materialId
    });

    document.getElementById('linea-pres-concepto').value = '';
    document.getElementById('linea-pres-material').value = '';
    renderizarTablaEditorPresupuesto();
}

function eliminarLineaEditorPresupuesto(index) {
    estadoEditorPresupuesto.lineas.splice(index, 1);
    renderizarTablaEditorPresupuesto();
}

function actualizarValorLineaPresupuesto(index, campo, valor) {
    const num = parseFloat(valor);
    if (!estadoEditorPresupuesto.lineas[index]) return;
    estadoEditorPresupuesto.lineas[index][campo] = isNaN(num) ? 0 : Math.round(num * 100) / 100;
    renderizarTablaEditorPresupuesto();
}

function renderizarTablaEditorPresupuesto() {
    const tbody = document.getElementById('tabla-lineas-editor-presupuesto');
    const lineas = estadoEditorPresupuesto.lineas;

    if (!lineas || lineas.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center" style="color: var(--text-muted); padding: 1.2rem;">No hay conceptos en el presupuesto. Añade al menos una línea de Pieza/Repuesto o Mano de obra.</td></tr>`;
    } else {
        tbody.innerHTML = lineas.map((l, idx) => {
            const sub = Math.round(l.cantidad * l.precioUnitario * 100) / 100;
            const badge = l.tipo === 'MANO_OBRA'
                ? `<span class="badge badge-pending" style="font-size: 0.7rem;">Mano de obra</span>`
                : `<span class="badge badge-process" style="font-size: 0.7rem;">Pieza/Repuesto</span>`;
            const unidad = l.tipo === 'MANO_OBRA' ? 'h' : 'ud.';
            return `
                <tr>
                    <td>${badge}</td>
                    <td><strong>${l.concepto}</strong></td>
                    <td style="text-align: center; width: 115px;">
                        <input type="number" min="0.1" step="0.5" value="${l.cantidad}" style="padding: 0.25rem 0.4rem; width: 75px; text-align: center;"
                            onchange="actualizarValorLineaPresupuesto(${idx}, 'cantidad', this.value)"> ${unidad}
                    </td>
                    <td style="text-align: right; width: 125px;">
                        <input type="number" min="0.01" step="0.5" value="${l.precioUnitario}" style="padding: 0.25rem 0.4rem; width: 85px; text-align: right;"
                            onchange="actualizarValorLineaPresupuesto(${idx}, 'precioUnitario', this.value)"> €
                    </td>
                    <td style="text-align: right; font-weight: 700;">${formatearEuros(sub)}</td>
                    <td style="text-align: center;">
                        <button type="button" class="btn btn-danger" style="padding: 0.2rem 0.5rem; font-size: 0.75rem;" onclick="eliminarLineaEditorPresupuesto(${idx})">🗑️</button>
                    </td>
                </tr>
            `;
        }).join('');
    }

    let subPiezas = 0;
    let subMo = 0;
    lineas.forEach(l => {
        const sub = (Number(l.cantidad) || 0) * (Number(l.precioUnitario) || 0);
        if (l.tipo === 'MANO_OBRA') subMo += sub;
        else subPiezas += sub;
    });
    const subBase = subPiezas + subMo;
    const margen = subBase * (estadoEditorPresupuesto.porcentajeMargenTaller / 100);
    const baseImp = subBase + margen;
    const iva = baseImp * 0.21;
    const total = baseImp + iva;

    document.getElementById('res-pres-piezas').textContent = formatearEuros(subPiezas);
    document.getElementById('res-pres-mo').textContent = formatearEuros(subMo);
    document.getElementById('res-pres-margen').textContent = formatearEuros(margen);
    document.getElementById('res-pres-iva').textContent = `${formatearEuros(baseImp)} + ${formatearEuros(iva)}`;
    document.getElementById('res-pres-total').textContent = formatearEuros(total);
}

async function confirmarYGuardarPresupuesto(btn) {
    const alertBox = document.getElementById('editor-pres-alert');
    alertBox.innerHTML = '';
    const rep = estadoEditorPresupuesto.reparacion;
    const lineas = estadoEditorPresupuesto.lineas;

    if (!lineas || lineas.length === 0) {
        mostrarAlerta('No se permite guardar un presupuesto vacío: debes añadir al menos un concepto (Pieza/Repuesto o Mano de obra).', 'error', alertBox);
        return;
    }

    const lineaInvalida = lineas.find(l => !l.concepto || Number(l.cantidad) <= 0 || Number(l.precioUnitario) <= 0);
    if (lineaInvalida) {
        mostrarAlerta('No se permite guardar el presupuesto: todas las cantidades, horas e importes unitarios deben ser mayores que cero (> 0).', 'error', alertBox);
        return;
    }

    const diagnosticoPrevio = document.getElementById('editor-pres-diagnostico').value.trim();
    const diasValidez = parseInt(document.getElementById('editor-pres-validez').value, 10) || 15;

    toggleLoading(btn, true);
    try {
        const res = await apiFetch(`/reparaciones/${rep.id}/presupuesto-formal`, {
            method: 'POST',
            body: JSON.stringify({
                diagnosticoPrevio,
                diasValidez,
                lineas
            })
        });
        ultimoPresupuestoModal = res;
        document.getElementById('btn-previsualizar-presupuesto').style.display = 'inline-block';
        mostrarAlerta(res.mensaje || 'Presupuesto guardado y enviado al cliente (PENDIENTE_ACEPTACION).', 'success', alertBox);
        if (typeof estadoEditorPresupuesto.onSavedCallback === 'function') {
            await estadoEditorPresupuesto.onSavedCallback();
        }
    } catch (error) {
        mostrarAlerta(error.message, 'error', alertBox);
    } finally {
        toggleLoading(btn, false);
    }
}

function previsualizarPresupuestoGuardado() {
    if (ultimoPresupuestoModal) {
        abrirModalPresupuestoObjeto(ultimoPresupuestoModal);
    }
}
