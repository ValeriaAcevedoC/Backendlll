# Bank Batch

Proyecto Java/Spring Boot para procesamiento batch de datos bancarios con PostgreSQL y exposicion de APIs BFF protegidas con JWT para los canales Web, Movil y Cajero.

## Descripcion

La aplicacion procesa archivos CSV bancarios, valida y normaliza la informacion, guarda los resultados en PostgreSQL y expone datos consolidados mediante endpoints REST separados por canal.

El sistema incluye:

- Procesamiento batch de transacciones diarias.
- Calculo de intereses de cuentas.
- Procesamiento de movimientos anuales.
- Generacion de resumenes diarios y anuales.
- APIs BFF para Web, Movil y Cajero.
- Autenticacion con JWT.
- Autorizacion por rol de canal.
- HTTPS local con keystore PKCS12.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- Spring Batch
- Spring JDBC
- Spring Web
- Spring Security
- JJWT 0.12.6
- PostgreSQL 16
- Docker Compose
- Maven Wrapper

## Estructura principal

```text
.
|-- docker-compose.yml
|-- pom.xml
|-- README.md
|-- src
|   |-- main
|   |   |-- java/cl/duoc/bank_batch
|   |   |   |-- BankBatchApplication.java
|   |   |   |-- bff
|   |   |   |   |-- cajero
|   |   |   |   |-- movil
|   |   |   |   `-- web
|   |   |   |-- config
|   |   |   |-- model
|   |   |   |-- policy
|   |   |   |-- processor
|   |   |   `-- security
|   |   `-- resources
|   |       |-- application.properties
|   |       |-- bank-batch.p12
|   |       |-- schema.sql
|   |       `-- data
|   |           |-- cuentas_anuales.csv
|   |           |-- intereses.csv
|   |           `-- transacciones.csv
|   `-- test
```

## Arquitectura BFF

El proyecto usa una arquitectura BFF, o Backend For Frontend, separada por canal. Cada canal tiene su propio controlador, servicio o logica de negocio asociada, y DTOs especificos para exponer solamente los datos que necesita ese consumidor.

Flujo actual por canal:

```text
Web    -> WebBffController    -> WebBffService   -> WebResumenDTO
Movil  -> MovilBffController  -> MovilBffService -> MovilResumenDTO
Cajero -> CajeroBffController -> CajeroService   -> SaldoCajeroDTO / RetiroResponseDTO
```

### Mejora implementada: DTOs por canal

Como mejora aplicada a partir de la sugerencia docente de la entrega anterior, las respuestas de los BFF ahora usan DTOs especificos en vez de construir respuestas directamente con `Map<String, Object>`.

DTOs implementados:

- `WebResumenDTO`
- `MovilResumenDTO`
- `SaldoCajeroDTO`
- `RetiroResponseDTO`

Esta mejora deja explicito el contrato de datos de cada canal, facilita entender que campos devuelve cada endpoint y reduce el acoplamiento entre controladores y estructura de respuesta. Tambien mejora la modularidad, mantenibilidad y escalabilidad del proyecto, porque cada BFF puede evolucionar su respuesta sin afectar innecesariamente a los otros canales.

## Jobs batch

El proyecto define tres jobs:

| Job | Archivo de entrada | Resultado principal |
|---|---|---|
| `transaccionJob` | `data/transacciones.csv` | `transacciones_procesadas` y `resumen_diario` |
| `interesJob` | `data/intereses.csv` | `cuentas_intereses` |
| `estadoCuentaJob` | `data/cuentas_anuales.csv` | `movimientos_anuales` y `resumen_anual` |

Los steps trabajan con chunks de 5 registros, procesamiento multihilo, reintentos para errores transitorios de base de datos y una politica personalizada de skips.

### Calidad de datos

`CustomSkipPolicy` permite omitir errores controlados hasta un limite de 100 registros, entre ellos:

- `FlatFileParseException`
- `NumberFormatException`
- `DateTimeParseException`
- `IllegalArgumentException`

`DataQualityDecider` valida el porcentaje de omisiones. Si supera el 10% de los registros evaluados, el job falla con estado `CALIDAD_INSUFICIENTE`.

### Validaciones principales

Transacciones:

- Normaliza fechas en formatos `yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy` y `yyyy/MM/dd`.
- Valida montos nulos, negativos o iguales a cero.
- Normaliza el tipo de transaccion.
- Acepta `credito` y `debito`.
- Marca anomalias en vez de descartar registros procesables.

Intereses:

- Valida nombre, saldo, edad y tipo de cuenta.
- Acepta cuentas `ahorro` y `prestamo`.
- Calcula 1% de interes para ahorro.
- Calcula 2% de interes para prestamo.
- Guarda tasa, interes calculado, saldo final, validez y observacion.

Movimientos anuales:

