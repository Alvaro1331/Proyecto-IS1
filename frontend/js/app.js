const API_BASE = 'http://localhost:8080/api';

function getUsuarioActual() {
    const userStr = sessionStorage.getItem('usuario');
    return userStr ? JSON.parse(userStr) : null;
}

function isLoggedIn() {
    return !!getUsuarioActual();
}

function logout() {
    sessionStorage.removeItem('usuario');
    window.location.href = '/pages/login.html';
}

function requireAuth(rolesPermitidos = []) {
    const user = getUsuarioActual();
    if (!user) {
        window.location.href = '/pages/login.html';
        return;
    }
    if (rolesPermitidos.length > 0 && !rolesPermitidos.includes(user.rol)) {
        window.location.href = '/pages/login.html';
    }
}

async function apiFetch(endpoint, options = {}) {
    try {
        const defaultHeaders = {
            'Content-Type': 'application/json'
        };
        const res = await fetch(`${API_BASE}${endpoint}`, {
            ...options,
            headers: {
                ...defaultHeaders,
                ...options.headers
            }
        });
        
        let data = null;
        if (res.headers.get('content-type')?.includes('application/json')) {
            data = await res.json();
        }

        if (!res.ok) {
            throw new Error(data?.message || `Error: ${res.status}`);
        }
        return data;
    } catch (error) {
        console.error('API Error:', error);
        mostrarAlerta(error.message, 'error');
        throw error;
    }
}

function mostrarAlerta(mensaje, tipo, contenedorId = 'alert-container') {
    let contenedor = document.getElementById(contenedorId);
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
    alertDiv.innerHTML = `
        <span>${mensaje}</span>
        <button type="button" class="close-alert" onclick="cerrarAlerta(this.parentElement)">&times;</button>
    `;
    contenedor.appendChild(alertDiv);
    setTimeout(() => cerrarAlerta(alertDiv), 5000);
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
