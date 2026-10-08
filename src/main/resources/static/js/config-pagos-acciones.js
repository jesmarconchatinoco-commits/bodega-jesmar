(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('.form-config-pago').forEach(function (form) {
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                guardar(form);
            });
        });
    });

    function guardar(form) {
        var fd = new FormData(form);
        var btn = form.querySelector('button[type="submit"]');
        btn.disabled = true;

        fetch('/config-pagos/api/guardar', { method: 'POST', body: fd })
            .then(function (r) { return r.json(); })
            .then(function (res) {
                mostrarAlerta(res.message || 'Guardado.', res.success !== false ? 'success' : 'danger');
                if (res.success !== false) {
                    setTimeout(function () { window.location.reload(); }, 800);
                } else {
                    btn.disabled = false;
                }
            })
            .catch(function () {
                mostrarAlerta('Error al guardar la configuración.', 'danger');
                btn.disabled = false;
            });
    }

    function mostrarAlerta(msg, tipo) {
        var el = document.getElementById('config-pago-alerta');
        el.className = 'alert alert-' + tipo;
        el.textContent = msg;
        el.classList.remove('d-none');
    }
})();
