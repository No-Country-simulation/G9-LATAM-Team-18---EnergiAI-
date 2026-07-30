package com.energiai.energiaiapi.controller;

import com.energiai.energiaiapi.dto.AnalisisRequest;
import com.energiai.energiaiapi.dto.AnalisisResponse;
import com.energiai.energiaiapi.service.AnalisisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analisis")
@Tag(name = "Analisis", description = "Analisis de eficiencia energetica")
public class AnalisisController {

    private final AnalisisService analisisService;

    public AnalisisController(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @PostMapping
    @Operation(summary = "Analiza una factura",
            description = "Recibe la factura (5 obligatorios + 7 opcionales) y, opcionalmente, la clasificacion "
                    + "calculada por el frontend con ONNX. Devuelve categoria, costo, indice y recomendaciones. "
                    + "Persiste en el historial solo si guardar=true y hay sesion iniciada.")
    public ResponseEntity<AnalisisResponse> analizar(@Valid @RequestBody AnalisisRequest request,
                                                     Authentication authentication) {
        String email = emailDe(authentication);
        return ResponseEntity.ok(analisisService.analizar(request, email));
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
