package com.test.desarrollo_web.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PresentacionMedidaUtil {

    private static final Pattern PATRON_CANTIDAD_UNIDAD =
            Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s*(.+)$");

    private PresentacionMedidaUtil() {
    }

    public static Map<String, String> extraerMedida(String nombrePresentacion) {
        Map<String, String> medida = new LinkedHashMap<>();
        if (nombrePresentacion == null || nombrePresentacion.isBlank()) {
            medida.put("cantidad", "");
            medida.put("unidad", "");
            medida.put("medida", "—");
            return medida;
        }

        String limpio = nombrePresentacion.trim();
        Matcher matcher = PATRON_CANTIDAD_UNIDAD.matcher(limpio);
        if (matcher.matches()) {
            String cantidad = matcher.group(1).replace(',', '.');
            String unidad = matcher.group(2).trim();
            medida.put("cantidad", cantidad);
            medida.put("unidad", unidad);
            medida.put("medida", cantidad + " " + unidad);
        } else {
            medida.put("cantidad", "1");
            medida.put("unidad", limpio);
            medida.put("medida", limpio);
        }
        return medida;
    }
}
