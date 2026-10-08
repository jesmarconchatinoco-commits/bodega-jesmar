package com.test.desarrollo_web.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "TIPO_COMPROBANTE")
public class TipoComprobante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String serie;

    @Column(name = "correlativo_actual", nullable = false)
    private Integer correlativoActual;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.ACTIVO;

    public enum Estado {
        ACTIVO, INACTIVO
    }

    public TipoComprobante() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public Integer getCorrelativoActual() {
        return correlativoActual;
    }

    public void setCorrelativoActual(Integer correlativoActual) {
        this.correlativoActual = correlativoActual;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
