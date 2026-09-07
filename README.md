# Bank Batch

Proyecto Java con Spring Boot, Spring Batch, Spring Web, Spring Security y PostgreSQL para procesar archivos bancarios en lote y exponer la informacion procesada mediante APIs BFF para tres canales: Web, Movil y Cajero.

## Objetivo

El objetivo del sistema es procesar datos bancarios desde archivos CSV, validar su calidad, normalizar la informacion, persistir resultados en PostgreSQL y entregar vistas simplificadas para distintos canales consumidores mediante una estrategia BFF.

La aplicacion permite:

- Procesar transacciones bancarias.
- Calcular intereses de cuentas.
- Procesar movimientos anuales.
- Generar resumenes diarios y anuales.
- Consultar informacion bancaria desde APIs REST protegidas.
- Separar las respuestas segun el canal que consume la informacion: Web, Movil o Cajero.

## Estrategia BFF elegida

Se eligio una estrategia BFF, o Backend For Frontend, separada por canal. En lugar de exponer una unica API generica para todos los clientes, el proyecto define controladores especificos para cada experiencia:

- `WebBffController`: entrega una vision mas completa para el canal web.
- `MovilBffController`: entrega una respuesta mas liviana para aplicaciones moviles.
- `CajeroBffController`: entrega operaciones puntuales para cajeros automaticos, como consulta de saldo y retiro.

Esta estrategia permite adaptar cada respuesta a las necesidades reales de cada cliente. El canal web puede mostrar indicadores generales del procesamiento, el canal movil recibe menos datos para simplificar la respuesta, y el cajero se enfoca en operaciones transaccionales concretas.

La separacion tambien permite aplicar seguridad por rol, evitando que un usuario de un canal consuma endpoints de otro canal.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- Spring Batch
- Spring Web
- Spring Security
- Spring JDBC
- PostgreSQL 16
- Maven Wrapper
- Docker Compose

## Estructura

```text
.
|-- docker-compose.yml
|-- pom.xml
|-- mvnw
|-- mvnw.cmd
|-- README.md
`-- src
    |-- main
    |   |-- java/cl/duoc/bank_batch
    |   |   |-- BankBatchApplication.java
    |   |   |-- bff
    |   |   |   |-- cajero
    |   |   |   |   |-- CajeroBffController.java
    |   |   |   |   `-- CajeroService.java
    |   |   |   |-- movil
    |   |   |   |   `-- MovilBffController.java
    |   |   |   `-- web
    |   |   |       `-- WebBffController.java
    |   |   |-- config
    |   |   |   |-- BatchConfig.java
    |   |   |   |-- EstadoCuentaJobConfig.java
    |   |   |   |-- InteresJobConfig.java
    |   |   |   |-- SecurityConfig.java
    |   |   |   `-- TransaccionJobConfig.java
    |   |   |-- model
    |   |   |-- policy
    |   |   `-- processor
    |   `-- resources
    |       |-- application.properties
    |       |-- schema.sql
    |       `-- data
    |           |-- cuentas_anuales.csv
    |           |-- intereses.csv
    |           `-- transacciones.csv
    `-- test
```

## Componentes principales

### Jobs batch

La aplicacion contiene tres jobs principales:

- `transaccionJob`: procesa transacciones bancarias desde `transacciones.csv`.
- `interesJob`: calcula intereses de cuentas desde `intereses.csv`.
- `estadoCuentaJob`: procesa movimientos anuales desde `cuentas_anuales.csv`.

### Tablas principales

El archivo `src/main/resources/schema.sql` crea las tablas necesarias:

- `transacciones_procesadas`
- `resumen_diario`
- `cuentas_intereses`
- `movimientos_anuales`
- `resumen_anual`
- `retiros_cajero`

### Procesamiento y calidad de datos

Los jobs usan procesamiento por chunks de 5 registros, tolerancia a fallos, politica personalizada de skips y reintentos para errores transitorios de base de datos.

La clase `DataQualityDecider` valida la calidad del procesamiento. Si mas del 10% de los registros evaluados fueron omitidos, el job falla con estado `CALIDAD_INSUFICIENTE`.

La clase `CustomSkipPolicy` permite omitir errores controlados, como:

- `FlatFileParseException`
- `NumberFormatException`
- `DateTimeParseException`
- `IllegalArgumentException`

### Mejora Semana 3: resumen diario

A partir del feedback docente de la entrega anterior, se agrego al flujo de `transaccionJob` la generacion del resumen diario mediante `resumenDiarioStep`.

El flujo actual es:

```text
transaccionStep
      |
      v
