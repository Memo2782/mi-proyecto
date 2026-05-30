# Arquitectura Reactiva Empresarial: Spring Boot, gRPC, Akka Classic & MongoDB

Este proyecto implementa una arquitectura distribuida de alto rendimiento y baja latencia utilizando programación reactiva, el modelo de actores clásico de Akka y comunicación binaria de alta velocidad mediante gRPC. El sistema expone métricas operacionales listas para ser raspadas por Prometheus y gestiona el envío de notificaciones mediante el protocolo SMPP.

## 🚀 Tecnologías y Componentes Utilizados

*   **Java 17** (OpenJDK Temurin / Homebrew)
*   **Spring Boot 3.4.2** & **Gradle Kotlin DSL (`build.gradle.kts`)**
*   **Spring Webflux**: API REST asíncrona y no bloqueante.
*   **Spring Data MongoDB Reactive**: Conexión y persistencia reactiva.
*   **Akka Classic Actors (v2.8.5)**: Procesamiento desacoplado y concurrente de pedidos.
*   **gRPC (v1.62.2)** & **Protocol Buffers (v3.25.3)**: Protocolo de inserción binaria de datos.
*   **Fizzed Cloudhopper SMPP (v5.0.9)**: Conector nativo para envío de SMS.
*   **Spring Actuator & Micrometer**: Exposición de métricas en formato nativo de Prometheus.
*   **Log4j2 (YAML)**: Motor de bitácoras configurado mediante `log4j2.yml`.

---

## 🛠️ Requerimientos Cumplidos de Forma Estricta

1.  **Configuración Programática (Punto 4):** Se evita la autoconfiguración clásica de Spring. El puerto del servidor Webflux (`apiPort: 9898`) y las propiedades de MongoDB (`mongodbUri`, `mongodbDatabase`) se leen desde `application.yml` y se inyectan manualmente en la clase `AppConfig.java`.
2.  **Servicio gRPC (Punto 5):** Definición e implementación de un canal de red en el puerto `50051` para la inserción de pedidos con listas de ítems complejas.
3.  **Akka Classic Actors (Punto 6 y 7):** Un actor clásico central (`OrderActor`) recibe de forma asíncrona la petición gRPC, persiste la entidad `Order` mapeada con tipos de datos avanzados (`ObjectId`, `OffsetDateTime`) en MongoDB, e incrementa las métricas operacionales.
4.  **Notificaciones SMPP (Punto 8):** Integración de la librería Cloudhopper para despachar un SMS estructurado con el formato requerido una vez procesado el pedido por el actor.
5.  **API REST Webflux (Punto 9):** Endpoints reactivos dedicados para consultar estados por ID y cálculo de volúmenes mediante filtrado temporal utilizando `OffsetDateTime`.
6.  **Configuraciones YAML (Punto 3, 11 y 12):** Sustitución completa de `.properties` y `.xml` por archivos estructurados en formato YAML (`application.yml` y `log4j2.yml`). Exposición de métricas Prometheus mediante Actuator incluyendo un contador global automatizado.

---

## 📁 Estructura del Workspace

```text
mi-proyecto/
├── build.gradle.kts           # Configuración de dependencias Kotlin DSL y Plugins de gRPC
├── settings.gradle.kts        # Definición del nombre del microservicio
├── .github/workflows/
│   └── build.yml              # Pipeline de integración continua (CI) para GitHub Actions
└── src/
    └── main/
        ├── proto/
        │   └── order.proto    # Definición de contratos gRPC / Protobuf
        ├── resources/
        │   ├── application.yml# Variables de conexión programática y Actuator
        │   └── log4j2.yml     # Configuración del motor de Logs en YAML
        └── java/
            └── com/
                └── ejemplo/
                    ├── Application.java          # Bootstrap de Spring (Excluye autoconfig)
                    ├── config/
                    │   └── AppConfig.java        # Fábricas de bases de datos y Custom Converters
                    ├── model/
                    │   └── Order.java            # Entidad de persistencia en MongoDB
                    ├── repository/
                    │   └── OrderRepository.java  # Repositorio reactivo Spring Data
                    ├── smpp/
                    │   └── SmppClientService.java # Cliente Cloudhopper SMPP
                    ├── akka/
                    │   └── OrderActor.java       # Actor Classic Core & Micrometer Counters
                    ├── grpc/
                    │   ├── GrpcServerManager.java # Orquestador del Servidor gRPC
                    │   └── GrpcTestClient.java   # Cliente de simulación de carga (Pruebas)
                    └── controller/
                        └── OrderApiController.java # Endpoints HTTP Reactivos de Webflux
```

