document.addEventListener('DOMContentLoaded', function () {
    if (window.ContrasenaSegura) {
        ContrasenaSegura.init({
            formId: 'form-restablecer',
            passwordId: 'password',
            confirmacionId: 'confirmacion',
            errorId: 'error-cliente',
            reglaLongitudId: 'regla-longitud',
            reglaLetrasId: 'regla-letras',
            reglaNumerosId: 'regla-numeros',
            reglaPuntosId: 'regla-puntos',
            reglaCoincidenId: 'regla-coinciden'
        });
    }
});
