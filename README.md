# G9-LATAM-Team-18---EnergiAI-
Crear una solución inteligente capaz de analizar patrones de consumo de energía eléctrica y generar información que ayude en la toma de decisiones relacionadas con la eficiencia energética.

# EnergiAI - Inteligencia para el Consumo de Energía ⚡

> **Hackathon ONE – Proyectos G9 | Alura + Oracle + NoCountry**  
> Sitio web del proyecto: [https://alura-es-cursos.github.io/proyectos-hackathon-g9-latam/](https://alura-es-cursos.github.io/proyectos-hackathon-g9-latam/)

---

## 📄 Descripción del Proyecto

**EnergiAI** es una solución inteligente diseñada para analizar patrones de consumo de energía eléctrica en viviendas y pequeños establecimientos. A través de técnicas de Ciencia de Datos y el despliegue en la nube, transforma variables de consumo en información clara y útil para incentivar la eficiencia energética, estimar impactos financieros y promover hábitos sostenibles.

La aplicación clasifica el perfil energético en tres categorías principales:
*   **Eficiente**
*   **Moderado**
*   **Ineficiente**

---

## 🎯 Necesidad del Cliente (Visión de Negocio)

Muchas personas y pequeños comercios reciben facturas de energía elevadas, pero carecen de visibilidad sobre qué hábitos o factores impactan directamente en sus costos. **EnergiAI** resuelve esta problemática permitiendo al usuario:
*   Comprender su perfil de consumo de manera sencilla.
*   Identificar focos de desperdicio energético.
*   Recibir recomendaciones de mejora personalizadas.
*   Estimar costos mensuales asociados.
*   Realizar un seguimiento de indicadores a lo largo del tiempo.

---

## 🚀 Objetivo del Hackathon & MVP

El objetivo principal es desarrollar un **Producto Mínimo Viable (MVP) funcional** que integre un modelo predictivo, una API REST documentada y la infraestructura en la nube de Oracle Cloud Infrastructure (OCI).

### Funcionalidades Obligatorias (MVP)
La API expone el endpoint principal `POST /analisis-energetica` que procesa la información y retorna una respuesta unificada en formato JSON.

#### 📊 Ejemplo de Solicitud (Payload de entrada)
```json
{
  "consumo_kwh": 420,
  "uso_horario_pico": true,
  "cantidad_equipos": 10,
  "tipo_inmueble": "Casa",
  "hours_alto_consumo": 8
}
```

#### ✅ Ejemplo de Respuesta (Payload de salida)
```json
{
  "categoria": "Ineficiente",
  "probabilidad": 0.81,
  "costo_estimado_mensual": 315.00,
  "recomendaciones": [
    "Reducir el uso de equipos durante los horarios pico",
    "Evaluar equipos con alto consumo energético",
    "Distribuir las actividades de mayor consumo a lo largo del día"
  ]
}
```
*Nota: La estimación financiera se calcula utilizando una tarifa de referencia estandarizada de **$ 0,75 por kWh**.*

---

## 🛠️ Rutas Técnicas y Arquitectura

La solución se divide en tres componentes estratégicos integrados:

### 🔬 1. Ciencia de Datos (Data Science)
Construcción de una base de datos propia (datos públicos, abiertos, manuales o simulados) para entrenar modelos supervisados que automaticen la clasificación.
*   **Tecnologías:** Python, Pandas, Scikit-Learn.
*   **Modelos Sugeridos:** Random Forest, Regresión Logística, Árboles de Decisión.
*   **Entregables:** Notebook con análisis exploratorio de datos (EDA), ingeniería de atributos, entrenamiento, métricas de evaluación y serialización del modelo para producción.

### ⚙️ 2. Back-End (API REST)
Desarrollo de la lógica del servidor que consume el modelo entrenado y expone la interfaz de comunicación para otros sistemas.
*   **Tecnologías:** Java y Spring Boot.
*   **Entregables:** Endpoints de análisis y consulta, validación de datos de entrada, manejo estructurado de errores y documentación técnica de la API.

### ☁️ 3. Oracle Cloud Infrastructure (OCI)
Integración obligatoria con al menos un servicio de la nube de Oracle para dar soporte a la arquitectura del proyecto:
*   **OCI Object Storage:** Almacenamiento seguro de bases de datos o archivos del modelo serializado.
*   **OCI Compute:** Alojamiento y despliegue del servidor de la API REST.
*   **OCI Functions:** Procesamiento específico o complementario bajo demanda de manera serverless.
*   **Base de datos (Opcional):** Persistencia de registros históricos.

---

## 📋 Requisitos Mínimos del Sistema

Para dar por aprobada la entrega del MVP, el proyecto debe cumplir rigurosamente con:
1.  Modelo de Machine Learning entrenado y cargado correctamente en el ecosistema.
2.  Clasificación funcional de perfiles de eficiencia con su respectivo cálculo de probabilidad.
3.  Generación dinámica de recomendaciones operativas.
4.  Cálculo de estimación de costos mensuales basado en la tarifa base ($0,75/kWh).
5.  API REST completamente funcional y documentada.
6.  Integración real con un servicio de nube OCI.
7.  Un documento o sección con al menos **tres ejemplos reales o simulados** de uso.

---

## 🌟 Recursos Opcionales (Próximos Pasos)

Sugerencias de valor agregado para escalar el proyecto más allá del MVP:
*   **Front-End:** Interfaz web sencilla para el ingreso de datos, visualización de gráficas y reportes.
*   **Historial y Analítica:** Panel de control (Dashboard) para comparar períodos y ver rankings de eficiencia.
*   **Automatización:** Procesamiento por lotes mediante archivos CSV y alertas automáticas de alto consumo.
*   **DevOps:** Containerización del entorno con Docker y desarrollo de pruebas automatizadas.
*   **Simuladores:** Módulo interactivo de simulación de escenarios de ahorro financiero.

---

## 👥 Colaboradores y Socios

*   **Alura:** Plataforma líder de educación en tecnología en Brasil, encargada de la capacitación de los alumnos.
*   **Oracle:** Socio tecnológico estratégico, proveedor del programa ONE y la infraestructura de nube de OCI.
*   **NoCountry:** Plataforma asociada experta en la organización de equipos multidisciplinares e infraestructura colaborativa.

---
*Desarrollado como parte de las iniciativas de innovación del programa Oracle Next Education (ONE) - G9 LATAM.*
