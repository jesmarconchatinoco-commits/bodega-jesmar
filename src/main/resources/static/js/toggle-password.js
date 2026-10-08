window.togglePass = function (inputId, btn) {
    var input = document.getElementById(inputId);
    if (!input || !btn) {
        return;
    }

    var icono = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        if (icono) {
            icono.classList.remove('fa-eye');
            icono.classList.add('fa-eye-slash');
        }
        btn.setAttribute('aria-label', 'Ocultar contraseña');
        btn.setAttribute('title', 'Ocultar contraseña');
    } else {
        input.type = 'password';
        if (icono) {
            icono.classList.remove('fa-eye-slash');
            icono.classList.add('fa-eye');
        }
        btn.setAttribute('aria-label', 'Mostrar contraseña');
        btn.setAttribute('title', 'Mostrar contraseña');
    }
};
