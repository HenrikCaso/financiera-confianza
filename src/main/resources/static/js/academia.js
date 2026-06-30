document.addEventListener("DOMContentLoaded", function () {
    const toast = document.querySelector("[data-course-toast]");
    let toastTimer;

    function mostrarMensaje() {
        if (!toast) {
            alert("Curso próximamente disponible.");
            return;
        }

        window.clearTimeout(toastTimer);
        toast.classList.add("visible");

        toastTimer = window.setTimeout(function () {
            toast.classList.remove("visible");
        }, 2400);
    }

    document.querySelectorAll("[data-course-button]").forEach(function (button) {
        button.addEventListener("click", mostrarMensaje);
    });
});
