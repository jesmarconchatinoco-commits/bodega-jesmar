package com.test.desarrollo_web.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PedidoCatalogoRequest {
    private String nombreCliente;
    private String telefono;
    private String documento;
    private String observaciones;
    private String formaEntrega;
    private String direccion;
    private String referencia;
    private String distrito;
    private String metodoPago;
    private String codigoValidacionPago;
    private BigDecimal costoEnvio;
    private Long horarioRecojoId;
    private List<PedidoCatalogoItemRequest> items = new ArrayList<>();

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getFormaEntrega() {
        return formaEntrega;
    }

    public void setFormaEntrega(String formaEntrega) {
        this.formaEntrega = formaEntrega;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getDistrito() {
        return distrito;
    }

    public void setDistrito(String distrito) {
        this.distrito = distrito;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getCodigoValidacionPago() {
        return codigoValidacionPago;
    }

    public void setCodigoValidacionPago(String codigoValidacionPago) {
        this.codigoValidacionPago = codigoValidacionPago;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public Long getHorarioRecojoId() {
        return horarioRecojoId;
    }

    public void setHorarioRecojoId(Long horarioRecojoId) {
        this.horarioRecojoId = horarioRecojoId;
    }

    public List<PedidoCatalogoItemRequest> getItems() {
        return items;
    }

    public void setItems(List<PedidoCatalogoItemRequest> items) {
        this.items = items;
    }
}
