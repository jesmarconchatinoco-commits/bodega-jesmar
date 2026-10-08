package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.Models.TipoComprobante;
import com.test.desarrollo_web.Models.Ventas;
import com.test.desarrollo_web.Repository.TipoComprobanteRepository;
import com.test.desarrollo_web.Repository.VentaRepository;
import com.test.desarrollo_web.service.ConfiguracionPagoService;
import com.test.desarrollo_web.service.VentaService;
import com.test.desarrollo_web.service.PermissionService;
import com.test.desarrollo_web.service.export.VentaExcelExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final VentaRepository ventaRepository;
    private final TipoComprobanteRepository tipoComprobanteRepository;
    private final VentaExcelExportService ventaExcelExportService;
    private final PermissionService permissionService;
    private final ConfiguracionPagoService configuracionPagoService;

    public VentaController(VentaService ventaService,
                           VentaRepository ventaRepository,
                           TipoComprobanteRepository tipoComprobanteRepository,
                           VentaExcelExportService ventaExcelExportService,
                           PermissionService permissionService,
                           ConfiguracionPagoService configuracionPagoService) {
        this.ventaService = ventaService;
        this.ventaRepository = ventaRepository;
        this.tipoComprobanteRepository = tipoComprobanteRepository;
        this.ventaExcelExportService = ventaExcelExportService;
        this.permissionService = permissionService;
        this.configuracionPagoService = configuracionPagoService;
    }

    @GetMapping
    public String listar(Model model) {
        List<Ventas> ventas;
        try {
            ventas = ventaService.listarVentas();
        } catch (Exception e) {
            ventas = ventaRepository.findAll();
            model.addAttribute("error", "No se pudieron cargar todos los detalles de ventas. Se muestra el listado básico.");
        }

        List<TipoComprobante> comprobantes = tipoComprobanteRepository.findByEstado(TipoComprobante.Estado.ACTIVO);
        if (comprobantes.isEmpty()) {
            comprobantes = tipoComprobanteRepository.findAll();
        }

        model.addAttribute("ventas", ventas);
        model.addAttribute("tiposPago", ventaService.listarTiposPagoPos());
        model.addAttribute("tiposComprobante", comprobantes);
        model.addAttribute("pagoConfig", configuracionPagoService.obtenerConfigPos());
        java.util.Map<Long, String> origenesVenta = new java.util.LinkedHashMap<>();
        for (Ventas v : ventas) {
            origenesVenta.put(v.getId(), ventaService.resolverEtiquetaOrigen(v));
        }
        model.addAttribute("origenesVenta", origenesVenta);
        return "ventas/list";
    }

    @GetMapping("/api/comprobante/{id}/siguiente")
    @ResponseBody
    public Map<String, Object> siguienteNumero(@PathVariable Integer id) {
        return ventaService.obtenerSiguienteNumero(id);
    }

    @GetMapping("/api/cliente/local")
    @ResponseBody
    public Map<String, Object> buscarClienteLocal(@RequestParam String documento) {
        return ventaService.buscarClienteEnBodega(documento);
    }

    @GetMapping("/api/cliente/buscar")
    @ResponseBody
    public Map<String, Object> buscarCliente(@RequestParam String documento,
                                             @RequestParam(required = false) String nombre) {
        return ventaService.buscarOCrearCliente(documento, nombre);
    }

    @GetMapping("/api/productos")
    @ResponseBody
    public List<Map<String, Object>> productos() {
        return ventaService.listarProductosActivos();
    }

    @GetMapping("/api/categorias")
    @ResponseBody
    public List<Map<String, Object>> categoriasVenta() {
        return ventaService.listarCategoriasParaVenta();
    }

    @PostMapping(value = "/guardar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> guardar(
            @RequestParam(name = "clienteId") Integer clienteId,
            @RequestParam(name = "tipoComprobanteId") Integer tipoComprobanteId,
            @RequestParam(name = "tipoPagoId") Integer tipoPagoId,
            @RequestParam(name = "ventaCredito", defaultValue = "false") boolean ventaCredito,
            @RequestParam(name = "pagoInicial", required = false) BigDecimal pagoInicial,
            @RequestParam(name = "numeroCuotas", required = false) Integer numeroCuotas,
            @RequestParam(name = "intervaloDias", required = false) Integer intervaloDias,
            @RequestParam(name = "detalles") String detalles,
            @RequestParam(name = "fechasCuotas", required = false) String fechasCuotas,
            @RequestParam(name = "codigoVerificacionPago", required = false) String codigoVerificacionPago,
            @RequestParam(name = "montoEfectivo", required = false) BigDecimal montoEfectivo,
            @RequestParam(name = "montoYape", required = false) BigDecimal montoYape,
            @RequestParam(name = "montoPlin", required = false) BigDecimal montoPlin) {
        try {
            ventaService.guardarVenta(clienteId, tipoComprobanteId, tipoPagoId, ventaCredito,
                    pagoInicial, numeroCuotas, intervaloDias, detalles, fechasCuotas,
                    codigoVerificacionPago, montoEfectivo, montoYape, montoPlin);
            return ResponseEntity.ok(Map.of("success", true, "message", "Venta guardada correctamente."));
        } catch (Exception e) {
            String mensaje = e.getMessage();
            if (mensaje == null || mensaje.isBlank()) {
                mensaje = "No se pudo guardar la venta. Verifique cliente, productos y stock.";
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", mensaje));
        }
    }

    @GetMapping("/api/{id}/cuotas")
    @ResponseBody
    public Map<String, Object> cuotas(@PathVariable Long id) {
        return ventaService.obtenerCuotasVenta(id);
    }

    @PostMapping(value = "/api/cuotas/{id}/pagar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pagarCuota(@PathVariable Long id,
                                                          @RequestParam(defaultValue = "EFECTIVO") String medioPago,
                                                          @RequestParam(required = false) String referenciaPago) {
        try {
            return ResponseEntity.ok(ventaService.pagarCuota(id, medioPago, referenciaPago));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo registrar el pago.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }

    @GetMapping("/api/{id}/historial-pagos")
    @ResponseBody
    public Map<String, Object> historialPagos(@PathVariable Long id) {
        return ventaService.obtenerHistorialPagos(id);
    }

    @GetMapping("/api/{id}/detalle")
    @ResponseBody
    public Map<String, Object> detalle(@PathVariable Long id) {
        return ventaService.obtenerDetalleVenta(id);
    }

    @GetMapping("/api/{id}/editar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> editar(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ventaService.obtenerVentaParaEditar(id));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo cargar la venta.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }

    @PostMapping(value = "/actualizar", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizar(
            @RequestParam(name = "ventaId") Long ventaId,
            @RequestParam(name = "clienteId") Integer clienteId,
            @RequestParam(name = "tipoPagoId") Integer tipoPagoId,
            @RequestParam(name = "ventaCredito", defaultValue = "false") boolean ventaCredito,
            @RequestParam(name = "pagoInicial", required = false) BigDecimal pagoInicial,
            @RequestParam(name = "numeroCuotas", required = false) Integer numeroCuotas,
            @RequestParam(name = "intervaloDias", required = false) Integer intervaloDias,
            @RequestParam(name = "detalles") String detalles,
            @RequestParam(name = "fechasCuotas", required = false) String fechasCuotas,
            @RequestParam(name = "codigoVerificacionPago", required = false) String codigoVerificacionPago,
            @RequestParam(name = "montoEfectivo", required = false) BigDecimal montoEfectivo,
            @RequestParam(name = "montoYape", required = false) BigDecimal montoYape,
            @RequestParam(name = "montoPlin", required = false) BigDecimal montoPlin) {
        try {
            ventaService.actualizarVenta(ventaId, clienteId, tipoPagoId, ventaCredito,
                    pagoInicial, numeroCuotas, intervaloDias, detalles, fechasCuotas,
                    codigoVerificacionPago, montoEfectivo, montoYape, montoPlin);
            return ResponseEntity.ok(Map.of("success", true, "message", "Venta actualizada correctamente."));
        } catch (Exception e) {
            String mensaje = e.getMessage();
            if (mensaje == null || mensaje.isBlank()) {
                mensaje = "No se pudo actualizar la venta.";
            }
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", mensaje));
        }
    }

    @GetMapping("/api/inventario")
    @ResponseBody
    public List<Map<String, Object>> inventario() {
        return ventaService.listarInventario();
    }

    @GetMapping("/api/inventario/export/excel")
    public void exportarInventarioExcel(HttpServletResponse response) throws IOException {
        String csv = ventaService.generarCsvInventario();
        String filename = "inventario.csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping(value = "/api/inventario/presentaciones/{presentacionId}/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarStockPresentacion(@PathVariable Integer presentacionId,
                                                                           @RequestParam Integer stock) {
        try {
            return ResponseEntity.ok(ventaService.actualizarStockPresentacion(presentacionId, stock));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "No se pudo actualizar el stock.";
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
        }
    }

    @GetMapping("/api/inventario/presentaciones/{presentacionId}/movimientos")
    @ResponseBody
    public Map<String, Object> movimientosPresentacion(@PathVariable Integer presentacionId) {
        return ventaService.movimientosPresentacion(presentacionId);
    }

    @GetMapping("/api/inventario/presentaciones/{presentacionId}/movimientos/export/excel")
    public void exportarMovimientosExcel(@PathVariable Integer presentacionId, HttpServletResponse response) throws IOException {
        String csv = ventaService.generarCsvMovimientos(presentacionId);
        String filename = "movimientos-presentacion-" + presentacionId + ".csv";
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/api/inventario/presentaciones/{presentacionId}/movimientos/imprimir")
    public String imprimirMovimientos(@PathVariable Integer presentacionId, Model model) {
        model.addAttribute("data", ventaService.movimientosPresentacion(presentacionId));
        return "ventas/movimientos-imprimir";
    }

    @GetMapping("/{id}/imprimir")
    public String imprimir(@PathVariable Long id, Model model) {
        model.addAttribute("venta", ventaService.obtenerVentaParaImprimir(id));
        return "ventas/imprimir";
    }

    @GetMapping("/{id}/export/excel")
    public void exportarExcel(@PathVariable Long id, HttpServletResponse response) throws IOException {
        Ventas venta = ventaService.obtenerVentaParaImprimir(id);
        String usuario = permissionService.getUsuarioActual() != null
                ? permissionService.getUsuarioActual().getNombre()
                : "Sistema";
        byte[] data = ventaExcelExportService.exportarDetalleVenta(venta, usuario);
        String filename = "venta-" + venta.getNumeroDocumento().replace("/", "-") + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ventaService.anularVenta(id);
            redirectAttributes.addFlashAttribute("success", "Venta anulada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ventas";
    }
}
