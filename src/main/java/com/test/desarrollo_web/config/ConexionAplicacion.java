package com.test.desarrollo_web.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * Crea la conexión leyendo las variables de Render, sin dejar que
 * application.properties vuelva a elegir el controlador de MySQL.
 */
@Configuration
public class ConexionAplicacion {

    @Bean
    public DataSource dataSource(Environment environment) {
        Datos datos = Datos.leer(environment);
        System.out.println("ARRANQUE_DB_URL=" + datos.origen);
        System.out.println("MOTOR_BASE=" + datos.motor);
        System.out.println("DB_URL_USADA=" + datos.urlVisible);

        HikariDataSource pool = new HikariDataSource();
        pool.setDriverClassName(datos.driver);
        pool.setJdbcUrl(datos.jdbc);
        pool.setUsername(datos.usuario);
        pool.setPassword(datos.clave == null ? "" : datos.clave);
        if (!datos.postgres) {
            pool.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            pool.addDataSourceProperty("useSSL", "false");
            pool.addDataSourceProperty("serverTimezone", "UTC");
        }
        return pool;
    }

    @Bean
    public HibernatePropertiesCustomizer dialecto(Environment environment) {
        Datos datos = Datos.leer(environment);
        return propiedades -> propiedades.put("hibernate.dialect", datos.dialecto);
    }

    static final class Datos {
        final boolean postgres;
        final String jdbc;
        final String usuario;
        final String clave;
        final String driver;
        final String dialecto;
        final String motor;
        final String origen;
        final String urlVisible;

        private Datos(boolean postgres, String jdbc, String usuario, String clave, String origen) {
            this.postgres = postgres;
            this.jdbc = jdbc;
            this.usuario = usuario;
            this.clave = clave;
            this.origen = origen;
            this.driver = postgres ? "org.postgresql.Driver" : "com.mysql.cj.jdbc.Driver";
            this.dialecto = postgres
                    ? "org.hibernate.dialect.PostgreSQLDialect"
                    : "org.hibernate.dialect.MySQLDialect";
            this.motor = postgres ? "postgresql" : "mysql";
            this.urlVisible = visible(jdbc);
        }

        static Datos leer(Environment environment) {
            Hallazgo hallada = buscarPostgres();
            if (hallada != null) {
                return desdePostgres(hallada.valor(), hallada.origen(), environment);
            }
            String url = environment.getProperty(
                    "spring.datasource.url",
                    "jdbc:mysql://localhost:3306/sistema_ventas");
            return new Datos(false, url,
                    environment.getProperty("spring.datasource.username", "bodega_app"),
                    environment.getProperty("spring.datasource.password", ""),
                    "sin-postgres");
        }

        private static Hallazgo buscarPostgres() {
            Hallazgo directo = primero(
                    "DB_URL", System.getenv("DB_URL"),
                    "DATABASE_URL", System.getenv("DATABASE_URL"));
            if (directo != null) {
                return directo;
            }
            for (Map.Entry<String, String> entrada : System.getenv().entrySet()) {
                if (esPostgres(entrada.getValue())) {
                    return new Hallazgo(entrada.getKey(), entrada.getValue());
                }
            }
            return null;
        }

        private static Hallazgo primero(String nombre1, String valor1, String nombre2, String valor2) {
            if (esPostgres(valor1)) {
                return new Hallazgo(nombre1, valor1);
            }
            if (esPostgres(valor2)) {
                return new Hallazgo(nombre2, valor2);
            }
            return null;
        }

        private static Datos desdePostgres(String raw, String origen, Environment environment) {
            String limpio = recortar(limpiar(raw));
            String usuario = environment.getProperty("spring.datasource.username", "");
            String clave = environment.getProperty("spring.datasource.password", "");
            String jdbc = limpio;
            if (!limpio.toLowerCase(Locale.ROOT).startsWith("jdbc:")) {
                String sinEsquema = limpio.replaceFirst("(?i)^postgres(ql)?://", "");
                jdbc = "jdbc:postgresql://" + despuesDeArroba(sinEsquema);
                String credenciales = antesDeArroba(sinEsquema);
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
            return new Datos(true, jdbc, usuario, clave, origen);
        }

        private static boolean esPostgres(String valor) {
            if (valor == null || valor.isBlank()) {
                return false;
            }
            String texto = valor.toLowerCase(Locale.ROOT);
            return texto.contains("postgres://")
                    || texto.contains("postgresql://")
                    || texto.contains("jdbc:postgresql:");
        }

        private static String limpiar(String raw) {
            String valor = raw.replace("\uFEFF", "").trim();
            if ((valor.startsWith("\"") && valor.endsWith("\"")) || (valor.startsWith("'") && valor.endsWith("'"))) {
                valor = valor.substring(1, valor.length() - 1).trim();
            }
            return valor;
        }

        private static String recortar(String raw) {
            String valor = raw.toLowerCase(Locale.ROOT);
            int jdbc = valor.indexOf("jdbc:postgresql:");
            int largo = valor.indexOf("postgresql://");
            int corto = valor.indexOf("postgres://");
            int inicio = jdbc >= 0 ? jdbc : (largo >= 0 ? largo : corto);
            return inicio > 0 ? raw.substring(inicio) : raw;
        }

        private static String antesDeArroba(String texto) {
            int arroba = texto.lastIndexOf('@');
            return arroba < 0 ? null : texto.substring(0, arroba);
        }

        private static String despuesDeArroba(String texto) {
            int arroba = texto.lastIndexOf('@');
            return arroba < 0 ? texto : texto.substring(arroba + 1);
        }

        private static String decodificar(String valor) {
            try {
                return URLDecoder.decode(valor, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return valor;
            }
        }

        private static String visible(String jdbc) {
            int esquema = jdbc.indexOf("://");
            int arroba = jdbc.lastIndexOf('@');
            if (esquema >= 0 && arroba > esquema) {
                return jdbc.substring(0, esquema + 3) + jdbc.substring(arroba + 1);
            }
            return jdbc;
        }
    }

    private record Hallazgo(String origen, String valor) {
    }
}
