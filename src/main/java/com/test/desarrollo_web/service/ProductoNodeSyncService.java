package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Categoria;
import com.test.desarrollo_web.Models.Producto;
import com.test.desarrollo_web.Models.ProductoPresentacion;
import com.test.desarrollo_web.Repository.ProductoPresentacionRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import com.test.desarrollo_web.config.NodeApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoNodeSyncService {

    private static final Logger log = LoggerFactory.getLogger(ProductoNodeSyncService.class);

    private final NodeApiProperties nodeApiProperties;
    private final ProductoRepository productoRepository;
    private final ProductoPresentacionRepository presentacionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public ProductoNodeSyncService(NodeApiProperties nodeApiProperties,
                                   ProductoRepository productoRepository,
                                   ProductoPresentacionRepository presentacionRepository,
                                   ObjectMapper objectMapper) {
        this.nodeApiProperties = nodeApiProperties;
        this.productoRepository = productoRepository;
        this.presentacionRepository = presentacionRepository;
        this.objectMapper = objectMapper;
    }

    public void sincronizarProducto(Integer productoId) {
        if (!nodeApiProperties.isEnabled() || productoId == null) {
            return;
        }

        try {
            Producto producto = productoRepository.findById(productoId).orElse(null);
            if (producto == null) {
                return;
            }

            Categoria categoria = producto.getCategoria();
            List<ProductoPresentacion> presentaciones =
                    presentacionRepository.findByProducto_IdOrderByOrdenAscIdAsc(productoId);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("springProductoId", producto.getId());
            payload.put("nombre", producto.getNombre());
            payload.put("descripcion", producto.getDescripcion());
            payload.put("precio", producto.getPrecio());
            payload.put("precioCompra", producto.getPrecioCompra());
            payload.put("stock", producto.getStock());
            payload.put("stockMinimo", producto.getStockMinimo());
            payload.put("estado", producto.getEstado() != null ? producto.getEstado().name() : "ACTIVO");

            Map<String, Object> categoriaMap = new LinkedHashMap<>();
            if (categoria != null) {
                categoriaMap.put("springId", categoria.getId());
                categoriaMap.put("nombre", categoria.getNombre());
                categoriaMap.put("descripcion", categoria.getDescripcion());
                categoriaMap.put("estado", categoria.getEstado() != null ? categoria.getEstado().name() : "ACTIVO");
            }
            payload.put("categoria", categoriaMap);

            List<Map<String, Object>> presList = new ArrayList<>();
            for (ProductoPresentacion p : presentaciones) {
                Map<String, Object> pres = new LinkedHashMap<>();
                pres.put("springId", p.getId());
                pres.put("nombre", p.getNombre());
                pres.put("precio", p.getPrecio());
                pres.put("precioCompra", p.getPrecioCompra());
                pres.put("stock", p.getStock());
                pres.put("stockMinimo", p.getStockMinimo());
                pres.put("codigoBarras", p.getCodigoBarras());
                pres.put("estado", p.getEstado() != null ? p.getEstado().name() : "ACTIVO");
                pres.put("orden", p.getOrden());
                presList.add(pres);
            }
            payload.put("presentaciones", presList);

            enviarPost("/api/sync/from-spring", payload);
            log.info("Producto {} sincronizado con Node API", productoId);
        } catch (Exception e) {
            log.warn("No se pudo sincronizar producto {} con Node: {}", productoId, e.getMessage());
        }
    }

    public void eliminarProducto(Integer productoId) {
        if (!nodeApiProperties.isEnabled() || productoId == null) {
            return;
        }

        try {
            enviarDelete("/api/sync/producto/" + productoId);
            log.info("Producto {} eliminado en Node API", productoId);
        } catch (Exception e) {
            log.warn("No se pudo eliminar producto {} en Node: {}", productoId, e.getMessage());
        }
    }

    private void enviarPost(String path, Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizarUrl(path)))
                .timeout(Duration.ofSeconds(nodeApiProperties.getTimeoutSeconds()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private void enviarDelete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizarUrl(path)))
                .timeout(Duration.ofSeconds(nodeApiProperties.getTimeoutSeconds()))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    private String normalizarUrl(String path) {
        String base = nodeApiProperties.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }
}
