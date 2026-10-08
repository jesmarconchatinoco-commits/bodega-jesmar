package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import com.test.desarrollo_web.security.TwoFactorSessionKeys;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.test.desarrollo_web.config.TwoFactorProperties;
import com.test.desarrollo_web.security.SecurityNotificationService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
public class TwoFactorAuthService {

    private static final Logger log = LoggerFactory.getLogger(TwoFactorAuthService.class);
    private static final int MAX_REENVIOS = 3;

    private final UsuarioRepository usuarioRepository;
    private final SecurityNotificationService notificationService;
    private final TwoFactorProperties twoFactorProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public TwoFactorAuthService(UsuarioRepository usuarioRepository,
                                SecurityNotificationService notificationService,
                                TwoFactorProperties twoFactorProperties) {
        this.usuarioRepository = usuarioRepository;
        this.notificationService = notificationService;
        this.twoFactorProperties = twoFactorProperties;
    }

    public boolean estaHabilitado() {
        return twoFactorProperties.isHabilitado();
    }

    public void iniciarVerificacion(String username, HttpSession session) {
        Usuario usuario = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado para 2FA: " + username));

        String codigo = generarCodigo();
        long expira = Instant.now().plusSeconds(twoFactorProperties.getCodigoExpiracionMinutos() * 60L).toEpochMilli();

        session.setAttribute(TwoFactorSessionKeys.PENDING_USER, username);
        session.setAttribute(TwoFactorSessionKeys.OTP_CODE, codigo);
        session.setAttribute(TwoFactorSessionKeys.OTP_EXPIRY, expira);
        session.setAttribute(TwoFactorSessionKeys.EMAIL_MASK, enmascararCorreo(usuario.getCorreo()));
        session.setAttribute(TwoFactorSessionKeys.RESEND_COUNT, 0);

        boolean enviado = notificationService.enviarCodigoVerificacion(usuario, codigo);
        guardarEstadoEnvio(session, enviado);
        log.info("Código 2FA generado para usuario {} (expira en {} min)", username,
                twoFactorProperties.getCodigoExpiracionMinutos());
    }

    public boolean reenviarCodigo(HttpSession session) {
        String username = (String) session.getAttribute(TwoFactorSessionKeys.PENDING_USER);
        if (username == null) {
            return false;
        }
        int reenvios = Optional.ofNullable((Integer) session.getAttribute(TwoFactorSessionKeys.RESEND_COUNT))
                .orElse(0);
        if (reenvios >= MAX_REENVIOS) {
            return false;
        }

        Usuario usuario = usuarioRepository.findByUsuario(username).orElse(null);
        if (usuario == null) {
            return false;
        }

        String codigo = generarCodigo();
        long expira = Instant.now().plusSeconds(twoFactorProperties.getCodigoExpiracionMinutos() * 60L).toEpochMilli();
        session.setAttribute(TwoFactorSessionKeys.OTP_CODE, codigo);
        session.setAttribute(TwoFactorSessionKeys.OTP_EXPIRY, expira);
        session.setAttribute(TwoFactorSessionKeys.RESEND_COUNT, reenvios + 1);

        boolean enviado = notificationService.enviarCodigoVerificacion(usuario, codigo);
        guardarEstadoEnvio(session, enviado);
        return true;
    }

    private void guardarEstadoEnvio(HttpSession session, boolean emailEnviado) {
        session.setAttribute(TwoFactorSessionKeys.ENVIO_EMAIL_OK, emailEnviado);
        session.setAttribute(TwoFactorSessionKeys.ERROR_ENVIO, !emailEnviado);

        if (twoFactorProperties.isMostrarCodigoEnPantalla() && !emailEnviado) {
            session.setAttribute(TwoFactorSessionKeys.MOSTRAR_CODIGO_PANTALLA, true);
        } else {
            session.removeAttribute(TwoFactorSessionKeys.MOSTRAR_CODIGO_PANTALLA);
        }
    }

    public boolean validarCodigo(HttpSession session, String codigoIngresado) {
        if (codigoIngresado == null || !codigoIngresado.matches("\\d{6}")) {
            return false;
        }

        String codigoGuardado = (String) session.getAttribute(TwoFactorSessionKeys.OTP_CODE);
        Long expira = (Long) session.getAttribute(TwoFactorSessionKeys.OTP_EXPIRY);
        if (codigoGuardado == null || expira == null) {
            return false;
        }
        if (Instant.now().toEpochMilli() > expira) {
            return false;
        }
        return codigoGuardado.equals(codigoIngresado.trim());
    }

    public void limpiarSesion(HttpSession session) {
        session.removeAttribute(TwoFactorSessionKeys.PENDING_USER);
        session.removeAttribute(TwoFactorSessionKeys.OTP_CODE);
        session.removeAttribute(TwoFactorSessionKeys.OTP_EXPIRY);
        session.removeAttribute(TwoFactorSessionKeys.EMAIL_MASK);
        session.removeAttribute(TwoFactorSessionKeys.RESEND_COUNT);
        session.removeAttribute(TwoFactorSessionKeys.MOSTRAR_CODIGO_PANTALLA);
        session.removeAttribute(TwoFactorSessionKeys.ENVIO_EMAIL_OK);
        session.removeAttribute(TwoFactorSessionKeys.ERROR_ENVIO);
    }

    public boolean tieneVerificacionPendiente(HttpSession session) {
        return session.getAttribute(TwoFactorSessionKeys.PENDING_USER) != null;
    }

    private String generarCodigo() {
        int numero = secureRandom.nextInt(1_000_000);
        return String.format("%06d", numero);
    }

    static String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "correo registrado";
        }
        String[] partes = correo.split("@", 2);
        String local = partes[0];
        String dominio = partes[1];
        if (local.length() <= 1) {
            return "*@" + dominio;
        }
        return local.charAt(0) + "***@" + dominio;
    }
}
