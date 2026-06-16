// servicios.js - Lógica de la pantalla de pagos de servicios

document.addEventListener('DOMContentLoaded', function() {

    // Prevención del bfcache (botón "Atrás" del navegador)
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });

    // ==========================================
    // AUTO-OCULTAR ALERTAS EN SERVICIOS
    // ==========================================
    // ¡Ya no hay un segundo DOMContentLoaded aquí!
    const alertas = document.querySelectorAll('.alert');

    if (alertas.length > 0) {
        setTimeout(() => {
            alertas.forEach(alerta => {
                alerta.style.transition = "opacity 0.6s ease";
                alerta.style.opacity = "0";

                setTimeout(() => {
                    alerta.style.display = "none";
                    alerta.remove();
                }, 600);
            });
        }, 4000);
    }

}); // <-- Esta es la única llave que cierra el evento principal