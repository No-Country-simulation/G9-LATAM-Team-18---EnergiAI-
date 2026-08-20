package com.energiai.energiaiapi.repository;

import com.energiai.energiaiapi.domain.Analisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AnalisisRepository extends JpaRepository<Analisis, Long> {

    List<Analisis> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);

    Optional<Analisis> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByUsuarioIdAndFacturaMesAndFacturaAnio(Long usuarioId, String mes, Integer anio);

    Optional<Analisis> findByUsuarioIdAndFacturaMesAndFacturaAnio(Long usuarioId, String mes, Integer anio);
}
