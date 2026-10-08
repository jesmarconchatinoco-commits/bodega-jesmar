package com.test.desarrollo_web.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Render entrega la base como postgres://usuario:clave@host/base.
 * Esta fuente se pone delante de application.properties para que el controlador
 * de MySQL no vuelva a imponerse.
 */
public class PreparadorBaseDatos implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = limpiar(primero(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DB_URL"),
                System.getenv("DATABASE_URL"),
                System.getenv("DB_URL")));
        if (raw == null) {
            System.out.println("DB_URL_USADA=no definida, usa localhost");
            return;
        }
        if (!esPostgres(raw)) {
            System.out.println("DB_URL_USADA=" + raw);
            System.out.println("MOTOR_BASE=mysql");
            return;
        }

        String usuario = null;
        String clave = null;
        String jdbcUrl = raw;
        if (raw.regionMatches(true, 0, "postgres://", 0, "postgres://".length())
                || raw.regionMatches(true, 0, "postgresql://", 0, "postgresql://".length())) {
            jdbcUrl = "jdbc:postgresql://" + sinCredenciales(raw.replaceFirst("(?i)^postgres(ql)?://", ""));
            String credenciales = credenciales(raw.replaceFirst("(?i)^postgres(ql)?://", ""));
            if (credenciales != null) {
                int dosPuntos = credenciales.indexOf(':');
                if (dosPuntos >= 0) {
                    usuario = decodificar(credenciales.substring(0, dosPuntos));
                    clave = decodificar(credenciales.substring(dosPuntos + 1));
                } else {
                    usuario = decodificar(credenciales);
                }
            }
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
        props.put("spring.jpa.database-platform", "org.hibernate.dialect.PostgreSQLDialect");
        if (usuario != null && !usuario.isBlank()) {
            props.put("spring.datasource.username", usuario);
        }
        if (clave != null) {
            props.put("spring.datasource.password", clave);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("conexionRender", props));
        System.out.println("DB_URL_USADA=" + jdbcUrl);
        System.out.println("MOTOR_BASE=postgresql");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static String primero(String... valores) {
        if (valores == null) {
            return null;
        }
        for (String valor : valores) {
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }

    private static String limpiar(String raw) {
        if (raw == null) {
            return null;
        }
        String valor = raw.trim();
        if ((valor.startsWith("\"") && valor.endsWith("\"")) || (valor.startsWith("'") && valor.endsWith("'"))) {
            valor = valor.substring(1, valor.length() - 1).trim();
        }
        return valor.isBlank() ? null : valor;
    }

    private static boolean esPostgres(String raw) {
        String valor = raw.toLowerCase(Locale.ROOT);
        return valor.startsWith("postgres://")
                || valor.startsWith("postgresql://")
                || valor.startsWith("jdbc:postgresql:");
    }

    private static String credenciales(String sinEsquema) {
        int arroba = sinEsquema.lastIndexOf('@');
        if (arroba < 0) {
            return null;
        }
        return sinEsquema.substring(0, arroba);
    }

    private static String sinCredenciales(String sinEsquema) {
        int arroba = sinEsquema.lastIndexOf('@');
        if (arroba < 0) {
            return sinEsquema;
        }
        return sinEsquema.substring(arroba + 1);
    }

    private static String decodificar(String valor) {
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return valor;
        }
    }
}
