/* MotorFlow · selector de tema claro/oscuro.
   Se carga en <head> para aplicar el tema antes de pintar la página (sin parpadeo)
   e inserta el selector de tema en la barra de navegación superior. */
(function () {
    var KEY = 'motorflow-theme';
    var root = document.documentElement;

    function preferido() {
        try {
            var guardado = localStorage.getItem(KEY);
            if (guardado === 'light' || guardado === 'dark') return guardado;
        } catch (e) { /* almacenamiento no disponible */ }
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function aplicar(tema) {
        root.setAttribute('data-theme', tema);
        var btn = document.getElementById('theme-toggle');
        if (btn) {
            var etiqueta = tema === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
            btn.setAttribute('aria-label', etiqueta);
            btn.setAttribute('title', etiqueta);
        }
    }

    aplicar(preferido());

    document.addEventListener('DOMContentLoaded', function () {
        var btn = document.createElement('button');
        btn.id = 'theme-toggle';
        btn.type = 'button';
        btn.className = 'theme-toggle';
        btn.innerHTML =
            '<svg class="icon-moon" viewBox="0 0 24 24"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>' +
            '<svg class="icon-sun" viewBox="0 0 24 24"><circle cx="12" cy="12" r="4"/>' +
            '<path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>';
        btn.addEventListener('click', function () {
            var nuevo = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
            try { localStorage.setItem(KEY, nuevo); } catch (e) { /* ignorar */ }
            aplicar(nuevo);
        });

        var navUser = document.querySelector('.navbar-user, .nav-user');
        var navLinks = document.querySelector('.nav-links');
        if (navUser) {
            navUser.insertBefore(btn, navUser.firstChild);
        } else if (navLinks && navLinks.tagName === 'UL') {
            var li = document.createElement('li');
            li.style.display = 'flex';
            li.style.alignItems = 'center';
            li.appendChild(btn);
            navLinks.appendChild(li);
        } else if (navLinks) {
            navLinks.appendChild(btn);
        } else {
            btn.classList.add('theme-toggle-floating');
            document.body.appendChild(btn);
        }
        aplicar(root.getAttribute('data-theme'));
    });
})();
