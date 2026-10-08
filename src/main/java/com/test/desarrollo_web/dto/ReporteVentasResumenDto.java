package com.test.desarrollo_web.dto;

import java.math.BigDecimal;

public class ReporteVentasResumenDto {

    private long totalVentas;
    private long cantidadComprobantes;
    private long totalClientes;
    private BigDecimal totalVendido;
    private BigDecimal totalDescuentos;
    private BigDecimal totalImpuestos;
    private BigDecimal promedioVenta;

    public long getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(long totalVentas) {
        this.totalVentas = totalVentas;
    }

    public long getCantidadComprobantes() {
        return cantidadComprobantes;
    }

    public void setCantidadComprobantes(long cantidadComprobantes) {
        this.cantidadComprobantes = cantidadComprobantes;
    }

    public long getTotalClientes() {
        return totalClientes;
    }

    public void setTotalClientes(long totalClientes) {
        this.totalClientes = totalClientes;
    }

    public BigDecimal getTotalVendido() {
        return totalVendido;
    }

    public void setTotalVendido(BigDecimal totalVendido) {
        this.totalVendido = totalVendido;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public void setTotalDescuentos(BigDecimal totalDescuentos) {
        this.totalDescuentos = totalDescuentos;
    }

    public BigDecimal getTotalImpuestos() {
        return totalImpuestos;
    }

    public void setTotalImpuestos(BigDecimal totalImpuestos) {
        this.totalImpuestos = totalImpuestos;
    }

    public BigDecimal getPromedioVenta() {
        return promedioVenta;
    }

    public void setPromedioVenta(BigDecimal promedioVenta) {
        this.promedioVenta = promedioVenta;
    }
}
