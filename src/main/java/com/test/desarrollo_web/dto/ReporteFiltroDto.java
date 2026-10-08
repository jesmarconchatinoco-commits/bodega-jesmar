package com.test.desarrollo_web.dto;

import java.time.LocalDate;

public class ReporteFiltroDto {

    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private String periodo;
    private Integer clienteId;
    private Integer vendedorId;
    private Integer categoriaId;
    private Integer productoId;
    private String marca;
    private Integer tipoPagoId;
    private Integer tipoComprobanteId;
    private String estadoVenta;
    private String canal;

    public LocalDate getFechaDesde() {
        return fechaDesde;
    }

    public void setFechaDesde(LocalDate fechaDesde) {
        this.fechaDesde = fechaDesde;
    }

    public LocalDate getFechaHasta() {
        return fechaHasta;
    }

    public void setFechaHasta(LocalDate fechaHasta) {
        this.fechaHasta = fechaHasta;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public Integer getVendedorId() {
        return vendedorId;
    }

    public void setVendedorId(Integer vendedorId) {
        this.vendedorId = vendedorId;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public Integer getTipoPagoId() {
        return tipoPagoId;
    }

    public void setTipoPagoId(Integer tipoPagoId) {
        this.tipoPagoId = tipoPagoId;
    }

    public Integer getTipoComprobanteId() {
        return tipoComprobanteId;
    }

    public void setTipoComprobanteId(Integer tipoComprobanteId) {
        this.tipoComprobanteId = tipoComprobanteId;
    }

    public String getEstadoVenta() {
        return estadoVenta;
    }

    public void setEstadoVenta(String estadoVenta) {
        this.estadoVenta = estadoVenta;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }
}
