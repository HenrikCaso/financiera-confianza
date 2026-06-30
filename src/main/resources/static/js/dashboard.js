// dashboard.js - Lógica del panel principal

document.addEventListener('DOMContentLoaded', function() {
    // Bloqueo del botón "Atrás" del navegador (Efecto bfcache)
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });

    document.querySelectorAll('[data-dashboard-coming-soon]').forEach(function (elemento) {
        elemento.addEventListener('click', function () {
            alert('Función próximamente disponible');
        });
    });
}); // <--- AQUÍ CIERRA EL EVENT LISTENER

// LA FUNCIÓN DEBE ESTAR AFUERA, EN EL ENTORNO GLOBAL
function copiarAlPortapapeles(idElemento, botonRef) {
    // 1. Obtenemos el texto del span
    const texto = document.getElementById(idElemento).innerText;

    // 2. Usamos la API del portapapeles del navegador
    navigator.clipboard.writeText(texto).then(() => {

        // 3. Efecto visual: Cambiamos el icono a un 'Check'
        const icono = botonRef.querySelector('i');
        icono.className = 'fas fa-check';
        botonRef.style.backgroundColor = '#10b981'; // Color verde éxito
        botonRef.style.borderColor = '#10b981';

        // 4. Lo regresamos a la normalidad después de 2 segundos
        setTimeout(() => {
            icono.className = 'far fa-copy';
            botonRef.style.backgroundColor = 'rgba(255, 255, 255, 0.15)';
            botonRef.style.borderColor = 'rgba(255, 255, 255, 0.3)';
        }, 2000);

    }).catch(err => {
        console.error('Error al copiar: ', err);
    });
}
