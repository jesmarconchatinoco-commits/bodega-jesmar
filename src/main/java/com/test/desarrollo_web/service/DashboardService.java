package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.PedidoCatalogo;
import com.test.desarrollo_web.Models.Producto;
import com.test.desarrollo_web.Models.Ventas;
import com.test.desarrollo_web.Repository.*;
import com.test.desarrollo_web.dto.TopProductoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class DashboardService {

    private static final DateTimeFormatter FMT_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FMT_MES = DateTimeFormatter.ofPattern("MMM yyyy", Locale.of("es", "PE"));

    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoCatalogoRepository pedidoCatalogoRepository;

    public DashboardService(VentaRepository ventaRepository,
                            DetalleVentaRepository detalleVentaRepository,
                            ProductoRepository productoRepository,
                            ClienteRepository clienteRepository,
                            PedidoCatalogoRepository pedidoCatalogoRepository) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoCatalogoRepository = pedidoCatalogoRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumen() {
        Map<String, Object> resumen = new LinkedHashMap<>();

        BigDecimal ventasDelDia = nvl(ventaRepository.sumVentasDelDia());
        BigDecimal ventasDelMes = nvl(ventaRepository.sumVentasDelMes());

        resumen.put("ventasDelDia", ventasDelDia);
        resumen.put("ventasDelMes", ventasDelMes);
        resumen.put("cantidadVentasHoy", ventaRepository.countVentasDelDia());
        resumen.put("cantidadVentasMes", ventaRepository.countVentasDelMes());
        resumen.put("totalClientes", clienteRepository.count());
        resumen.put("totalProductosActivos", productoRepository.countByEstado(Producto.Estado.ACTIVO));
        resumen.put("pedidosWebPendientes", pedidoCatalogoRepository.countByEstado(PedidoCatalogo.Estado.PENDIENTE));
        resumen.put("productosStockBajo", productoRepository.countStockBajo());

        resumen.put("topProductos", buildTopProductos());
        resumen.put("ventasRecientes", buildVentasRecientes());
        resumen.put("pedidosRecientes", buildPedidosRecientes());
        resumen.put("stockBajo", buildStockBajo());
        resumen.put("ventasUltimos7Dias", buildVentasUltimos7Dias());
        resumen.put("ventasPorCanal", buildVentasPorCanal());
        resumen.put("gananciasVentas", obtenerGanancias("DIA"));

        return resumen;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerGanancias(String periodo) {
        String filtro = periodo != null ? periodo.trim().toUpperCase(Locale.ROOT) : "DIA";
        return switch (filtro) {
            case "MES" -> buildGananciasPorMes();
            case "ANIO", "AÑO" -> buildGananciasPorAnio();
            default -> buildGananciasPorDia();
        };
    }

    private Map<String, Object> buildGananciasPorDia() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(6).atStartOfDay();
        Map<LocalDate, BigDecimal> montos = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            montos.put(hoy.minusDays(i), BigDecimal.ZERO);
        }
        acumularGanancias(montos, detalleVentaRepository.findLineasGananciaDesde(desde));

        return toChartGanancias("DIA", "Últimos 7 días", montos.entrySet().stream()
                .map(e -> Map.entry(e.getKey().format(FMT_DIA), e.getValue()))
                .toList());
    }

    private Map<String, Object> buildGananciasPorMes() {
        LocalDate hoy = LocalDate.now().withDayOfMonth(1);
        LocalDateTime desde = hoy.minusMonths(11).atStartOfDay();
        Map<YearMonth, BigDecimal> montos = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            montos.put(YearMonth.from(hoy.minusMonths(i)), BigDecimal.ZERO);
        }
        acumularGananciasMes(montos, detalleVentaRepository.findLineasGananciaDesde(desde));

        return toChartGanancias("MES", "Últimos 12 meses", montos.entrySet().stream()
                .map(e -> Map.entry(capitalizarMes(e.getKey().atDay(1).format(FMT_MES)), e.getValue()))
                .toList());
    }

    private Map<String, Object> buildGananciasPorAnio() {
        int anioActual = LocalDate.now().getYear();
        LocalDateTime desde = LocalDate.of(anioActual - 4, 1, 1).atStartOfDay();
        Map<Integer, BigDecimal> montos = new LinkedHashMap<>();
        for (int i = 4; i >= 0; i--) {
            montos.put(anioActual - i, BigDecimal.ZERO);
        }
        acumularGananciasAnio(montos, detalleVentaRepository.findLineasGananciaDesde(desde));

        List<Map.Entry<String, BigDecimal>> entries = montos.entrySet().stream()
                .map(e -> Map.entry(String.valueOf(e.getKey()), e.getValue()))
                .toList();
        return toChartGanancias("ANIO", "Últimos 5 años", entries);
    }

    private void acumularGanancias(Map<LocalDate, BigDecimal> montos, List<Object[]> lineas) {
        for (Object[] row : lineas) {
            if (row[0] == null) continue;
            LocalDateTime fecha = (LocalDateTime) row[0];
            BigDecimal ganancia = calcularGananciaLinea(row);
            LocalDate clave = fecha.toLocalDate();
            if (montos.containsKey(clave)) {
                montos.put(clave, montos.get(clave).add(ganancia));
            }
        }
    }

    private void acumularGananciasMes(Map<YearMonth, BigDecimal> montos, List<Object[]> lineas) {
        for (Object[] row : lineas) {
            if (row[0] == null) continue;
            YearMonth clave = YearMonth.from((LocalDateTime) row[0]);
            if (montos.containsKey(clave)) {
                montos.put(clave, montos.get(clave).add(calcularGananciaLinea(row)));
            }
        }
    }

    private void acumularGananciasAnio(Map<Integer, BigDecimal> montos, List<Object[]> lineas) {
        for (Object[] row : lineas) {
            if (row[0] == null) continue;
            int clave = ((LocalDateTime) row[0]).getYear();
            if (montos.containsKey(clave)) {
                montos.put(clave, montos.get(clave).add(calcularGananciaLinea(row)));
            }
        }
    }

    private BigDecimal calcularGananciaLinea(Object[] row) {
        BigDecimal precioVenta = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
        int cantidad = row[2] != null ? ((Number) row[2]).intValue() : 0;
        BigDecimal precioCompra = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
        return precioVenta.subtract(precioCompra)
                .multiply(BigDecimal.valueOf(cantidad))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> toChartGanancias(String periodo, String titulo, List<Map.Entry<String, BigDecimal>> entries) {
        List<String> labels = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : entries) {
            labels.add(entry.getKey());
            BigDecimal valor = entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO;
            valores.add(valor);
            total = total.add(valor);
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("periodo", periodo);
        chart.put("titulo", titulo);
        chart.put("labels", labels);
        chart.put("valores", valores);
        chart.put("total", total);
        return chart;
    }

    private String capitalizarMes(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return texto.substring(0, 1).toUpperCase(Locale.ROOT) + texto.substring(1);
    }

    private List<TopProductoDto> buildTopProductos() {
        List<TopProductoDto> productos = new ArrayList<>();
        for (Object[] row : detalleVentaRepository.findTop5ProductosVendidos()) {
            String nombre = row[0] != null ? row[0].toString() : "—";
            Long cantidad = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            BigDecimal total = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            productos.add(new TopProductoDto(nombre, cantidad, total));
        }
        return productos;
    }

    private List<Map<String, Object>> buildVentasRecientes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Ventas venta : ventaRepository.findTop5Recientes(Ventas.Estado.ANULADO)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", venta.getId());
            item.put("numeroDocumento", venta.getNumeroDocumento());
            item.put("cliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "—");
            item.put("total", venta.getTotal());
            item.put("fecha", venta.getFecha() != null ? venta.getFecha().toString() : null);
            item.put("estado", venta.getEstado() != null ? venta.getEstado().name() : "PAGADO");
            lista.add(item);
        }
        return lista;
    }

    private List<Map<String, Object>> buildPedidosRecientes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (PedidoCatalogo pedido : pedidoCatalogoRepository.findTop5ByOrderByFechaDesc()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pedido.getId());
            item.put("nombreCliente", pedido.getNombreCliente());
            item.put("telefono", pedido.getTelefono());
            item.put("total", pedido.getTotal());
            item.put("fecha", pedido.getFecha() != null ? pedido.getFecha().toString() : null);
            item.put("estado", pedido.getEstado() != null ? pedido.getEstado().name() : "PENDIENTE");
            lista.add(item);
        }
        return lista;
    }

    private List<Map<String, Object>> buildStockBajo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Producto producto : productoRepository.findStockBajo()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", producto.getId());
            item.put("nombre", producto.getNombre());
            item.put("stock", producto.getStock());
            item.put("stockMinimo", producto.getStockMinimo());
            item.put("categoria", producto.getCategoria() != null ? producto.getCategoria().getNombre() : "—");
            lista.add(item);
        }
        return lista;
    }

    private Map<String, Object> buildVentasUltimos7Dias() {
        LocalDate hoy = LocalDate.now();
        Map<LocalDate, BigDecimal> montos = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            montos.put(hoy.minusDays(i), BigDecimal.ZERO);
        }

        for (Object[] row : ventaRepository.sumVentasPorDiaUltimos7()) {
            if (row[0] == null) continue;
            LocalDate dia = row[0] instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate()
                    : LocalDate.parse(row[0].toString());
            BigDecimal monto = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            montos.put(dia, monto);
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : montos.entrySet()) {
            labels.add(entry.getKey().format(FMT_DIA));
            valores.add(entry.getValue());
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("labels", labels);
        chart.put("valores", valores);
        return chart;
    }

    private Map<String, Object> buildVentasPorCanal() {
        BigDecimal ventasPos = BigDecimal.ZERO;
        BigDecimal ventaWeb = BigDecimal.ZERO;

        for (Object[] row : ventaRepository.sumVentasPorCanalMes()) {
            String canal = row[0] != null ? row[0].toString() : "";
            BigDecimal monto = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
            if ("Venta por Web".equalsIgnoreCase(canal)) {
                ventaWeb = monto;
            } else {
                ventasPos = monto;
            }
        }

        Map<String, Object> chart = new LinkedHashMap<>();
        if (ventasPos.compareTo(BigDecimal.ZERO) == 0 && ventaWeb.compareTo(BigDecimal.ZERO) == 0) {
            chart.put("labels", List.of());
            chart.put("valores", List.of());
            return chart;
        }

        chart.put("labels", List.of("Ventas POS", "Venta por Web"));
        chart.put("valores", List.of(ventasPos, ventaWeb));
        return chart;
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
