package com.test.desarrollo_web.controller;

import com.test.desarrollo_web.dto.ReporteFiltroDto;
import com.test.desarrollo_web.dto.ReporteVentasDocumentoDto;
import com.test.desarrollo_web.service.LogoService;
import com.test.desarrollo_web.service.ReporteHistorialService;
import com.test.desarrollo_web.service.ReporteService;
import com.test.desarrollo_web.service.ReporteVentasService;
import com.test.desarrollo_web.service.export.ReporteExcelExportService;
import com.test.desarrollo_web.service.export.ReportePdfExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private static final Set<String> TIPOS_VENTAS = Set.of("GENERAL", "POS", "WEB");

    private final ReporteService reporteService;
    private final ReporteVentasService reporteVentasService;
    private final ReporteHistorialService reporteHistorialService;
    private final ReportePdfExportService pdfExportService;
    private final ReporteExcelExportService excelExportService;
    private final LogoService logoService;

    public ReporteController(ReporteService reporteService,
                             ReporteVentasService reporteVentasService,
                             ReporteHistorialService reporteHistorialService,
                             ReportePdfExportService pdfExportService,
                             ReporteExcelExportService excelExportService,
                             LogoService logoService) {
        this.reporteService = reporteService;
        this.reporteVentasService = reporteVentasService;
        this.reporteHistorialService = reporteHistorialService;
        this.pdfExportService = pdfExportService;
        this.excelExportService = excelExportService;
        this.logoService = logoService;
    }

    @GetMapping
    public String index(Model model) {
        reporteService.validarAcceso();
        model.addAttribute("logoUrl", logoService.getLogoUrl().orElse(null));
        model.addAttribute("empresaNombre", ReporteVentasService.EMPRESA_NOMBRE);
        model.addAttribute("fechaGeneracion", LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        return "reportes/list";
    }

    @GetMapping(value = "/api/filtros", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> filtros() {
        return reporteService.obtenerCatalogoFiltros();
    }

    @PostMapping(value = "/api/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> dashboard(@RequestBody ReporteFiltroDto filtro) {
        return reporteService.obtenerDashboard(filtro);
    }

    @PostMapping(value = "/api/consultar/{tipo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> consultar(@PathVariable String tipo, @RequestBody ReporteFiltroDto filtro) {
        String tipoNorm = normalizarTipoPath(tipo);
        Map<String, Object> resultado = reporteService.consultar(tipoNorm, filtro);
        reporteHistorialService.registrarSeguro(tipoNorm, "CONSULTA", null, filtro);
        return resultado;
    }

    @PostMapping(value = "/api/exportar/{tipo}/pdf")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable String tipo, @RequestBody ReporteFiltroDto filtro) {
        reporteService.validarAcceso();
        String tipoNorm = normalizarTipoPath(tipo);
        validarTipoVentas(tipoNorm);
        ReporteVentasDocumentoDto doc = reporteVentasService.generarDocumento(tipoNorm, filtro);
        byte[] data = pdfExportService.exportar(doc);
        reporteHistorialService.registrarSeguro(tipoNorm, "EXPORTAR", "PDF", filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-" + tipoNorm.toLowerCase() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @PostMapping(value = "/api/exportar/{tipo}/excel")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable String tipo, @RequestBody ReporteFiltroDto filtro) {
        reporteService.validarAcceso();
        String tipoNorm = normalizarTipoPath(tipo);
        validarTipoVentas(tipoNorm);
        ReporteVentasDocumentoDto doc = reporteVentasService.generarDocumento(tipoNorm, filtro);
        byte[] data = excelExportService.exportar(doc);
        reporteHistorialService.registrarSeguro(tipoNorm, "EXPORTAR", "EXCEL", filtro);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"reporte-" + tipoNorm.toLowerCase() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping(value = "/api/detalle-venta/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> detalleVenta(@PathVariable Long id) {
        return reporteService.obtenerDetalleVenta(id);
    }

    private static String normalizarTipoPath(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "GENERAL";
        }
        return tipo.trim().toUpperCase();
    }

    private void validarTipoVentas(String tipo) {
        if (!TIPOS_VENTAS.contains(tipo)) {
            throw new RuntimeException("La exportación PDF/Excel solo está disponible para reportes de ventas.");
        }
    }
}
