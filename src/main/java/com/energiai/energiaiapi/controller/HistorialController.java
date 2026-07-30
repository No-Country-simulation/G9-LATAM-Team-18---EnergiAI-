package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.HistorialItemResponse;
import com.energiai.energiaiapi.service.HistorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Historial del usuario autenticado. El usuario se deriva del JWT: no se recibe
 * el usuarioId como parametro (evita que un cliente lea historiales ajenos).
 */
@RestController
@RequestMapping("/api/historial")
@Tag(name = "Historial", description = "Historial de analisis del usuario autenticado")
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @GetMapping
    @Operation(summary = "Lista el historial del usuario autenticado (requiere JWT)")
    public List<HistorialItemResponse> miHistorial(Authentication authentication) {
        String email = emailDe(authentication);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesion para ver tu historial");
        }
        return historialService.historialDe(email);
    }

    private String emailDe(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
}