- Normaliza fechas.
- Valida monto, tipo de movimiento y descripcion.
- Acepta `deposito`, `retiro` y `compra`.
- Genera resumen anual por cuenta.

## Base de datos

PostgreSQL se levanta con `docker-compose.yml`:

| Configuracion | Valor |
|---|---|
| Imagen | `postgres:16` |
| Contenedor | `banco-postgres` |
| Base de datos | `banco` |
| Usuario | `postgres` |
| Password | `postgres` |
| Puerto local | `5433` |

El archivo `src/main/resources/schema.sql` crea estas tablas:

- `transacciones_procesadas`
- `cuentas_intereses`
- `movimientos_anuales`
- `resumen_anual`
- `resumen_diario`
- `retiros_cajero`

## Configuracion

La aplicacion usa PostgreSQL local y arranca en HTTPS:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/banco
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:schema.sql

spring.batch.jdbc.initialize-schema=never
spring.batch.job.enabled=false

server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:bank-batch.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=bank-batch
```

Importante: `spring.batch.job.enabled=false` evita que los jobs se ejecuten automaticamente al iniciar la aplicacion. Para ejecutar un job desde consola, se debe habilitar explicitamente en los argumentos.

## Requisitos

- JDK 21
- Docker Desktop o Docker Engine
- PowerShell, CMD o terminal compatible

## Ejecucion

### 1. Levantar PostgreSQL

```powershell
docker compose up -d
```

### 2. Compilar

```powershell
.\mvnw.cmd compile
```

### 3. Ejecutar pruebas

```powershell
.\mvnw.cmd test
```

### 4. Ejecutar la aplicacion como API

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en:

```text
https://localhost:8443
```

Como el certificado es local/autofirmado, en `curl` se usa `-k`.

En PowerShell, para aceptar el certificado local en esta version del entorno, ejecutar primero:

```powershell
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
```

### 5. Ejecutar jobs batch

Para ejecutar un job especifico:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=transaccionJob"
```

Jobs disponibles:

```text
transaccionJob
interesJob
estadoCuentaJob
```

Ejemplos:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=interesJob"
```

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=estadoCuentaJob"
```

