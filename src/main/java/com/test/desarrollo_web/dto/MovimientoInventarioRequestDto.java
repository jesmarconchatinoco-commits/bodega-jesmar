package com.test.desarrollo_web.dto;

public class MovimientoInventarioRequestDto {

    private Integer presentacionId;
    private Integer cantidad;
    private Integer stockNuevo;
    private String observacion;
    private String motivo;
    private String stockFisico;

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

    public Integer getStockNuevo() {
        return stockNuevo;
    }

    public void setStockNuevo(Integer stockNuevo) {
        this.stockNuevo = stockNuevo;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getStockFisico() {
        return stockFisico;
    }

    public void setStockFisico(String stockFisico) {
        this.stockFisico = stockFisico;
    }
}
