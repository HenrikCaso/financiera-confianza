// Esperamos a que todo el HTML esté cargado en la pantalla
document.addEventListener('DOMContentLoaded', function() {

    const togglePassword = document.querySelector('#togglePassword');
    const password = document.querySelector('#claveInput');

    // Validamos que existan en la página para evitar errores
    if (togglePassword && password) {
        togglePassword.addEventListener('click', function (e) {
            // Cambia el tipo de input (de password a texto y viceversa)
            const type = password.getAttribute('type') === 'password' ? 'text' : 'password';
            password.setAttribute('type', type);
            // Cambia el icono (ojito abierto / ojito tachado)
            this.classList.toggle('fa-eye-slash');
        });
    }

});