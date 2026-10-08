package com.test.desarrollo_web.security;

import com.test.desarrollo_web.Models.Modulo;
import com.test.desarrollo_web.Models.Usuario;
import com.test.desarrollo_web.Repository.UsuarioRepository;
import com.test.desarrollo_web.service.PermissionService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Carga el Principal (usuario autenticado) y sus Granted Authorities (permisos).
 * - Principal: nombre de usuario en sesión ({@link org.springframework.security.core.Authentication#getName()}).
 * - Role: grupo de permisos del perfil (ej. ROLE_ADMINISTRADOR).
 * - Granted Authority: permiso individual (MOD_DASHBOARD, MOD_VENTAS, etc.).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> optionalUsuario = usuarioRepository.findByUsuario(username);
        Usuario usuario = optionalUsuario.orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        Set<GrantedAuthority> authorities = new HashSet<>();
        if (usuario.getPerfil() != null && usuario.getPerfil().getNombre() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().getNombre().toUpperCase()));
            if (PermissionService.esPerfilAdministrador(usuario.getPerfil().getNombre())) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            } else if (usuario.getPerfil().getModulos() != null) {
                for (Modulo modulo : usuario.getPerfil().getModulos()) {
                    if (modulo.getCodigo() != null) {
                        authorities.add(new SimpleGrantedAuthority("MOD_" + modulo.getCodigo()));
                    }
                }
            }
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return User.builder()
            .username(usuario.getUsuario())
            .password(usuario.getPassword())
            .authorities(authorities)
            .disabled(usuario.getEstado() != Usuario.Estado.ACTIVO)
            .build();
    }
}
