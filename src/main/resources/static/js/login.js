document.addEventListener('DOMContentLoaded', function () {

    const alertLogout = document.getElementById('alert-logout');
    if (alertLogout) {
        setTimeout(function () {
            alertLogout.style.transition = 'opacity 0.4s ease';
            alertLogout.style.opacity = '0';
            setTimeout(function () {
                alertLogout.remove();
            }, 400);
        }, 5000);
    }

    const form = document.querySelector('form');

    const usuario = document.getElementById('usuario');

    const password = document.getElementById('password');



    if (!form || !usuario || !password) {

        return;

    }



    const soloLetras = /[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]/;

    const regUsuario = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+$/;

    const teclasPermitidas = ['Backspace', 'Delete', 'Tab', 'ArrowLeft', 'ArrowRight', 'Home', 'End'];



    function filtrarUsuario() {

        const limpio = usuario.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]/g, '');

        if (usuario.value !== limpio) {

            usuario.value = limpio;

        }

    }



    usuario.addEventListener('input', filtrarUsuario);



    usuario.addEventListener('keydown', function (e) {

        if (e.ctrlKey || e.metaKey || e.altKey || teclasPermitidas.includes(e.key)) {

            return;

        }

        if (!soloLetras.test(e.key)) {

            e.preventDefault();

        }

    });



    usuario.addEventListener('paste', function (e) {

        e.preventDefault();

        const texto = e.clipboardData.getData('text').replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]/g, '');

        const inicio = usuario.selectionStart;

        const fin = usuario.selectionEnd;

        usuario.value = usuario.value.slice(0, inicio) + texto + usuario.value.slice(fin);

        usuario.selectionStart = usuario.selectionEnd = inicio + texto.length;

    });



    form.addEventListener('submit', function (e) {

        const valor = usuario.value.trim();

        if (!valor) {

            e.preventDefault();

            usuario.focus();

            return;

        }

        if (!regUsuario.test(valor)) {

            e.preventDefault();

            usuario.focus();

            return;

        }

        if (!password.value.trim()) {

            e.preventDefault();

            password.focus();

        }

    });

});

