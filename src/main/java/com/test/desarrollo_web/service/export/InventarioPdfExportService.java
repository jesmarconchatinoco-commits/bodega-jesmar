package com.test.desarrollo_web.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.test.desarrollo_web.service.LogoService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class InventarioPdfExportService {

    private static final Color PURPLE = new Color(109, 40, 217);
    private static final Color PURPLE_LIGHT = new Color(237, 233, 254);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LogoService logoService;

    public InventarioPdfExportService(LogoService logoService) {
        this.logoService = logoService;
    }

    public byte[] exportarInventario(List<Map<String, Object>> filas,
                                     Map<String, Object> resumen,
                                     String usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 72, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PiePagina("Bodega Jesmar"));
            doc.open();

            agregarEncabezado(doc, "Reporte de Inventario", usuario);
            agregarResumen(doc, resumen);
            agregarTablaInventario(doc, filas);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de inventario: " + e.getMessage(), e);
        }
    }

    public byte[] exportarMovimientos(List<Map<String, Object>> movimientos, String usuario) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 36, 36, 72, 50);
            PdfWriter writer = PdfWriter.getInstance(doc, baos);
            writer.setPageEvent(new PiePagina("Bodega Jesmar"));
            doc.open();

            agregarEncabezado(doc, "Historial de Movimientos de Inventario", usuario);
            agregarTablaMovimientos(doc, movimientos);
            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF de movimientos: " + e.getMessage(), e);
        }
    }

    private void agregarEncabezado(Document doc, String titulo, String usuario) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 3f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(10f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        Optional<Path> logo = logoService.getLogoFilePath();
        if (logo.isPresent()) {
            try {
                Image img = Image.getInstance(logo.get().toString());
                img.scaleToFit(60, 45);
                logoCell.addElement(img);
            } catch (Exception ignored) {
                logoCell.addElement(new Phrase(" "));
            }
        }
        header.addCell(logoCell);

        PdfPCell info = new PdfPCell();
        info.setBorder(Rectangle.NO_BORDER);
        Font empresa = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PURPLE);
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
        info.addElement(new Paragraph("Bodega Jesmar", empresa));
        info.addElement(new Paragraph(titulo, tituloFont));
        header.addCell(info);
        doc.add(header);

        Font meta = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        doc.add(new Paragraph("Generado: " + LocalDateTime.now().format(FMT) + "  |  Usuario: " + usuario, meta));
        doc.add(Chunk.NEWLINE);
    }

    private void agregarResumen(Document doc, Map<String, Object> resumen) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PURPLE);
        doc.add(new Paragraph("Resumen: Productos " + resumen.get("totalProductos")
                + " | Presentaciones " + resumen.get("totalPresentaciones")
                + " | Stock bajo " + resumen.get("stockBajo")
                + " | Sin stock " + resumen.get("sinStock")
                + " | Valor " + resumen.get("valorInventario"), bold));
        doc.add(Chunk.NEWLINE);
    }

    private void agregarTablaInventario(Document doc, List<Map<String, Object>> filas) throws DocumentException {
        String[] headers = {"Producto", "Categoría", "Presentación", "Código", "Stock", "Mín.", "P. Compra", "P. Venta", "Estado"};
        PdfPTable table = crearTabla(headers);
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);
        boolean zebra = false;
        for (Map<String, Object> f : filas) {
            Color bg = zebra ? PURPLE_LIGHT : Color.WHITE;
            zebra = !zebra;
            agregarCelda(table, str(f.get("producto")), rowFont, bg);
            agregarCelda(table, str(f.get("categoria")), rowFont, bg);
            agregarCelda(table, str(f.get("presentacion")), rowFont, bg);
            agregarCelda(table, str(f.get("codigo")), rowFont, bg);
            agregarCelda(table, str(f.get("stock")), rowFont, bg);
            agregarCelda(table, str(f.get("stockMinimo")), rowFont, bg);
            agregarCelda(table, str(f.get("precioCompra")), rowFont, bg);
            agregarCelda(table, str(f.get("precioVenta")), rowFont, bg);
            agregarCelda(table, str(f.get("estadoLabel")), rowFont, bg);
        }
        doc.add(table);
    }

    private void agregarTablaMovimientos(Document doc, List<Map<String, Object>> movimientos) throws DocumentException {
        String[] headers = {"Fecha", "Producto", "Presentación", "Tipo", "Cant.", "Stock ant.", "Stock nuevo", "Referencia", "Usuario"};
        PdfPTable table = crearTabla(headers);
        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        boolean zebra = false;
        for (Map<String, Object> m : movimientos) {
            Color bg = zebra ? PURPLE_LIGHT : Color.WHITE;
            zebra = !zebra;
            agregarCelda(table, str(m.get("fecha")), rowFont, bg);
            agregarCelda(table, str(m.get("producto")), rowFont, bg);
            agregarCelda(table, str(m.get("presentacion")), rowFont, bg);
            agregarCelda(table, str(m.get("tipo")), rowFont, bg);
            agregarCelda(table, str(m.get("cantidad")), rowFont, bg);
            agregarCelda(table, str(m.get("stockAnterior")), rowFont, bg);
            agregarCelda(table, str(m.get("stockNuevo")), rowFont, bg);
            agregarCelda(table, str(m.get("referencia")), rowFont, bg);
            agregarCelda(table, str(m.get("usuario")), rowFont, bg);
        }
        doc.add(table);
    }

    private PdfPTable crearTabla(String[] headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingAfter(8f);
        table.setHeaderRows(1);
        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(PURPLE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4f);
            table.addCell(cell);
        }
        return table;
    }

    private void agregarCelda(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(3f);
        table.addCell(cell);
    }

    private String str(Object val) {
        return val != null ? val.toString() : "—";
    }

    private static class PiePagina extends PdfPageEventHelper {
        private final String empresa;

        PiePagina(String empresa) {
            this.empresa = empresa;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font pie = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            String texto = empresa + "  |  Impreso: " + LocalDateTime.now().format(FMT)
                    + "  |  Página " + writer.getPageNumber();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(texto, pie),
                    (document.left() + document.right()) / 2,
                    document.bottom() - 18, 0);
        }
    }
}
