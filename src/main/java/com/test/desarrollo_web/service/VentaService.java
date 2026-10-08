package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.*;
import com.test.desarrollo_web.Repository.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.test.desarrollo_web.dto.DetalleVentaItemDto;
import com.test.desarrollo_web.util.ImagenRutas;
import com.test.desarrollo_web.util.PresentacionMedidaUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VentaService {

    private static final String TIPO_CONTADO = "Contado";
    private static final String TIPO_CREDITO = "Crédito";
    private static final String TIPO_PAGO_MIXTO = "Pago Mixto";
    private static final List<String> TIPOS_PAGO_POS = List.of(TIPO_CONTADO, "Yape", "Plin", TIPO_PAGO_MIXTO);

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final TipoPagoRepository tipoPagoRepository;
    private final TipoComprobanteRepository tipoComprobanteRepository;
    private final ProductoRepository productoRepository;
    private final VentaCuotaRepository ventaCuotaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ClienteService clienteService;
    private final PermissionService permissionService;
    private final ProductoPresentacionService presentacionService;
    private final CategoriaRepository categoriaRepository;
    private final ObjectMapper objectMapper;
    private final MovimientoInventarioService movimientoInventarioService;
    private final PedidoCatalogoRepository pedidoCatalogoRepository;

    public VentaService(VentaRepository ventaRepository,
                        ClienteRepository clienteRepository,
                        UsuarioRepository usuarioRepository,
                        TipoPagoRepository tipoPagoRepository,
                        TipoComprobanteRepository tipoComprobanteRepository,
                        ProductoRepository productoRepository,
                        VentaCuotaRepository ventaCuotaRepository,
                        DetalleVentaRepository detalleVentaRepository,
                        ClienteService clienteService,
                        PermissionService permissionService,
                        ProductoPresentacionService presentacionService,
                        CategoriaRepository categoriaRepository,
                        ObjectMapper objectMapper,
                        MovimientoInventarioService movimientoInventarioService,
                        PedidoCatalogoRepository pedidoCatalogoRepository) {
        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.tipoComprobanteRepository = tipoComprobanteRepository;
        this.productoRepository = productoRepository;
        this.ventaCuotaRepository = ventaCuotaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.clienteService = clienteService;
        this.permissionService = permissionService;
        this.presentacionService = presentacionService;
        this.categoriaRepository = categoriaRepository;
        this.objectMapper = objectMapper;
        this.movimientoInventarioService = movimientoInventarioService;
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
    }

    @Transactional(readOnly = true)
    public List<Ventas> listarVentas() {
        return ventaRepository.findAllWithDetalles();
    }

    public Map<String, Object> obtenerSiguienteNumero(Integer tipoComprobanteId) {
        TipoComprobante comprobante = tipoComprobanteRepository.findById(tipoComprobanteId)
                .orElseThrow(() -> new RuntimeException("Tipo de comprobante no encontrado"));

        int siguiente = comprobante.getCorrelativoActual() + 1;
        String numeroFormateado = String.format("%08d", siguiente);
        String numeroDocumento = comprobante.getSerie() + "-" + numeroFormateado;

        return Map.of(
                "serie", comprobante.getSerie(),
                "correlativo", numeroFormateado,
                "numeroDocumento", numeroDocumento,
                "nombre", comprobante.getNombre()
        );
    }

    public Map<String, Object> buscarClienteEnBodega(String documento) {
        return clienteService.buscarSoloEnBodega(documento);
    }

    public Map<String, Object> buscarOCrearCliente(String documento, String nombreManual) {
        return clienteService.buscarOCrearCliente(documento, nombreManual);
    }

    @Transactional
    public Ventas guardarVenta(Integer clienteId,
                               Integer tipoComprobanteId,
                               Integer tipoPagoId,
                               boolean ventaCredito,
                               BigDecimal pagoInicial,
                               Integer numeroCuotas,
                               Integer intervaloDias,
                               String detallesJson,
                               String fechasCuotasJson,
                               String codigoVerificacionPago,
                               BigDecimal montoEfectivo,
                               BigDecimal montoYape,
                               BigDecimal montoPlin) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        TipoComprobante comprobante = tipoComprobanteRepository.findById(tipoComprobanteId)
                .orElseThrow(() -> new RuntimeException("Tipo de comprobante no encontrado"));
        TipoPago tipoPago = obtenerTipoPagoPos(tipoPagoId);

        Usuario vendedor = permissionService.getUsuarioActual();
        if (vendedor == null) {
            vendedor = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay vendedor disponible"));
        }

        List<DetalleVentaItemDto> items = parseDetalles(detallesJson);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Agregue al menos un producto a la venta.");
        }

        int siguienteCorrelativo = comprobante.getCorrelativoActual() + 1;
        String numeroDocumento = comprobante.getSerie() + "-" + String.format("%08d", siguienteCorrelativo);

        Ventas venta = new Ventas();
        venta.setNumeroDocumento(numeroDocumento);
        venta.setCliente(cliente);
        venta.setVendedor(vendedor);
        venta.setTipoComprobante(comprobante);
        venta.setTipoPago(tipoPago);
        venta.setOrigenVenta(Ventas.OrigenVenta.POS);

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVentaItemDto item : items) {
            total = total.add(agregarDetalleVenta(venta, item, MovimientoInventario.TipoMovimiento.VENTA_POS));
        }
        venta.setTotal(total);
        aplicarVentaContado(venta, total);
        aplicarDatosPagoPos(venta, tipoPago, total, codigoVerificacionPago, montoEfectivo, montoYape, montoPlin);

        comprobante.setCorrelativoActual(siguienteCorrelativo);
        tipoComprobanteRepository.save(comprobante);

        return ventaRepository.save(venta);
    }

    private BigDecimal agregarDetalleVenta(Ventas venta, DetalleVentaItemDto item,
                                           MovimientoInventario.TipoMovimiento tipoMovimiento) {
        if (item.getPresentacionId() == null) {
            throw new RuntimeException("Cada producto de la venta debe tener una presentación seleccionada.");
        }
        ProductoPresentacion presentacion = presentacionService.obtenerParaVenta(
                item.getPresentacionId(), item.getProductoId());
        Producto producto = presentacion.getProducto();

        if (presentacion.getStock() < item.getCantidad()) {
            throw new RuntimeException("Stock insuficiente para: " + producto.getNombre()
                    + " (" + presentacion.getNombre() + ")");
        }

        BigDecimal precio = item.getPrecio() != null ? item.getPrecio() : presentacion.getPrecio();
        BigDecimal descuento = item.getDescuento() != null ? item.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(BigDecimal.ZERO) < 0) {
            descuento = BigDecimal.ZERO;
        }
        BigDecimal bruto = precio.multiply(BigDecimal.valueOf(item.getCantidad()));
        BigDecimal subtotal = bruto.subtract(descuento);
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            subtotal = BigDecimal.ZERO;
        }

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setPresentacion(presentacion);
        detalle.setCantidad(item.getCantidad());
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(subtotal);
        venta.addDetalle(detalle);

        int stockAnterior = presentacion.getStock() != null ? presentacion.getStock() : 0;
        presentacionService.descontarStock(presentacion, item.getCantidad());
        int stockNuevo = stockAnterior - item.getCantidad();
        movimientoInventarioService.registrarTraza(
                presentacion,
                tipoMovimiento != null ? tipoMovimiento : MovimientoInventario.TipoMovimiento.VENTA_POS,
                item.getCantidad(),
                stockAnterior,
                stockNuevo,
                venta.getNumeroDocumento(),
                venta.getId(),
                tipoMovimiento == MovimientoInventario.TipoMovimiento.VENTA_WEB
                        ? "Venta Web" : "Venta POS"
        );

        return subtotal;
    }

    private void restaurarStockDetalle(DetalleVenta detalle) {
        if (detalle.getPresentacion() != null) {
            ProductoPresentacion presentacion = detalle.getPresentacion();
            int stockAnterior = presentacion.getStock() != null ? presentacion.getStock() : 0;
            presentacionService.restaurarStock(presentacion, detalle.getCantidad());
            int stockNuevo = stockAnterior + detalle.getCantidad();
            Ventas venta = detalle.getVenta();
            movimientoInventarioService.registrarTraza(
                    presentacion,
                    MovimientoInventario.TipoMovimiento.INGRESO_DEVOLUCION,
                    detalle.getCantidad(),
                    stockAnterior,
                    stockNuevo,
                    venta != null ? venta.getNumeroDocumento() : "DEVOLUCION",
                    venta != null ? venta.getId() : null,
                    "Devolución / anulación de venta"
            );
            return;
        }
        Producto producto = detalle.getProducto();
        if (producto != null) {
            producto.setStock(producto.getStock() + detalle.getCantidad());
            productoRepository.save(producto);
            presentacionService.sincronizarDesdeProductoPadre(producto);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentaParaEditar(Long ventaId) {
        Ventas venta = ventaRepository.findByIdCompleta(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (venta.getEstado() == Ventas.Estado.ANULADO) {
            throw new RuntimeException("No se puede editar una venta anulada.");
        }
        if (esVentaCredito(venta)) {
            throw new RuntimeException("No se puede editar una venta a crédito. Solo ventas al contado.");
        }

        Map<Integer, Integer> stockOriginal = new LinkedHashMap<>();
        List<Map<String, Object>> productos = new ArrayList<>();
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                Integer productoId = d.getProducto() != null ? d.getProducto().getId() : null;
                if (productoId == null) continue;

                Integer presentacionId = d.getPresentacion() != null ? d.getPresentacion().getId() : null;
                int cantidad = d.getCantidad();
                int stockKey = presentacionId != null ? presentacionId : productoId;
                stockOriginal.put(stockKey, stockOriginal.getOrDefault(stockKey, 0) + cantidad);

                String nombreLinea = d.getProducto().getNombre();
                String presentacionNombre = d.getPresentacion() != null ? d.getPresentacion().getNombre() : "—";
                if (d.getPresentacion() != null) {
                    nombreLinea += " — " + presentacionNombre;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("productoId", productoId);
                item.put("presentacionId", presentacionId);
                item.put("productoNombre", d.getProducto().getNombre());
                item.put("presentacionNombre", presentacionNombre);
                Map<String, String> medida = PresentacionMedidaUtil.extraerMedida(presentacionNombre);
                item.put("medida", medida.get("medida"));
                if (d.getProducto() != null) {
                    item.put("marca", d.getProducto().getDescripcion() != null && !d.getProducto().getDescripcion().isBlank()
                            ? d.getProducto().getDescripcion().trim() : "Sin marca");
                }
                if (d.getPresentacion() != null) {
                    String rutaImagen = presentacionService.rutaImagenPrincipal(d.getPresentacion());
                    item.put("imagenRuta", rutaImagen != null ? rutaImagen : "");
                    item.put("imagen", ImagenRutas.toPublicUrl(rutaImagen));
                } else {
                    item.put("imagenRuta", "");
                    item.put("imagen", null);
                }
                item.put("nombre", nombreLinea);
                item.put("cantidad", cantidad);
                item.put("precio", d.getPrecioUnitario());
                int stockActual = d.getPresentacion() != null
                        ? d.getPresentacion().getStock()
                        : d.getProducto().getStock();
                item.put("stock", stockActual + cantidad);
                productos.add(item);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ventaId", venta.getId());
        res.put("numeroDocumento", venta.getNumeroDocumento());
        res.put("clienteId", venta.getCliente() != null ? venta.getCliente().getId() : null);
        res.put("documentoCliente", venta.getCliente() != null ? venta.getCliente().getDocumento() : "");
        res.put("nombreCliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "");
        res.put("tipoComprobanteId", venta.getTipoComprobante() != null ? venta.getTipoComprobante().getId() : null);
        res.put("tipoPagoId", venta.getTipoPago() != null ? venta.getTipoPago().getId() : null);
        res.put("productos", productos);
        res.put("stockOriginal", stockOriginal);
        return res;
    }

    @Transactional
    public Ventas actualizarVenta(Long ventaId,
                                  Integer clienteId,
                                  Integer tipoPagoId,
                                  boolean ventaCredito,
                                  BigDecimal pagoInicial,
                                  Integer numeroCuotas,
                                  Integer intervaloDias,
                                  String detallesJson,
                                  String fechasCuotasJson,
                                  String codigoVerificacionPago,
                                  BigDecimal montoEfectivo,
                                  BigDecimal montoYape,
                                  BigDecimal montoPlin) {

        Ventas venta = ventaRepository.findByIdWithDetalles(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (venta.getEstado() == Ventas.Estado.ANULADO) {
            throw new RuntimeException("No se puede editar una venta anulada.");
        }
        if (esVentaCredito(venta)) {
            throw new RuntimeException("No se puede editar una venta a crédito. Solo ventas al contado.");
        }

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<DetalleVentaItemDto> items = parseDetalles(detallesJson);
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Agregue al menos un producto a la venta.");
        }

        for (DetalleVenta detalle : new ArrayList<>(venta.getDetalles())) {
            restaurarStockDetalle(detalle);
            venta.getDetalles().remove(detalle);
        }

        venta.setCliente(cliente);

        TipoPago tipoPago = obtenerTipoPagoPos(tipoPagoId);
        venta.setTipoPago(tipoPago);

        BigDecimal total = BigDecimal.ZERO;
        MovimientoInventario.TipoMovimiento tipoMov = resolverTipoMovimientoVenta(venta);
        for (DetalleVentaItemDto item : items) {
            total = total.add(agregarDetalleVenta(venta, item, tipoMov));
        }
        venta.setTotal(total);
        aplicarVentaContado(venta, total);
        aplicarDatosPagoPos(venta, tipoPago, total, codigoVerificacionPago, montoEfectivo, montoYape, montoPlin);

        return ventaRepository.save(venta);
    }

    private void aplicarVentaContado(Ventas venta, BigDecimal total) {
        venta.setEstado(Ventas.Estado.PAGADO);
        venta.setPagoInicial(total);
        venta.setDeuda(BigDecimal.ZERO);
        venta.setNumeroCuotas(null);
        venta.setIntervaloDias(null);
    }

    @Transactional(readOnly = true)
    public List<TipoPago> listarTiposPagoPos() {
        return tipoPagoRepository.findAll().stream()
                .filter(this::esTipoPagoPos)
                .sorted(Comparator.comparingInt(this::ordenTipoPagoPos))
                .toList();
    }

    private TipoPago obtenerTipoPagoPos(Integer tipoPagoId) {
        TipoPago tipoPago = tipoPagoRepository.findById(tipoPagoId)
                .orElseThrow(() -> new RuntimeException("Tipo de pago no encontrado"));
        if (!esTipoPagoPos(tipoPago)) {
            throw new RuntimeException("Tipo de pago no permitido. Seleccione Contado, Yape, Plin o Pago Mixto.");
        }
        return tipoPago;
    }

    private boolean esTipoPagoPos(TipoPago tipo) {
        if (tipo == null || tipo.getNombre() == null) {
            return false;
        }
        String nombre = tipo.getNombre().trim();
        return TIPOS_PAGO_POS.stream().anyMatch(n -> n.equalsIgnoreCase(nombre));
    }

    private int ordenTipoPagoPos(TipoPago tipo) {
        String n = tipo.getNombre() != null ? tipo.getNombre().toLowerCase() : "";
        if (n.contains("contado")) return 0;
        if (n.contains("yape")) return 1;
        if (n.contains("plin")) return 2;
        if (n.contains("mixto")) return 3;
        return 99;
    }

    private void aplicarDatosPagoPos(Ventas venta,
                                     TipoPago tipoPago,
                                     BigDecimal total,
                                     String codigoVerificacionPago,
                                     BigDecimal montoEfectivo,
                                     BigDecimal montoYape,
                                     BigDecimal montoPlin) {
        String nombre = tipoPago != null && tipoPago.getNombre() != null
                ? tipoPago.getNombre().trim() : "";

        venta.setCodigoVerificacionPago(null);
        venta.setMontoEfectivo(null);
        venta.setMontoYape(null);
        venta.setMontoPlin(null);

        if (esPagoMixto(nombre)) {
            BigDecimal efectivo = nz(montoEfectivo);
            BigDecimal yape = nz(montoYape);
            BigDecimal plin = nz(montoPlin);
            BigDecimal suma = efectivo.add(yape).add(plin).setScale(2, RoundingMode.HALF_UP);
            if (suma.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) {
                throw new RuntimeException("La suma de los montos debe ser exactamente igual al total de la venta.");
            }
            venta.setMontoEfectivo(efectivo);
            venta.setMontoYape(yape);
            venta.setMontoPlin(plin);
            if (yape.compareTo(BigDecimal.ZERO) > 0 || plin.compareTo(BigDecimal.ZERO) > 0) {
                venta.setCodigoVerificacionPago(validarCodigoVerificacion(codigoVerificacionPago));
            }
            return;
        }

        if (esPagoDigital(nombre)) {
            venta.setCodigoVerificacionPago(validarCodigoVerificacion(codigoVerificacionPago));
            if ("Yape".equalsIgnoreCase(nombre)) {
                venta.setMontoYape(total);
            } else if ("Plin".equalsIgnoreCase(nombre)) {
                venta.setMontoPlin(total);
            }
            return;
        }

        venta.setMontoEfectivo(total);
    }

    private String validarCodigoVerificacion(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new RuntimeException("El código de verificación es obligatorio para pagos con Yape o Plin.");
        }
        String limpio = codigo.trim().toUpperCase();
        if (!limpio.matches("^[A-Z0-9\\-]{6,30}$")) {
            throw new RuntimeException("El código de verificación debe tener entre 6 y 30 caracteres alfanuméricos.");
        }
        return limpio;
    }

    private boolean esPagoDigital(String nombrePago) {
        if (nombrePago == null) return false;
        String n = nombrePago.toLowerCase();
        return n.contains("yape") || n.contains("plin");
    }

    private boolean esPagoMixto(String nombrePago) {
        return nombrePago != null && nombrePago.toLowerCase().contains("mixto");
    }

    private BigDecimal nz(BigDecimal valor) {
        return valor != null ? valor.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String resolverEtiquetaOrigen(Ventas venta) {
        if (venta == null) return "POS";
        if (venta.getOrigenVenta() != null) {
            return venta.getOrigenVenta() == Ventas.OrigenVenta.WEB ? "Web" : "POS";
        }
        if (venta.getId() != null && pedidoCatalogoRepository.existsByVentaId(venta.getId())) {
            return "Web";
        }
        return "POS";
    }

    private MovimientoInventario.TipoMovimiento resolverTipoMovimientoVenta(Ventas venta) {
        String origen = resolverEtiquetaOrigen(venta);
        return "Web".equalsIgnoreCase(origen)
                ? MovimientoInventario.TipoMovimiento.VENTA_WEB
                : MovimientoInventario.TipoMovimiento.VENTA_POS;
    }

    @Transactional
    public void anularVenta(Long id) {
        Ventas venta = ventaRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if (venta.getEstado() == Ventas.Estado.ANULADO) {
            return;
        }

        for (DetalleVenta detalle : venta.getDetalles()) {
            restaurarStockDetalle(detalle);
        }

        venta.setEstado(Ventas.Estado.ANULADO);
        venta.setDeuda(BigDecimal.ZERO);
        ventaRepository.save(venta);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProductosActivos() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (ProductoPresentacion presentacion : presentacionService.listarActivasParaVenta()) {
            Producto producto = presentacion.getProducto();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", presentacion.getId());
            item.put("presentacionId", presentacion.getId());
            item.put("productoId", producto.getId());
            item.put("codigo", String.format("P%05d", producto.getId()));
            item.put("codigoPresentacion", String.format("PR%05d", presentacion.getId()));
            item.put("productoNombre", producto.getNombre());
            item.put("marca", producto.getDescripcion() != null && !producto.getDescripcion().isBlank()
                    ? producto.getDescripcion().trim() : "Sin marca");
            item.put("presentacionNombre", presentacion.getNombre());
            Map<String, String> medida = PresentacionMedidaUtil.extraerMedida(presentacion.getNombre());
            item.put("medida", medida.get("medida"));
            item.put("cantidadMedida", medida.get("cantidad"));
            item.put("unidadMedida", medida.get("unidad"));
            item.put("nombre", producto.getNombre() + " — " + presentacion.getNombre());
            item.put("precio", presentacion.getPrecio());
            item.put("stock", presentacion.getStock());
            item.put("codigoBarras", presentacion.getCodigoBarras() != null ? presentacion.getCodigoBarras() : "");
            if (producto.getCategoria() != null) {
                item.put("categoriaId", producto.getCategoria().getId());
                item.put("categoriaNombre", producto.getCategoria().getNombre());
            } else {
                item.put("categoriaId", null);
                item.put("categoriaNombre", "Sin categoría");
            }
            String rutaImagen = presentacionService.rutaImagenPrincipal(presentacion);
            item.put("imagenRuta", rutaImagen != null ? rutaImagen : "");
            item.put("imagen", ImagenRutas.toPublicUrl(rutaImagen));
            resultado.add(item);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCategoriasParaVenta() {
        List<Categoria> categorias = categoriaRepository.findByEstado(Categoria.Estado.ACTIVO);
        if (categorias.isEmpty()) {
            categorias = categoriaRepository.findAll();
        }
        return categorias.stream()
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", c.getId());
                    map.put("nombre", c.getNombre());
                    return map;
                })
                .toList();
    }

    private List<DetalleVentaItemDto> parseDetalles(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<DetalleVentaItemDto> items = objectMapper.readValue(json, new TypeReference<>() {});
            return items != null ? items : List.of();
        } catch (Exception e) {
            throw new RuntimeException("No se pudieron leer los productos de la venta.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerCuotasVenta(Long ventaId) {
        Ventas venta = ventaRepository.findByIdWithCliente(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        List<VentaCuota> cuotasEntidad = ventaCuotaRepository.findByVenta_IdOrderByNumeroCuotaAsc(ventaId);
        VentaCuota siguientePendiente = cuotasEntidad.stream()
                .filter(c -> c.getEstado() == VentaCuota.Estado.PENDIENTE)
                .findFirst()
                .orElse(null);
        List<Map<String, Object>> cuotas = cuotasEntidad.stream()
                .map(c -> mapearCuotaConReglas(c, siguientePendiente))
                .collect(Collectors.toList());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ventaId", venta.getId());
        res.put("cliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "—");
        res.put("deudaRestante", venta.getDeuda());
        res.put("cuotas", cuotas);
        return res;
    }

    @Transactional
    public Map<String, Object> pagarCuota(Long cuotaId, String medioPago, String referenciaPago) {
        VentaCuota cuota = ventaCuotaRepository.findByIdWithVenta(cuotaId)
                .orElseThrow(() -> new RuntimeException("Cuota no encontrada"));
        if (cuota.getEstado() == VentaCuota.Estado.PAGADA) {
            throw new RuntimeException("Esta cuota ya fue pagada.");
        }

        Ventas venta = cuota.getVenta();
        if (venta == null) {
            throw new RuntimeException("Venta no encontrada");
        }

        validarCuotaPagable(cuota);

        String medio = medioPago != null && !medioPago.isBlank() ? medioPago.trim().toUpperCase() : "EFECTIVO";
        String referencia = referenciaPago != null ? referenciaPago.replaceAll("\\s+", "").trim() : "";
        validarReferenciaPago(medio, referencia);

        cuota.setEstado(VentaCuota.Estado.PAGADA);
        cuota.setFechaPagoReal(LocalDate.now());
        cuota.setMedioPago(medio);
        cuota.setReferenciaPago(referencia.isBlank() ? null : referencia);
        ventaCuotaRepository.save(cuota);

        BigDecimal nuevaDeuda = venta.getDeuda().subtract(cuota.getMonto()).max(BigDecimal.ZERO);
        venta.setDeuda(nuevaDeuda.setScale(2, RoundingMode.HALF_UP));
        if (venta.getDeuda().compareTo(BigDecimal.ZERO) <= 0) {
            venta.setDeuda(BigDecimal.ZERO);
            venta.setEstado(Ventas.Estado.PAGADO);
        }
        ventaRepository.save(venta);

        return Map.of(
                "success", true,
                "deudaRestante", venta.getDeuda(),
                "message", "Cuota pagada correctamente."
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerHistorialPagos(Long ventaId) {
        Ventas venta = ventaRepository.findByIdWithCliente(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        List<Map<String, Object>> pagos = new ArrayList<>();
        if (venta.getPagoInicial() != null && venta.getPagoInicial().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> inicial = new LinkedHashMap<>();
            inicial.put("numeroCuota", 0);
            inicial.put("fechaVencimiento", venta.getFecha() != null ? venta.getFecha().toLocalDate().toString() : null);
            inicial.put("fechaPago", venta.getFecha() != null ? venta.getFecha().toLocalDate().toString() : null);
            inicial.put("monto", venta.getPagoInicial());
            inicial.put("estado", "PAGADA");
            inicial.put("medioPago", "PAGO INICIAL");
            inicial.put("etiqueta", "Pago inicial");
            pagos.add(inicial);
        }

        for (VentaCuota cuota : ventaCuotaRepository.findByVenta_IdAndEstadoOrderByNumeroCuotaAsc(
                ventaId, VentaCuota.Estado.PAGADA)) {
            pagos.add(mapearCuotaPagada(cuota));
        }

        if (pagos.isEmpty() && venta.getEstado() == Ventas.Estado.PAGADO
                && (venta.getDeuda() == null || venta.getDeuda().compareTo(BigDecimal.ZERO) == 0)) {
            Map<String, Object> contado = new LinkedHashMap<>();
            contado.put("numeroCuota", 1);
            contado.put("fechaVencimiento", venta.getFecha() != null ? venta.getFecha().toLocalDate().toString() : null);
            contado.put("fechaPago", venta.getFecha() != null ? venta.getFecha().toLocalDate().toString() : null);
            contado.put("monto", venta.getTotal());
            contado.put("estado", "PAGADA");
            contado.put("medioPago", venta.getTipoPago() != null ? venta.getTipoPago().getNombre() : "CONTADO");
            contado.put("etiqueta", "Pago al contado");
            pagos.add(contado);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ventaId", venta.getId());
        res.put("cliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "—");
        res.put("deudaRestante", venta.getDeuda());
        res.put("liquidada", venta.getDeuda() == null || venta.getDeuda().compareTo(BigDecimal.ZERO) <= 0);
        res.put("pagos", pagos);
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleVenta(Long ventaId) {
        Ventas venta = ventaRepository.findByIdCompleta(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        List<Map<String, Object>> productos = new ArrayList<>();
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("producto", d.getProducto() != null ? d.getProducto().getNombre() : "—");
                item.put("cantidad", d.getCantidad());
                item.put("precio", d.getPrecioUnitario());
                item.put("subtotal", d.getSubtotal());
                productos.add(item);
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", venta.getId());
        res.put("numeroDocumento", venta.getNumeroDocumento());
        res.put("cliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "—");
        res.put("documentoCliente", venta.getCliente() != null ? venta.getCliente().getDocumento() : "");
        res.put("vendedor", venta.getVendedor() != null ? venta.getVendedor().getNombre() : "—");
        res.put("fecha", venta.getFecha() != null ? venta.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
        res.put("tipoPago", venta.getTipoPago() != null ? venta.getTipoPago().getNombre() : "—");
        res.put("origenVenta", resolverEtiquetaOrigen(venta));
        res.put("codigoVerificacionPago", venta.getCodigoVerificacionPago() != null ? venta.getCodigoVerificacionPago() : "—");
        res.put("montoEfectivo", venta.getMontoEfectivo());
        res.put("montoYape", venta.getMontoYape());
        res.put("montoPlin", venta.getMontoPlin());
        res.put("comprobante", venta.getTipoComprobante() != null ? venta.getTipoComprobante().getNombre() : "—");
        res.put("total", venta.getTotal());
        res.put("deuda", venta.getDeuda());
        res.put("pagoInicial", venta.getPagoInicial());
        res.put("estado", venta.getEstado() != null ? venta.getEstado().name() : "");
        res.put("productos", productos);
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarInventario() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (ProductoPresentacion pp : presentacionService.listarActivasParaVenta()) {
            Producto producto = pp.getProducto();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pp.getId());
            item.put("presentacionId", pp.getId());
            item.put("productoId", producto.getId());
            item.put("productoNombre", producto.getNombre());
            item.put("presentacionNombre", pp.getNombre());
            item.put("nombre", producto.getNombre() + " — " + pp.getNombre());
            item.put("stock", pp.getStock() != null ? pp.getStock() : 0);
            item.put("stockMinimo", pp.getStockMinimo() != null ? pp.getStockMinimo() : 0);
            int stock = pp.getStock() != null ? pp.getStock() : 0;
            int stockMin = pp.getStockMinimo() != null ? pp.getStockMinimo() : 0;
            item.put("bajo", stock <= stockMin);
            lista.add(item);
        }
        return lista;
    }

    @Transactional
    public Map<String, Object> actualizarStockPresentacion(Integer presentacionId, Integer stock) {
        ProductoPresentacion presentacion = presentacionService.ajustarStockPresentacionInventario(presentacionId, stock);
        int stockActual = presentacion.getStock() != null ? presentacion.getStock() : 0;
        int stockMin = presentacion.getStockMinimo() != null ? presentacion.getStockMinimo() : 0;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("presentacionId", presentacion.getId());
        res.put("productoId", presentacion.getProducto().getId());
        res.put("stock", stockActual);
        res.put("stockMinimo", stockMin);
        res.put("bajo", stockActual <= stockMin);
        res.put("message", "Stock de la presentación actualizado correctamente.");
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> movimientosPresentacion(Integer presentacionId) {
        ProductoPresentacion presentacion = presentacionService.obtenerPresentacion(presentacionId);
        Producto producto = presentacion.getProducto();

        List<Map<String, Object>> movimientos = new ArrayList<>();
        BigDecimal totalVendido = BigDecimal.ZERO;
        for (DetalleVenta dv : detalleVentaRepository.findMovimientosByPresentacion(presentacionId)) {
            Map<String, Object> mov = mapearMovimientoVenta(dv);
            movimientos.add(mov);
            totalVendido = totalVendido.add(dv.getSubtotal());
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("presentacionId", presentacion.getId());
        res.put("productoId", producto.getId());
        res.put("producto", producto.getNombre());
        res.put("presentacion", presentacion.getNombre());
        res.put("nombre", producto.getNombre() + " — " + presentacion.getNombre());
        res.put("stock", presentacion.getStock() != null ? presentacion.getStock() : 0);
        res.put("totalVendido", totalVendido);
        res.put("movimientos", movimientos);
        return res;
    }

    private Map<String, Object> mapearMovimientoVenta(DetalleVenta dv) {
        Map<String, Object> mov = new LinkedHashMap<>();
        Ventas v = dv.getVenta();
        mov.put("fecha", v.getFecha() != null ? v.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—");
        mov.put("numeroDocumento", v.getNumeroDocumento());
        mov.put("precio", dv.getPrecioUnitario());
        mov.put("cantidad", dv.getCantidad());
        mov.put("subtotal", dv.getSubtotal());
        return mov;
    }

    @Transactional(readOnly = true)
    public Ventas obtenerVentaParaImprimir(Long ventaId) {
        return ventaRepository.findByIdCompleta(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
    }

    public String generarCsvInventario() {
        StringBuilder sb = new StringBuilder();
        sb.append("Producto,Presentacion,Stock actual,Stock minimo,Estado stock\n");
        for (Map<String, Object> item : listarInventario()) {
            sb.append(csv(String.valueOf(item.get("productoNombre")))).append(',');
            sb.append(csv(String.valueOf(item.get("presentacionNombre")))).append(',');
            sb.append(item.get("stock")).append(',');
            sb.append(item.get("stockMinimo")).append(',');
            sb.append(Boolean.TRUE.equals(item.get("bajo")) ? "Bajo" : "Normal").append('\n');
        }
        return sb.toString();
    }

    public String generarCsvMovimientos(Integer presentacionId) {
        Map<String, Object> data = movimientosPresentacion(presentacionId);
        StringBuilder sb = new StringBuilder();
        sb.append("Producto,Presentacion,Stock actual,Total vendido\n");
        sb.append(csv(String.valueOf(data.get("producto")))).append(',');
        sb.append(csv(String.valueOf(data.get("presentacion")))).append(',');
        sb.append(data.get("stock")).append(',');
        sb.append(data.get("totalVendido")).append('\n');
        sb.append("\nFecha Venta,Nº Documento,Precio Venta,Cantidad,Subtotal\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> movimientos = (List<Map<String, Object>>) data.get("movimientos");
        if (movimientos != null) {
            for (Map<String, Object> m : movimientos) {
                sb.append(csv(String.valueOf(m.get("fecha")))).append(',');
                sb.append(csv(String.valueOf(m.get("numeroDocumento")))).append(',');
                sb.append(m.get("precio")).append(',');
                sb.append(m.get("cantidad")).append(',');
                sb.append(m.get("subtotal")).append('\n');
            }
        }
        return sb.toString();
    }

    public String generarCsvVenta(Ventas venta) {
        StringBuilder sb = new StringBuilder();
        sb.append("Documento,Cliente,Fecha,Vendedor,Forma Pago,Total,Deuda,Estado\n");
        sb.append(csv(venta.getNumeroDocumento())).append(',');
        sb.append(csv(venta.getCliente() != null ? venta.getCliente().getNombre() : "")).append(',');
        sb.append(csv(venta.getFecha() != null ? venta.getFecha().toString() : "")).append(',');
        sb.append(csv(venta.getVendedor() != null ? venta.getVendedor().getNombre() : "")).append(',');
        sb.append(csv(venta.getTipoPago() != null ? venta.getTipoPago().getNombre() : "")).append(',');
        sb.append(venta.getTotal()).append(',');
        sb.append(venta.getDeuda()).append(',');
        sb.append(venta.getEstado()).append('\n');
        sb.append("\nProducto,Cantidad,Precio,Subtotal\n");
        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                sb.append(csv(d.getProducto() != null ? d.getProducto().getNombre() : "")).append(',');
                sb.append(d.getCantidad()).append(',');
                sb.append(d.getPrecioUnitario()).append(',');
                sb.append(d.getSubtotal()).append('\n');
            }
        }
        return sb.toString();
    }

    private String csv(String valor) {
        if (valor == null) return "";
        if (valor.contains(",") || valor.contains("\"")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }

    private Map<String, Object> mapearCuota(VentaCuota cuota) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cuota.getId());
        m.put("numeroCuota", cuota.getNumeroCuota());
        m.put("fechaVencimiento", cuota.getFechaPago().toString());
        m.put("monto", cuota.getMonto());
        m.put("estado", cuota.getEstado().name());
        m.put("fechaPagoReal", cuota.getFechaPagoReal() != null ? cuota.getFechaPagoReal().toString() : null);
        m.put("medioPago", cuota.getMedioPago());
        m.put("referenciaPago", cuota.getReferenciaPago());
        return m;
    }

    private void validarReferenciaPago(String medio, String referencia) {
        switch (medio) {
            case "YAPE", "PLIN" -> {
                if (referencia.isBlank()) {
                    throw new RuntimeException("Ingrese el número de celular del pago.");
                }
                if (!referencia.matches("^9\\d{8}$")) {
                    throw new RuntimeException("El celular debe tener 9 dígitos y comenzar con 9.");
                }
            }
            case "TRANSFERENCIA" -> {
                if (referencia.isBlank()) {
                    throw new RuntimeException("Ingrese el número de celular o cuenta de la transferencia.");
                }
                if (!referencia.matches("^\\d{9,20}$")) {
                    throw new RuntimeException("Ingrese un número de celular o cuenta válido.");
                }
            }
            case "TARJETA" -> {
                if (referencia.isBlank()) {
                    throw new RuntimeException("Ingrese el número de tarjeta.");
                }
                if (!referencia.matches("^\\d{13,19}$")) {
                    throw new RuntimeException("El número de tarjeta debe tener entre 13 y 19 dígitos.");
                }
            }
            case "CHEQUE" -> {
                if (referencia.isBlank()) {
                    throw new RuntimeException("Ingrese el número de cheque.");
                }
                if (!referencia.matches("^[A-Za-z0-9\\-]{3,30}$")) {
                    throw new RuntimeException("Ingrese un número de cheque válido.");
                }
            }
            default -> {
                // Efectivo u otros: referencia opcional
            }
        }
    }

    private Map<String, Object> mapearCuotaConReglas(VentaCuota cuota, VentaCuota siguientePendiente) {
        Map<String, Object> m = mapearCuota(cuota);
        boolean pagada = cuota.getEstado() == VentaCuota.Estado.PAGADA;
        boolean esSiguiente = siguientePendiente != null && cuota.getId().equals(siguientePendiente.getId());
        boolean pagable = !pagada && esSiguiente;

        m.put("pagable", pagable);
        if (pagada || esSiguiente) {
            m.put("motivoBloqueo", null);
        } else {
            m.put("motivoBloqueo", "Debe pagar las cuotas anteriores primero.");
        }
        return m;
    }

    private void validarCuotaPagable(VentaCuota cuota) {
        Long ventaId = cuota.getVenta().getId();
        List<VentaCuota> cuotas = ventaCuotaRepository.findByVenta_IdOrderByNumeroCuotaAsc(ventaId);

        VentaCuota primeraPendiente = cuotas.stream()
                .filter(c -> c.getEstado() == VentaCuota.Estado.PENDIENTE)
                .findFirst()
                .orElse(null);

        if (primeraPendiente == null || !primeraPendiente.getId().equals(cuota.getId())) {
            throw new RuntimeException("Solo puede pagar la siguiente cuota pendiente en orden.");
        }
    }

    private Map<String, Object> mapearCuotaPagada(VentaCuota cuota) {
        Map<String, Object> m = mapearCuota(cuota);
        m.put("fechaPago", cuota.getFechaPagoReal() != null ? cuota.getFechaPagoReal().toString() : null);
        m.put("etiqueta", "Cuota " + cuota.getNumeroCuota());
        return m;
    }

    public boolean esVentaCredito(Ventas venta) {
        if (venta == null) return false;
        if (venta.getDeuda() != null && venta.getDeuda().compareTo(BigDecimal.ZERO) > 0) return true;
        if (venta.getTipoPago() != null && TIPO_CREDITO.equalsIgnoreCase(venta.getTipoPago().getNombre())) return true;
        return venta.getEstado() == Ventas.Estado.PENDIENTE;
    }

    @Transactional
    public Ventas crearVentaDesdePedido(PedidoCatalogo pedido) {
        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            throw new RuntimeException("El pedido no tiene productos.");
        }

        Cliente cliente = clienteService.resolverClienteDesdePedido(
                pedido.getDocumento(),
                pedido.getNombreCliente(),
                pedido.getTelefono(),
                pedido.getId());

        TipoComprobante comprobante = tipoComprobanteRepository.findAll().stream()
                .filter(c -> "NOTA DE VENTA".equalsIgnoreCase(c.getNombre()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Tipo de comprobante NOTA DE VENTA no configurado."));

        TipoPago tipoPago = resolverTipoPagoPedido(pedido);

        Usuario vendedor = permissionService.getUsuarioActual();
        if (vendedor == null) {
            vendedor = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No hay vendedor disponible."));
        }

        int siguienteCorrelativo = comprobante.getCorrelativoActual() + 1;
        String numeroDocumento = comprobante.getSerie() + "-" + String.format("%08d", siguienteCorrelativo);

        Ventas venta = new Ventas();
        venta.setNumeroDocumento(numeroDocumento);
        venta.setCliente(cliente);
        venta.setVendedor(vendedor);
        venta.setTipoComprobante(comprobante);
        venta.setTipoPago(tipoPago);
        venta.setOrigenVenta(Ventas.OrigenVenta.WEB);
        venta.setEstado(Ventas.Estado.PAGADO);
        venta.setPagoInicial(pedido.getTotal());
        venta.setDeuda(BigDecimal.ZERO);
        if (pedido.getMetodoPago() != null) {
            if (pedido.getMetodoPago() == PedidoCatalogo.MetodoPago.YAPE) {
                venta.setMontoYape(pedido.getTotal());
            } else if (pedido.getMetodoPago() == PedidoCatalogo.MetodoPago.PLIN) {
                venta.setMontoPlin(pedido.getTotal());
            }
        }
        if (pedido.getCodigoValidacionPago() != null && !pedido.getCodigoValidacionPago().isBlank()) {
            venta.setCodigoVerificacionPago(pedido.getCodigoValidacionPago().trim().toUpperCase());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PedidoCatalogoDetalle detalle : pedido.getDetalles()) {
            ProductoPresentacion presentacion = presentacionService.obtenerParaVenta(
                    detalle.getPresentacionId(), detalle.getProductoId());
            Producto producto = presentacion.getProducto();

            if (presentacion.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre()
                        + " (" + presentacion.getNombre() + ", disponible: " + presentacion.getStock() + ").");
            }

            DetalleVenta dv = new DetalleVenta();
            dv.setProducto(producto);
            dv.setPresentacion(presentacion);
            dv.setCantidad(detalle.getCantidad());
            dv.setPrecioUnitario(detalle.getPrecioUnitario());
            dv.setSubtotal(detalle.getSubtotal());
            venta.addDetalle(dv);

            int stockAnterior = presentacion.getStock() != null ? presentacion.getStock() : 0;
            presentacionService.descontarStock(presentacion, detalle.getCantidad());
            int stockNuevo = stockAnterior - detalle.getCantidad();
            movimientoInventarioService.registrarTraza(
                    presentacion,
                    MovimientoInventario.TipoMovimiento.VENTA_WEB,
                    detalle.getCantidad(),
                    stockAnterior,
                    stockNuevo,
                    venta.getNumeroDocumento(),
                    venta.getId(),
                    "Venta Web — Pedido #" + pedido.getId()
            );
            total = total.add(detalle.getSubtotal());
        }

        BigDecimal totalPedido = pedido.getTotal() != null ? pedido.getTotal() : total;
        venta.setTotal(totalPedido);
        venta.setPagoInicial(totalPedido);
        comprobante.setCorrelativoActual(siguienteCorrelativo);
        tipoComprobanteRepository.save(comprobante);
        return ventaRepository.save(venta);
    }

    private TipoPago resolverTipoPagoPedido(PedidoCatalogo pedido) {
        if (pedido.getMetodoPago() == PedidoCatalogo.MetodoPago.YAPE) {
            return tipoPagoRepository.findByNombreIgnoreCase("Yape")
                    .orElseThrow(() -> new RuntimeException("Tipo de pago Yape no configurado."));
        }
        if (pedido.getMetodoPago() == PedidoCatalogo.MetodoPago.PLIN) {
            return tipoPagoRepository.findByNombreIgnoreCase("Plin")
                    .orElseThrow(() -> new RuntimeException("Tipo de pago Plin no configurado."));
        }
        return tipoPagoRepository.findByNombreIgnoreCase(TIPO_CONTADO)
                .orElseThrow(() -> new RuntimeException("Tipo de pago Contado no configurado."));
    }

}
