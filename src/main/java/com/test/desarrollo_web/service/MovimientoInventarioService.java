package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.*;
import com.test.desarrollo_web.Repository.MovimientoInventarioRepository;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoInventarioService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MovimientoInventarioRepository movimientoRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final ProductoPresentacionService presentacionService;
    private final PermissionService permissionService;

    public MovimientoInventarioService(MovimientoInventarioRepository movimientoRepository,
                                       ProductoPresentacionRepository presentacionRepository,
                                       ProductoPresentacionService presentacionService,
                                       PermissionService permissionService) {
        this.movimientoRepository = movimientoRepository;
        this.presentacionRepository = presentacionRepository;
        this.presentacionService = presentacionService;
        this.permissionService = permissionService;
    }

    @Transactional
    public MovimientoInventario registrarIngreso(ProductoPresentacion presentacion,
                                                 int cantidad,
                                                 MovimientoInventario.TipoMovimiento tipo,
                                                 String referencia,
                                                 String proveedorNombre,
                                                 Compra compra,
                                                 Long ventaId,
                                                 String observacion) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0.");
        }
        int anterior = stockActual(presentacion);
        int nuevo = anterior + cantidad;
        presentacion.setStock(nuevo);
        presentacionRepository.save(presentacion);
        presentacionService.sincronizarAgregadosProducto(presentacion.getProducto().getId());
        return guardarMovimiento(presentacion, tipo, cantidad, anterior, nuevo, referencia,
                proveedorNombre, compra, ventaId, null, observacion);
    }

    @Transactional
    public MovimientoInventario registrarSalida(ProductoPresentacion presentacion,
                                                int cantidad,
                                                MovimientoInventario.TipoMovimiento tipo,
                                                String referencia,
                                                Long ventaId,
                                                String motivo,
                                                String observacion) {
        if (cantidad <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a 0.");
        }
        int anterior = stockActual(presentacion);
        if (anterior < cantidad) {
            throw new RuntimeException("Stock insuficiente para \"" + presentacion.getNombre() + "\".");
        }
        int nuevo = anterior - cantidad;
        presentacion.setStock(nuevo);
        presentacionRepository.save(presentacion);
        presentacionService.sincronizarAgregadosProducto(presentacion.getProducto().getId());
        return guardarMovimiento(presentacion, tipo, cantidad, anterior, nuevo, referencia,
                null, null, ventaId, motivo, observacion);
    }

    @Transactional
    public MovimientoInventario registrarAjuste(ProductoPresentacion presentacion,
                                                int stockNuevo,
                                                String motivo,
                                                String observacion) {
        if (stockNuevo < 0) {
            throw new RuntimeException("El stock no puede ser negativo.");
        }
        int anterior = stockActual(presentacion);
        if (anterior == stockNuevo) {
            throw new RuntimeException("El stock nuevo es igual al stock actual.");
        }
        int diferencia = Math.abs(stockNuevo - anterior);
        presentacion.setStock(stockNuevo);
        presentacionRepository.save(presentacion);
        presentacionService.sincronizarAgregadosProducto(presentacion.getProducto().getId());
        return guardarMovimiento(presentacion, MovimientoInventario.TipoMovimiento.AJUSTE,
                diferencia, anterior, stockNuevo, "AJUSTE", null, null, null, motivo, observacion);
    }

    @Transactional
    public void registrarTraza(ProductoPresentacion presentacion,
                               MovimientoInventario.TipoMovimiento tipo,
                               int cantidad,
                               int stockAnterior,
                               int stockNuevo,
                               String referencia,
                               Long ventaId,
                               String observacion) {
        guardarMovimiento(presentacion, tipo, cantidad, stockAnterior, stockNuevo, referencia,
                null, null, ventaId, null, observacion);
    }

    @Transactional
    public void registrarTraza(ProductoPresentacion presentacion,
                               MovimientoInventario.TipoMovimiento tipo,
                               int cantidad,
                               int stockAnterior,
                               int stockNuevo,
                               String referencia,
                               Long ventaId,
                               String motivo,
                               String observacion) {
        guardarMovimiento(presentacion, tipo, cantidad, stockAnterior, stockNuevo, referencia,
                null, null, ventaId, motivo, observacion);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPorPresentacion(Integer presentacionId) {
        return movimientoRepository.findByPresentacionIdOrderByFechaDesc(presentacionId).stream()
                .map(this::mapearMovimiento)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarTodos() {
        return movimientoRepository.findAllConDetalleOrderByFechaDesc().stream()
                .map(this::mapearMovimientoCompleto)
                .toList();
    }

    private MovimientoInventario guardarMovimiento(ProductoPresentacion presentacion,
                                                   MovimientoInventario.TipoMovimiento tipo,
                                                   int cantidad,
                                                   int anterior,
                                                   int nuevo,
                                                   String referencia,
                                                   String proveedorNombre,
                                                   Compra compra,
                                                   Long ventaId,
                                                   String motivo,
                                                   String observacion) {
        MovimientoInventario mov = new MovimientoInventario();
        mov.setPresentacion(presentacion);
        mov.setProducto(presentacion.getProducto());
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setStockAnterior(anterior);
        mov.setStockNuevo(nuevo);
        mov.setReferencia(referencia);
        mov.setProveedorNombre(proveedorNombre);
        mov.setCompra(compra);
        mov.setVentaId(ventaId);
        mov.setMotivo(motivo);
        mov.setUsuario(permissionService.getUsuarioActual());
        mov.setFecha(LocalDateTime.now());
        mov.setObservacion(observacion);
        return movimientoRepository.save(mov);
    }

    private Map<String, Object> mapearMovimiento(MovimientoInventario m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", m.getId());
        if (m.getFecha() != null) {
            map.put("fecha", m.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            map.put("hora", m.getFecha().format(DateTimeFormatter.ofPattern("HH:mm")));
            map.put("fechaHora", m.getFecha().format(FMT));
        } else {
            map.put("fecha", "—");
            map.put("hora", "—");
            map.put("fechaHora", "—");
        }
        map.put("tipo", etiquetaTipo(m.getTipo()));
        map.put("tipoCodigo", m.getTipo() != null ? m.getTipo().name() : "");
        map.put("cantidad", m.getCantidad());
        map.put("entrada", calcularEntrada(m));
        map.put("salida", calcularSalida(m));
        map.put("stockAnterior", m.getStockAnterior());
        map.put("stockNuevo", m.getStockNuevo());
        map.put("stockActual", m.getStockNuevo());
        map.put("documento", m.getReferencia() != null ? m.getReferencia() : "—");
        map.put("referencia", m.getReferencia() != null ? m.getReferencia() : "—");
        map.put("proveedor", m.getProveedorNombre() != null ? m.getProveedorNombre() : "—");
        map.put("motivo", m.getMotivo() != null ? m.getMotivo() : "—");
        map.put("usuario", m.getUsuario() != null ? m.getUsuario().getNombre() : "Sistema");
        map.put("observacion", m.getObservacion() != null ? m.getObservacion() : "—");
        return map;
    }

    private Map<String, Object> mapearMovimientoCompleto(MovimientoInventario m) {
        Map<String, Object> map = mapearMovimiento(m);
        Producto producto = m.getProducto();
        ProductoPresentacion pp = m.getPresentacion();
        map.put("producto", producto != null ? producto.getNombre() : "—");
        map.put("presentacion", pp != null ? pp.getNombre() : "—");
        map.put("presentacionId", pp != null ? pp.getId() : null);
        map.put("categoria", producto != null && producto.getCategoria() != null
                ? producto.getCategoria().getNombre() : "—");
        return map;
    }

    private String etiquetaTipo(MovimientoInventario.TipoMovimiento tipo) {
        if (tipo == null) {
            return "—";
        }
        return switch (tipo) {
            case INGRESO_COMPRA -> "Ingreso por Compra";
            case INGRESO_MANUAL -> "Ingreso Manual";
            case SALIDA -> "Salida";
            case AJUSTE -> "Ajuste";
            case SALIDA_VENTA, VENTA_POS -> "Venta POS";
            case VENTA_WEB -> "Venta Web";
            case INGRESO_DEVOLUCION -> "Ingreso por Devolución";
        };
    }

    private Integer calcularEntrada(MovimientoInventario m) {
        if (m.getTipo() == null) {
            return 0;
        }
        return switch (m.getTipo()) {
            case INGRESO_COMPRA, INGRESO_MANUAL, INGRESO_DEVOLUCION -> m.getCantidad();
            case AJUSTE -> m.getStockNuevo() > m.getStockAnterior() ? m.getCantidad() : 0;
            default -> 0;
        };
    }

    private Integer calcularSalida(MovimientoInventario m) {
        if (m.getTipo() == null) {
            return 0;
        }
        return switch (m.getTipo()) {
            case SALIDA, SALIDA_VENTA, VENTA_POS, VENTA_WEB -> m.getCantidad();
            case AJUSTE -> m.getStockNuevo() < m.getStockAnterior() ? m.getCantidad() : 0;
            default -> 0;
        };
    }

    private int stockActual(ProductoPresentacion presentacion) {
        return presentacion.getStock() != null ? presentacion.getStock() : 0;
    }
}
