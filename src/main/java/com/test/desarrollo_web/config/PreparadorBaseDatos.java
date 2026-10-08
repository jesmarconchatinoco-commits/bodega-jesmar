package com.test.desarrollo_web.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Render entrega la base Postgres como postgres://usuario:clave@host/base.
 * Spring necesita jdbc:postgresql:// y el usuario aparte.
 */
public final class PreparadorBaseDatos {

    private PreparadorBaseDatos() {
    }

    public static void aplicar() {
        String raw = primero(System.getenv("DATABASE_URL"), System.getenv("DB_URL"));
        if (raw == null || raw.isBlank()) {
            System.out.println("DB_URL_USADA=no definida, usa localhost");
            return;
        }
        raw = raw.trim();
        if ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'"))) {
            raw = raw.substring(1, raw.length() - 1).trim();
        }

        if (raw.startsWith("jdbc:mysql:")) {
            System.setProperty("spring.datasource.url", raw);
            System.out.println("DB_URL_USADA=" + raw);
            System.out.println("MOTOR_BASE=mysql");
            return;
        }
        if (raw.startsWith("jdbc:postgresql:")) {
            activarPostgres(raw, null, null);
            return;
        }
        if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
            String sinEsquema = raw.replaceFirst("^postgres(ql)?://", "");
            String usuario = null;
            String clave = null;
            String resto = sinEsquema;
            int arroba = sinEsquema.lastIndexOf('@');
            if (arroba >= 0) {
                String credenciales = sinEsquema.substring(0, arroba);
                resto = sinEsquema.substring(arroba + 1);
                int dosPuntos = credenciales.indexOf(':');
                if (dosPuntos >= 0) {
                    usuario = decodificar(credenciales.substring(0, dosPuntos));
                    clave = decodificar(credenciales.substring(dosPuntos + 1));
                } else {
                    usuario = decodificar(credenciales);
                }
            }
            activarPostgres("jdbc:postgresql://" + resto, usuario, clave);
            return;
        }

        System.setProperty("spring.datasource.url", raw);
        System.out.println("DB_URL_USADA=" + raw);
    }

    private static void activarPostgres(String jdbcUrl, String usuario, String clave) {
        System.setProperty("spring.datasource.url", jdbcUrl);
        System.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        System.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        if (usuario != null && !usuario.isBlank()) {
            System.setProperty("spring.datasource.username", usuario);
            System.setProperty("DB_USERNAME", usuario);
        }
        if (clave != null) {
            System.setProperty("spring.datasource.password", clave);
            System.setProperty("DB_PASSWORD", clave);
        }
        System.out.println("DB_URL_USADA=" + jdbcUrl);
        System.out.println("MOTOR_BASE=postgresql");
    }

    private static String primero(String preferida, String alternativa) {
        if (preferida != null && !preferida.isBlank()) {
            return preferida;
        }
        return alternativa;
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }
}
