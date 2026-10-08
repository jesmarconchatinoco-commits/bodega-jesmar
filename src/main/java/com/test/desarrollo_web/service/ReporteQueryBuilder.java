package com.test.desarrollo_web.service;

import com.test.desarrollo_web.dto.ReporteFiltroDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye fragmentos SQL reutilizables para consultas de reportes con filtros dinámicos.
 */
public final class ReporteQueryBuilder {

    private ReporteQueryBuilder() {
    }

    public static Map<String, Object> resolverRangoFechas(ReporteFiltroDto filtro) {
        LocalDate hoy = LocalDate.now();
        String periodo = filtro != null && filtro.getPeriodo() != null
                ? filtro.getPeriodo().trim().toUpperCase()
                : "TODO";

        LocalDate desde;
        LocalDate hasta = hoy;

        switch (periodo) {
            case "DIA" -> desde = hoy;
            case "SEMANA" -> desde = hoy.minusDays(6);
            case "ANIO" -> desde = LocalDate.of(hoy.getYear(), 1, 1);
            case "TODO" -> desde = LocalDate.of(2000, 1, 1);
            case "RANGO" -> {
                desde = filtro != null && filtro.getFechaDesde() != null
                        ? filtro.getFechaDesde() : hoy.minusMonths(1);
                hasta = filtro != null && filtro.getFechaHasta() != null
                        ? filtro.getFechaHasta() : hoy;
            }
            default -> desde = LocalDate.of(hoy.getYear(), hoy.getMonth(), 1);
        }

        if (desde.isAfter(hasta)) {
            LocalDate tmp = desde;
            desde = hasta;
            hasta = tmp;
        }

        Map<String, Object> rango = new HashMap<>();
        rango.put("desde", desde.atStartOfDay());
        rango.put("hasta", hasta.atTime(LocalTime.MAX));
        rango.put("desdeFecha", desde);
        rango.put("hastaFecha", hasta);
        return rango;
    }

    public static FiltroSql construirFiltroVentas(ReporteFiltroDto filtro, String aliasVenta) {
        Map<String, Object> rango = resolverRangoFechas(filtro);
        String v = aliasVenta != null ? aliasVenta : "v";

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        where.append(" AND ").append(v).append(".fecha >= ? ");
        params.add(rango.get("desde"));
        where.append(" AND ").append(v).append(".fecha <= ? ");
        params.add(rango.get("hasta"));

        if (filtro != null) {
            if (filtro.getEstadoVenta() != null && !filtro.getEstadoVenta().isBlank()) {
                where.append(" AND ").append(v).append(".estado = ? ");
                params.add(filtro.getEstadoVenta().trim().toUpperCase());
            } else {
                where.append(" AND ").append(v).append(".estado <> 'ANULADO' ");
            }

            if (filtro.getClienteId() != null) {
                where.append(" AND ").append(v).append(".id_cliente = ? ");
                params.add(filtro.getClienteId());
            }
            if (filtro.getVendedorId() != null) {
                where.append(" AND ").append(v).append(".id_vendedor = ? ");
                params.add(filtro.getVendedorId());
            }
            if (filtro.getTipoPagoId() != null) {
                where.append(" AND ").append(v).append(".id_tipo_pago = ? ");
                params.add(filtro.getTipoPagoId());
            }
            if (filtro.getTipoComprobanteId() != null) {
                where.append(" AND ").append(v).append(".id_tipo_comprobante = ? ");
                params.add(filtro.getTipoComprobanteId());
            }

            String canal = filtro.getCanal() != null ? filtro.getCanal().trim().toUpperCase() : "";
            if ("POS".equals(canal)) {
                where.append(" AND NOT EXISTS (SELECT 1 FROM PEDIDO_CATALOGO pc WHERE pc.venta_id = ")
                        .append(v).append(".id) ");
            } else if ("WEB".equals(canal)) {
                where.append(" AND EXISTS (SELECT 1 FROM PEDIDO_CATALOGO pc WHERE pc.venta_id = ")
                        .append(v).append(".id) ");
            }
        } else {
            where.append(" AND ").append(v).append(".estado <> 'ANULADO' ");
        }

        return new FiltroSql(where.toString(), params, rango);
    }

    public static String filtroProducto(ReporteFiltroDto filtro, String aliasProducto, List<Object> params) {
        if (filtro == null) {
            return "";
        }
        StringBuilder extra = new StringBuilder();
        String p = aliasProducto != null ? aliasProducto : "p";
        if (filtro.getProductoId() != null) {
            extra.append(" AND ").append(p).append(".id = ? ");
            params.add(filtro.getProductoId());
        }
        if (filtro.getCategoriaId() != null) {
            extra.append(" AND ").append(p).append(".id_categoria = ? ");
            params.add(filtro.getCategoriaId());
        }
        if (filtro.getMarca() != null && !filtro.getMarca().isBlank()) {
            extra.append(" AND LOWER(COALESCE(").append(p).append(".descripcion, '')) LIKE ? ");
            params.add("%" + filtro.getMarca().trim().toLowerCase() + "%");
        }
        return extra.toString();
    }

    public record FiltroSql(String whereSql, List<Object> params, Map<String, Object> rango) {
    }
}
