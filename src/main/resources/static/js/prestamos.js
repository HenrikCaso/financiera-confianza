document.addEventListener('DOMContentLoaded', function() {

    // Prevención del bfcache
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) window.location.reload();
    });

    const montoRange = document.getElementById('montoRange');
    const montoDisplay = document.getElementById('montoDisplay');
    const cuotasSelect = document.getElementById('cuotasSelect');
    const diaPagoSelect = document.getElementById('diaPagoSelect');

    const teaDisplay = document.getElementById('teaDisplay');
    const fechaPagoDisplay = document.getElementById('fechaPagoDisplay');
    const cuotaDisplay = document.getElementById('cuotaMensual');

    function calcularCuota() {
        const P = parseFloat(montoRange.value);
        const n = parseInt(cuotasSelect.value);

        // 1. Lógica dinámica de la TEA según el riesgo (tiempo)
        let tea = 0.155; // 15.5% por defecto (12 meses)
        if (n === 6) tea = 0.125;  // 12.5% (Menos riesgo, TEA más baja)
        if (n === 24) tea = 0.185; // 18.5% (Más riesgo, TEA más alta)

        // Convertir TEA a Tasa Efectiva Mensual (TEM)
        const i = Math.pow(1 + tea, 1/12) - 1;

        // 2. Fórmula de Amortización Francesa
        const cuota = (P * i * Math.pow(1 + i, n)) / (Math.pow(1 + i, n) - 1);

        // 3. Lógica para la fecha del primer pago (Al mes siguiente)
        const diaElegido = parseInt(diaPagoSelect.value);
        let fechaActual = new Date();
        let mesPago = fechaActual.getMonth() + 1; // Mes siguiente (0 indexado)
        let anioPago = fechaActual.getFullYear();

        if (mesPago > 11) { // Si estamos en diciembre, el próximo mes es enero del otro año
            mesPago = 0;
            anioPago++;
        }

        // Formatear números para que tengan un cero delante (ej: 05 en vez de 5)
        const diaStr = diaElegido.toString().padStart(2, '0');
        const mesStr = (mesPago + 1).toString().padStart(2, '0');

        // 4. Actualizar la pantalla
        montoDisplay.innerText = P.toLocaleString();
        teaDisplay.innerText = (tea * 100).toFixed(2) + "%";
        fechaPagoDisplay.innerText = `${diaStr}/${mesStr}/${anioPago}`;
        cuotaDisplay.innerText = "S/ " + cuota.toFixed(2);
    }

    // Escuchar cambios en cualquier control para recalcular al instante
    montoRange.addEventListener('input', calcularCuota);
    cuotasSelect.addEventListener('change', calcularCuota);
    diaPagoSelect.addEventListener('change', calcularCuota);

    // Calcular por primera vez al cargar la página
    calcularCuota();
});