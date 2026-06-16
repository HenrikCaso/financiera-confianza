document.addEventListener('DOMContentLoaded', function() {

    // 1. Lógica del "Ojito" para ver u ocultar la contraseña
    const togglePassword = document.getElementById('togglePassword');
    const claveInput = document.getElementById('claveInput');

    if (togglePassword && claveInput) {
        togglePassword.addEventListener('click', function () {
            // Cambiar el tipo de input
            const type = claveInput.getAttribute('type') === 'password' ? 'text' : 'password';
            claveInput.setAttribute('type', type);

            // Cambiar el ícono (ojo abierto / ojo cerrado)
            this.classList.toggle('fa-eye');
            this.classList.toggle('fa-eye-slash');
        });
    }

    // 2. Lógica para desaparecer la alerta de éxito automáticamente
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
});