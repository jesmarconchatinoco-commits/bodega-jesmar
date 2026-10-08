package com.test.desarrollo_web.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
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
 * Se aplica al final del arranque para que application.properties no vuelva a imponer MySQL.
 */
public class PreparadorBaseDatos implements EnvironmentPostProcessor, ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        aplicar(environment);
    }

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        aplicar(context.getEnvironment());
    }

    private void aplicar(ConfigurableEnvironment environment) {
        String raw = direccion(environment);
        Map<String, Object> props = new LinkedHashMap<>();
        if (raw == null || !esPostgres(raw)) {
            props.put("spring.datasource.driver-class-name", "com.mysql.cj.jdbc.Driver");
            props.put("spring.jpa.database-platform", "org.hibernate.dialect.MySQLDialect");
            environment.getPropertySources().addFirst(new MapPropertySource("conexionRender", props));
            System.out.println("DB_URL_USADA=" + (raw == null ? "no definida, usa localhost" : "mysql"));
            System.out.println("MOTOR_BASE=mysql");
            return;
        }

        raw = recortar(raw);
        String usuario = null;
        String clave = null;
        String jdbcUrl = raw;
        if (!raw.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
            String sinEsquema = raw.replaceFirst("(?i)^postgres(ql)?://", "");
            jdbcUrl = "jdbc:postgresql://" + sinCredenciales(sinEsquema);
            String credenciales = credenciales(sinEsquema);
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
        System.out.println("DB_URL_USADA=" + sinClave(jdbcUrl));
        System.out.println("MOTOR_BASE=postgresql");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static String direccion(ConfigurableEnvironment environment) {
        String postgres = null;
        String cualquiera = null;
        for (String valor : new String[]{
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("DB_URL"),
                System.getenv("DATABASE_URL"),
                System.getenv("DB_URL"),
                environment.getProperty("spring.datasource.url")
        }) {
            String limpio = limpiar(valor);
            if (limpio == null) {
                continue;
            }
            if (esPostgres(limpio)) {
                postgres = limpio;
                break;
            }
            if (cualquiera == null) {
                cualquiera = limpio;
            }
        }
        if (postgres == null) {
            for (Map.Entry<String, String> entrada : System.getenv().entrySet()) {
                String limpio = limpiar(entrada.getValue());
                if (limpio != null && esPostgres(limpio)) {
                    System.out.println("DB_URL_VARIABLE=" + entrada.getKey());
                    postgres = limpio;
                    break;
                }
            }
        }
        return postgres != null ? postgres : cualquiera;
    }

    private static String limpiar(String raw) {
        if (raw == null) {
            return null;
        }
        String valor = raw.replace("\uFEFF", "").trim();
        if ((valor.startsWith("\"") && valor.endsWith("\"")) || (valor.startsWith("'") && valor.endsWith("'"))) {
            valor = valor.substring(1, valor.length() - 1).trim();
        }
        return valor.isBlank() ? null : valor;
    }

    private static boolean esPostgres(String raw) {
        String valor = raw.toLowerCase(Locale.ROOT);
        return valor.contains("postgres://")
                || valor.contains("postgresql://")
                || valor.contains("jdbc:postgresql:");
    }

    private static String recortar(String raw) {
        String valor = raw.toLowerCase(Locale.ROOT);
        int jdbc = valor.indexOf("jdbc:postgresql:");
        int largo = valor.indexOf("postgresql://");
        int corto = valor.indexOf("postgres://");
        int inicio = jdbc >= 0 ? jdbc : (largo >= 0 ? largo : corto);
        return inicio > 0 ? raw.substring(inicio) : raw;
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

    private static String sinClave(String jdbcUrl) {
        int esquema = jdbcUrl.indexOf("://");
        int arroba = jdbcUrl.lastIndexOf('@');
        if (esquema >= 0 && arroba > esquema) {
            return jdbcUrl.substring(0, esquema + 3) + jdbcUrl.substring(arroba + 1);
        }
        return jdbcUrl;
    }

    private static String decodificar(String valor) {
        try {
            return URLDecoder.decode(valor, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return valor;
        }
    }
}
