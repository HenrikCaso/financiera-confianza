document.addEventListener("DOMContentLoaded", function () {
    const dataNode = document.getElementById("adminDashboardData");

    if (!dataNode || typeof Chart === "undefined") {
        return;
    }

    const value = function (key) {
        const number = Number.parseFloat(dataNode.dataset[key]);
        return Number.isFinite(number) ? number : 0;
    };

    const baseOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: "bottom"
            }
        }
    };

    const solicitudesCanvas =
            document.getElementById("solicitudesEtapaChart");

    if (solicitudesCanvas) {
        new Chart(solicitudesCanvas, {
            type: "doughnut",
            data: {
                labels: [
                    "Solicitado",
                    "En riesgos",
                    "Aprobada",
                    "Desembolsado",
                    "Finalizado",
                    "Rechazada"
                ],
                datasets: [{
                    data: [
                        value("etapaSolicitado"),
                        value("etapaRiesgos"),
                        value("etapaAprobada"),
                        value("etapaDesembolsado"),
                        value("etapaFinalizado"),
                        value("etapaRechazada")
                    ],
                    backgroundColor: [
                        "#2563eb",
                        "#f59e0b",
                        "#16a34a",
                        "#0f766e",
                        "#64748b",
                        "#dc2626"
                    ],
                    borderWidth: 0
                }]
            },
            options: baseOptions
        });
    }

    const financieroCanvas =
            document.getElementById("financieroChart");

    if (financieroCanvas) {
        new Chart(financieroCanvas, {
            type: "bar",
            data: {
                labels: [
                    "Desembolsado",
                    "Pagado",
                    "Pendiente"
                ],
                datasets: [{
                    label: "S/",
                    data: [
                        value("totalDesembolsado"),
                        value("totalPagado"),
                        value("pendienteCobrar")
                    ],
                    backgroundColor: [
                        "#2563eb",
                        "#16a34a",
                        "#f59e0b"
                    ],
                    borderRadius: 6
                }]
            },
            options: {
                ...baseOptions,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function (tickValue) {
                                return "S/ " + tickValue;
                            }
                        }
                    }
                }
            }
        });
    }

    const moraCanvas = document.getElementById("moraChart");

    if (moraCanvas) {
        new Chart(moraCanvas, {
            type: "bar",
            data: {
                labels: [
                    "Al día",
                    "Mora temprana",
                    "Mora intermedia",
                    "Mora crítica"
                ],
                datasets: [{
                    label: "Cuotas",
                    data: [
                        value("carteraAlDia"),
                        value("moraTemprana"),
                        value("moraIntermedia"),
                        value("moraCritica")
                    ],
                    backgroundColor: [
                        "#16a34a",
                        "#f59e0b",
                        "#ea580c",
                        "#dc2626"
                    ],
                    borderRadius: 6
                }]
            },
            options: {
                ...baseOptions,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            precision: 0
                        }
                    }
                }
            }
        });
    }
});
