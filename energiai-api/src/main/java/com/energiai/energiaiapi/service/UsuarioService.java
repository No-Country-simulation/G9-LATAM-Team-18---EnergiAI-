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
import java.util.Locale;
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
        String email = normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + email);
        }
        Usuario usuario = new Usuario(
                email,
                passwordEncoder.encode(request.password()),
                request.nombre(),
                AuthProvider.LOCAL);
        return usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String email, String password) {
        Usuario usuario = buscarPorEmail(email)
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
     * Si el email ya existe como LOCAL, Google se vincula (misma cuenta, JWT por email).
     * Otro proveedor distinto sigue rechazado.
     */
    @Transactional
    public Usuario registrarOActualizarOAuth(String email,
                                             String nombre,
                                             AuthProvider provider,
                                             String providerId) {
        String mail = normalizarEmail(email);
        Optional<Usuario> existente = buscarPorEmail(mail);
        if (existente.isPresent()) {
            Usuario usuario = existente.get();
            if (usuario.getAuthProvider() != provider) {
                if (usuario.getAuthProvider() == AuthProvider.LOCAL && provider == AuthProvider.GOOGLE) {
                    if (providerId != null) {
                        usuario.setProviderId(providerId);
                    }
                    if ((usuario.getNombre() == null || usuario.getNombre().isBlank())
                            && nombre != null && !nombre.isBlank()) {
                        usuario.setNombre(nombre);
                    }
                    return usuarioRepository.save(usuario);
                }
                throw new ReglaNegocioException(
                        "Ya existe una cuenta con el email " + mail
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

        Usuario usuario = new Usuario(mail, null, nombre, provider);
        usuario.setProviderId(providerId);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = buscarPorEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

        String password = usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "{noop}oauth2";
        return new User(usuario.getEmail(), password, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorEmail(String email) {
        String mail = normalizarEmail(email);
        if (mail == null) {
            return Optional.empty();
        }
        return usuarioRepository.findByEmailIgnoreCase(mail)
                .or(() -> usuarioRepository.findByEmail(mail));
    }

    static String normalizarEmail(String email) {
        if (email == null) {
            return null;
        }
        String t = email.trim().toLowerCase(Locale.ROOT);
        return t.isEmpty() ? null : t;
    }
}
