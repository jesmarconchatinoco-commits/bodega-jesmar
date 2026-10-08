package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.ReporteHistorial;
import com.test.desarrollo_web.Repository.*;
import com.test.desarrollo_web.dto.ReporteFiltroDto;
import com.test.desarrollo_web.util.CsvExportUtil;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReporteService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM");

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${spring.datasource.url:}")
    private String urlBaseDatos;

    private final ReporteHistorialRepository historialRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final TipoPagoRepository tipoPagoRepository;
    private final TipoComprobanteRepository tipoComprobanteRepository;
    private final VentaService ventaService;
    private final PermissionService permissionService;
    private final ReporteVentasService reporteVentasService;

    public ReporteService(ReporteHistorialRepository historialRepository,
                          ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          CategoriaRepository categoriaRepository,
                          ProductoRepository productoRepository,
                          TipoPagoRepository tipoPagoRepository,
                          TipoComprobanteRepository tipoComprobanteRepository,
                          VentaService ventaService,
                          PermissionService permissionService,
                          ReporteVentasService reporteVentasService) {
        this.historialRepository = historialRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.tipoPagoRepository = tipoPagoRepository;
        this.tipoComprobanteRepository = tipoComprobanteRepository;
        this.ventaService = ventaService;
        this.permissionService = permissionService;
        this.reporteVentasService = reporteVentasService;
    }

    @Transactional(readOnly = true)
    public void validarAcceso() {
        if (!permissionService.puedeAccederReportes()) {
            throw new RuntimeException("No tiene permiso para acceder a Gestión de Reportes.");
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerCatalogoFiltros() {
        validarAcceso();
        Map<String, Object> cat = new LinkedHashMap<>();
        cat.put("clientes", clienteRepository.findAll().stream()
                .map(c -> Map.of("id", c.getId(), "nombre", c.getNombre())).toList());
        cat.put("vendedores", usuarioRepository.findAll().stream()
                .map(u -> Map.of("id", u.getId(), "nombre", u.getNombre())).toList());
        cat.put("categorias", categoriaRepository.findAll().stream()
                .map(c -> Map.of("id", c.getId(), "nombre", c.getNombre())).toList());
        cat.put("productos", productoRepository.findAll().stream()
                .map(p -> Map.of("id", p.getId(), "nombre", p.getNombre())).toList());
        cat.put("tiposPago", tipoPagoRepository.findAll().stream()
                .map(t -> Map.of("id", t.getId(), "nombre", t.getNombre())).toList());
        cat.put("tiposComprobante", tipoComprobanteRepository.findAll().stream()
                .map(t -> Map.of("id", t.getId(), "nombre", t.getNombre())).toList());
        cat.put("estadosVenta", List.of("PAGADO", "PENDIENTE", "ANULADO"));
        cat.put("canales", List.of(
                Map.of("codigo", "", "nombre", "Todos"),
                Map.of("codigo", "POS", "nombre", "Ventas POS"),
                Map.of("codigo", "WEB", "nombre", "Venta por Web")
        ));
        return cat;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDashboard(ReporteFiltroDto filtro) {
        validarAcceso();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("kpis", obtenerKpis(filtro));
        res.put("graficos", obtenerGraficos(filtro));
        res.put("gerencial", obtenerIndicadoresGerenciales(filtro));
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> consultar(String tipo, ReporteFiltroDto filtro) {
        validarAcceso();
        return consultarSinHistorial(normalizarTipo(tipo), filtro);
    }

    @Transactional
    public byte[] exportarCsv(String tipo, ReporteFiltroDto filtro) {
        validarAcceso();
        Map<String, Object> data = consultarSinHistorial(normalizarTipo(tipo), filtro);
        @SuppressWarnings("unchecked")
        List<String> columnas = (List<String>) data.get("columnas");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> filas = (List<Map<String, Object>>) data.get("filas");

        List<List<String>> csvFilas = new ArrayList<>();
        for (Map<String, Object> fila : filas) {
            List<String> linea = new ArrayList<>();
            for (String col : columnas) {
                Object val = fila.get(col);
                linea.add(val != null ? val.toString() : "");
            }
            csvFilas.add(linea);
        }

        String csv = CsvExportUtil.generar(columnas, csvFilas);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] cuerpo = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] salida = new byte[bom.length + cuerpo.length];
        System.arraycopy(bom, 0, salida, 0, bom.length);
        System.arraycopy(cuerpo, 0, salida, bom.length, cuerpo.length);
        return salida;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalleVenta(Long ventaId) {
        validarAcceso();
        return ventaService.obtenerDetalleVenta(ventaId);
    }

    private Map<String, Object> consultarSinHistorial(String tipo, ReporteFiltroDto filtro) {
        return switch (tipo) {
            case "GENERAL", "POS", "WEB" -> reporteVentasService.toTablaApi(
                    reporteVentasService.generarDocumento(tipo, filtro));
            case "PRODUCTOS" -> reporteProductos(filtro);
            case "CLIENTES" -> reporteClientes(filtro);
            case "CATEGORIAS" -> reporteCategorias(filtro);
            case "INVENTARIO" -> reporteInventario();
            case "COMPRAS" -> reporteCompras(filtro);
            case "CAJA" -> reporteCaja(filtro);
            case "UTILIDADES" -> reporteUtilidades(filtro);
            case "METODOS_PAGO" -> reporteMetodosPago(filtro);
            case "VENDEDORES" -> reporteVendedores(filtro);
            case "COMPARATIVO" -> reporteComparativo(filtro);
            case "HISTORIAL" -> reporteHistorial();
            default -> throw new RuntimeException("Tipo de reporte no válido.");
        };
    }

    private Map<String, Object> obtenerKpis(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sqlBase = " FROM VENTA v " + fs.whereSql();

        BigDecimal ingresos = scalarDecimal("SELECT COALESCE(SUM(v.total), 0)" + sqlBase, fs.params());
        long cantidad = scalarLong("SELECT COUNT(*)" + sqlBase, fs.params());
        BigDecimal utilidad = calcularUtilidad(filtro);
        long productos = scalarLong("""
                SELECT COALESCE(SUM(dv.cantidad), 0)
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                """ + fs.whereSql().replace("v.fecha", "v.fecha"), fs.params());
        long clientes = scalarLong("""
                SELECT COUNT(DISTINCT v.id_cliente)
                FROM VENTA v
                """ + fs.whereSql(), fs.params());
        BigDecimal ticket = cantidad > 0
                ? ingresos.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal ventasPos = ventasPorCanal(filtro, "POS");
        BigDecimal ventasWeb = ventasPorCanal(filtro, "WEB");

        LocalDate hoy = LocalDate.now();
        BigDecimal ventasDia = scalarDecimal("""
                SELECT COALESCE(SUM(v.total), 0) FROM VENTA v
                WHERE DATE(v.fecha) = ? AND v.estado <> 'ANULADO'
                """, List.of(hoy));
        BigDecimal ventasMes = scalarDecimal("""
                SELECT COALESCE(SUM(v.total), 0) FROM VENTA v
                WHERE YEAR(v.fecha) = ? AND MONTH(v.fecha) = ? AND v.estado <> 'ANULADO'
                """, List.of(hoy.getYear(), hoy.getMonthValue()));

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("ventasDia", ventasDia);
        kpis.put("ventasMes", ventasMes);
        kpis.put("ingresosTotales", ingresos);
        kpis.put("utilidad", utilidad);
        kpis.put("cantidadVentas", cantidad);
        kpis.put("productosVendidos", productos);
        kpis.put("clientesAtendidos", clientes);
        kpis.put("ticketPromedio", ticket);
        kpis.put("ventasPos", ventasPos);
        kpis.put("ventasWeb", ventasWeb);
        return kpis;
    }

    private Map<String, Object> obtenerGraficos(ReporteFiltroDto filtro) {
        Map<String, Object> graficos = new LinkedHashMap<>();
        graficos.put("ventasPorDia", chartVentasPorDia(filtro));
        graficos.put("ventasPorMes", chartVentasPorMes(filtro));
        graficos.put("comparativoCanal", chartComparativo(filtro));
        graficos.put("topProductos", chartTopProductos(filtro, 8));
        graficos.put("topCategorias", chartTopCategorias(filtro, 8));
        graficos.put("metodosPago", chartMetodosPago(filtro));
        graficos.put("ventasPorHora", chartVentasPorHora(filtro));
        graficos.put("topClientes", chartTopClientes(filtro, 8));
        graficos.put("topVendedores", chartTopVendedores(filtro, 8));
        graficos.put("evolucionUtilidad", chartEvolucionUtilidad(filtro));
        return graficos;
    }

    private Map<String, Object> obtenerIndicadoresGerenciales(ReporteFiltroDto filtro) {
        Map<String, Object> g = new LinkedHashMap<>();
        BigDecimal ingresos = toBigDecimal(obtenerKpis(filtro).get("ingresosTotales"));
        BigDecimal utilidad = calcularUtilidad(filtro);
        BigDecimal margen = ingresos.compareTo(BigDecimal.ZERO) > 0
                ? utilidad.multiply(BigDecimal.valueOf(100)).divide(ingresos, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtroAnuladas(filtro), "v");
        long canceladas = scalarLong("SELECT COUNT(*) FROM VENTA v " + fs.whereSql(), fs.params());

        g.put("crecimientoVentas", calcularCrecimiento(filtro));
        g.put("porcentajeUtilidad", margen);
        g.put("margenGanancia", margen);
        g.put("productosRotacion", chartTopProductos(filtro, 5));
        g.put("productosStockBajo", ventaService.listarInventario().stream()
                .filter(i -> Boolean.TRUE.equals(i.get("bajo")))
                .limit(10).toList());
        g.put("clientesFrecuentes", chartTopClientes(filtro, 5));
        g.put("ventasCanceladas", canceladas);
        g.put("devoluciones", 0);
        return g;
    }

    private Map<String, Object> reporteProductos(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        List<Object> params = new ArrayList<>(fs.params());
        String extra = ReporteQueryBuilder.filtroProducto(filtro, "p", params);
        String sql = """
                SELECT p.nombre, COALESCE(p.descripcion, 'Sin marca') AS marca,
                       SUM(dv.cantidad) AS cantidad, SUM(dv.subtotal) AS total
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                """ + fs.whereSql() + extra + """
                 GROUP BY p.id, p.nombre, p.descripcion
                 ORDER BY total DESC
                """;
        return tablaAgregada(sql, params, List.of("Producto", "Marca", "Cantidad", "Total"),
                new int[]{0, 1, 2, 3}, "Reporte de Productos");
    }

    private Map<String, Object> reporteClientes(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT c.nombre, c.documento, COUNT(v.id) AS ventas, COALESCE(SUM(v.total), 0) AS total
                FROM VENTA v
                INNER JOIN CLIENTE c ON v.id_cliente = c.id
                """ + fs.whereSql() + """
                 GROUP BY c.id, c.nombre, c.documento
                 ORDER BY total DESC
                """;
        return tablaAgregada(sql, fs.params(), List.of("Cliente", "Documento", "Ventas", "Total"),
                new int[]{0, 1, 2, 3}, "Reporte de Clientes");
    }

    private Map<String, Object> reporteCategorias(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        List<Object> params = new ArrayList<>(fs.params());
        String extra = ReporteQueryBuilder.filtroProducto(filtro, "p", params);
        String sql = """
                SELECT COALESCE(cat.nombre, 'Sin categoría') AS categoria,
                       SUM(dv.cantidad) AS cantidad, SUM(dv.subtotal) AS total
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN CATEGORIA cat ON p.id_categoria = cat.id
                """ + fs.whereSql() + extra + """
                 GROUP BY COALESCE(cat.nombre, 'Sin categoría')
                 ORDER BY total DESC
                """;
        return tablaAgregada(sql, params, List.of("Categoría", "Cantidad", "Total"),
                new int[]{0, 1, 2}, "Reporte de Categorías");
    }

    private Map<String, Object> reporteInventario() {
        List<Map<String, Object>> items = ventaService.listarInventario();
        List<String> columnas = List.of("Producto", "Presentación", "Stock", "Mínimo", "Estado");
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Map<String, Object> i : items) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("Producto", i.get("productoNombre"));
            f.put("Presentación", i.get("presentacionNombre"));
            f.put("Stock", i.get("stock"));
            f.put("Mínimo", i.get("stockMinimo"));
            f.put("Estado", Boolean.TRUE.equals(i.get("bajo")) ? "Stock bajo" : "Normal");
            filas.add(f);
        }
        return respuestaTabla("Reporte de Inventario", columnas, filas);
    }

    private Map<String, Object> reporteCompras(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        List<Object> params = new ArrayList<>(fs.params());
        String extra = ReporteQueryBuilder.filtroProducto(filtro, "p", params);
        String sql = """
                SELECT p.nombre,
                       COALESCE(SUM(dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0) AS costo,
                       COALESCE(SUM(dv.subtotal), 0) AS venta,
                       COALESCE(SUM(dv.subtotal), 0) - COALESCE(SUM(dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0) AS utilidad
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN producto_presentacion pp ON dv.id_presentacion = pp.id
                """ + fs.whereSql() + extra + """
                 GROUP BY p.id, p.nombre
                 ORDER BY venta DESC
                """;
        return tablaAgregada(sql, params, List.of("Producto", "Costo compra", "Venta", "Utilidad"),
                new int[]{0, 1, 2, 3}, "Reporte de Compras / Costos");
    }

    private Map<String, Object> reporteCaja(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT DATE(v.fecha) AS dia, tp.nombre AS metodo,
                       COUNT(v.id) AS operaciones, COALESCE(SUM(v.total), 0) AS monto
                FROM VENTA v
                INNER JOIN TIPO_PAGO tp ON v.id_tipo_pago = tp.id
                """ + fs.whereSql() + """
                 GROUP BY DATE(v.fecha), tp.id, tp.nombre
                 ORDER BY dia DESC, monto DESC
                """;
        return tablaAgregada(sql, fs.params(), List.of("Fecha", "Método de pago", "Operaciones", "Monto"),
                new int[]{0, 1, 2, 3}, "Reporte de Caja");
    }

    private Map<String, Object> reporteUtilidades(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        List<Object> params = new ArrayList<>(fs.params());
        String extra = ReporteQueryBuilder.filtroProducto(filtro, "p", params);
        String sql = """
                SELECT DATE(v.fecha) AS dia,
                       COALESCE(SUM(dv.subtotal), 0) AS venta,
                       COALESCE(SUM(dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0) AS costo,
                       COALESCE(SUM(dv.subtotal), 0) - COALESCE(SUM(dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0) AS utilidad
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN producto_presentacion pp ON dv.id_presentacion = pp.id
                """ + fs.whereSql() + extra + """
                 GROUP BY DATE(v.fecha)
                 ORDER BY dia DESC
                """;
        return tablaAgregada(sql, params, List.of("Fecha", "Venta", "Costo", "Utilidad"),
                new int[]{0, 1, 2, 3}, "Reporte de Utilidades");
    }

    private Map<String, Object> reporteMetodosPago(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT tp.nombre, COUNT(v.id) AS operaciones, COALESCE(SUM(v.total), 0) AS total
                FROM VENTA v
                INNER JOIN TIPO_PAGO tp ON v.id_tipo_pago = tp.id
                """ + fs.whereSql() + """
                 GROUP BY tp.id, tp.nombre
                 ORDER BY total DESC
                """;
        return tablaAgregada(sql, fs.params(), List.of("Método de pago", "Operaciones", "Total"),
                new int[]{0, 1, 2}, "Reporte de Métodos de Pago");
    }

    private Map<String, Object> reporteVendedores(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT u.nombre, COUNT(v.id) AS ventas, COALESCE(SUM(v.total), 0) AS total
                FROM VENTA v
                INNER JOIN USUARIO u ON v.id_vendedor = u.id
                """ + fs.whereSql() + """
                 GROUP BY u.id, u.nombre
                 ORDER BY total DESC
                """;
        return tablaAgregada(sql, fs.params(), List.of("Vendedor", "Ventas", "Total"),
                new int[]{0, 1, 2}, "Reporte de Vendedores");
    }

    private Map<String, Object> reporteComparativo(ReporteFiltroDto filtro) {
        ReporteFiltroDto sinCanal = filtro != null ? filtro : new ReporteFiltroDto();
        sinCanal.setCanal(null);
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(sinCanal, "v");
        String sql = """
                SELECT canal, COUNT(*) AS ventas, COALESCE(SUM(total), 0) AS monto FROM (
                    SELECT v.total,
                        CASE WHEN EXISTS (SELECT 1 FROM PEDIDO_CATALOGO pc WHERE pc.venta_id = v.id)
                             THEN 'Venta por Web' ELSE 'Ventas POS' END AS canal
                    FROM VENTA v
                """ + fs.whereSql() + """
                ) t GROUP BY canal ORDER BY monto DESC
                """;
        return tablaAgregada(sql, fs.params(), List.of("Canal", "Ventas", "Monto"),
                new int[]{0, 1, 2}, "Reporte Comparativo POS vs Web");
    }

    private Map<String, Object> reporteHistorial() {
        List<String> columnas = List.of("Fecha", "Usuario", "Reporte", "Acción", "Formato");
        List<Map<String, Object>> filas = new ArrayList<>();
        for (ReporteHistorial h : historialRepository.findTop50ByOrderByFechaDesc()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("Fecha", h.getFecha() != null ? h.getFecha().format(FMT) : "—");
            f.put("Usuario", h.getUsuarioNombre());
            f.put("Reporte", h.getTipoReporte());
            f.put("Acción", h.getAccion());
            f.put("Formato", h.getFormato() != null ? h.getFormato() : "—");
            filas.add(f);
        }
        return respuestaTabla("Historial de Reportes", columnas, filas);
    }

    /* ---------- Gráficos ---------- */

    private Map<String, Object> chartVentasPorDia(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = "SELECT DATE(v.fecha), COALESCE(SUM(v.total), 0) FROM VENTA v "
                + fs.whereSql() + " GROUP BY DATE(v.fecha) ORDER BY DATE(v.fecha)";
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, true);
    }

    private Map<String, Object> chartVentasPorMes(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT CONCAT(YEAR(v.fecha), '-', LPAD(MONTH(v.fecha), 2, '0')),
                       COALESCE(SUM(v.total), 0)
                FROM VENTA v
                """ + fs.whereSql() + """
                 GROUP BY CONCAT(YEAR(v.fecha), '-', LPAD(MONTH(v.fecha), 2, '0'))
                 ORDER BY 1""";
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartComparativo(ReporteFiltroDto filtro) {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", List.of("Ventas POS", "Venta por Web"));
        chart.put("valores", List.of(
                ventasPorCanal(filtro, "POS"),
                ventasPorCanal(filtro, "WEB")
        ));
        return chart;
    }

    private Map<String, Object> chartTopProductos(ReporteFiltroDto filtro, int limit) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT p.nombre, SUM(dv.cantidad)
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                """ + fs.whereSql() + """
                 GROUP BY p.id, p.nombre ORDER BY 2 DESC LIMIT """ + " " + limit;
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartTopCategorias(ReporteFiltroDto filtro, int limit) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT COALESCE(cat.nombre, 'Sin categoría'), SUM(dv.subtotal)
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN CATEGORIA cat ON p.id_categoria = cat.id
                """ + fs.whereSql() + """
                 GROUP BY COALESCE(cat.nombre, 'Sin categoría')
                 ORDER BY 2 DESC LIMIT """ + " " + limit;
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartMetodosPago(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT tp.nombre, COALESCE(SUM(v.total), 0)
                FROM VENTA v INNER JOIN TIPO_PAGO tp ON v.id_tipo_pago = tp.id
                """ + fs.whereSql() + " GROUP BY tp.id, tp.nombre ORDER BY 2 DESC";
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartVentasPorHora(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT HOUR(v.fecha), COUNT(*)
                FROM VENTA v
                """ + fs.whereSql() + " GROUP BY HOUR(v.fecha) ORDER BY 1";
        List<Object[]> rows = ejecutarNativa(sql, fs.params());
        List<String> labels = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        for (Object[] r : rows) {
            labels.add(r[0] + ":00");
            valores.add(toBigDecimal(r[1]));
        }
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("labels", labels);
        c.put("valores", valores);
        return c;
    }

    private Map<String, Object> chartTopClientes(ReporteFiltroDto filtro, int limit) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT c.nombre, COALESCE(SUM(v.total), 0)
                FROM VENTA v INNER JOIN CLIENTE c ON v.id_cliente = c.id
                """ + fs.whereSql() + """
                 GROUP BY c.id, c.nombre ORDER BY 2 DESC LIMIT """ + " " + limit;
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartTopVendedores(ReporteFiltroDto filtro, int limit) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT u.nombre, COALESCE(SUM(v.total), 0)
                FROM VENTA v INNER JOIN USUARIO u ON v.id_vendedor = u.id
                """ + fs.whereSql() + """
                 GROUP BY u.id, u.nombre ORDER BY 2 DESC LIMIT """ + " " + limit;
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, false);
    }

    private Map<String, Object> chartEvolucionUtilidad(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        String sql = """
                SELECT DATE(v.fecha),
                       COALESCE(SUM(dv.subtotal - dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0)
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN producto_presentacion pp ON dv.id_presentacion = pp.id
                """ + fs.whereSql() + " GROUP BY DATE(v.fecha) ORDER BY 1";
        return toChart(ejecutarNativa(sql, fs.params()), 0, 1, true);
    }

    /* ---------- Utilidades internas ---------- */

    private Map<String, Object> respuestaTabla(String titulo, List<String> columnas, List<Map<String, Object>> filas) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("titulo", titulo);
        res.put("columnas", columnas);
        res.put("filas", filas);
        res.put("total", filas.size());
        return res;
    }

    private Map<String, Object> tablaAgregada(String sql, List<Object> params, List<String> columnas,
                                              int[] idx, String titulo) {
        List<Object[]> rows = ejecutarNativa(sql, params);
        List<Map<String, Object>> filas = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> f = new LinkedHashMap<>();
            for (int i = 0; i < columnas.size(); i++) {
                Object val = r[idx[i]];
                String col = columnas.get(i);
                if (col.toLowerCase().contains("total") || col.toLowerCase().contains("monto")
                        || col.toLowerCase().contains("venta") || col.toLowerCase().contains("costo")
                        || col.toLowerCase().contains("utilidad")) {
                    if (!(val instanceof Number && !(col.equals("Ventas") || col.equals("Operaciones") || col.equals("Cantidad")))) {
                        f.put(col, val);
                    } else {
                        f.put(col, formatoMoneda(val));
                    }
                } else if (val instanceof java.sql.Date) {
                    f.put(col, ((java.sql.Date) val).toLocalDate().format(FMT_DIA));
                } else {
                    f.put(col, val);
                }
            }
            filas.add(f);
        }
        return respuestaTabla(titulo, columnas, filas);
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> ejecutarNativa(String sql, List<Object> params) {
        Query q = entityManager.createNativeQuery(sqlDeMotor(sql));
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        return q.getResultList();
    }

    private BigDecimal scalarDecimal(String sql, List<Object> params) {
        Query q = entityManager.createNativeQuery(sqlDeMotor(sql));
        for (int i = 0; i < params.size(); i++) {
            q.setParameter(i + 1, params.get(i));
        }
        Object r = q.getSingleResult();
        return toBigDecimal(r);
    }

    private long scalarLong(String sql, List<Object> params) {
        return scalarDecimal(sql, params).longValue();
    }

    private String sqlDeMotor(String sql) {
        if (urlBaseDatos != null && urlBaseDatos.toLowerCase(Locale.ROOT).contains("postgresql")) {
            return SqlMySqlAPostgres.convertir(sql);
        }
        return sql;
    }

    private BigDecimal calcularUtilidad(ReporteFiltroDto filtro) {
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(filtro, "v");
        return scalarDecimal("""
                SELECT COALESCE(SUM(dv.subtotal - dv.cantidad * COALESCE(pp.precio_compra, p.precio_compra, 0)), 0)
                FROM DETALLE_VENTA dv
                INNER JOIN VENTA v ON dv.id_venta = v.id
                INNER JOIN producto p ON dv.id_producto = p.id
                LEFT JOIN producto_presentacion pp ON dv.id_presentacion = pp.id
                """ + fs.whereSql(), fs.params());
    }

    private BigDecimal ventasPorCanal(ReporteFiltroDto filtro, String canal) {
        ReporteFiltroDto f = filtroConCanal(filtro, canal);
        ReporteQueryBuilder.FiltroSql fs = ReporteQueryBuilder.construirFiltroVentas(f, "v");
        return scalarDecimal("SELECT COALESCE(SUM(v.total), 0) FROM VENTA v " + fs.whereSql(), fs.params());
    }

    private BigDecimal calcularCrecimiento(ReporteFiltroDto filtro) {
        Map<String, Object> rango = ReporteQueryBuilder.resolverRangoFechas(filtro);
        LocalDate desde = (LocalDate) rango.get("desdeFecha");
        LocalDate hasta = (LocalDate) rango.get("hastaFecha");
        long dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) + 1;
        LocalDate prevDesde = desde.minusDays(dias);
        LocalDate prevHasta = desde.minusDays(1);

        BigDecimal actual = scalarDecimal("""
                SELECT COALESCE(SUM(total), 0) FROM VENTA
                WHERE fecha >= ? AND fecha <= ? AND estado <> 'ANULADO'
                """, List.of(desde.atStartOfDay(), hasta.atTime(java.time.LocalTime.MAX)));
        BigDecimal anterior = scalarDecimal("""
                SELECT COALESCE(SUM(total), 0) FROM VENTA
                WHERE fecha >= ? AND fecha <= ? AND estado <> 'ANULADO'
                """, List.of(prevDesde.atStartOfDay(), prevHasta.atTime(java.time.LocalTime.MAX)));

        if (anterior.compareTo(BigDecimal.ZERO) == 0) {
            return actual.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toChart(List<Object[]> rows, int labelIdx, int valueIdx, boolean formatearFecha) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        for (Object[] r : rows) {
            Object lbl = r[labelIdx];
            if (formatearFecha && lbl instanceof java.sql.Date) {
                labels.add(((java.sql.Date) lbl).toLocalDate().format(FMT_DIA));
            } else {
                labels.add(lbl != null ? lbl.toString() : "");
            }
            valores.add(toBigDecimal(r[valueIdx]));
        }
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("labels", labels);
        c.put("valores", valores);
        return c;
    }

    private ReporteFiltroDto filtroAnuladas(ReporteFiltroDto filtro) {
        ReporteFiltroDto f = new ReporteFiltroDto();
        if (filtro != null) {
            f.setFechaDesde(filtro.getFechaDesde());
            f.setFechaHasta(filtro.getFechaHasta());
            f.setPeriodo(filtro.getPeriodo());
            f.setClienteId(filtro.getClienteId());
            f.setVendedorId(filtro.getVendedorId());
            f.setCategoriaId(filtro.getCategoriaId());
            f.setProductoId(filtro.getProductoId());
            f.setMarca(filtro.getMarca());
            f.setTipoPagoId(filtro.getTipoPagoId());
            f.setTipoComprobanteId(filtro.getTipoComprobanteId());
            f.setCanal(filtro.getCanal());
        }
        f.setEstadoVenta("ANULADO");
        return f;
    }

    private ReporteFiltroDto filtroConCanal(ReporteFiltroDto filtro, String canal) {
        ReporteFiltroDto f = filtro != null ? filtro : new ReporteFiltroDto();
        f.setCanal(canal);
        return f;
    }

    private String normalizarTipo(String tipo) {
        return tipo != null ? tipo.trim().toUpperCase(Locale.ROOT) : "GENERAL";
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(val.toString());
    }

    private String formatoMoneda(Object val) {
        return "S/ " + toBigDecimal(val).setScale(2, RoundingMode.HALF_UP);
    }
}
