package com.test.desarrollo_web.service.export;

import com.test.desarrollo_web.Models.DetalleVenta;
import com.test.desarrollo_web.Models.Ventas;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class VentaExcelExportService {

    public static final String EMPRESA_NOMBRE = "Bodega Jesmar";
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_GEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LogoService logoService;

    public VentaExcelExportService(LogoService logoService) {
        this.logoService = logoService;
    }

    public byte[] exportarDetalleVenta(Ventas venta, String usuarioGenerador) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Detalle de Venta");
            Estilos estilos = crearEstilos(wb);

            insertarLogo(wb, sheet);
            int rowIdx = 4;

            rowIdx = escribirEncabezado(sheet, rowIdx, estilos);
            rowIdx = escribirMetaVenta(sheet, rowIdx, venta, usuarioGenerador, estilos);
            rowIdx = escribirTablaProductos(sheet, rowIdx, venta, estilos);
            escribirTotales(sheet, rowIdx, venta, estilos);

            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 768, 18000));
            }

            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar Excel de venta: " + e.getMessage(), e);
        }
    }

    private int escribirEncabezado(Sheet sheet, int rowIdx, Estilos estilos) {
        Row rEmpresa = sheet.createRow(rowIdx++);
        Cell cEmpresa = rEmpresa.createCell(0);
        cEmpresa.setCellValue(EMPRESA_NOMBRE);
        cEmpresa.setCellStyle(estilos.titulo);
        sheet.addMergedRegion(new CellRangeAddress(rEmpresa.getRowNum(), rEmpresa.getRowNum(), 0, 3));

        Row rTitulo = sheet.createRow(rowIdx++);
        Cell cTitulo = rTitulo.createCell(0);
        cTitulo.setCellValue("Detalle de Venta");
        cTitulo.setCellStyle(estilos.subtitulo);
        sheet.addMergedRegion(new CellRangeAddress(rTitulo.getRowNum(), rTitulo.getRowNum(), 0, 3));

        rowIdx++;
        return rowIdx;
    }

    private int escribirMetaVenta(Sheet sheet, int rowIdx, Ventas venta, String usuario, Estilos estilos) {
        String fechaVenta = venta.getFecha() != null ? venta.getFecha().format(FMT_FECHA) : "—";
        String cliente = venta.getCliente() != null ? venta.getCliente().getNombre() : "—";
        String docCliente = venta.getCliente() != null && venta.getCliente().getDocumento() != null
                ? venta.getCliente().getDocumento() : "";
        if (!docCliente.isBlank()) {
            cliente = cliente + " (" + docCliente + ")";
        }

        String origen = venta.getOrigenVenta() != null
                ? (venta.getOrigenVenta() == Ventas.OrigenVenta.WEB ? "Web" : "POS")
                : "POS";
        String codigo = venta.getCodigoVerificacionPago() != null && !venta.getCodigoVerificacionPago().isBlank()
                ? venta.getCodigoVerificacionPago() : "—";

        String[][] meta = {
                {"Documento", nvl(venta.getNumeroDocumento())},
                {"Fecha", fechaVenta},
                {"Origen de la venta", origen},
                {"Cliente", cliente},
                {"Vendedor", venta.getVendedor() != null ? venta.getVendedor().getNombre() : "—"},
                {"Forma de pago", venta.getTipoPago() != null ? venta.getTipoPago().getNombre() : "—"},
                {"Cód. verificación", codigo},
                {"Comprobante", venta.getTipoComprobante() != null ? venta.getTipoComprobante().getNombre() : "—"},
                {"Estado", venta.getEstado() != null ? venta.getEstado().name() : "—"},
                {"Generado", LocalDateTime.now().format(FMT_GEN) + "  |  Usuario: " + nvl(usuario)}
        };

        for (String[] linea : meta) {
            Row row = sheet.createRow(rowIdx++);
            Cell label = row.createCell(0);
            label.setCellValue(linea[0] + ":");
            label.setCellStyle(estilos.metaLabel);
            Cell value = row.createCell(1);
            value.setCellValue(linea[1]);
            value.setCellStyle(estilos.metaValue);
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 3));
        }

        rowIdx++;
        return rowIdx;
    }

    private int escribirTablaProductos(Sheet sheet, int rowIdx, Ventas venta, Estilos estilos) {
        String[] headers = {"Producto", "Cantidad", "Precio unitario", "Subtotal"};
        Row head = sheet.createRow(rowIdx++);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = head.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(estilos.header);
        }

        if (venta.getDetalles() != null) {
            for (DetalleVenta d : venta.getDetalles()) {
                Row row = sheet.createRow(rowIdx++);
                crearTexto(row, 0, nombreProducto(d), estilos.data);
                crearNumero(row, 1, d.getCantidad(), estilos.dataCenter);
                crearMoneda(row, 2, d.getPrecioUnitario(), estilos.money);
                crearMoneda(row, 3, d.getSubtotal(), estilos.money);
            }
        }
        return rowIdx;
    }

    private void escribirTotales(Sheet sheet, int rowIdx, Ventas venta, Estilos estilos) {
        rowIdx++;
        if (venta.getPagoInicial() != null && venta.getPagoInicial().compareTo(BigDecimal.ZERO) > 0) {
            Row rPago = sheet.createRow(rowIdx++);
            crearTexto(rPago, 2, "Pago inicial:", estilos.totalLabel);
            crearMoneda(rPago, 3, venta.getPagoInicial(), estilos.money);
        }
        if (venta.getDeuda() != null && venta.getDeuda().compareTo(BigDecimal.ZERO) > 0) {
            Row rDeuda = sheet.createRow(rowIdx++);
            crearTexto(rDeuda, 2, "Deuda pendiente:", estilos.totalLabel);
            crearMoneda(rDeuda, 3, venta.getDeuda(), estilos.money);
        }
        Row rTotal = sheet.createRow(rowIdx);
        crearTexto(rTotal, 2, "TOTAL:", estilos.totalBold);
        crearMoneda(rTotal, 3, venta.getTotal(), estilos.totalBoldMoney);
    }

    private String nombreProducto(DetalleVenta d) {
        String nombre = d.getProducto() != null ? d.getProducto().getNombre() : "—";
        if (d.getPresentacion() != null && d.getPresentacion().getNombre() != null) {
            return nombre + " — " + d.getPresentacion().getNombre();
        }
        return nombre;
    }

    private void insertarLogo(XSSFWorkbook wb, Sheet sheet) {
        Optional<Path> logo = logoService.getLogoFilePath();
        if (logo.isEmpty()) {
            return;
        }
        try {
            Path path = logo.get();
            byte[] bytes = java.nio.file.Files.readAllBytes(path);
            int type = detectarTipoImagen(path, bytes);
            int idx = wb.addPicture(bytes, type);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, 0, 0, 1, 3);
            drawing.createPicture(anchor, idx);
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
        return Workbook.PICTURE_TYPE_PNG;
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

    private void crearMoneda(Row row, int col, BigDecimal val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val != null ? val.doubleValue() : 0d);
        cell.setCellStyle(style);
    }

    private String nvl(String val) {
        return val != null && !val.isBlank() ? val : "—";
    }

    private Estilos crearEstilos(Workbook wb) {
        Estilos e = new Estilos();
        DataFormat df = wb.createDataFormat();

        Font tituloFont = wb.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 16);
        tituloFont.setColor(IndexedColors.VIOLET.getIndex());
        e.titulo = wb.createCellStyle();
        e.titulo.setFont(tituloFont);

        Font subtituloFont = wb.createFont();
        subtituloFont.setBold(true);
        subtituloFont.setFontHeightInPoints((short) 12);
        e.subtitulo = wb.createCellStyle();
        e.subtitulo.setFont(subtituloFont);

        Font metaLabelFont = wb.createFont();
        metaLabelFont.setBold(true);
        e.metaLabel = estiloBase(wb);
        e.metaLabel.setFont(metaLabelFont);
        e.metaLabel.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        e.metaLabel.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        e.metaValue = estiloBase(wb);

        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        e.header = estiloBase(wb);
        e.header.setFont(headerFont);
        e.header.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        e.header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        e.header.setAlignment(HorizontalAlignment.CENTER);

        e.data = estiloBase(wb);
        e.dataCenter = estiloBase(wb);
        e.dataCenter.setAlignment(HorizontalAlignment.CENTER);

        e.money = estiloBase(wb);
        e.money.setDataFormat(df.getFormat("\"S/ \"#,##0.00"));
        e.money.setAlignment(HorizontalAlignment.RIGHT);

        Font totalFont = wb.createFont();
        totalFont.setBold(true);
        e.totalLabel = estiloBase(wb);
        e.totalLabel.setFont(totalFont);
        e.totalLabel.setAlignment(HorizontalAlignment.RIGHT);

        e.totalBold = estiloBase(wb);
        e.totalBold.setFont(totalFont);
        e.totalBold.setAlignment(HorizontalAlignment.RIGHT);
        e.totalBold.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        e.totalBold.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        e.totalBoldMoney = estiloBase(wb);
        e.totalBoldMoney.setFont(totalFont);
        e.totalBoldMoney.setDataFormat(df.getFormat("\"S/ \"#,##0.00"));
        e.totalBoldMoney.setAlignment(HorizontalAlignment.RIGHT);
        e.totalBoldMoney.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
        e.totalBoldMoney.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return e;
    }

    private CellStyle estiloBase(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private static class Estilos {
        CellStyle titulo;
        CellStyle subtitulo;
        CellStyle metaLabel;
        CellStyle metaValue;
        CellStyle header;
        CellStyle data;
        CellStyle dataCenter;
        CellStyle money;
        CellStyle totalLabel;
        CellStyle totalBold;
        CellStyle totalBoldMoney;
    }
}
