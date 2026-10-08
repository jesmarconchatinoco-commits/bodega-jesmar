(function () {
    'use strict';

    var pedidoActualId = null;

    function formatoSoles(valor) {
        var n = parseFloat(valor);
        if (isNaN(n)) return 'S/ 0.00';
        return 'S/ ' + n.toFixed(2);
    }

    function badgeEstado(estado) {
        if (estado === 'ATENDIDO') {
            return '<span class="badge badge-success">Atendido</span>';
        }
        if (estado === 'CANCELADO') {
            return '<span class="badge badge-secondary">Cancelado</span>';
        }
        return '<span class="badge badge-warning">Pendiente</span>';
    }

    function mostrarToast(mensaje, tipo) {
        var cls = tipo === 'success' ? 'alert-success' : (tipo === 'warning' ? 'alert-warning' : 'alert-danger');
        var $toast = $('<div class="alert ' + cls + ' alert-dismissible fade show shadow-sm" role="alert">' +
            mensaje +
            '<button type="button" class="close" data-dismiss="alert"><span>&times;</span></button></div>');
        $('body').append($('<div class="position-fixed" style="top:80px;right:20px;z-index:9999;max-width:380px;"></div>').append($toast));
        setTimeout(function () { $toast.alert('close'); }, 4500);
    }

    function htmlLogoTienda() {
        var logoUrl = window.BOLETA_LOGO_URL;
        return logoUrl
            ? '<img src="' + logoUrl + '" alt="Bodega Jesmar">'
            : '<div class="boleta-logo-fallback"><i class="fas fa-warehouse"></i></div>';
    }

    function renderBoletaElectronica(res) {
        var filas = '';
        (res.productos || []).forEach(function (p) {
            filas += '<tr>' +
                '<td>' + (p.producto || '—') + '</td>' +
                '<td class="text-center">' + p.cantidad + '</td>' +
                '<td class="text-right">' + formatoSoles(p.precio) + '</td>' +
                '<td class="text-right">' + formatoSoles(p.subtotal) + '</td>' +
                '</tr>';
        });
        if (!filas) {
            filas = '<tr><td colspan="4" class="text-center text-muted">Sin productos</td></tr>';
        }

        return '<div class="boleta-logo">' + htmlLogoTienda() + '</div>' +
            '<div class="boleta-empresa">Bodega Jesmar</div>' +
            '<div class="boleta-tipo">' + (res.comprobante || 'Nota de venta') + '</div>' +
            '<div class="boleta-doc">' + (res.numeroDocumento || '—') + '</div>' +
            '<div class="boleta-linea"><span>Cliente:</span><span>' + (res.cliente || '—') + '</span></div>' +
            (res.documentoCliente ? '<div class="boleta-linea"><span>DNI/RUC:</span><span>' + res.documentoCliente + '</span></div>' : '') +
            '<div class="boleta-linea"><span>Fecha:</span><span>' + (res.fecha || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Vendedor:</span><span>' + (res.vendedor || '—') + '</span></div>' +
            '<div class="boleta-linea"><span>Forma de pago:</span><span>' + (res.tipoPago || 'Contado') + '</span></div>' +
            '<div class="boleta-separador"></div>' +
            '<table class="boleta-items">' +
            '<thead><tr><th>Producto</th><th class="text-center">Cant</th><th class="text-right">P.U.</th><th class="text-right">Subt.</th></tr></thead>' +
            '<tbody>' + filas + '</tbody></table>' +
            '<div class="boleta-total">TOTAL: ' + formatoSoles(res.total) + '</div>' +
            '<div class="boleta-pie">Venta generada desde pedido del catálogo<br>¡Gracias por su compra!</div>';
    }

    function labelFormaEntrega(valor) {
        if (valor === 'ENVIO_DOMICILIO') return 'Envío a domicilio';
        if (valor === 'RECOJO_TIENDA') return 'Recojo en tienda';
        return valor || '—';
    }

    function labelMetodoPago(valor) {
        if (valor === 'YAPE') return 'Yape';
        if (valor === 'PLIN') return 'Plin';
        return valor || '—';
    }

    function renderDetallePedido(p) {
        var filas = '';
        (p.detalles || []).forEach(function (d) {
            filas += '<tr>' +
                '<td>' + (d.nombreProducto || '—') + '</td>' +
                '<td class="text-center">' + d.cantidad + '</td>' +
                '<td class="text-right">S/ ' + parseFloat(d.precioUnitario).toFixed(2) + '</td>' +
                '<td class="text-right font-weight-bold">S/ ' + parseFloat(d.subtotal).toFixed(2) + '</td>' +
                '</tr>';
        });
        if (!filas) {
            filas = '<tr><td colspan="4" class="text-center text-muted">Sin productos</td></tr>';
        }

        var ventaHtml = p.ventaId
            ? '<div class="col-md-6 mt-2"><strong>Venta:</strong> <span class="text-success">#' + p.ventaId + '</span></div>'
            : '';

        var entregaHtml = p.formaEntrega
            ? '<div class="col-md-6 mt-2"><strong>Entrega:</strong> ' + labelFormaEntrega(p.formaEntrega) + '</div>'
            : '';

        var recojoHtml = p.horarioRecojoLabel
            ? '<div class="col-12 mt-2"><strong>Recojo programado:</strong> ' + p.horarioRecojoLabel + '</div>'
            : '';

        var pagoHtml = p.metodoPago
            ? '<div class="col-md-6 mt-2"><strong>Pago:</strong> ' + labelMetodoPago(p.metodoPago) + '</div>'
            : '';

        var codigoHtml = p.codigoValidacionPago
            ? '<div class="col-md-6 mt-2"><strong>Código validación:</strong> ' +
              '<span class="badge badge-codigo-validacion">' + p.codigoValidacionPago + '</span></div>'
            : '';

        var direccionHtml = '';
        if (p.formaEntrega === 'ENVIO_DOMICILIO') {
            direccionHtml = '<div class="col-12 mt-2"><strong>Dirección:</strong> ' + (p.direccion || '—');
            if (p.referencia) direccionHtml += ' · Ref: ' + p.referencia;
            if (p.distrito) direccionHtml += ' · Distrito: ' + p.distrito;
            direccionHtml += '</div>';
        }

        var comprobanteHtml = p.comprobanteUrl
            ? '<div class="mt-3"><strong>Comprobante de pago:</strong><br>' +
              '<a href="' + p.comprobanteUrl + '" target="_blank" rel="noopener">' +
              '<img src="' + p.comprobanteUrl + '" alt="Comprobante" style="max-height:160px;border-radius:8px;margin-top:8px;border:1px solid #dee2e6;"></a></div>'
            : '';

        var subtotal = p.subtotal != null ? parseFloat(p.subtotal).toFixed(2) : parseFloat(p.total).toFixed(2);
        var costoEnvio = p.costoEnvio != null ? parseFloat(p.costoEnvio).toFixed(2) : '0.00';
        var envioRow = parseFloat(costoEnvio) > 0
            ? '<tr><td colspan="3" class="text-right">Costo de envío</td><td class="text-right">S/ ' + costoEnvio + '</td></tr>'
            : '';

        var obs = p.observaciones
            ? '<div class="mt-3"><strong>Observaciones:</strong><p class="mb-0 text-muted">' + p.observaciones + '</p></div>'
            : '';

        var movimientosHtml = '';
        if (p.estado === 'ATENDIDO' && p.ventaId && (p.detalles || []).length) {
            movimientosHtml = '<div class="mt-3 p-2 bg-light rounded border">' +
                '<strong><i class="fas fa-boxes mr-1"></i> Movimientos de inventario</strong>' +
                '<ul class="mb-1 mt-2 small">';
            (p.detalles || []).forEach(function (d) {
                movimientosHtml += '<li><strong>' + (d.nombreProducto || 'Producto') + ':</strong> −' +
                    d.cantidad + ' unidad(es) descontadas del stock</li>';
            });
            movimientosHtml += '</ul>' +
                '<p class="mb-0 small text-muted">Venta <strong>#' + p.ventaId + '</strong> registrada en ' +
                '<a href="/inventario">Gestión de Inventario</a>. Consulte el kardex y movimientos detallados desde el módulo Inventario.</p></div>';
        }

        return '<div class="row mb-3">' +
            '<div class="col-md-6"><strong>Pedido:</strong> #' + p.id + '</div>' +
            '<div class="col-md-6 text-md-right"><strong>Estado:</strong> ' + badgeEstado(p.estado) + '</div>' +
            '<div class="col-md-6 mt-2"><strong>Cliente:</strong> ' + (p.nombreCliente || '—') + '</div>' +
            '<div class="col-md-6 mt-2"><strong>Teléfono:</strong> ' + (p.telefono || '—') + '</div>' +
            '<div class="col-md-6 mt-2"><strong>Documento:</strong> ' + (p.documento || '—') + '</div>' +
            '<div class="col-md-6 mt-2"><strong>Fecha:</strong> ' + (p.fecha || '—') + '</div>' +
            entregaHtml + pagoHtml + codigoHtml + recojoHtml + direccionHtml + ventaHtml +
            '</div>' +
            '<div class="table-responsive">' +
            '<table class="table table-bordered table-sm mb-0">' +
            '<thead class="thead-light"><tr>' +
            '<th>Producto</th><th class="text-center">Cant.</th>' +
            '<th class="text-right">Precio</th><th class="text-right">Subtotal</th>' +
            '</tr></thead><tbody>' + filas + '</tbody>' +
            '<tfoot>' +
            '<tr><td colspan="3" class="text-right">Subtotal</td><td class="text-right">S/ ' + subtotal + '</td></tr>' +
            envioRow +
            '<tr class="font-weight-bold">' +
            '<td colspan="3" class="text-right">Total</td>' +
            '<td class="text-right text-primary">S/ ' + parseFloat(p.total).toFixed(2) + '</td>' +
            '</tr></tfoot></table></div>' + comprobanteHtml + movimientosHtml + obs;
    }

    function renderAccionesModal(p) {
        var $footer = $('#pedido-detalle-acciones');
        $footer.find('.btn-atender-modal, .btn-cancelar-modal, .btn-boleta-modal, .btn-editar-comprobante-modal').remove();

        if (p.estado === 'ATENDIDO' && p.ventaId) {
            $footer.prepend(
                '<button type="button" class="btn btn-outline-success btn-boleta-modal mr-1" data-venta-id="' + p.ventaId + '">' +
                '<i class="fas fa-file-invoice mr-1"></i> Ver nota de venta</button>'
            );
        }
    }

    function abrirDetalle(id) {
        pedidoActualId = id;
        $('#pedido-detalle-contenido').html('<p class="text-center text-muted py-4 mb-0">Cargando...</p>');
        $('#modalDetallePedido').modal('show');

        $.get('/pedidos/api/' + id)
            .done(function (p) {
                $('#pedido-detalle-contenido').html(renderDetallePedido(p));
                renderAccionesModal(p);
            })
            .fail(function () {
                $('#pedido-detalle-contenido').html(
                    '<p class="text-danger text-center py-4 mb-0">No se pudo cargar el pedido.</p>'
                );
            });
    }

    function cargarBoletaVenta(ventaId) {
        $('#pedido-boleta-contenido').html('<p class="text-center text-muted py-4 mb-0">Cargando nota de venta...</p>');
        $('#modalBoletaPedido').modal('show');

        $.get('/ventas/api/' + ventaId + '/detalle')
            .done(function (res) {
                $('#pedido-boleta-contenido').html(renderBoletaElectronica(res));
            })
            .fail(function () {
                $('#pedido-boleta-contenido').html(
                    '<p class="text-danger text-center py-4 mb-0">No se pudo cargar la nota de venta.</p>'
                );
            });
    }

    window.imprimirBoletaPedido = function () {
        document.body.classList.add('boleta-imprimiendo');
        window.print();
        setTimeout(function () {
            document.body.classList.remove('boleta-imprimiendo');
        }, 500);
    };

    function atenderPedido(id) {
        if (!confirm('¿Validar este pedido?\n\nSe registrará la venta en la tabla de Ventas, se descontará el stock de cada producto, se generará la nota de venta y quedarán registrados los movimientos de inventario.')) {
            return;
        }
        $.post('/pedidos/api/' + id + '/atender')
            .done(function (res) {
                var msg = '<i class="fas fa-check-circle mr-1"></i>' + res.message;
                if (res.ventaId) {
                    msg += ' <a href="/ventas" class="alert-link font-weight-bold">Ir a Ventas</a>';
                }
                mostrarToast(msg, 'success');
                $('#modalDetallePedido').modal('hide');
                if (res.ventaId) {
                    cargarBoletaVenta(res.ventaId);
                }
                setTimeout(function () { window.location.reload(); }, 3500);
            })
            .fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message)
                    ? xhr.responseJSON.message
                    : 'No se pudo atender el pedido.';
                mostrarToast('<i class="fas fa-exclamation-circle mr-1"></i>' + msg, 'danger');
            });
    }

    function cancelarPedido(id) {
        if (!confirm('¿Cancelar este pedido?')) {
            return;
        }
        $.post('/pedidos/api/' + id + '/cancelar')
            .done(function (res) {
                mostrarToast('<i class="fas fa-check-circle mr-1"></i>' + res.message, 'success');
                setTimeout(function () { window.location.reload(); }, 800);
            })
            .fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message)
                    ? xhr.responseJSON.message
                    : 'No se pudo cancelar el pedido.';
                mostrarToast('<i class="fas fa-exclamation-circle mr-1"></i>' + msg, 'danger');
            });
    }

    function abrirEditarComprobante(id, url) {
        $('#comprobantePedidoId').val(id);
        $('#inputComprobantePedido').val('');
        $('.custom-file-label').text('Elegir imagen JPG o PNG');

        if (url) {
            $('#previewComprobantePedido').attr('src', url).show();
            $('#sinComprobantePedido').hide();
        } else {
            $('#previewComprobantePedido').hide().attr('src', '');
            $('#sinComprobantePedido').show();
        }
        $('#modalEditarComprobante').modal('show');
    }

    function guardarComprobantePedido() {
        var id = $('#comprobantePedidoId').val();
        var fileInput = document.getElementById('inputComprobantePedido');
        if (!fileInput || !fileInput.files || !fileInput.files[0]) {
            mostrarToast('Seleccione una imagen JPG o PNG.', 'warning');
            return;
        }

        var formData = new FormData();
        formData.append('comprobante', fileInput.files[0]);

        $('#btnGuardarComprobantePedido').prop('disabled', true);
        $.ajax({
            url: '/pedidos/api/' + id + '/comprobante',
            method: 'POST',
            data: formData,
            processData: false,
            contentType: false
        }).done(function (res) {
            mostrarToast('<i class="fas fa-check-circle mr-1"></i>' + res.message, 'success');
            $('#modalEditarComprobante').modal('hide');
            setTimeout(function () { window.location.reload(); }, 800);
        }).fail(function (xhr) {
            var msg = (xhr.responseJSON && xhr.responseJSON.message)
                ? xhr.responseJSON.message
                : 'No se pudo actualizar el comprobante.';
            mostrarToast('<i class="fas fa-exclamation-circle mr-1"></i>' + msg, 'danger');
        }).always(function () {
            $('#btnGuardarComprobantePedido').prop('disabled', false);
        });
    }

    $(document).ready(function () {
        $('#filtroEstado').on('change', function () {
            var estado = $(this).val();
            var url = estado && estado !== 'TODOS' ? '/pedidos?estado=' + estado : '/pedidos';
            window.location.href = url;
        });

        $(document).on('click', '.btn-ver-pedido', function () {
            abrirDetalle($(this).data('id'));
        });

        $(document).on('click', '.btn-ver-pedido-modal', function () {
            abrirDetalle($(this).data('id'));
        });

        $(document).on('click', '.btn-ver-venta-pedido', function () {
            var ventaId = $(this).data('venta-id');
            var id = $(this).data('id');
            if (ventaId) {
                cargarBoletaVenta(ventaId);
            } else {
                abrirDetalle(id);
            }
        });

        $(document).on('click', '.btn-atender-pedido', function () {
            atenderPedido($(this).data('id'));
        });

        $(document).on('click', '.btn-cancelar-pedido', function () {
            cancelarPedido($(this).data('id'));
        });

        $(document).on('click', '.btn-boleta-modal', function () {
            var ventaId = $(this).data('venta-id');
            if (ventaId) cargarBoletaVenta(ventaId);
        });

        $(document).on('click', '.btn-editar-comprobante', function () {
            abrirEditarComprobante($(this).data('id'), $(this).data('url') || '');
        });

        $(document).on('click', '.btn-ver-comprobante, .pedido-comprobante-thumb', function (e) {
            e.preventDefault();
            var url = $(this).data('url') || $(this).attr('src');
            if (url) {
                $('#imgVerComprobante').attr('src', url);
                $('#modalVerComprobante').modal('show');
            }
        });

        $('#inputComprobantePedido').on('change', function () {
            var file = this.files && this.files[0];
            if (!file) return;
            var label = file.name;
            $(this).next('.custom-file-label').text(label);
            var reader = new FileReader();
            reader.onload = function (e) {
                $('#previewComprobantePedido').attr('src', e.target.result).show();
                $('#sinComprobantePedido').hide();
            };
            reader.readAsDataURL(file);
        });

        $('#btnGuardarComprobantePedido').on('click', guardarComprobantePedido);
    });
})();
