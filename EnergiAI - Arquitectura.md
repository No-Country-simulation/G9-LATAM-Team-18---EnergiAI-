---
title: EnergiAI - Arquitectura

---

# 1. Visión General del Sistema
El sistema se compone de una aplicación web moderna orientada a servicios, donde la lógica de negocio central integra un motor de Inteligencia Artificial (desarrollado por Ana y Erick). La solución combina una arquitectura de código desacoplada en capas y un despliegue optimizado en la nube sobre Oracle Cloud Infrastructure (OCI).

# 2. Arquitectura de Software (Capas del Aplicativo)
Se utiliza una arquitectura por capas para mantener la separación de responsabilidades y facilitar la escalabilidad:


* Capa Presentación y Aplicación: Interfaz visual que interactúa directamente con el usuario final. Se comunica con el servidor a través de peticiones HTTP/HTTPS.

* Capa de Aplicación y Orquestación: Funciona como puente entre la interfaz de usuario y las reglas del negocio, orquestando las peticiones, gestionando la autenticación y comunicando los datos.

* Capa Motor de inferencia – Reglas de Negocio / Núcleo (IA): El núcleo del software. Contiene el motor y algoritmos de Inteligencia Artificial desarrollados de forma independiente.


![mermaid-diagram-2026-07-30-185758](https://hackmd.io/_uploads/HkHV7_tHGe.png)




# 3. Arquitectura de Infraestructura (OCI Cloud)
El despliegue de la infraestructura está centralizado en un entorno virtualizado en la nube con la siguiente configuración:

* Componentes de Red y Nube
Red Virtual (VCN): Rango de red principal 10.0.0.0/16.

* Subred Pública: Rango 10.0.0.0/24, expuesta mediante un Internet Gateway para permitir el tráfico externo.

* Reglas de Seguridad (Security List):

    * SSH (22): Administración remota.

    * HTTP (80) / HTTPS (443): Tráfico web público.

    * PostgreSQL (5432): Acceso a la base de datos (según sea necesario).

# Servidor y Contenedores
Instancia de Cómputo: Servidor virtual en OCI sobre una VM Ampere A1.Flex ejecutando el sistema operativo Ubuntu.

Entorno de Ejecución: Por ahora, pruebas en PC de desarrollo, conectado a PostgreSQL remoto en OCI.

Motor de BD PostgreSQL: Base de datos relacional vinculada al puerto 5432.

Contenedor de la Aplicación (Backend/Frontend): Aplicación sirviendo el tráfico web en el puerto 80. El backend es el artefacto **`energiai-api-0.0.1-SNAPSHOT.jar`** (Spring Boot, JDK 21), generado con `mvn package` y ejecutado con `java -jar`, con perfil `prod` para la VM OCI y `SPRING_DATASOURCE_URL` apuntando a `localhost` si la base corre en la misma máquina.