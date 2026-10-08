(function () {
    'use strict';

    var config = { costoEnvio: 0, yapeCelular: '', plinCelular: '' };
    var carrito = [];
    var modalInstance = null;
    var onSuccessCallback = null;
    var onAlertCallback = null;

    var state = {
        dniVerificado: false,
        nombreCliente: '',
        formaEntrega: null,
        horarioRecojoId: null,
        horarioRecojoLabel: '',
        metodoPago: null,
        comprobanteFile: null,
        codigoValidacionPago: ''
    };

    function init(checkoutConfig, callbacks) {
        config = checkoutConfig || config;
        if (config.yape && config.yape.celular) {
            config.yapeCelular = config.yape.celular;
        }
        if (config.plin && config.plin.celular) {
            config.plinCelular = config.plin.celular;
        }
        onSuccessCallback = callbacks && callbacks.onSuccess;
        onAlertCallback = callbacks && callbacks.onAlert;
        bindEvents();
    }

    function bindEvents() {
        var modalEl = document.getElementById('modalCheckout');
        if (modalEl) {
            modalEl.addEventListener('hidden.bs.modal', resetCheckout);
        }

        onClick('btnBuscarDni', buscarDni);
        onClick('btnConfirmarPedido', confirmarPedido);

        var codigoInput = document.getElementById('inputCodigoValidacion');
        if (codigoInput) {
            codigoInput.addEventListener('input', function () {
                codigoInput.value = codigoInput.value.replace(/[^a-zA-Z0-9\-]/g, '').toUpperCase().slice(0, 30);
                state.codigoValidacionPago = codigoInput.value.trim();
                actualizarConfirmar();
            });
        }

        var dniInput = document.getElementById('pedidoDocumento');
        if (dniInput) {
            dniInput.addEventListener('input', function () {
                dniInput.value = dniInput.value.replace(/\D/g, '').slice(0, 8);
                if (state.dniVerificado) {
                    state.dniVerificado = false;
                    state.nombreCliente = '';
                    var nombre = document.getElementById('pedidoNombre');
                    if (nombre) {
                        nombre.value = '';
                        nombre.readOnly = false;
                    }
                    toggleSection('sectionEntrega', false);
                    toggleSection('sectionPago', false);
                    actualizarConfirmar();
                }
            });
            dniInput.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    buscarDni();
                }
            });
        }

        document.querySelectorAll('[data-forma-entrega]').forEach(function (card) {
            card.addEventListener('click', function () {
                seleccionarEntrega(card.dataset.formaEntrega);
            });
        });

        document.querySelectorAll('[data-metodo-pago]').forEach(function (card) {
            card.addEventListener('click', function () {
                seleccionarPago(card.dataset.metodoPago);
            });
        });

        ['pedidoDireccion', 'pedidoReferencia', 'pedidoDistrito', 'pedidoTelefono', 'pedidoNombre'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', function () {
                    if (id === 'pedidoTelefono') {
                        el.value = el.value.replace(/\D/g, '').slice(0, 9);
                    }
                    if (id === 'pedidoNombre' && !el.readOnly && el.value.trim()) {
                        var doc = document.getElementById('pedidoDocumento');
                        if (doc && doc.value.replace(/\D/g, '').length === 8) {
                            state.dniVerificado = true;
                            state.nombreCliente = el.value.trim();
                            toggleSection('sectionEntrega', true);
                        }
                    }
                    actualizarConfirmar();
                });
            }
        });

        setupUpload();

        onClick('btnCambiarRecojo', function () {
            if (window.CatalogoHorariosRecojo) {
                CatalogoHorariosRecojo.abrir(
                    function (horario) {
                        state.horarioRecojoId = horario.id;
                        state.horarioRecojoLabel = horario.fechaLabel + ' · ' + horario.label;
                        mostrarRecojoSeleccionado(state.horarioRecojoLabel);
                        actualizarConfirmar();
                    },
                    function () { }
                );
            }
        });
    }

    function setupUpload() {
        var zone = document.getElementById('uploadComprobante');
        var input = document.getElementById('inputComprobante');
        if (!zone || !input) return;

        zone.addEventListener('click', function () { input.click(); });

        zone.addEventListener('dragover', function (e) {
            e.preventDefault();
            zone.classList.add('dragover');
        });
        zone.addEventListener('dragleave', function () {
            zone.classList.remove('dragover');
        });
        zone.addEventListener('drop', function (e) {
            e.preventDefault();
            zone.classList.remove('dragover');
            if (e.dataTransfer.files.length) {
                procesarArchivo(e.dataTransfer.files[0]);
            }
        });

        input.addEventListener('change', function () {
            if (input.files && input.files[0]) {
                procesarArchivo(input.files[0]);
            }
        });
    }

    function procesarArchivo(file) {
        var tipos = ['image/jpeg', 'image/jpg', 'image/png'];
        if (tipos.indexOf(file.type) < 0) {
            alerta('Solo se permiten imágenes JPG, JPEG o PNG.', 'warning');
            return;
        }
        state.comprobanteFile = file;
        var reader = new FileReader();
        reader.onload = function (e) {
            var preview = document.getElementById('previewComprobante');
            var img = document.getElementById('imgPreviewComprobante');
            if (preview && img) {
                img.src = e.target.result;
                preview.classList.add('show');
            }
        };
        reader.readAsDataURL(file);
        actualizarConfirmar();
    }

    function abrir(items) {
        carrito = items || [];
        resetCheckout();
        if (window.CatalogoCliente) {
            CatalogoCliente.aplicarEnCheckout();
        }
        renderResumen();
        var modalEl = document.getElementById('modalCheckout');
        if (modalEl && window.bootstrap) {
            modalInstance = bootstrap.Modal.getOrCreateInstance(modalEl);
            modalInstance.show();
        }
    }

    function aplicarDatosCliente(datos) {
        if (!datos || !datos.documento) return;

        var dniInput = document.getElementById('pedidoDocumento');
        var nombre = document.getElementById('pedidoNombre');
        var tel = document.getElementById('pedidoTelefono');

        if (dniInput) dniInput.value = datos.documento;

        if (datos.nombre && nombre) {
            nombre.value = datos.nombre;
            nombre.readOnly = true;
            state.nombreCliente = datos.nombre;
            state.dniVerificado = true;
        }

        if (datos.telefono && tel) {
            tel.value = datos.telefono;
        }

        if (state.dniVerificado) {
            mostrarMensajeDni('Datos cargados desde su cuenta.', 'success');
            toggleSection('sectionEntrega', true);
            actualizarConfirmar();
        }
    }

    function resetCheckout() {
        state = {
            dniVerificado: false,
            nombreCliente: '',
            formaEntrega: null,
            horarioRecojoId: null,
            horarioRecojoLabel: '',
            metodoPago: null,
            comprobanteFile: null,
            codigoValidacionPago: ''
        };

        var form = document.getElementById('formCheckout');
        if (form) form.reset();

        var nombre = document.getElementById('pedidoNombre');
        if (nombre) nombre.readOnly = false;

        limpiarMensajeDni();
        toggleSection('sectionEntrega', false);
        toggleSection('sectionPago', false);
        ocultarCamposEnvio();
        ocultarRecojoSeleccionado();

        document.querySelectorAll('[data-forma-entrega]').forEach(function (c) {
            c.classList.remove('selected');
        });
        document.querySelectorAll('[data-metodo-pago]').forEach(function (c) {
            c.classList.remove('selected');
        });

        var qrBox = document.getElementById('qrPagoBox');
        if (qrBox) {
            qrBox.classList.remove('show');
            qrBox.innerHTML = '';
        }

        var preview = document.getElementById('previewComprobante');
        if (preview) preview.classList.remove('show');
        var input = document.getElementById('inputComprobante');
        if (input) input.value = '';

        actualizarConfirmar();
    }

    function toggleSection(id, visible) {
        var el = document.getElementById(id);
        if (!el) return;
        el.classList.toggle('cat-section-hidden', !visible);
        el.classList.toggle('cat-section-visible', visible);
    }

    function buscarDni() {
        var inputDoc = document.getElementById('pedidoDocumento');
        var inputNombre = document.getElementById('pedidoNombre');
        if (!inputDoc || !inputNombre) return;

        var documento = inputDoc.value.replace(/\D/g, '').trim();
        if (documento.length !== 8) {
            mostrarMensajeDni('Ingrese un DNI válido de 8 dígitos.', 'danger');
            return;
        }

        inputDoc.value = documento;
        mostrarMensajeDni('Consultando DNI...', 'muted');
        var btn = document.getElementById('btnBuscarDni');
        if (btn) btn.disabled = true;

        fetch('/catalogo/api/documento?documento=' + encodeURIComponent(documento))
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (!res.success) {
                    mostrarMensajeDni(res.message || 'No se pudo validar el DNI.', 'danger');
                    state.dniVerificado = false;
                    toggleSection('sectionEntrega', false);
                    toggleSection('sectionPago', false);
                    actualizarConfirmar();
                    return;
                }

                if (res.requiereNombre) {
                    inputNombre.readOnly = false;
                    inputNombre.focus();
                    state.dniVerificado = false;
                    mostrarMensajeDni(res.message || 'Ingrese su nombre manualmente.', 'muted');
                    toggleSection('sectionEntrega', false);
                    actualizarConfirmar();
                    return;
                }

                if (res.found && res.nombre) {
                    inputNombre.value = res.nombre;
                    inputNombre.readOnly = true;
                    state.nombreCliente = res.nombre;
                    state.dniVerificado = true;

                    var tel = document.getElementById('pedidoTelefono');
                    if (tel && res.telefono && !tel.value.trim()) {
                        tel.value = res.telefono;
                    }

                    mostrarMensajeDni(res.message || 'Cliente verificado correctamente.', 'success');
                    toggleSection('sectionEntrega', true);
                    actualizarConfirmar();
                    return;
                }

                state.dniVerificado = false;
                mostrarMensajeDni(res.message || 'DNI no encontrado.', 'danger');
                toggleSection('sectionEntrega', false);
                actualizarConfirmar();
            })
            .catch(function () {
                mostrarMensajeDni('Error de conexión al consultar el DNI.', 'danger');
                state.dniVerificado = false;
                actualizarConfirmar();
            })
            .finally(function () {
                if (btn) btn.disabled = false;
            });
    }

    function seleccionarEntrega(valor) {
        if (valor === 'RECOJO_TIENDA') {
            ocultarCamposEnvio();
            state.formaEntrega = null;
            state.horarioRecojoId = null;
            state.horarioRecojoLabel = '';
            ocultarRecojoSeleccionado();
            toggleSection('sectionPago', false);

            document.querySelectorAll('[data-forma-entrega]').forEach(function (c) {
                c.classList.remove('selected');
            });

            if (window.CatalogoHorariosRecojo) {
                CatalogoHorariosRecojo.abrir(
                    function (horario) {
                        state.formaEntrega = 'RECOJO_TIENDA';
                        state.horarioRecojoId = horario.id;
                        state.horarioRecojoLabel = horario.fechaLabel + ' · ' + horario.label;
                        document.querySelectorAll('[data-forma-entrega]').forEach(function (c) {
                            c.classList.toggle('selected', c.dataset.formaEntrega === 'RECOJO_TIENDA');
                        });
                        mostrarRecojoSeleccionado(state.horarioRecojoLabel);
                        toggleSection('sectionPago', true);
                        renderResumen();
                        actualizarConfirmar();
                    },
                    function () {
                        document.querySelectorAll('[data-forma-entrega]').forEach(function (c) {
                            c.classList.remove('selected');
                        });
                        state.formaEntrega = null;
                        state.horarioRecojoId = null;
                        state.horarioRecojoLabel = '';
                        ocultarRecojoSeleccionado();
                        toggleSection('sectionPago', false);
                        actualizarConfirmar();
                    }
                );
            } else {
                alerta('No se pudo abrir el selector de horarios. Recargue la página.', 'danger');
            }
            return;
        }

        state.formaEntrega = valor;
        state.horarioRecojoId = null;
        state.horarioRecojoLabel = '';
        ocultarRecojoSeleccionado();

        document.querySelectorAll('[data-forma-entrega]').forEach(function (c) {
            c.classList.toggle('selected', c.dataset.formaEntrega === valor);
        });

        if (valor === 'ENVIO_DOMICILIO') {
            mostrarCamposEnvio();
        } else {
            ocultarCamposEnvio();
        }

        toggleSection('sectionPago', true);
        renderResumen();
        actualizarConfirmar();
    }

    function mostrarRecojoSeleccionado(texto) {
        var box = document.getElementById('recojoSeleccionado');
        var label = document.getElementById('recojoSeleccionadoTexto');
        if (box && label) {
            label.textContent = texto;
            box.classList.add('show');
        }
    }

    function ocultarRecojoSeleccionado() {
        var box = document.getElementById('recojoSeleccionado');
        var label = document.getElementById('recojoSeleccionadoTexto');
        if (box) box.classList.remove('show');
        if (label) label.textContent = '';
    }

    function mostrarCamposEnvio() {
        var fields = document.getElementById('camposEnvio');
        if (fields) fields.classList.add('show');
    }

    function ocultarCamposEnvio() {
        var fields = document.getElementById('camposEnvio');
        if (fields) fields.classList.remove('show');
        ['pedidoDireccion', 'pedidoReferencia', 'pedidoDistrito'].forEach(function (id) {
            var el = document.getElementById(id);
            if (el) el.value = '';
        });
    }

    function seleccionarPago(valor) {
        state.metodoPago = valor;
        document.querySelectorAll('[data-metodo-pago]').forEach(function (c) {
            c.classList.toggle('selected', c.dataset.metodoPago === valor);
        });
        generarQr(valor);
        actualizarConfirmar();
    }

    function generarQr(metodo) {
        var qrBox = document.getElementById('qrPagoBox');
        if (!qrBox) return;

        var block = metodo === 'YAPE' ? (config.yape || {}) : (config.plin || {});
        var celular = block.celular || (metodo === 'YAPE' ? config.yapeCelular : config.plinCelular) || '';
        var titular = block.nombreTitular || '';
        var total = calcularTotal();
        var etiqueta = metodo === 'YAPE' ? 'Yape' : 'Plin';
        var texto = block.textoQr || (etiqueta + ' al ' + celular + ' - Total: S/ ' + formatearPrecio(total));

        if (block.imagenQr) {
            qrBox.innerHTML =
                '<p class="small text-muted mb-2">Pague con <strong>' + etiqueta + '</strong> — <strong>S/ ' +
                formatearPrecio(total) + '</strong></p>' +
                '<div class="d-flex justify-content-center"><img src="' + block.imagenQr +
                '" alt="QR" style="max-width:180px" class="img-thumbnail"/></div>' +
                (titular ? '<p class="small text-muted text-center mt-2 mb-0">' + titular + '</p>' : '') +
                '<p class="small text-muted text-center mb-0">' + etiqueta + ' al ' + celular + '</p>';
            qrBox.classList.add('show');
            return;
        }

        qrBox.innerHTML =
            '<p class="small text-muted mb-2">Escanea el código QR y realiza el pago de <strong>S/ ' +
            formatearPrecio(total) + '</strong></p>' +
            (titular ? '<p class="small text-muted text-center mb-1">' + titular + '</p>' : '') +
            '<div id="qrCodeContainer" class="d-flex justify-content-center"></div>' +
            '<p class="small text-muted text-center mt-2 mb-0">' + etiqueta + ' al ' + celular + '</p>';

        qrBox.classList.add('show');

        var container = document.getElementById('qrCodeContainer');
        if (!container) return;

        if (typeof QRCode === 'function') {
            new QRCode(container, {
                text: texto,
                width: 180,
                height: 180,
                colorDark: '#1e293b',
                colorLight: '#ffffff',
                correctLevel: QRCode.CorrectLevel.M
            });
            return;
        }

        container.innerHTML =
            '<img src="https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=' +
            encodeURIComponent(texto) + '" alt="Código QR de pago" class="cat-qr-img">';
    }

    function calcularSubtotal() {
        return carrito.reduce(function (s, i) { return s + i.precio * i.cantidad; }, 0);
    }

    function calcularCostoEnvio() {
        return state.formaEntrega === 'ENVIO_DOMICILIO' ? Number(config.costoEnvio) || 0 : 0;
    }

    function calcularTotal() {
        return calcularSubtotal() + calcularCostoEnvio();
    }

    function renderResumen() {
        var tbody = document.getElementById('resumenItems');
        if (!tbody) return;

        var subtotal = calcularSubtotal();
        var envio = calcularCostoEnvio();
        var total = subtotal + envio;

        if (!carrito.length) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">Sin productos</td></tr>';
        } else {
            tbody.innerHTML = carrito.map(function (item) {
                var lineSub = item.precio * item.cantidad;
                return '<tr>' +
                    '<td>' + escapeHtml(item.nombre) + '</td>' +
                    '<td class="text-center">' + item.cantidad + '</td>' +
                    '<td class="text-end">S/ ' + formatearPrecio(item.precio) + '</td>' +
                    '<td class="text-end fw-semibold">S/ ' + formatearPrecio(lineSub) + '</td>' +
                    '</tr>';
            }).join('');
        }

        var elSub = document.getElementById('resumenSubtotal');
        var elEnvio = document.getElementById('resumenEnvio');
        var elEnvioRow = document.getElementById('resumenEnvioRow');
        var elTotal = document.getElementById('resumenTotal');

        if (elSub) elSub.textContent = 'S/ ' + formatearPrecio(subtotal);
        if (elEnvio) elEnvio.textContent = 'S/ ' + formatearPrecio(envio);
        if (elEnvioRow) elEnvioRow.style.display = envio > 0 ? 'flex' : 'none';
        if (elTotal) elTotal.textContent = 'S/ ' + formatearPrecio(total);

        if (state.metodoPago) {
            generarQr(state.metodoPago);
        }
    }

    function entregaValida() {
        if (!state.formaEntrega) return false;
        if (state.formaEntrega === 'ENVIO_DOMICILIO') {
            var dir = document.getElementById('pedidoDireccion');
            var dist = document.getElementById('pedidoDistrito');
            return dir && dir.value.trim() && dist && dist.value.trim();
        }
        if (state.formaEntrega === 'RECOJO_TIENDA') {
            return !!state.horarioRecojoId;
        }
        return true;
    }

    function telefonoValido() {
        var tel = document.getElementById('pedidoTelefono');
        if (!tel) return false;
        return /^9\d{8}$/.test(tel.value.replace(/\D/g, ''));
    }

    function codigoValidacionValido() {
        var input = document.getElementById('inputCodigoValidacion');
        if (!input) return false;
        var codigo = input.value.trim().toUpperCase();
        state.codigoValidacionPago = codigo;
        return /^[A-Z0-9\-]{6,30}$/.test(codigo);
    }

    function actualizarConfirmar() {
        var btn = document.getElementById('btnConfirmarPedido');
        if (!btn) return;

        var listo = state.dniVerificado &&
            entregaValida() &&
            state.metodoPago &&
            state.comprobanteFile &&
            codigoValidacionValido() &&
            telefonoValido();

        btn.disabled = !listo;
    }

    function confirmarPedido() {
        if (!state.dniVerificado || !entregaValida() || !state.metodoPago || !state.comprobanteFile) {
            alerta('Complete todos los pasos antes de confirmar.', 'warning');
            return;
        }

        if (!codigoValidacionValido()) {
            alerta('Ingrese el código de validación de pago (6 a 30 caracteres).', 'warning');
            return;
        }

        var telefono = document.getElementById('pedidoTelefono').value.replace(/\D/g, '');
        if (!/^9\d{8}$/.test(telefono)) {
            alerta('El teléfono debe tener 9 dígitos y comenzar con 9.', 'warning');
            return;
        }

        var datos = {
            nombreCliente: state.nombreCliente || document.getElementById('pedidoNombre').value.trim(),
            telefono: telefono,
            documento: document.getElementById('pedidoDocumento').value.replace(/\D/g, ''),
            formaEntrega: state.formaEntrega,
            metodoPago: state.metodoPago,
            codigoValidacionPago: state.codigoValidacionPago,
            costoEnvio: calcularCostoEnvio(),
            items: carrito.map(function (i) {
                return {
                    productoId: i.productoId,
                    presentacionId: i.presentacionId,
                    cantidad: i.cantidad
                };
            })
        };

        if (state.formaEntrega === 'ENVIO_DOMICILIO') {
            datos.direccion = document.getElementById('pedidoDireccion').value.trim();
            datos.referencia = document.getElementById('pedidoReferencia').value.trim();
            datos.distrito = document.getElementById('pedidoDistrito').value.trim();
        } else if (state.formaEntrega === 'RECOJO_TIENDA') {
            datos.horarioRecojoId = state.horarioRecojoId;
        }

        var formData = new FormData();
        formData.append('datos', new Blob([JSON.stringify(datos)], { type: 'application/json' }));
        formData.append('comprobante', state.comprobanteFile);

        var btn = document.getElementById('btnConfirmarPedido');
        btn.disabled = true;

        fetch('/catalogo/api/pedido', { method: 'POST', body: formData })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                if (res.success) {
                    if (modalInstance) modalInstance.hide();
                    if (onSuccessCallback) onSuccessCallback(res);
                } else {
                    alerta(res.message || 'No se pudo registrar el pedido.', 'danger');
                }
            })
            .catch(function () {
                alerta('Error de conexión al registrar el pedido.', 'danger');
            })
            .finally(function () {
                actualizarConfirmar();
            });
    }

    function limpiarMensajeDni() {
        var msg = document.getElementById('pedidoDocumentoMsg');
        if (msg) {
            msg.textContent = '';
            msg.className = 'form-text text-muted';
        }
    }

    function mostrarMensajeDni(texto, tipo) {
        var msg = document.getElementById('pedidoDocumentoMsg');
        if (!msg) return;
        msg.textContent = texto || '';
        msg.className = 'form-text ' + (tipo === 'success' ? 'text-success' : tipo === 'danger' ? 'text-danger' : 'text-muted');
    }

    function onClick(id, handler) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', handler);
    }

    function formatearPrecio(valor) {
        return Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text || '';
        return div.innerHTML;
    }

    function alerta(msg, tipo) {
        if (onAlertCallback) onAlertCallback(msg, tipo);
    }

    window.CatalogoCheckout = {
        init: init,
        abrir: abrir,
        aplicarDatosCliente: aplicarDatosCliente
    };
})();
