document.addEventListener("DOMContentLoaded", () => {
    const toggleButtons =
        document.querySelectorAll(".toggle-password");

    toggleButtons.forEach((button) => {
        button.addEventListener("click", () => {
            const targetId = button.dataset.target;
            const input = document.getElementById(targetId);
            const icon = button.querySelector("i");

            if (!input || !icon) {
                return;
            }

            const isPassword = input.type === "password";

            input.type = isPassword ? "text" : "password";

            icon.classList.toggle("fa-eye", !isPassword);
            icon.classList.toggle("fa-eye-slash", isPassword);

            button.setAttribute(
                "aria-label",
                isPassword ? "Ocultar clave" : "Mostrar clave"
            );
        });
    });

    const numericInputs = document.querySelectorAll(
        "#nuevaClave, #confirmarClave"
    );

    numericInputs.forEach((input) => {
        input.addEventListener("input", () => {
            input.value = input.value
                .replace(/\D/g, "")
                .slice(0, 6);
        });
    });
});