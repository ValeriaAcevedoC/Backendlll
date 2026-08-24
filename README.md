# Bank Batch

Proyecto Java con Spring Boot y Spring Batch para procesar archivos CSV bancarios y persistir los resultados en PostgreSQL.

La aplicacion carga datos desde `src/main/resources/data`, aplica reglas de validacion y calculo, y guarda la informacion procesada en tablas de base de datos.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- Spring Batch
- Spring JDBC
- PostgreSQL 16
- Maven Wrapper
- Docker Compose

## Requisitos

- JDK 21 instalado
- Docker Desktop o Docker Engine
- PowerShell, CMD o una terminal compatible

## Estructura del proyecto

```text
.
|-- docker-compose.yml
|-- pom.xml
|-- mvnw / mvnw.cmd
`-- src
    |-- main
    |   |-- java/cl/duoc/bank_batch
    |   |   |-- config
    |   |   |-- model
    |   |   |-- policy
    |   |   `-- processor
    |   `-- resources
    |       |-- application.properties
    |       |-- schema.sql
    |       `-- data
    |           |-- transacciones.csv
    |           |-- intereses.csv
    |           `-- cuentas_anuales.csv
    `-- test
```

## Configuracion

La aplicacion usa PostgreSQL con la siguiente configuracion definida en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/banco
spring.datasource.username=postgres
spring.datasource.password=postgres
```

El archivo `docker-compose.yml` levanta una base de datos PostgreSQL llamada `banco` en el puerto local `5433`.

## Puesta en marcha

1. Levantar PostgreSQL:

```bash
docker compose up -d
```

2. Ejecutar la aplicacion:

```bash
./mvnw spring-boot:run
```

En Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

3. Ejecutar las pruebas:

```bash
./mvnw test
```

En Windows:

```powershell
.\mvnw.cmd test
```

## Jobs disponibles

El proyecto define tres jobs principales de Spring Batch.

### `transaccionJob`

Lee el archivo:

```text
src/main/resources/data/transacciones.csv
```

Procesa transacciones bancarias con los campos:

- `id`
- `fecha`
- `monto`
- `tipo`

Reglas aplicadas:

- Marca como anomalia los montos nulos.
- Marca como anomalia los montos negativos.
- Marca como anomalia los montos iguales a cero.
- Valida que el tipo de transaccion sea `debito` o `credito`.

Guarda los resultados en la tabla:

```text
transacciones_procesadas
```

### `interesJob`

Lee el archivo:

```text
src/main/resources/data/intereses.csv
```

Procesa cuentas para calcular intereses con los campos:

- `cuenta_id`
- `nombre`
- `saldo`
- `edad`
- `tipo`

Reglas aplicadas:

- Para cuentas de tipo `ahorro`, aplica una tasa de interes de `1%`.
- Para cuentas de tipo `prestamo`, aplica una tasa de interes de `2%`.
- Para otros tipos de cuenta, marca el registro como no valido.

Guarda los resultados en la tabla:

```text
cuentas_intereses
```

### `estadoCuentaJob`

Lee el archivo:

```text
src/main/resources/data/cuentas_anuales.csv
```

Procesa movimientos anuales con los campos:

- `cuenta_id`
- `fecha`
- `transaccion`
- `monto`
- `descripcion`

Reglas aplicadas:

- Marca como anomalia los montos nulos.
- Marca como anomalia los montos iguales a cero.
- Valida que el tipo de movimiento sea `deposito`, `retiro` o `compra`.

Guarda los movimientos procesados en:

```text
movimientos_anuales
```

Luego genera un resumen agrupado por cuenta en:

```text
resumen_anual
```

El resumen considera:

- total de movimientos
- total de ingresos
- total de egresos
- saldo anual
- total de anomalias

## Tolerancia a fallos y escalamiento

En esta iteracion se agrego tolerancia a fallos y procesamiento paralelo a los tres steps principales (`transaccionStep`, `interesStep`, `movimientoAnualStep`).

### Politica de omision personalizada (SkipPolicy)

Se implemento la clase `CustomSkipPolicy` (`cl.duoc.bank_batch.policy.CustomSkipPolicy`), que define que excepciones pueden omitirse durante la lectura o el procesamiento sin detener el step completo:

- `FlatFileParseException`: una linea del CSV no calza con el formato esperado.
- `NumberFormatException`: un campo numerico no se puede parsear.

Cada registro omitido se informa por consola. Si se supera el limite de omisiones configurado, el step se detiene lanzando `SkipLimitExceededException`.

Los tres steps se configuran con `.faultTolerant().skipPolicy(customSkipPolicy)` para activar esta politica.

### Escalamiento con TaskExecutor

Se agrego la clase `BatchConfig` (`cl.duoc.bank_batch.config.BatchConfig`), que define un `ThreadPoolTaskExecutor` con un pool fijo de 3 hilos (`Batch-Thread-`).

Los tres steps se configuran con `.taskExecutor(taskExecutor)`, de modo que los chunks (tamaño 5) se procesan en paralelo entre los 3 hilos disponibles.

## Base de datos

El archivo `src/main/resources/schema.sql` crea las tablas principales:

- `transacciones_procesadas`
- `cuentas_intereses`
- `movimientos_anuales`
- `resumen_anual`

## Ejecutar un job especifico

Para ejecutar solo un job, se puede indicar su nombre mediante propiedades de Spring Batch:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionJob"
```

Otros nombres disponibles:

- `interesJob`
- `estadoCuentaJob`

## Consultas utiles

```sql
SELECT * FROM transacciones_procesadas;
SELECT * FROM cuentas_intereses;
SELECT * FROM movimientos_anuales;
SELECT * FROM resumen_anual;
```

## Detener la base de datos

```bash
docker compose down
```

Para eliminar tambien el volumen de datos:

```bash
docker compose down -v
```