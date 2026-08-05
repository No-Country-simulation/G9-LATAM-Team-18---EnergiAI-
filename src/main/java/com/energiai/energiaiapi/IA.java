package com.energiai.energiaiapi;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import ai.onnxruntime.OnnxMap;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.util.List;
import java.util.Map;
// ---------------------------------------------------------------------
// 1. DTO para estructurar los resultados del posprocesamiento
// ---------------------------------------------------------------------
class ResultadoPrediccion {
    private final long prediccion;
    private final double[] probabilidades;

    public ResultadoPrediccion(long prediccion, double[] probabilidades) {
        this.prediccion = prediccion;
        this.probabilidades = probabilidades;
    }

    public long getPrediccion() { return prediccion; }
    public double[] getProbabilidades() { return probabilidades; }

    @Override
    public String toString() {
        return String.format("{ prediccion: %d, probabilidades: [%.4f, %.4f] }",
                prediccion, probabilidades[0], probabilidades[1]);
    }
}

// ---------------------------------------------------------------------
// 2. Interfaz que reemplaza la simulación de clase abstracta de JS
// ---------------------------------------------------------------------
interface IPerfilDeConsumo {
    Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException;
    String posprocesamiento(OrtSession.Result resultados) throws OrtException;
}

// ---------------------------------------------------------------------
// 3. Implementación concreta del perfil
// ---------------------------------------------------------------------
class UnGranPerfil implements IPerfilDeConsumo {
    private final float[] valor;

    public UnGranPerfil(float[] valor) {
        this.valor = valor;
    }

    @Override
    public Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException {
        // En ONNX Java, una forma [1, N] se crea pasando un arreglo bidimensional float[1][N]
        float[][] entrada2D = new float[1][this.valor.length];
        System.arraycopy(this.valor, 0, entrada2D[0], 0, this.valor.length);

        OnnxTensor tensorEntrada = OnnxTensor.createTensor(env, entrada2D);

        Map<String, OnnxTensor> feeds = new HashMap<>();
        feeds.put("float_input", tensorEntrada);
        return feeds;
    }

    @Override
    public String posprocesamiento(OrtSession.Result resultados) throws OrtException {
        // 1. Extracción de la predicción (label)
        long[] labelData = (long[]) resultados.get("label").get().getValue();
        long prediccion = labelData[0];

        // 2. Extracción de probabilidades como matriz float[][]
        float[][] probMatrix = (float[][]) resultados.get("probabilities").get().getValue();

        // Tomamos la primera fila (para el primer registro del batch)
        float prob0 = probMatrix[0][0]; // Probabilidad de la Clase 0
        float prob1 = probMatrix[0][1]; // Probabilidad de la Clase 1

        return new ResultadoPrediccion(prediccion, new double[]{prob0, prob1}).toString();
    }
}

// ---------------------------------------------------------------------
// 4. Clase principal de carga e inferencia de IA
// ---------------------------------------------------------------------
class EnergiA implements AutoCloseable {
    private final String direccionIA;
    private OrtEnvironment env;
    private OrtSession session;

    public EnergiA(String direccionIA) {
        this.direccionIA = direccionIA;
    }

    public synchronized void cargarModelo() throws OrtException {
        if (this.session == null) {
            System.out.println("Cargando modelo ONNX en memoria...");
            this.env = OrtEnvironment.getEnvironment();
            this.session = this.env.createSession(this.direccionIA, new OrtSession.SessionOptions());
            System.out.println("Modelo cargado exitosamente.");
        }
    }

