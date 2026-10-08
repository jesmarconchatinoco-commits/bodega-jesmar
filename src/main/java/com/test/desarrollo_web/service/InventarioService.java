package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.MovimientoInventario;
import com.test.desarrollo_web.Models.Producto;
import com.test.desarrollo_web.Models.ProductoPresentacion;
import com.test.desarrollo_web.dto.MovimientoInventarioRequestDto;
import com.test.desarrollo_web.Repository.CategoriaRepository;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import com.test.desarrollo_web.util.ImagenRutas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class InventarioService {

    private final ProductoPresentacionRepository presentacionRepository;
    private final ProductoRepository productoRepository;
    private final ProductoPresentacionService presentacionService;
    private final MovimientoInventarioService movimientoService;
    private final CompraService compraService;
    private final CategoriaRepository categoriaRepository;
    private final PermissionService permissionService;

    public InventarioService(ProductoPresentacionRepository presentacionRepository,
                             ProductoRepository productoRepository,
                             ProductoPresentacionService presentacionService,
                             MovimientoInventarioService movimientoService,
                             CompraService compraService,
                             CategoriaRepository categoriaRepository,
                             PermissionService permissionService) {
        this.presentacionRepository = presentacionRepository;
        this.productoRepository = productoRepository;
        this.presentacionService = presentacionService;
        this.movimientoService = movimientoService;
        this.compraService = compraService;
        this.categoriaRepository = categoriaRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public void validarAcceso() {
        if (!permissionService.puedeAccederInventario()) {
            throw new RuntimeException("No tiene permiso para acceder a Gestión de Inventario.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumen() {
        List<ProductoPresentacion> presentaciones = presentacionRepository.findActivasParaVenta();
        Set<Integer> productos = new HashSet<>();
        int stockBajo = 0;
        int sinStock = 0;
        BigDecimal valorTotal = BigDecimal.ZERO;

        for (ProductoPresentacion pp : presentaciones) {
            productos.add(pp.getProducto().getId());
            int stock = stock(pp);
            int min = minimo(pp);
            if (stock == 0) {
                sinStock++;
            } else if (stock <= min) {
                stockBajo++;
            }
            BigDecimal precioCompra = pp.getPrecioCompra() != null ? pp.getPrecioCompra() : BigDecimal.ZERO;
            valorTotal = valorTotal.add(precioCompra.multiply(BigDecimal.valueOf(stock)));
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("totalProductos", productos.size());
        res.put("totalPresentaciones", presentaciones.size());
        res.put("stockBajo", stockBajo);
        res.put("sinStock", sinStock);
        res.put("valorInventario", formatoMoneda(valorTotal));
        return res;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarInventario(String busqueda, Integer categoriaId, String estadoStock) {
        String filtro = busqueda != null ? busqueda.trim().toLowerCase() : "";
        String estado = estadoStock != null ? estadoStock.trim().toLowerCase() : "";

        return presentacionRepository.findActivasParaVenta().stream()
                .filter(pp -> categoriaId == null
                        || (pp.getProducto().getCategoria() != null
                        && categoriaId.equals(pp.getProducto().getCategoria().getId())))
                .map(this::mapearFilaInventario)
                .filter(item -> filtro.isEmpty() || coincideBusqueda(item, filtro))
                .filter(item -> estado.isEmpty() || estado.equals(item.get("estadoStock")))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPresentacionesParaCompra() {
        return presentacionRepository.findActivasParaVenta().stream()
                .map(pp -> {
                    Producto p = pp.getProducto();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", pp.getId());
                    m.put("producto", p.getNombre());
                    m.put("presentacion", pp.getNombre());
                    m.put("label", p.getNombre() + " — " + pp.getNombre());
                    m.put("precioCompra", pp.getPrecioCompra());
                    m.put("precioVenta", pp.getPrecio());
                    m.put("stock", stock(pp));
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCategorias() {
        return categoriaRepository.findAll().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("nombre", c.getNombre());
                    return m;
                }).toList();
    }

    @Transactional
    public Map<String, Object> registrarIngresoManual(MovimientoInventarioRequestDto dto) {
        validarMovimiento(dto);
        ProductoPresentacion pp = presentacionService.obtenerPresentacion(dto.getPresentacionId());
        movimientoService.registrarIngreso(pp, dto.getCantidad(),
                MovimientoInventario.TipoMovimiento.INGRESO_MANUAL,
                "ING-MANUAL", null, null, null, dto.getObservacion());
        return respuestaExito("Ingreso manual registrado correctamente.");
    }

    @Transactional
    public Map<String, Object> registrarSalida(MovimientoInventarioRequestDto dto) {
        validarMovimiento(dto);
        if (dto.getMotivo() == null || dto.getMotivo().isBlank()) {
            throw new RuntimeException("Seleccione el motivo de la salida.");
        }
        ProductoPresentacion pp = presentacionService.obtenerPresentacion(dto.getPresentacionId());
        String obs = combinarObservacionSalida(dto.getMotivo(), dto.getObservacion());
        movimientoService.registrarSalida(pp, dto.getCantidad(),
                MovimientoInventario.TipoMovimiento.SALIDA,
                "SALIDA", null, dto.getMotivo().trim(), obs);
        return respuestaExito("Salida registrada correctamente.");
    }

    @Transactional
    public Map<String, Object> registrarAjuste(MovimientoInventarioRequestDto dto) {
        if (dto == null || dto.getPresentacionId() == null || dto.getStockNuevo() == null) {
            throw new RuntimeException("Datos de ajuste incompletos.");
        }
        if (dto.getMotivo() == null || dto.getMotivo().isBlank()) {
            throw new RuntimeException("El motivo del ajuste es obligatorio.");
        }
        ProductoPresentacion pp = presentacionService.obtenerPresentacion(dto.getPresentacionId());
        movimientoService.registrarAjuste(pp, dto.getStockNuevo(), dto.getMotivo().trim(), dto.getObservacion());
        return respuestaExito("Ajuste de inventario registrado correctamente.");
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProveedores() {
        return compraService.listarProveedores();
    }

    @Transactional
    public Map<String, Object> registrarProveedor(com.test.desarrollo_web.dto.ProveedorRequestDto dto) {
        return compraService.registrarProveedor(dto);
    }

    @Transactional
    public Map<String, Object> registrarCompra(com.test.desarrollo_web.dto.CompraRequestDto dto) {
        return compraService.registrarCompra(dto);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> kardexPresentacion(Integer presentacionId) {
        return movimientoService.listarPorPresentacion(presentacionId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> historialMovimientos() {
        return movimientoService.listarTodos();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProductosGestion() {
        return productoRepository.findActivosWithDetalles().stream()
                .map(this::mapearProductoGestion)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerStockPresentacion(Integer presentacionId) {
        ProductoPresentacion pp = presentacionService.obtenerPresentacion(presentacionId);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("presentacionId", pp.getId());
        res.put("producto", pp.getProducto().getNombre());
        res.put("presentacion", pp.getNombre());
        res.put("stockActual", stock(pp));
        return res;
    }

    private Map<String, Object> mapearProductoGestion(Producto producto) {
        List<ProductoPresentacion> presentaciones = presentacionRepository
                .findByProducto_IdOrderByOrdenAscIdAsc(producto.getId()).stream()
                .filter(pp -> pp.getEstado() == ProductoPresentacion.Estado.ACTIVO)
                .toList();
        int stockTotal = presentaciones.stream()
                .mapToInt(pp -> pp.getStock() != null ? pp.getStock() : 0)
                .sum();
        String imagen = null;
        if (producto.getImagen() != null && producto.getImagen().getRuta() != null) {
            imagen = ImagenRutas.toPublicUrl(producto.getImagen().getRuta());
        } else if (!presentaciones.isEmpty()) {
            imagen = presentacionService.urlImagenPrincipal(presentaciones.get(0));
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("productoId", producto.getId());
        item.put("imagen", imagen);
        item.put("producto", producto.getNombre());
        item.put("categoria", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "—");
        item.put("cantidadPresentaciones", presentaciones.size());
        item.put("stockTotal", stockTotal);
        item.put("estado", producto.getEstado() != null ? producto.getEstado().name() : "ACTIVO");
        return item;
    }

    private String combinarObservacionSalida(String motivo, String observacion) {
        if (observacion == null || observacion.isBlank()) {
            return motivo;
        }
        return motivo + " — " + observacion.trim();
    }

    private Map<String, Object> mapearFilaInventario(ProductoPresentacion pp) {
        Producto producto = pp.getProducto();
        int stock = stock(pp);
        int min = minimo(pp);
        String estado = calcularEstadoStock(stock, min);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", pp.getId());
        item.put("presentacionId", pp.getId());
        item.put("productoId", producto.getId());
        item.put("imagen", presentacionService.urlImagenPrincipal(pp));
        item.put("producto", producto.getNombre());
        item.put("categoria", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "—");
        item.put("presentacion", pp.getNombre());
        item.put("codigo", pp.getCodigoBarras() != null ? pp.getCodigoBarras() : "—");
        item.put("stock", stock);
        item.put("stockMinimo", min);
        item.put("precioCompra", formatoMoneda(pp.getPrecioCompra()));
        item.put("precioVenta", formatoMoneda(pp.getPrecio()));
        item.put("estadoStock", estado);
        item.put("estadoLabel", etiquetaEstado(estado));
        return item;
    }

    private boolean coincideBusqueda(Map<String, Object> item, String filtro) {
        return contiene(item.get("producto"), filtro)
                || contiene(item.get("presentacion"), filtro)
                || contiene(item.get("categoria"), filtro)
                || contiene(item.get("codigo"), filtro);
    }

    private boolean contiene(Object val, String filtro) {
        return val != null && val.toString().toLowerCase().contains(filtro);
    }

    private String calcularEstadoStock(int stock, int min) {
        if (stock == 0) {
            return "agotado";
        }
        if (stock <= min) {
            return "bajo";
        }
        return "normal";
    }

    private String etiquetaEstado(String estado) {
        return switch (estado) {
            case "agotado" -> "Sin stock";
            case "bajo" -> "Stock bajo";
            default -> "Normal";
        };
    }

    private int stock(ProductoPresentacion pp) {
        return pp.getStock() != null ? pp.getStock() : 0;
    }

    private int minimo(ProductoPresentacion pp) {
        return pp.getStockMinimo() != null ? pp.getStockMinimo() : 0;
    }

    private void validarMovimiento(MovimientoInventarioRequestDto dto) {
        if (dto == null || dto.getPresentacionId() == null) {
            throw new RuntimeException("Seleccione una presentación.");
        }
        if (dto.getCantidad() == null || dto.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0.");
        }
    }

    private Map<String, Object> respuestaExito(String mensaje) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", mensaje);
        return res;
    }

    private String formatoMoneda(BigDecimal val) {
        BigDecimal n = val != null ? val : BigDecimal.ZERO;
        return "S/ " + n.setScale(2, RoundingMode.HALF_UP);
    }
}
