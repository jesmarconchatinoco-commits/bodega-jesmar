package com.test.desarrollo_web.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracion_pago")
public class ConfiguracionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 10)
    private Canal canal;

    @Column(nullable = false, length = 15)
    private String celular;

    @Column(name = "nombre_titular", nullable = false, length = 120)
    private String nombreTitular;

    @Column(name = "texto_qr", length = 500)
    private String textoQr;

    @Column(name = "imagen_qr", length = 255)
    private String imagenQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Estado estado = Estado.ACTIVO;

    public enum Canal {
        YAPE, PLIN
    }

    public enum Estado {
        ACTIVO, INACTIVO
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Canal getCanal() {
        return canal;
    }

    public void setCanal(Canal canal) {
        this.canal = canal;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getNombreTitular() {
        return nombreTitular;
    }

    public void setNombreTitular(String nombreTitular) {
        this.nombreTitular = nombreTitular;
    }

    public String getTextoQr() {
        return textoQr;
    }

    public void setTextoQr(String textoQr) {
        this.textoQr = textoQr;
    }

    public String getImagenQr() {
        return imagenQr;
    }

    public void setImagenQr(String imagenQr) {
        this.imagenQr = imagenQr;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
