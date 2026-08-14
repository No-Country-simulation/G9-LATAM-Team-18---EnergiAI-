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

    // ---------- Costos con estacionalidad (solo modo historial; null en registros previos) ----------
    @Column(name = "estacion_calculo", length = 20)
    private String estacionCalculo;

    @Column(name = "costo_bruto_mensual")
    private Double costoBrutoMensual;

    @Column(name = "pct_estacional")
    private Double pctEstacional;

    @Column(name = "pct_ajuste_total")
    private Double pctAjusteTotal;

    @Column(name = "pct_ahorro_potencial")
    private Double pctAhorroPotencial;

    @Column(name = "costo_ajustado_mensual")
    private Double costoAjustadoMensual;

    @Column(name = "ahorro_potencial_mensual")
    private Double ahorroPotencialMensual;

    @Column(name = "ahorro_potencial_anual")
    private Double ahorroPotencialAnual;

    @Column(name = "costo_anual_estimado")
    private Double costoAnualEstimado;

    @Column(name = "costo_anual_estacionalizado")
    private Double costoAnualEstacionalizado;

    @Column(name = "parametros_costos_version", length = 50)
    private String parametrosCostosVersion;

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

    public String getEstacionCalculo() {
        return estacionCalculo;
    }

    public void setEstacionCalculo(String estacionCalculo) {
        this.estacionCalculo = estacionCalculo;
    }

    public Double getCostoBrutoMensual() {
        return costoBrutoMensual;
    }

    public void setCostoBrutoMensual(Double costoBrutoMensual) {
        this.costoBrutoMensual = costoBrutoMensual;
    }

    public Double getPctEstacional() {
        return pctEstacional;
    }

    public void setPctEstacional(Double pctEstacional) {
        this.pctEstacional = pctEstacional;
    }

    public Double getPctAjusteTotal() {
        return pctAjusteTotal;
    }

    public void setPctAjusteTotal(Double pctAjusteTotal) {
        this.pctAjusteTotal = pctAjusteTotal;
    }

    public Double getPctAhorroPotencial() {
        return pctAhorroPotencial;
    }

    public void setPctAhorroPotencial(Double pctAhorroPotencial) {
        this.pctAhorroPotencial = pctAhorroPotencial;
    }

    public Double getCostoAjustadoMensual() {
        return costoAjustadoMensual;
    }

    public void setCostoAjustadoMensual(Double costoAjustadoMensual) {
        this.costoAjustadoMensual = costoAjustadoMensual;
    }

    public Double getAhorroPotencialMensual() {
        return ahorroPotencialMensual;
    }

    public void setAhorroPotencialMensual(Double ahorroPotencialMensual) {
        this.ahorroPotencialMensual = ahorroPotencialMensual;
    }

    public Double getAhorroPotencialAnual() {
        return ahorroPotencialAnual;
    }

    public void setAhorroPotencialAnual(Double ahorroPotencialAnual) {
        this.ahorroPotencialAnual = ahorroPotencialAnual;
    }

    public Double getCostoAnualEstimado() {
        return costoAnualEstimado;
    }

    public void setCostoAnualEstimado(Double costoAnualEstimado) {
        this.costoAnualEstimado = costoAnualEstimado;
    }

    public Double getCostoAnualEstacionalizado() {
        return costoAnualEstacionalizado;
    }

    public void setCostoAnualEstacionalizado(Double costoAnualEstacionalizado) {
        this.costoAnualEstacionalizado = costoAnualEstacionalizado;
    }

    public String getParametrosCostosVersion() {
        return parametrosCostosVersion;
    }

    public void setParametrosCostosVersion(String parametrosCostosVersion) {
        this.parametrosCostosVersion = parametrosCostosVersion;
    }
}