DataQualityDecider
      |
      +-- calidad insuficiente -> FAIL
      |
      +-- calidad correcta -> resumenDiarioStep
                                  |
                                  v
                             resumen_diario
```

Durante las pruebas, el proceso termino correctamente, mostro el mensaje:

```text
Resumen diario generado correctamente
```

Ademas, el job `transaccionJob` finalizo con estado `COMPLETED`.

## Web / Movil / Cajero

### BFF Web

Ruta base:

```text
/api/bff/web
```

Endpoint disponible:

```http
GET /api/bff/web/resumen
```

Entrega un resumen general para el canal web:

- Total de transacciones procesadas.
- Total de resumenes diarios.
- Total de cuentas con intereses.
- Total de resumenes anuales.

Este canal esta pensado para una vista administrativa o de escritorio, donde se requiere mas informacion consolidada.

### BFF Movil

Ruta base:

```text
/api/bff/movil
```

Endpoint disponible:

```http
GET /api/bff/movil/resumen
```

Entrega una respuesta mas simple:

- Total de transacciones procesadas.
- Total de resumenes diarios.

Este canal esta pensado para clientes moviles, donde conviene entregar respuestas mas livianas y directas.

### BFF Cajero

Ruta base:

```text
/api/bff/cajero
```

Endpoints disponibles:

```http
GET /api/bff/cajero/saldo/{cuentaId}
POST /api/bff/cajero/retiro/{cuentaId}
```

El cajero permite:

- Consultar saldo disponible de una cuenta.
- Realizar un retiro validando monto, existencia de cuenta y saldo suficiente.
- Registrar el retiro en la tabla `retiros_cajero`.
- Actualizar el `saldo_final` de la cuenta en `cuentas_intereses`.

Este canal esta orientado a operaciones concretas y transaccionales.

## Seguridad

La seguridad esta configurada en `SecurityConfig` con Spring Security y autenticacion HTTP Basic.

Cada canal tiene usuario y rol propio:

| Canal | Usuario | Password | Rol |
|---|---|---|---|
| Web | `web` | `web123` | `WEB` |
| Movil | `movil` | `movil123` | `MOVIL` |
| Cajero | `cajero` | `cajero123` | `CAJERO` |

Reglas de autorizacion:

- `/api/bff/web/**` requiere rol `WEB`.
- `/api/bff/movil/**` requiere rol `MOVIL`.
- `/api/bff/cajero/**` requiere rol `CAJERO`.
- Cualquier otra ruta requiere autenticacion.

CSRF esta deshabilitado para facilitar el consumo de APIs REST desde herramientas como Postman o `curl`.

Las credenciales estan definidas en memoria y son adecuadas para un entorno academico o de demostracion. En un entorno productivo deberian reemplazarse por usuarios persistidos, passwords cifradas y mecanismos como OAuth2/JWT.

## Configuracion

La conexion a PostgreSQL esta definida en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/banco
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql

spring.batch.jdbc.initialize-schema=never
```

El archivo `docker-compose.yml` levanta PostgreSQL con:

- Imagen: `postgres:16`
- Contenedor: `banco-postgres`
- Base de datos: `banco`
- Usuario: `postgres`
- Password: `postgres`
- Puerto local: `5433`

## Como ejecutar

### 1. Requisitos

- JDK 21
- Docker Desktop o Docker Engine
- PowerShell, CMD o terminal compatible

### 2. Levantar PostgreSQL

```powershell
docker compose up -d
```

### 3. Compilar el proyecto

```powershell
.\mvnw.cmd compile
```

### 4. Ejecutar pruebas

```powershell
.\mvnw.cmd test
```

### 5. Ejecutar la aplicacion

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionJob"
```

Se especifica `transaccionJob` porque el proyecto contiene varios Spring Batch Jobs registrados. Con esta ejecucion se procesan las transacciones y, si la calidad de datos es correcta, se genera el resumen diario.

La aplicacion queda disponible en:

```text
http://localhost:8080
```

### 6. Ejecutar un job especifico

Jobs disponibles:

- `transaccionJob`
- `interesJob`
- `estadoCuentaJob`

Para ejecutar otro proceso, se reemplaza el valor de `spring.batch.job.name` por el nombre del job requerido:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=interesJob"
```

Para poblar la informacion usada por todos los BFF, se deben ejecutar los jobs necesarios segun los datos que se quieran consultar.

## Como probar las APIs

Antes de probar los endpoints, se debe tener PostgreSQL levantado y la aplicacion ejecutandose.

### Probar BFF Web

```powershell
curl.exe -u web:web123 http://localhost:8080/api/bff/web/resumen
```

Respuesta real:

```json
{
  "canal": "web",
  "descripcion": "BFF Web Banco XYZ",
  "resumen": {
    "transaccionesProcesadas": 1000,
    "resumenesDiarios": 338,
    "cuentasConIntereses": 50,
    "resumenesAnuales": 20
  }
}
```

Estos valores corresponden a la ejecucion real de las pruebas.

### Probar BFF Movil

```powershell
curl.exe -u movil:movil123 http://localhost:8080/api/bff/movil/resumen
```

Respuesta esperada:

```json
{
  "canal": "movil",
  "transacciones": 1000,
  "resumenesDiarios": 338
}
```

### Probar consulta de saldo en Cajero

```powershell
curl.exe -u cajero:cajero123 http://localhost:8080/api/bff/cajero/saldo/104
```

Respuesta esperada:

```json
{
  "canal": "cajero",
  "cuentaId": 104,
  "saldoDisponible": 7000.00
}
```

### Probar retiro en Cajero

```powershell
$cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("cajero:cajero123"))
$body = @{ monto = 1000 } | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/bff/cajero/retiro/104" `
  -Method POST `
  -Headers @{ Authorization = "Basic $cred" } `
  -ContentType "application/json" `
  -Body $body
```

Respuesta esperada:

```json
{
  "canal": "cajero",
  "cuentaId": 104,
  "montoRetirado": 1000,
  "saldoAnterior": 7000.00,
  "saldoDisponible": 6000.00
}
```

Este comando con `Invoke-RestMethod` fue el metodo probado exitosamente en PowerShell para enviar el cuerpo JSON del retiro.

### Verificar persistencia del retiro

El retiro se puede comprobar directamente en PostgreSQL con:

```sql
SELECT
    id,
    cuenta_id,
    monto,
    fecha,
    saldo_anterior,
    saldo_posterior
FROM retiros_cajero
ORDER BY id DESC
LIMIT 5;
```

Registro real generado durante la prueba:

| id | cuenta_id | monto | saldo_anterior | saldo_posterior |
|---:|---:|---:|---:|---:|
| 1 | 104 | 1000.00 | 7000.00 | 6000.00 |

La fecha y hora del retiro quedaron registradas automaticamente en la columna `fecha`.

### Probar seguridad y autorizacion

Prueba sin credenciales:

```powershell
curl.exe -i http://localhost:8080/api/bff/web/resumen
```

Resultado real:

```text
HTTP 401 Unauthorized
```

Prueba con usuario correcto Web:

```powershell
curl.exe -i -u web:web123 http://localhost:8080/api/bff/web/resumen
```

Resultado real:

```text
HTTP 200 OK
```

### Probar autorizacion por canal

Si se intenta acceder a un endpoint con un usuario de otro canal, la aplicacion debe responder con error de autorizacion.

Ejemplo:

```powershell
curl.exe -i -u cajero:cajero123 http://localhost:8080/api/bff/web/resumen
```

Resultado real:

```text
HTTP/1.1 403
Forbidden
```

Esto demuestra que el usuario `cajero` esta autenticado, pero no tiene autorizacion para utilizar el BFF Web.

Prueba con usuario Movil:

```powershell
curl.exe -i -u movil:movil123 http://localhost:8080/api/bff/movil/resumen
```

Resultado real:

```text
HTTP/1.1 200
```

Prueba con usuario Cajero:

```powershell
curl.exe -i -u cajero:cajero123 http://localhost:8080/api/bff/cajero/saldo/104
```

Resultado real:

```text
HTTP/1.1 200
```

## Consultas utiles en base de datos

```sql
SELECT * FROM transacciones_procesadas;
SELECT * FROM resumen_diario;
SELECT * FROM cuentas_intereses;
SELECT * FROM movimientos_anuales;
SELECT * FROM resumen_anual;
SELECT * FROM retiros_cajero;
```

## Detener el entorno

Detener PostgreSQL:

```powershell
docker compose down
```

Detener PostgreSQL y borrar el volumen:

```powershell
docker compose down -v
```

## Verificacion final

Para validar el proyecto completo:

```powershell
docker compose up -d
.\mvnw.cmd compile
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionJob"
```

Luego se pueden probar las APIs con los comandos `curl.exe` indicados anteriormente.
