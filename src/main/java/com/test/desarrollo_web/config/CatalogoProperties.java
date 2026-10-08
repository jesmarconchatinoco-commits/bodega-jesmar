package com.test.desarrollo_web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.catalogo")
public class CatalogoProperties {

    private BigDecimal costoEnvio = new BigDecimal("10.00");
    private String yapeCelular = "957779526";
    private String plinCelular = "957779526";

    public BigDecimal getCostoEnvio() {
        return costoEnvio;
    }

    public void setCostoEnvio(BigDecimal costoEnvio) {
        this.costoEnvio = costoEnvio;
    }

    public String getYapeCelular() {
        return yapeCelular;
    }

    public void setYapeCelular(String yapeCelular) {
        this.yapeCelular = yapeCelular;
    }

    public String getPlinCelular() {
        return plinCelular;
    }

    public void setPlinCelular(String plinCelular) {
        this.plinCelular = plinCelular;
    }
}
