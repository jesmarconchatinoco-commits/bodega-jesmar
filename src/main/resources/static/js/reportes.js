(function () {
    'use strict';

    var tipoActual = 'GENERAL';
    var dataTable = null;
    var charts = {};
    var columnasActuales = [];
    var filasActuales = [];
    var tituloActual = 'Reporte';
    var permiteDetalle = false;

    var COLORES = ['#6d28d9', '#8b5cf6', '#10b981', '#38bdf8', '#f59e0b', '#ef4444', '#6366f1', '#ec4899'];

    document.addEventListener('DOMContentLoaded', init);

    function init() {
        bindEventos();
        toggleControlesExportacion();
        cargarCatalogoFiltros().then(function () {
            aplicarFiltros();
        });
    }

    function bindEventos() {
        document.getElementById('btnAplicarFiltros').addEventListener('click', aplicarFiltros);
        document.getElementById('btnLimpiarFiltros').addEventListener('click', limpiarFiltros);
        document.getElementById('filtroPeriodo').addEventListener('change', toggleRangoFechas);
        document.getElementById('btnToggleFiltros').addEventListener('click', function () {
            var panel = document.getElementById('panelFiltros');
            panel.classList.toggle('d-none');
        });

        document.querySelectorAll('.rep-nav-item').forEach(function (el) {
            el.addEventListener('click', function (e) {
                e.preventDefault();
                seleccionarReporte(el.getAttribute('data-tipo'));
            });
        });

        document.getElementById('btnExportPdf').addEventListener('click', function () { exportar('PDF'); });
        document.getElementById('btnExportExcel').addEventListener('click', function () { exportar('EXCEL'); });

        var filtros = document.querySelectorAll('#formFiltrosReportes select, #formFiltrosReportes input');
        filtros.forEach(function (f) {
            f.addEventListener('change', debounce(aplicarFiltros, 400));
        });
        document.getElementById('filtroMarca').addEventListener('input', debounce(aplicarFiltros, 600));

        document.getElementById('tablaReporte').addEventListener('click', function (e) {
            var btn = e.target.closest('.rep-btn-detalle');
            var row = e.target.closest('.rep-row-detalle');
            var id = btn ? btn.getAttribute('data-venta-id') : (row ? row.getAttribute('data-venta-id') : null);
            if (id) abrirDetalleVenta(id);
        });
    }

    function toggleRangoFechas() {
        var esRango = document.getElementById('filtroPeriodo').value === 'RANGO';
        document.querySelectorAll('.rango-fechas').forEach(function (el) {
            el.classList.toggle('d-none', !esRango);
        });
    }

    function seleccionarReporte(tipo) {
        tipoActual = tipo;
        document.querySelectorAll('.rep-nav-item').forEach(function (el) {
            el.classList.toggle('active', el.getAttribute('data-tipo') === tipo);
        });
        toggleControlesExportacion();
        cargarReporte(tipo);
    }

    function toggleControlesExportacion() {
        var esVentas = ['GENERAL', 'POS', 'WEB'].indexOf(tipoActual) >= 0;
        var exportBtns = document.getElementById('repExportBtns');
        var resumenPanel = document.getElementById('repResumenPanel');
        if (exportBtns) {
            exportBtns.classList.toggle('d-none', !esVentas);
        }
        if (resumenPanel && !esVentas) {
            resumenPanel.classList.add('d-none');
        }
    }

    function obtenerFiltros() {
        var periodo = document.getElementById('filtroPeriodo').value;
        var filtro = { periodo: periodo };

        if (periodo === 'RANGO') {
            var desde = document.getElementById('filtroDesde').value;
            var hasta = document.getElementById('filtroHasta').value;
            if (desde) filtro.fechaDesde = desde;
            if (hasta) filtro.fechaHasta = hasta;
        }

        var cliente = document.getElementById('filtroCliente').value;
        if (cliente) filtro.clienteId = parseInt(cliente, 10);

        var vendedor = document.getElementById('filtroVendedor').value;
        if (vendedor) filtro.vendedorId = parseInt(vendedor, 10);

        var categoria = document.getElementById('filtroCategoria').value;
        if (categoria) filtro.categoriaId = parseInt(categoria, 10);

        var producto = document.getElementById('filtroProducto').value;
        if (producto) filtro.productoId = parseInt(producto, 10);

        var marca = document.getElementById('filtroMarca').value.trim();
        if (marca) filtro.marca = marca;

        var tipoPago = document.getElementById('filtroTipoPago').value;
        if (tipoPago) filtro.tipoPagoId = parseInt(tipoPago, 10);

        var tipoComp = document.getElementById('filtroTipoComprobante').value;
        if (tipoComp) filtro.tipoComprobanteId = parseInt(tipoComp, 10);

        var estado = document.getElementById('filtroEstado').value;
        if (estado) filtro.estadoVenta = estado;

        var canal = document.getElementById('filtroCanal').value;
        if (canal) filtro.canal = canal;

        return filtro;
    }

    function limpiarFiltros() {
        document.getElementById('formFiltrosReportes').reset();
        document.getElementById('filtroPeriodo').value = 'TODO';
        toggleRangoFechas();
        aplicarFiltros();
    }

    function aplicarFiltros() {
        mostrarCargando(true);
        var filtro = obtenerFiltros();

        Promise.all([
            postJson('/reportes/api/dashboard', filtro),
            postJson('/reportes/api/consultar/' + tipoActual, filtro)
        ]).then(function (results) {
            actualizarKpis(results[0].kpis || {});
            actualizarGraficos(results[0].graficos || {});
            actualizarGerencial(results[0].gerencial || {});
            renderTabla(results[1]);
            mostrarCargando(false);
        }).catch(function (err) {
            mostrarError(err.message || 'Error al actualizar reportes');
            mostrarCargando(false);
        });
    }

    function cargarReporte(tipo) {
        mostrarCargando(true);
        postJson('/reportes/api/consultar/' + tipo, obtenerFiltros())
            .then(function (data) {
                renderTabla(data);
                mostrarCargando(false);
            })
            .catch(function (err) {
                mostrarError(err.message);
                mostrarCargando(false);
            });
    }

    function cargarCatalogoFiltros() {
        return fetch('/reportes/api/filtros')
            .then(function (r) {
                if (!r.ok) throw new Error('No se pudo cargar filtros');
                return r.json();
            })
            .then(function (cat) {
                llenarSelect('filtroCliente', cat.clientes);
                llenarSelect('filtroVendedor', cat.vendedores);
                llenarSelect('filtroCategoria', cat.categorias);
                llenarSelect('filtroProducto', cat.productos);
                llenarSelect('filtroTipoPago', cat.tiposPago);
                llenarSelect('filtroTipoComprobante', cat.tiposComprobante);
                llenarSelect('filtroCanal', cat.canales, 'codigo');

                var selEstado = document.getElementById('filtroEstado');
                (cat.estadosVenta || []).forEach(function (e) {
                    var opt = document.createElement('option');
                    opt.value = e;
                    opt.textContent = e;
                    selEstado.appendChild(opt);
                });
            });
    }

    function llenarSelect(id, items, keyField) {
        var sel = document.getElementById(id);
        var key = keyField || 'id';
        (items || []).forEach(function (item) {
            if (item.codigo === '' && id === 'filtroCanal') return;
            var opt = document.createElement('option');
            opt.value = item[key];
            opt.textContent = item.nombre;
            sel.appendChild(opt);
        });
    }

    function actualizarKpis(kpis) {
        document.querySelectorAll('.rep-kpi-card[data-kpi]').forEach(function (card) {
            var key = card.getAttribute('data-kpi');
            if (key === 'canal') {
                document.getElementById('kpiPos').textContent = formatoMoneda(kpis.ventasPos);
                document.getElementById('kpiWeb').textContent = formatoMoneda(kpis.ventasWeb);
                return;
            }
            var val = kpis[key];
            var el = card.querySelector('.rep-kpi-value');
            if (!el) return;
            if (key === 'cantidadVentas' || key === 'productosVendidos' || key === 'clientesAtendidos') {
                el.textContent = formatoNumero(val);
            } else {
                el.textContent = formatoMoneda(val);
            }
        });
    }

    function actualizarGerencial(g) {
        document.getElementById('gerCrecimiento').textContent = (g.crecimientoVentas != null ? g.crecimientoVentas : 0) + '%';
        document.getElementById('gerMargen').textContent = (g.margenGanancia != null ? g.margenGanancia : 0) + '%';
        document.getElementById('gerCanceladas').textContent = g.ventasCanceladas != null ? g.ventasCanceladas : 0;
        document.getElementById('gerDevoluciones').textContent = g.devoluciones != null ? g.devoluciones : 0;

        var stockBajo = g.productosStockBajo || [];
        var texto = stockBajo.length
            ? stockBajo.map(function (p) {
                return (p.productoNombre || p.Producto || '—') + ' (' + (p.stock != null ? p.stock : '?') + ')';
            }).join(', ')
            : 'Sin alertas';
        document.getElementById('gerStockBajo').textContent = texto;
    }

    function actualizarGraficos(graficos) {
        if (typeof Chart === 'undefined') return;

        crearOActualizarBarra('chartVentasDia', graficos.ventasPorDia, 'Ventas (S/)');
        crearOActualizarBarra('chartVentasMes', graficos.ventasPorMes, 'Ventas (S/)');
        crearOActualizarDoughnut('chartComparativo', graficos.comparativoCanal);
        crearOActualizarDoughnut('chartMetodosPago', graficos.metodosPago);
        crearOActualizarBarra('chartVentasHora', graficos.ventasPorHora, 'Operaciones', false);
        crearOActualizarBarra('chartTopProductos', graficos.topProductos, 'Cantidad', false);
        crearOActualizarBarra('chartTopCategorias', graficos.topCategorias, 'Monto (S/)');
        crearOActualizarBarra('chartTopClientes', graficos.topClientes, 'Monto (S/)');
        crearOActualizarBarra('chartTopVendedores', graficos.topVendedores, 'Monto (S/)');
        crearOActualizarLinea('chartEvolucion', graficos.evolucionUtilidad);
    }

    function crearOActualizarBarra(canvasId, data, label, esMoneda) {
        if (esMoneda === undefined) esMoneda = true;
        var ctx = document.getElementById(canvasId);
        if (!ctx || !data) return;

        var labels = data.labels || [];
        var valores = (data.valores || []).map(Number);

        if (charts[canvasId]) {
            charts[canvasId].data.labels = labels;
            charts[canvasId].data.datasets[0].data = valores;
            charts[canvasId].update('none');
            return;
        }

        charts[canvasId] = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: label,
                    data: valores,
                    backgroundColor: COLORES.map(function (c) { return c + '99'; }),
                    borderColor: COLORES,
                    borderWidth: 1,
                    borderRadius: 4,
                    maxBarThickness: 40
                }]
            },
            options: chartOpts(esMoneda)
        });
    }

    function crearOActualizarDoughnut(canvasId, data) {
        var ctx = document.getElementById(canvasId);
        if (!ctx || !data) return;

        var labels = data.labels || [];
        var valores = (data.valores || []).map(Number);

        if (charts[canvasId]) {
            charts[canvasId].data.labels = labels;
            charts[canvasId].data.datasets[0].data = valores;
            charts[canvasId].update('none');
            return;
        }

        charts[canvasId] = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: valores,
                    backgroundColor: COLORES.slice(0, labels.length),
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, padding: 8, font: { size: 11 } } } }
            }
        });
    }

    function crearOActualizarLinea(canvasId, data) {
        var ctx = document.getElementById(canvasId);
        if (!ctx || !data) return;

        var labels = data.labels || [];
        var valores = (data.valores || []).map(Number);

        if (charts[canvasId]) {
            charts[canvasId].data.labels = labels;
            charts[canvasId].data.datasets[0].data = valores;
            charts[canvasId].update('none');
            return;
        }

        charts[canvasId] = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Utilidad (S/)',
                    data: valores,
                    borderColor: '#6d28d9',
                    backgroundColor: 'rgba(109, 40, 217, 0.1)',
                    fill: true,
                    tension: 0.35,
                    pointRadius: 3
                }]
            },
            options: chartOpts(true)
        });
    }

    function chartOpts(esMoneda) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: esMoneda ? {
                        label: function (ctx) {
                            return 'S/ ' + Number(ctx.raw || 0).toFixed(2);
                        }
                    } : {}
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: esMoneda ? {
                        callback: function (v) { return 'S/ ' + v; }
                    } : {}
                }
            }
        };
    }

    function renderTabla(data) {
        tituloActual = data.titulo || 'Reporte';
        columnasActuales = data.columnas || [];
        filasActuales = data.filas || [];
        permiteDetalle = ['GENERAL', 'POS', 'WEB'].indexOf(tipoActual) >= 0;

        document.getElementById('repTablaTitulo').innerHTML =
            '<i class="fas fa-table mr-1"></i> ' + escapeHtml(tituloActual) +
            ' <small class="ml-2 opacity-75">(' + filasActuales.length + ' registros)</small>';

        if (dataTable) {
            dataTable.destroy();
            dataTable = null;
        }

        var thead = document.getElementById('tablaReporteHead');
        var tbody = document.getElementById('tablaReporteBody');
        thead.innerHTML = '';
        tbody.innerHTML = '';

        if (filasActuales.length === 0) {
            columnasActuales.forEach(function (col) {
                var th = document.createElement('th');
                th.textContent = col;
                thead.appendChild(th);
            });
            if (permiteDetalle) {
                var thAccEmpty = document.createElement('th');
                thAccEmpty.textContent = 'Detalle';
                thAccEmpty.className = 'col-acciones text-center';
                thead.appendChild(thAccEmpty);
            }
            var trEmpty = document.createElement('tr');
            var tdEmpty = document.createElement('td');
            tdEmpty.colSpan = Math.max(columnasActuales.length + (permiteDetalle ? 1 : 0), 1);
            tdEmpty.className = 'text-center text-muted py-4';
            tdEmpty.textContent = 'No hay registros para los filtros seleccionados. Pruebe ampliar el período a "Todo el historial".';
            trEmpty.appendChild(tdEmpty);
            tbody.appendChild(trEmpty);
            initDataTable();
            actualizarResumen(data);
            return;
        }

        columnasActuales.forEach(function (col) {
            var th = document.createElement('th');
            th.textContent = col;
            thead.appendChild(th);
        });
        if (permiteDetalle) {
            var thAcc = document.createElement('th');
            thAcc.textContent = 'Detalle';
            thAcc.className = 'col-acciones text-center';
            thead.appendChild(thAcc);
        }

        filasActuales.forEach(function (fila) {
            var tr = document.createElement('tr');
            var ventaId = fila.id != null ? fila.id : null;
            if (permiteDetalle && ventaId) {
                tr.className = 'rep-row-detalle';
                tr.setAttribute('data-venta-id', ventaId);
            }
            columnasActuales.forEach(function (col) {
                var td = document.createElement('td');
                td.textContent = fila[col] != null ? fila[col] : '—';
                tr.appendChild(td);
            });
            if (permiteDetalle) {
                var tdBtn = document.createElement('td');
                tdBtn.className = 'text-center col-acciones';
                if (ventaId) {
                    tdBtn.innerHTML = '<button type="button" class="btn btn-xs btn-info rep-btn-detalle" data-venta-id="' +
                        ventaId + '"><i class="fas fa-eye"></i></button>';
                }
                tr.appendChild(tdBtn);
            }
            tbody.appendChild(tr);
        });

        initDataTable();
        actualizarResumen(data);
    }

    function actualizarResumen(data) {
        var panel = document.getElementById('repResumenPanel');
        if (!panel) return;

        var esVentas = ['GENERAL', 'POS', 'WEB'].indexOf(tipoActual) >= 0;
        if (!esVentas || !data || !data.resumen) {
            panel.classList.add('d-none');
            return;
        }

        var r = data.resumen;
        panel.classList.remove('d-none');
        document.getElementById('resTotalVentas').textContent = formatoNumero(r.totalVentas);
        document.getElementById('resComprobantes').textContent = formatoNumero(r.cantidadComprobantes);
        document.getElementById('resClientes').textContent = formatoNumero(r.totalClientes);
        document.getElementById('resVendido').textContent = r.totalVendido || 'S/ 0.00';
        document.getElementById('resDescuentos').textContent = r.totalDescuentos || 'S/ 0.00';
        document.getElementById('resImpuestos').textContent = r.totalImpuestos || 'S/ 0.00';
        document.getElementById('resPromedio').textContent = r.promedioVenta || 'S/ 0.00';
        document.getElementById('resRango').textContent = data.rangoFechas || '—';
    }

    function initDataTable() {
        var $table = $('#tablaReporte');
        var dom = "<'row align-items-center justify-content-between mb-2 px-2'<'col-sm-6'l><'col-sm-6'f>>" +
            "<'row'<'col-12'tr>>" +
            "<'row align-items-center justify-content-between mt-2 px-2'<'col-sm-6'i><'col-sm-6'p>>";

        dataTable = $table.DataTable({
            responsive: true,
            autoWidth: false,
            pageLength: 10,
            lengthMenu: [[10, 25, 50, 100, -1], [10, 25, 50, 100, 'Todos']],
            order: [],
            language: {
                search: 'Buscar:',
                lengthMenu: 'Mostrar _MENU_',
                info: '_START_ a _END_ de _TOTAL_',
                infoEmpty: 'Sin registros',
                infoFiltered: '(de _MAX_)',
                zeroRecords: 'Sin resultados',
                emptyTable: 'Sin datos',
                paginate: { first: '«', last: '»', next: '›', previous: '‹' }
            },
            dom: dom,
            columnDefs: permiteDetalle ? [{ orderable: false, searchable: false, targets: -1 }] : []
        });
    }

    function abrirDetalleVenta(id) {
        var body = document.getElementById('modalDetalleVentaBody');
        body.innerHTML = '<div class="text-center py-4"><i class="fas fa-spinner fa-spin"></i> Cargando...</div>';
        $('#modalDetalleVenta').modal('show');

        fetch('/reportes/api/detalle-venta/' + id)
            .then(function (r) {
                if (!r.ok) throw new Error('Venta no encontrada');
                return r.json();
            })
            .then(function (v) {
                var html = '<div class="row mb-3">' +
                    '<div class="col-md-6"><strong>Documento:</strong> ' + escapeHtml(v.numeroDocumento) + '</div>' +
                    '<div class="col-md-6"><strong>Fecha:</strong> ' + escapeHtml(v.fecha) + '</div>' +
                    '<div class="col-md-6"><strong>Cliente:</strong> ' + escapeHtml(v.cliente) + '</div>' +
                    '<div class="col-md-6"><strong>Vendedor:</strong> ' + escapeHtml(v.vendedor) + '</div>' +
                    '<div class="col-md-6"><strong>Pago:</strong> ' + escapeHtml(v.tipoPago) + '</div>' +
                    '<div class="col-md-6"><strong>Estado:</strong> ' + escapeHtml(v.estado) + '</div>' +
                    '</div><table class="table table-sm table-bordered"><thead><tr>' +
                    '<th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead><tbody>';

                (v.productos || []).forEach(function (p) {
                    html += '<tr><td>' + escapeHtml(p.producto) + '</td><td>' + p.cantidad +
                        '</td><td>S/ ' + Number(p.precio).toFixed(2) + '</td><td>S/ ' +
                        Number(p.subtotal).toFixed(2) + '</td></tr>';
                });

                html += '</tbody></table><div class="text-right font-weight-bold">Total: S/ ' +
                    Number(v.total).toFixed(2) + '</div>';
                body.innerHTML = html;
            })
            .catch(function (err) {
                body.innerHTML = '<div class="alert alert-danger mb-0">' + escapeHtml(err.message) + '</div>';
            });
    }

    function exportar(formato) {
        if (['GENERAL', 'POS', 'WEB'].indexOf(tipoActual) < 0) {
            mostrarError('La exportación solo está disponible para reportes de ventas (General, POS o Web).');
            return;
        }

        var filtro = obtenerFiltros();
        var url = formato === 'PDF'
            ? '/reportes/api/exportar/' + tipoActual + '/pdf'
            : '/reportes/api/exportar/' + tipoActual + '/excel';
        var ext = formato === 'PDF' ? 'pdf' : 'xlsx';
        var nombre = 'reporte-' + tipoActual.toLowerCase() + '.' + ext;

        mostrarCargando(true);
        postBlob(url, filtro)
            .then(function (blob) {
                descargarBlob(blob, nombre);
                mostrarCargando(false);
            })
            .catch(function (err) {
                mostrarError('Error al exportar: ' + (err.message || 'intente de nuevo'));
                mostrarCargando(false);
            });
    }

    function postJson(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body || {})
        }).then(function (r) {
            if (!r.ok) {
                return r.text().then(function (t) {
                    throw new Error(parsearMensajeError(t, r.status));
                });
            }
            return r.json();
        });
    }

    function parsearMensajeError(texto, status) {
        if (!texto) {
            return 'Error al cargar reportes (código ' + status + ')';
        }
        try {
            var json = JSON.parse(texto);
            if (json.message) return json.message;
            if (json.error) return json.error;
        } catch (e) { /* no es JSON */ }
        if (texto.length > 200) {
            return 'Error del servidor al cargar reportes. Reinicie la aplicación e intente de nuevo.';
        }
        return texto;
    }

    function postBlob(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body || {})
        }).then(function (r) {
            if (!r.ok) throw new Error('Error al exportar');
            return r.blob();
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
        document.getElementById('repLoading').classList.toggle('d-none', !show);
    }

    function mostrarError(msg) {
        var el = document.getElementById('reportes-alerta');
        el.textContent = msg;
        el.classList.remove('d-none');
        setTimeout(function () { el.classList.add('d-none'); }, 5000);
    }

    function formatoMoneda(val) {
        var n = Number(val || 0);
        return 'S/ ' + n.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function formatoNumero(val) {
        return Number(val || 0).toLocaleString('es-PE');
    }

    function escapeHtml(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function debounce(fn, ms) {
        var t;
        return function () {
            clearTimeout(t);
            var args = arguments;
            var ctx = this;
            t = setTimeout(function () { fn.apply(ctx, args); }, ms);
        };
    }
})();
