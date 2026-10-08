(function () {
    'use strict';

    var STORAGE = 'jesmar_catalogo_cliente';
    var modalInstance = null;
    var onAlertCallback = null;

    function init(options) {
        onAlertCallback = options && options.onAlert ? options.onAlert : null;
        bindEventos();
        actualizarHeader();
        actualizarPanelModal();
    }

    function bindEventos() {
        var btn = document.getElementById('btnMiCuenta');
        if (btn) {
            btn.addEventListener('click', abrirModal);
        }

        var formLogin = document.getElementById('formLoginCliente');
        if (formLogin) {
            formLogin.addEventListener('submit', function (e) {
                e.preventDefault();
                login();
            });
        }

        var formReg = document.getElementById('formRegistroCliente');
        if (formReg) {
            formReg.addEventListener('submit', function (e) {
                e.preventDefault();
                registrar();
            });
        }

        var btnBuscar = document.getElementById('btnBuscarDniRegistro');
        if (btnBuscar) {
            btnBuscar.addEventListener('click', buscarDniRegistro);
        }

        var btnCerrar = document.getElementById('btnCerrarSesionCliente');
        if (btnCerrar) {
            btnCerrar.addEventListener('click', cerrarSesion);
        }

        var btnEditar = document.getElementById('btnEditarDatosCliente');
        if (btnEditar) {
            btnEditar.addEventListener('click', function () {
                mostrarAuth(true);
                irTabRegistro();
                var sesion = getSesion();
                if (sesion) {
                    var doc = document.getElementById('regDocumento');
                    var nom = document.getElementById('regNombre');
                    var tel = document.getElementById('regTelefono');
                    if (doc) doc.value = sesion.documento || '';
                    if (nom) nom.value = sesion.nombre || '';
                    if (tel) tel.value = sesion.telefono || '';
                }
            });
        }

        ['loginDocumento', 'regDocumento', 'regTelefono'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', function () {
                    el.value = el.value.replace(/\D/g, '');
                });
            }
        });
    }

    function getSesion() {
        try {
            var raw = localStorage.getItem(STORAGE);
            if (!raw) return null;
            var data = JSON.parse(raw);
            if (!data || !data.documento) return null;
            return data;
        } catch (e) {
            return null;
        }
    }

    function setSesion(data) {
        localStorage.setItem(STORAGE, JSON.stringify({
            documento: data.documento || '',
            nombre: data.nombre || '',
            telefono: data.telefono || '',
            clienteId: data.clienteId || null
        }));
        actualizarHeader();
        actualizarPanelModal();
    }

    function clearSesion() {
        localStorage.removeItem(STORAGE);
        actualizarHeader();
        actualizarPanelModal();
    }

    function abrirModal() {
        actualizarPanelModal();
        var modalEl = document.getElementById('modalMiCuenta');
        if (modalEl && window.bootstrap) {
            modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
            modalInstance.show();
        }
    }

    function actualizarHeader() {
        var sesion = getSesion();
        var btn = document.getElementById('btnMiCuenta');
        var dot = document.getElementById('catUserDot');
        if (!btn) return;

        if (sesion && sesion.nombre) {
            btn.classList.add('cat-icon-btn--logged');
            btn.title = 'Hola, ' + primerNombre(sesion.nombre);
            if (dot) dot.style.display = '';
        } else {
            btn.classList.remove('cat-icon-btn--logged');
            btn.title = 'Mi cuenta';
            if (dot) dot.style.display = 'none';
        }
    }

    function actualizarPanelModal() {
        var sesion = getSesion();
        var panelSesion = document.getElementById('panelClienteSesion');
        var panelAuth = document.getElementById('panelClienteAuth');
        if (!panelSesion || !panelAuth) return;

        if (sesion && sesion.nombre) {
            panelSesion.style.display = '';
            panelAuth.style.display = 'none';
            var nom = document.getElementById('sesionClienteNombre');
            var dni = document.getElementById('sesionClienteDni');
            var tel = document.getElementById('sesionClienteTelefono');
            if (nom) nom.textContent = sesion.nombre;
            if (dni) dni.textContent = sesion.documento;
            if (tel) tel.textContent = sesion.telefono || '—';
        } else {
            panelSesion.style.display = 'none';
            panelAuth.style.display = '';
        }
    }

    function mostrarAuth(visible) {
        var panelSesion = document.getElementById('panelClienteSesion');
        var panelAuth = document.getElementById('panelClienteAuth');
        if (panelSesion) panelSesion.style.display = visible ? 'none' : '';
        if (panelAuth) panelAuth.style.display = visible ? '' : 'none';
    }

    function irTabRegistro() {
        var tab = document.getElementById('tab-registro');
        if (tab && window.bootstrap && bootstrap.Tab) {
            bootstrap.Tab.getOrCreateInstance(tab).show();
        }
    }

    function login() {
        var docInput = document.getElementById('loginDocumento');
        var doc = docInput ? docInput.value.replace(/\D/g, '') : '';
        if (doc.length !== 8) {
            mostrarMsg('loginClienteMsg', 'Ingrese un DNI válido de 8 dígitos.', 'danger');
            return;
        }

        mostrarMsg('loginClienteMsg', 'Verificando DNI...', 'muted');
        fetch('/catalogo/api/cliente/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ documento: doc })
        })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res.success) {
                    mostrarMsg('loginClienteMsg', res.message || 'No se pudo iniciar sesión.', 'danger');
                    return;
                }

                if (res.requiereNombre) {
                    mostrarMsg('loginClienteMsg', res.message || 'Regístrese con sus datos.', 'muted');
                    irTabRegistro();
                    var regDoc = document.getElementById('regDocumento');
                    if (regDoc) regDoc.value = doc;
                    return;
                }

                if (!res.found || !res.nombre) {
                    mostrarMsg('loginClienteMsg', res.message || 'No se encontró el DNI. Regístrese primero.', 'danger');
                    irTabRegistro();
                    var regDoc2 = document.getElementById('regDocumento');
                    if (regDoc2) regDoc2.value = doc;
                    return;
                }

                setSesion({
                    documento: res.documento || doc,
                    nombre: res.nombre,
                    telefono: res.telefono || '',
                    clienteId: res.clienteId || null
                });

                if (!res.clienteId || !res.telefono) {
                    alerta('Sesión iniciada. Complete su teléfono en Registrarse para agilizar sus compras.', 'success');
                } else {
                    alerta(res.message || 'Sesión iniciada correctamente.', 'success');
                }

                if (modalInstance) modalInstance.hide();
            })
            .catch(function () {
                mostrarMsg('loginClienteMsg', 'Error de conexión. Intente nuevamente.', 'danger');
            });
    }

    function registrar() {
        var doc = (document.getElementById('regDocumento') || {}).value;
        var nombre = (document.getElementById('regNombre') || {}).value;
        var telefono = (document.getElementById('regTelefono') || {}).value;
        doc = (doc || '').replace(/\D/g, '');
        telefono = (telefono || '').replace(/\D/g, '');

        if (doc.length !== 8) {
            mostrarMsg('regClienteMsg', 'Ingrese un DNI válido de 8 dígitos.', 'danger');
            return;
        }
        if (!(nombre || '').trim()) {
            mostrarMsg('regClienteMsg', 'Ingrese su nombre completo.', 'danger');
            return;
        }
        if (!/^9\d{8}$/.test(telefono)) {
            mostrarMsg('regClienteMsg', 'El teléfono debe tener 9 dígitos y comenzar con 9.', 'danger');
            return;
        }

        mostrarMsg('regClienteMsg', 'Guardando datos...', 'muted');
        fetch('/catalogo/api/cliente/registrar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                documento: doc,
                nombre: nombre.trim(),
                telefono: telefono
            })
        })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res.success) {
                    mostrarMsg('regClienteMsg', res.message || 'No se pudo registrar.', 'danger');
                    return;
                }

                setSesion({
                    documento: res.documento || doc,
                    nombre: res.nombre || nombre.trim(),
                    telefono: res.telefono || telefono,
                    clienteId: res.clienteId || null
                });

                alerta(res.message || 'Registro completado.', 'success');
                if (modalInstance) modalInstance.hide();
            })
            .catch(function () {
                mostrarMsg('regClienteMsg', 'Error de conexión. Intente nuevamente.', 'danger');
            });
    }

    function buscarDniRegistro() {
        var docInput = document.getElementById('regDocumento');
        var doc = docInput ? docInput.value.replace(/\D/g, '') : '';
        if (doc.length !== 8) {
            mostrarMsg('regClienteMsg', 'Ingrese un DNI válido de 8 dígitos.', 'danger');
            return;
        }

        mostrarMsg('regClienteMsg', 'Consultando DNI...', 'muted');
        fetch('/catalogo/api/documento?documento=' + encodeURIComponent(doc))
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res.success) {
                    mostrarMsg('regClienteMsg', res.message || 'No se pudo consultar el DNI.', 'danger');
                    return;
                }

                var nombreInput = document.getElementById('regNombre');
                if (res.found && res.nombre && nombreInput) {
                    nombreInput.value = res.nombre;
                    nombreInput.readOnly = !res.requiereNombre;
                    mostrarMsg('regClienteMsg', res.message || 'Nombre cargado.', 'success');
                    return;
                }

                if (res.requiereNombre && nombreInput) {
                    nombreInput.readOnly = false;
                    nombreInput.focus();
                    mostrarMsg('regClienteMsg', res.message || 'Ingrese su nombre manualmente.', 'muted');
                    return;
                }

                mostrarMsg('regClienteMsg', res.message || 'Complete sus datos manualmente.', 'muted');
            })
            .catch(function () {
                mostrarMsg('regClienteMsg', 'Error de conexión al consultar el DNI.', 'danger');
            });
    }

    function cerrarSesion() {
        clearSesion();
        alerta('Sesión cerrada.', 'success');
        if (modalInstance) modalInstance.hide();
    }

    function aplicarEnCheckout() {
        var sesion = getSesion();
        if (!sesion || !sesion.documento) return;
        if (window.CatalogoCheckout && CatalogoCheckout.aplicarDatosCliente) {
            CatalogoCheckout.aplicarDatosCliente(sesion);
        }
    }

    function mostrarMsg(id, texto, tipo) {
        var el = document.getElementById(id);
        if (!el) return;
        el.style.display = texto ? '' : 'none';
        el.textContent = texto || '';
        el.className = 'small mb-2 cat-cliente-msg-' + (tipo || 'muted');
    }

    function primerNombre(nombre) {
        var partes = (nombre || '').trim().split(/\s+/);
        return partes[0] || 'Cliente';
    }

    function alerta(msg, tipo) {
        if (onAlertCallback) onAlertCallback(msg, tipo);
    }

    window.CatalogoCliente = {
        init: init,
        getSesion: getSesion,
        aplicarEnCheckout: aplicarEnCheckout,
        abrirModal: abrirModal
    };
})();