Para que las APIs BFF tengan datos completos, se recomienda ejecutar al menos:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=transaccionJob"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=interesJob"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=estadoCuentaJob"
```

## Seguridad

La seguridad esta implementada con Spring Security y JWT.

Endpoint publico:

```http
POST /auth/login
```

Usuarios en memoria:

| Canal | Usuario | Password | Rol |
|---|---|---|---|
| Web | `web` | `web123` | `ROLE_WEB` |
| Movil | `movil` | `movil123` | `ROLE_MOVIL` |
| Cajero | `cajero` | `cajero123` | `ROLE_CAJERO` |

Reglas de autorizacion:

- `/api/bff/web/**` requiere rol `WEB`.
- `/api/bff/movil/**` requiere rol `MOVIL`.
- `/api/bff/cajero/**` requiere rol `CAJERO`.
- El resto de rutas requiere autenticacion.

El token JWT dura 1 hora.

### Obtener token

Antes de usar `Invoke-RestMethod` contra HTTPS local, ejecutar una vez en la sesion de PowerShell:

```powershell
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
```

Web:

```powershell
$login = @{ usuario = "web"; password = "web123" } | ConvertTo-Json
$tokenWeb = (Invoke-RestMethod -Uri "https://localhost:8443/auth/login" -Method POST -ContentType "application/json" -Body $login).token
```

Movil:

```powershell
$login = @{ usuario = "movil"; password = "movil123" } | ConvertTo-Json
$tokenMovil = (Invoke-RestMethod -Uri "https://localhost:8443/auth/login" -Method POST -ContentType "application/json" -Body $login).token
```

Cajero:

```powershell
$login = @{ usuario = "cajero"; password = "cajero123" } | ConvertTo-Json
$tokenCajero = (Invoke-RestMethod -Uri "https://localhost:8443/auth/login" -Method POST -ContentType "application/json" -Body $login).token
```

Respuesta del login:

```json
{
  "usuario": "web",
  "roles": ["ROLE_WEB"],
  "token": "jwt-generado",
  "tipo": "Bearer"
}
```

## APIs BFF

### BFF Web

Ruta base:

```text
/api/bff/web
```

Endpoint:

```http
GET /api/bff/web/resumen
```

Devuelve un resumen general del procesamiento:

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

Prueba con PowerShell:

```powershell
Invoke-RestMethod `
  -Uri "https://localhost:8443/api/bff/web/resumen" `
  -Headers @{ Authorization = "Bearer $tokenWeb" }
```

Prueba con curl:

```powershell
curl.exe -k -H "Authorization: Bearer $tokenWeb" https://localhost:8443/api/bff/web/resumen
```

### BFF Movil

Ruta base:

```text
/api/bff/movil
```

Endpoint:

```http
GET /api/bff/movil/resumen
```

Devuelve una respuesta mas liviana:

```json
{
  "canal": "movil",
  "transacciones": 1000,
  "resumenesDiarios": 338
}
```

Prueba:

```powershell
Invoke-RestMethod `
  -Uri "https://localhost:8443/api/bff/movil/resumen" `
  -Headers @{ Authorization = "Bearer $tokenMovil" }
```

### BFF Cajero

Ruta base:

```text
/api/bff/cajero
```

Endpoints:

```http
GET /api/bff/cajero/saldo/{cuentaId}
POST /api/bff/cajero/retiro/{cuentaId}
```

Consultar saldo:

```powershell
Invoke-RestMethod `
  -Uri "https://localhost:8443/api/bff/cajero/saldo/101" `
  -Headers @{ Authorization = "Bearer $tokenCajero" }
```

Respuesta de ejemplo:

```json
{
  "canal": "cajero",
  "cuentaId": 101,
  "saldoDisponible": 7960.00
}
```

El valor de `saldoDisponible` puede cambiar si se realizan nuevos retiros sobre la misma cuenta.

Realizar retiro:

```powershell
$body = @{ monto = 100 } | ConvertTo-Json

Invoke-RestMethod `
  -Uri "https://localhost:8443/api/bff/cajero/retiro/101" `
  -Method POST `
  -Headers @{ Authorization = "Bearer $tokenCajero" } `
  -ContentType "application/json" `
  -Body $body
```

Respuesta de ejemplo:

```json
{
  "canal": "cajero",
  "cuentaId": 101,
  "montoRetirado": 100,
  "saldoAnterior": 8060.00,
  "saldoDisponible": 7960.00
}
```

Los saldos del ejemplo dependen del estado actual de la base de datos y pueden variar si la prueba se repite.

El retiro:

- Valida que el monto sea mayor a cero.
- Consulta el saldo en `cuentas_intereses`.
- Valida saldo suficiente.
- Actualiza `saldo_final`.
- Registra el movimiento en `retiros_cajero`.

## Consultas utiles

```sql
SELECT * FROM transacciones_procesadas;
SELECT * FROM resumen_diario;
SELECT * FROM cuentas_intereses;
SELECT * FROM movimientos_anuales;
SELECT * FROM resumen_anual;
SELECT * FROM retiros_cajero;
```

Ver ultimos retiros:

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

## Flujo recomendado de prueba

```powershell
docker compose up -d
.\mvnw.cmd test

.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=transaccionJob"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=interesJob"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.enabled=true --spring.batch.job.name=estadoCuentaJob"

.\mvnw.cmd spring-boot:run
```

Luego iniciar sesion en `/auth/login` y consumir los endpoints usando:

```text
Authorization: Bearer <token>
```

## Evidencias y pruebas realizadas

Se validaron las siguientes evidencias sobre el estado actual del proyecto:

- Compilacion Maven ejecutada con `.\mvnw.cmd compile`: resultado `BUILD SUCCESS`.
- PostgreSQL 16 ejecutandose mediante Docker con el contenedor `banco-postgres`.
- Base de datos publicada localmente en `localhost:5433`.
- Aplicacion disponible por HTTPS en el puerto `8443`.
- Login correcto mediante `POST /auth/login` y generacion de JWT Bearer.
- BFF Web probado con usuario `web` y rol `ROLE_WEB`.
- BFF Movil probado con usuario `movil` y rol `ROLE_MOVIL`.
- BFF Cajero probado con usuario `cajero` y rol `ROLE_CAJERO`.
- Consulta de saldo realizada sobre la cuenta `101`.
- Retiro sobre cuenta `101` registrado previamente y persistido en PostgreSQL.
- Acceso a BFF con rol incorrecto bloqueado con `403 Forbidden`.
- Acceso sin token bloqueado por la configuracion de seguridad actual.

Ultimos retiros observados en PostgreSQL durante la revision:

```text
id | cuenta_id | monto  | saldo_anterior | saldo_posterior
---|-----------|--------|----------------|----------------
3  | 101       | 100.00 | 8060.00        | 7960.00
2  | 101       | 100.00 | 8160.00        | 8060.00
```

Nota: en la validacion actual, una llamada sin token a un BFF protegido fue rechazada por seguridad. La respuesta observada para `/api/bff/web/resumen` sin token fue `403 Forbidden`.

## Detener entorno

Detener PostgreSQL:

```powershell
docker compose down
```

Detener PostgreSQL y eliminar el volumen:

```powershell
docker compose down -v
```

## Notas

- El certificado HTTPS incluido es para ejecucion local.
- Las credenciales estan en memoria y son adecuadas solo para demostracion o entorno academico.
- Para produccion se deberian externalizar secretos, cifrar passwords, renovar la clave JWT y usar un mecanismo de identidad robusto.