    public String predecir(IPerfilDeConsumo perfilDeConsumo) {
        Map<String, OnnxTensor> feeds = null;
        try {
            if (this.session == null) {
                cargarModelo();
                if (this.session == null) {
                    throw new IllegalStateException("No se pudo cargar el modelo");
                }
            }

            feeds = perfilDeConsumo.preprocesamiento(this.env);

            long inicio = System.nanoTime();

            // Usamos try-with-resources para asegurar que se liberen los resultados en C++
            try (OrtSession.Result resultados = session.run(feeds)) {
                long fin = System.nanoTime();
                double tiempoMs = (fin - inicio) / 1_000_000.0;

                System.out.printf("⚡ Tiempo de inferencia: %.2f ms%n", tiempoMs);

                return perfilDeConsumo.posprocesamiento(resultados);
            }

        } catch (Exception e) {
            System.err.println("Error al ejecutar el modelo ONNX: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // Liberamos manualmente los tensores de entrada generados
            if (feeds != null) {
                for (OnnxTensor tensor : feeds.values()) {
                    tensor.close();
                }
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (session != null) session.close();
        if (env != null) env.close();
    }
}

class PerfilMegaEnergia implements IPerfilDeConsumo {

    private final float consumoKwh;
    private final boolean usoHorarioPico;
    private final float cantidadEquipos;
    private final String tipoInmueble; // "Casa", "Departamento", "Negocio"
    private final float horasAltoConsumo;

    public PerfilMegaEnergia(float consumoKwh, boolean usoHorarioPico, float cantidadEquipos, String tipoInmueble, float horasAltoConsumo) {
        this.consumoKwh = consumoKwh;
        this.usoHorarioPico = usoHorarioPico;
        this.cantidadEquipos = cantidadEquipos;
        this.tipoInmueble = tipoInmueble;
        this.horasAltoConsumo = horasAltoConsumo;
    }

    @Override
    public Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException {
        // ORDEN EXACTO QUE ESPERA LA IA TRAS GET_DUMMIES:
        // [0] consumo_kwh
        // [1] uso_horario_pico
        // [2] cantidad_equipos
        // [3] tipo_inmueble_Departamento
        // [4] tipo_inmueble_Negocio
        // [5] horas_alto_consumo

        float isPico = this.usoHorarioPico ? 1.0f : 0.0f;
        float isDepartamento = "Departamento".equalsIgnoreCase(this.tipoInmueble) ? 1.0f : 0.0f;
        float isNegocio = "Negocio".equalsIgnoreCase(this.tipoInmueble) ? 1.0f : 0.0f;

        float[][] entrada2D = new float[1][6];
        entrada2D[0][0] = this.consumoKwh;
        entrada2D[0][1] = isPico;
        entrada2D[0][2] = this.cantidadEquipos;
        entrada2D[0][3] = isDepartamento;     // Posición 3
        entrada2D[0][4] = isNegocio;          // Posición 4
        entrada2D[0][5] = this.horasAltoConsumo; // Posición 5

        OnnxTensor tensorEntrada = OnnxTensor.createTensor(env, entrada2D);

        Map<String, OnnxTensor> feeds = new HashMap<>();
        feeds.put("float_input", tensorEntrada);
        return feeds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String posprocesamiento(OrtSession.Result resultados) throws OrtException {
        ai.onnxruntime.OnnxValue labelValue = null;
        ai.onnxruntime.OnnxValue probValue = null;

        // Buscamos dinámicamente las salidas
        for (Map.Entry<String, ai.onnxruntime.OnnxValue> entry : resultados) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("label") || key.contains("output_0") || labelValue == null) {
                labelValue = entry.getValue();
            }
            if (key.contains("prob") || key.contains("output_1")) {
                probValue = entry.getValue();
            }
        }

        if (labelValue == null) {
            throw new IllegalStateException("El modelo ONNX no devolvió ninguna salida válida.");
        }

        // 1. Extracción de la clasificación (predicción)
        String[] labelData = (String[]) labelValue.getValue();
        String clasificacion = labelData[0];

        // 2. Extracción de probabilidades (desempaquetando ai.onnxruntime.OnnxMap)
        float certeza = 0.0f;
        if (probValue != null) {
            Object val = probValue.getValue();
            if (val instanceof List && !((List<?>) val).isEmpty()) {
                Object firstItem = ((List<?>) val).get(0);
                Map<String, Float> probabilidades = null;

                // Si es un OnnxMap de ONNX Runtime, extraemos su Map interno
                if (firstItem instanceof ai.onnxruntime.OnnxMap) {
                    probabilidades = (Map<String, Float>) ((ai.onnxruntime.OnnxMap) firstItem).getValue();
                } else if (firstItem instanceof Map) {
                    probabilidades = (Map<String, Float>) firstItem;
                }

                if (probabilidades != null) {
                    certeza = probabilidades.getOrDefault(clasificacion, 0.0f) * 100;
                }
            }
        }

        // 3. Generación de la recomendación
        String recomendacion = generarRecomendacion(clasificacion);

        return String.format(
                "Resultado: %s (Certeza: %.1f%%)\nRecomendación: %s",
                clasificacion.toUpperCase(), certeza, recomendacion
        );
    }

    private String generarRecomendacion(String clasificacion) {
        switch (clasificacion.toLowerCase()) {
            case "eficiente":
                return "¡Excelente manejo! Sigue así para mantener tu tarifa.";
            case "moderado":
                return "Trata de desplazar el uso de aparatos fuera del horario pico.";
            case "ineficiente":
                return "Alerta: Revisa el aislamiento térmico y reemplaza equipos antiguos.";
            default:
                return "Sin recomendación disponible.";
        }
    }
}

class Perfil2 implements IPerfilDeConsumo{
    private final float[] valor;
    public Perfil2(float[] valor){
        this.valor = valor;
    }

    @Override
    public Map<String, OnnxTensor> preprocesamiento(OrtEnvironment env) throws OrtException {
        // Si 'this.valor' es un float[] unidimensional, lo envolvemos en un float[][] (2D)
        float[][] matriz2D = new float[][]{ this.valor };

        // Se crea el tensor con la matriz de 2 dimensiones requerida
        OnnxTensor tensorEntrada = OnnxTensor.createTensor(env, matriz2D);

        return Collections.singletonMap("float_input", tensorEntrada);
    }

    @Override
    public String posprocesamiento(OrtSession.Result resultados) throws OrtException {
        // 1. Obtener la etiqueta en texto
        String[] labelData = (String[]) resultados.get(0).getValue();
        String prediccionTexto = labelData[0]; // "eficiente", "moderado", "ineficiente"

        // 2. Mapear el texto a un ID de tipo long (0, 1, 2)
        long prediccionId = switch (prediccionTexto.toLowerCase()) {
            case "eficiente" -> 0L;
            case "moderado" -> 1L;
            case "ineficiente" -> 2L;
            default -> -1L;
        };

        // 3. Obtener probabilidades
        @SuppressWarnings("unchecked")
        List<OnnxMap> listaMapas = (List<OnnxMap>) resultados.get(1).getValue();

        @SuppressWarnings("unchecked")
        Map<String, Float> mapaProbabilidades = (Map<String, Float>) listaMapas.get(0).getValue();

        double probEficiente = mapaProbabilidades.getOrDefault("eficiente", 0.0f).doubleValue();
        double probModerado = mapaProbabilidades.getOrDefault("moderado", 0.0f).doubleValue();
        double probIneficiente = mapaProbabilidades.getOrDefault("ineficiente", 0.0f).doubleValue();

        // 4. Instanciar pasándole el long (prediccionId)
        return new ResultadoPrediccion(
                prediccionId,
                new double[]{ probEficiente, probModerado, probIneficiente }
        ).toString();
    }
}

// ---------------------------------------------------------------------
// 5. Clase de prueba (Main)
// ---------------------------------------------------------------------
//public class Main {
//    public static void main(String[] args) {
        // try-with-resources asegura cerrar la sesión ONNX al terminar
//        try (EnergiA energia = new EnergiA("./modelo_rf.onnx")) {

//            System.out.println(energia.predecir(new UnGranPerfil(new float[]{0, 0, 0, 0, 0, 2.33f, 70.90f, 1, 1, 1})));
//            System.out.println(energia.predecir(new UnGranPerfil(new float[]{0, 0, 0, 0, 0, 2.48f, 69.69f, 1, 1, 1})));
//            System.out.println(energia.predecir(new UnGranPerfil(new float[]{0, 0, 0, 0, 0, 3.33f, 71.10f, 1, 1, 1})));
//            System.out.println(energia.predecir(new UnGranPerfil(new float[]{0, 0, 0, 0, 0, 4.44f, 69.00f, 1, 1, 1})));
//            System.out.println(energia.predecir(new UnGranPerfil(new float[]{0, 0, 0, 0, 0, 1.11f, 70.50f, 1, 1, 1})));

//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}