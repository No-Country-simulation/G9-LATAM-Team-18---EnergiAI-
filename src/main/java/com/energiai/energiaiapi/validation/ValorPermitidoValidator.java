package com.energiai.energiaiapi.validation;

import com.energiai.energiaiapi.util.TextoNormalizado;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ValorPermitidoValidator implements ConstraintValidator<ValorPermitido, String> {

    private Set<String> permitidosNormalizados;

    @Override
    public void initialize(ValorPermitido annotation) {
        permitidosNormalizados = Arrays.stream(annotation.valores())
                .map(TextoNormalizado::de)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/blank: lo cubre @NotBlank si es obligatorio; opcionales pueden omitirse.
        if (value == null || value.isBlank()) {
            return true;
        }
        return permitidosNormalizados.contains(TextoNormalizado.de(value));
    }
}
