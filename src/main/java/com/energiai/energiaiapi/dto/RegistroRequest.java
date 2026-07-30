package com.energiai.energiaiapi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(

        @NotBlank @Email(message = "El email no es valido")
        String email,

        @NotBlank
        @Size(min = 8, message = "La password debe tener al menos 8 caracteres")
        String password,

        String nombre
) {
}
