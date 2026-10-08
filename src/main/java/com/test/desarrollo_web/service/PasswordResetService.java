package com.test.desarrollo_web.service;

import com.test.desarrollo_web.Models.PasswordResetToken;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.PasswordResetTokenRepository;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class PasswordResetService {

    private static final int EXPIRACION_MINUTOS = 60;
    private static final Pattern CONTRASENA_SEGURA = Pattern.compile(
            "^(?=.*[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ])(?=.*\\d)(?=.*\\.).{8,}$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailNotificationService emailNotificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UsuarioRepository usuarioRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailNotificationService emailNotificationService,
                                PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.emailNotificationService = emailNotificationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ResultadoSolicitud solicitarRestablecimiento(String correo, String enlaceBase) {
        if (correo == null || correo.isBlank()) {
            return ResultadoSolicitud.error("Ingrese su correo electrónico.");
        }

        String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        if (!correoNormalizado.contains("@") || !correoNormalizado.contains(".")) {
            return ResultadoSolicitud.error("Ingrese un correo electrónico válido.");
        }

        if (!emailNotificationService.estaConfigurado()) {
            return ResultadoSolicitud.error(
                    "El envío de correos no está configurado. Contacte al administrador del sistema.");
        }

        var usuarioOpt = usuarioRepository.findByCorreoIgnoreCase(correoNormalizado);
        if (usuarioOpt.isEmpty()) {
            return ResultadoSolicitud.exito(
                    "Si el correo está registrado, recibirá un enlace para restablecer su contraseña.");
        }

        Usuario usuario = usuarioOpt.get();
        if (usuario.getEstado() != Usuario.Estado.ACTIVO) {
            return ResultadoSolicitud.exito(
                    "Si el correo está registrado, recibirá un enlace para restablecer su contraseña.");
        }

        tokenRepository.invalidarTokensActivos(usuario.getId());

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(generarToken());
        token.setUsuario(usuario);
        token.setCreadoEn(LocalDateTime.now());
        token.setExpiraEn(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS));
        token.setUsado(false);
        tokenRepository.save(token);

        String enlace = enlaceBase + "/recuperar-contrasena/restablecer?token=" + token.getToken();
        boolean enviado = emailNotificationService.enviarEnlaceRecuperacionContrasena(
                usuario.getCorreo(),
                usuario.getNombre(),
                enlace,
                EXPIRACION_MINUTOS);

        if (!enviado) {
            throw new RuntimeException("No se pudo enviar el correo. Verifique la configuración SMTP.");
        }

        return ResultadoSolicitud.exito(
                "Si el correo está registrado, recibirá un enlace para restablecer su contraseña.");
    }

    @Transactional(readOnly = true)
    public PasswordResetToken obtenerTokenValido(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return tokenRepository.findByToken(token.trim())
                .filter(resetToken -> resetToken.isValido())
                .orElse(null);
    }

    @Transactional
    public ResultadoRestablecimiento actualizarContrasenaDirecta(Integer usuarioId,
                                                                 String password,
                                                                 String confirmacion) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResultadoRestablecimiento.error("El usuario no existe.");
        }

        String errorValidacion = validarContrasena(password, confirmacion);
        if (errorValidacion != null) {
            return ResultadoRestablecimiento.error(errorValidacion);
        }

        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepository.save(usuario);
        tokenRepository.invalidarTokensActivos(usuario.getId());

        return ResultadoRestablecimiento.exito(
                "La contraseña de " + usuario.getNombre() + " fue actualizada correctamente.");
    }

    @Transactional
    public ResultadoSolicitud enviarEnlacePorUsuario(Integer usuarioId, String enlaceBase) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResultadoSolicitud.error("El usuario no existe.");
        }
        if (usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            return ResultadoSolicitud.error("El usuario no tiene correo electrónico registrado.");
        }
        return solicitarRestablecimiento(usuario.getCorreo(), enlaceBase);
    }

    @Transactional
    public ResultadoRestablecimiento restablecerContrasena(String token, String password, String confirmacion) {
        PasswordResetToken resetToken = obtenerTokenValido(token);
        if (resetToken == null) {
            return ResultadoRestablecimiento.error(
                    "El enlace no es válido o ha expirado. Solicite uno nuevo.");
        }

        String errorValidacion = validarContrasena(password, confirmacion);
        if (errorValidacion != null) {
            return ResultadoRestablecimiento.error(errorValidacion);
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(password));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
        tokenRepository.invalidarTokensActivos(usuario.getId());

        return ResultadoRestablecimiento.exito("Su contraseña fue actualizada correctamente. Ya puede iniciar sesión.");
    }

    public static String validarContrasena(String password, String confirmacion) {
        if (password == null || password.isBlank()) {
            return "Ingrese la nueva contraseña.";
        }
        if (confirmacion == null || confirmacion.isBlank()) {
            return "Confirme la nueva contraseña.";
        }
        if (!password.equals(confirmacion)) {
            return "Las contraseñas no coinciden.";
        }
        if (!CONTRASENA_SEGURA.matcher(password).matches()) {
            return "La contraseña debe tener al menos 8 caracteres e incluir letras, números y puntos (.).";
        }
        return null;
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record ResultadoSolicitud(boolean exito, String mensaje) {
        static ResultadoSolicitud exito(String mensaje) {
            return new ResultadoSolicitud(true, mensaje);
        }

        static ResultadoSolicitud error(String mensaje) {
            return new ResultadoSolicitud(false, mensaje);
        }
    }

    public record ResultadoRestablecimiento(boolean exito, String mensaje) {
        static ResultadoRestablecimiento exito(String mensaje) {
            return new ResultadoRestablecimiento(true, mensaje);
        }

        static ResultadoRestablecimiento error(String mensaje) {
            return new ResultadoRestablecimiento(false, mensaje);
        }
    }
}
