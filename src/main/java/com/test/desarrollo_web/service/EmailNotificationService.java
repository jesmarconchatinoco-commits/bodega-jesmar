package com.test.desarrollo_web.service;

import com.test.desarrollo_web.config.TwoFactorProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.file.Path;
import java.time.Year;
import java.util.Optional;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final Optional<JavaMailSender> mailSender;
    private final SpringTemplateEngine templateEngine;
    private final LogoService logoService;
    private final TwoFactorProperties twoFactorProperties;

    @Value("${spring.mail.username:}")
    private String remitente;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public EmailNotificationService(@Autowired(required = false) JavaMailSender mailSender,
                                    SpringTemplateEngine templateEngine,
                                    LogoService logoService,
                                    TwoFactorProperties twoFactorProperties) {
        this.mailSender = Optional.ofNullable(mailSender);
        this.templateEngine = templateEngine;
        this.logoService = logoService;
        this.twoFactorProperties = twoFactorProperties;
    }

    public boolean estaConfigurado() {
        return mailSender.isPresent()
                && remitente != null && !remitente.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    public boolean enviarCodigo2fa(String destinatario, String nombreUsuario, String codigo) {
        Context context = contextoBase();
        context.setVariable("tituloEmail", "Verificación en dos pasos");
        context.setVariable("subtitulo", "Código de acceso seguro");
        context.setVariable("nombreUsuario", nombreSeguro(nombreUsuario));
        context.setVariable("codigo", codigoLimpio(codigo));
        context.setVariable("minutosExpiracion", twoFactorProperties.getCodigoExpiracionMinutos());

        String html = templateEngine.process("email/codigo-2fa", context);
        String textoPlano = "Hola " + nombreSeguro(nombreUsuario) + ",\n\n"
                + "Su código de verificación en dos pasos es: " + codigo + "\n\n"
                + "El código expira en " + twoFactorProperties.getCodigoExpiracionMinutos()
                + " minutos. Si no solicitó este acceso, ignore este mensaje.\n\n"
                + "Bodega Jesmar";

        return enviarCorreoHtml(destinatario, "Código de verificación - Bodega Jesmar", textoPlano, html);
    }

    public boolean enviarEnlaceRecuperacionContrasena(String destinatario,
                                                      String nombreUsuario,
                                                      String enlace,
                                                      int minutosValidez) {
        Context context = contextoBase();
        context.setVariable("tituloEmail", "Restablecer contraseña");
        context.setVariable("subtitulo", "Recuperación de acceso a su cuenta");
        context.setVariable("nombreUsuario", nombreSeguro(nombreUsuario));
        context.setVariable("enlace", enlace);
        context.setVariable("minutosValidez", minutosValidez);

        String html = templateEngine.process("email/recuperar-contrasena", context);
        String textoPlano = "Hola " + nombreSeguro(nombreUsuario) + ",\n\n"
                + "Recibimos una solicitud para restablecer su contraseña en Bodega Jesmar.\n\n"
                + "Use el siguiente enlace (válido por " + minutosValidez + " minutos):\n"
                + enlace + "\n\n"
                + "Si no solicitó este cambio, ignore este mensaje. Su contraseña actual no cambiará.\n\n"
                + "Bodega Jesmar";

        return enviarCorreoHtml(destinatario, "Restablecer contraseña - Bodega Jesmar", textoPlano, html);
    }

    private Context contextoBase() {
        Context context = new Context();
        context.setVariable("anio", Year.now().getValue());
        Optional<Path> logo = logoService.getLogoFilePath();
        context.setVariable("tieneLogo", logo.isPresent());
        return context;
    }

    private boolean enviarCorreoHtml(String destinatario, String asunto, String textoPlano, String html) {
        if (!estaConfigurado()) {
            log.warn("[EMAIL] SMTP no configurado. Agregue spring.mail.password en application.properties");
            return false;
        }

        try {
            MimeMessage mimeMessage = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(textoPlano, html);

            logoService.getLogoFilePath().ifPresent(path -> {
                try {
                    helper.addInline("logo", path.toFile());
                } catch (Exception e) {
                    log.warn("No se pudo adjuntar el logo al correo: {}", e.getMessage());
                }
            });

            mailSender.get().send(mimeMessage);
            log.info("Correo HTML enviado a {}", destinatario);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
            return false;
        }
    }

    private static String nombreSeguro(String nombre) {
        return nombre != null && !nombre.isBlank() ? nombre.trim() : "Usuario";
    }

    private static String codigoLimpio(String codigo) {
        if (codigo == null) {
            return "------";
        }
        String limpio = codigo.replaceAll("\\D", "");
        return limpio.isBlank() ? codigo.trim() : limpio;
    }
}
