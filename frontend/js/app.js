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
    switch (estado) {
        case 'PENDIENTE': clase += ' badge-pending'; break;
        case 'EN_PROCESO': clase += ' badge-process'; break;
        case 'FINALIZADA': clase += ' badge-finished'; break;
        case 'ENTREGADA': clase += ' badge-delivered'; break;
    }
    return `<span class="${clase}">${estado.replace('_', ' ')}</span>`;
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
