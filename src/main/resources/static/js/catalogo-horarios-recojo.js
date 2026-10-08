(function () {
    'use strict';

    var modalInstance = null;
    var fechasData = [];
    var fechaSeleccionada = null;
    var horarioSeleccionado = null;
    var onSelectCallback = null;
    var onCancelCallback = null;
    var confirmado = false;

    function init() {
        var modalEl = document.getElementById('modalRecojoHorario');
        if (!modalEl) return;

        if (modalEl.parentElement !== document.body) {
            document.body.appendChild(modalEl);
        }

        modalEl.addEventListener('show.bs.modal', function () {
            var abiertos = document.querySelectorAll('.modal.show').length;
            var zIndex = 1055 + (10 * abiertos);
            modalEl.style.zIndex = String(zIndex);
            setTimeout(function () {
                var backdrops = document.querySelectorAll('.modal-backdrop');
                if (backdrops.length) {
                    backdrops[backdrops.length - 1].style.zIndex = String(zIndex - 1);
                }
            }, 0);
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            modalEl.style.zIndex = '';
            if (!confirmado && onCancelCallback) {
                onCancelCallback();
            }
            confirmado = false;
            horarioSeleccionado = null;
            onSelectCallback = null;
            onCancelCallback = null;
        });

        onClick('btnConfirmarRecojo', confirmarSeleccion);
    }

    function abrir(onSelect, onCancel) {
        onSelectCallback = onSelect;
        onCancelCallback = onCancel;
        confirmado = false;
        horarioSeleccionado = null;
        fechaSeleccionada = null;

        var modalEl = document.getElementById('modalRecojoHorario');
        var contenido = document.getElementById('recojoHorarioContenido');
        if (!modalEl || !contenido) return;

        contenido.innerHTML = '<div class="text-center text-muted py-4"><i class="fas fa-spinner fa-spin me-2"></i>Cargando horarios...</div>';
        var btnConfirmar = document.getElementById('btnConfirmarRecojo');
        if (btnConfirmar) btnConfirmar.disabled = true;

        if (window.bootstrap) {
            var existente = bootstrap.Modal.getInstance(modalEl);
            if (existente) existente.dispose();
            modalInstance = new bootstrap.Modal(modalEl, {
                backdrop: 'static',
                keyboard: false,
                focus: true
            });
            modalInstance.show();
        } else {
            modalEl.classList.add('show');
            modalEl.style.display = 'block';
            modalEl.removeAttribute('aria-hidden');
            document.body.classList.add('modal-open');
        }

        fetch('/catalogo/api/horarios-recojo')
            .then(function (r) { return r.json(); })
            .then(function (res) {
                fechasData = res.fechas || [];
                renderContenido();
            })
            .catch(function () {
                contenido.innerHTML = '<div class="alert alert-danger mb-0">No se pudieron cargar los horarios disponibles.</div>';
            });
    }

    function renderContenido() {
        var contenido = document.getElementById('recojoHorarioContenido');
        if (!contenido) return;

        if (!fechasData.length) {
            contenido.innerHTML =
                '<div class="text-center text-muted py-4">' +
                '<i class="fas fa-calendar-times fa-2x mb-3 d-block"></i>' +
                'No hay fechas de recojo disponibles en este momento.' +
                '</div>';
            return;
        }

        if (!fechaSeleccionada) {
            fechaSeleccionada = fechasData[0];
        }

        var fechasHtml = fechasData.map(function (f) {
            var active = fechaSeleccionada && f.id === fechaSeleccionada.id ? ' active' : '';
            return '<button type="button" class="cat-recojo-fecha-btn' + active + '" data-fecha-id="' + f.id + '">' +
                escapeHtml(f.fechaLabel) + '</button>';
        }).join('');

        var horariosHtml = '';
        if (fechaSeleccionada && fechaSeleccionada.horarios) {
            horariosHtml = fechaSeleccionada.horarios.map(function (h) {
                var selected = horarioSeleccionado && horarioSeleccionado.id === h.id ? ' selected' : '';
                var disabled = h.completo ? ' disabled completo' : '';
                var cuposText = h.completo
                    ? '<span class="badge bg-secondary">Completo</span>'
                    : '<span class="badge bg-success">' + h.disponibles + ' cupo(s)</span>';

                return '<button type="button" class="cat-recojo-horario-card' + selected + disabled + '" ' +
                    (h.completo ? 'disabled' : 'data-horario-id="' + h.id + '"') + '>' +
                    '<div class="hora">' + escapeHtml(h.label) + '</div>' +
                    '<div class="cupos">' + cuposText + '</div>' +
                    '</button>';
            }).join('');
        }

        contenido.innerHTML =
            '<div class="cat-recojo-fechas">' + fechasHtml + '</div>' +
            '<div class="cat-recojo-horarios">' + (horariosHtml || '<p class="text-muted small mb-0">Sin horarios para esta fecha.</p>') + '</div>';

        contenido.querySelectorAll('[data-fecha-id]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var id = parseInt(btn.dataset.fechaId, 10);
                fechaSeleccionada = fechasData.find(function (f) { return f.id === id; }) || fechasData[0];
                horarioSeleccionado = null;
                renderContenido();
                actualizarConfirmar();
            });
        });

        contenido.querySelectorAll('[data-horario-id]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var id = parseInt(btn.dataset.horarioId, 10);
                var horario = (fechaSeleccionada.horarios || []).find(function (h) { return h.id === id; });
                if (!horario || horario.completo) return;
                horarioSeleccionado = {
                    id: horario.id,
                    label: horario.label,
                    fechaLabel: fechaSeleccionada.fechaLabel,
                    fecha: fechaSeleccionada.fecha
                };
                renderContenido();
                actualizarConfirmar();
            });
        });
    }

    function confirmarSeleccion() {
        if (!horarioSeleccionado || !onSelectCallback) return;
        confirmado = true;
        var seleccion = horarioSeleccionado;
        if (modalInstance) {
            modalInstance.hide();
        } else {
            var modalEl = document.getElementById('modalRecojoHorario');
            if (modalEl) {
                modalEl.classList.remove('show');
                modalEl.style.display = 'none';
                modalEl.setAttribute('aria-hidden', 'true');
            }
        }
        onSelectCallback(seleccion);
    }

    function actualizarConfirmar() {
        var btn = document.getElementById('btnConfirmarRecojo');
        if (btn) btn.disabled = !horarioSeleccionado;
    }

    function onClick(id, handler) {
        var el = document.getElementById(id);
        if (el) el.addEventListener('click', handler);
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.textContent = text || '';
        return div.innerHTML;
    }

    window.CatalogoHorariosRecojo = {
        init: init,
        abrir: abrir
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
