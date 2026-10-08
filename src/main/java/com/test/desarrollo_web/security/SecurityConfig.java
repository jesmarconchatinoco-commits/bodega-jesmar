package com.test.desarrollo_web.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import jakarta.servlet.http.HttpSession;

@Configuration
public class SecurityConfig {

    private final ModuleAccessFilter moduleAccessFilter;
    private final TwoFactorEnforcementFilter twoFactorEnforcementFilter;
    private final TwoFactorAuthenticationSuccessHandler twoFactorSuccessHandler;
    private final com.test.desarrollo_web.service.TwoFactorAuthService twoFactorAuthService;

    public SecurityConfig(ModuleAccessFilter moduleAccessFilter,
                          TwoFactorEnforcementFilter twoFactorEnforcementFilter,
                          TwoFactorAuthenticationSuccessHandler twoFactorSuccessHandler,
                          com.test.desarrollo_web.service.TwoFactorAuthService twoFactorAuthService) {
        this.moduleAccessFilter = moduleAccessFilter;
        this.twoFactorEnforcementFilter = twoFactorEnforcementFilter;
        this.twoFactorSuccessHandler = twoFactorSuccessHandler;
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/uploads/**", "/imagen/**", "/archivos/**",
                                "/login", "/login/verificar", "/login/reenviar-codigo",
                                "/recuperar-contrasena", "/recuperar-contrasena/**",
                                "/catalogo/**", "/error", "/logout",
                                "/version", "/salud")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterAfter(twoFactorEnforcementFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(moduleAccessFilter, TwoFactorEnforcementFilter.class)
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("usuario")
                        .passwordParameter("password")
                        .successHandler(twoFactorSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            HttpSession session = request.getSession(false);
                            if (session != null) {
                                twoFactorAuthService.limpiarSesion(session);
                            }
                        })
                        .permitAll())
                .csrf(csrf -> csrf.disable())
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
