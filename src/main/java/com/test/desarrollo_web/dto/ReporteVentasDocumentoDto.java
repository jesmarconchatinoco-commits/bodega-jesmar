package com.test.desarrollo_web.dto;

import java.util.ArrayList;
import java.util.List;

public class ReporteVentasDocumentoDto {

    private String titulo;
    private String empresa;
    private String usuarioGenerador;
    private String fechaGeneracion;
    private String rangoFechas;
    private List<ReporteVentaFilaDto> filas = new ArrayList<>();
    private ReporteVentasResumenDto resumen = new ReporteVentasResumenDto();
    private List<String> columnas = List.of(
            "Fecha", "Hora", "Tipo", "Serie", "N° Comprobante", "Cliente", "Documento",
            "Vendedor", "Método pago", "Cód. verificación", "Estado", "Subtotal", "IGV", "Descuento", "Total"
    );

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getUsuarioGenerador() {
        return usuarioGenerador;
    }

    public void setUsuarioGenerador(String usuarioGenerador) {
        this.usuarioGenerador = usuarioGenerador;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public void setFechaGeneracion(String fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public String getRangoFechas() {
        return rangoFechas;
    }

    public void setRangoFechas(String rangoFechas) {
        this.rangoFechas = rangoFechas;
    }

    public List<ReporteVentaFilaDto> getFilas() {
        return filas;
    }

    public void setFilas(List<ReporteVentaFilaDto> filas) {
        this.filas = filas;
    }

    public ReporteVentasResumenDto getResumen() {
        return resumen;
    }

    public void setResumen(ReporteVentasResumenDto resumen) {
        this.resumen = resumen;
    }

    public List<String> getColumnas() {
        return columnas;
    }

    public void setColumnas(List<String> columnas) {
        this.columnas = columnas;
    }
}