---

## 📋 Trazabilidad de Log de Ejecución Real (Log4j2 YAML)

A continuación se muestra la secuencia real capturada en consola que evidencia el ciclo de vida síncrono/asíncrono de un pedido impactado a través del pipeline de red:

```text
# 1. El motor inicia de forma programática mapeando los puertos personalizados
INFO  com.ejemplo.smpp.SmppClientService - Cliente SMPP inicializado de forma programática.
INFO  com.ejemplo.grpc.GrpcServerManager - Servidor gRPC iniciado de forma nativa en el puerto 50051
INFO  org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver - Exposing 3 endpoints beneath base path '/actuator'
INFO  org.springframework.boot.web.embedded.netty.NettyWebServer - Netty started on port 9898 (http)
INFO  com.ejemplo.Application - Started Application in 1.646 seconds

# 2. Llegada de un pedido vía gRPC y delegación inmediata al despachador de Akka
INFO  com.ejemplo.grpc.GrpcServerManager - Mensaje gRPC delegado al sistema de Actores para ID: ORD-2026-PERU
INFO  com.ejemplo.akka.OrderActor - Actor Classic procesando payload para ID: ORD-2026-PERU

# 3. Persistencia asíncrona reactiva confirmada en MongoDB y disparo de notificación por la pasarela SMPP
INFO  com.ejemplo.akka.OrderActor - Guardado confirmado en MongoDB: ORD-2026-PERU
WARN  com.ejemplo.smpp.SmppClientService - [Simulación SMS] Para: +51999888777 | Texto: Your order ORD-2026-PERU has been processed

# 4. Consumo concurrente y no bloqueante de hilos NIO en la API Webflux leyendo de base de datos
INFO  com.ejemplo.controller.OrderApiController - Consulta REST Webflux para el estado de la orden: ORD-2026-PERU
INFO  com.ejemplo.controller.OrderApiController - Consulta REST Webflux para volumen de órdenes entre 2026-05-01T00:00Z y 2026-05-31T23:59:59Z
```

---

## ⚙️ Compilación y Ejecución en macOS

### 1. Iniciar Base de Datos Local
Asegúrate de tener corriendo tu instancia nativa de MongoDB en tu Mac a través de Homebrew:
```bash
brew services start mongodb-community
```

### 2. Compilar Protocol Buffers y Código gRPC
Genera de forma limpia las clases Java a partir del archivo `.proto`:
```bash
gradle generateProto
```

### 3. Iniciar el Servidor de Aplicación
Arranca el microservicio:
```bash
gradle bootRun
```

---

## 📊 Flujo Secuencial de Pruebas de Red

Abre una **nueva pestaña** de la terminal (`Cmd + T`) en tu Mac y ejecuta los siguientes comandos cURL para comprobar el comportamiento reactivo de extremo a extremo:

### Paso A: Verificar Estado Inicial en Prometheus
```bash
curl -s http://localhost:9898/actuator/prometheus | grep app_orders_processed_total
```

### Paso B: Disparar Pedido vía gRPC
Envía un pedido complejo (`ORD-2026-PERU`) directamente al puerto `50051` ejecutando el cliente de simulación:
```bash
javac -cp "build/classes/java/main:\$(find ~/.gradle/caches -name "*.jar" | tr '\n' ':')" -d build/classes/java/main src/main/java/com/ejemplo/grpc/GrpcTestClient.java
java -cp "build/classes/java/main:\$(find ~/.gradle/caches -name "*.jar" | tr '\n' ':')" com.ejemplo.grpc.GrpcTestClient
```

### Paso C: Consultar Estado del Pedido en Webflux (API Reactiva)
```bash
curl -i http://localhost:9898/api/orders/ORD-2026-PERU/status
```

### Paso D: Consultar Conteo de Pedidos por Rango de Fechas
```bash
curl -i -X GET "http://localhost:9898/api/orders/count-by-date?start=2026-05-01T00:00:00Z&end=2026-05-31T23:59:59Z"
```

### Paso E: Verificar Incremento de Métrica Operacional
```bash
curl -s http://localhost:9898/actuator/prometheus | grep app_orders_processed_total
```
