package com.test.desarrollo_web;

import com.test.desarrollo_web.Models.Perfil;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.PerfilRepository;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    private static final String ADMIN_CORREO = "jesmarconchatinoco@gmail.com";
    private static final String ADMIN_TELEFONO = "957779526";

    @Bean
    @Order(10)
    public CommandLineRunner initData(PerfilRepository perfilRepository,
                                      UsuarioRepository usuarioRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${app.admin.initial-password:}") String adminPassword) {
        return args -> {
            Perfil adminPerfil = perfilRepository.findAll().stream()
                    .filter(p -> p.getNombre() != null
                            && (p.getNombre().equalsIgnoreCase("Administrador")
                            || p.getNombre().equalsIgnoreCase("ADMIN")))
                    .findFirst()
                    .orElseGet(() -> {
                        Perfil perfil = new Perfil();
                        perfil.setNombre("Administrador");
                        perfil.setDescripcion("Acceso total al sistema");
                        return perfilRepository.save(perfil);
                    });

            usuarioRepository.findByUsuario("admin").ifPresentOrElse(admin -> {
                boolean cambio = false;
                if (!ADMIN_CORREO.equalsIgnoreCase(admin.getCorreo())) {
                    admin.setCorreo(ADMIN_CORREO);
                    cambio = true;
                }
                if (!ADMIN_TELEFONO.equals(admin.getTelefono())) {
                    admin.setTelefono(ADMIN_TELEFONO);
                    cambio = true;
                }
                if (cambio) {
                    usuarioRepository.save(admin);
                }
            }, () -> {
                if (adminPassword == null || adminPassword.isBlank()) {
                    System.err.println("No se creó el usuario admin: defina app.admin.initial-password en application-local.properties");
                    return;
                }
                Usuario admin = new Usuario();
                admin.setNombre("Administrador Jesmar");
                admin.setUsuario("admin");
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setCorreo(ADMIN_CORREO);
                admin.setTelefono(ADMIN_TELEFONO);
                admin.setPerfil(adminPerfil);
                usuarioRepository.save(admin);
            });
        };
    }
}
