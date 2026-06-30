document.addEventListener("DOMContentLoaded", function () {

    const resultado = document.getElementById("resultado");
    const commitmentGroup = document.getElementById("commitmentGroup");
    const commitmentInput = document.getElementById("fechaCompromisoPago");

    if (!resultado || !commitmentGroup || !commitmentInput) {
        return;
    }

    function actualizarFechaCompromiso() {

        const esCompromiso = resultado.value === "COMPROMISO_PAGO";

        commitmentGroup.style.display = esCompromiso ? "flex" : "none";

        commitmentInput.required = esCompromiso;

        if (!esCompromiso) {
            commitmentInput.value = "";
        }
    }

    resultado.addEventListener("change", actualizarFechaCompromiso);

    actualizarFechaCompromiso();
});