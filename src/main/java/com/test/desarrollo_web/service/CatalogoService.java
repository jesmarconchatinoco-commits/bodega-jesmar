package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.*;
import com.test.desarrollo_web.Repository.CategoriaRepository;
import com.test.desarrollo_web.Repository.ImagenSliderRepository;
import com.test.desarrollo_web.Repository.PedidoCatalogoRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import com.test.desarrollo_web.config.CatalogoProperties;
import com.test.desarrollo_web.config.ImageStorageCategory;
import com.test.desarrollo_web.util.ImagenRutas;
import com.test.desarrollo_web.dto.PedidoCatalogoItemRequest;
import com.test.desarrollo_web.dto.PedidoCatalogoRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagenSliderRepository imagenSliderRepository;
    private final PedidoCatalogoRepository pedidoCatalogoRepository;
    private final LogoService logoService;
    private final FileStorageService fileStorageService;
    private final CatalogoProperties catalogoProperties;
    private final HorarioAtencionService horarioAtencionService;
    private final ProductoPresentacionService presentacionService;
    private final ConfiguracionPagoService configuracionPagoService;

    public CatalogoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           ImagenSliderRepository imagenSliderRepository,
                           PedidoCatalogoRepository pedidoCatalogoRepository,
                           LogoService logoService,
                           FileStorageService fileStorageService,
                           CatalogoProperties catalogoProperties,
                           HorarioAtencionService horarioAtencionService,
                           ProductoPresentacionService presentacionService,
                           ConfiguracionPagoService configuracionPagoService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagenSliderRepository = imagenSliderRepository;
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
        this.logoService = logoService;
        this.fileStorageService = fileStorageService;
        this.catalogoProperties = catalogoProperties;
        this.horarioAtencionService = horarioAtencionService;
        this.presentacionService = presentacionService;
        this.configuracionPagoService = configuracionPagoService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerConfigCheckout() {
        Map<String, Object> config = configuracionPagoService.obtenerConfigCheckout();
        return config;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarProductos() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Producto producto : productoRepository.findActivosWithDetalles()) {
            resultado.add(mapearProducto(producto));
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarCategorias() {
        List<Map<String, Object>> categorias = new ArrayList<>();
        for (Categoria categoria : categoriaRepository.findByEstado(Categoria.Estado.ACTIVO)) {
            categorias.add(Map.of(
                    "id", categoria.getId(),
                    "nombre", categoria.getNombre()
            ));
        }
        categorias.sort(Comparator.comparing(c -> c.get("nombre").toString()));
        return categorias;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarSliders() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (ImagenSlider slider : imagenSliderRepository.findActiveSlidersWithImagen(
                ImagenSlider.TITULO_SLIDER, ImagenSlider.Estado.ACTIVO)) {
            if (slider.getImagen() != null) {
                resultado.add(Map.of(
                        "id", slider.getId(),
                        "url", ImagenRutas.toPublicUrl(slider.getImagen().getRuta())
                ));
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Optional<Map<String, Object>> obtenerLogo() {
        return logoService.findLogoActivo()
                .filter(logo -> logo.getImagen() != null)
                .map(logo -> Map.of(
                        "url", ImagenRutas.toPublicUrl(logo.getImagen().getRuta())
                ));
    }

    @Transactional
    public Map<String, Object> registrarPedido(PedidoCatalogoRequest request, MultipartFile comprobante) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("El carrito está vacío.");
        }

        String nombre = request.getNombreCliente() != null ? request.getNombreCliente().trim() : "";
        String telefono = request.getTelefono() != null ? request.getTelefono().trim() : "";
        String documento = request.getDocumento() != null ? request.getDocumento().replaceAll("\\D", "").trim() : "";

        if (documento.length() != 8) {
            throw new RuntimeException("Ingrese un DNI válido de 8 dígitos.");
        }
        if (nombre.isBlank()) {
            throw new RuntimeException("Verifique su DNI para cargar el nombre del cliente.");
        }
        if (telefono.isBlank()) {
            throw new RuntimeException("Ingrese su teléfono de contacto.");
        }
        if (!telefono.matches("^9\\d{8}$")) {
            throw new RuntimeException("El teléfono debe tener 9 dígitos y comenzar con 9.");
        }

        PedidoCatalogo.FormaEntrega formaEntrega = parseFormaEntrega(request.getFormaEntrega());
        PedidoCatalogo.MetodoPago metodoPago = parseMetodoPago(request.getMetodoPago());

        if (formaEntrega == PedidoCatalogo.FormaEntrega.ENVIO_DOMICILIO) {
            if (request.getDireccion() == null || request.getDireccion().isBlank()) {
                throw new RuntimeException("Ingrese la dirección de entrega.");
            }
            if (request.getDistrito() == null || request.getDistrito().isBlank()) {
                throw new RuntimeException("Ingrese el distrito de entrega.");
            }
        } else if (request.getHorarioRecojoId() == null) {
            throw new RuntimeException("Seleccione la fecha y hora de recojo en tienda.");
        }

        if (comprobante == null || comprobante.isEmpty()) {
            throw new RuntimeException("Suba la captura del comprobante de pago.");
        }

        String codigoValidacion = request.getCodigoValidacionPago() != null
                ? request.getCodigoValidacionPago().trim().toUpperCase() : "";
        if (codigoValidacion.isBlank()) {
            throw new RuntimeException("Ingrese el código de validación de pago.");
        }
        if (codigoValidacion.length() < 6 || codigoValidacion.length() > 30) {
            throw new RuntimeException("El código de validación debe tener entre 6 y 30 caracteres.");
        }
        if (!codigoValidacion.matches("^[A-Z0-9\\-]+$")) {
            throw new RuntimeException("El código de validación solo puede contener letras, números y guiones.");
        }

        PedidoCatalogo pedido = new PedidoCatalogo();
        pedido.setNombreCliente(nombre);
        pedido.setTelefono(telefono);
        pedido.setDocumento(documento);
        pedido.setFormaEntrega(formaEntrega);
        pedido.setMetodoPago(metodoPago);
        pedido.setCodigoValidacionPago(codigoValidacion);
        pedido.setComprobantePago(fileStorageService.saveFile(comprobante, ImageStorageCategory.PEDIDOS));

        if (formaEntrega == PedidoCatalogo.FormaEntrega.ENVIO_DOMICILIO) {
            pedido.setDireccion(request.getDireccion().trim());
            pedido.setReferencia(request.getReferencia() != null ? request.getReferencia().trim() : null);
            pedido.setDistrito(request.getDistrito().trim());
            pedido.setCostoEnvio(catalogoProperties.getCostoEnvio());
            pedido.setObservaciones(construirObservacionesEntrega(pedido));
        } else {
            HorarioAtencion horarioRecojo = horarioAtencionService.reservarHorario(request.getHorarioRecojoId());
            pedido.setHorarioRecojo(horarioRecojo);
            pedido.setCostoEnvio(BigDecimal.ZERO);
            pedido.setObservaciones(horarioAtencionService.formatearRecojo(horarioRecojo));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (PedidoCatalogoItemRequest item : request.getItems()) {
            if (item.getProductoId() == null || item.getCantidad() == null || item.getCantidad() < 1) {
                throw new RuntimeException("Producto o cantidad inválida en el carrito.");
            }

            ProductoPresentacion presentacion = presentacionService.obtenerParaVenta(
                    item.getPresentacionId(), item.getProductoId());
            Producto producto = presentacion.getProducto();

            if (presentacion.getStock() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para: " + producto.getNombre()
                        + " (" + presentacion.getNombre() + ")");
            }

            BigDecimal lineSubtotal = presentacion.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));
            PedidoCatalogoDetalle detalle = new PedidoCatalogoDetalle();
            detalle.setProductoId(producto.getId());
            detalle.setPresentacionId(presentacion.getId());
            detalle.setNombreProducto(producto.getNombre());
            detalle.setNombrePresentacion(presentacion.getNombre());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(presentacion.getPrecio());
            detalle.setSubtotal(lineSubtotal);
            pedido.addDetalle(detalle);
            subtotal = subtotal.add(lineSubtotal);
        }

        pedido.setSubtotal(subtotal);
        pedido.setTotal(subtotal.add(pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO));
        pedido = pedidoCatalogoRepository.save(pedido);

        return Map.of(
                "success", true,
                "pedidoId", pedido.getId(),
                "total", pedido.getTotal(),
                "message", "Pedido #" + pedido.getId() + " registrado. Verificaremos su pago y nos comunicaremos con usted."
        );
    }

    private PedidoCatalogo.FormaEntrega parseFormaEntrega(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("Seleccione la forma de entrega.");
        }
        try {
            return PedidoCatalogo.FormaEntrega.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Forma de entrega no válida.");
        }
    }

    private PedidoCatalogo.MetodoPago parseMetodoPago(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RuntimeException("Seleccione el método de pago.");
        }
        try {
            return PedidoCatalogo.MetodoPago.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Método de pago no válido.");
        }
    }

    private String construirObservacionesEntrega(PedidoCatalogo pedido) {
        StringBuilder sb = new StringBuilder("Envío a domicilio");
        sb.append(" | Dir: ").append(pedido.getDireccion());
        if (pedido.getReferencia() != null && !pedido.getReferencia().isBlank()) {
            sb.append(" | Ref: ").append(pedido.getReferencia());
        }
        sb.append(" | Distrito: ").append(pedido.getDistrito());
        return sb.toString();
    }

    private Map<String, Object> mapearProducto(Producto producto) {
        List<Map<String, Object>> presentaciones = presentacionService.mapearPresentacionesActivas(producto);

        List<String> imagenes = new ArrayList<>();
        for (Map<String, Object> pres : presentaciones) {
            Object imgs = pres.get("imagenes");
            if (imgs instanceof List<?> lista) {
                for (Object url : lista) {
                    if (url != null && !imagenes.contains(url.toString())) {
                        imagenes.add(url.toString());
                    }
                }
            }
        }
        if (imagenes.isEmpty()) {
            for (ProductoPresentacion presentacion : presentacionService.listarActivasPorProducto(producto.getId())) {
                String url = presentacionService.urlImagenPrincipal(presentacion);
                if (url != null && !imagenes.contains(url)) {
                    imagenes.add(url);
                }
            }
        }

        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", producto.getId());
        mapa.put("nombre", producto.getNombre());
        mapa.put("descripcion", producto.getDescripcion() != null ? producto.getDescripcion() : "");
        mapa.put("precio", producto.getPrecio());
        mapa.put("stock", producto.getStock());
        mapa.put("categoriaId", producto.getCategoria() != null ? producto.getCategoria().getId() : null);
        mapa.put("categoriaNombre", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "Sin categoría");
        mapa.put("imagenes", imagenes);

        mapa.put("presentaciones", presentaciones);
        if (!presentaciones.isEmpty()) {
            mapa.put("precio", presentaciones.stream()
                    .map(p -> (BigDecimal) p.get("precio"))
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(producto.getPrecio()));
            mapa.put("stock", presentaciones.stream()
                    .mapToInt(p -> (Integer) p.get("stock"))
                    .sum());
        }
        return mapa;
    }
}
