package com.example.financieraconfianza.dto;

public class LoginResponseDTO {

    private String token;
    private String rol;

    public LoginResponseDTO() {}

    public LoginResponseDTO(String token, String rol) {
        this.token = token;
        this.rol = rol;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}