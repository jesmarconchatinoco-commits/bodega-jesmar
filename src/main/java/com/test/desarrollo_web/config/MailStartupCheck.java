package com.test.desarrollo_web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MailStartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MailStartupCheck.class);

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.warn("================================================================");
            log.warn("2FA: spring.mail.password NO configurada.");
            log.warn("Edite src/main/resources/application.properties");
            log.warn("Contraseña de aplicación: https://myaccount.google.com/apppasswords");
            log.warn("================================================================");
        } else {
            log.info("2FA: correo configurado para {}", username);
        }
    }
}
