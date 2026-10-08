package com.test.desarrollo_web.service.export;

import com.test.desarrollo_web.service.LogoService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InventarioExcelExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LogoService logoService;

    public InventarioExcelExportService(LogoService logoService) {
        this.logoService = logoService;
    }

    public byte[] exportarInventario(List<Map<String, Object>> filas,
                                     Map<String, Object> resumen,
                                     String usuario) {
        return generar("Inventario", filas, resumen, usuario, new String[]{
                "Producto", "Categoría", "Presentación", "Código", "Stock", "Stock mín.",
                "Precio compra", "Precio venta", "Estado"
        }, (row, f, styles) -> {
            crearTexto(row, 0, str(f.get("producto")), styles.data);
            crearTexto(row, 1, str(f.get("categoria")), styles.data);
            crearTexto(row, 2, str(f.get("presentacion")), styles.data);
            crearTexto(row, 3, str(f.get("codigo")), styles.data);
            crearNumero(row, 4, f.get("stock"), styles.data);
            crearNumero(row, 5, f.get("stockMinimo"), styles.data);
            crearTexto(row, 6, str(f.get("precioCompra")), styles.data);
            crearTexto(row, 7, str(f.get("precioVenta")), styles.data);
            crearTexto(row, 8, str(f.get("estadoLabel")), styles.data);
        });
    }

    public byte[] exportarMovimientos(List<Map<String, Object>> movimientos, String usuario) {
        Map<String, Object> resumen = Map.of(
                "totalProductos", "—",
                "totalPresentaciones", movimientos.size(),
                "stockBajo", "—",
                "sinStock", "—",
                "valorInventario", "—"
        );
        return generar("Movimientos", movimientos, resumen, usuario, new String[]{
                "Fecha", "Producto", "Presentación", "Tipo", "Cantidad",
                "Stock anterior", "Stock nuevo", "Referencia", "Proveedor", "Usuario", "Observación"
        }, (row, m, styles) -> {
            crearTexto(row, 0, str(m.get("fecha")), styles.data);
            crearTexto(row, 1, str(m.get("producto")), styles.data);
            crearTexto(row, 2, str(m.get("presentacion")), styles.data);
            crearTexto(row, 3, str(m.get("tipo")), styles.data);
            crearNumero(row, 4, m.get("cantidad"), styles.data);
            crearNumero(row, 5, m.get("stockAnterior"), styles.data);
            crearNumero(row, 6, m.get("stockNuevo"), styles.data);
            crearTexto(row, 7, str(m.get("referencia")), styles.data);
            crearTexto(row, 8, str(m.get("proveedor")), styles.data);
            crearTexto(row, 9, str(m.get("usuario")), styles.data);
            crearTexto(row, 10, str(m.get("observacion")), styles.data);
        });
    }

    private byte[] generar(String titulo,
                           List<Map<String, Object>> filas,
                           Map<String, Object> resumen,
                           String usuario,
                           String[] headers,
                           FilaWriter writer) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(titulo);
            insertarLogo(wb, sheet);
            Estilos estilos = crearEstilos(wb);
            int rowIdx = 4;

            Row r0 = sheet.createRow(rowIdx++);
            Cell c0 = r0.createCell(0);
            c0.setCellValue("Bodega Jesmar");
            c0.setCellStyle(estilos.titulo);
            sheet.addMergedRegion(new CellRangeAddress(r0.getRowNum(), r0.getRowNum(), 0, headers.length - 1));

            Row r1 = sheet.createRow(rowIdx++);
            r1.createCell(0).setCellValue("Gestión de Inventario — " + titulo);
            Row r2 = sheet.createRow(rowIdx++);
            r2.createCell(0).setCellValue("Generado: " + LocalDateTime.now().format(FMT));
            r2.createCell(4).setCellValue("Usuario: " + usuario);
            Row r3 = sheet.createRow(rowIdx++);
            r3.createCell(0).setCellValue("Resumen: Productos " + resumen.get("totalProductos")
                    + " | Presentaciones " + resumen.get("totalPresentaciones")
                    + " | Stock bajo " + resumen.get("stockBajo")
                    + " | Sin stock " + resumen.get("sinStock")
                    + " | Valor " + resumen.get("valorInventario"));
            rowIdx++;

            Row head = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = head.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(estilos.header);
            }

            for (Map<String, Object> fila : filas) {
                Row row = sheet.createRow(rowIdx++);
                writer.escribir(row, fila, estilos);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 512, 14000));
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
            byte[] bytes = java.nio.file.Files.readAllBytes(logo.get());
            int type = logo.get().getFileName().toString().toLowerCase().endsWith(".png")
                    ? Workbook.PICTURE_TYPE_PNG : Workbook.PICTURE_TYPE_JPEG;
            int idx = wb.addPicture(bytes, type);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 0, 0, 2, 3);
            drawing.createPicture(anchor, idx);
        } catch (Exception ignored) {
            // Logo opcional
        }
    }

    private Estilos crearEstilos(Workbook wb) {
        Estilos e = new Estilos();
        Font tituloFont = wb.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 14);
        tituloFont.setColor(IndexedColors.VIOLET.getIndex());
        e.titulo = wb.createCellStyle();
        e.titulo.setFont(tituloFont);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        e.header = wb.createCellStyle();
        e.header.setFont(headerFont);
        e.header.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        e.header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        e.header.setAlignment(HorizontalAlignment.CENTER);
        e.header.setBorderTop(BorderStyle.THIN);
        e.header.setBorderBottom(BorderStyle.THIN);
        e.header.setBorderLeft(BorderStyle.THIN);
        e.header.setBorderRight(BorderStyle.THIN);

        e.data = wb.createCellStyle();
        e.data.setBorderTop(BorderStyle.THIN);
        e.data.setBorderBottom(BorderStyle.THIN);
        e.data.setBorderLeft(BorderStyle.THIN);
        e.data.setBorderRight(BorderStyle.THIN);
        e.data.setVerticalAlignment(VerticalAlignment.CENTER);
        return e;
    }

    private void crearTexto(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val : "—");
        cell.setCellStyle(style);
    }

    private void crearNumero(Row row, int col, Object val, CellStyle style) {
        Cell cell = row.createCell(col);
        if (val instanceof Number n) {
            cell.setCellValue(n.doubleValue());
        } else {
            cell.setCellValue(val != null ? val.toString() : "0");
        }
        cell.setCellStyle(style);
    }

    private String str(Object val) {
        return val != null ? val.toString() : "—";
    }

    @FunctionalInterface
    private interface FilaWriter {
        void escribir(Row row, Map<String, Object> data, Estilos styles);
    }

    private static class Estilos {
        CellStyle titulo;
        CellStyle header;
        CellStyle data;
    }
}
