package com.example.financieraconfianza.dto;

public class LoginRequestDTO {

    private String numDoc;
    private String clave;

    public String getNumDoc() {
        return numDoc;
    }

    public void setNumDoc(String numDoc) {
        this.numDoc = numDoc;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }
}