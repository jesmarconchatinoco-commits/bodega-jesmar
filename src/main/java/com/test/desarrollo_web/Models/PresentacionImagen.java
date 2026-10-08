package com.test.desarrollo_web.Models;

import jakarta.persistence.*;

@Entity
@Table(name = "presentacion_imagen")
public class PresentacionImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "presentacion_id", nullable = false)
    private ProductoPresentacion presentacion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "imagen_id", nullable = false)
    private Imagen imagen;

    @Column(nullable = false)
    private Integer orden = 0;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductoPresentacion getPresentacion() {
        return presentacion;
    }

    public void setPresentacion(ProductoPresentacion presentacion) {
        this.presentacion = presentacion;
    }

    public Imagen getImagen() {
        return imagen;
    }

    public void setImagen(Imagen imagen) {
        this.imagen = imagen;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
