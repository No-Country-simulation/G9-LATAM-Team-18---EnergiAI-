package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.domain.enums.AuthProvider;
import com.energiai.energiaiapi.dto.AuthResponse;
import com.energiai.energiaiapi.dto.ErrorResponse;
import com.energiai.energiaiapi.dto.LoginRequest;
import com.energiai.energiaiapi.dto.OAuthTokenRequest;
import com.energiai.energiaiapi.dto.RegistroRequest;
import com.energiai.energiaiapi.security.JwtService;
import com.energiai.energiaiapi.security.oauth.FacebookAccessTokenVerifier;
import com.energiai.energiaiapi.security.oauth.GoogleIdTokenVerifier;
import com.energiai.energiaiapi.security.oauth.OAuthProfile;
import com.energiai.energiaiapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro/login local y canje de tokens OAuth (Google/Facebook) por JWT propio.
 * El flujo browser OAuth2 vive en {@code /oauth2/authorization/{provider}} cuando
 * {@code app.oauth2.enabled=true}.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Registro, login local y OAuth → JWT")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    private final FacebookAccessTokenVerifier facebookAccessTokenVerifier;

    public AuthController(UsuarioService usuarioService,
                          JwtService jwtService,
                          GoogleIdTokenVerifier googleIdTokenVerifier,
                          FacebookAccessTokenVerifier facebookAccessTokenVerifier) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.googleIdTokenVerifier = googleIdTokenVerifier;
        this.facebookAccessTokenVerifier = facebookAccessTokenVerifier;
    }

    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registro local",
            description = "Crea usuario (email + password ≥ 8) y devuelve JWT. Usar el token en Authorize → bearerAuth.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario creado",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validacion",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email ya registrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenPara(usuario));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Login local", description = "Autentica email/password y devuelve JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciales invalidas",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.email(), request.password());
        return ResponseEntity.ok(tokenPara(usuario));
    }

    @PostMapping(value = "/oauth/google", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OAuth Google (API)",
            description = "Canjea un Google `id_token` por JWT de EnergiAI (Postman/Bruno/SPA).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "id_token invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> oauthGoogle(@Valid @RequestBody OAuthTokenRequest request) {
        OAuthProfile profile = googleIdTokenVerifier.verificar(request.token());
        Usuario usuario = usuarioService.registrarOActualizarOAuth(
                profile.email(), profile.nombre(), AuthProvider.GOOGLE, profile.providerId());
        return ResponseEntity.ok(tokenPara(usuario));
    }

    @PostMapping(value = "/oauth/facebook", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "OAuth Facebook (API)",
            description = "Canjea un Facebook `access_token` por JWT de EnergiAI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "access_token invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AuthResponse> oauthFacebook(@Valid @RequestBody OAuthTokenRequest request) {
        OAuthProfile profile = facebookAccessTokenVerifier.verificar(request.token());
        Usuario usuario = usuarioService.registrarOActualizarOAuth(
                profile.email(), profile.nombre(), AuthProvider.FACEBOOK, profile.providerId());
        return ResponseEntity.ok(tokenPara(usuario));
    }

    private AuthResponse tokenPara(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getEmail());
        return AuthResponse.bearer(token, usuario.getEmail(), usuario.getNombre());
    }
}
