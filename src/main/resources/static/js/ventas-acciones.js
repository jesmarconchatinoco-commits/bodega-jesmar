(function () {
    var ventaDetalleActual = null;

    function formatoSoles(valor) {
        var n = parseFloat(valor);
        if (isNaN(n)) n = 0;
        return 'S/ ' + n.toFixed(2);
    }

    function ventaIdDesdeBtn(btn) {
        return $(btn).data('venta-id');
    }

    function escHtml(s) {
        return String(s || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function filaInventarioHtml(p) {
        var stockCls = p.bajo ? 'inventario-stock-bajo' : 'inventario-stock-ok';
        return '<tr data-presentacion-id="' + p.presentacionId + '" data-producto-id="' + p.productoId + '" data-stock-minimo="' + p.stockMinimo + '" data-stock-actual="' + p.stock + '">' +
            '<td class="align-middle">' + escHtml(p.productoNombre || p.nombre) + '</td>' +
            '<td class="align-middle"><span class="badge badge-light border">' + escHtml(p.presentacionNombre || '—') + '</span></td>' +
            '<td class="align-middle inventario-stock-cell">' +
            '<div class="input-group input-group-sm inventario-stock-edit">' +
            '<input type="number" class="form-control inventario-stock-input ' + stockCls + '" min="0" step="1" value="' + p.stock + '">' +
            '<div class="input-group-append">' +
            '<button type="button" class="btn btn-outline-success btn-guardar-stock" title="Guardar stock">' +
            '<i class="fas fa-save"></i></button>' +
            '</div></div>' +
            '<small class="inventario-stock-msg"></small>' +
            '</td>' +
            '<td class="align-middle text-center">' + p.stockMinimo + '</td>' +
            '<td class="align-middle text-center">' +
            '<button type="button" class="btn btn-sm btn-info btn-movimientos" ' +
            'data-presentacion-id="' + p.presentacionId + '" ' +
            'data-producto-nombre="' + escHtml(p.productoNombre || '') + '" ' +
            'data-presentacion-nombre="' + escHtml(p.presentacionNombre || '') + '">' +
            '<i class="fas fa-chart-bar mr-1"></i>Movimientos</button>' +
            '</td></tr>';
    }

    function actualizarEstiloStockInput($input, bajo) {
        $input.toggleClass('inventario-stock-bajo', !!bajo)
            .toggleClass('inventario-stock-ok', !bajo);
    }

    function guardarStockInventario($row) {
        var presentacionId = $row.data('presentacion-id');
        var $input = $row.find('.inventario-stock-input');
        var $msg = $row.find('.inventario-stock-msg');
        var $btn = $row.find('.btn-guardar-stock');
        var stockActual = parseInt($row.data('stock-actual'), 10);
        var cantidadAgregar = parseInt($input.val(), 10);

        if (isNaN(stockActual) || stockActual < 0) {
            stockActual = 0;
        }
        if (isNaN(cantidadAgregar) || cantidadAgregar <= 0) {
            $msg.removeClass('text-success text-muted').addClass('text-danger')
                .text('Ingrese la cantidad a agregar (mayor a 0).').show();
            $input.focus();
            return;
        }
        if (!$input.data('editando') && cantidadAgregar === stockActual) {
            $msg.removeClass('text-success text-muted').addClass('text-muted')
                .text('Haga clic en el cuadro, ingrese la cantidad a agregar y guarde.').show();
            $input.focus();
            return;
        }

        var nuevoStock = stockActual + cantidadAgregar;

        $btn.prop('disabled', true);
        $msg.hide().text('');
        $.post('/ventas/api/inventario/presentaciones/' + presentacionId + '/stock', { stock: nuevoStock })
            .done(function (res) {
                if (res.success) {
                    $input.val(res.stock).data('editando', false);
                    $row.data('stock-actual', res.stock);
                    actualizarEstiloStockInput($input, res.bajo);
                    $msg.removeClass('text-danger text-muted').addClass('text-success')
                        .text('Stock total: ' + stockActual + ' + ' + cantidadAgregar + ' = ' + res.stock).show();
                    if (window.sincronizarStockPresentacion) {
                        window.sincronizarStockPresentacion(res.presentacionId, res.stock);
                    }
                    setTimeout(function () { $msg.fadeOut(200, function () { $(this).text('').show(); }); }, 2500);
                }
            })
            .fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message)
                    ? xhr.responseJSON.message
                    : 'No se pudo guardar el stock.';
                $msg.removeClass('text-success text-muted').addClass('text-danger').text(msg).show();
            })
            .always(function () {
                $btn.prop('disabled', false);
            });
    }

    window.abrirInventario = function () {
        window.location.href = '/inventario';
    };

    $(document).on('click', '.btn-guardar-stock', function () {
        guardarStockInventario($(this).closest('tr'));
    });

    $(document).on('focus', '.inventario-stock-input', function () {
        var $input = $(this);
        if (!$input.data('editando')) {
            $input.data('editando', true).val('');
        }
    });

    $(document).on('blur', '.inventario-stock-input', function () {
        var $input = $(this);
        if ($input.data('editando') && ($input.val() === '' || $input.val() == null)) {
            var stockActual = $input.closest('tr').data('stock-actual');
            $input.val(stockActual).data('editando', false);
        }
    });

    $(document).on('keypress', '.inventario-stock-input', function (e) {
        if (e.which === 13) {
            e.preventDefault();
            guardarStockInventario($(this).closest('tr'));
        }
    });

    function renderBoletaElectronica(res) {
        var logoHtml = htmlLogoTienda();

        var filas = '';
        (res.productos || []).forEach(function (p) {
            filas += '<tr>' +
                '<td>' + p.producto + '</td>' +
                '<td class="text-center">' + p.cantidad + '</td>' +
                '<td class="text-right">' + formatoSoles(p.precio) + '</td>' +
                '<td class="text-right">' + formatoSoles(p.subtotal) + '</td>' +
                '</tr>';
        });
        if (!filas) {
            filas = '<tr><td colspan="4" class="text-center text-muted">Sin productos</td></tr>';
        }

        var deudaHtml = parseFloat(res.deuda) > 0
            ? '<div class="boleta-deuda">Deuda pendiente: ' + formatoSoles(res.deuda) + '</div>'
            : '';
        var pagoInicialHtml = res.pagoInicial && parseFloat(res.pagoInicial) > 0
            ? '<div class="boleta-linea"><span>Pago inicial:</span><span>' + formatoSoles(res.pagoInicial) + '</span></div>'
            : '';

        return '<div class="boleta-logo">' + logoHtml + '</div>' +
            '<div class="boleta-empresa">Bodega Jesmar</div>' +
            '<div class="boleta-tipo">' + (res.comprobante || 'Nota de venta') + '</div>' +
            '<div class="boleta-doc">' + (res.numeroDocumento || '—') + '</div>' +
            '<div class="boleta-linea"><span>Cliente:</span><span>' + (res.cliente || '—') + '</span></div>' +
            (res.documentoCliente ? '<div class="boleta-linea"><span>DNI/RUC:</span><span>' + res.documentoCliente + '</span></div>' : '') +
            '<div class="boleta-linea"><span>Fecha:</span><span>' + (res.fecha || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Vendedor:</span><span>' + (res.vendedor || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Forma de pago:</span><span>' + (res.tipoPago || '—') + '</span></div>' +
            (res.origenVenta ? '<div class="boleta-linea"><span>Origen:</span><span>' + res.origenVenta + '</span></div>' : '') +
            (res.codigoVerificacionPago && res.codigoVerificacionPago !== '—'
                ? '<div class="boleta-linea"><span>Cód. verificación:</span><span>' + res.codigoVerificacionPago + '</span></div>' : '') +
            '<div class="boleta-linea"><span>Comprobante:</span><span>' + (res.comprobante || '—') + '</span></div>' +
            pagoInicialHtml +
            '<div class="boleta-separador"></div>' +
            '<table class="boleta-items">' +
            '<thead><tr><th>Producto</th><th class="text-center">Cant</th><th class="text-right">P.U.</th><th class="text-right">Subt.</th></tr></thead>' +
            '<tbody>' + filas + '</tbody></table>' +
            '<div class="boleta-total">TOTAL: ' + formatoSoles(res.total) + '</div>' +
            deudaHtml +
            '<div class="boleta-pie">Nota de venta<br>¡Gracias por su compra!</div>';
    }

    function elevarModalAlFrente($modal) {
        $('.dropdown-menu.show').removeClass('show');
        $('.dropdown.show').removeClass('show');

        if (!$modal.parent().is('body')) {
            $modal.appendTo('body');
        }

        $modal.off('show.bs.modal.frente shown.bs.modal.frente hidden.bs.modal.frente');

        $modal.on('show.bs.modal.frente', function () {
            var zIndex = 1050 + (10 * $('.modal:visible').length) + 10;
            $(this).css('z-index', zIndex);
        });

        $modal.on('shown.bs.modal.frente', function () {
            var zIndex = parseInt($(this).css('z-index'), 10) || 1060;
            $('.modal-backdrop').not('.modal-stack').last()
                .css('z-index', zIndex - 1)
                .addClass('modal-stack');
            $(this).css('z-index', zIndex);
        });

        $modal.on('hidden.bs.modal.frente', function () {
            $(this).css('z-index', '');
            if ($('.modal:visible').length) {
                $('body').addClass('modal-open');
            }
        });
    }

    function htmlLogoTienda() {
        var logoUrl = window.BOLETA_LOGO_URL;
        return logoUrl
            ? '<img src="' + logoUrl + '" alt="Bodega Jesmar">'
            : '<div class="boleta-logo-fallback"><i class="fas fa-warehouse"></i></div>';
    }

    function mostrarModalAlFrente($modal) {
        elevarModalAlFrente($modal);
        $modal.modal('show');
    }

    function mostrarBoletaAlFrente() {
        mostrarModalAlFrente($('#modalBoleta'));
    }

    function cargarBoletaVenta(id) {
        $('#boleta-contenido').html('<p class="text-center text-muted py-4 mb-0">Cargando nota de venta...</p>');
        mostrarBoletaAlFrente();

        $.get('/ventas/api/' + id + '/detalle')
            .done(function (res) {
                ventaDetalleActual = res.id;
                $('#boleta-contenido').html(renderBoletaElectronica(res));
            })
            .fail(function () {
                $('#boleta-contenido').html('<p class="text-danger text-center py-4 mb-0">No se pudo cargar la nota de venta.</p>');
            });
    }

    window.abrirBoletaElectronica = function (btn) {
        var id = ventaIdDesdeBtn(btn);
        if (id) {
            cargarBoletaVenta(id);
        }
    };

    window.abrirBoletaDesdeDetalle = function () {
        if (ventaDetalleActual) {
            cargarBoletaVenta(ventaDetalleActual);
        }
    };

    window.imprimirBoletaElectronica = function () {
        document.body.classList.add('boleta-imprimiendo');
        window.print();
        setTimeout(function () {
            document.body.classList.remove('boleta-imprimiendo');
        }, 500);
    };

    window.imprimirVentaPdf = window.abrirBoletaElectronica;

    $(document).on('click', '.btn-movimientos', function () {
        var presentacionId = $(this).data('presentacion-id');
        var productoNombre = $(this).data('producto-nombre');
        var presentacionNombre = $(this).data('presentacion-nombre');
        $('#mov-presentacion-id').val(presentacionId);
        $('#mov-producto-nombre').text(productoNombre || '—');
        $('#mov-presentacion-nombre').text(presentacionNombre || '—');
        $('#mov-tbody').html('<tr><td colspan="5" class="text-center text-muted">Cargando...</td></tr>');
        $('#mov-total').text('S/ 0.00');
        $('#modalMovimientos').modal('show');

        $.get('/ventas/api/inventario/presentaciones/' + presentacionId + '/movimientos')
            .done(function (res) {
                $('#mov-producto-nombre').text(res.producto || productoNombre);
                $('#mov-presentacion-nombre').text(res.presentacion || presentacionNombre);
                $('#mov-total').text(formatoSoles(res.totalVendido));
                var tbody = $('#mov-tbody').empty();
                if (!res.movimientos || res.movimientos.length === 0) {
                    tbody.append('<tr><td colspan="5" class="text-center text-muted">Sin movimientos de venta.</td></tr>');
                    return;
                }
                res.movimientos.forEach(function (m) {
                    tbody.append(
                        '<tr>' +
                        '<td>' + m.fecha + '</td>' +
                        '<td>' + m.numeroDocumento + '</td>' +
                        '<td>' + formatoSoles(m.precio) + '</td>' +
                        '<td>' + m.cantidad + '</td>' +
                        '<td>' + formatoSoles(m.subtotal) + '</td>' +
                        '</tr>'
                    );
                });
            })
            .fail(function () {
                $('#mov-tbody').html('<tr><td colspan="5" class="text-center text-danger">No se pudieron cargar los movimientos.</td></tr>');
            });
    });

    window.abrirDetalle = function (btn) {
        var id = ventaIdDesdeBtn(btn);
        ventaDetalleActual = id;
        $('#detalle-contenido').html('<p class="text-muted text-center">Cargando...</p>');
        $('#modalDetalle').modal('show');

        $.get('/ventas/api/' + id + '/detalle')
            .done(function (res) {
                ventaDetalleActual = res.id;
                var filas = '';
                (res.productos || []).forEach(function (p) {
                    filas += '<tr>' +
                        '<td>' + p.producto + '</td>' +
                        '<td class="text-center">' + p.cantidad + '</td>' +
                        '<td class="text-right">' + formatoSoles(p.precio) + '</td>' +
                        '<td class="text-right">' + formatoSoles(p.subtotal) + '</td>' +
                        '</tr>';
                });
                if (!filas) {
                    filas = '<tr><td colspan="4" class="text-center text-muted">Sin productos</td></tr>';
                }

                $('#detalle-contenido').html(
                    '<div class="mb-3">' +
                    '<div><strong>Documento:</strong> ' + res.numeroDocumento + '</div>' +
                    '<div><strong>Cliente:</strong> ' + res.cliente + (res.documentoCliente ? ' (' + res.documentoCliente + ')' : '') + '</div>' +
                    '<div><strong>Fecha:</strong> ' + res.fecha + '</div>' +
                    '<div><strong>Vendedor:</strong> ' + res.vendedor + '</div>' +
                    '<div><strong>Forma de pago:</strong> ' + res.tipoPago + ' · <strong>Comprobante:</strong> ' + res.comprobante + '</div>' +
                    '<div><strong>Origen de la venta:</strong> ' + (res.origenVenta || 'POS') + '</div>' +
                    (res.codigoVerificacionPago && res.codigoVerificacionPago !== '—'
                        ? '<div><strong>Cód. verificación:</strong> ' + res.codigoVerificacionPago + '</div>' : '') +
                    '</div>' +
                    '<div class="table-responsive">' +
                    '<table class="table table-bordered table-sm">' +
                    '<thead class="thead-light"><tr><th>Producto</th><th class="text-center">Cant.</th><th class="text-right">Precio</th><th class="text-right">Subtotal</th></tr></thead>' +
                    '<tbody>' + filas + '</tbody>' +
                    '</table></div>' +
                    '<div class="text-right font-weight-bold h5">Total: ' + formatoSoles(res.total) + '</div>' +
                    (parseFloat(res.deuda) > 0 ? '<div class="text-right text-danger">Deuda pendiente: ' + formatoSoles(res.deuda) + '</div>' : '')
                );
            })
            .fail(function () {
                $('#detalle-contenido').html('<p class="text-danger text-center">No se pudo cargar el detalle de la venta.</p>');
            });
    };

    window.exportarVentaExcel = function () {
        if (!ventaDetalleActual) return;
        window.location.href = '/ventas/' + ventaDetalleActual + '/export/excel';
    };

    window.exportarVentaPdf = window.abrirBoletaDesdeDetalle;

    function renderInventarioPdf(items) {
        var filas = '';
        var bajos = 0;
        (items || []).forEach(function (p) {
            if (p.bajo) bajos++;
            filas += '<tr' + (p.bajo ? ' class="text-danger"' : '') + '>' +
                '<td>' + escHtml(p.productoNombre || p.nombre) + '</td>' +
                '<td>' + escHtml(p.presentacionNombre || '—') + '</td>' +
                '<td class="text-center">' + p.stock + '</td>' +
                '<td class="text-center">' + p.stockMinimo + '</td>' +
                '<td class="text-center">' + (p.bajo ? 'Bajo' : 'Normal') + '</td>' +
                '</tr>';
        });
        if (!filas) {
            filas = '<tr><td colspan="5" class="text-center text-muted">No hay presentaciones activas</td></tr>';
        }

        var fechaReporte = new Date().toLocaleString('es-PE');

        return '<div class="boleta-logo">' + htmlLogoTienda() + '</div>' +
            '<div class="boleta-empresa">Bodega Jesmar</div>' +
            '<div class="boleta-tipo">Reporte de inventario por presentación</div>' +
            '<div class="boleta-linea"><span>Presentaciones:</span><span>' + (items ? items.length : 0) + '</span></div>' +
            '<div class="boleta-linea"><span>Stock bajo:</span><span>' + bajos + '</span></div>' +
            '<div class="boleta-linea"><span>Generado:</span><span>' + fechaReporte + '</span></div>' +
            '<div class="boleta-separador"></div>' +
            '<table class="boleta-items reporte-movimientos-tabla">' +
            '<thead><tr>' +
            '<th>Producto</th><th>Presentación</th><th class="text-center">Stock</th><th class="text-center">Mínimo</th><th class="text-center">Estado</th>' +
            '</tr></thead><tbody>' + filas + '</tbody></table>' +
            '<div class="boleta-pie">Documento generado electrónicamente · Bodega Jesmar</div>';
    }

    window.exportarInventarioExcel = function () {
        window.location.href = '/ventas/api/inventario/export/excel';
    };

    window.exportarInventarioPdf = function () {
        $('#inventario-pdf-contenido').html('<p class="text-center text-muted py-4 mb-0">Cargando reporte...</p>');
        mostrarModalAlFrente($('#modalInventarioPdf'));

        $.get('/ventas/api/inventario')
            .done(function (data) {
                $('#inventario-pdf-contenido').html(renderInventarioPdf(data));
            })
            .fail(function () {
                $('#inventario-pdf-contenido').html(
                    '<p class="text-danger text-center py-4 mb-0">No se pudo cargar el reporte de inventario.</p>'
                );
            });
    };

    window.imprimirInventarioPdf = function () {
        document.body.classList.add('inventario-pdf-imprimiendo');
        window.print();
        setTimeout(function () {
            document.body.classList.remove('inventario-pdf-imprimiendo');
        }, 500);
    };

    window.exportarMovimientosExcel = function () {
        var presentacionId = $('#mov-presentacion-id').val();
        if (!presentacionId) return;
        window.location.href = '/ventas/api/inventario/presentaciones/' + presentacionId + '/movimientos/export/excel';
    };

    function renderMovimientosPdf(res) {
        var filas = '';
        (res.movimientos || []).forEach(function (m) {
            filas += '<tr>' +
                '<td>' + m.fecha + '</td>' +
                '<td>' + m.numeroDocumento + '</td>' +
                '<td class="text-right">' + formatoSoles(m.precio) + '</td>' +
                '<td class="text-center">' + m.cantidad + '</td>' +
                '<td class="text-right">' + formatoSoles(m.subtotal) + '</td>' +
                '</tr>';
        });
        if (!filas) {
            filas = '<tr><td colspan="5" class="text-center text-muted">Sin movimientos de venta</td></tr>';
        }

        var fechaReporte = new Date().toLocaleString('es-PE');

        return '<div class="boleta-logo">' + htmlLogoTienda() + '</div>' +
            '<div class="boleta-empresa">Bodega Jesmar</div>' +
            '<div class="boleta-tipo">Reporte de movimientos por presentación</div>' +
            '<div class="boleta-linea"><span>Producto:</span><span>' + (res.producto || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Presentación:</span><span>' + (res.presentacion || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Stock actual:</span><span>' + (res.stock != null ? res.stock : '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Generado:</span><span>' + fechaReporte + '</span></div>' +
            '<div class="boleta-separador"></div>' +
            '<table class="boleta-items reporte-movimientos-tabla">' +
            '<thead><tr>' +
            '<th>Fecha</th><th>Documento</th><th class="text-right">Precio</th><th class="text-center">Cant.</th><th class="text-right">Subtotal</th>' +
            '</tr></thead><tbody>' + filas + '</tbody></table>' +
            '<div class="boleta-total text-success">Total vendido: ' + formatoSoles(res.totalVendido) + '</div>' +
            '<div class="boleta-pie">Documento generado electrónicamente · Bodega Jesmar</div>';
    }

    window.exportarMovimientosPdf = function () {
        var presentacionId = $('#mov-presentacion-id').val();
        if (!presentacionId) return;

        $('#movimientos-pdf-contenido').html('<p class="text-center text-muted py-4 mb-0">Cargando reporte...</p>');
        mostrarModalAlFrente($('#modalMovimientosPdf'));

        $.get('/ventas/api/inventario/presentaciones/' + presentacionId + '/movimientos')
            .done(function (res) {
                $('#movimientos-pdf-contenido').html(renderMovimientosPdf(res));
            })
            .fail(function () {
                $('#movimientos-pdf-contenido').html(
                    '<p class="text-danger text-center py-4 mb-0">No se pudo cargar el reporte de movimientos.</p>'
                );
            });
    };

    window.imprimirMovimientosPdf = function () {
        document.body.classList.add('movimientos-pdf-imprimiendo');
        window.print();
        setTimeout(function () {
            document.body.classList.remove('movimientos-pdf-imprimiendo');
        }, 500);
    };
})();
