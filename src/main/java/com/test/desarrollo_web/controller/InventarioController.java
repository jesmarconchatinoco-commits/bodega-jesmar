package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.dto.CompraRequestDto;
import com.test.desarrollo_web.dto.MovimientoInventarioRequestDto;
import com.test.desarrollo_web.dto.ProveedorRequestDto;
import com.test.desarrollo_web.service.InventarioService;
import com.test.desarrollo_web.service.LogoService;
import com.test.desarrollo_web.service.PermissionService;
import com.test.desarrollo_web.service.export.InventarioExcelExportService;
import com.test.desarrollo_web.service.export.InventarioPdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final PermissionService permissionService;
    private final InventarioPdfExportService pdfExportService;
    private final InventarioExcelExportService excelExportService;
    private final LogoService logoService;

    public InventarioController(InventarioService inventarioService,
                                  PermissionService permissionService,
                                  InventarioPdfExportService pdfExportService,
                                  InventarioExcelExportService excelExportService,
                                  LogoService logoService) {
        this.inventarioService = inventarioService;
        this.permissionService = permissionService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
        this.logoService = logoService;
    }

    @GetMapping
    public String index(Model model) {
        inventarioService.validarAcceso();
        model.addAttribute("logoUrl", logoService.getLogoUrl().orElse(null));
        model.addAttribute("fechaGeneracion", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return "inventario/list";
    }

    @GetMapping(value = "/api/resumen", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> resumen() {
        inventarioService.validarAcceso();
        return inventarioService.obtenerResumen();
    }

    @GetMapping(value = "/api/lista", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> lista(@RequestParam(required = false) String busqueda,
                                           @RequestParam(required = false) Integer categoriaId,
                                           @RequestParam(required = false) String estadoStock) {
        inventarioService.validarAcceso();
        return inventarioService.listarInventario(busqueda, categoriaId, estadoStock);
    }

    @GetMapping(value = "/api/categorias", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> categorias() {
        inventarioService.validarAcceso();
        return inventarioService.listarCategorias();
    }

    @GetMapping(value = "/api/presentaciones", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> presentaciones() {
        inventarioService.validarAcceso();
        return inventarioService.listarPresentacionesParaCompra();
    }

    @GetMapping(value = "/api/proveedores", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> proveedores() {
        inventarioService.validarAcceso();
        return inventarioService.listarProveedores();
    }

    @PostMapping(value = "/api/proveedores", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> registrarProveedor(@RequestBody ProveedorRequestDto dto) {
        inventarioService.validarAcceso();
        return inventarioService.registrarProveedor(dto);
    }

    @PostMapping(value = "/api/compras", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> registrarCompra(@RequestBody CompraRequestDto dto) {
        inventarioService.validarAcceso();
        return inventarioService.registrarCompra(dto);
    }

    @PostMapping(value = "/api/movimientos/ingreso", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> ingresoManual(@RequestBody MovimientoInventarioRequestDto dto) {
        inventarioService.validarAcceso();
        return inventarioService.registrarIngresoManual(dto);
    }

    @PostMapping(value = "/api/movimientos/salida", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> salida(@RequestBody MovimientoInventarioRequestDto dto) {
        inventarioService.validarAcceso();
        return inventarioService.registrarSalida(dto);
    }

    @PostMapping(value = "/api/movimientos/ajuste", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> ajuste(@RequestBody MovimientoInventarioRequestDto dto) {
        inventarioService.validarAcceso();
        return inventarioService.registrarAjuste(dto);
    }

    @GetMapping(value = "/api/movimientos", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> movimientos() {
        inventarioService.validarAcceso();
        return inventarioService.historialMovimientos();
    }

    @GetMapping(value = "/api/movimientos/{presentacionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> kardex(@PathVariable Integer presentacionId) {
        inventarioService.validarAcceso();
        return inventarioService.kardexPresentacion(presentacionId);
    }

    @GetMapping(value = "/api/productos-gestion", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> productosGestion() {
        inventarioService.validarAcceso();
        return inventarioService.listarProductosGestion();
    }

    @GetMapping(value = "/api/presentaciones/{presentacionId}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> stockPresentacion(@PathVariable Integer presentacionId) {
        inventarioService.validarAcceso();
        return inventarioService.obtenerStockPresentacion(presentacionId);
    }

    @PostMapping(value = "/api/exportar/pdf")
    public ResponseEntity<byte[]> exportarPdf(@RequestParam(required = false) String busqueda,
                                              @RequestParam(required = false) Integer categoriaId,
                                              @RequestParam(required = false) String estadoStock) {
        inventarioService.validarAcceso();
        List<Map<String, Object>> filas = inventarioService.listarInventario(busqueda, categoriaId, estadoStock);
        Map<String, Object> resumen = inventarioService.obtenerResumen();
        String usuario = nombreUsuario();
        byte[] data = pdfExportService.exportarInventario(filas, resumen, usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventario.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping(value = "/api/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel(@RequestParam(required = false) String busqueda,
                                                @RequestParam(required = false) Integer categoriaId,
                                                @RequestParam(required = false) String estadoStock) {
        inventarioService.validarAcceso();
        List<Map<String, Object>> filas = inventarioService.listarInventario(busqueda, categoriaId, estadoStock);
        Map<String, Object> resumen = inventarioService.obtenerResumen();
        String usuario = nombreUsuario();
        byte[] data = excelExportService.exportarInventario(filas, resumen, usuario);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"inventario.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private String nombreUsuario() {
        var usuario = permissionService.getUsuarioActual();
        return usuario != null ? usuario.getNombre() : "Sistema";
    }
}
