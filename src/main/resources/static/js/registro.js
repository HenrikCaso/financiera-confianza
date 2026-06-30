document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("[data-registro-form]");

    if (!form) {
        return;
    }

    const tipoDoc = form.querySelector("[data-document-type]");
    const numDoc = form.querySelector("[data-num-doc]");
    const celular = form.querySelector("[data-celular]");
    const clave = form.querySelector("[data-clave]");
    const confirmarClave = form.querySelector("[data-confirmar-clave]");

    function soloDigitos(valor) {
        return valor.replace(/\D/g, "");
    }

    function configurarDocumento() {
        if (!tipoDoc || !numDoc) {
            return;
        }

        if (tipoDoc.value === "DNI") {
            numDoc.setAttribute("inputmode", "numeric");
            numDoc.setAttribute("maxlength", "8");
            numDoc.setAttribute("pattern", "[0-9]{8}");
            numDoc.setAttribute("title", "El DNI debe tener 8 digitos.");
            numDoc.placeholder = "Numero de DNI";
            numDoc.value = soloDigitos(numDoc.value).slice(0, 8);
            return;
        }

        if (tipoDoc.value === "CE") {
            numDoc.setAttribute("inputmode", "text");
            numDoc.setAttribute("maxlength", "12");
            numDoc.setAttribute("pattern", "[A-Za-z0-9]{9,12}");
            numDoc.setAttribute("title", "El carne de extranjeria debe tener de 9 a 12 caracteres alfanumericos.");
            numDoc.placeholder = "Numero de documento CE";
            numDoc.value = numDoc.value.replace(/\s/g, "").toUpperCase().slice(0, 12);
            return;
        }

        numDoc.removeAttribute("pattern");
        numDoc.setAttribute("maxlength", "12");
        numDoc.placeholder = "Numero de documento";
    }

    function validarCelular() {
        if (!celular) {
            return;
        }

        celular.value = soloDigitos(celular.value).slice(0, 9);
    }

    function validarClaves() {
        if (!clave || !confirmarClave) {
            return;
        }

        if (confirmarClave.value && clave.value !== confirmarClave.value) {
            confirmarClave.setCustomValidity("Las claves ingresadas no coinciden.");
            return;
        }

        confirmarClave.setCustomValidity("");
    }

    if (tipoDoc) {
        tipoDoc.addEventListener("change", configurarDocumento);
    }

    if (numDoc) {
        numDoc.addEventListener("input", configurarDocumento);
    }

    if (celular) {
        celular.addEventListener("input", validarCelular);
    }

    if (clave) {
        clave.addEventListener("input", validarClaves);
    }

    if (confirmarClave) {
        confirmarClave.addEventListener("input", validarClaves);
    }

    form.addEventListener("submit", function () {
        configurarDocumento();
        validarCelular();
        validarClaves();
    });

    configurarDocumento();
    validarCelular();
    validarClaves();
});
