package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.dto.ClienteCatalogoRequest;
import com.test.desarrollo_web.dto.PedidoCatalogoRequest;
import com.test.desarrollo_web.service.CatalogoService;
import com.test.desarrollo_web.service.ClienteService;
import com.test.desarrollo_web.service.HorarioAtencionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;
    private final ClienteService clienteService;
    private final HorarioAtencionService horarioAtencionService;
    private final ObjectMapper objectMapper;

    public CatalogoController(CatalogoService catalogoService,
                              ClienteService clienteService,
                              HorarioAtencionService horarioAtencionService,
                              ObjectMapper objectMapper) {
        this.catalogoService = catalogoService;
        this.clienteService = clienteService;
        this.horarioAtencionService = horarioAtencionService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String catalogo(Model model) throws Exception {
        var productos = catalogoService.listarProductos();
        var categorias = catalogoService.listarCategorias();
        model.addAttribute("productos", productos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("sliders", catalogoService.listarSliders());
        model.addAttribute("logo", catalogoService.obtenerLogo().orElse(null));
        model.addAttribute("catalogoJson", objectMapper.writeValueAsString(Map.of(
                "productos", productos,
                "categorias", categorias,
                "checkout", catalogoService.obtenerConfigCheckout()
        )));
        return "catalogo";
    }

    @GetMapping(value = "/api/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> productos() {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("productos", catalogoService.listarProductos());
        respuesta.put("categorias", catalogoService.listarCategorias());
        respuesta.put("checkout", catalogoService.obtenerConfigCheckout());
        return respuesta;
    }

    @GetMapping(value = "/api/documento", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> consultarDocumento(@RequestParam String documento) {
        return clienteService.consultarDocumento(documento);
    }

    @PostMapping(value = "/api/cliente/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loginCliente(@RequestBody ClienteCatalogoRequest request) {
        try {
            return ResponseEntity.ok(clienteService.iniciarSesionCatalogo(request.getDocumento()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo iniciar sesión."
            ));
        }
    }

    @PostMapping(value = "/api/cliente/registrar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarCliente(@RequestBody ClienteCatalogoRequest request) {
        try {
            return ResponseEntity.ok(clienteService.registrarDesdeCatalogo(
                    request.getDocumento(), request.getNombre(), request.getTelefono()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo completar el registro."
            ));
        }
    }

    @GetMapping(value = "/api/horarios-recojo", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> horariosRecojo() {
        return horarioAtencionService.listarDisponiblesPublico();
    }

    @PostMapping(value = "/api/pedido", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarPedido(@RequestPart("datos") String datosJson,
                                                               @RequestPart("comprobante") MultipartFile comprobante) {
        try {
            PedidoCatalogoRequest request = objectMapper.readValue(datosJson, PedidoCatalogoRequest.class);
            return ResponseEntity.ok(catalogoService.registrarPedido(request, comprobante));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo registrar el pedido."
            ));
        }
    }
}
