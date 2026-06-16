document.addEventListener('DOMContentLoaded', function() {

    // Elementos del DOM para la navegación
    const paso1 = document.getElementById('paso1');
    const paso2 = document.getElementById('paso2');
    const btnVolver = document.getElementById('btnVolver');
    const typeCards = document.querySelectorAll('.type-card');

    // Elementos del formulario dinámico
    const tituloFormulario = document.getElementById('tituloFormulario');
    const tipoTransferenciaInput = document.getElementById('tipoTransferenciaInput');
    const labelCuentaDestino = document.getElementById('labelCuentaDestino');
    const cuentaInput = document.getElementById('cuentaDestino');
    const destinatarioInfo = document.getElementById('destinatarioInfo');
    const btnConfirmar = document.getElementById('btnConfirmar');

    // 1. Lógica al hacer clic en una tarjeta (Pasar al Paso 2)
    typeCards.forEach(card => {
        card.addEventListener('click', function() {
            const tipoElegido = this.getAttribute('data-type');

            // Guardamos el valor para enviarlo a Java
            tipoTransferenciaInput.value = tipoElegido;

            // Cambiamos el título del formulario
            tituloFormulario.innerHTML = `<i class="fas fa-exchange-alt"></i> Transferencia: ${tipoElegido}`;

            // Adaptamos el campo de cuenta destino según la opción
            if (tipoElegido === 'A otros bancos') {
                labelCuentaDestino.innerText = 'Cuenta Destino (CCI de 20 dígitos)';
                cuentaInput.maxLength = 20;
                cuentaInput.placeholder = 'Ej: 00219300000000000012';
            } else if (tipoElegido === 'Al exterior') {
                labelCuentaDestino.innerText = 'Código SWIFT / IBAN';
                cuentaInput.maxLength = 30;
                cuentaInput.placeholder = 'Ej: BCPXPEPLXXX';
            } else {
                labelCuentaDestino.innerText = 'Cuenta Destino (14 dígitos requeridos)';
                cuentaInput.maxLength = 14;
                cuentaInput.placeholder = 'Ej: 19300000000000';
            }

            // Limpiamos el input por si había algo escrito
            cuentaInput.value = '';
            destinatarioInfo.style.display = 'none';

            // Ocultar paso 1 y mostrar paso 2
            paso1.style.display = 'none';
            paso2.style.display = 'block';
        });
    });

    // 2. Lógica para el botón "Volver"
    btnVolver.addEventListener('click', function() {
        paso2.style.display = 'none';
        paso1.style.display = 'block';
    });


    cuentaInput.addEventListener('input', function(e) {

        if (tipoTransferenciaInput.value !== 'Al exterior') {
            this.value = this.value.replace(/[^0-9]/g, '');
        }

        const longitudRequerida = (tipoTransferenciaInput.value === 'A otros bancos') ? 20 : 14;

        if ((tipoTransferenciaInput.value === 'A terceros' || tipoTransferenciaInput.value === 'Entre mis cuentas') && this.value.length === longitudRequerida) {

            // Estado de "Cargando"
            destinatarioInfo.style.display = 'block';
            destinatarioInfo.style.backgroundColor = '#fefce8';
            destinatarioInfo.style.borderColor = '#fef08a';
            destinatarioInfo.style.color = '#ca8a04';
            document.getElementById('nombreDestinatario').innerHTML = '<i class="fas fa-spinner fa-spin"></i> Buscando en base de datos...';

            btnConfirmar.disabled = true;
            btnConfirmar.style.opacity = '0.5';

            // CONSULTA REAL A TU BACKEND JAVA (API)
            fetch('/api/validar-cuenta?cuenta=' + this.value)
                .then(response => {
                    if (!response.ok) throw new Error('Cuenta no existe');
                    return response.json();
                })
                .then(data => {
                    // Cuenta encontrada con éxito en Supabase
                    destinatarioInfo.style.backgroundColor = '#f0fdf4';
                    destinatarioInfo.style.borderColor = '#bbf7d0';
                    destinatarioInfo.style.color = '#15803d';
                    document.getElementById('nombreDestinatario').innerHTML = data.nombre;

                    btnConfirmar.disabled = false;
                    btnConfirmar.style.opacity = '1';
                })
                .catch(error => {
                    // La cuenta no existe en tu base de datos
                    destinatarioInfo.style.backgroundColor = '#fef2f2';
                    destinatarioInfo.style.borderColor = '#fecaca';
                    destinatarioInfo.style.color = '#b91c1c';
                    document.getElementById('nombreDestinatario').innerHTML = '<i class="fas fa-times-circle"></i> Cuenta inválida o no registrada';

                    btnConfirmar.disabled = true;
                    btnConfirmar.style.opacity = '0.5';
                });

        } else {
            destinatarioInfo.style.display = 'none';
            if(tipoTransferenciaInput.value === 'A otros bancos' || tipoTransferenciaInput.value === 'Al exterior'){
                // Para bancos externos o exterior no podemos validar nombres, lo dejamos pasar
                btnConfirmar.disabled = false;
                btnConfirmar.style.opacity = '1';
            } else {
                btnConfirmar.disabled = true;
                btnConfirmar.style.opacity = '0.5';
            }
        }
    });
    // ==========================================
    // AUTO-OCULTAR ALERTAS (UX MEJORADO)
    // ==========================================
    // ¡Ya no hay un segundo DOMContentLoaded aquí!
    console.log("➡️ Paso 1: El script de notificaciones inició correctamente.");

    // Buscamos las alertas
    const alertas = document.querySelectorAll('.alert');
    console.log("➡️ Paso 2: Alertas encontradas en la pantalla: ", alertas.length);

    if (alertas.length > 0) {
        console.log("➡️ Paso 3: Iniciando el cronómetro de 4 segundos...");

        setTimeout(() => {
            console.log("➡️ Paso 4: ¡Tiempo cumplido! Ocultando alerta...");
            alertas.forEach(alerta => {
                alerta.style.transition = "opacity 0.6s ease";
                alerta.style.opacity = "0";

                setTimeout(() => {
                    alerta.style.display = "none";
                    alerta.remove();
                    console.log("➡️ Paso 5: Alerta destruida.");
                }, 600);
            });
        }, 4000);
    }
});