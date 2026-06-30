document.addEventListener("DOMContentLoaded", () => {
    const inputTexto = document.getElementById("filtroMoraTexto");
    const selectClasificacion = document.getElementById("filtroMoraClasificacion");
    const selectGestion = document.getElementById("filtroMoraGestion");
    const btnLimpiar = document.getElementById("limpiarFiltrosMora");
    const filas = document.querySelectorAll(".mora-row");
    const filaSinResultados = document.getElementById("moraNoResults");

    function normalizar(texto) {
        return (texto || "")
            .toString()
            .toLowerCase()
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .trim();
    }

    function filtrarMora() {
        const texto = normalizar(inputTexto.value);
        const clasificacion = selectClasificacion.value;
        const gestion = selectGestion.value;

        let visibles = 0;

        filas.forEach(fila => {
            const credito = normalizar(fila.dataset.credito);
            const dni = normalizar(fila.dataset.dni);
            const cliente = normalizar(fila.dataset.cliente);
            const filaClasificacion = fila.dataset.clasificacion || "";
            const filaGestion = fila.dataset.gestion || "SIN_GESTION";

            const coincideTexto =
                texto === "" ||
                credito.includes(texto) ||
                dni.includes(texto) ||
                cliente.includes(texto);

            const coincideClasificacion =
                clasificacion === "" ||
                filaClasificacion === clasificacion;

            const coincideGestion =
                gestion === "" ||
                filaGestion === gestion;

            const mostrar =
                coincideTexto &&
                coincideClasificacion &&
                coincideGestion;

            fila.style.display = mostrar ? "" : "none";

            if (mostrar) {
                visibles++;
            }
        });

        if (filaSinResultados) {
            filaSinResultados.style.display = visibles === 0 ? "" : "none";
        }
    }

    inputTexto.addEventListener("input", filtrarMora);
    selectClasificacion.addEventListener("change", filtrarMora);
    selectGestion.addEventListener("change", filtrarMora);

    btnLimpiar.addEventListener("click", () => {
        inputTexto.value = "";
        selectClasificacion.value = "";
        selectGestion.value = "";
        filtrarMora();
    });
});