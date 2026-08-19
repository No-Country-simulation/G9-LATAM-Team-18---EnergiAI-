package com.energiai.energiaiapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegistroRequest")
public record RegistroRequest(

        @Schema(example = "lucia@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email(message = "El email no es valido")
        String email,

        @Schema(example = "secreto123", minLength = 8, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, message = "La password debe tener al menos 8 caracteres")
        String password,

        @Schema(example = "Lucia", nullable = true)
        String nombre
) {
}
