package com.test.desarrollo_web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class MotorBaseDatos {

    private final boolean postgres;

    public MotorBaseDatos(@Value("${spring.datasource.url:}") String url) {
        String valor = url == null ? "" : url.toLowerCase(Locale.ROOT);
        this.postgres = valor.startsWith("jdbc:postgresql:");
    }

    public boolean esPostgres() {
        return postgres;
    }
}
