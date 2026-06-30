/*
 * Confirmación del pago de cuotas.
 * Debe permanecer fuera de DOMContentLoaded porque
 * se llama directamente desde el formulario HTML.
 */
window.confirmarPago = function (formulario) {

    const totalTexto =
        formulario.dataset.total || "0";

    const total =
        Number(totalTexto.replace(",", "."));

    const numeroCuota =
        formulario.dataset.cuota || "";

    const credito =
        formulario.dataset.credito || "";

    const referenciaCredito =
        credito
            ? " del crédito #" + credito
            : "";

    if (!Number.isFinite(total) || total <= 0) {

        window.alert(
            "No se pudo determinar el monto de la cuota."
        );

        return false;
    }

    return window.confirm(
        "Se descontarán S/ "
        + total.toFixed(2)
        + " para pagar la cuota N.° "
        + numeroCuota
        + referenciaCredito
        + ".\n\n¿Deseas continuar?"
    );
};


document.addEventListener(
    "DOMContentLoaded",
    function () {

        console.log(
            "prestamos.js v5 cargado correctamente"
        );

        // =====================================================
        // ABRIR Y CERRAR CRONOGRAMAS POR CRÉDITO
        // =====================================================

        const botonesCredito =
            document.querySelectorAll(
                ".credito-toggle"
            );

        botonesCredito.forEach(
            function (boton) {

                boton.addEventListener(
                    "click",
                    function () {

                        const detalleId =
                            boton.getAttribute(
                                "aria-controls"
                            );

                        const detalle =
                            document.getElementById(
                                detalleId
                            );

                        if (!detalle) {
                            return;
                        }

                        const quedoCerrado =
                            detalle.classList.toggle(
                                "credito-colapsado"
                            );

                        boton.setAttribute(
                            "aria-expanded",
                            String(!quedoCerrado)
                        );
                    }
                );
            }
        );

        window.addEventListener(
            "pageshow",
            function (event) {

                if (event.persisted) {
                    window.location.reload();
                }
            }
        );

        const montoRange =
            document.getElementById("montoRange");

        const montoDisplay =
            document.getElementById("montoDisplay");

        const cuotasSelect =
            document.getElementById("cuotasSelect");

        const diaPagoSelect =
            document.getElementById("diaPagoSelect");

        const ingresosInput =
            document.getElementById("ingresosMensuales");

        const gastosInput =
            document.getElementById("gastosMensuales");

        const teaDisplay =
            document.getElementById("teaDisplay");

        const temDisplay =
            document.getElementById("temDisplay");

        const fechaPagoDisplay =
            document.getElementById("fechaPagoDisplay");

        const cuotaDisplay =
            document.getElementById("cuotaMensual");

        const rdsDisplay =
            document.getElementById("rdsDisplay");

        const capacidadDisplay =
            document.getElementById("capacidadDisplay");

        const seguros =
            document.querySelectorAll(
                'input[name="tieneSeguro"]'
            );

        const TEA_CON_SEGURO = 0.4092;
        const TEA_SIN_SEGURO = 0.4392;

        function obtenerTieneSeguro() {

            const seleccionado =
                document.querySelector(
                    'input[name="tieneSeguro"]:checked'
                );

            return seleccionado
                ? seleccionado.value === "true"
                : true;
        }

        function calcularPrimeraFechaPago(
            diaElegido
        ) {

            const hoy = new Date();

            const anio =
                hoy.getFullYear();

            const mesSiguiente =
                hoy.getMonth() + 1;

            const ultimoDia =
                new Date(
                    anio,
                    mesSiguiente + 1,
                    0
                ).getDate();

            const diaReal =
                Math.min(
                    diaElegido,
                    ultimoDia
                );

            return new Date(
                anio,
                mesSiguiente,
                diaReal
            );
        }

        function actualizarRiesgo(
            rds,
            ingresos
        ) {

            rdsDisplay.classList.remove(
                "riesgo-sin-calcular",
                "riesgo-verde",
                "riesgo-amarillo",
                "riesgo-rojo"
            );

            if (ingresos <= 0
                || !Number.isFinite(rds)) {

                rdsDisplay.textContent =
                    "Sin calcular";

                rdsDisplay.classList.add(
                    "riesgo-sin-calcular"
                );

                return;
            }

            rdsDisplay.textContent =
                rds.toFixed(2) + "%";

            if (rds <= 30) {

                rdsDisplay.classList.add(
                    "riesgo-verde"
                );

            } else if (rds <= 40) {

                rdsDisplay.classList.add(
                    "riesgo-amarillo"
                );

            } else {

                rdsDisplay.classList.add(
                    "riesgo-rojo"
                );
            }
        }

        function calcularCredito() {

            if (
                !montoRange
                || !cuotasSelect
                || !diaPagoSelect
                || !teaDisplay
                || !temDisplay
                || !fechaPagoDisplay
                || !cuotaDisplay
                || !rdsDisplay
                || !capacidadDisplay
            ) {
                return;
            }

            const monto =
                Number(montoRange.value);

            const numeroCuotas =
                Number(cuotasSelect.value);

            const diaElegido =
                Number(diaPagoSelect.value);

            const ingresos = parseFloat(document.getElementById("ingresosMensuales").value);
            const gastos = parseFloat(document.getElementById("gastosMensuales").value);

            const tieneSeguro =
                obtenerTieneSeguro();

            const tea =
                tieneSeguro
                    ? TEA_CON_SEGURO
                    : TEA_SIN_SEGURO;

            const tem =
                Math.pow(
                    1 + tea,
                    1 / 12
                ) - 1;

            const cuota =
                (monto * tem)
                /
                (
                    1
                    - Math.pow(
                        1 + tem,
                        -numeroCuotas
                    )
                );

            const primeraFecha =
                calcularPrimeraFechaPago(
                    diaElegido
                );

            const datosFinancierosInvalidos =
                !Number.isFinite(ingresos)
                || ingresos <= 0
                || !Number.isFinite(gastos)
                || gastos < 0;

            let rds = NaN;
            let capacidadDisponible = NaN;

            if (!datosFinancierosInvalidos) {

                rds =
                    (
                        (gastos + cuota)
                        / ingresos
                    ) * 100;

                capacidadDisponible =
                    ingresos
                    - gastos
                    - cuota;
            }

            montoDisplay.textContent =
                monto.toLocaleString(
                    "es-PE",
                    {
                        minimumFractionDigits: 0,
                        maximumFractionDigits: 0
                    }
                );

            teaDisplay.textContent =
                (tea * 100).toFixed(2)
                + "%";

            temDisplay.textContent =
                (tem * 100).toFixed(4)
                + "%";

            fechaPagoDisplay.textContent =
                primeraFecha.toLocaleDateString(
                    "es-PE"
                );

            cuotaDisplay.textContent =
                "S/ "
                + cuota.toFixed(2);

            actualizarRiesgo(
                rds,
                ingresos
            );

            capacidadDisplay.classList.remove(
                "capacidad-positiva",
                "capacidad-negativa"
            );

            if (datosFinancierosInvalidos) {

                capacidadDisplay.textContent =
                    "Sin calcular";

            } else {

                capacidadDisplay.textContent =
                    "S/ "
                    + capacidadDisponible.toFixed(2);

                capacidadDisplay.classList.add(
                    capacidadDisponible >= 0
                        ? "capacidad-positiva"
                        : "capacidad-negativa"
                );
            }
        }

        if (
            montoRange
            && cuotasSelect
            && diaPagoSelect
        ) {

            montoRange.addEventListener(
                "input",
                calcularCredito
            );

            cuotasSelect.addEventListener(
                "change",
                calcularCredito
            );

            diaPagoSelect.addEventListener(
                "change",
                calcularCredito
            );

            ingresosInput?.addEventListener(
                "input",
                calcularCredito
            );

            gastosInput?.addEventListener(
                "input",
                calcularCredito
            );

            seguros.forEach(
                function (seguro) {

                    seguro.addEventListener(
                        "change",
                        calcularCredito
                    );
                }
            );

            calcularCredito();
        }

        window.setTimeout(
            function () {

                const mensajeExito =
                    document.getElementById(
                        "mensajeExito"
                    );

                const mensajeError =
                    document.getElementById(
                        "mensajeError"
                    );

                if (mensajeExito) {
                    mensajeExito.style.display =
                        "none";
                }

                if (mensajeError) {
                    mensajeError.style.display =
                        "none";
                }
            },
            5000
        );
    }
);