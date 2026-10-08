package com.test.desarrollo_web.dto;

import java.math.BigDecimal;

public class TopProductoDto {
    private String nombre;
    private Long cantidad;
    private BigDecimal total;

    public TopProductoDto() {
    }

    public TopProductoDto(String nombre, Long cantidad, BigDecimal total) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.total = total;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Long getCantidad() {
        return cantidad;
    }

    public void setCantidad(Long cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
