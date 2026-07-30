package com.energiai.energiaiapi.service;

import com.energiai.energiaiapi.domain.Analisis;
import com.energiai.energiaiapi.domain.Usuario;
import com.energiai.energiaiapi.dto.HistorialItemResponse;
import com.energiai.energiaiapi.exception.RecursoNoEncontradoException;
import com.energiai.energiaiapi.repository.AnalisisRepository;
import com.energiai.energiaiapi.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Historial de analisis del usuario autenticado. El usuario se resuelve a partir
 * del email del token JWT (nunca se recibe como parametro editable por el cliente).
 */
@Service
public class HistorialService {

    private final AnalisisRepository analisisRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialService(AnalisisRepository analisisRepository, UsuarioRepository usuarioRepository) {
        this.analisisRepository = analisisRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<HistorialItemResponse> historialDe(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + emailUsuario));

        return analisisRepository.findByUsuarioIdOrderByCreadoEnDesc(usuario.getId())
                .stream()
                .map(this::aItem)
                .toList();
    }

    private HistorialItemResponse aItem(Analisis a) {
        return new HistorialItemResponse(
                a.getId(),
                a.getCreadoEn(),
                a.getCategoria(),
                a.getProbabilidad(),
                a.getCostoEstimadoMensual(),
                a.getIndiceEficiencia(),
                a.getFactura() != null ? a.getFactura().getConsumoKwh() : null,
                a.getFactura() != null ? a.getFactura().getTipoInmueble() : null,
                List.copyOf(a.getRecomendaciones())
        );
    }
}
