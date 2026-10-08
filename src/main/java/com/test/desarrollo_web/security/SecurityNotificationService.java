package com.test.desarrollo_web.security;

import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envía el código del segundo factor de autenticación (Spring Security 2FA)
 * al correo electrónico del usuario autenticado.
 */
@Service
public class SecurityNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SecurityNotificationService.class);

    private final EmailNotificationService emailNotificationService;

    public SecurityNotificationService(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    public boolean enviarCodigoVerificacion(Usuario usuario, String codigo) {
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            log.error("Usuario {} sin correo registrado para 2FA", usuario.getUsuario());
            return false;
        }

        boolean emailEnviado = emailNotificationService.enviarCodigo2fa(
                usuario.getCorreo(), usuario.getNombre(), codigo);

        if (!emailEnviado) {
            log.error("No se pudo enviar código 2FA para {}. Configure spring.mail.password en application.properties",
                    usuario.getUsuario());
        }
        return emailEnviado;
    }
}
