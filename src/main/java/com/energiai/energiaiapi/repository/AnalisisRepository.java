package com.energiai.energiaiapi.repository;

import com.energiai.energiaiapi.domain.Analisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalisisRepository extends JpaRepository<Analisis, Long> {

    List<Analisis> findByUsuarioIdOrderByCreadoEnDesc(Long usuarioId);
}
