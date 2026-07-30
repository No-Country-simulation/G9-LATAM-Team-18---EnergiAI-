package com.energiai.energiaiapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Datos de entrada de un analisis (la "factura" que carga el usuario).
 * 5 campos obligatorios + 7 opcionales. Se persisten en crudo/legible;
 * la codificacion numerica que necesita el modelo es responsabilidad del
 * pipeline de inferencia (frontend ONNX o el fallback en backend).
 */
@Entity
@Table(name = "factura")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------- Obligatorios ----------
    @Column(name = "consumo_kwh", nullable = false)
    private Integer consumoKwh;

    @Column(name = "uso_horario_pico", nullable = false)
    private Boolean usoHorarioPico;

    @Column(name = "cantidad_equipos", nullable = false)
    private Integer cantidadEquipos;

    @Column(name = "tipo_inmueble", nullable = false)
    private String tipoInmueble;

    @Column(name = "horas_alto_consumo", nullable = false)
    private Double horasAltoConsumo;

    // ---------- Opcionales ----------
    @Column(name = "area_inmueble")
    private Double areaInmueble;

    @Column(name = "numero_personas")
    private Integer numeroPersonas;

    @Column(name = "tiene_aire_acondicionado")
    private Boolean tieneAireAcondicionado;

    @Column(name = "tiene_calentador_electrico")
    private Boolean tieneCalentadorElectrico;

    @Column(name = "tiene_iluminacion_led")
    private Boolean tieneIluminacionLed;

    @Column(name = "antiguedad_electrodomesticos")
    private String antiguedadElectrodomesticos;

    @Column(name = "tarifa_electrica")
    private Double tarifaElectrica;

    public Factura() {
    }

    public Long getId() {
        return id;
    }

    public Integer getConsumoKwh() {
        return consumoKwh;
    }

    public void setConsumoKwh(Integer consumoKwh) {
        this.consumoKwh = consumoKwh;
    }

    public Boolean getUsoHorarioPico() {
        return usoHorarioPico;
    }

    public void setUsoHorarioPico(Boolean usoHorarioPico) {
        this.usoHorarioPico = usoHorarioPico;
    }

    public Integer getCantidadEquipos() {
        return cantidadEquipos;
    }

    public void setCantidadEquipos(Integer cantidadEquipos) {
        this.cantidadEquipos = cantidadEquipos;
    }

    public String getTipoInmueble() {
        return tipoInmueble;
    }

    public void setTipoInmueble(String tipoInmueble) {
        this.tipoInmueble = tipoInmueble;
    }

    public Double getHorasAltoConsumo() {
        return horasAltoConsumo;
    }

    public void setHorasAltoConsumo(Double horasAltoConsumo) {
        this.horasAltoConsumo = horasAltoConsumo;
    }

    public Double getAreaInmueble() {
        return areaInmueble;
    }

    public void setAreaInmueble(Double areaInmueble) {
        this.areaInmueble = areaInmueble;
    }

    public Integer getNumeroPersonas() {
        return numeroPersonas;
    }

    public void setNumeroPersonas(Integer numeroPersonas) {
        this.numeroPersonas = numeroPersonas;
    }

    public Boolean getTieneAireAcondicionado() {
        return tieneAireAcondicionado;
    }

    public void setTieneAireAcondicionado(Boolean tieneAireAcondicionado) {
        this.tieneAireAcondicionado = tieneAireAcondicionado;
    }

    public Boolean getTieneCalentadorElectrico() {
        return tieneCalentadorElectrico;
    }

    public void setTieneCalentadorElectrico(Boolean tieneCalentadorElectrico) {
        this.tieneCalentadorElectrico = tieneCalentadorElectrico;
    }

    public Boolean getTieneIluminacionLed() {
        return tieneIluminacionLed;
    }

    public void setTieneIluminacionLed(Boolean tieneIluminacionLed) {
        this.tieneIluminacionLed = tieneIluminacionLed;
    }

    public String getAntiguedadElectrodomesticos() {
        return antiguedadElectrodomesticos;
    }

    public void setAntiguedadElectrodomesticos(String antiguedadElectrodomesticos) {
        this.antiguedadElectrodomesticos = antiguedadElectrodomesticos;
    }

    public Double getTarifaElectrica() {
        return tarifaElectrica;
    }

    public void setTarifaElectrica(Double tarifaElectrica) {
        this.tarifaElectrica = tarifaElectrica;
    }
}
