package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Producto;
import com.test.desarrollo_web.Models.ProductoPresentacion;
import com.test.desarrollo_web.Repository.ImagenRepository;
import com.test.desarrollo_web.Repository.PresentacionImagenRepository;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import com.test.desarrollo_web.dto.ProductoPresentacionRequest;
import com.test.desarrollo_web.util.ImagenRutas;
import com.test.desarrollo_web.util.PresentacionMedidaUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductoPresentacionService {

    private final ProductoPresentacionRepository presentacionRepository;
    private final ProductoRepository productoRepository;
    private final PresentacionImagenRepository presentacionImagenRepository;
    private final FileStorageService fileStorageService;
    private final ImagenRepository imagenRepository;
    private final ProductoNodeSyncService productoNodeSyncService;

    public ProductoPresentacionService(ProductoPresentacionRepository presentacionRepository,
                                       ProductoRepository productoRepository,
                                       PresentacionImagenRepository presentacionImagenRepository,
                                       FileStorageService fileStorageService,
                                       ImagenRepository imagenRepository,
                                       ProductoNodeSyncService productoNodeSyncService) {
        this.presentacionRepository = presentacionRepository;
        this.productoRepository = productoRepository;
        this.presentacionImagenRepository = presentacionImagenRepository;
        this.fileStorageService = fileStorageService;
        this.imagenRepository = imagenRepository;
        this.productoNodeSyncService = productoNodeSyncService;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarPorProducto(Integer productoId) {
        productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado."));
        return presentacionRepository.findByProducto_IdOrderByOrdenAscIdAsc(productoId).stream()
                .map(this::mapearPresentacion)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Integer, Map<String, Object>> resumenesPorProductos(List<Integer> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Map<String, Object>> resumenes = new LinkedHashMap<>();
        for (Integer productoId : productoIds) {
            resumenes.put(productoId, resumenProducto(productoId));
        }
        return resumenes;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> resumenProducto(Integer productoId) {
        List<ProductoPresentacion> presentaciones =
                presentacionRepository.findByProducto_IdOrderByOrdenAscIdAsc(productoId);
        int stockTotal = 0;
        BigDecimal valorInventario = BigDecimal.ZERO;
        String imagenPrincipal = "";
        for (ProductoPresentacion presentacion : presentaciones) {
            int stock = presentacion.getStock() != null ? presentacion.getStock() : 0;
            stockTotal += stock;
            BigDecimal precioCompra = presentacion.getPrecioCompra() != null
                    ? presentacion.getPrecioCompra()
                    : BigDecimal.ZERO;
            valorInventario = valorInventario.add(precioCompra.multiply(BigDecimal.valueOf(stock)));
            if (imagenPrincipal.isEmpty()) {
                String ruta = rutaImagenPrincipal(presentacion);
                if (ruta != null) {
                    imagenPrincipal = ruta;
                }
            }
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalPresentaciones", presentaciones.size());
        resumen.put("stockTotal", stockTotal);
        resumen.put("valorInventario", valorInventario);
        resumen.put("imagenPrincipal", imagenPrincipal);
        return resumen;
    }

    @Transactional
    public Map<String, Object> guardar(Integer productoId, ProductoPresentacionRequest request) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado."));

        String nombre = request.getNombre() != null ? request.getNombre().trim() : "";
        if (nombre.isBlank()) {
            throw new RuntimeException("Ingrese el nombre de la presentación.");
        }
        if (request.getPrecio() == null || request.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El precio de venta debe ser mayor o igual a 0.");
        }
        BigDecimal precioCompra = request.getPrecioCompra() != null ? request.getPrecioCompra() : BigDecimal.ZERO;
        if (precioCompra.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("El precio de compra debe ser mayor o igual a 0.");
        }
        if (precioCompra.compareTo(request.getPrecio()) > 0) {
            throw new RuntimeException("El precio de compra no puede ser mayor al precio de venta.");
        }
        if (request.getStock() == null || request.getStock() < 0) {
            throw new RuntimeException("El stock debe ser mayor o igual a 0.");
        }
        if (request.getStockMinimo() == null || request.getStockMinimo() < 0) {
            throw new RuntimeException("El stock mínimo debe ser mayor o igual a 0.");
        }

        ProductoPresentacion presentacion;
        if (request.getId() != null) {
            presentacion = presentacionRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));
            if (!presentacion.getProducto().getId().equals(productoId)) {
                throw new RuntimeException("La presentación no pertenece a este producto.");
            }
        } else {
            presentacion = new ProductoPresentacion();
            presentacion.setProducto(producto);
            int orden = (int) presentacionRepository.countByProducto_Id(productoId);
            presentacion.setOrden(orden);
        }

        validarNombreUnico(productoId, nombre, presentacion.getId());

        presentacion.setNombre(nombre);
        presentacion.setPrecio(request.getPrecio());
        presentacion.setPrecioCompra(precioCompra);
        presentacion.setStock(request.getStock());
        presentacion.setStockMinimo(request.getStockMinimo());
        presentacion.setEstado(parseEstado(request.getEstado()));

        presentacion = presentacionRepository.save(presentacion);
        if (presentacion.getCodigoBarras() == null || presentacion.getCodigoBarras().isBlank()) {
            presentacion.setCodigoBarras(generarCodigoBarrasUnico(presentacion.getProducto().getId(), presentacion.getId()));
            presentacion = presentacionRepository.save(presentacion);
        }
        sincronizarAgregadosProducto(productoId);

        Map<String, Object> res = mapearPresentacion(presentacion);
        res.put("success", true);
        res.put("message", request.getId() != null ? "Presentación actualizada." : "Presentación registrada.");
        return res;
    }

    @Transactional
    public Map<String, Object> eliminar(Integer presentacionId) {
        ProductoPresentacion presentacion = presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));
        Integer productoId = presentacion.getProducto().getId();

        long total = presentacionRepository.countByProducto_Id(productoId);
        if (total <= 1) {
            throw new RuntimeException("El producto debe tener al menos una presentación.");
        }

        presentacionRepository.delete(presentacion);
        sincronizarAgregadosProducto(productoId);

        return Map.of(
                "success", true,
                "message", "Presentación eliminada correctamente."
        );
    }

    @Transactional
    public void asegurarPresentacionDefault(Producto producto) {
        if (producto == null || producto.getId() == null) {
            return;
        }
        if (presentacionRepository.countByProducto_Id(producto.getId()) > 0) {
            sincronizarAgregadosProducto(producto.getId());
            return;
        }

        ProductoPresentacion presentacion = new ProductoPresentacion();
        presentacion.setProducto(producto);
        presentacion.setNombre("Unidad");
        presentacion.setPrecio(producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO);
        presentacion.setPrecioCompra(producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO);
        presentacion.setStock(producto.getStock() != null ? producto.getStock() : 0);
        presentacion.setStockMinimo(producto.getStockMinimo() != null ? producto.getStockMinimo() : 0);
        presentacion.setEstado(ProductoPresentacion.Estado.ACTIVO);
        presentacion.setOrden(0);
        presentacionRepository.save(presentacion);
        sincronizarAgregadosProducto(producto.getId());
    }

    @Transactional
    public void generarCodigosBarrasFaltantes() {
        for (ProductoPresentacion presentacion : presentacionRepository.findAll()) {
            if (presentacion.getCodigoBarras() == null || presentacion.getCodigoBarras().isBlank()) {
                presentacion.setCodigoBarras(generarCodigoBarrasUnico(
                        presentacion.getProducto().getId(), presentacion.getId()));
                presentacionRepository.save(presentacion);
            }
        }
    }

    private String generarCodigoBarrasUnico(Integer productoId, Integer presentacionId) {
        String codigo = construirCodigoBarrasEan13(productoId, presentacionId);
        int intentos = 0;
        while (presentacionRepository.existsByCodigoBarras(codigo) && intentos < 10) {
            intentos++;
            codigo = construirCodigoBarrasEan13(productoId, presentacionId + intentos);
        }
        return codigo;
    }

    private String construirCodigoBarrasEan13(Integer productoId, Integer presentacionId) {
        String base12 = String.format("20%04d%06d",
                productoId != null ? productoId : 0,
                presentacionId != null ? presentacionId : 0);
        if (base12.length() > 12) {
            base12 = base12.substring(0, 12);
        }
        while (base12.length() < 12) {
            base12 = "20" + base12;
        }
        return base12 + calcularDigitoControlEan13(base12);
    }

    private int calcularDigitoControlEan13(String codigo12) {
        int suma = 0;
        for (int i = 0; i < 12; i++) {
            int digito = Character.getNumericValue(codigo12.charAt(i));
            suma += (i % 2 == 0) ? digito : digito * 3;
        }
        int resto = suma % 10;
        return resto == 0 ? 0 : 10 - resto;
    }

    @Transactional
    public void migrarProductosSinPresentacion() {
        for (Producto producto : productoRepository.findAll()) {
            if (presentacionRepository.countByProducto_Id(producto.getId()) == 0) {
                asegurarPresentacionDefault(producto);
            } else {
                migrarCamposPresentacionDesdeProducto(producto);
            }
        }
        generarCodigosBarrasFaltantes();
    }

    @Transactional
    public void migrarCamposPresentacionDesdeProducto(Producto producto) {
        for (ProductoPresentacion presentacion : presentacionRepository
                .findByProducto_IdOrderByOrdenAscIdAsc(producto.getId())) {
            boolean cambio = false;
            if (presentacion.getPrecioCompra() == null) {
                presentacion.setPrecioCompra(producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO);
                cambio = true;
            }
            if (presentacion.getStockMinimo() == null) {
                presentacion.setStockMinimo(producto.getStockMinimo() != null ? producto.getStockMinimo() : 0);
                cambio = true;
            }
            if (cambio) {
                presentacionRepository.save(presentacion);
            }
        }
        sincronizarAgregadosProducto(producto.getId());
    }

    @Transactional
    public void sincronizarDesdeProductoPadre(Producto producto) {
        if (producto == null || producto.getId() == null) {
            return;
        }
        if (presentacionRepository.countByProducto_Id(producto.getId()) == 0) {
            asegurarPresentacionDefault(producto);
        } else {
            sincronizarAgregadosProducto(producto.getId());
        }
    }

    @Transactional
    public void sincronizarAgregadosProducto(Integer productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado."));
        List<ProductoPresentacion> presentaciones = presentacionRepository
                .findByProducto_IdOrderByOrdenAscIdAsc(productoId);

        int stockTotal = presentaciones.stream()
                .mapToInt(p -> p.getStock() != null ? p.getStock() : 0)
                .sum();

        int stockMinimoTotal = presentaciones.stream()
                .mapToInt(p -> p.getStockMinimo() != null ? p.getStockMinimo() : 0)
                .sum();

        BigDecimal precioMinimo = presentaciones.stream()
                .filter(p -> p.getEstado() == ProductoPresentacion.Estado.ACTIVO)
                .map(p -> p.getPrecio())
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(producto.getPrecio() != null ? producto.getPrecio() : BigDecimal.ZERO);

        BigDecimal precioCompraMinimo = presentaciones.stream()
                .filter(p -> p.getEstado() == ProductoPresentacion.Estado.ACTIVO)
                .map(p -> p.getPrecioCompra() != null ? p.getPrecioCompra() : BigDecimal.ZERO)
                .min(Comparator.naturalOrder())
                .orElse(producto.getPrecioCompra() != null ? producto.getPrecioCompra() : BigDecimal.ZERO);

        producto.setStock(stockTotal);
        producto.setStockMinimo(stockMinimoTotal);
        producto.setPrecio(precioMinimo);
        producto.setPrecioCompra(precioCompraMinimo);
        productoRepository.save(producto);
        productoNodeSyncService.sincronizarProducto(productoId);
    }

    @Transactional(readOnly = true)
    public ProductoPresentacion obtenerParaVenta(Integer presentacionId, Integer productoId) {
        if (presentacionId != null) {
            ProductoPresentacion presentacion = presentacionRepository.findById(presentacionId)
                    .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));
            if (productoId != null && !presentacion.getProducto().getId().equals(productoId)) {
                throw new RuntimeException("La presentación no corresponde al producto seleccionado.");
            }
            validarPresentacionVendible(presentacion);
            return presentacion;
        }

        if (productoId == null) {
            throw new RuntimeException("Seleccione una presentación del producto.");
        }

        List<ProductoPresentacion> activas = presentacionRepository.findActivasByProductoId(productoId);
        if (activas.size() == 1) {
            validarPresentacionVendible(activas.get(0));
            return activas.get(0);
        }
        if (activas.isEmpty()) {
            throw new RuntimeException("El producto no tiene presentaciones activas.");
        }
        throw new RuntimeException("Seleccione la presentación del producto.");
    }

    @Transactional(readOnly = true)
    public List<ProductoPresentacion> listarActivasPorProducto(Integer productoId) {
        return presentacionRepository.findActivasByProductoId(productoId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> mapearPresentacionesActivas(Producto producto) {
        return presentacionRepository.findActivasByProductoId(producto.getId()).stream()
                .map(this::mapearPresentacionPublica)
                .toList();
    }

    @Transactional
    public ProductoPresentacion ajustarStockPresentacionInventario(Integer presentacionId, Integer stockNuevo) {
        if (stockNuevo == null || stockNuevo < 0) {
            throw new RuntimeException("El stock debe ser un número mayor o igual a 0.");
        }
        ProductoPresentacion presentacion = presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));
        presentacion.setStock(stockNuevo);
        presentacionRepository.save(presentacion);
        sincronizarAgregadosProducto(presentacion.getProducto().getId());
        return presentacion;
    }

    @Transactional(readOnly = true)
    public ProductoPresentacion obtenerPresentacion(Integer presentacionId) {
        return presentacionRepository.findById(presentacionId)
                .orElseThrow(() -> new RuntimeException("Presentación no encontrada."));
    }

    @Transactional
    public void ajustarStockProductoInventario(Integer productoId, Integer stockNuevo) {
        List<ProductoPresentacion> presentaciones = presentacionRepository
                .findByProducto_IdOrderByOrdenAscIdAsc(productoId);
        if (presentaciones.isEmpty()) {
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado."));
            producto.setStock(stockNuevo);
            productoRepository.save(producto);
            asegurarPresentacionDefault(producto);
            return;
        }

        if (presentaciones.size() == 1) {
            presentaciones.get(0).setStock(stockNuevo);
            presentacionRepository.save(presentaciones.get(0));
        } else {
            int stockOtros = presentaciones.stream().skip(1)
                    .mapToInt(p -> p.getStock() != null ? p.getStock() : 0)
                    .sum();
            presentaciones.get(0).setStock(Math.max(0, stockNuevo - stockOtros));
            presentacionRepository.save(presentaciones.get(0));
        }
        sincronizarAgregadosProducto(productoId);
    }

    @Transactional(readOnly = true)
    public List<ProductoPresentacion> listarActivasParaVenta() {
        return presentacionRepository.findActivasParaVenta();
    }

    @Transactional
    public void descontarStock(ProductoPresentacion presentacion, int cantidad) {
        presentacion.setStock(presentacion.getStock() - cantidad);
        presentacionRepository.save(presentacion);
        sincronizarAgregadosProducto(presentacion.getProducto().getId());
    }

    @Transactional
    public void restaurarStock(ProductoPresentacion presentacion, int cantidad) {
        presentacion.setStock(presentacion.getStock() + cantidad);
        presentacionRepository.save(presentacion);
        sincronizarAgregadosProducto(presentacion.getProducto().getId());
    }

    public void validarPresentacionVendible(ProductoPresentacion presentacion) {
        if (presentacion.getEstado() != ProductoPresentacion.Estado.ACTIVO) {
            throw new RuntimeException("La presentación \"" + presentacion.getNombre() + "\" no está activa.");
        }
        Producto producto = presentacion.getProducto();
        if (producto == null || producto.getEstado() != Producto.Estado.ACTIVO) {
            throw new RuntimeException("El producto no está activo.");
        }
    }

    private void validarNombreUnico(Integer productoId, String nombre, Integer presentacionId) {
        boolean duplicado = presentacionId != null
                ? presentacionRepository.existsByProducto_IdAndNombreIgnoreCaseAndIdNot(productoId, nombre, presentacionId)
                : presentacionRepository.existsByProducto_IdAndNombreIgnoreCase(productoId, nombre);
        if (duplicado) {
            throw new RuntimeException("Ya existe una presentación con ese nombre para este producto.");
        }
    }

    private ProductoPresentacion.Estado parseEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return ProductoPresentacion.Estado.ACTIVO;
        }
        try {
            return ProductoPresentacion.Estado.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de presentación no válido.");
        }
    }

    private List<Map<String, Object>> mapearImagenesPresentacion(Integer presentacionId) {
        return presentacionImagenRepository.findByPresentacion_IdOrderByOrdenAsc(presentacionId).stream()
                .map(pi -> {
                    Map<String, Object> img = new LinkedHashMap<>();
                    img.put("id", pi.getId());
                    img.put("ruta", pi.getImagen() != null
                            ? ImagenRutas.normalizarRuta(pi.getImagen().getRuta())
                            : "");
                    return img;
                })
                .toList();
    }

    private Map<String, Object> mapearPresentacion(ProductoPresentacion presentacion) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", presentacion.getId());
        mapa.put("productoId", presentacion.getProducto().getId());
        mapa.put("nombre", presentacion.getNombre());
        mapa.put("precioCompra", presentacion.getPrecioCompra() != null ? presentacion.getPrecioCompra() : BigDecimal.ZERO);
        mapa.put("precio", presentacion.getPrecio());
        mapa.put("stock", presentacion.getStock());
        mapa.put("stockMinimo", presentacion.getStockMinimo() != null ? presentacion.getStockMinimo() : 0);
        mapa.put("codigoBarras", presentacion.getCodigoBarras() != null ? presentacion.getCodigoBarras() : "");
        mapa.put("estado", presentacion.getEstado() != null ? presentacion.getEstado().name() : "ACTIVO");
        mapa.put("orden", presentacion.getOrden() != null ? presentacion.getOrden() : 0);
        String rutaPrincipal = rutaImagenPrincipal(presentacion);
        mapa.put("imagenPrincipal", rutaPrincipal != null ? rutaPrincipal : "");
        mapa.put("imagenes", mapearImagenesPresentacion(presentacion.getId()));
        return mapa;
    }

    @Transactional
    public void limpiarImagenesDeProducto(Integer productoId) {
        List<ProductoPresentacion> presentaciones =
                presentacionRepository.findByProducto_IdOrderByOrdenAscIdAsc(productoId);
        Set<Long> imagenesEliminadas = new HashSet<>();
        for (ProductoPresentacion presentacion : presentaciones) {
            presentacionImagenRepository.findByPresentacion_IdOrderByOrdenAsc(presentacion.getId())
                    .forEach(registro -> eliminarImagenSiPendiente(registro.getImagen(), imagenesEliminadas));
            presentacionImagenRepository.deleteByPresentacion_Id(presentacion.getId());
            eliminarImagenSiPendiente(presentacion.getImagen(), imagenesEliminadas);
        }
    }

    private void eliminarImagenSiPendiente(com.test.desarrollo_web.Models.Imagen imagen, Set<Long> imagenesEliminadas) {
        if (imagen == null || imagen.getId() == null || !imagenesEliminadas.add(imagen.getId())) {
            return;
        }
        if (imagen.getRuta() != null) {
            fileStorageService.deleteFile(imagen.getRuta());
        }
        imagenRepository.delete(imagen);
    }

    @Transactional(readOnly = true)
    public String rutaImagenPrincipal(ProductoPresentacion presentacion) {
        if (presentacion == null) {
            return null;
        }
        return presentacionImagenRepository.findByPresentacion_IdOrderByOrdenAsc(presentacion.getId()).stream()
                .map(pi -> pi.getImagen())
                .filter(img -> img != null && img.getRuta() != null && !img.getRuta().isBlank())
                .map(img -> ImagenRutas.normalizarRuta(img.getRuta()))
                .findFirst()
                .orElseGet(() -> presentacion.getImagen() != null && presentacion.getImagen().getRuta() != null
                        ? ImagenRutas.normalizarRuta(presentacion.getImagen().getRuta())
                        : null);
    }

    @Transactional(readOnly = true)
    public String urlImagenPrincipal(ProductoPresentacion presentacion) {
        return ImagenRutas.toPublicUrl(rutaImagenPrincipal(presentacion));
    }

    private Map<String, Object> mapearPresentacionPublica(ProductoPresentacion presentacion) {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", presentacion.getId());
        mapa.put("nombre", presentacion.getNombre());
        mapa.put("precio", presentacion.getPrecio());
        mapa.put("stock", presentacion.getStock());
        Map<String, String> medida = PresentacionMedidaUtil.extraerMedida(presentacion.getNombre());
        mapa.put("medida", medida.get("medida"));
        String imagen = urlImagenPrincipal(presentacion);
        mapa.put("imagen", imagen);
        if (imagen != null) {
            mapa.put("imagenes", List.of(imagen));
        } else {
            mapa.put("imagenes", List.of());
        }
        return mapa;
    }
}
