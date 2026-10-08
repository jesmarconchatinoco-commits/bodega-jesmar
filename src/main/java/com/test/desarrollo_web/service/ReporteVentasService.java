package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.dto.ReporteFiltroDto;
import com.test.desarrollo_web.dto.ReporteVentaFilaDto;
import com.test.desarrollo_web.dto.ReporteVentasDocumentoDto;
import com.test.desarrollo_web.dto.ReporteVentasResumenDto;
import com.test.desarrollo_web.util.SqlMySqlAPostgres;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Locale;

@Service
public class ReporteVentasService {

    public static final String EMPRESA_NOMBRE = "Bodega Jesmar";
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_GEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.datasource.url:}")
    private String urlBaseDatos;

    private final PermissionService permissionService;

    public ReporteVentasService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public ReporteVentasDocumentoDto generarDocumento(String tipo, ReporteFiltroDto filtro) {
        String canal = resolverCanal(tipo);
        String titulo = tituloReporte(tipo);

        List<ReporteVentaFilaDto> filas = consultarFilas(filtro, canal);
        ReporteVentasResumenDto resumen = calcularResumen(filas);

        Usuario usuario = permissionService.getUsuarioActual();
        Map<String, Object> rango = ReporteQueryBuilder.resolverRangoFechas(filtro);

        ReporteVentasDocumentoDto doc = new ReporteVentasDocumentoDto();
        doc.setTitulo(titulo);
        doc.setEmpresa(EMPRESA_NOMBRE);
        doc.setUsuarioGenerador(usuario != null ? usuario.getNombre() : "Sistema");
        doc.setFechaGeneracion(LocalDateTime.now().format(FMT_GEN));
        doc.setRangoFechas(formatearRango(rango));
        doc.setFilas(filas);
        doc.setResumen(resumen);
        return doc;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> toTablaApi(ReporteVentasDocumentoDto doc) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("titulo", doc.getTitulo());
        res.put("columnas", doc.getColumnas());
        res.put("filas", filasParaTabla(doc.getFilas()));
        res.put("resumen", resumenParaApi(doc.getResumen()));
        res.put("total", doc.getFilas().size());
        res.put("rangoFechas", doc.getRangoFechas());
        return res;
    }

    private List<ReporteVentaFilaDto> consultarFilas(ReporteFiltroDto filtro, String canalForzado) {
        ReporteFiltroDto f = filtro != null ? filtro : new ReporteFiltroDto();
        if (canalForzado != null) {
            f.setCanal(canalForzado);
        }

        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(f, "v");
        String sql = """
                SELECT v.id, v.fecha, v.numero_documento,
                       COALESCE(tc.serie, SUBSTRING_INDEX(v.numero_documento, '-', 1)) AS serie,
                       c.nombre AS cliente, c.documento,
                       u.nombre AS vendedor, tp.nombre AS metodo_pago, v.estado, v.total,
                       COALESCE(v.origen_venta,
                           CASE WHEN EXISTS (SELECT 1 FROM PEDIDO_CATALOGO pc WHERE pc.venta_id = v.id)
                                THEN 'WEB' ELSE 'POS' END) AS tipo_venta,
                       COALESCE(det.subtotal_sum, 0) AS subtotal,
                       COALESCE(det.descuento_sum, 0) AS descuento,
                       v.codigo_verificacion_pago
                FROM VENTA v
                LEFT JOIN CLIENTE c ON v.id_cliente = c.id
                LEFT JOIN USUARIO u ON v.id_vendedor = u.id
                LEFT JOIN TIPO_PAGO tp ON v.id_tipo_pago = tp.id
                LEFT JOIN TIPO_COMPROBANTE tc ON v.id_tipo_comprobante = tc.id
                LEFT JOIN (
                    SELECT d.id_venta,
                           SUM(d.subtotal) AS subtotal_sum,
                           SUM(GREATEST(d.cantidad * d.precio_unitario - d.subtotal, 0)) AS descuento_sum
                    FROM DETALLE_VENTA d
                    GROUP BY d.id_venta
                ) det ON det.id_venta = v.id
                """ + fs.whereSql() + """
                 ORDER BY v.fecha DESC, v.numero_documento DESC, c.nombre ASC, u.nombre ASC, v.total DESC
                """;

        String consulta = sql;
        if (urlBaseDatos != null && urlBaseDatos.toLowerCase(Locale.ROOT).contains("postgresql")) {
            consulta = SqlMySqlAPostgres.convertir(sql);
        }
        Query q = entityManager.createNativeQuery(consulta);
        for (int i = 0; i < fs.params().size(); i++) {
            q.setParameter(i + 1, fs.params().get(i));
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        LinkedHashMap<Long, ReporteVentaFilaDto> unicos = new LinkedHashMap<>();
        for (Object[] r : rows) {
            Long id = ((Number) r[0]).longValue();
            if (unicos.containsKey(id)) {
                continue;
            }
            unicos.put(id, mapearFila(r));
        }
        return new ArrayList<>(unicos.values());
    }

    private ReporteVentaFilaDto mapearFila(Object[] r) {
        LocalDateTime fecha = parseFecha(r[1]);
        String numeroDoc = r[2] != null ? r[2].toString() : "";
        String serie = r[3] != null ? r[3].toString() : "";
        String correlativo = extraerCorrelativo(numeroDoc, serie);

        BigDecimal total = toBigDecimal(r[9]);
        BigDecimal subtotal = toBigDecimal(r[11]);
        BigDecimal descuento = toBigDecimal(r[12]);
        BigDecimal igv = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        ReporteVentaFilaDto f = new ReporteVentaFilaDto();
        f.setId(((Number) r[0]).longValue());
        f.setFecha(fecha != null ? fecha.format(FMT_FECHA) : "—");
        f.setHora(fecha != null ? fecha.format(FMT_HORA) : "—");
        String tipoVenta = r[10] != null ? r[10].toString() : "POS";
        f.setTipoVenta("WEB".equalsIgnoreCase(tipoVenta) ? "Web" : "POS");
        f.setSerie(serie);
        f.setNumeroComprobante(correlativo);
        f.setCliente(r[4] != null ? r[4].toString() : "—");
        f.setDocumento(r[5] != null ? r[5].toString() : "—");
        f.setVendedor(r[6] != null ? r[6].toString() : "—");
        f.setMetodoPago(r[7] != null ? r[7].toString() : "—");
        f.setEstado(r[8] != null ? r[8].toString() : "—");
        f.setSubtotal(subtotal);
        f.setIgv(igv);
        f.setDescuento(descuento);
        f.setTotal(total);
        f.setCodigoVerificacion(r.length > 13 && r[13] != null ? r[13].toString() : "—");
        return f;
    }

    private ReporteVentasResumenDto calcularResumen(List<ReporteVentaFilaDto> filas) {
        ReporteVentasResumenDto r = new ReporteVentasResumenDto();
        r.setCantidadComprobantes(filas.size());
        r.setTotalVentas(filas.size());

        Set<String> clientes = new HashSet<>();
        BigDecimal vendido = BigDecimal.ZERO;
        BigDecimal descuentos = BigDecimal.ZERO;
        BigDecimal impuestos = BigDecimal.ZERO;

        for (ReporteVentaFilaDto f : filas) {
            if (f.getCliente() != null) {
                clientes.add(f.getCliente());
            }
            vendido = vendido.add(nullSafe(f.getTotal()));
            descuentos = descuentos.add(nullSafe(f.getDescuento()));
            impuestos = impuestos.add(nullSafe(f.getIgv()));
        }

        r.setTotalClientes(clientes.size());
        r.setTotalVendido(vendido.setScale(2, RoundingMode.HALF_UP));
        r.setTotalDescuentos(descuentos.setScale(2, RoundingMode.HALF_UP));
        r.setTotalImpuestos(impuestos.setScale(2, RoundingMode.HALF_UP));
        r.setPromedioVenta(filas.isEmpty()
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : vendido.divide(BigDecimal.valueOf(filas.size()), 2, RoundingMode.HALF_UP));
        return r;
    }

    private List<Map<String, Object>> filasParaTabla(List<ReporteVentaFilaDto> filas) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ReporteVentaFilaDto f : filas) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", f.getId());
            row.put("Fecha", f.getFecha());
            row.put("Hora", f.getHora());
            row.put("Tipo", f.getTipoVenta());
            row.put("Serie", f.getSerie());
            row.put("N° Comprobante", f.getNumeroComprobante());
            row.put("Cliente", f.getCliente());
            row.put("Documento", f.getDocumento());
            row.put("Vendedor", f.getVendedor());
            row.put("Método pago", f.getMetodoPago());
            row.put("Cód. verificación", f.getCodigoVerificacion() != null ? f.getCodigoVerificacion() : "—");
            row.put("Estado", f.getEstado());
            row.put("Subtotal", formatoMoneda(f.getSubtotal()));
            row.put("IGV", formatoMoneda(f.getIgv()));
            row.put("Descuento", formatoMoneda(f.getDescuento()));
            row.put("Total", formatoMoneda(f.getTotal()));
            out.add(row);
        }
        return out;
    }

    private Map<String, Object> resumenParaApi(ReporteVentasResumenDto r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalVentas", r.getTotalVentas());
        m.put("cantidadComprobantes", r.getCantidadComprobantes());
        m.put("totalClientes", r.getTotalClientes());
        m.put("totalVendido", formatoMoneda(r.getTotalVendido()));
        m.put("totalDescuentos", formatoMoneda(r.getTotalDescuentos()));
        m.put("totalImpuestos", formatoMoneda(r.getTotalImpuestos()));
        m.put("promedioVenta", formatoMoneda(r.getPromedioVenta()));
        return m;
    }

    private String resolverCanal(String tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo.trim().toUpperCase(Locale.ROOT)) {
            case "POS" -> "POS";
            case "WEB" -> "WEB";
            default -> null;
        };
    }

    private String tituloReporte(String tipo) {
        if (tipo == null) {
            return "Reporte General de Ventas";
        }
        return switch (tipo.trim().toUpperCase(Locale.ROOT)) {
            case "POS" -> "Reporte de Ventas POS";
            case "WEB" -> "Reporte de Ventas Web";
            default -> "Reporte General de Ventas";
        };
    }

    private String formatearRango(Map<String, Object> rango) {
        LocalDate desde = (LocalDate) rango.get("desdeFecha");
        LocalDate hasta = (LocalDate) rango.get("hastaFecha");
        if (desde == null || hasta == null) {
            return "—";
        }
        return desde.format(FMT_FECHA) + " al " + hasta.format(FMT_FECHA);
    }

    private String extraerCorrelativo(String numeroDoc, String serie) {
        if (numeroDoc == null || numeroDoc.isBlank()) {
            return "—";
        }
        if (serie != null && !serie.isBlank() && numeroDoc.startsWith(serie + "-")) {
            return numeroDoc.substring(serie.length() + 1);
        }
        int idx = numeroDoc.indexOf('-');
        return idx >= 0 ? numeroDoc.substring(idx + 1) : numeroDoc;
    }

    private LocalDateTime parseFecha(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (val instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        return null;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal bd) {
            return bd;
        }
        if (val instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        String texto = val.toString().trim();
        if (texto.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String formatoMoneda(BigDecimal val) {
        return "S/ " + nullSafe(val).setScale(2, RoundingMode.HALF_UP);
    }
}
