package com.example.financieraconfianza.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoDoc;

    @Column(unique = true, nullable = false)
    private String numDoc;

    private String correo;
    private String celular;
    private String clave;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTipoDoc() { return tipoDoc; }
    public void setTipoDoc(String tipoDoc) { this.tipoDoc = tipoDoc; }

    public String getNumDoc() { return numDoc; }
    public void setNumDoc(String numDoc) { this.numDoc = numDoc; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCelular() { return celular; }
    public void setCelular(String celular) { this.celular = celular; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    private Double saldo;

    // Getters y Setters para el saldo
    public Double getSaldo() { return saldo; }
    public void setSaldo(Double saldo) { this.saldo = saldo; }

    // Nuevo campo para la seguridad de la tarjeta
    private Boolean tarjetaBloqueada = false; // Por defecto nace desbloqueada

    // Getters y Setters
    public Boolean getTarjetaBloqueada() { return tarjetaBloqueada; }
    public void setTarjetaBloqueada(Boolean tarjetaBloqueada) { this.tarjetaBloqueada = tarjetaBloqueada; }

    // Nuevo campo para el número de cuenta del banco
    private String numeroCuenta;

    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }

    private String cci;
    public String getCci() { return cci; }
    public void setCci(String cci) { this.cci = cci; }
    
    // Campo para diferenciar clientes de trabajadores del banco
    private String rol = "CLIENTE"; // Por defecto todos nacen como clientes

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Column(name = "bloqueo_definitivo")
    private Boolean bloqueoDefinitivo = false; // Por defecto es false

    public Boolean getBloqueoDefinitivo() {
        return bloqueoDefinitivo;
    }

    public void setBloqueoDefinitivo(Boolean bloqueoDefinitivo) {
        this.bloqueoDefinitivo = bloqueoDefinitivo;
    }
}