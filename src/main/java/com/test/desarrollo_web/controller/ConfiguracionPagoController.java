package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.service.ConfiguracionPagoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/config-pagos")
public class ConfiguracionPagoController {

    private final ConfiguracionPagoService configuracionPagoService;

    public ConfiguracionPagoController(ConfiguracionPagoService configuracionPagoService) {
        this.configuracionPagoService = configuracionPagoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("configuraciones", configuracionPagoService.listarAdmin());
        return "config-pagos/list";
    }

    @GetMapping(value = "/api/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> apiListar() {
        return configuracionPagoService.listarAdmin();
    }

    @PostMapping(value = "/api/guardar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardar(@RequestParam(required = false) Integer id,
                                                        @RequestParam String canal,
                                                        @RequestParam String celular,
                                                        @RequestParam String nombreTitular,
                                                        @RequestParam(required = false) String textoQr,
                                                        @RequestParam(defaultValue = "ACTIVO") String estado,
                                                        @RequestParam(required = false) MultipartFile imagenQr) {
        try {
            return ResponseEntity.ok(configuracionPagoService.guardar(
                    id, canal, celular, nombreTitular, textoQr, estado, imagenQr));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo guardar la configuración."
            ));
        }
    }
}
