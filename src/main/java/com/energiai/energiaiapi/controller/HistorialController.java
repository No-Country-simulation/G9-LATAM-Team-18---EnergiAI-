package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.ErrorResponse;
import com.energiai.energiaiapi.dto.HistorialItemResponse;
import com.energiai.energiaiapi.security.SecurityUtils;
import com.energiai.energiaiapi.service.HistorialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Historial del usuario autenticado. Protegido por SecurityConfig (JWT obligatorio).
 * El usuario se deriva del token: no se recibe usuarioId como parametro.
 */
@RestController
@RequestMapping("/api/historial")
@Tag(name = "Historial", description = "Historial de analisis del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class HistorialController {

    private final HistorialService historialService;

    public HistorialController(HistorialService historialService) {
        this.historialService = historialService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lista historial",
            description = "Devuelve los analisis guardados del usuario del JWT (mas recientes primero). "
                    + "Cada item incluye el bloque `costos` congelado al momento del analisis "
                    + "(null en registros anteriores al desglose estacional).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = HistorialItemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Sin JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<HistorialItemResponse> miHistorial(Authentication authentication) {
        return historialService.historialDe(emailDe(authentication));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Detalle de analisis",
            description = "Detalle de un analisis propio. 404 si no existe o no pertenece al usuario.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = HistorialItemResponse.class))),
            @ApiResponse(responseCode = "401", description = "Sin JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public HistorialItemResponse detalle(@PathVariable Long id, Authentication authentication) {
        return historialService.detalleDe(emailDe(authentication), id);
    }

    private static String emailDe(Authentication authentication) {
        return SecurityUtils.emailDe(authentication)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Debes iniciar sesion para ver tu historial"));
    }
}
