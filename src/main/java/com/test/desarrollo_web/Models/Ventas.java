package com.test.desarrollo_web.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapea la tabla VENTA de la base de datos:
 * id, numero_documento, id_cliente, id_vendedor, id_tipo_pago, id_tipo_comprobante,
 * fecha, total, deuda, estado (ENUM: PENDIENTE, PAGADO, ANULADO),
 * intervalo_dias, numero_cuotas, pago_inicial
 */
@Entity
@Table(name = "VENTA")
public class Ventas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_pago", nullable = false)
    private TipoPago tipoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_comprobante", nullable = false)
    private TipoComprobante tipoComprobante;

    @Column(name = "fecha", updatable = false)
    private LocalDateTime fecha;

    @PrePersist
    void asignarFechaAlCrear() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal deuda = BigDecimal.ZERO;

    @Column(name = "pago_inicial", precision = 10, scale = 2)
    private BigDecimal pagoInicial;

    @Column(name = "numero_cuotas")
    private Integer numeroCuotas;

    @Column(name = "intervalo_dias")
    private Integer intervaloDias;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Estado estado = Estado.PAGADO;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen_venta", length = 10)
    private OrigenVenta origenVenta;

    @Column(name = "codigo_verificacion_pago", length = 30)
    private String codigoVerificacionPago;

    @Column(name = "monto_efectivo", precision = 10, scale = 2)
    private BigDecimal montoEfectivo;

    @Column(name = "monto_yape", precision = 10, scale = 2)
    private BigDecimal montoYape;

    @Column(name = "monto_plin", precision = 10, scale = 2)
    private BigDecimal montoPlin;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VentaCuota> cuotas = new ArrayList<>();

    public enum Estado {
        PENDIENTE,
        PAGADO,
        ANULADO
    }

    public enum OrigenVenta {
        POS,
        WEB
    }

    public Ventas() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Usuario getVendedor() {
        return vendedor;
    }

    public void setVendedor(Usuario vendedor) {
        this.vendedor = vendedor;
    }

    public TipoPago getTipoPago() {
        return tipoPago;
    }

    public void setTipoPago(TipoPago tipoPago) {
        this.tipoPago = tipoPago;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDeuda() {
        return deuda;
    }

    public void setDeuda(BigDecimal deuda) {
        this.deuda = deuda;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public BigDecimal getPagoInicial() {
        return pagoInicial;
    }

    public void setPagoInicial(BigDecimal pagoInicial) {
        this.pagoInicial = pagoInicial;
    }

    public Integer getNumeroCuotas() {
        return numeroCuotas;
    }

    public void setNumeroCuotas(Integer numeroCuotas) {
        this.numeroCuotas = numeroCuotas;
    }

    public Integer getIntervaloDias() {
        return intervaloDias;
    }

    public void setIntervaloDias(Integer intervaloDias) {
        this.intervaloDias = intervaloDias;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }

    public List<VentaCuota> getCuotas() {
        return cuotas;
    }

    public void setCuotas(List<VentaCuota> cuotas) {
        this.cuotas = cuotas;
    }

    public void addDetalle(DetalleVenta detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    public void addCuota(VentaCuota cuota) {
        cuotas.add(cuota);
        cuota.setVenta(this);
    }

    public OrigenVenta getOrigenVenta() {
        return origenVenta;
    }

    public void setOrigenVenta(OrigenVenta origenVenta) {
        this.origenVenta = origenVenta;
    }

    public String getCodigoVerificacionPago() {
        return codigoVerificacionPago;
    }

    public void setCodigoVerificacionPago(String codigoVerificacionPago) {
        this.codigoVerificacionPago = codigoVerificacionPago;
    }

    public BigDecimal getMontoEfectivo() {
        return montoEfectivo;
    }

    public void setMontoEfectivo(BigDecimal montoEfectivo) {
        this.montoEfectivo = montoEfectivo;
    }

    public BigDecimal getMontoYape() {
        return montoYape;
    }

    public void setMontoYape(BigDecimal montoYape) {
        this.montoYape = montoYape;
    }

    public BigDecimal getMontoPlin() {
        return montoPlin;
    }

    public void setMontoPlin(BigDecimal montoPlin) {
        this.montoPlin = montoPlin;
    }
}

