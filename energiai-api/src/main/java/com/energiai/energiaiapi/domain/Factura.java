package com.energiai.energiaiapi.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Datos de entrada de un analisis (la "factura" que carga el usuario).
 * 5 campos obligatorios + 8 opcionales. Se persisten en crudo/legible;
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
    @Column(name = "consumo_mensual", nullable = false)
    private Double consumoMensual;

    @Column(name = "uso_horario_pico", nullable = false)
    private Boolean usoHorarioPico;

    @Column(name = "cantidad_equipos", nullable = false)
    private Integer cantidadEquipos;

    @Column(name = "tipo_inmueble", nullable = false)
    private String tipoInmueble;

    @Column(name = "horas_promedio_uso", nullable = false)
    private Double horasPromedioUso;

    // ---------- Opcionales ----------
    @Column(name = "estacion_anio")
    private String estacionAnio;

    @Column(name = "mes")
    private String mes;

    @Column(name = "numero_personas")
    private Integer numeroPersonas;

    @Column(name = "tiene_aire_acondicionado")
    private Boolean tieneAireAcondicionado;

    @Column(name = "tiene_calentador")
    private Boolean tieneCalentador;

    @Column(name = "tiene_iluminacion_led")
    private Boolean tieneIluminacionLed;

    @Column(name = "antiguedad_electrodomesticos")
    private String antiguedadElectrodomesticos;

    @Column(name = "tarifa_electrica")
    private Double tarifaElectrica;

    /** Features sinteticas del pipeline xgboost (nullable en registros previos). */
    @Column(name = "intensidad_por_equipo")
    private Double intensidadPorEquipo;

    @Column(name = "horas_pico_interaccion")
    private Double horasPicoInteraccion;

    @Column(name = "desviacion_equipos_tipo")
    private Double desviacionEquiposTipo;

    public Factura() {
    }

    public Long getId() {
        return id;
    }

    public Double getConsumoMensual() {
        return consumoMensual;
    }

    public void setConsumoMensual(Double consumoMensual) {
        this.consumoMensual = consumoMensual;
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

    public Double getHorasPromedioUso() {
        return horasPromedioUso;
    }

    public void setHorasPromedioUso(Double horasPromedioUso) {
        this.horasPromedioUso = horasPromedioUso;
    }

    public String getEstacionAnio() {
        return estacionAnio;
    }

    public void setEstacionAnio(String estacionAnio) {
        this.estacionAnio = estacionAnio;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
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

    public Boolean getTieneCalentador() {
        return tieneCalentador;
    }

    public void setTieneCalentador(Boolean tieneCalentador) {
        this.tieneCalentador = tieneCalentador;
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

    public Double getIntensidadPorEquipo() {
        return intensidadPorEquipo;
    }

    public void setIntensidadPorEquipo(Double intensidadPorEquipo) {
        this.intensidadPorEquipo = intensidadPorEquipo;
    }

    public Double getHorasPicoInteraccion() {
        return horasPicoInteraccion;
    }

    public void setHorasPicoInteraccion(Double horasPicoInteraccion) {
        this.horasPicoInteraccion = horasPicoInteraccion;
    }

    public Double getDesviacionEquiposTipo() {
        return desviacionEquiposTipo;
    }

    public void setDesviacionEquiposTipo(Double desviacionEquiposTipo) {
        this.desviacionEquiposTipo = desviacionEquiposTipo;
    }
}
