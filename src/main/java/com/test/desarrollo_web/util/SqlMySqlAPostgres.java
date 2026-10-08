package com.test.desarrollo_web.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Traduce funciones de MySQL usadas en los reportes para que también corran en PostgreSQL.
 */
public final class SqlMySqlAPostgres {

    private static final Pattern INTERVALO = Pattern.compile(
            "(?i)CURRENT_DATE\\s*,\\s*INTERVAL\\s+(\\d+)\\s+(DAY|MONTH|YEAR)");

    private SqlMySqlAPostgres() {
    }

    public static String convertir(String sql) {
        if (sql == null || sql.isBlank()) {
            return sql;
        }
        String resultado = sql.replaceAll("(?i)CURDATE\\s*\\(\\s*\\)", "CURRENT_DATE");
        resultado = reemplazarLlamada(resultado, "DATE_SUB", SqlMySqlAPostgres::intervalo);
        resultado = reemplazarLlamada(resultado, "SUBSTRING_INDEX", SqlMySqlAPostgres::splitPart);
        resultado = reemplazarLlamada(resultado, "HOUR", args -> "EXTRACT(HOUR FROM " + args + ")");
        resultado = reemplazarLlamada(resultado, "YEAR", args -> "CAST(EXTRACT(YEAR FROM " + args + ") AS INTEGER)");
        resultado = reemplazarLlamada(resultado, "MONTH", args -> "CAST(EXTRACT(MONTH FROM " + args + ") AS INTEGER)");
        resultado = reemplazarLlamada(resultado, "DATE", args -> "CAST(" + args + " AS DATE)");
        return resultado.replace("AS INTEGER), 2, '0'", "AS TEXT), 2, '0'");
    }

    private static String intervalo(String args) {
        Matcher matcher = INTERVALO.matcher(args.trim());
        if (!matcher.matches()) {
            return "DATE_SUB(" + args + ")";
        }
        String unidad = switch (matcher.group(2).toUpperCase(Locale.ROOT)) {
            case "MONTH" -> "months";
            case "YEAR" -> "years";
            default -> "days";
        };
        return "(CURRENT_DATE - INTERVAL '" + matcher.group(1) + " " + unidad + "')";
    }

    private static String splitPart(String args) {
        List<String> partes = separarArgumentos(args);
        if (partes.size() == 3 && "1".equals(partes.get(2).trim())) {
            return "split_part(" + partes.get(0) + ", " + partes.get(1) + ", 1)";
        }
        return "SUBSTRING_INDEX(" + args + ")";
    }

    private static String reemplazarLlamada(String sql, String nombre, Function<String, String> traduccion) {
        String mayusculas = sql.toUpperCase(Locale.ROOT);
        String token = nombre.toUpperCase(Locale.ROOT) + "(";
        StringBuilder salida = new StringBuilder();
        int indice = 0;
        while (indice < sql.length()) {
            int inicio = indiceDeFuncion(mayusculas, token, indice);
            if (inicio < 0) {
                salida.append(sql, indice, sql.length());
                break;
            }
            salida.append(sql, indice, inicio);
            int parentesis = inicio + token.length() - 1;
            int cierre = cerrarParentesis(sql, parentesis);
            if (cierre < 0) {
                salida.append(sql.substring(inicio));
                break;
            }
            salida.append(traduccion.apply(sql.substring(parentesis + 1, cierre)));
            indice = cierre + 1;
        }
        return salida.toString();
    }

    private static int indiceDeFuncion(String mayusculas, String token, int desde) {
        int indice = desde;
        while (indice < mayusculas.length()) {
            int hallado = mayusculas.indexOf(token, indice);
            if (hallado < 0) {
                return -1;
            }
            if (hallado == 0 || !Character.isLetterOrDigit(mayusculas.charAt(hallado - 1))) {
                return hallado;
            }
            indice = hallado + 1;
        }
        return -1;
    }

    private static int cerrarParentesis(String sql, int apertura) {
        int nivel = 0;
        for (int i = apertura; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                nivel++;
            } else if (c == ')') {
                nivel--;
                if (nivel == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static List<String> separarArgumentos(String args) {
        List<String> partes = new ArrayList<>();
        int nivel = 0;
        int inicio = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '(') {
                nivel++;
            } else if (c == ')') {
                nivel--;
            } else if (c == ',' && nivel == 0) {
                partes.add(args.substring(inicio, i).trim());
                inicio = i + 1;
            }
        }
        partes.add(args.substring(inicio).trim());
        return partes;
    }
}
