package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.dto.AuthResponse;
import com.energiai.energiaiapi.dto.LoginRequest;
import com.energiai.energiaiapi.dto.RegistroRequest;
import com.energiai.energiaiapi.security.JwtService;
import com.energiai.energiaiapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y login local (email + password). Devuelve un JWT que el frontend
 * envia en Authorization: Bearer para los endpoints que dependen del usuario.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro y autenticacion")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(UsuarioService usuarioService, JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @PostMapping("/registro")
    @Operation(summary = "Registra un usuario nuevo (email + password)")
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = usuarioService.registrar(request);
        String token = jwtService.generarToken(usuario.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre()));
    }

    @PostMapping("/login")
    @Operation(summary = "Inicia sesion y devuelve un token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.password());
        String token = jwtService.generarToken(usuario.getEmail());
        return ResponseEntity.ok(AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre()));
    }
}
