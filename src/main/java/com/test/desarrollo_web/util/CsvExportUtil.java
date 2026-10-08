package com.test.desarrollo_web.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CsvExportUtil {

    private CsvExportUtil() {
    }

    public static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        String limpio = valor.replace("\"", "\"\"");
        if (limpio.contains(",") || limpio.contains("\"") || limpio.contains("\n") || limpio.contains("\r")) {
            return "\"" + limpio + "\"";
        }
        return limpio;
    }

    public static String generar(List<String> encabezados, List<List<String>> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", encabezados.stream().map(CsvExportUtil::escapar).toList()));
        sb.append("\n");
        for (List<String> fila : filas) {
            sb.append(String.join(",", fila.stream().map(CsvExportUtil::escapar).toList()));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static ResponseEntity<byte[]> respuesta(String nombreArchivo, String contenido) {
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] cuerpo = contenido.getBytes(StandardCharsets.UTF_8);
        byte[] salida = new byte[bom.length + cuerpo.length];
        System.arraycopy(bom, 0, salida, 0, bom.length);
        System.arraycopy(cuerpo, 0, salida, bom.length, cuerpo.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(salida);
    }
}
