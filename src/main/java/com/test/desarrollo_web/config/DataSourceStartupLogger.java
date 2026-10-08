package com.test.desarrollo_web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DataSourceStartupLogger {

    private static final Logger log = LoggerFactory.getLogger(DataSourceStartupLogger.class);

    public DataSourceStartupLogger(@Value("${spring.datasource.url:}") String url,
                                   @Value("${spring.datasource.username:}") String username) {
        log.warn("Base de datos configurada. Usuario: {}. URL: {}", username, url);
    }
}
