/**
 * Inicializa tablas responsivas con DataTables en todos los módulos.
 */
$(document).ready(function () {
    if (typeof $.fn.DataTable === 'undefined') {
        return;
    }

    var domResponsive =
        "<'row align-items-center justify-content-between mb-3 px-2 px-md-3'<'col-sm-6'l><'col-sm-6'f>>" +
        "<'row'<'col-12'tr>>" +
        "<'row align-items-center justify-content-between mt-3 px-2 px-md-3'<'col-sm-6'i><'col-sm-6'p>>";

    var domPedidos =
        "<'pedidos-dt-toolbar'<'pedidos-dt-length'l><'pedidos-dt-filter'f>>" +
        "<'pedidos-dt-scroll'tr>" +
        "<'pedidos-dt-footer'<'pedidos-dt-info'i><'pedidos-dt-paginate'p>>";

    $('.tabla-modulo').each(function () {
        var $table = $(this);
        if ($.fn.DataTable.isDataTable($table)) {
            return;
        }

        var orderCol = $table.data('order-col');
        var orderDir = $table.data('order-dir') || 'asc';
        var order = orderCol !== undefined && orderCol !== ''
            ? [[parseInt(orderCol, 10), orderDir]]
            : [];

        var numCols = $table.find('thead th').length;
        var esTablaVentas = $table.hasClass('tabla-ventas');
        var esTablaPedidos = $table.hasClass('tabla-pedidos');
        var columnDefs = [];

        if (numCols > 0) {
            columnDefs.push({
                orderable: false,
                searchable: false,
                className: esTablaVentas ? 'ventas-col-acciones' : 'col-acciones',
                targets: -1
            });
        }

        if (esTablaVentas) {
            columnDefs.push(
                { responsivePriority: 1, targets: 2 },
                { responsivePriority: 1, targets: 5 },
                { responsivePriority: 1, targets: 6 },
                { responsivePriority: 1, targets: 7 },
                { responsivePriority: 2, targets: 0 },
                { responsivePriority: 2, targets: 1 },
                { responsivePriority: 2, targets: 4 },
                { responsivePriority: 4, targets: 3 }
            );
        } else if (numCols >= 5 && !esTablaPedidos) {
            columnDefs.push(
                { responsivePriority: 1, targets: -1 },
                { responsivePriority: 2, targets: 0 },
                { responsivePriority: 3, targets: 1 }
            );
        }

        var opcionesTabla = {
            autoWidth: false,
            pageLength: 10,
            lengthMenu: [[10, 25, 50, -1], [10, 25, 50, 'Todos']],
            order: order,
            columnDefs: columnDefs,
            initComplete: function () {
                var api = this.api();
                api.columns.adjust();
                if (api.responsive) {
                    api.responsive.recalc();
                }
            },
            language: {
                search: 'Buscar:',
                lengthMenu: 'Mostrar _MENU_ registros',
                info: 'Mostrando _START_ a _END_ de _TOTAL_ registros',
                infoEmpty: 'Sin registros',
                infoFiltered: '(filtrado de _MAX_ en total)',
                zeroRecords: 'No se encontraron resultados',
                emptyTable: 'No hay datos disponibles',
                paginate: {
                    first: 'Primero',
                    last: 'Último',
                    next: 'Siguiente',
                    previous: 'Anterior'
                }
            },
            dom: esTablaPedidos ? domPedidos : domResponsive
        };

        if (esTablaPedidos) {
            opcionesTabla.responsive = false;
            opcionesTabla.scrollX = false;
            opcionesTabla.autoWidth = true;
        } else {
            opcionesTabla.responsive = {
                details: {
                    type: 'column',
                    target: 'tr'
                }
            };
            opcionesTabla.scrollX = false;
            opcionesTabla.scrollCollapse = false;
        }

        $table.DataTable(opcionesTabla);
    });
});
