package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.*;
import com.test.desarrollo_web.Repository.CompraRepository;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import com.test.desarrollo_web.Repository.ProveedorRepository;
import com.test.desarrollo_web.dto.CompraRequestDto;
import com.test.desarrollo_web.dto.ProveedorRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final ProductoPresentacionService presentacionService;
    private final MovimientoInventarioService movimientoService;
    private final PermissionService permissionService;

    public CompraService(CompraRepository compraRepository,
                         ProveedorRepository proveedorRepository,
                         ProductoPresentacionRepository presentacionRepository,
                         ProductoPresentacionService presentacionService,
                         MovimientoInventarioService movimientoService,
                         PermissionService permissionService) {
        this.compraRepository = compraRepository;
        this.proveedorRepository = proveedorRepository;
        this.presentacionRepository = presentacionRepository;
        this.presentacionService = presentacionService;
        this.movimientoService = movimientoService;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProveedores() {
        return proveedorRepository.findByEstadoOrderByNombreAsc(Proveedor.Estado.ACTIVO).stream()
                .map(this::mapearProveedor)
                .toList();
    }

    @Transactional
    public Map<String, Object> registrarProveedor(ProveedorRequestDto dto) {
        if (dto == null || dto.getNombre() == null || dto.getNombre().isBlank()) {
            throw new RuntimeException("El nombre del proveedor es obligatorio.");
        }
        Proveedor proveedor = new Proveedor();
        proveedor.setNombre(dto.getNombre().trim());
        proveedor.setDocumento(dto.getDocumento());
        proveedor.setTelefono(dto.getTelefono());
        proveedor.setEmail(dto.getEmail());
        proveedor.setEstado(Proveedor.Estado.ACTIVO);
        proveedorRepository.save(proveedor);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("proveedor", mapearProveedor(proveedor));
        return res;
    }

    @Transactional
    public Map<String, Object> registrarCompra(CompraRequestDto dto) {
        validarCompra(dto);

        Proveedor proveedor = proveedorRepository.findById(dto.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado."));

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setTipoComprobante(parseTipoComprobante(dto.getTipoComprobante()));
        compra.setSerie(dto.getSerie() != null ? dto.getSerie().trim() : "");
        compra.setNumero(dto.getNumero().trim());
        compra.setFechaEmision(dto.getFechaEmision());
        compra.setFechaRegistro(LocalDateTime.now());
        compra.setUsuario(permissionService.getUsuarioActual());
        compra.setObservaciones(dto.getObservaciones());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<DetalleCompra> detalles = new ArrayList<>();
        String referencia = compra.getSerie() + "-" + compra.getNumero();

        for (CompraRequestDto.DetalleCompraLineaDto linea : dto.getDetalles()) {
            ProductoPresentacion presentacion = presentacionService.obtenerPresentacion(linea.getPresentacionId());
            int cantidad = linea.getCantidad();
            BigDecimal precio = linea.getPrecioCompra().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineSubtotal = precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);

            DetalleCompra detalle = new DetalleCompra();
            detalle.setCompra(compra);
            detalle.setPresentacion(presentacion);
            detalle.setCantidad(cantidad);
            detalle.setPrecioCompra(precio);
            detalle.setSubtotal(lineSubtotal);
            detalles.add(detalle);
            subtotal = subtotal.add(lineSubtotal);
        }

        BigDecimal igv = dto.getIgv() != null
                ? dto.getIgv().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        compra.setSubtotal(subtotal);
        compra.setIgv(igv);
        compra.setTotal(subtotal.add(igv));
        compra.setDetalles(detalles);
        compraRepository.save(compra);

        for (DetalleCompra detalle : detalles) {
            ProductoPresentacion presentacion = detalle.getPresentacion();
            presentacion.setPrecioCompra(detalle.getPrecioCompra());
            presentacionRepository.save(presentacion);
            movimientoService.registrarIngreso(
                    presentacion,
                    detalle.getCantidad(),
                    MovimientoInventario.TipoMovimiento.INGRESO_COMPRA,
                    referencia,
                    proveedor.getNombre(),
                    compra,
                    null,
                    "Compra " + compra.getTipoComprobante().name() + " " + referencia
            );
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("compraId", compra.getId());
        res.put("message", "Compra registrada y stock actualizado correctamente.");
        return res;
    }

    private void validarCompra(CompraRequestDto dto) {
        if (dto == null) {
            throw new RuntimeException("Datos de compra inválidos.");
        }
        if (dto.getProveedorId() == null) {
            throw new RuntimeException("Seleccione un proveedor.");
        }
        if (dto.getNumero() == null || dto.getNumero().isBlank()) {
            throw new RuntimeException("Ingrese el número de comprobante.");
        }
        if (dto.getFechaEmision() == null) {
            throw new RuntimeException("Ingrese la fecha de emisión.");
        }
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new RuntimeException("Agregue al menos un producto a la compra.");
        }
        for (CompraRequestDto.DetalleCompraLineaDto linea : dto.getDetalles()) {
            if (linea.getPresentacionId() == null) {
                throw new RuntimeException("Presentación inválida en el detalle.");
            }
            if (linea.getCantidad() == null || linea.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad debe ser mayor a 0.");
            }
            if (linea.getPrecioCompra() == null || linea.getPrecioCompra().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("El precio de compra no es válido.");
            }
        }
    }

    private Compra.TipoComprobante parseTipoComprobante(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return Compra.TipoComprobante.BOLETA;
        }
        return switch (tipo.trim().toUpperCase()) {
            case "FACTURA" -> Compra.TipoComprobante.FACTURA;
            case "GUIA" -> Compra.TipoComprobante.GUIA;
            case "NOTA_COMPRA", "NOTA DE COMPRA" -> Compra.TipoComprobante.NOTA_COMPRA;
            case "OTROS" -> Compra.TipoComprobante.OTROS;
            default -> Compra.TipoComprobante.BOLETA;
        };
    }

    private Map<String, Object> mapearProveedor(Proveedor p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("nombre", p.getNombre());
        m.put("documento", p.getDocumento());
        m.put("telefono", p.getTelefono());
        m.put("email", p.getEmail());
        return m;
    }
}
