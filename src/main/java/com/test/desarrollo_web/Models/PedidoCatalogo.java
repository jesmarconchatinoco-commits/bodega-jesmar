package com.test.desarrollo_web.Models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PEDIDO_CATALOGO")
public class PedidoCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String nombreCliente;

    @Column(length = 20)
    private String telefono;

    @Column(length = 20)
    private String documento;

    @Column(length = 500)
    private String observaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_entrega", length = 30)
    private FormaEntrega formaEntrega;

    @Column(length = 255)
    private String direccion;

    @Column(length = 255)
    private String referencia;

    @Column(length = 100)
    private String distrito;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 20)
    private MetodoPago metodoPago;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "costo_envio", precision = 10, scale = 2)
    private BigDecimal costoEnvio = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_imagen_id")
    private Imagen comprobantePago;

    @Column(name = "codigo_validacion_pago", length = 30)
    private String codigoValidacionPago;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "venta_id")
    private Long ventaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_recojo_id")
    private HorarioAtencion horarioRecojo;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoCatalogoDetalle> detalles = new ArrayList<>();

    public enum Estado {
        PENDIENTE, ATENDIDO, CANCELADO
    }

    public enum FormaEntrega {
        ENVIO_DOMICILIO, RECOJO_TIENDA
    }

    public enum MetodoPago {
        YAPE, PLIN
    }

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public FormaEntrega getFormaEntrega() {
        return formaEntrega;
    }

    public void setFormaEntrega(FormaEntrega formaEntrega) {
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

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public Imagen getComprobantePago() {
        return comprobantePago;
    }

    public void setComprobantePago(Imagen comprobantePago) {
        this.comprobantePago = comprobantePago;
    }

    public String getCodigoValidacionPago() {
        return codigoValidacionPago;
    }

    public void setCodigoValidacionPago(String codigoValidacionPago) {
        this.codigoValidacionPago = codigoValidacionPago;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public Long getVentaId() {
        return ventaId;
    }

    public void setVentaId(Long ventaId) {
        this.ventaId = ventaId;
    }

    public HorarioAtencion getHorarioRecojo() {
        return horarioRecojo;
    }

    public void setHorarioRecojo(HorarioAtencion horarioRecojo) {
        this.horarioRecojo = horarioRecojo;
    }

    public List<PedidoCatalogoDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<PedidoCatalogoDetalle> detalles) {
        this.detalles = detalles;
    }

    public void addDetalle(PedidoCatalogoDetalle detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }
}
