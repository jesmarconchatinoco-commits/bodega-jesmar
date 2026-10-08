package com.test.desarrollo_web.service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.test.desarrollo_web.dto.ReporteVentaFilaDto;
import com.test.desarrollo_web.dto.ReporteVentasDocumentoDto;
import com.test.desarrollo_web.dto.ReporteVentasResumenDto;
import com.test.desarrollo_web.service.LogoService;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ReportePdfExportService {

    private static final Color PURPLE = new Color(109, 40, 217);
    private static final Color PURPLE_LIGHT = new Color(237, 233, 254);
    private static final DateTimeFormatter FMT_PIE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LogoService logoService;

    public ReportePdfExportService(LogoService logoService) {
        this.logoService = logoService;
    }

    public byte[] exportar(ReporteVentasDocumentoDto doc) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 80, 60);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PiePagina(doc));
            document.open();

            agregarEncabezado(document, doc);
            agregarMeta(document, doc);
            agregarTabla(document, doc);
            agregarResumen(document, doc.getResumen());

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }

    private void agregarEncabezado(Document document, ReporteVentasDocumentoDto doc) throws DocumentException {
        PdfPTable header = new PdfPTable(new float[]{1.2f, 3f});
        header.setWidthPercentage(100);
        header.setSpacingAfter(12f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Optional<Path> logoPath = logoService.getLogoFilePath();
        if (logoPath.isPresent()) {
            try {
                Image img = Image.getInstance(logoPath.get().toString());
                img.scaleToFit(70, 50);
                logoCell.addElement(img);
            } catch (Exception ignored) {
                logoCell.addElement(new Phrase(" "));
            }
        }
        header.addCell(logoCell);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        Font empresaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, PURPLE);
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.DARK_GRAY);
        infoCell.addElement(new Paragraph(doc.getEmpresa(), empresaFont));
        infoCell.addElement(new Paragraph(doc.getTitulo(), tituloFont));
        header.addCell(infoCell);
        document.add(header);
    }

    private void agregarMeta(Document document, ReporteVentasDocumentoDto doc) throws DocumentException {
        Font meta = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        document.add(new Paragraph("Generado: " + doc.getFechaGeneracion() + "  |  Usuario: " + doc.getUsuarioGenerador(), meta));
        document.add(new Paragraph("Rango consultado: " + doc.getRangoFechas() + "  |  Registros: " + doc.getFilas().size(), meta));
        document.add(Chunk.NEWLINE);
    }

    private void agregarTabla(Document document, ReporteVentasDocumentoDto doc) throws DocumentException {
        String[] headers = {"Fecha", "Hora", "Tipo", "Serie", "N° Comp.", "Cliente", "Documento",
                "Vendedor", "Pago", "Cód. verif.", "Estado", "Subtotal", "IGV", "Desc.", "Total"};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingAfter(10f);
        table.setHeaderRows(1);

        Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(PURPLE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5f);
            table.addCell(cell);
        }

        Font rowFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Color.BLACK);
        boolean zebra = false;
        for (ReporteVentaFilaDto f : doc.getFilas()) {
            Color bg = zebra ? PURPLE_LIGHT : Color.WHITE;
            zebra = !zebra;
            agregarCelda(table, f.getFecha(), rowFont, bg);
            agregarCelda(table, f.getHora(), rowFont, bg);
            agregarCelda(table, f.getTipoVenta(), rowFont, bg);
            agregarCelda(table, f.getSerie(), rowFont, bg);
            agregarCelda(table, f.getNumeroComprobante(), rowFont, bg);
            agregarCelda(table, f.getCliente(), rowFont, bg);
            agregarCelda(table, f.getDocumento(), rowFont, bg);
            agregarCelda(table, f.getVendedor(), rowFont, bg);
            agregarCelda(table, f.getMetodoPago(), rowFont, bg);
            agregarCelda(table, f.getCodigoVerificacion(), rowFont, bg);
            agregarCelda(table, f.getEstado(), rowFont, bg);
            agregarCeldaMoneda(table, f.getSubtotal(), rowFont, bg);
            agregarCeldaMoneda(table, f.getIgv(), rowFont, bg);
            agregarCeldaMoneda(table, f.getDescuento(), rowFont, bg);
            agregarCeldaMoneda(table, f.getTotal(), rowFont, bg);
        }
        document.add(table);
    }

    private void agregarResumen(Document document, ReporteVentasResumenDto r) throws DocumentException {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, PURPLE);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        document.add(new Paragraph("Resumen del reporte", bold));
        document.add(new Paragraph("Total ventas: " + r.getTotalVentas()
                + "  |  Comprobantes: " + r.getCantidadComprobantes()
                + "  |  Clientes: " + r.getTotalClientes(), normal));
        document.add(new Paragraph("Total vendido: " + moneda(r.getTotalVendido())
                + "  |  Descuentos: " + moneda(r.getTotalDescuentos())
                + "  |  Impuestos: " + moneda(r.getTotalImpuestos())
                + "  |  Promedio: " + moneda(r.getPromedioVenta()), normal));
    }

    private void agregarCelda(PdfPTable table, String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", font));
        cell.setBackgroundColor(bg);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void agregarCeldaMoneda(PdfPTable table, BigDecimal val, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(moneda(val), font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private String moneda(BigDecimal v) {
        BigDecimal n = v != null ? v : BigDecimal.ZERO;
        return "S/ " + n.setScale(2, RoundingMode.HALF_UP);
    }

    private static class PiePagina extends PdfPageEventHelper {
        private final ReporteVentasDocumentoDto doc;

        PiePagina(ReporteVentasDocumentoDto doc) {
            this.doc = doc;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Font pie = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            String texto = doc.getEmpresa() + "  |  Impreso: " + LocalDateTime.now().format(FMT_PIE)
                    + "  |  Página " + writer.getPageNumber();
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(texto, pie),
                    (document.left() + document.right()) / 2,
                    document.bottom() - 20, 0);
        }
    }
}
