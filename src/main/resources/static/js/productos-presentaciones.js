(function () {

    'use strict';



    var productoPresentacionesId = null;

    var productoActual = null;

    var presentacionImagenesId = null;

    var presentacionesCache = [];

    var filtroBusqueda = '';



    function fmtPrecio(val) {

        return parseFloat(val || 0).toFixed(2);

    }



    function escHtml(text) {

        return $('<div>').text(text || '').html();

    }



    function extraerCantidadUnidad(nombre) {

        var n = (nombre || '').trim();

        var match = n.match(/^(\d+(?:[.,]\d+)?)\s*(.+)$/);

        if (match) {

            return {

                cantidad: match[1].replace(',', '.'),

                unidad: match[2].trim()

            };

        }

        return { cantidad: '1', unidad: n || 'Und' };

    }



    function calcularGanancia(precioVenta, precioCompra) {

        return parseFloat(precioVenta || 0) - parseFloat(precioCompra || 0);

    }



    function rutaPublica(ruta) {
        return (ruta || '').replace(/^\/archivos\//, '');
    }

    function thumbHtml(p, cssClass) {

        var cls = cssClass || 'pres-thumb';

        var ruta = rutaPublica(p.imagenPrincipal || '');

        if (!ruta && p.imagenes && p.imagenes.length) {

            ruta = rutaPublica(p.imagenes[0].ruta || '');

        }

        if (ruta) {

            return '<img src="/archivos/' + escHtml(ruta) + '" alt="" class="' + cls + '">';

        }

        return '<span class="text-muted"><i class="fas fa-image fa-lg"></i></span>';

    }



    function badgeEstadoPresentacion(p) {

        if (p.estado === 'INACTIVO') {

            return '<span class="badge pres-badge-inactivo">Inactivo</span>';

        }

        var stock = p.stock != null ? p.stock : 0;

        var stockMin = p.stockMinimo != null ? p.stockMinimo : 0;

        if (stock <= 0) {

            return '<span class="badge pres-badge-sin-stock">Sin Stock</span>';

        }

        if (stockMin > 0 && stock <= stockMin) {

            return '<span class="badge pres-badge-stock-bajo">Stock Bajo</span>';

        }

        return '<span class="badge pres-badge-activo">Activo</span>';

    }



    function badgeEstadoProducto(estado) {

        if (estado === 'INACTIVO') {

            return '<span class="badge badge-secondary"><i class="fas fa-ban mr-1"></i>Inactivo</span>';

        }

        return '<span class="badge badge-success"><i class="fas fa-check mr-1"></i>Activo</span>';

    }



    function calcularResumen(lista) {

        var stockTotal = 0;

        var valorInventario = 0;

        (lista || []).forEach(function (p) {

            var stock = p.stock != null ? p.stock : 0;

            stockTotal += stock;

            valorInventario += stock * parseFloat(p.precioCompra || 0);

        });

        return {

            total: (lista || []).length,

            stockTotal: stockTotal,

            valorInventario: valorInventario

        };

    }



    function actualizarResumenProducto(lista) {

        var resumen = calcularResumen(lista);

        $('#pres-resumen-count, #pres-kpi-total').text(resumen.total);

        $('#pres-resumen-stock, #pres-kpi-stock').text(resumen.stockTotal);

        $('#pres-kpi-valor').text('S/ ' + fmtPrecio(resumen.valorInventario));



        if (productoActual) {

            productoActual.stockTotal = resumen.stockTotal;

            productoActual.totalPresentaciones = resumen.total;

        }

    }



    function actualizarImagenResumen(lista) {

        var imgWrap = $('#pres-resumen-img').empty();

        var ruta = '';

        if (lista && lista.length) {

            for (var i = 0; i < lista.length; i++) {

                if (lista[i].imagenPrincipal) {

                    ruta = rutaPublica(lista[i].imagenPrincipal);

                    break;

                }

                if (lista[i].imagenes && lista[i].imagenes.length && lista[i].imagenes[0].ruta) {

                    ruta = rutaPublica(lista[i].imagenes[0].ruta);

                    break;

                }

            }

        }

        if (!ruta && productoActual && productoActual.imagenPrincipal) {

            ruta = rutaPublica(productoActual.imagenPrincipal);

        }

        if (ruta) {

            imgWrap.append($('<img>').attr('src', '/archivos/' + ruta).attr('alt', ''));

        } else {

            imgWrap.html('<i class="fas fa-box-open"></i>');

        }

    }



    function abrirModalPresentaciones(id, nombre) {

        productoPresentacionesId = id;

        productoActual = (typeof productosData !== 'undefined')

            ? productosData.find(function (x) { return x.id === id; })

            : null;

        if (!productoActual) {

            productoActual = { id: id, nombre: nombre || '' };

        }



        $('#pres-prod-nombre').text(productoActual.nombre || nombre || '—');

        $('#pres-prod-categoria').text(productoActual.categoriaNombre || '—');

        $('#pres-prod-marca').text(productoActual.descripcion || 'Sin marca');

        $('#pres-prod-estado-wrap').html(badgeEstadoProducto(productoActual.estado || 'ACTIVO'));



        filtroBusqueda = '';

        $('#pres-buscar').val('');

        $('#pres-lista-msg').hide().text('');

        $('#modalPresentaciones').modal('show');

        cargarPresentaciones();

    }



    function cargarPresentaciones() {

        if (!productoPresentacionesId) return;

        $('#pres-tabla-body').html('<tr><td colspan="12" class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin mr-1"></i>Cargando...</td></tr>');



        $.get('/productos/api/' + productoPresentacionesId + '/presentaciones')

            .done(function (data) {

                presentacionesCache = data || [];

                actualizarResumenProducto(presentacionesCache);

                actualizarImagenResumen(presentacionesCache);

                renderTablaPresentaciones();

            })

            .fail(function () {

                $('#pres-tabla-body').html('<tr><td colspan="12" class="text-center text-danger py-4">No se pudieron cargar las presentaciones.</td></tr>');

            });

    }



    function filtrarPresentaciones(lista) {

        if (!filtroBusqueda) return lista;

        var q = filtroBusqueda.toLowerCase();

        return lista.filter(function (p) {

            var cu = extraerCantidadUnidad(p.nombre);

            return (p.nombre || '').toLowerCase().indexOf(q) >= 0

                || (p.codigoBarras || '').toLowerCase().indexOf(q) >= 0

                || cu.unidad.toLowerCase().indexOf(q) >= 0;

        });

    }



    function renderTablaPresentaciones() {

        var tbody = $('#pres-tabla-body').empty();

        var lista = filtrarPresentaciones(presentacionesCache);



        if (!presentacionesCache.length) {

            tbody.html('<tr><td colspan="12" class="text-center text-muted py-4"><i class="fas fa-inbox fa-2x d-block mb-2"></i>Sin presentaciones. Agregue al menos una.</td></tr>');

            return;

        }

        if (!lista.length) {

            tbody.html('<tr><td colspan="12" class="text-center text-muted py-4">Ninguna presentación coincide con la búsqueda.</td></tr>');

            return;

        }



        lista.forEach(function (p) {

            var cu = extraerCantidadUnidad(p.nombre);

            var ganancia = calcularGanancia(p.precio, p.precioCompra);

            var gananciaCls = ganancia >= 0 ? 'pres-ganancia-pos' : 'pres-ganancia-neg';

            var gananciaSigno = ganancia >= 0 ? '+' : '';



            tbody.append(

                '<tr>' +

                '<td class="text-center align-middle">' + thumbHtml(p) + '</td>' +

                '<td class="align-middle font-weight-bold">' + escHtml(p.nombre) + '</td>' +

                '<td class="text-center align-middle prod-col-num">' + escHtml(cu.cantidad) + '</td>' +

                '<td class="text-center align-middle">' + escHtml(cu.unidad) + '</td>' +

                '<td class="text-center align-middle">S/ ' + fmtPrecio(p.precioCompra) + '</td>' +

                '<td class="text-center align-middle font-weight-bold text-primary">S/ ' + fmtPrecio(p.precio) + '</td>' +

                '<td class="text-center align-middle ' + gananciaCls + '">' + gananciaSigno + 'S/ ' + fmtPrecio(ganancia) + '</td>' +

                '<td class="text-center align-middle prod-col-num">' + (p.stock != null ? p.stock : 0) + '</td>' +

                '<td class="text-center align-middle prod-col-num">' + (p.stockMinimo != null ? p.stockMinimo : 0) + '</td>' +

                '<td class="text-center align-middle"><code class="small">' + escHtml(p.codigoBarras || '—') + '</code></td>' +

                '<td class="text-center align-middle">' + badgeEstadoPresentacion(p) + '</td>' +

                '<td class="text-center align-middle">' +

                '<div class="pres-acciones-grupo">' +

                '<button type="button" class="btn btn-accion-pres btn-pres-img btn-imagenes-pres" data-id="' + p.id + '" data-toggle="tooltip" title="Gestionar imágenes"><i class="fas fa-images"></i></button>' +

                '<button type="button" class="btn btn-accion-pres btn-pres-edit btn-editar-pres" data-id="' + p.id + '" data-toggle="tooltip" title="Editar presentación"><i class="fas fa-pen"></i></button>' +

                '<button type="button" class="btn btn-accion-pres btn-pres-del btn-eliminar-pres" data-id="' + p.id + '" data-toggle="tooltip" title="Eliminar presentación"><i class="fas fa-trash-alt"></i></button>' +

                '</div></td></tr>'

            );

        });



        $('#modalPresentaciones [data-toggle="tooltip"]').tooltip({ container: '#modalPresentaciones' });

    }



    function construirNombrePresentacion() {

        var nombreManual = $('#pres-form-nombre').val().trim();

        if (nombreManual) return nombreManual;



        var cantidad = $('#pres-form-cantidad').val();

        var unidad = $('#pres-form-unidad').val();

        if (cantidad && unidad) {

            return cantidad + ' ' + unidad;

        }

        return '';

    }



    function parsearNombreEnFormulario(nombre) {

        var cu = extraerCantidadUnidad(nombre);

        if (/^\d/.test((nombre || '').trim())) {

            $('#pres-form-cantidad').val(cu.cantidad);

            var unidadSelect = $('#pres-form-unidad');

            if (unidadSelect.find('option[value="' + cu.unidad + '"]').length) {

                unidadSelect.val(cu.unidad);

            } else {

                unidadSelect.val('');

            }

        } else {

            $('#pres-form-cantidad').val('');

            var unidadSelect2 = $('#pres-form-unidad');

            if (unidadSelect2.find('option[value="' + cu.unidad + '"]').length) {

                unidadSelect2.val(cu.unidad);

            }

        }

    }



    function actualizarPreviewGanancia() {

        var ganancia = calcularGanancia($('#pres-form-precio').val(), $('#pres-form-precio-compra').val());

        var preview = $('#pres-form-ganancia-preview');

        $('#pres-form-ganancia-val').text((ganancia >= 0 ? '+' : '') + 'S/ ' + fmtPrecio(ganancia));

        preview.toggleClass('negativa', ganancia < 0);

    }



    function actualizarPreviewFoto(pres) {

        var box = $('#pres-form-foto-preview').empty();

        var ruta = '';

        if (pres) {

            ruta = rutaPublica(pres.imagenPrincipal || '');

            if (!ruta && pres.imagenes && pres.imagenes.length) {

                ruta = rutaPublica(pres.imagenes[0].ruta || '');

            }

        }

        if (ruta) {

            box.append($('<img>').attr('src', '/archivos/' + ruta).attr('alt', ''));

        } else {

            box.html('<i class="fas fa-camera"></i>');

        }

    }



    function limpiarFormPresentacion() {

        $('#pres-form-id').val('');

        $('#pres-form-nombre, #pres-form-cantidad, #pres-form-precio, #pres-form-precio-compra, #pres-form-stock, #pres-form-stock-min, #pres-form-codigo').val('');

        $('#pres-form-unidad').val('');

        $('#pres-form-precio-compra').val('0.00');

        $('#pres-form-stock').val('0');

        $('#pres-form-stock-min').val('0');

        $('#pres-form-estado').val('ACTIVO');

        $('#pres-form-codigo-wrap').hide();

        $('#pres-form-codigo-hint').show();

        ['pres-form-nombre', 'pres-form-precio', 'pres-form-precio-compra', 'pres-form-stock', 'pres-form-stock-min'].forEach(function (id) {

            $('#' + id).removeClass('is-invalid');

        });

        $('#err-pres-nombre, #err-pres-precio, #err-pres-precio-compra, #err-pres-stock, #err-pres-stock-min').text('');

        $('#pres-form-msg').hide().text('');

        actualizarPreviewFoto(null);

        actualizarPreviewGanancia();

    }



    function abrirFormPresentacion(pres) {

        limpiarFormPresentacion();

        var esEdit = pres && pres.id;

        $('#tituloFormPresentacion').html(esEdit

            ? '<i class="fas fa-pen mr-1"></i> Editar presentación'

            : '<i class="fas fa-plus-circle mr-1"></i> Nueva presentación');



        if (esEdit) {

            $('#pres-form-id').val(pres.id);

            $('#pres-form-nombre').val(pres.nombre);

            parsearNombreEnFormulario(pres.nombre);

            $('#pres-form-precio-compra').val(parseFloat(pres.precioCompra || 0).toFixed(2));

            $('#pres-form-precio').val(parseFloat(pres.precio).toFixed(2));

            $('#pres-form-stock').val(pres.stock);

            $('#pres-form-stock-min').val(pres.stockMinimo != null ? pres.stockMinimo : 0);

            $('#pres-form-estado').val(pres.estado || 'ACTIVO');

            if (pres.codigoBarras) {

                $('#pres-form-codigo').val(pres.codigoBarras);

                $('#pres-form-codigo-wrap').show();

                $('#pres-form-codigo-hint').hide();

            }

            actualizarPreviewFoto(pres);

        }



        actualizarPreviewGanancia();

        $('#modalFormPresentacion').modal('show');

    }



    function esPrecioValido(valor) {

        if (valor === '' || valor == null) return false;

        var n = parseFloat(valor);

        return !isNaN(n) && n >= 0;

    }



    function validarFormPresentacion() {

        var ok = true;

        var editId = $('#pres-form-id').val();

        var nombre = construirNombrePresentacion();

        var precioCompra = $('#pres-form-precio-compra').val();

        var precio = $('#pres-form-precio').val();

        var stock = $('#pres-form-stock').val();

        var stockMin = $('#pres-form-stock-min').val();

        var codigo = $('#pres-form-codigo').val().trim();



        ['pres-form-nombre', 'pres-form-precio', 'pres-form-precio-compra', 'pres-form-stock', 'pres-form-stock-min'].forEach(function (id) {

            $('#' + id).removeClass('is-invalid');

        });

        $('#err-pres-nombre, #err-pres-precio, #err-pres-precio-compra, #err-pres-stock, #err-pres-stock-min').text('');

        $('#pres-form-msg').hide().text('');



        if (!nombre) {

            $('#pres-form-nombre').addClass('is-invalid');

            $('#err-pres-nombre').text('Ingrese el nombre o complete cantidad y unidad de medida.');

            ok = false;

        } else {

            var nombreLower = nombre.toLowerCase();

            var dupNombre = presentacionesCache.some(function (p) {

                return p.nombre.toLowerCase() === nombreLower && String(p.id) !== String(editId || '');

            });

            if (dupNombre) {

                $('#pres-form-nombre').addClass('is-invalid');

                $('#err-pres-nombre').text('Ya existe una presentación con ese nombre para este producto.');

                ok = false;

            }

        }



        if (!esPrecioValido(precioCompra)) {

            $('#pres-form-precio-compra').addClass('is-invalid');

            $('#err-pres-precio-compra').text('Ingrese un precio de compra válido.');

            ok = false;

        }

        if (!esPrecioValido(precio)) {

            $('#pres-form-precio').addClass('is-invalid');

            $('#err-pres-precio').text('Ingrese un precio de venta válido.');

            ok = false;

        } else if (esPrecioValido(precioCompra) && parseFloat(precio) < parseFloat(precioCompra)) {

            $('#pres-form-precio').addClass('is-invalid');

            $('#err-pres-precio').text('El precio de venta no puede ser menor al precio de compra.');

            ok = false;

        }



        if (stock === '' || parseInt(stock, 10) < 0 || isNaN(parseInt(stock, 10))) {

            $('#pres-form-stock').addClass('is-invalid');

            $('#err-pres-stock').text('Ingrese un stock válido.');

            ok = false;

        }

        if (stockMin === '' || parseInt(stockMin, 10) < 0 || isNaN(parseInt(stockMin, 10))) {

            $('#pres-form-stock-min').addClass('is-invalid');

            $('#err-pres-stock-min').text('Ingrese un stock mínimo válido.');

            ok = false;

        }



        if (codigo) {

            var dupCodigo = presentacionesCache.some(function (p) {

                return p.codigoBarras === codigo && String(p.id) !== String(editId || '');

            });

            if (dupCodigo) {

                $('#pres-form-msg').text('El código de barras ya está registrado en otra presentación.').show();

                ok = false;

            }

        }



        return ok;

    }



    function guardarPresentacion() {

        if (!productoPresentacionesId || !validarFormPresentacion()) return;



        var payload = {

            id: $('#pres-form-id').val() ? parseInt($('#pres-form-id').val(), 10) : null,

            nombre: construirNombrePresentacion(),

            precioCompra: parseFloat($('#pres-form-precio-compra').val()),

            precio: parseFloat($('#pres-form-precio').val()),

            stock: parseInt($('#pres-form-stock').val(), 10),

            stockMinimo: parseInt($('#pres-form-stock-min').val(), 10),

            estado: $('#pres-form-estado').val()

        };



        $('#btnGuardarPresentacion').prop('disabled', true);

        $.ajax({

            url: '/productos/api/' + productoPresentacionesId + '/presentaciones',

            method: 'POST',

            contentType: 'application/json',

            data: JSON.stringify(payload)

        })

            .done(function (res) {

                if (res.codigoBarras) {

                    $('#pres-form-codigo').val(res.codigoBarras);

                    $('#pres-form-codigo-wrap').show();

                    $('#pres-form-codigo-hint').hide();

                }

                $('#modalFormPresentacion').modal('hide');

                cargarPresentaciones();

            })

            .fail(function (xhr) {

                var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo guardar.';

                $('#pres-form-msg').text(msg).show();

            })

            .always(function () {

                $('#btnGuardarPresentacion').prop('disabled', false);

            });

    }



    function eliminarPresentacion(id) {

        if (!confirm('¿Eliminar esta presentación?')) return;

        $.ajax({

            url: '/productos/api/presentaciones/' + id,

            method: 'DELETE'

        })

            .done(function () {

                cargarPresentaciones();

            })

            .fail(function (xhr) {

                var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo eliminar.';

                $('#pres-lista-msg').text(msg).show();

            });

    }



    function abrirModalImagenesPresentacion(id, nombre) {

        presentacionImagenesId = id;

        $('#pres-img-nombre').text(nombre || '');

        $('#pres-img-files').val('');

        $('#pres-img-files').next('.custom-file-label').text('Seleccionar imágenes');

        $('#pres-galeria-preview').empty();

        $('#pres-lbl-preview').hide();

        $('#err-pres-img-files').text('');



        var pres = presentacionesCache.find(function (p) { return p.id === id; });

        var galeria = $('#pres-galeria-existente').empty();

        if (pres && pres.imagenes && pres.imagenes.length) {

            pres.imagenes.forEach(function (img) {

                var wrap = $('<div class="img-thumb-preview">');

                wrap.append($('<img>').attr('src', '/archivos/' + img.ruta));

                var btn = $('<button type="button" class="btn-remove-img">&times;</button>');

                btn.on('click', function () {

                    if (!confirm('¿Eliminar esta imagen?')) return;

                    $.ajax({

                        url: '/productos/api/presentaciones/imagenes/' + img.id,

                        method: 'DELETE'

                    }).done(function () {

                        abrirModalImagenesPresentacion(id, nombre);

                        cargarPresentaciones();

                    }).fail(function (xhr) {

                        alert((xhr.responseJSON && xhr.responseJSON.message) || 'No se pudo eliminar.');

                    });

                });

                wrap.append(btn);

                galeria.append(wrap);

            });

        } else {

            galeria.html('<p class="text-muted mb-0">Sin imágenes. Agregue desde el selector.</p>');

        }



        $('#modalImagenesPresentacion').modal('show');

    }



    function guardarImagenesPresentacion() {

        var input = document.getElementById('pres-img-files');

        if (!input || !input.files || !input.files.length) {

            $('#err-pres-img-files').text('Seleccione al menos una imagen.');

            return;

        }

        if (!presentacionImagenesId) return;



        var formData = new FormData();

        for (var i = 0; i < input.files.length; i++) {

            formData.append('imagenFiles', input.files[i]);

        }



        $('#btnGuardarImagenesPresentacion').prop('disabled', true);

        $.ajax({

            url: '/productos/api/presentaciones/' + presentacionImagenesId + '/imagenes',

            method: 'POST',

            data: formData,

            processData: false,

            contentType: false

        })

            .done(function () {

                var pres = presentacionesCache.find(function (p) { return p.id === presentacionImagenesId; });

                abrirModalImagenesPresentacion(presentacionImagenesId, pres ? pres.nombre : '');

                cargarPresentaciones();

            })

            .fail(function (xhr) {

                $('#err-pres-img-files').text((xhr.responseJSON && xhr.responseJSON.message) || 'No se pudieron guardar las imágenes.');

            })

            .always(function () {

                $('#btnGuardarImagenesPresentacion').prop('disabled', false);

            });

    }



    $(document).on('click', '.btn-presentaciones-prod', function () {

        abrirModalPresentaciones(parseInt($(this).data('id'), 10), $(this).data('nombre') || '');

    });



    $('#btnNuevaPresentacion').on('click', function () {

        abrirFormPresentacion(null);

    });



    $(document).on('click', '.btn-editar-pres', function () {

        var id = parseInt($(this).data('id'), 10);

        var pres = presentacionesCache.find(function (p) { return p.id === id; });

        if (pres) abrirFormPresentacion(pres);

    });



    $(document).on('click', '.btn-imagenes-pres', function () {

        var id = parseInt($(this).data('id'), 10);

        var pres = presentacionesCache.find(function (p) { return p.id === id; });

        abrirModalImagenesPresentacion(id, pres ? pres.nombre : '');

    });



    $(document).on('click', '.btn-eliminar-pres', function () {

        eliminarPresentacion(parseInt($(this).data('id'), 10));

    });



    $('#btnGuardarPresentacion').on('click', guardarPresentacion);



    $('#pres-form-precio, #pres-form-precio-compra').on('input', actualizarPreviewGanancia);



    $('#pres-form-cantidad, #pres-form-unidad').on('change input', function () {

        if (!$('#pres-form-nombre').val().trim()) {

            var auto = construirNombrePresentacion();

            if (auto) {

                $('#pres-form-nombre').attr('placeholder', auto);

            }

        }

    });



    $('#pres-buscar').on('input', function () {

        filtroBusqueda = $(this).val().trim();

        renderTablaPresentaciones();

    });



    $('#btnGuardarImagenesPresentacion').on('click', guardarImagenesPresentacion);



    $('#pres-img-files').on('change', function () {

        var files = this.files;

        var label = files.length > 0 ? files.length + ' imagen(es)' : 'Seleccionar imágenes';

        $(this).next('.custom-file-label').text(label);

        $('#err-pres-img-files').text('');

        var preview = $('#pres-galeria-preview').empty();

        var permitidos = ['image/jpeg', 'image/png'];

        if (!files.length) {

            $('#pres-lbl-preview').hide();

            return;

        }

        $('#pres-lbl-preview').show();

        Array.prototype.forEach.call(files, function (f) {

            if (!permitidos.includes(f.type) || f.size > 2 * 1024 * 1024) {

                $('#err-pres-img-files').text('Solo JPG o PNG de hasta 2 MB: ' + f.name);

                preview.empty();

                return;

            }

            var reader = new FileReader();

            reader.onload = function (e) {

                preview.append($('<div class="img-thumb-preview">').append($('<img>').attr('src', e.target.result)));

            };

            reader.readAsDataURL(f);

        });

    });

})();


