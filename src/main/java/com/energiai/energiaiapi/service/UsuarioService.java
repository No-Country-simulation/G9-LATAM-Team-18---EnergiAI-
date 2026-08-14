package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.AuthProvider;
import com.energiai.energiaiapi.dto.RegistroRequest;
import com.energiai.energiaiapi.exception.ReglaNegocioException;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Gestion de usuarios (registro local, login, OAuth) y puente con Spring Security.
 */
@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + request.email());
        }
        Usuario usuario = new Usuario(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nombre(),
                AuthProvider.LOCAL);
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String email, String password) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ReglaNegocioException("Credenciales invalidas"));
        if (usuario.getAuthProvider() != AuthProvider.LOCAL) {
            throw new ReglaNegocioException(
                    "Esta cuenta usa autenticacion " + usuario.getAuthProvider()
                            + ". Inicia sesion con ese proveedor.");
        }
        if (usuario.getPasswordHash() == null
                || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new ReglaNegocioException("Credenciales invalidas");
        }
        return usuario;
    }

    /**
     * Crea o actualiza un usuario proveniente de OAuth2 (Google/Facebook).
     * Si el email ya existe con otro proveedor, rechaza el enlace.
     */
    @Transactional
    public Usuario registrarOActualizarOAuth(String email,
                                             String nombre,
                                             AuthProvider provider,
                                             String providerId) {
        Optional<Usuario> existente = usuarioRepository.findByEmail(email);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.getAuthProvider() != provider) {
                throw new ReglaNegocioException(
                        "Ya existe una cuenta con el email " + email
                                + " registrada via " + usuario.getAuthProvider());
            }
            if (nombre != null && !nombre.isBlank()) {
                usuario.setNombre(nombre);
            }
            if (providerId != null) {
                usuario.setProviderId(providerId);
            }
            return usuarioRepository.save(usuario);
        }

        Usuario usuario = new Usuario(email, null, nombre, provider);
        usuario.setProviderId(providerId);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        String password = usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "{noop}oauth2";
        return new User(usuario.getEmail(), password, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
