(function () {
    'use strict';

    var dataTable = null;
    var historialTable = null;
    var productosGestionTable = null;
    var kardexGlobalTable = null;
    var presentaciones = [];
    var proveedores = [];
    var lineasCompra = [];
    var lineasPanelCompra = [];
    var ajusteStockActualVal = 0;

    var TIPOS_COMPROBANTE = [
        { value: 'FACTURA', label: 'Factura' },
        { value: 'BOLETA', label: 'Boleta' },
        { value: 'GUIA', label: 'Guía' },
        { value: 'NOTA_COMPRA', label: 'Nota de compra' },
        { value: 'OTROS', label: 'Otros' }
    ];

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        document.getElementById('compraFecha').value = hoyIso();
        document.getElementById('panelCompraFecha').value = hoyIso();
        document.getElementById('salidaFecha').value = hoyIso();
        llenarTiposComprobante();
        bindEventos();
        initTabs();
        cargarCatalogos().then(cargarTodo);
    }

    function llenarTiposComprobante() {
        document.querySelectorAll('.inv-tipo-comprobante').forEach(function (sel) {
            sel.innerHTML = TIPOS_COMPROBANTE.map(function (t) {
                return '<option value="' + t.value + '">' + t.label + '</option>';
            }).join('');
        });
    }

    function initTabs() {
        $('#invTabs a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
            var target = e.target.getAttribute('href');
            if (target === '#tabProductos') {
                loadProductosGestion();
            } else if (target === '#tabKardex') {
                loadKardexGlobal();
            } else if (target === '#tabInventario' && dataTable) {
                dataTable.columns.adjust().responsive.recalc();
            } else if (target === '#tabIngreso') {
                initPanelCompra();
            }
        });
    }

    function bindEventos() {
        document.getElementById('btnAplicarFiltros').addEventListener('click', cargarInventario);
        document.getElementById('filtroBusqueda').addEventListener('keyup', debounce(cargarInventario, 400));
        document.getElementById('filtroCategoria').addEventListener('change', cargarInventario);
        document.getElementById('filtroEstado').addEventListener('change', cargarInventario);

        document.getElementById('btnRegistrarCompra').addEventListener('click', abrirModalCompra);
        document.getElementById('btnIngresoManual').addEventListener('click', function () { abrirMovimiento('ingreso'); });
        document.getElementById('btnSalida').addEventListener('click', function () { abrirMovimiento('salida'); });
        document.getElementById('btnAjuste').addEventListener('click', function () { abrirMovimiento('ajuste'); });
        document.getElementById('btnHistorial').addEventListener('click', abrirHistorial);
        document.getElementById('btnExportPdf').addEventListener('click', function () { exportar('pdf'); });
        document.getElementById('btnExportExcel').addEventListener('click', function () { exportar('excel'); });

        document.getElementById('btnAgregarLineaCompra').addEventListener('click', function () { agregarLineaCompra('modal'); });
        document.getElementById('btnGuardarCompra').addEventListener('click', function () { guardarCompra('modal'); });
        document.getElementById('btnGuardarMovimiento').addEventListener('click', guardarMovimiento);
        document.getElementById('btnNuevoProveedor').addEventListener('click', function () { $('#modalProveedor').modal('show'); });
        document.getElementById('btnPanelNuevoProveedor').addEventListener('click', function () { $('#modalProveedor').modal('show'); });
        document.getElementById('btnGuardarProveedor').addEventListener('click', guardarProveedor);
        document.getElementById('compraDetalleBody').addEventListener('change', function () { recalcularCompra('modal'); });
        document.getElementById('compraDetalleBody').addEventListener('input', function () { recalcularCompra('modal'); });
        document.getElementById('compraIgv').addEventListener('input', function () { recalcularCompra('modal'); });

        document.getElementById('btnPanelAgregarLinea').addEventListener('click', function () { agregarLineaCompra('panel'); });
        document.getElementById('btnPanelGuardarCompra').addEventListener('click', function () { guardarCompra('panel'); });
        document.getElementById('panelCompraDetalleBody').addEventListener('change', function () { recalcularCompra('panel'); });
        document.getElementById('panelCompraDetalleBody').addEventListener('input', function () { recalcularCompra('panel'); });
        document.getElementById('panelCompraIgv').addEventListener('input', function () { recalcularCompra('panel'); });

        document.getElementById('btnPanelGuardarSalida').addEventListener('click', guardarSalidaPanel);
        document.getElementById('btnPanelGuardarAjuste').addEventListener('click', guardarAjustePanel);
        document.getElementById('btnRefrescarKardex').addEventListener('click', loadKardexGlobal);

        document.getElementById('movPresentacion').addEventListener('change', onMovPresentacionChange);
        document.getElementById('movStockNuevo').addEventListener('input', calcularDiferenciaModal);
        document.getElementById('ajustePresentacion').addEventListener('change', onAjustePresentacionChange);
        document.getElementById('ajusteStockFisico').addEventListener('input', calcularDiferenciaPanel);
    }

    function compraCtx(ctx) {
        if (ctx === 'panel') {
            return {
                proveedor: 'panelCompraProveedor',
                tipo: 'panelCompraTipo',
                serie: 'panelCompraSerie',
                numero: 'panelCompraNumero',
                fecha: 'panelCompraFecha',
                observaciones: 'panelCompraObservaciones',
                detalleBody: 'panelCompraDetalleBody',
                igv: 'panelCompraIgv',
                subtotal: 'panelCompraSubtotal',
                total: 'panelCompraTotal',
                lineas: lineasPanelCompra
            };
        }
        return {
            proveedor: 'compraProveedor',
            tipo: 'compraTipo',
            serie: 'compraSerie',
            numero: 'compraNumero',
            fecha: 'compraFecha',
            observaciones: 'compraObservaciones',
            detalleBody: 'compraDetalleBody',
            igv: 'compraIgv',
            subtotal: 'compraSubtotal',
            total: 'compraTotal',
            lineas: lineasCompra
        };
    }

    function initPanelCompra() {
        if (document.getElementById('panelCompraDetalleBody').children.length === 0) {
            lineasPanelCompra = [];
            document.getElementById('panelCompraSerie').value = '';
            document.getElementById('panelCompraNumero').value = '';
            document.getElementById('panelCompraObservaciones').value = '';
            document.getElementById('panelCompraIgv').value = '0';
            llenarProveedoresEn('panelCompraProveedor');
            agregarLineaCompra('panel');
            recalcularCompra('panel');
        }
    }

    function cargarCatalogos() {
        return Promise.all([
            fetch('/inventario/api/categorias').then(r => r.json()),
            fetch('/inventario/api/presentaciones').then(r => r.json()),
            fetch('/inventario/api/proveedores').then(r => r.json())
        ]).then(function (res) {
            llenarCategorias(res[0]);
            presentaciones = res[1] || [];
            proveedores = res[2] || [];
            llenarProveedores();
            llenarProveedoresEn('panelCompraProveedor');
            llenarSelectPresentaciones('movPresentacion');
            llenarSelectPresentaciones('salidaPresentacion');
            llenarSelectPresentaciones('ajustePresentacion');
        });
    }

    function cargarTodo() {
        mostrarCargando(true);
        Promise.all([
            fetch('/inventario/api/resumen').then(r => r.json()),
            fetch(urlLista()).then(r => r.json())
        ]).then(function (res) {
            actualizarKpis(res[0]);
            renderTabla(res[1]);
            mostrarCargando(false);
        }).catch(function (err) {
            mostrarError(err.message || 'Error al cargar inventario');
            mostrarCargando(false);
        });
    }

    function cargarInventario() {
        mostrarCargando(true);
        fetch(urlLista())
            .then(function (r) { return r.json(); })
            .then(function (filas) {
                renderTabla(filas);
                mostrarCargando(false);
            })
            .catch(function (err) {
                mostrarError(err.message);
                mostrarCargando(false);
            });
        fetch('/inventario/api/resumen')
            .then(function (r) { return r.json(); })
            .then(actualizarKpis);
    }

    function loadProductosGestion() {
        fetch('/inventario/api/productos-gestion')
            .then(function (r) { return r.json(); })
            .then(function (filas) {
                if (productosGestionTable) {
                    productosGestionTable.destroy();
                    productosGestionTable = null;
                }
                var tbody = document.getElementById('tablaProductosGestionBody');
                tbody.innerHTML = '';
                if (!filas || filas.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-4">No hay productos registrados.</td></tr>';
                } else {
                    filas.forEach(function (p) {
                        var img = p.imagen
                            ? '<img src="' + escapeAttr(p.imagen) + '" class="inv-product-img" alt="" />'
                            : '<span class="material-symbols-outlined text-muted">image</span>';
                        var estado = p.estado || 'ACTIVO';
                        var badgeCls = estado === 'ACTIVO' ? 'normal' : 'agotado';
                        var badge = '<span class="inv-stock-badge ' + badgeCls + '">' + escapeHtml(estado) + '</span>';
                        var tr = document.createElement('tr');
                        tr.innerHTML =
                            '<td class="text-center">' + img + '</td>' +
                            '<td>' + escapeHtml(p.producto) + '</td>' +
                            '<td>' + escapeHtml(p.categoria) + '</td>' +
                            '<td class="text-center">' + (p.cantidadPresentaciones || 0) + '</td>' +
                            '<td class="text-center font-weight-bold">' + (p.stockTotal || 0) + '</td>' +
                            '<td>' + badge + '</td>' +
                            '<td class="text-center"><button type="button" class="btn btn-xs btn-info btn-presentaciones-prod" data-id="' + p.productoId + '" data-producto-id="' + p.productoId + '" data-nombre="' + escapeAttr(p.producto) + '"><span class="material-symbols-outlined inv-icon-sm">visibility</span> Ver</button></td>';
                        tbody.appendChild(tr);
                    });
                }
                productosGestionTable = $('#tablaProductosGestion').DataTable({
                    responsive: true,
                    autoWidth: false,
                    pageLength: 10,
                    order: [[1, 'asc']],
                    language: { search: 'Buscar:', zeroRecords: 'Sin productos' },
                    columnDefs: [{ orderable: false, targets: [0, 6] }]
                });
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function loadKardexGlobal() {
        fetch('/inventario/api/movimientos')
            .then(function (r) { return r.json(); })
            .then(function (movs) {
                if (kardexGlobalTable) {
                    kardexGlobalTable.destroy();
                    kardexGlobalTable = null;
                }
                var tbody = document.getElementById('tablaKardexGlobalBody');
                tbody.innerHTML = '';
                if (!movs || movs.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="12" class="text-center text-muted py-4">Sin movimientos registrados.</td></tr>';
                } else {
                    movs.forEach(function (m) {
                        tbody.appendChild(filaMovimientoCompleta(m));
                    });
                }
                kardexGlobalTable = $('#tablaKardexGlobal').DataTable({
                    responsive: true,
                    autoWidth: false,
                    pageLength: 25,
                    order: [[0, 'desc'], [1, 'desc']],
                    language: { search: 'Buscar:', zeroRecords: 'Sin movimientos' }
                });
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function urlLista() {
        var params = new URLSearchParams();
        var busq = document.getElementById('filtroBusqueda').value.trim();
        var cat = document.getElementById('filtroCategoria').value;
        var est = document.getElementById('filtroEstado').value;
        if (busq) params.set('busqueda', busq);
        if (cat) params.set('categoriaId', cat);
        if (est) params.set('estadoStock', est);
        var qs = params.toString();
        return '/inventario/api/lista' + (qs ? '?' + qs : '');
    }

    function actualizarKpis(k) {
        document.getElementById('kpiProductos').textContent = k.totalProductos || 0;
        document.getElementById('kpiPresentaciones').textContent = k.totalPresentaciones || 0;
        document.getElementById('kpiStockBajo').textContent = k.stockBajo || 0;
        document.getElementById('kpiSinStock').textContent = k.sinStock || 0;
        document.getElementById('kpiValor').textContent = k.valorInventario || 'S/ 0.00';
    }

    function renderTabla(filas) {
        if (dataTable) {
            dataTable.destroy();
            dataTable = null;
        }
        var tbody = document.getElementById('tablaInventarioBody');
        tbody.innerHTML = '';

        if (!filas || filas.length === 0) {
            tbody.innerHTML = '<tr><td colspan="11" class="text-center text-muted py-4">No hay registros de inventario.</td></tr>';
            initDataTable();
            return;
        }

        filas.forEach(function (f) {
            var tr = document.createElement('tr');
            var img = f.imagen
                ? '<img src="' + escapeAttr(f.imagen) + '" class="inv-product-img" alt="" />'
                : '<span class="material-symbols-outlined text-muted">image</span>';
            var estado = f.estadoStock || 'normal';
            var badge = '<span class="inv-stock-badge ' + estado + '"><span class="inv-stock-dot ' + estado + '"></span>' +
                escapeHtml(f.estadoLabel || estado) + '</span>';

            tr.innerHTML =
                '<td class="text-center">' + img + '</td>' +
                '<td>' + escapeHtml(f.producto) + '</td>' +
                '<td>' + escapeHtml(f.categoria) + '</td>' +
                '<td>' + escapeHtml(f.presentacion) + '</td>' +
                '<td>' + escapeHtml(f.codigo) + '</td>' +
                '<td class="text-center font-weight-bold">' + f.stock + '</td>' +
                '<td class="text-center">' + f.stockMinimo + '</td>' +
                '<td>' + escapeHtml(f.precioCompra) + '</td>' +
                '<td>' + escapeHtml(f.precioVenta) + '</td>' +
                '<td>' + badge + '</td>' +
                '<td class="text-center"><button type="button" class="btn btn-xs btn-info btn-kardex" data-id="' + f.presentacionId + '" data-nombre="' + escapeAttr(f.producto + ' — ' + f.presentacion) + '"><span class="material-symbols-outlined inv-icon-sm">history</span></button></td>';
            tbody.appendChild(tr);
        });

        tbody.querySelectorAll('.btn-kardex').forEach(function (btn) {
            btn.addEventListener('click', function () {
                abrirKardex(btn.getAttribute('data-id'), btn.getAttribute('data-nombre'));
            });
        });

        initDataTable();
    }

    function initDataTable() {
        dataTable = $('#tablaInventario').DataTable({
            responsive: true,
            autoWidth: false,
            pageLength: 10,
            lengthMenu: [[10, 25, 50, 100, -1], [10, 25, 50, 100, 'Todos']],
            order: [[1, 'asc']],
            language: {
                search: 'Buscar:',
                lengthMenu: 'Mostrar _MENU_',
                info: '_START_ a _END_ de _TOTAL_',
                zeroRecords: 'Sin resultados',
                paginate: { first: '«', last: '»', next: '›', previous: '‹' }
            },
            columnDefs: [{ orderable: false, targets: [0, 10] }]
        });
    }

    function filaMovimientoCompleta(m) {
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td>' + escapeHtml(m.fecha) + '</td>' +
            '<td>' + escapeHtml(m.hora) + '</td>' +
            '<td>' + escapeHtml(m.producto) + '</td>' +
            '<td>' + escapeHtml(m.presentacion) + '</td>' +
            '<td>' + escapeHtml(m.tipo) + '</td>' +
            '<td>' + escapeHtml(m.documento) + '</td>' +
            '<td class="text-center text-success">' + fmtCantidad(m.entrada) + '</td>' +
            '<td class="text-center text-danger">' + fmtCantidad(m.salida) + '</td>' +
            '<td class="text-center">' + (m.stockAnterior != null ? m.stockAnterior : '—') + '</td>' +
            '<td class="text-center">' + (m.stockNuevo != null ? m.stockNuevo : '—') + '</td>' +
            '<td>' + escapeHtml(m.usuario) + '</td>' +
            '<td>' + escapeHtml(m.observacion) + '</td>';
        return tr;
    }

    function filaMovimientoKardex(m) {
        var tr = document.createElement('tr');
        tr.innerHTML =
            '<td>' + escapeHtml(m.fecha) + '</td>' +
            '<td>' + escapeHtml(m.hora) + '</td>' +
            '<td>' + escapeHtml(m.tipo) + '</td>' +
            '<td>' + escapeHtml(m.documento) + '</td>' +
            '<td class="text-center text-success">' + fmtCantidad(m.entrada) + '</td>' +
            '<td class="text-center text-danger">' + fmtCantidad(m.salida) + '</td>' +
            '<td class="text-center">' + (m.stockAnterior != null ? m.stockAnterior : '—') + '</td>' +
            '<td class="text-center">' + (m.stockNuevo != null ? m.stockNuevo : '—') + '</td>' +
            '<td>' + escapeHtml(m.usuario) + '</td>' +
            '<td>' + escapeHtml(m.observacion) + '</td>';
        return tr;
    }

    function fmtCantidad(val) {
        if (val == null || val === 0) return '—';
        return val;
    }

    function abrirModalCompra() {
        lineasCompra = [];
        document.getElementById('compraSerie').value = '';
        document.getElementById('compraNumero').value = '';
        document.getElementById('compraObservaciones').value = '';
        document.getElementById('compraIgv').value = '0';
        document.getElementById('compraDetalleBody').innerHTML = '';
        agregarLineaCompra('modal');
        recalcularCompra('modal');
        $('#modalCompra').modal('show');
    }

    function agregarLineaCompra(ctx) {
        var c = compraCtx(ctx);
        var idx = c.lineas.length;
        c.lineas.push({ presentacionId: null, cantidad: 1, precioCompra: 0 });
        var tr = document.createElement('tr');
        tr.setAttribute('data-idx', idx);
        tr.innerHTML =
            '<td><select class="form-control form-control-sm linea-presentacion" data-idx="' + idx + '">' +
            opcionesPresentaciones(null) + '</select></td>' +
            '<td><input type="number" min="1" class="form-control form-control-sm linea-cantidad" data-idx="' + idx + '" value="1" /></td>' +
            '<td><input type="number" step="0.01" min="0" class="form-control form-control-sm linea-precio" data-idx="' + idx + '" value="0" /></td>' +
            '<td class="linea-subtotal text-right">S/ 0.00</td>' +
            '<td class="text-center"><button type="button" class="btn btn-xs btn-danger btn-quitar-linea" data-idx="' + idx + '">&times;</button></td>';
        document.getElementById(c.detalleBody).appendChild(tr);

        tr.querySelector('.linea-presentacion').addEventListener('change', function (e) {
            var i = parseInt(e.target.getAttribute('data-idx'), 10);
            var val = parseInt(e.target.value, 10);
            c.lineas[i].presentacionId = val || null;
            var pres = presentaciones.find(function (p) { return p.id === val; });
            if (pres && pres.precioCompra) {
                tr.querySelector('.linea-precio').value = Number(pres.precioCompra).toFixed(2);
                c.lineas[i].precioCompra = Number(pres.precioCompra);
            }
            recalcularCompra(ctx);
        });
        tr.querySelector('.btn-quitar-linea').addEventListener('click', function (e) {
            var i = parseInt(e.target.getAttribute('data-idx'), 10);
            c.lineas.splice(i, 1);
            reindexarLineasCompra(ctx);
        });
    }

    function reindexarLineasCompra(ctx) {
        var c = compraCtx(ctx);
        var body = document.getElementById(c.detalleBody);
        body.innerHTML = '';
        var copia = c.lineas.slice();
        if (ctx === 'panel') lineasPanelCompra = [];
        else lineasCompra = [];
        copia.forEach(function (l) {
            agregarLineaCompra(ctx);
            var lineas = ctx === 'panel' ? lineasPanelCompra : lineasCompra;
            var idx = lineas.length - 1;
            lineas[idx] = l;
            var tr = body.lastElementChild;
            tr.querySelector('.linea-presentacion').value = l.presentacionId || '';
            tr.querySelector('.linea-cantidad').value = l.cantidad || 1;
            tr.querySelector('.linea-precio').value = l.precioCompra || 0;
        });
        recalcularCompra(ctx);
    }

    function recalcularCompra(ctx) {
        var c = compraCtx(ctx);
        var subtotal = 0;
        document.querySelectorAll('#' + c.detalleBody + ' tr').forEach(function (tr, idx) {
            var cant = parseFloat(tr.querySelector('.linea-cantidad').value) || 0;
            var precio = parseFloat(tr.querySelector('.linea-precio').value) || 0;
            var lineSub = cant * precio;
            subtotal += lineSub;
            tr.querySelector('.linea-subtotal').textContent = 'S/ ' + lineSub.toFixed(2);
            if (c.lineas[idx]) {
                c.lineas[idx].cantidad = cant;
                c.lineas[idx].precioCompra = precio;
                c.lineas[idx].presentacionId = parseInt(tr.querySelector('.linea-presentacion').value, 10) || null;
            }
        });
        var igv = parseFloat(document.getElementById(c.igv).value) || 0;
        document.getElementById(c.subtotal).textContent = 'S/ ' + subtotal.toFixed(2);
        document.getElementById(c.total).textContent = 'S/ ' + (subtotal + igv).toFixed(2);
    }

    function guardarCompra(ctx) {
        var c = compraCtx(ctx);
        var detalles = [];
        document.querySelectorAll('#' + c.detalleBody + ' tr').forEach(function (tr) {
            var pid = parseInt(tr.querySelector('.linea-presentacion').value, 10);
            var cant = parseInt(tr.querySelector('.linea-cantidad').value, 10);
            var precio = parseFloat(tr.querySelector('.linea-precio').value);
            if (pid && cant > 0) {
                detalles.push({ presentacionId: pid, cantidad: cant, precioCompra: precio });
            }
        });
        var body = {
            proveedorId: parseInt(document.getElementById(c.proveedor).value, 10),
            tipoComprobante: document.getElementById(c.tipo).value,
            serie: document.getElementById(c.serie).value,
            numero: document.getElementById(c.numero).value,
            fechaEmision: document.getElementById(c.fecha).value,
            observaciones: document.getElementById(c.observaciones).value,
            igv: parseFloat(document.getElementById(c.igv).value) || 0,
            detalles: detalles
        };
        postJson('/inventario/api/compras', body)
            .then(function (res) {
                if (ctx === 'modal') {
                    $('#modalCompra').modal('hide');
                } else {
                    lineasPanelCompra = [];
                    document.getElementById('panelCompraDetalleBody').innerHTML = '';
                    document.getElementById('panelCompraSerie').value = '';
                    document.getElementById('panelCompraNumero').value = '';
                    document.getElementById('panelCompraObservaciones').value = '';
                    document.getElementById('panelCompraIgv').value = '0';
                    agregarLineaCompra('panel');
                    recalcularCompra('panel');
                }
                mostrarExito(res.message || 'Compra registrada');
                cargarTodo();
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function abrirMovimiento(tipo) {
        document.getElementById('movTipo').value = tipo;
        document.getElementById('movCantidad').value = '1';
        document.getElementById('movStockNuevo').value = '';
        document.getElementById('movStockActual').value = '';
        document.getElementById('movDiferencia').value = '';
        document.getElementById('movObservacion').value = '';
        document.getElementById('movMotivoSalida').value = '';
        document.getElementById('movMotivoAjuste').value = '';

        document.getElementById('grpCantidad').classList.toggle('d-none', tipo === 'ajuste');
        document.getElementById('grpStockActual').classList.toggle('d-none', tipo !== 'ajuste');
        document.getElementById('grpStockNuevo').classList.toggle('d-none', tipo !== 'ajuste');
        document.getElementById('grpDiferencia').classList.toggle('d-none', tipo !== 'ajuste');
        document.getElementById('grpMotivoSalida').classList.toggle('d-none', tipo !== 'salida');
        document.getElementById('grpMotivoAjuste').classList.toggle('d-none', tipo !== 'ajuste');

        var titulos = { ingreso: 'Registrar Ingreso Manual', salida: 'Registrar Salida', ajuste: 'Ajuste de Inventario' };
        document.getElementById('modalMovimientoTitulo').textContent = titulos[tipo] || 'Movimiento';

        if (tipo === 'ajuste' && document.getElementById('movPresentacion').value) {
            onMovPresentacionChange();
        }
        $('#modalMovimiento').modal('show');
    }

    function onMovPresentacionChange() {
        var tipo = document.getElementById('movTipo').value;
        if (tipo !== 'ajuste') return;
        var pid = parseInt(document.getElementById('movPresentacion').value, 10);
        if (!pid) {
            document.getElementById('movStockActual').value = '';
            document.getElementById('movDiferencia').value = '';
            return;
        }
        fetchStockPresentacion(pid).then(function (stock) {
            document.getElementById('movStockActual').value = stock;
            calcularDiferenciaModal();
        });
    }

    function calcularDiferenciaModal() {
        var actual = parseInt(document.getElementById('movStockActual').value, 10) || 0;
        var fisico = parseInt(document.getElementById('movStockNuevo').value, 10);
        if (isNaN(fisico)) {
            document.getElementById('movDiferencia').value = '';
            return;
        }
        document.getElementById('movDiferencia').value = fisico - actual;
    }

    function onAjustePresentacionChange() {
        var pid = parseInt(document.getElementById('ajustePresentacion').value, 10);
        if (!pid) {
            ajusteStockActualVal = 0;
            document.getElementById('ajusteStockActual').value = '';
            document.getElementById('ajusteDiferencia').value = '';
            return;
        }
        fetchStockPresentacion(pid).then(function (stock) {
            ajusteStockActualVal = stock;
            document.getElementById('ajusteStockActual').value = stock;
            calcularDiferenciaPanel();
        });
    }

    function calcularDiferenciaPanel() {
        var fisico = parseInt(document.getElementById('ajusteStockFisico').value, 10);
        if (isNaN(fisico)) {
            document.getElementById('ajusteDiferencia').value = '';
            return;
        }
        document.getElementById('ajusteDiferencia').value = fisico - ajusteStockActualVal;
    }

    function fetchStockPresentacion(presentacionId) {
        return fetch('/inventario/api/presentaciones/' + presentacionId + '/stock')
            .then(function (r) { return r.json(); })
            .then(function (data) { return data.stockActual != null ? data.stockActual : 0; })
            .catch(function () { return 0; });
    }

    function guardarMovimiento() {
        var tipo = document.getElementById('movTipo').value;
        var body = {
            presentacionId: parseInt(document.getElementById('movPresentacion').value, 10),
            observacion: document.getElementById('movObservacion').value
        };
        var url;
        if (tipo === 'ingreso') {
            body.cantidad = parseInt(document.getElementById('movCantidad').value, 10);
            url = '/inventario/api/movimientos/ingreso';
        } else if (tipo === 'salida') {
            body.cantidad = parseInt(document.getElementById('movCantidad').value, 10);
            body.motivo = document.getElementById('movMotivoSalida').value;
            url = '/inventario/api/movimientos/salida';
        } else {
            body.stockNuevo = parseInt(document.getElementById('movStockNuevo').value, 10);
            body.motivo = document.getElementById('movMotivoAjuste').value.trim();
            url = '/inventario/api/movimientos/ajuste';
        }
        postJson(url, body)
            .then(function (res) {
                $('#modalMovimiento').modal('hide');
                mostrarExito(res.message || 'Movimiento registrado');
                cargarTodo();
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function guardarSalidaPanel() {
        var body = {
            presentacionId: parseInt(document.getElementById('salidaPresentacion').value, 10),
            cantidad: parseInt(document.getElementById('salidaCantidad').value, 10),
            motivo: document.getElementById('salidaMotivo').value,
            observacion: document.getElementById('salidaObservacion').value
        };
        postJson('/inventario/api/movimientos/salida', body)
            .then(function (res) {
                document.getElementById('salidaCantidad').value = '1';
                document.getElementById('salidaMotivo').value = '';
                document.getElementById('salidaObservacion').value = '';
                mostrarExito(res.message || 'Salida registrada');
                cargarTodo();
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function guardarAjustePanel() {
        var stockFisico = parseInt(document.getElementById('ajusteStockFisico').value, 10);
        var body = {
            presentacionId: parseInt(document.getElementById('ajustePresentacion').value, 10),
            stockNuevo: stockFisico,
            motivo: document.getElementById('ajusteMotivo').value.trim(),
            observacion: document.getElementById('ajusteObservacion').value
        };
        postJson('/inventario/api/movimientos/ajuste', body)
            .then(function (res) {
                document.getElementById('ajusteStockFisico').value = '';
                document.getElementById('ajusteDiferencia').value = '';
                document.getElementById('ajusteMotivo').value = '';
                document.getElementById('ajusteObservacion').value = '';
                onAjustePresentacionChange();
                mostrarExito(res.message || 'Ajuste registrado');
                cargarTodo();
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function abrirHistorial() {
        fetch('/inventario/api/movimientos')
            .then(function (r) { return r.json(); })
            .then(function (movs) {
                if (historialTable) {
                    historialTable.destroy();
                    historialTable = null;
                }
                var tbody = document.getElementById('tablaHistorialBody');
                tbody.innerHTML = '';
                movs.forEach(function (m) {
                    tbody.appendChild(filaMovimientoCompleta(m));
                });
                historialTable = $('#tablaHistorial').DataTable({
                    pageLength: 10,
                    order: [[0, 'desc'], [1, 'desc']],
                    language: { search: 'Buscar:', zeroRecords: 'Sin movimientos' }
                });
                $('#modalHistorial').modal('show');
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function abrirKardex(presentacionId, nombre) {
        document.getElementById('kardexTitulo').textContent = 'Kardex — ' + nombre;
        fetch('/inventario/api/movimientos/' + presentacionId)
            .then(function (r) { return r.json(); })
            .then(function (movs) {
                var tbody = document.getElementById('kardexBody');
                tbody.innerHTML = '';
                if (!movs.length) {
                    tbody.innerHTML = '<tr><td colspan="10" class="text-center text-muted py-3">Sin movimientos registrados</td></tr>';
                } else {
                    movs.forEach(function (m) {
                        tbody.appendChild(filaMovimientoKardex(m));
                    });
                }
                $('#modalKardex').modal('show');
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function guardarProveedor() {
        var nombre = document.getElementById('provNombre').value.trim();
        if (!nombre) {
            mostrarError('Ingrese el nombre del proveedor.');
            return;
        }
        var body = {
            nombre: nombre,
            documento: document.getElementById('provDocumento').value.trim()
        };
        postJson('/inventario/api/proveedores', body)
            .then(function (res) {
                $('#modalProveedor').modal('hide');
                proveedores.push(res.proveedor);
                llenarProveedores(res.proveedor.id);
                llenarProveedoresEn('panelCompraProveedor', res.proveedor.id);
                document.getElementById('provNombre').value = '';
                document.getElementById('provDocumento').value = '';
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function exportar(formato) {
        var base = '/inventario/api/exportar/' + formato;
        var qs = urlLista().split('?')[1];
        var url = qs ? base + '?' + qs : base;
        fetch(url, { method: 'POST' })
            .then(function (r) {
                if (!r.ok) throw new Error('Error al exportar');
                return r.blob();
            })
            .then(function (blob) {
                descargarBlob(blob, 'inventario.' + (formato === 'pdf' ? 'pdf' : 'xlsx'));
            })
            .catch(function (err) { mostrarError(err.message); });
    }

    function llenarCategorias(cats) {
        var sel = document.getElementById('filtroCategoria');
        sel.innerHTML = '<option value="">Todas</option>';
        (cats || []).forEach(function (c) {
            var opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = c.nombre;
            sel.appendChild(opt);
        });
    }

    function llenarProveedores(seleccionarId) {
        llenarProveedoresEn('compraProveedor', seleccionarId);
    }

    function llenarProveedoresEn(id, seleccionarId) {
        var sel = document.getElementById(id);
        if (!sel) return;
        sel.innerHTML = '<option value="">Seleccione...</option>';
        proveedores.forEach(function (p) {
            var opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = p.nombre;
            sel.appendChild(opt);
        });
        if (seleccionarId) sel.value = seleccionarId;
    }

    function llenarSelectPresentaciones(id) {
        var sel = document.getElementById(id);
        if (!sel) return;
        sel.innerHTML = '<option value="">Seleccione...</option>' + opcionesPresentaciones(null);
    }

    function opcionesPresentaciones(selectedId) {
        return presentaciones.map(function (p) {
            var sel = selectedId === p.id ? ' selected' : '';
            return '<option value="' + p.id + '"' + sel + '>' + escapeHtml(p.label) + '</option>';
        }).join('');
    }

    function postJson(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body || {})
        }).then(function (r) {
            if (!r.ok) {
                return r.text().then(function (t) {
                    try { var j = JSON.parse(t); throw new Error(j.message || t); } catch (e) {
                        if (e.message) throw e;
                        throw new Error(t || 'Error en la operación');
                    }
                });
            }
            return r.json();
        });
    }

    function descargarBlob(blob, nombre) {
        var a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = nombre;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
    }

    function mostrarCargando(show) {
        document.getElementById('invLoading').classList.toggle('d-none', !show);
    }

    function mostrarError(msg) {
        var el = document.getElementById('inv-alerta');
        el.textContent = msg;
        el.className = 'alert alert-danger';
        el.classList.remove('d-none');
        setTimeout(function () { el.classList.add('d-none'); }, 6000);
    }

    function mostrarExito(msg) {
        var el = document.getElementById('inv-alerta');
        el.textContent = msg;
        el.className = 'alert alert-success';
        el.classList.remove('d-none');
        setTimeout(function () { el.classList.add('d-none'); }, 4000);
    }

    function hoyIso() {
        return new Date().toISOString().slice(0, 10);
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    function escapeAttr(str) {
        return escapeHtml(str).replace(/'/g, '&#39;');
    }

    function debounce(fn, ms) {
        var t;
        return function () {
            clearTimeout(t);
            var args = arguments, ctx = this;
            t = setTimeout(function () { fn.apply(ctx, args); }, ms);
        };
    }
})();
