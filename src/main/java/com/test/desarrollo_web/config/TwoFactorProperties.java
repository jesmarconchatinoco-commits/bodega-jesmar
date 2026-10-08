package com.test.desarrollo_web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.2fa")
public class TwoFactorProperties {

    private boolean habilitado = true;
    private int codigoExpiracionMinutos = 5;
    private boolean mostrarCodigoEnPantalla = false;

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public int getCodigoExpiracionMinutos() {
        return codigoExpiracionMinutos;
    }

    public void setCodigoExpiracionMinutos(int codigoExpiracionMinutos) {
        this.codigoExpiracionMinutos = codigoExpiracionMinutos;
    }

    public boolean isMostrarCodigoEnPantalla() {
        return mostrarCodigoEnPantalla;
    }

    public void setMostrarCodigoEnPantalla(boolean mostrarCodigoEnPantalla) {
        this.mostrarCodigoEnPantalla = mostrarCodigoEnPantalla;
    }
}
