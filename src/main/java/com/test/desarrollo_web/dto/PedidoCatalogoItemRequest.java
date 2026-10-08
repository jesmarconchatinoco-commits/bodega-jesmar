package com.test.desarrollo_web.dto;

public class PedidoCatalogoItemRequest {
    private Integer productoId;
    private Integer presentacionId;
    private Integer cantidad;

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public Integer getPresentacionId() {
        return presentacionId;
    }

    public void setPresentacionId(Integer presentacionId) {
        this.presentacionId = presentacionId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
