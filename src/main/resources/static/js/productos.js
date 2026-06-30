document.addEventListener("DOMContentLoaded", function () {
    const toast = document.querySelector("[data-product-toast]");
    let toastTimer;

    function mostrarMensaje() {
        if (!toast) {
            alert("Solicitud recibida. Un asesor se comunicar\u00e1 contigo pronto.");
            return;
        }

        window.clearTimeout(toastTimer);
        toast.classList.add("visible");

        toastTimer = window.setTimeout(function () {
            toast.classList.remove("visible");
        }, 2600);
    }

    document.querySelectorAll("[data-product-form]").forEach(function (form) {
        const celular = form.querySelector('input[name="celular"]');

        if (celular) {
            celular.addEventListener("input", function () {
                celular.value = celular.value.replace(/\D/g, "").slice(0, 9);
            });
        }

        form.addEventListener("submit", function (event) {
            event.preventDefault();

            if (!form.checkValidity()) {
                form.reportValidity();
                return;
            }

            mostrarMensaje();
            form.reset();
        });
    });
});
