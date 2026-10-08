(function (global) {
    'use strict';

    var PATRON_SEGURO = /^(?=.*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ])(?=.*\d)(?=.*\.).{8,}$/;

    function marcarRegla(elemento, cumple) {
        if (!elemento) return;
        elemento.classList.toggle('text-success', cumple);
        elemento.classList.toggle('text-danger', !cumple);
        elemento.classList.toggle('text-muted', !cumple);
    }

    function validar(password, confirmacion) {
        if (!password) {
            return 'Ingrese la nueva contraseña.';
        }
        if (!confirmacion) {
            return 'Confirme la nueva contraseña.';
        }
        if (!PATRON_SEGURO.test(password)) {
            return 'La contraseña debe tener al menos 8 caracteres e incluir letras, números y puntos (.).';
        }
        if (password !== confirmacion) {
            return 'Las contraseñas no coinciden.';
        }
        return null;
    }

    function init(config) {
        var form = document.getElementById(config.formId);
        var password = document.getElementById(config.passwordId);
        var confirmacion = document.getElementById(config.confirmacionId);
        var errorCliente = config.errorId ? document.getElementById(config.errorId) : null;

        if (!form || !password || !confirmacion) {
            return;
        }

        var reglas = {
            longitud: config.reglaLongitudId ? document.getElementById(config.reglaLongitudId) : null,
            letras: config.reglaLetrasId ? document.getElementById(config.reglaLetrasId) : null,
            numeros: config.reglaNumerosId ? document.getElementById(config.reglaNumerosId) : null,
            puntos: config.reglaPuntosId ? document.getElementById(config.reglaPuntosId) : null,
            coinciden: config.reglaCoincidenId ? document.getElementById(config.reglaCoincidenId) : null
        };

        function actualizarReglas() {
            var valor = password.value || '';
            var confirm = confirmacion.value || '';

            marcarRegla(reglas.longitud, valor.length >= 8);
            marcarRegla(reglas.letras, /[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]/.test(valor));
            marcarRegla(reglas.numeros, /\d/.test(valor));
            marcarRegla(reglas.puntos, /\./.test(valor));
            marcarRegla(reglas.coinciden, valor.length > 0 && valor === confirm);
        }

        password.addEventListener('input', actualizarReglas);
        confirmacion.addEventListener('input', actualizarReglas);
        actualizarReglas();

        form.addEventListener('submit', function (e) {
            var error = validar(password.value, confirmacion.value);
            if (error) {
                e.preventDefault();
                if (errorCliente) {
                    errorCliente.textContent = error;
                    errorCliente.style.display = 'block';
                }
            } else if (errorCliente) {
                errorCliente.style.display = 'none';
            }
        });
    }

    global.ContrasenaSegura = {
        PATRON_SEGURO: PATRON_SEGURO,
        validar: validar,
        init: init
    };
})(window);
