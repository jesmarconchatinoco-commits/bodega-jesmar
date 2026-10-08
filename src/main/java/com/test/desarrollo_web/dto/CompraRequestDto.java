package com.test.desarrollo_web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CompraRequestDto {

    private Integer proveedorId;
    private String tipoComprobante;
    private String serie;
    private String numero;
    private LocalDate fechaEmision;
    private BigDecimal igv;
    private String observaciones;
    private List<DetalleCompraLineaDto> detalles = new ArrayList<>();

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(String tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDate fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<DetalleCompraLineaDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleCompraLineaDto> detalles) {
        this.detalles = detalles;
    }

    public static class DetalleCompraLineaDto {
        private Integer presentacionId;
        private Integer cantidad;
        private BigDecimal precioCompra;

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

        public BigDecimal getPrecioCompra() {
            return precioCompra;
        }

        public void setPrecioCompra(BigDecimal precioCompra) {
            this.precioCompra = precioCompra;
        }
    }
}
