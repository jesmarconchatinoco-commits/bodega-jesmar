package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.PedidoCatalogo;
import com.test.desarrollo_web.Repository.PedidoCatalogoRepository;
import com.test.desarrollo_web.service.PedidoCatalogoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Controller
@RequestMapping("/pedidos")
public class PedidoCatalogoController {

    private final PedidoCatalogoService pedidoCatalogoService;
    private final PedidoCatalogoRepository pedidoCatalogoRepository;

    public PedidoCatalogoController(PedidoCatalogoService pedidoCatalogoService,
                                    PedidoCatalogoRepository pedidoCatalogoRepository) {
        this.pedidoCatalogoService = pedidoCatalogoService;
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String estado, Model model) {
        model.addAttribute("pedidos", pedidoCatalogoService.listar(estado));
        model.addAttribute("filtroEstado", estado != null && !estado.isBlank() ? estado : "TODOS");
        model.addAttribute("pendientes", pedidoCatalogoRepository.countByEstado(PedidoCatalogo.Estado.PENDIENTE));
        model.addAttribute("atendidos", pedidoCatalogoRepository.countByEstado(PedidoCatalogo.Estado.ATENDIDO));
        model.addAttribute("cancelados", pedidoCatalogoRepository.countByEstado(PedidoCatalogo.Estado.CANCELADO));
        return "pedidos/list";
    }

    @GetMapping(value = "/api/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> detalle(@PathVariable Long id) {
        return pedidoCatalogoService.obtenerDetalle(id);
    }

    @GetMapping(value = "/api/{id}/venta", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> venta(@PathVariable Long id) {
        return pedidoCatalogoService.obtenerVentaPedido(id);
    }

    @PostMapping(value = "/api/{id}/atender", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> atender(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pedidoCatalogoService.atenderPedido(id));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo atender el pedido.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }

    @PostMapping(value = "/api/{id}/comprobante", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarComprobante(@PathVariable Long id,
                                                                     @RequestPart("comprobante") MultipartFile comprobante) {
        try {
            return ResponseEntity.ok(pedidoCatalogoService.actualizarComprobante(id, comprobante));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo actualizar el comprobante.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }

    @PostMapping(value = "/api/{id}/cancelar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(pedidoCatalogoService.cancelarPedido(id));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo cancelar el pedido.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }
}
