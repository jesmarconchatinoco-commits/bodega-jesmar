package com.test.desarrollo_web.security;

public final class TwoFactorSessionKeys {

    public static final String PENDING_USER = "TWO_FACTOR_PENDING_USER";
    public static final String OTP_CODE = "TWO_FACTOR_OTP_CODE";
    public static final String OTP_EXPIRY = "TWO_FACTOR_OTP_EXPIRY";
    public static final String EMAIL_MASK = "TWO_FACTOR_EMAIL_MASK";
    public static final String RESEND_COUNT = "TWO_FACTOR_RESEND_COUNT";
    public static final String MOSTRAR_CODIGO_PANTALLA = "TWO_FACTOR_MOSTRAR_CODIGO_PANTALLA";
    public static final String ENVIO_EMAIL_OK = "TWO_FACTOR_ENVIO_EMAIL_OK";
    public static final String ERROR_ENVIO = "TWO_FACTOR_ERROR_ENVIO";
    public static final String CODIGO_CORRECTO = "TWO_FACTOR_CODIGO_CORRECTO";
    public static final String REDIRECT_AFTER = "TWO_FACTOR_REDIRECT_AFTER";

    private TwoFactorSessionKeys() {
    }
}
