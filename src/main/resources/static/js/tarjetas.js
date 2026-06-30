document.addEventListener("DOMContentLoaded", function () {
    window.addEventListener("pageshow", function (event) {
        if (event.persisted) {
            window.location.reload();
        }
    });

    const toggleSeguridad = document.getElementById("toggleSeguridad");
    const formToggle = document.getElementById("formToggle");

    if (toggleSeguridad && formToggle) {
        toggleSeguridad.addEventListener("change", function () {
            formToggle.submit();
        });
    }

    const formBloqueoDefinitivo = document.querySelector("[data-confirm-definitive]");

    if (formBloqueoDefinitivo) {
        formBloqueoDefinitivo.addEventListener("submit", function (event) {
            const confirmado = window.confirm(
                "\u00bfEstas seguro de reportar tu tarjeta como robada o extraviada? Esta accion no se puede deshacer desde la aplicacion."
            );

            if (!confirmado) {
                event.preventDefault();
            }
        });
    }

    const toast = document.querySelector("[data-card-toast]");
    let toastTimer;

    function mostrarProductoNoDisponible() {
        if (!toast) {
            window.alert("Producto proximamente disponible.");
            return;
        }

        window.clearTimeout(toastTimer);
        toast.classList.add("visible");

        toastTimer = window.setTimeout(function () {
            toast.classList.remove("visible");
        }, 2400);
    }

    document.querySelectorAll("[data-card-product-button]").forEach(function (button) {
        button.addEventListener("click", mostrarProductoNoDisponible);
    });

    const alertas = document.querySelectorAll(".alert");

    window.setTimeout(function () {
        alertas.forEach(function (alerta) {
            alerta.style.display = "none";
        });
    }, 4000);
});
