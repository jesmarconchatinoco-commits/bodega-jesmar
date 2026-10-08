(function () {
    'use strict';

    var fechasData = [];

    function mostrarToast(mensaje, tipo) {
        var cls = tipo === 'success' ? 'alert-success' : (tipo === 'warning' ? 'alert-warning' : 'alert-danger');
        var $toast = $('<div class="alert ' + cls + ' alert-dismissible fade show shadow-sm" role="alert">' +
            mensaje +
            '<button type="button" class="close" data-dismiss="alert"><span>&times;</span></button></div>');
        $('body').append($('<div class="position-fixed" style="top:80px;right:20px;z-index:9999;max-width:380px;"></div>').append($toast));
        setTimeout(function () { $toast.alert('close'); }, 4500);
    }

    function cargarFechas() {
        $('#horariosContenedor').html('<p class="text-muted text-center py-4 mb-0">Cargando horarios...</p>');
        $.get('/horarios-atencion/api/list')
            .done(function (data) {
                fechasData = data || [];
                renderListado();
            })
            .fail(function () {
                $('#horariosContenedor').html('<p class="text-danger text-center py-4 mb-0">No se pudieron cargar los horarios.</p>');
            });
    }

    function renderListado() {
        if (!fechasData.length) {
            $('#horariosContenedor').html(
                '<div class="text-center text-muted py-4">' +
                '<i class="fas fa-calendar-plus fa-2x mb-3 d-block"></i>' +
                'No hay fechas de atención registradas.<br>' +
                '<button type="button" class="btn btn-primary btn-sm mt-3" id="btnNuevaFechaEmpty">' +
                '<i class="fas fa-plus mr-1"></i> Crear primera fecha</button></div>'
            );
            $('#btnNuevaFechaEmpty').on('click', function () { abrirModal(null); });
            return;
        }

        var html = '';
        fechasData.forEach(function (fecha) {
            var estadoBadge = fecha.activo
                ? '<span class="badge badge-success">Habilitada</span>'
                : '<span class="badge badge-secondary">Deshabilitada</span>';

            var horariosHtml = '';
            (fecha.horarios || []).forEach(function (h) {
                var hEstado = h.activo
                    ? (h.completo ? '<span class="badge badge-warning">Completo</span>' : '<span class="badge badge-success">Disponible</span>')
                    : '<span class="badge badge-secondary">Inactivo</span>';

                horariosHtml += '<tr>' +
                    '<td>' + h.label + '</td>' +
                    '<td class="text-center">' + h.cupoMaximo + '</td>' +
                    '<td class="text-center">' + h.reservados + '</td>' +
                    '<td class="text-center">' + h.disponibles + '</td>' +
                    '<td class="text-center">' + hEstado + '</td>' +
                    '<td class="text-center">' +
                    '<button type="button" class="btn btn-xs btn-outline-secondary btn-toggle-horario" data-id="' + h.id + '" data-activo="' + h.activo + '" title="Habilitar/Deshabilitar">' +
                    '<i class="fas fa-power-off"></i></button></td></tr>';
            });

            html += '<div class="card mb-3 shadow-sm">' +
                '<div class="card-header d-flex flex-wrap justify-content-between align-items-center">' +
                '<div><strong>' + fecha.fechaLabel + '</strong> ' + estadoBadge + '</div>' +
                '<div class="mt-2 mt-md-0">' +
                '<button type="button" class="btn btn-info btn-sm mr-1 btn-editar-fecha" data-id="' + fecha.id + '"><i class="fas fa-edit"></i> Editar</button>' +
                '<button type="button" class="btn btn-outline-secondary btn-sm mr-1 btn-toggle-fecha" data-id="' + fecha.id + '" data-activo="' + fecha.activo + '">' +
                (fecha.activo ? '<i class="fas fa-ban"></i> Deshabilitar' : '<i class="fas fa-check"></i> Habilitar') + '</button>' +
                '<button type="button" class="btn btn-danger btn-sm btn-eliminar-fecha" data-id="' + fecha.id + '"><i class="fas fa-trash"></i></button>' +
                '</div></div>' +
                '<div class="card-body p-0"><table class="table table-sm table-striped mb-0">' +
                '<thead><tr><th>Horario</th><th class="text-center">Cupo máx.</th><th class="text-center">Reservados</th>' +
                '<th class="text-center">Disponibles</th><th class="text-center">Estado</th><th class="text-center">Acción</th></tr></thead>' +
                '<tbody>' + (horariosHtml || '<tr><td colspan="6" class="text-center text-muted">Sin horarios</td></tr>') + '</tbody></table></div></div>';
        });

        $('#horariosContenedor').html(html);
    }

    function abrirModal(fecha) {
        $('#fechaAtencionId').val(fecha ? fecha.id : '');
        $('#fechaAtencionInput').val(fecha ? fecha.fecha : '');
        $('#fechaAtencionActivo').val(fecha && !fecha.activo ? 'false' : 'true');

        var hoy = new Date().toISOString().split('T')[0];
        $('#fechaAtencionInput').attr('min', hoy);

        $('#tablaHorariosModal').empty();

        if (fecha && fecha.horarios && fecha.horarios.length) {
            fecha.horarios.forEach(function (h) {
                agregarFilaHorario(h);
            });
        } else {
            agregarFilaHorario(null);
        }

        $('#modalHorarioTitulo').html('<i class="fas fa-clock mr-2"></i>' + (fecha ? 'Editar fecha de atención' : 'Nueva fecha de atención'));
        $('#modalHorarioAtencion').modal('show');
    }

    function agregarFilaHorario(data) {
        var reservados = data ? data.reservados : 0;
        var idAttr = data && data.id ? ' data-id="' + data.id + '"' : '';
        var disabledEliminar = reservados > 0 ? ' disabled title="Tiene reservas confirmadas"' : '';

        var fila = '<tr' + idAttr + '>' +
            '<td><input type="time" class="form-control form-control-sm hora-inicio" value="' + (data ? data.horaInicio : '09:00') + '"></td>' +
            '<td><input type="time" class="form-control form-control-sm hora-fin" value="' + (data ? data.horaFin : '10:00') + '"></td>' +
            '<td><input type="number" min="1" class="form-control form-control-sm cupo-maximo text-center" value="' + (data ? data.cupoMaximo : 5) + '"></td>' +
            '<td class="text-center align-middle"><span class="badge badge-info reservados-label">' + reservados + '</span></td>' +
            '<td><select class="form-control form-control-sm horario-activo"><option value="true"' + (data && !data.activo ? '' : ' selected') + '>Activo</option>' +
            '<option value="false"' + (data && !data.activo ? ' selected' : '') + '>Inactivo</option></select></td>' +
            '<td class="text-center align-middle"><button type="button" class="btn btn-danger btn-xs btn-quitar-horario"' + disabledEliminar + '><i class="fas fa-times"></i></button></td>' +
            '</tr>';

        $('#tablaHorariosModal').append(fila);
    }

    function recolectarHorarios() {
        var horarios = [];
        $('#tablaHorariosModal tr').each(function () {
            var $row = $(this);
            var item = {
                horaInicio: $row.find('.hora-inicio').val(),
                horaFin: $row.find('.hora-fin').val(),
                cupoMaximo: parseInt($row.find('.cupo-maximo').val(), 10) || 1,
                activo: $row.find('.horario-activo').val() === 'true'
            };
            var id = $row.data('id');
            if (id) item.id = id;
            horarios.push(item);
        });
        return horarios;
    }

    function guardarFecha() {
        var id = $('#fechaAtencionId').val();
        var payload = {
            fecha: $('#fechaAtencionInput').val(),
            activo: $('#fechaAtencionActivo').val() === 'true',
            horarios: recolectarHorarios()
        };

        if (!payload.fecha) {
            mostrarToast('Seleccione la fecha de atención.', 'warning');
            return;
        }
        if (!payload.horarios.length) {
            mostrarToast('Agregue al menos un horario.', 'warning');
            return;
        }

        var req;
        if (id) {
            req = $.ajax({
                url: '/horarios-atencion/api/fecha/' + id,
                method: 'PUT',
                contentType: 'application/json',
                data: JSON.stringify(payload)
            });
        } else {
            req = $.ajax({
                url: '/horarios-atencion/api/fecha',
                method: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload)
            });
        }

        req.done(function (res) {
            mostrarToast('<i class="fas fa-check-circle mr-1"></i>' + res.message, 'success');
            $('#modalHorarioAtencion').modal('hide');
            cargarFechas();
        }).fail(function (xhr) {
            var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo guardar la fecha.';
            mostrarToast('<i class="fas fa-exclamation-circle mr-1"></i>' + msg, 'danger');
        });
    }

    function toggleFecha(id, activo) {
        $.ajax({
            url: '/horarios-atencion/api/fecha/' + id + '/toggle?activo=' + activo,
            method: 'PATCH'
        }).done(function (res) {
            mostrarToast(res.message, 'success');
            cargarFechas();
        }).fail(function (xhr) {
            var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo actualizar la fecha.';
            mostrarToast(msg, 'danger');
        });
    }

    function toggleHorario(id, activo) {
        $.ajax({
            url: '/horarios-atencion/api/horario/' + id + '/toggle?activo=' + activo,
            method: 'PATCH'
        }).done(function (res) {
            mostrarToast(res.message, 'success');
            cargarFechas();
        }).fail(function (xhr) {
            var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo actualizar el horario.';
            mostrarToast(msg, 'danger');
        });
    }

    function eliminarFecha(id) {
        if (!confirm('¿Eliminar esta fecha de atención y todos sus horarios?')) return;
        $.ajax({ url: '/horarios-atencion/api/fecha/' + id, method: 'DELETE' })
            .done(function (res) {
                mostrarToast(res.message, 'success');
                cargarFechas();
            })
            .fail(function (xhr) {
                var msg = (xhr.responseJSON && xhr.responseJSON.message) ? xhr.responseJSON.message : 'No se pudo eliminar la fecha.';
                mostrarToast(msg, 'danger');
            });
    }

    $(function () {
        cargarFechas();

        $('#btnNuevaFecha').on('click', function () { abrirModal(null); });
        $('#btnAgregarHorario').on('click', function () { agregarFilaHorario(null); });
        $('#btnGuardarFechaAtencion').on('click', guardarFecha);

        $(document).on('click', '.btn-editar-fecha', function () {
            var id = parseInt($(this).data('id'), 10);
            var fecha = fechasData.find(function (f) { return f.id === id; });
            abrirModal(fecha || null);
        });

        $(document).on('click', '.btn-toggle-fecha', function () {
            var id = parseInt($(this).data('id'), 10);
            var activo = $(this).data('activo') === true || $(this).data('activo') === 'true';
            toggleFecha(id, !activo);
        });

        $(document).on('click', '.btn-toggle-horario', function () {
            var id = parseInt($(this).data('id'), 10);
            var activo = $(this).data('activo') === true || $(this).data('activo') === 'true';
            toggleHorario(id, !activo);
        });

        $(document).on('click', '.btn-eliminar-fecha', function () {
            eliminarFecha(parseInt($(this).data('id'), 10));
        });

        $(document).on('click', '.btn-quitar-horario', function () {
            if ($('#tablaHorariosModal tr').length <= 1) {
                mostrarToast('Debe mantener al menos un horario.', 'warning');
                return;
            }
            $(this).closest('tr').remove();
        });
    });
})();
