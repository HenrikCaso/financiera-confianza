document.addEventListener("DOMContentLoaded", function () {
    const mensajes = document.querySelectorAll(".flash-message");

    mensajes.forEach(function (mensaje) {
        setTimeout(function () {
            mensaje.classList.add("flash-hide");

            setTimeout(function () {
                mensaje.remove();
            }, 400);
        }, 4000);
    });
});