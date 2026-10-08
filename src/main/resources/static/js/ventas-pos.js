(function () {
    'use strict';

    var productosVenta = [];
    var productosDisponibles = [];
    var categoriasCatalogo = [];
    var ventaStockOriginal = {};
    var ventaBusquedaTimer = null;
    var pagoConfig = window.posPagoConfig || { yapeCelular: '', plinCelular: '', yape: {}, plin: {} };

    function cfgPago(canal) {
        var key = canal === 'Yape' ? 'yape' : 'plin';
        var block = pagoConfig[key] || {};
        return {
            celular: block.celular || pagoConfig[key + 'Celular'] || '',
            titular: block.nombreTitular || '',
            textoQr: block.textoQr || '',
            imagenQr: block.imagenQr || ''
        };
    }

    function esPagoMixtoNombre(nombre) {
        return (nombre || '').toLowerCase().indexOf('mixto') >= 0;
    }

    function fmt(n) {
        var v = parseFloat(n);
        if (isNaN(v)) v = 0;
        return v.toFixed(2);
    }

    function escHtml(s) {
        return String(s || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function normalizarDocumento(doc) {
        return (doc || '').replace(/\D/g, '').trim();
    }

    function documentoValido(doc) {
        var limpio = normalizarDocumento(doc);
        return limpio.length === 8 || limpio.length === 11;
    }

    function iconoPago(nombre) {
        var n = (nombre || '').toLowerCase();
        if (n.indexOf('yape') >= 0) return 'fa-mobile-alt';
        if (n.indexOf('plin') >= 0) return 'fa-mobile-alt';
        if (n.indexOf('contado') >= 0) return 'fa-money-bill-wave';
        if (n.indexOf('mixto') >= 0) return 'fa-layer-group';
        return 'fa-wallet';
    }

    function nombreTipoPagoActivo() {
        var id = $('#venta-tipo-pago').val();
        var $card = id ? $('.pos-tipo-pago-card[data-id="' + id + '"]') : $();
        return ($card.data('nombre') || '').toString();
    }

    function esPagoDigital(nombre) {
        var n = (nombre || '').toLowerCase();
        return n.indexOf('yape') >= 0 || n.indexOf('plin') >= 0;
    }

    function actualizarQrPago() {
        var $box = $('#pos-qr-pago');
        if (!$box.length) return;

        var nombre = nombreTipoPagoActivo();
        if (esPagoMixtoNombre(nombre)) {
            $box.removeClass('show').empty();
            actualizarQrMixto();
            return;
        }

        limpiarQrMixto();

        if (!esPagoDigital(nombre)) {
            $box.removeClass('show').empty();
            return;
        }

        var n = nombre.toLowerCase();
        var esYape = n.indexOf('yape') >= 0;
        var etiqueta = esYape ? 'Yape' : 'Plin';
        var total = calcularTotales().total;

        if (total <= 0) {
            $box.addClass('show').html(
                '<p class="small text-muted mb-0 text-center">' +
                '<i class="fas fa-info-circle mr-1"></i>Agregue productos para generar el QR de <strong>' +
                escHtml(etiqueta) + '</strong>.</p>'
            );
            return;
        }

        renderQrEnContenedor($box, etiqueta, total, 'posQrCodeContainer', false);
    }

    function limpiarQrMixto() {
        $('#pos-mixto-qr-yape, #pos-mixto-qr-plin').removeClass('show').empty();
    }

    function actualizarQrMixto() {
        var yape = parseFloat($('#pos-mixto-yape').val()) || 0;
        var plin = parseFloat($('#pos-mixto-plin').val()) || 0;
        renderQrEnContenedor($('#pos-mixto-qr-yape'), 'Yape', yape, 'posMixtoQrYape', true);
        renderQrEnContenedor($('#pos-mixto-qr-plin'), 'Plin', plin, 'posMixtoQrPlin', true);
    }

    function renderQrEnContenedor($box, etiqueta, monto, containerId, compacto) {
        if (!$box || !$box.length) return;

        if (monto <= 0) {
            $box.removeClass('show').empty();
            return;
        }

        var cfg = cfgPago(etiqueta);
        var celular = cfg.celular;
        var titular = cfg.titular;
        var texto = cfg.textoQr || (etiqueta + ' al ' + celular + ' - Total: S/ ' + fmt(monto));
        var qrSize = compacto ? 96 : 160;

        if (cfg.imagenQr) {
            if (compacto) {
                $box.addClass('show').html(
                    '<img src="' + escHtml(cfg.imagenQr) + '" alt="QR ' + escHtml(etiqueta) + '" class="pos-qr-img" />' +
                    '<div class="pos-mixto-qr-label"><strong>' + escHtml(etiqueta) + '</strong> S/ ' + fmt(monto) + '</div>'
                );
            } else {
                $box.addClass('show').html(
                    '<p class="small text-muted mb-2 text-center">Pague con <strong class="text-purple">' +
                    escHtml(etiqueta) + '</strong> — <strong>S/ ' + fmt(monto) + '</strong></p>' +
                    '<div class="d-flex justify-content-center"><img src="' + escHtml(cfg.imagenQr) +
                    '" alt="QR" class="pos-qr-img" style="max-width:180px"/></div>' +
                    (titular ? '<p class="small text-muted mb-0 text-center mt-2">' + escHtml(titular) + '</p>' : '') +
                    '<p class="small text-muted mb-0 text-center">' + escHtml(etiqueta) + ' al ' + escHtml(celular) + '</p>'
                );
            }
            return;
        }

        if (compacto) {
            $box.addClass('show').html(
                '<div id="' + containerId + '" class="d-flex justify-content-center"></div>' +
                '<div class="pos-mixto-qr-label"><strong>' + escHtml(etiqueta) + '</strong> S/ ' + fmt(monto) +
                (celular ? '<br>' + escHtml(celular) : '') + '</div>'
            );
        } else {
            $box.html(
                '<p class="small text-muted mb-2 text-center">Escanea con <strong class="text-purple">' +
                escHtml(etiqueta) + '</strong> y paga <strong class="text-purple">S/ ' + fmt(monto) + '</strong></p>' +
                '<div class="pos-qr-container d-flex justify-content-center" id="' + containerId + '"></div>' +
                (titular ? '<p class="small text-muted mb-0 text-center mt-1">' + escHtml(titular) + '</p>' : '') +
                '<p class="small text-muted mb-0 text-center mt-2">' + escHtml(etiqueta) + ' al ' + escHtml(celular) + '</p>'
            );
            $box.addClass('show');
        }

        var container = document.getElementById(containerId);
        if (!container) return;

        if (typeof QRCode === 'function') {
            new QRCode(container, {
                text: texto,
                width: qrSize,
                height: qrSize,
                colorDark: '#1e293b',
                colorLight: '#ffffff',
                correctLevel: QRCode.CorrectLevel.M
            });
            return;
        }

        container.innerHTML =
            '<img src="https://api.qrserver.com/v1/create-qr-code/?size=' + qrSize + 'x' + qrSize + '&data=' +
            encodeURIComponent(texto) + '" alt="QR ' + escHtml(etiqueta) + '" class="pos-qr-img">';
    }

    function mostrarToast(mensaje, tipo) {
        var cls = tipo === 'success' ? 'alert-success' : (tipo === 'warning' ? 'alert-warning' : 'alert-danger');
        $('#venta-toast-area').html('<div class="alert ' + cls + ' py-2 mb-0 shadow-sm">' + escHtml(mensaje) + '</div>');
        setTimeout(function () { $('#venta-toast-area').html(''); }, 4500);
    }

    function bloquearCamposCliente(bloquear) {
        $('#venta-documento').prop('readonly', bloquear).toggleClass('pos-input-readonly', bloquear);
        $('#venta-nombre-cliente').prop('readonly', bloquear).toggleClass('pos-input-readonly', bloquear);
        $('#venta-btn-buscar').prop('disabled', bloquear);
    }

    function mostrarClienteVerificado(mostrar) {
        if (mostrar) {
            $('#venta-cliente-status').fadeIn(150);
        } else {
            $('#venta-cliente-status').hide();
        }
    }

    function datosClienteVerificados(res) {
        return !res.requiereNombre && (res.origen === 'api' || (res.origen === 'bodega' && res.found));
    }

    function aplicarClienteVenta(res) {
        if (res.requiereNombre) {
            $('#venta-cliente-id').val('');
            bloquearCamposCliente(false);
            mostrarClienteVerificado(false);
            $('#venta-nombre-cliente').prop('readonly', false).focus();
            return;
        }
        if (res.documento) {
            $('#venta-documento').val(res.documento);
        }
        if (res.clienteId) {
            $('#venta-cliente-id').val(res.clienteId);
        }
        if (res.nombre) {
            $('#venta-nombre-cliente').val(res.nombre);
        }
        var verificado = datosClienteVerificados(res);
        bloquearCamposCliente(verificado);
        mostrarClienteVerificado(verificado && !!res.clienteId);
    }

    function actualizarPanelesPago() {
        var nombre = nombreTipoPagoActivo();
        var mixto = esPagoMixtoNombre(nombre);
        var digital = esPagoDigital(nombre);
        var requiereCodigo = digital || mixto;

        $('#pos-pago-mixto-box').toggleClass('d-none', !mixto);
        $('#pos-codigo-verificacion-box').toggleClass('d-none', !requiereCodigo || (mixto && !mixtoRequiereCodigo()));

        if (mixto) {
            actualizarSaldoMixto();
        }
        actualizarQrPago();
    }

    function mixtoRequiereCodigo() {
        var yape = parseFloat($('#pos-mixto-yape').val()) || 0;
        var plin = parseFloat($('#pos-mixto-plin').val()) || 0;
        return yape > 0 || plin > 0;
    }

    function actualizarSaldoMixto() {
        var total = calcularTotales().total;
        var efectivo = parseFloat($('#pos-mixto-efectivo').val()) || 0;
        var yape = parseFloat($('#pos-mixto-yape').val()) || 0;
        var plin = parseFloat($('#pos-mixto-plin').val()) || 0;
        var saldo = total - efectivo - yape - plin;
        $('#pos-mixto-saldo').text('S/ ' + fmt(saldo));
        $('#pos-codigo-verificacion-box').toggleClass('d-none', !mixtoRequiereCodigo());
        actualizarQrMixto();
    }

    function seleccionarTipoPago(id) {
        if (!id) return;
        $('#venta-tipo-pago').val(id);
        $('.pos-tipo-pago-card').removeClass('active');
        $('.pos-tipo-pago-card[data-id="' + id + '"]').addClass('active');
        actualizarPanelesPago();
    }

    function aplicarSeleccionTipoPago() {
        var savedId = $('#venta-tipo-pago').val();
        var $saved = savedId ? $('.pos-tipo-pago-card[data-id="' + savedId + '"]') : $();
        if ($saved.length) {
            seleccionarTipoPago(savedId);
        } else {
            var $contado = $('.pos-tipo-pago-card').filter(function () {
                return String($(this).data('nombre') || '').toLowerCase().indexOf('contado') >= 0;
            }).first();
            if ($contado.length) {
                seleccionarTipoPago($contado.data('id'));
            } else {
                var firstId = $('.pos-tipo-pago-card').first().data('id');
                if (firstId) {
                    seleccionarTipoPago(firstId);
                }
            }
        }
    }

    function initTiposPago() {
        var $grid = $('#pos-tipo-pago-grid');
        if (!$grid.length) return;

        $grid.find('.pos-tipo-pago-card').each(function () {
            var nombre = $(this).data('nombre') || '';
            $(this).find('i').attr('class', 'fas ' + iconoPago(nombre));
        });

        if (!$grid.data('init')) {
            $grid.data('init', true);
            $grid.on('click', '.pos-tipo-pago-card', function () {
                seleccionarTipoPago($(this).data('id'));
            });
        }

        aplicarSeleccionTipoPago();
    }

    function resetModalVentaNueva() {
        $('#venta-edit-id').val('');
        ventaStockOriginal = {};
        $('#modalVentaTitulo').html('<i class="fas fa-cart-plus mr-2"></i>Nueva Venta');
        $('#btn-guardar-venta').html('<i class="fas fa-save mr-1"></i> Guardar Venta');
        $('#venta-comprobante').prop('disabled', false);
        mostrarClienteVerificado(false);
    }

    window.abrirModalVenta = function () {
        productosVenta = [];
        resetModalVentaNueva();
        $('#venta-documento, #venta-nombre-cliente').val('');
        bloquearCamposCliente(false);
        if (window.apiPeruActivo) {
            $('#venta-nombre-cliente').prop('readonly', true).addClass('pos-input-readonly');
        }
        $('#venta-cliente-id').val('');
        $('#venta-tipo-pago').val('');
        $('#pos-qr-pago').removeClass('show').empty();
        limpiarQrMixto();
        $('#pos-codigo-verificacion').val('');
        $('#pos-mixto-efectivo, #pos-mixto-yape, #pos-mixto-plin').val('0');
        $('#pos-codigo-verificacion-box, #pos-pago-mixto-box').addClass('d-none');
        $('#venta-toast-area').html('');
        initTiposPago();
        renderProductosTabla();
        actualizarNumeroVenta();
        initCatalogoVenta();
        $('#modalVenta').modal('show');
    };

    function stockMaximoPresentacion(presentacionId) {
        var p = productosDisponibles.find(function (x) { return x.id === presentacionId; });
        var base = p ? parseInt(p.stock, 10) : 0;
        var extra = ventaStockOriginal[presentacionId] ? parseInt(ventaStockOriginal[presentacionId], 10) : 0;
        return base + extra;
    }

    function cantidadEnCarrito(presentacionId) {
        return productosVenta
            .filter(function (p) { return p.presentacionId === presentacionId; })
            .reduce(function (sum, p) { return sum + p.cantidad; }, 0);
    }

    function stockDisponiblePresentacion(presentacionId) {
        return stockMaximoPresentacion(presentacionId) - cantidadEnCarrito(presentacionId);
    }

    window.editarVenta = function (btn) {
        var id = $(btn).data('venta-id');
        $.get('/ventas/api/' + id + '/editar')
            .done(function (res) {
                resetModalVentaNueva();
                $('#venta-edit-id').val(res.ventaId);
                $('#modalVentaTitulo').html('<i class="fas fa-edit mr-2"></i>Editar Venta');
                $('#btn-guardar-venta').html('<i class="fas fa-save mr-1"></i> Actualizar Venta');

                $('#venta-documento').val(res.documentoCliente || '');
                $('#venta-nombre-cliente').val(res.nombreCliente || '');
                $('#venta-cliente-id').val(res.clienteId);
                bloquearCamposCliente(true);
                mostrarClienteVerificado(!!res.clienteId);

                $('#venta-comprobante').val(res.tipoComprobanteId).prop('disabled', true);
                $('#venta-numero').text(res.numeroDocumento);
                if (res.tipoPagoId) {
                    $('#venta-tipo-pago').val(res.tipoPagoId);
                }

                ventaStockOriginal = res.stockOriginal || {};
                initTiposPago();
                productosVenta = (res.productos || []).map(function (p) {
                    return {
                        productoId: p.productoId,
                        presentacionId: p.presentacionId,
                        productoNombre: p.productoNombre || p.nombre,
                        presentacionNombre: p.presentacionNombre || '—',
                        medida: p.medida || extraerMedida(p.presentacionNombre).medida,
                        marca: p.marca || 'Sin marca',
                        imagen: urlImagenCatalogo(p),
                        nombre: p.nombre,
                        precio: parseFloat(p.precio),
                        cantidad: parseInt(p.cantidad, 10),
                        stock: parseInt(p.stock, 10),
                        descuento: parseFloat(p.descuento || 0)
                    };
                });

                $('#venta-toast-area').html('');
                renderProductosTabla();
                initCatalogoVenta();
                $('#modalVenta').modal('show');
            })
            .fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo cargar la venta para editar.';
                alert(msg);
            });
    };

    function buscarClienteLocal(doc, silencioso, callback) {
        var documento = normalizarDocumento(doc || $('#venta-documento').val());
        if (!documentoValido(documento)) {
            if (callback) callback(null);
            return;
        }

        $.get('/ventas/api/cliente/local', { documento: documento })
            .done(function (res) {
                if (res.success && res.found) {
                    aplicarClienteVenta(res);
                    if (!silencioso) {
                        mostrarToast(res.message, 'success');
                    }
                    if (callback) callback(res);
                } else if (callback) {
                    callback(null);
                }
            })
            .fail(function () {
                if (callback) callback(null);
            });
    }

    function buscarClienteEnApi(doc, $btn) {
        mostrarToast(window.apiPeruActivo ? 'Consultando ApiPeru (cliente no está en bodega)...' : 'Registrando cliente nuevo...', 'success');
        $.ajax({
            url: '/ventas/api/cliente/buscar',
            method: 'GET',
            data: {
                documento: doc,
                nombre: $('#venta-nombre-cliente').val().trim()
            },
            dataType: 'json',
            timeout: 30000
        }).done(function (res) {
            if (res.success) {
                aplicarClienteVenta(res);
                mostrarToast(res.message, res.requiereNombre ? 'warning' : 'success');
            } else {
                mostrarToast(res.message || 'Cliente no encontrado.', 'error');
                mostrarClienteVerificado(false);
            }
        }).fail(function (xhr) {
            var msg = 'Error al buscar el cliente.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                msg = xhr.responseJSON.message;
            }
            mostrarToast(msg, 'error');
        }).always(function () {
            if ($btn) {
                $btn.prop('disabled', false).html('<i class="fas fa-search"></i>');
            }
        });
    }

    window.buscarCliente = function () {
        var doc = normalizarDocumento($('#venta-documento').val());
        if (!documentoValido(doc)) {
            mostrarToast('Ingrese un DNI (8 dígitos) o RUC (11 dígitos).', 'error');
            return;
        }
        $('#venta-documento').val(doc);

        var $btn = $('#venta-btn-buscar');
        $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i>');
        mostrarToast('Buscando en la bodega...', 'success');

        buscarClienteLocal(doc, true, function (local) {
            if (local && local.found) {
                mostrarToast(local.message, 'success');
                $btn.prop('disabled', false).html('<i class="fas fa-search"></i>');
                return;
            }
            buscarClienteEnApi(doc, $btn);
        });
    };

    window.actualizarNumeroVenta = function () {
        var id = $('#venta-comprobante').val();
        if (!id) return;
        $.get('/ventas/api/comprobante/' + id + '/siguiente')
            .done(function (res) {
                $('#venta-numero').text(res.correlativo);
            });
    };

    function cargarCategorias(callback) {
        $.get('/ventas/api/categorias')
            .done(function (data) {
                categoriasCatalogo = data || [];
                var $sel = $('#pos-catalogo-categoria');
                $sel.find('option:not(:first)').remove();
                categoriasCatalogo.forEach(function (c) {
                    $sel.append('<option value="' + c.id + '">' + escHtml(c.nombre) + '</option>');
                });
                if (callback) callback();
            })
            .fail(function () {
                categoriasCatalogo = [];
                mostrarToast('No se pudieron cargar las categorías del catálogo.', 'error');
                if (callback) callback();
            });
    }

    window.cargarProductos = function (callback) {
        $.get('/ventas/api/productos')
            .done(function (data) {
                productosDisponibles = data || [];
                if (callback) callback();
            })
            .fail(function (xhr) {
                productosDisponibles = [];
                var msg = 'No se pudieron cargar los productos del catálogo.';
                if (xhr.responseJSON && xhr.responseJSON.message) {
                    msg = xhr.responseJSON.message;
                }
                mostrarToast(msg, 'error');
                if (callback) callback();
            });
    };

    function actualizarOpcionesCategoria() {
        var $sel = $('#pos-catalogo-categoria');
        categoriasCatalogo.forEach(function (c) {
            var count = productosDisponibles.filter(function (p) {
                return String(p.categoriaId) === String(c.id);
            }).length;
            $sel.find('option[value="' + c.id + '"]').text(c.nombre + ' (' + count + ')');
        });
    }

    function urlImagenCatalogo(item) {
        if (!item) return '';
        var url = item.imagen || '';
        if (!url && item.imagenRuta) {
            url = '/archivos/' + String(item.imagenRuta).replace(/^\/archivos\//, '');
        }
        return url;
    }

    function initCatalogoVenta() {
        $('#pos-catalogo-buscar').val('');
        $('#pos-catalogo-categoria').val('');
        $('#pos-catalogo-grid').html('<div class="pos-catalogo-vacio w-100">Cargando productos...</div>');
        $('#pos-catalogo-count').text('Cargando...');
        cargarCategorias(function () {
            cargarProductos(function () {
                actualizarOpcionesCategoria();
                renderCatalogoGrid();
            });
        });
    }

    function lineaBruto(item) {
        return item.precio * item.cantidad;
    }

    function lineaSubtotal(item) {
        var desc = parseFloat(item.descuento) || 0;
        var sub = lineaBruto(item) - desc;
        return sub < 0 ? 0 : sub;
    }

    function calcularTotales() {
        var bruto = 0;
        var descuentoTotal = 0;
        productosVenta.forEach(function (p) {
            bruto += lineaBruto(p);
            descuentoTotal += parseFloat(p.descuento) || 0;
        });
        var neto = bruto - descuentoTotal;
        if (neto < 0) neto = 0;
        return {
            bruto: bruto,
            descuento: descuentoTotal,
            neto: neto,
            total: neto
        };
    }

    function actualizarResumen() {
        var t = calcularTotales();
        $('#pos-res-subtotal').text('S/ ' + fmt(t.bruto));
        $('#pos-res-descuento').text('- S/ ' + fmt(t.descuento));
        $('#pos-res-total').text('S/ ' + fmt(t.total));
        if ($('#pos-pago-mixto-box').length && !$('#pos-pago-mixto-box').hasClass('d-none')) {
            actualizarSaldoMixto();
        } else if (!esPagoMixtoNombre(nombreTipoPagoActivo())) {
            actualizarQrPago();
        }
    }

    window.agregarProducto = function (itemCatalogo) {
        var presentacionId = itemCatalogo.presentacionId || itemCatalogo.id;
        if (!presentacionId) {
            mostrarToast('Seleccione una presentación del producto.', 'error');
            return;
        }

        var disponible = stockDisponiblePresentacion(presentacionId);
        if (disponible <= 0) {
            mostrarToast('Sin stock disponible para esta presentación.', 'warning');
            return;
        }

        var maxStock = stockMaximoPresentacion(presentacionId);
        var existente = productosVenta.find(function (p) { return p.presentacionId === presentacionId; });
        if (existente) {
            if (existente.cantidad >= maxStock) {
                mostrarToast('No hay más stock disponible para esta presentación.', 'warning');
                return;
            }
            existente.cantidad++;
            existente.stock = maxStock;
        } else {
            productosVenta.push({
                productoId: itemCatalogo.productoId,
                presentacionId: presentacionId,
                productoNombre: itemCatalogo.productoNombre,
                presentacionNombre: itemCatalogo.presentacionNombre,
                medida: itemCatalogo.medida || extraerMedida(itemCatalogo.presentacionNombre).medida,
                marca: itemCatalogo.marca || 'Sin marca',
                imagen: urlImagenCatalogo(itemCatalogo),
                codigoPresentacion: itemCatalogo.codigoPresentacion || '',
                nombre: itemCatalogo.nombre,
                precio: parseFloat(itemCatalogo.precio),
                cantidad: 1,
                stock: maxStock,
                descuento: 0
            });
        }
        renderProductosTabla();
        renderCatalogoGrid();
        mostrarToast('Presentación agregada a la venta.', 'success');
    };

    window.quitarProducto = function (index) {
        productosVenta.splice(index, 1);
        renderProductosTabla();
        renderCatalogoGrid();
    };

    window.cambiarCantidad = function (index, delta) {
        var item = productosVenta[index];
        var nueva = item.cantidad + delta;
        if (nueva < 1) return;
        if (nueva > item.stock) {
            mostrarToast('Stock insuficiente para esta presentación.', 'warning');
            return;
        }
        var otrosEnCarrito = cantidadEnCarrito(item.presentacionId) - item.cantidad;
        var disponible = stockMaximoPresentacion(item.presentacionId) - otrosEnCarrito;
        if (nueva > disponible) {
            mostrarToast('Stock insuficiente para esta presentación.', 'warning');
            return;
        }
        item.cantidad = nueva;
        renderProductosTabla();
        renderCatalogoGrid();
    };

    window.cambiarDescuento = function (index, valor) {
        var item = productosVenta[index];
        var desc = parseFloat(valor);
        if (isNaN(desc) || desc < 0) desc = 0;
        var maxDesc = lineaBruto(item);
        if (desc > maxDesc) desc = maxDesc;
        item.descuento = desc;
        renderProductosTabla();
    };

    window.renderProductosTabla = function () {
        var tbody = $('#venta-productos-body');
        tbody.empty();

        if (productosVenta.length === 0) {
            tbody.append(
                '<tr id="venta-sin-productos">' +
                '<td colspan="6" class="text-center text-muted py-4">' +
                '<i class="fas fa-shopping-basket fa-2x mb-2 d-block opacity-50"></i>' +
                'Aún no hay presentaciones en la venta</td></tr>'
            );
        } else {
            productosVenta.forEach(function (p, i) {
                var subtotal = lineaSubtotal(p);
                var prodNombre = p.productoNombre || (p.nombre ? p.nombre.split(' — ')[0] : '—');
                var medida = p.medida || extraerMedida(p.presentacionNombre).medida;
                var marca = p.marca || 'Sin marca';
                var imgHtml = urlImagenCatalogo(p)
                    ? '<img src="' + escHtml(urlImagenCatalogo(p)) + '" alt="" class="pos-detalle-thumb">'
                    : '<span class="pos-detalle-sin-img"><i class="fas fa-image"></i></span>';
                tbody.append(
                    '<tr>' +
                    '<td><div class="pos-detalle-producto">' + imgHtml +
                    '<div><span class="font-weight-bold d-block">' + escHtml(prodNombre) + '</span>' +
                    '<small class="text-muted d-block"><i class="fas fa-industry mr-1"></i>' + escHtml(marca) + '</small></div></div></td>' +
                    '<td><span class="badge badge-light border text-purple font-weight-bold">' + escHtml(medida) + '</span>' +
                    '<div class="small text-muted mt-1">S/ ' + fmt(p.precio) + ' c/u</div></td>' +
                    '<td class="text-center">' +
                    '<div class="pos-qty-control">' +
                    '<button type="button" onclick="cambiarCantidad(' + i + ',-1)">−</button>' +
                    '<span>' + p.cantidad + '</span>' +
                    '<button type="button" onclick="cambiarCantidad(' + i + ',1)">+</button>' +
                    '</div></td>' +
                    '<td class="text-center">' +
                    '<input type="number" class="pos-desc-input" min="0" step="0.01" value="' + fmt(p.descuento || 0) + '" ' +
                    'onchange="cambiarDescuento(' + i + ', this.value)">' +
                    '</td>' +
                    '<td class="text-center font-weight-bold text-nowrap">S/ ' + fmt(subtotal) + '</td>' +
                    '<td class="text-center">' +
                    '<button type="button" class="btn btn-sm btn-outline-danger" onclick="quitarProducto(' + i + ')" title="Quitar">' +
                    '<i class="fas fa-trash"></i></button></td>' +
                    '</tr>'
                );
            });
        }
        actualizarResumen();
        var count = productosVenta.length;
        var unidades = productosVenta.reduce(function (s, p) { return s + p.cantidad; }, 0);
        $('#pos-items-count').text(count + ' presentación(es) · ' + unidades + ' und.');
    };

    function buscarPorCodigoBarrasExacto(codigo) {
        var limpio = (codigo || '').trim();
        if (!limpio) return null;
        return productosDisponibles.find(function (p) {
            return p.codigoBarras && String(p.codigoBarras).trim() === limpio;
        }) || null;
    }

    function filtrarCatalogo() {
        var q = ($('#pos-catalogo-buscar').val() || '').trim().toLowerCase();
        var catId = $('#pos-catalogo-categoria').val();

        return productosDisponibles.filter(function (p) {
            if (catId && String(p.categoriaId) !== String(catId)) return false;
            if (!q) return true;
            var texto = [
                p.nombre,
                p.productoNombre,
                p.presentacionNombre,
                p.marca,
                p.medida,
                p.unidadMedida,
                p.codigo,
                p.codigoPresentacion,
                p.codigoBarras,
                String(p.productoId),
                p.categoriaNombre
            ].join(' ').toLowerCase();
            return texto.indexOf(q) >= 0;
        });
    }

    function actualizarContadorCatalogo(mostrados) {
        var total = productosDisponibles.length;
        var texto = (mostrados != null ? mostrados : total) + ' de ' + total + ' presentación(es)';
        $('#pos-catalogo-count').text(texto);
    }

    function extraerMedida(nombre) {
        var n = (nombre || '').trim();
        var match = n.match(/^(\d+(?:[.,]\d+)?)\s*(.+)$/);
        if (match) {
            var cantidad = match[1].replace(',', '.');
            var unidad = match[2].trim();
            return { cantidad: cantidad, unidad: unidad, medida: cantidad + ' ' + unidad };
        }
        return { cantidad: '1', unidad: n || '—', medida: n || '—' };
    }

    function crearTarjetaProducto(p) {
        var stockDisp = stockDisponiblePresentacion(p.id);
        var stockTotal = parseInt(p.stock, 10) || 0;
        var sinStock = stockDisp <= 0;
        var imgUrl = urlImagenCatalogo(p);
        var marca = (p.marca && p.marca !== 'Sin marca') ? p.marca : 'Sin marca';
        var medida = p.medida || extraerMedida(p.presentacionNombre).medida;
        var presLabel = p.presentacionNombre || medida;

        var imgBlock = imgUrl
            ? '<img src="' + escHtml(imgUrl) + '" alt="' + escHtml(p.productoNombre || '') + '" loading="lazy">'
            : '<div class="pos-venta-sin-foto"><i class="fas fa-image"></i><span>Sin foto</span></div>';

        var btnAgregar = '<button type="button" class="pos-venta-card__fab pos-btn-agregar-prod" data-id="' + p.id + '"' +
            (sinStock ? ' disabled' : '') + ' title="Agregar a la venta">' +
            '<i class="fas fa-cart-plus"></i></button>';

        return '<article class="pos-venta-card' + (sinStock ? ' pos-venta-card--agotado' : '') + '">' +
            '<div class="pos-venta-card__img">' + imgBlock + btnAgregar + '</div>' +
            '<div class="pos-venta-card__body">' +
            '<div class="pos-venta-card__marca"><i class="fas fa-industry mr-1"></i>' + escHtml(marca) + '</div>' +
            '<h6 class="pos-venta-card__nombre">' + escHtml(p.productoNombre || p.nombre) + '</h6>' +
            '<div class="pos-venta-card__pres"><i class="fas fa-layer-group mr-1"></i>' + escHtml(presLabel) + '</div>' +
            '<div class="pos-venta-card__precio"><span class="pos-venta-card__precio-label">Precio</span> S/ ' + fmt(p.precio) + '</div>' +
            '<div class="pos-venta-card__stock' + (stockDisp <= 5 ? ' pos-venta-card__stock--bajo' : '') + '">' +
            '<i class="fas fa-cubes mr-1"></i>Stock: <strong>' + stockDisp + '</strong>' +
            '<span class="pos-venta-card__stock-total"> / ' + stockTotal + '</span></div>' +
            '</div>' +
            '<button type="button" class="pos-venta-card__btn pos-btn-agregar-prod" data-id="' + p.id + '"' +
            (sinStock ? ' disabled' : '') + ' title="Agregar a la venta">' +
            '<i class="fas fa-cart-plus"></i><span>Agregar</span></button>' +
            '</article>';
    }

    function renderCatalogoGrid() {
        var lista = filtrarCatalogo();
        var $grid = $('#pos-catalogo-grid').empty();
        var catId = $('#pos-catalogo-categoria').val();

        actualizarContadorCatalogo(lista.length);

        if (!lista.length) {
            if (!productosDisponibles.length) {
                $grid.html(
                    '<div class="pos-catalogo-vacio w-100">' +
                    '<i class="fas fa-box-open fa-2x mb-2 d-block opacity-50"></i>' +
                    'No hay presentaciones activas. Registre productos con presentaciones en el módulo Productos.</div>'
                );
            } else {
                $grid.html(
                    '<div class="pos-catalogo-vacio w-100">' +
                    '<i class="fas fa-search fa-2x mb-2 d-block opacity-50"></i>' +
                    'No se encontraron productos con esos filtros.</div>'
                );
            }
            return;
        }

        if (!catId) {
            var grupos = [];
            var mapaGrupos = {};
            lista.forEach(function (p) {
                var catNombre = p.categoriaNombre || 'Sin categoría';
                if (!mapaGrupos[catNombre]) {
                    mapaGrupos[catNombre] = [];
                    grupos.push(catNombre);
                }
                mapaGrupos[catNombre].push(p);
            });

            grupos.forEach(function (catNombre) {
                var items = mapaGrupos[catNombre];
                $grid.append(
                    '<div class="pos-catalogo-grupo-titulo">' +
                    '<i class="fas fa-folder-open mr-1"></i>' + escHtml(catNombre) +
                    ' <span class="badge badge-light border">' + items.length + '</span></div>'
                );
                items.forEach(function (p) {
                    $grid.append(crearTarjetaProducto(p));
                });
            });
            return;
        }

        lista.forEach(function (p) {
            $grid.append(crearTarjetaProducto(p));
        });
    }

    window.renderCatalogoGrid = renderCatalogoGrid;

    function agregarDesdeCatalogo(presentacionId) {
        var p = productosDisponibles.find(function (x) { return x.id === presentacionId; });
        if (!p) return;
        agregarProducto(p);
    }

    function asegurarCliente(callback) {
        var clienteId = $('#venta-cliente-id').val();
        if (clienteId) {
            callback(parseInt(clienteId, 10));
            return;
        }
        var doc = normalizarDocumento($('#venta-documento').val());
        var nombre = $('#venta-nombre-cliente').val().trim();
        if (!documentoValido(doc)) {
            mostrarToast('Ingrese el DNI/RUC del cliente.', 'error');
            return;
        }

        buscarClienteLocal(doc, true, function (local) {
            if (local && local.clienteId) {
                callback(parseInt(local.clienteId, 10));
                return;
            }
            mostrarToast('Registrando cliente...', 'success');
            $.get('/ventas/api/cliente/buscar', { documento: doc, nombre: nombre })
                .done(function (res) {
                    if (res.success && res.clienteId) {
                        aplicarClienteVenta(res);
                        callback(parseInt(res.clienteId, 10));
                    } else if (res.requiereNombre) {
                        bloquearCamposCliente(false);
                        mostrarClienteVerificado(false);
                        $('#venta-nombre-cliente').prop('readonly', false).focus();
                        mostrarToast('Escriba el nombre del cliente y pulse Guardar Venta de nuevo.', 'warning');
                    } else {
                        mostrarToast(res.message || 'No se pudo registrar el cliente.', 'error');
                    }
                })
                .fail(function () {
                    mostrarToast('Error al registrar el cliente.', 'error');
                });
        });
    }

    function enviarVenta(clienteId) {
        if (productosVenta.length === 0) {
            mostrarToast('Agregue al menos un producto a la venta.', 'error');
            return;
        }

        if (productosVenta.some(function (p) { return !p.presentacionId; })) {
            mostrarToast('Cada ítem debe tener una presentación seleccionada.', 'error');
            return;
        }

        var detalles = productosVenta.map(function (p) {
            return {
                productoId: p.productoId,
                presentacionId: p.presentacionId,
                cantidad: p.cantidad,
                precio: p.precio,
                descuento: parseFloat(p.descuento) || 0
            };
        });

        var tipoComprobanteId = parseInt($('#venta-comprobante').val(), 10);
        var tipoPagoId = parseInt($('#venta-tipo-pago').val(), 10);

        if (!tipoComprobanteId || !tipoPagoId) {
            mostrarToast('Seleccione el comprobante y la forma de pago.', 'error');
            return;
        }

        var nombrePago = nombreTipoPagoActivo();
        var codigoVerificacion = ($('#pos-codigo-verificacion').val() || '').trim();
        var montoEfectivo = null;
        var montoYape = null;
        var montoPlin = null;

        if (esPagoMixtoNombre(nombrePago)) {
            var total = calcularTotales().total;
            montoEfectivo = parseFloat($('#pos-mixto-efectivo').val()) || 0;
            montoYape = parseFloat($('#pos-mixto-yape').val()) || 0;
            montoPlin = parseFloat($('#pos-mixto-plin').val()) || 0;
            var suma = montoEfectivo + montoYape + montoPlin;
            if (Math.abs(suma - total) > 0.009) {
                mostrarToast('La suma de los montos debe ser exactamente igual al total.', 'error');
                return;
            }
            if ((montoYape > 0 || montoPlin > 0) && !codigoVerificacion) {
                mostrarToast('Ingrese el código de verificación para Yape/Plin.', 'error');
                return;
            }
        } else if (esPagoDigital(nombrePago)) {
            if (!codigoVerificacion) {
                mostrarToast('Ingrese el código de verificación del pago.', 'error');
                return;
            }
        }

        var editId = $('#venta-edit-id').val();
        var $btn = $('#btn-guardar-venta');
        $btn.prop('disabled', true);

        var payload = {
            clienteId: clienteId,
            tipoPagoId: tipoPagoId,
            detalles: JSON.stringify(detalles),
            codigoVerificacionPago: codigoVerificacion || '',
            montoEfectivo: montoEfectivo,
            montoYape: montoYape,
            montoPlin: montoPlin
        };

        var url = '/ventas/guardar';
        if (editId) {
            url = '/ventas/actualizar';
            payload.ventaId = editId;
        } else {
            payload.tipoComprobanteId = tipoComprobanteId;
        }

        $.ajax({
            url: url,
            method: 'POST',
            dataType: 'json',
            data: payload
        }).done(function (res) {
            if (res.success) {
                window.location.reload();
            } else {
                mostrarToast(res.message || 'No se pudo guardar la venta.', 'error');
                $btn.prop('disabled', false);
            }
        }).fail(function (xhr) {
            var msg = 'Error al guardar la venta.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                msg = xhr.responseJSON.message;
            }
            mostrarToast(msg, 'error');
            $btn.prop('disabled', false);
        });
    }

    window.guardarVenta = function () {
        asegurarCliente(function (clienteId) {
            enviarVenta(clienteId);
        });
    };

    window.sincronizarStockPresentacion = function (presentacionId, stock) {
        productosDisponibles.forEach(function (p) {
            if (p.id === presentacionId || p.presentacionId === presentacionId) {
                p.stock = stock;
            }
        });
        if (window.renderCatalogoGrid) {
            window.renderCatalogoGrid();
        }
    };

    window.sincronizarStockProducto = function (productoId, stock) {
        productosDisponibles.forEach(function (p) {
            if (p.productoId === productoId) {
                p.stock = stock;
            }
        });
    };

    $(document).ready(function () {
        initTiposPago();
        $(document).on('input', '.pos-mixto-input', actualizarSaldoMixto);

        $('#venta-documento').on('keypress', function (e) {
            if (e.which === 13) {
                e.preventDefault();
                buscarCliente();
            }
        });

        $('#venta-documento').on('input blur', function () {
            var doc = normalizarDocumento($(this).val());
            if (doc.length === 8 || doc.length === 11) {
                clearTimeout(ventaBusquedaTimer);
                ventaBusquedaTimer = setTimeout(function () {
                    buscarClienteLocal(doc, true);
                }, $(this).is(':focus') ? 500 : 0);
            } else {
                mostrarClienteVerificado(false);
            }
        });

        $('#pos-catalogo-buscar').on('input', renderCatalogoGrid);
        $('#pos-catalogo-categoria').on('change', renderCatalogoGrid);

        $(document).on('click', '.pos-btn-agregar-prod', function (e) {
            e.stopPropagation();
            agregarDesdeCatalogo(parseInt($(this).data('id'), 10));
        });

        $('#pos-catalogo-buscar').on('keypress', function (e) {
            if (e.which === 13) {
                e.preventDefault();
                var q = ($(this).val() || '').trim();
                var porBarras = buscarPorCodigoBarrasExacto(q);
                if (porBarras) {
                    agregarDesdeCatalogo(porBarras.id);
                    $(this).val('');
                    renderCatalogoGrid();
                    return;
                }
                var lista = filtrarCatalogo();
                if (lista.length === 1) {
                    agregarDesdeCatalogo(lista[0].id);
                    $(this).val('');
                    renderCatalogoGrid();
                }
            }
        });
    });
})();
