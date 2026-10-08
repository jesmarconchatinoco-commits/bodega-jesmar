package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.Producto;
import com.test.desarrollo_web.Repository.CategoriaRepository;
import com.test.desarrollo_web.Repository.ImagenRepository;
import com.test.desarrollo_web.Repository.ProductoRepository;
import com.test.desarrollo_web.dto.ProductoPresentacionRequest;
import com.test.desarrollo_web.service.FileStorageService;
import com.test.desarrollo_web.service.PresentacionImagenService;
import com.test.desarrollo_web.service.ProductoNodeSyncService;
import com.test.desarrollo_web.service.ProductoPresentacionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/productos")
public class ProductoController {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ImagenRepository imagenRepository;
    private final FileStorageService fileStorageService;
    private final ProductoPresentacionService presentacionService;
    private final PresentacionImagenService presentacionImagenService;
    private final ProductoNodeSyncService productoNodeSyncService;

    public ProductoController(ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository,
            ImagenRepository imagenRepository,
            FileStorageService fileStorageService,
            ProductoPresentacionService presentacionService,
            PresentacionImagenService presentacionImagenService,
            ProductoNodeSyncService productoNodeSyncService) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.imagenRepository = imagenRepository;
        this.fileStorageService = fileStorageService;
        this.presentacionService = presentacionService;
        this.presentacionImagenService = presentacionImagenService;
        this.productoNodeSyncService = productoNodeSyncService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Producto> productos = productoRepository.findAllWithDetalles();
        List<Integer> productoIds = productos.stream().map(p -> p.getId()).toList();
        Map<Integer, Map<String, Object>> resumenesPresentacion =
                presentacionService.resumenesPorProductos(productoIds);
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("presResumen", resumenesPresentacion);
        model.addAttribute("productosJson", buildProductosJson(productos, resumenesPresentacion));
        return "productos/list";
    }

    @PostMapping("/guardar")
    public String guardar(@RequestParam(required = false) Integer id,
                          @RequestParam String nombre,
                          @RequestParam(required = false) String descripcion,
                          @RequestParam(name = "categoriaId") Integer categoriaId,
                          @RequestParam(required = false, defaultValue = "0") BigDecimal precio,
                          @RequestParam(name = "precioCompra", required = false, defaultValue = "0") BigDecimal precioCompra,
                          @RequestParam(required = false, defaultValue = "0") Integer stock,
                          @RequestParam(name = "stockMinimo", required = false, defaultValue = "0") Integer stockMinimo,
                          @RequestParam Producto.Estado estado,
                          RedirectAttributes redirectAttributes) {
        try {
            Producto toSave = id != null
                    ? productoRepository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"))
                    : new Producto();

            toSave.setNombre(nombre.trim());
            toSave.setDescripcion(descripcion != null ? descripcion.trim() : null);
            toSave.setEstado(estado);
            if (id == null) {
                toSave.setPrecio(BigDecimal.ZERO);
                toSave.setPrecioCompra(BigDecimal.ZERO);
                toSave.setStock(0);
                toSave.setStockMinimo(0);
            }

            toSave.setCategoria(categoriaRepository.findById(categoriaId)
                    .orElseThrow(() -> new RuntimeException("Categoría no válida")));

            Producto guardado = productoRepository.save(toSave);

            presentacionService.asegurarPresentacionDefault(guardado);
            presentacionService.sincronizarAgregadosProducto(guardado.getId());

            redirectAttributes.addFlashAttribute("success", "Producto guardado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
        }
        return "redirect:/productos";
    }

    @GetMapping(value = "/api/{productoId}/presentaciones", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> listarPresentaciones(@PathVariable Integer productoId) {
        return presentacionService.listarPorProducto(productoId);
    }

    @PostMapping(value = "/api/{productoId}/presentaciones", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarPresentacion(@PathVariable Integer productoId,
                                                                   @RequestBody ProductoPresentacionRequest request) {
        try {
            return ResponseEntity.ok(presentacionService.guardar(productoId, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo guardar la presentación."
            ));
        }
    }

    @DeleteMapping(value = "/api/presentaciones/{presentacionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarPresentacion(@PathVariable Integer presentacionId) {
        try {
            return ResponseEntity.ok(presentacionService.eliminar(presentacionId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo eliminar la presentación."
            ));
        }
    }

    @PostMapping(value = "/api/presentaciones/{presentacionId}/imagenes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardarImagenesPresentacion(@PathVariable Integer presentacionId,
                                                                             @RequestPart("imagenFiles") MultipartFile[] imagenFiles) {
        try {
            int agregadas = presentacionImagenService.agregarImagenes(presentacionId, imagenFiles);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "agregadas", agregadas,
                    "message", agregadas > 0 ? agregadas + " imagen(es) agregada(s)." : "No se seleccionaron imágenes."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudieron guardar las imágenes."
            ));
        }
    }

    @DeleteMapping(value = "/api/presentaciones/imagenes/{presentacionImagenId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarImagenPresentacion(@PathVariable Long presentacionImagenId) {
        try {
            presentacionImagenService.eliminarImagen(presentacionImagenId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Imagen eliminada correctamente."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo eliminar la imagen."
            ));
        }
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Producto producto = productoRepository.findById(id).orElse(null);
            if (producto != null) {
                productoNodeSyncService.eliminarProducto(id);
                presentacionService.limpiarImagenesDeProducto(id);
                if (producto.getImagen() != null) {
                    fileStorageService.deleteFile(producto.getImagen().getRuta());
                    imagenRepository.delete(producto.getImagen());
                }
            }
            productoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Producto eliminado correctamente.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        return "redirect:/productos";
    }

    private String buildProductosJson(List<Producto> productos,
                                      Map<Integer, Map<String, Object>> resumenesPresentacion) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            Map<String, Object> resumen = resumenesPresentacion.getOrDefault(p.getId(), Map.of());
            sb.append("{");
            sb.append("\"id\":").append(p.getId()).append(",");
            sb.append("\"nombre\":\"").append(escapar(p.getNombre())).append("\",");
            sb.append("\"descripcion\":\"").append(escapar(p.getDescripcion())).append("\",");
            sb.append("\"categoriaId\":").append(p.getCategoria() != null ? p.getCategoria().getId() : "null").append(",");
            sb.append("\"categoriaNombre\":\"").append(escapar(
                    p.getCategoria() != null ? p.getCategoria().getNombre() : "")).append("\",");
            sb.append("\"precio\":").append(p.getPrecio()).append(",");
            sb.append("\"precioCompra\":").append(p.getPrecioCompra() != null ? p.getPrecioCompra() : "0").append(",");
            sb.append("\"stock\":").append(p.getStock()).append(",");
            sb.append("\"stockMinimo\":").append(p.getStockMinimo()).append(",");
            sb.append("\"estado\":\"").append(p.getEstado() != null ? p.getEstado().name() : "").append("\",");
            sb.append("\"totalPresentaciones\":").append(resumen.getOrDefault("totalPresentaciones", 0)).append(",");
            sb.append("\"stockTotal\":").append(resumen.getOrDefault("stockTotal", 0)).append(",");
            sb.append("\"imagenPrincipal\":\"").append(escapar(
                    String.valueOf(resumen.getOrDefault("imagenPrincipal", "")))).append("\"");
            sb.append("}");
            if (i < productos.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
