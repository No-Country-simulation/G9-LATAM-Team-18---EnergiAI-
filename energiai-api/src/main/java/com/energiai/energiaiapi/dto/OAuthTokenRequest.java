package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Token emitido por el proveedor OAuth (Google id_token o Facebook access_token).
 * Pensado para Postman/Bruno y SPAs que ya completaron el login en el cliente.
 */
public record OAuthTokenRequest(

        @Schema(description = "id_token (Google) o access_token (Facebook)",
                example = "eyJhbGciOiJSUzI1NiIs...")
        @NotBlank(message = "token es obligatorio")
        String token
) {
}
