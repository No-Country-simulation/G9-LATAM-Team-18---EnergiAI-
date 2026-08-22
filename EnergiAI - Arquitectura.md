---
title: EnergiAI - Arquitectura

---

<img src="https://hackmd.io/_uploads/SyWOvF8wzg.png" width="55%">

# 1. Visión General del Sistema
El sistema se compone de una aplicación web moderna orientada a servicios, donde la lógica de negocio central integra un motor de Inteligencia Artificial (desarrollado por Ana y Erick). La solución combina una arquitectura de código desacoplada en capas y un despliegue optimizado en la nube sobre Oracle Cloud Infrastructure (OCI).


Para representar la arquitectura del sistema se utilizó el Modelo C4, enfocándonos en sus dos primeros niveles de abstracción (Contexto y Contenedores), los cuales permiten visualizar de manera clara las interacciones externas y la estructura de componentes desplegados.

### 1.1 Diagrama de contexto

en este diagrama se puede vizualisar sencillamente en terminos generales el como funciona la aplicacion
![diagrama de contexto](https://hackmd.io/_uploads/ry151UMDGx.png)

### 1.2 Diagrama de contenedores
en este diagrama se observa el flujo en el que trabaja la aplicacion para poder generar la respuesta al usuario
![diagrama de contenedores](https://hackmd.io/_uploads/S1_PQIfvfx.png)

# 2. capas de arquitectura
Se utiliza una arquitectura por capas para mantener la separación de responsabilidades y facilitar la escalabilidad:


* Capa Presentación y Aplicación: Aplicación web (Frontend) que interactúa directamente con el usuario final. Captura las variables del inmueble y el consumo energético, enviando peticiones en formato JSON mediante HTTPS hacia el servidor.

* Capa de Aplicación y Orquestación: Construida en Java con Spring Boot (controller, security). Se encarga de recibir las solicitudes HTTP, validar los contratos de entrada mediante deserializadores estrictos, gestionar la autenticación con JWT (OAuth) y orquestar el flujo hacia la lógica interna.

* Capa Motor de inferencia – Reglas de Negocio / Núcleo (IA): Representa el motor central del software y abarca tres módulos principales:

  * Cálculo de Costos: Módulo que calcula la proyección estacional de tarifas y variaciones energéticas (CalculadoraCostosEstacionales).

  * Inferencia de IA Local: Módulo desacoplado que ejecuta el modelo embebido en formato ONNX (service/inference) para clasificar el perfil y calcular el índice de eficiencia de forma local.

  * Servicio de Recomendaciones Externo: Integración que empaqueta el contexto analizado y consume la API externa de Gemini para generar recomendaciones personalizadas.

* Capa Persistencia y Datos: Capa encargada de la gestión del estado del sistema. Utiliza una base de datos relacional PostgreSQL gestionada mediante Spring Data JPA (repository) y controlada por scripts de migración estructurados (db.migration) para almacenar perfiles de usuario e historial de análisis


# 3. Arquitectura de Infraestructura (OCI Cloud)
El despliegue de la infraestructura está centralizado en un entorno virtualizado en la nube con la siguiente configuración:

* Componentes de Red y Nube
Red Virtual (VCN): Rango de red principal 10.0.0.0/16.

* Subred Pública: Rango 10.0.0.0/24, expuesta mediante un Internet Gateway para permitir el tráfico externo.

* Reglas de Seguridad (Security List):

    * SSH (22): Administración remota.

    
    * PostgreSQL (5432): Acceso a la base de datos (según sea necesario).
    
    *  TCP (8080): Acceso a la API Java 21.

# Servidor y Contenedores
Instancia de Cómputo: Servidor virtual en OCI sobre una VM Ampere A1.Flex ejecutando el sistema operativo Ubuntu.

Entorno de Ejecución: Por ahora, pruebas en PC de desarrollo, conectado a PostgreSQL remoto en OCI.

Contenedor de la Aplicación: Motor de BD PostgreSQL, Base de datos relacional vinculada al puerto 5432.

Aplicación (Backend): se sirve al tráfico web en el puerto TCP 8080. El backend es el artefacto **`energiai-api-0.0.1-SNAPSHOT.jar`** (Spring Boot, JDK 21), generado con `mvn package` y ejecutado con `java -jar`, con perfil `prod` para la VM OCI y `SPRING_DATASOURCE_URL` apuntando a `localhost` si la base corre en la misma máquina.
