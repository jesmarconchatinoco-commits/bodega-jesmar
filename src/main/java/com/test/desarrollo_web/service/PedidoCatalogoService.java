package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.PedidoCatalogo;
import com.test.desarrollo_web.Models.Ventas;
import com.test.desarrollo_web.Repository.PedidoCatalogoRepository;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.util.ImagenRutas;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PedidoCatalogoService {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PedidoCatalogoRepository pedidoCatalogoRepository;
    private final VentaService ventaService;
    private final HorarioAtencionService horarioAtencionService;
    private final FileStorageService fileStorageService;

    public PedidoCatalogoService(PedidoCatalogoRepository pedidoCatalogoRepository,
                                 VentaService ventaService,
                                 HorarioAtencionService horarioAtencionService,
                                 FileStorageService fileStorageService) {
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
        this.ventaService = ventaService;
        this.horarioAtencionService = horarioAtencionService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<PedidoCatalogo> listar(String estadoFiltro) {
        if (estadoFiltro == null || estadoFiltro.isBlank() || "TODOS".equalsIgnoreCase(estadoFiltro)) {
            return pedidoCatalogoRepository.findAllWithComprobanteOrderByFechaDesc();
        }
        try {
            PedidoCatalogo.Estado estado = PedidoCatalogo.Estado.valueOf(estadoFiltro.toUpperCase());
            return pedidoCatalogoRepository.findByEstadoWithComprobanteOrderByFechaDesc(estado);
        } catch (IllegalArgumentException e) {
            return pedidoCatalogoRepository.findAllWithComprobanteOrderByFechaDesc();
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalle(Long id) {
        PedidoCatalogo pedido = pedidoCatalogoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado."));
        return mapearPedido(pedido, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerVentaPedido(Long id) {
        PedidoCatalogo pedido = pedidoCatalogoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado."));
        if (pedido.getVentaId() == null) {
            throw new RuntimeException("Este pedido aún no tiene una venta registrada.");
        }
        Map<String, Object> venta = ventaService.obtenerDetalleVenta(pedido.getVentaId());
        venta.put("pedidoId", pedido.getId());
        return venta;
    }

    @Transactional
    public Map<String, Object> atenderPedido(Long id) {
        PedidoCatalogo pedido = pedidoCatalogoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado."));

        if (pedido.getEstado() != PedidoCatalogo.Estado.PENDIENTE) {
            throw new RuntimeException("Solo se pueden atender pedidos en estado PENDIENTE.");
        }

        Ventas venta = ventaService.crearVentaDesdePedido(pedido);

        pedido.setEstado(PedidoCatalogo.Estado.ATENDIDO);
        pedido.setVentaId(venta.getId());
        pedidoCatalogoRepository.save(pedido);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("ventaId", venta.getId());
        res.put("numeroDocumento", venta.getNumeroDocumento());
        res.put("message", "Pedido #" + pedido.getId() + " validado. Venta " + venta.getNumeroDocumento()
                + " registrada en Ventas, stock descontado y movimientos generados.");
        res.put("pedido", mapearPedido(pedido, true));
        return res;
    }

    @Transactional
    public Map<String, Object> actualizarComprobante(Long id, MultipartFile comprobante) {
        PedidoCatalogo pedido = pedidoCatalogoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado."));

        if (pedido.getEstado() != PedidoCatalogo.Estado.PENDIENTE) {
            throw new RuntimeException("Solo se puede editar el comprobante de pedidos pendientes.");
        }
        if (comprobante == null || comprobante.isEmpty()) {
            throw new RuntimeException("Seleccione una imagen para el comprobante.");
        }

        String contentType = comprobante.getContentType() != null ? comprobante.getContentType().toLowerCase() : "";
        if (!contentType.equals("image/jpeg") && !contentType.equals("image/jpg") && !contentType.equals("image/png")) {
            throw new RuntimeException("Solo se permiten imágenes JPG, JPEG o PNG.");
        }

        if (pedido.getComprobantePago() != null && pedido.getComprobantePago().getRuta() != null) {
            fileStorageService.deleteFile(pedido.getComprobantePago().getRuta());
        }

        pedido.setComprobantePago(fileStorageService.saveFile(comprobante, ImageStorageCategory.PEDIDOS));
        pedidoCatalogoRepository.save(pedido);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Comprobante actualizado correctamente.");
        res.put("comprobanteUrl", ImagenRutas.toPublicUrl(pedido.getComprobantePago().getRuta()));
        return res;
    }

    @Transactional
    public Map<String, Object> cancelarPedido(Long id) {
        PedidoCatalogo pedido = pedidoCatalogoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado."));

        if (pedido.getEstado() != PedidoCatalogo.Estado.PENDIENTE) {
            throw new RuntimeException("Solo se pueden cancelar pedidos en estado PENDIENTE.");
        }

        pedido.setEstado(PedidoCatalogo.Estado.CANCELADO);
        pedidoCatalogoRepository.save(pedido);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", "Pedido #" + pedido.getId() + " cancelado.");
        res.put("pedido", mapearPedido(pedido, true));
        return res;
    }

    private Map<String, Object> mapearPedido(PedidoCatalogo pedido, boolean incluirDetalles) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", pedido.getId());
        mapa.put("nombreCliente", pedido.getNombreCliente());
        mapa.put("telefono", pedido.getTelefono());
        mapa.put("documento", pedido.getDocumento());
        mapa.put("observaciones", pedido.getObservaciones());
        mapa.put("formaEntrega", pedido.getFormaEntrega() != null ? pedido.getFormaEntrega().name() : null);
        mapa.put("direccion", pedido.getDireccion());
        mapa.put("referencia", pedido.getReferencia());
        mapa.put("distrito", pedido.getDistrito());
        mapa.put("metodoPago", pedido.getMetodoPago() != null ? pedido.getMetodoPago().name() : null);
        mapa.put("subtotal", pedido.getSubtotal());
        mapa.put("costoEnvio", pedido.getCostoEnvio());
        mapa.put("comprobanteUrl", pedido.getComprobantePago() != null
                ? ImagenRutas.toPublicUrl(pedido.getComprobantePago().getRuta()) : null);
        mapa.put("codigoValidacionPago", pedido.getCodigoValidacionPago());
        mapa.put("total", pedido.getTotal());
        mapa.put("fecha", pedido.getFecha() != null ? pedido.getFecha().format(FMT_FECHA) : "—");
        mapa.put("estado", pedido.getEstado() != null ? pedido.getEstado().name() : "PENDIENTE");
        mapa.put("ventaId", pedido.getVentaId());
        if (pedido.getHorarioRecojo() != null) {
            mapa.put("horarioRecojoId", pedido.getHorarioRecojo().getId());
            mapa.put("horarioRecojoLabel", horarioAtencionService.formatearRecojo(pedido.getHorarioRecojo()));
        }

        if (incluirDetalles) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (var detalle : pedido.getDetalles()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("productoId", detalle.getProductoId());
                item.put("presentacionId", detalle.getPresentacionId());
                String nombreLinea = detalle.getNombreProducto();
                if (detalle.getNombrePresentacion() != null && !detalle.getNombrePresentacion().isBlank()) {
                    nombreLinea += " (" + detalle.getNombrePresentacion() + ")";
                }
                item.put("nombreProducto", nombreLinea);
                item.put("nombrePresentacion", detalle.getNombrePresentacion());
                item.put("cantidad", detalle.getCantidad());
                item.put("precioUnitario", detalle.getPrecioUnitario());
                item.put("subtotal", detalle.getSubtotal());
                items.add(item);
            }
            mapa.put("detalles", items);
            mapa.put("cantidadItems", items.size());
        }
        return mapa;
    }
}
