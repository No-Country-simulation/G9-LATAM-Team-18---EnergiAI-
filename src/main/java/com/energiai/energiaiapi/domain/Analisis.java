package com.energiai.energiaiapi.domain;

import com.energiai.energiaiapi.domain.enums.CategoriaEficiencia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Resultado inmutable de un analisis energetico. Se persiste solo cuando el
 * usuario esta autenticado y pidio guardar (guardar=true). Congela la factura,
 * la categoria, el costo estimado y las recomendaciones del momento.
 */
@Entity
@Table(name = "analisis")
public class Analisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null para consultas anonimas que igualmente se decidan persistir en el futuro. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaEficiencia categoria;

    /** Probabilidad de la clase ganadora (0..1). */
    private double probabilidad;

    @Column(name = "costo_estimado_mensual", nullable = false)
    private double costoEstimadoMensual;

    @Column(name = "indice_eficiencia")
    private Double indiceEficiencia;

    @Column(name = "modelo_version")
    private String modeloVersion;

    /** FRONTEND_ONNX | BACKEND_FALLBACK - trazabilidad de quien clasifico. */
    @Column(name = "fuente_clasificacion")
    private String fuenteClasificacion;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "analisis_recomendacion", joinColumns = @JoinColumn(name = "analisis_id"))
    @Column(name = "texto", length = 500)
    private List<String> recomendaciones = new ArrayList<>();

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Analisis() {
    }

    @jakarta.persistence.PrePersist
    void prePersist() {
        if (creadoEn == null) {
            creadoEn = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    public CategoriaEficiencia getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaEficiencia categoria) {
        this.categoria = categoria;
    }

    public double getProbabilidad() {
        return probabilidad;
    }

    public void setProbabilidad(double probabilidad) {
        this.probabilidad = probabilidad;
    }

    public double getCostoEstimadoMensual() {
        return costoEstimadoMensual;
    }

    public void setCostoEstimadoMensual(double costoEstimadoMensual) {
        this.costoEstimadoMensual = costoEstimadoMensual;
    }

    public Double getIndiceEficiencia() {
        return indiceEficiencia;
    }

    public void setIndiceEficiencia(Double indiceEficiencia) {
        this.indiceEficiencia = indiceEficiencia;
    }

    public String getModeloVersion() {
        return modeloVersion;
    }

    public void setModeloVersion(String modeloVersion) {
        this.modeloVersion = modeloVersion;
    }

    public String getFuenteClasificacion() {
        return fuenteClasificacion;
    }

    public void setFuenteClasificacion(String fuenteClasificacion) {
        this.fuenteClasificacion = fuenteClasificacion;
    }

    public List<String> getRecomendaciones() {
        return recomendaciones;
    }

    public void setRecomendaciones(List<String> recomendaciones) {
        this.recomendaciones = recomendaciones;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
