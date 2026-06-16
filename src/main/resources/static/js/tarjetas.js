// tarjetas.js

document.addEventListener('DOMContentLoaded', function() {

    // Evitar caché de página (Atrás)
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) window.location.reload();
    });

    // Enviar el formulario automáticamente al cambiar el interruptor
    const toggleSeguridad = document.getElementById('toggleSeguridad');
    const formToggle = document.getElementById('formToggle');

    if (toggleSeguridad && formToggle) {
        toggleSeguridad.addEventListener('change', function() {
            // Cuando el usuario mueve el switch, enviamos la petición al servidor
            formToggle.submit();
        });
    }
});
document.addEventListener('DOMContentLoaded', function() {
    const toggle = document.getElementById('toggleSeguridad');
    const form = document.getElementById('formToggle');

    if (toggle) {
        toggle.addEventListener('change', function() {
            // Cuando el usuario mueve el switch, enviamos el formulario
            form.submit();
        });
    }

    // Auto-ocultar alertas (la misma lógica de siempre)
    const alertas = document.querySelectorAll('.alert');
    setTimeout(() => {
        alertas.forEach(a => a.style.display = 'none');
    }, 4000);
});