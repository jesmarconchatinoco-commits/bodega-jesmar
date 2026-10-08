package com.test.desarrollo_web.service.export;

import com.test.desarrollo_web.dto.ReporteVentaFilaDto;
import com.test.desarrollo_web.dto.ReporteVentasDocumentoDto;
import com.test.desarrollo_web.dto.ReporteVentasResumenDto;
import com.test.desarrollo_web.service.LogoService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Optional;

@Service
public class ReporteExcelExportService {

    private final LogoService logoService;

    public ReporteExcelExportService(LogoService logoService) {
        this.logoService = logoService;
    }

    public byte[] exportar(ReporteVentasDocumentoDto doc) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Reporte de Ventas");
            int rowIdx = 0;

            insertarLogo(wb, sheet);
            rowIdx = 4;

            CellStyle tituloStyle = estiloTitulo(wb);
            CellStyle metaStyle = estiloMeta(wb);
            CellStyle headerStyle = estiloEncabezado(wb);
            CellStyle dataStyle = estiloDatos(wb);
            CellStyle moneyStyle = estiloMoneda(wb);
            CellStyle resumenStyle = estiloResumen(wb);

            Row r0 = sheet.createRow(rowIdx++);
            Cell c0 = r0.createCell(0);
            c0.setCellValue(doc.getEmpresa());
            c0.setCellStyle(tituloStyle);
            sheet.addMergedRegion(new CellRangeAddress(r0.getRowNum(), r0.getRowNum(), 0, 13));

            Row r1 = sheet.createRow(rowIdx++);
            Cell c1 = r1.createCell(0);
            c1.setCellValue(doc.getTitulo());
            c1.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(r1.getRowNum(), r1.getRowNum(), 0, 13));

            Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(0).setCellValue("Generado: " + doc.getFechaGeneracion());
            r2.createCell(7).setCellValue("Usuario: " + doc.getUsuarioGenerador());

            Row r3 = sheet.createRow(rowIdx++);
            r3.createCell(0).setCellValue("Rango: " + doc.getRangoFechas());
            r3.createCell(7).setCellValue("Registros: " + doc.getFilas().size());
            rowIdx++;

            String[] headers = {"Fecha", "Hora", "Tipo", "Serie", "N° Comprobante", "Cliente", "Documento",
                    "Vendedor", "Método pago", "Cód. verificación", "Estado", "Subtotal", "IGV", "Descuento", "Total"};
            Row headRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (ReporteVentaFilaDto f : doc.getFilas()) {
                Row row = sheet.createRow(rowIdx++);
                crearTexto(row, 0, f.getFecha(), dataStyle);
                crearTexto(row, 1, f.getHora(), dataStyle);
                crearTexto(row, 2, f.getTipoVenta(), dataStyle);
                crearTexto(row, 3, f.getSerie(), dataStyle);
                crearTexto(row, 4, f.getNumeroComprobante(), dataStyle);
                crearTexto(row, 5, f.getCliente(), dataStyle);
                crearTexto(row, 6, f.getDocumento(), dataStyle);
                crearTexto(row, 7, f.getVendedor(), dataStyle);
                crearTexto(row, 8, f.getMetodoPago(), dataStyle);
                crearTexto(row, 9, f.getCodigoVerificacion(), dataStyle);
                crearTexto(row, 10, f.getEstado(), dataStyle);
                crearMoneda(row, 11, f.getSubtotal(), moneyStyle);
                crearMoneda(row, 12, f.getIgv(), moneyStyle);
                crearMoneda(row, 13, f.getDescuento(), moneyStyle);
                crearMoneda(row, 14, f.getTotal(), moneyStyle);
            }

            rowIdx++;
            ReporteVentasResumenDto res = doc.getResumen();
            Row rs = sheet.createRow(rowIdx++);
            Cell rsCell = rs.createCell(0);
            rsCell.setCellValue("RESUMEN — Ventas: " + res.getTotalVentas()
                    + " | Comprobantes: " + res.getCantidadComprobantes()
                    + " | Clientes: " + res.getTotalClientes()
                    + " | Total: S/ " + n(res.getTotalVendido())
                    + " | Descuentos: S/ " + n(res.getTotalDescuentos())
                    + " | Impuestos: S/ " + n(res.getTotalImpuestos())
                    + " | Promedio: S/ " + n(res.getPromedioVenta()));
            rsCell.setCellStyle(resumenStyle);
            sheet.addMergedRegion(new CellRangeAddress(rs.getRowNum(), rs.getRowNum(), 0, 14));

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 12000));
            }

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel: " + e.getMessage(), e);
        }
    }

    private void insertarLogo(XSSFWorkbook wb, Sheet sheet) {
        Optional<Path> logo = logoService.getLogoFilePath();
        if (logo.isEmpty()) {
            return;
        }
        try {
            Path path = logo.get();
            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            int pictureType = detectarTipoImagen(path, bytes);
            int pictureIdx = wb.addPicture(bytes, pictureType);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 0, 0, 2, 3);
            drawing.createPicture(anchor, pictureIdx);
        } catch (Exception ignored) {
            // Logo opcional
        }
    }

    private int detectarTipoImagen(Path path, byte[] bytes) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        if (name.endsWith(".png")) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) {
            return Workbook.PICTURE_TYPE_JPEG;
        }
        if (bytes.length >= 4 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_PNG;
    }

    private void crearTexto(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "—");
        cell.setCellStyle(style);
    }

    private void crearMoneda(Row row, int col, BigDecimal val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val.doubleValue() : 0d);
        cell.setCellStyle(style);
    }

    private String n(BigDecimal v) {
        return v != null ? v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private CellStyle estiloTitulo(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.VIOLET.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle estiloMeta(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle estiloEncabezado(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle estiloDatos(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle estiloMoneda(Workbook wb) {
        CellStyle style = estiloDatos(wb);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("\"S/ \"#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private CellStyle estiloResumen(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
