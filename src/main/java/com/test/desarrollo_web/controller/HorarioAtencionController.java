package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.dto.FechaAtencionRequest;
import com.test.desarrollo_web.service.HorarioAtencionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/horarios-atencion")
public class HorarioAtencionController {

    private final HorarioAtencionService horarioAtencionService;

    public HorarioAtencionController(HorarioAtencionService horarioAtencionService) {
        this.horarioAtencionService = horarioAtencionService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("fechasAtencion", horarioAtencionService.listarAdmin());
        return "horarios-atencion/list";
    }

    @GetMapping(value = "/api/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> apiListar() {
        return horarioAtencionService.listarAdmin();
    }

    @PostMapping(value = "/api/fecha", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crear(@RequestBody FechaAtencionRequest request) {
        try {
            return ResponseEntity.ok(horarioAtencionService.crearFecha(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo registrar la fecha."
            ));
        }
    }

    @PutMapping(value = "/api/fecha/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(@PathVariable Long id,
                                                          @RequestBody FechaAtencionRequest request) {
        try {
            return ResponseEntity.ok(horarioAtencionService.actualizarFecha(id, request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo actualizar la fecha."
            ));
        }
    }

    @DeleteMapping(value = "/api/fecha/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(horarioAtencionService.eliminarFecha(id));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo eliminar la fecha."
            ));
        }
    }

    @PatchMapping(value = "/api/fecha/{id}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleFecha(@PathVariable Long id,
                                                           @RequestParam boolean activo) {
        try {
            return ResponseEntity.ok(horarioAtencionService.toggleFecha(id, activo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo actualizar la fecha."
            ));
        }
    }

    @PatchMapping(value = "/api/horario/{id}/toggle", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> toggleHorario(@PathVariable Long id,
                                                             @RequestParam boolean activo) {
        try {
            return ResponseEntity.ok(horarioAtencionService.toggleHorario(id, activo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage() != null ? e.getMessage() : "No se pudo actualizar el horario."
            ));
        }
    }
}
