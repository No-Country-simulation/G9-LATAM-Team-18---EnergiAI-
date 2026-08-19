package com.energiai.energiaiapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValorPermitidoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValorPermitido {

    String message() default "Valor no permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /** Valores canonicos aceptados (comparacion sin acentos y case-insensitive). */
    String[] valores();
}
