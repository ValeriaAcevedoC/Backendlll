# Bank Batch

Proyecto Java con Spring Boot y Spring Batch para procesar archivos CSV bancarios, validar datos, normalizar informacion y guardar los resultados en PostgreSQL.

La aplicacion contiene tres jobs batch:

- `transaccionJob`: procesa transacciones bancarias.
- `interesJob`: calcula intereses por cuenta.
- `estadoCuentaJob`: procesa movimientos anuales y genera un resumen por cuenta.

## Tecnologias

- Java 21
- Spring Boot 4.1.0
- Spring Batch
- Spring JDBC
- PostgreSQL 16
- Maven Wrapper
- Docker Compose

## Requisitos

- JDK 21
- Docker Desktop o Docker Engine
- PowerShell, CMD o terminal compatible

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
    |   |   |-- config
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

## Comandos principales

Levantar PostgreSQL:

```powershell
docker compose up -d
```

Compilar:

```powershell
.\mvnw.cmd compile
```

Ejecutar pruebas:

```powershell
.\mvnw.cmd test
```

Ejecutar la aplicacion:

```powershell
.\mvnw.cmd spring-boot:run
```

Ejecutar un job especifico:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionJob"
```

Jobs disponibles:

- `transaccionJob`
- `interesJob`
- `estadoCuentaJob`

Detener PostgreSQL:

```powershell
docker compose down
```

Detener PostgreSQL y borrar el volumen:

```powershell
docker compose down -v
```

## Archivos de entrada

Los archivos de prueba estan en `src/main/resources/data`.

Cada archivo contiene 1000 registros de datos mas una cabecera:

- `transacciones.csv`
- `intereses.csv`
- `cuentas_anuales.csv`

La data incluye casos validos e invalidos para probar validaciones, skips, normalizacion y tolerancia a fallos.

## transaccionJob

Archivo de entrada:

```text
src/main/resources/data/transacciones.csv
```

Campos:

- `id`
- `fecha`
- `monto`
- `tipo`

Procesador:

```text
TransaccionProcessor
```

Reglas aplicadas:

- Valida fechas nulas, vacias o invalidas.
- Acepta y normaliza fechas en formatos `yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy` y `yyyy/MM/dd`.
- Normaliza la fecha final al formato ISO `yyyy-MM-dd`.
- Marca anomalia cuando el monto es nulo, negativo o igual a cero.
- Normaliza texto quitando acentos, espacios extra y convirtiendo a minusculas.
- Valida que el tipo de transaccion sea `debito` o `credito`.

Tabla de salida:

```text
transacciones_procesadas
```

Si el `id` ya existe, el registro se actualiza con `ON CONFLICT`.

## interesJob

Archivo de entrada:

```text
src/main/resources/data/intereses.csv
```

Campos:

- `cuentaId`
- `nombre`
- `saldo`
- `edad`
- `tipo`

Procesador:

```text
InteresProcessor
```

Reglas aplicadas:

- Valida nombre nulo o vacio y limpia espacios extra.
- Valida edad nula o fuera de rango: menor o igual a 0, o mayor a 120.
- Valida saldo nulo.
- Normaliza el tipo de cuenta quitando acentos, espacios extra y convirtiendo a minusculas.
- Procesa cuentas de tipo `ahorro` con tasa de interes de `1%`.
- Procesa cuentas de tipo `prestamo` con tasa de interes de `2%`.
- Marca como no validas las cuentas con tipo no procesable.
- Calcula interes con redondeo a 2 decimales.
- Calcula saldo final sumando saldo e interes.
- Registra observaciones cuando existen datos incompletos o inconsistentes.

Tabla de salida:

```text
cuentas_intereses
```

Si `cuenta_id` ya existe, el registro se actualiza con `ON CONFLICT`.

## estadoCuentaJob

Archivo de entrada:

```text
src/main/resources/data/cuentas_anuales.csv
```

Campos:

- `cuentaId`
- `fecha`
- `transaccion`
- `monto`
- `descripcion`

Procesador:

```text
MovimientoAnualProcessor
```

Reglas aplicadas:

- Valida fechas nulas, vacias o invalidas.
- Acepta y normaliza fechas en formatos `yyyy-MM-dd`, `dd-MM-yyyy`, `dd/MM/yyyy` y `yyyy/MM/dd`.
- Normaliza la fecha final al formato ISO `yyyy-MM-dd`.
- Lanza error saltable cuando el monto es nulo.
- Marca anomalia cuando el monto es igual a cero.
- Normaliza el tipo de movimiento quitando acentos, espacios extra y convirtiendo a minusculas.
- Valida que el tipo de movimiento sea `deposito`, `retiro` o `compra`.
- Valida descripcion nula o vacia y limpia espacios extra.

Tabla de salida de movimientos:

```text
movimientos_anuales
```

Si la calidad de datos es aceptable, ejecuta `resumenAnualStep` y genera la tabla:

```text
resumen_anual
```

El resumen anual calcula:

- Total de movimientos.
- Total de ingresos.
- Total de egresos.
- Saldo anual.
- Total de anomalias.

## Tolerancia a fallos

Los steps principales usan `faultTolerant()`:

- `transaccionStep`
- `interesStep`
- `movimientoAnualStep`

Cada step procesa chunks de 5 registros.

## Politica de skips

La clase `CustomSkipPolicy` define los errores que se pueden omitir sin detener inmediatamente el step.

Excepciones saltables:

- `FlatFileParseException`
- `NumberFormatException`
- `DateTimeParseException`
- `IllegalArgumentException`

El limite actual de omisiones es:

```java
private static final int LIMITE_SKIPS = 100;
```

Este limite se definio porque la data oficial contiene aproximadamente 1000 registros. De esa forma, el proceso tolera cerca de un 10% de registros problematicos y mantiene coherencia con `DataQualityDecider`.

Si `skipCount` supera el limite, se lanza `SkipLimitExceededException` y el step falla.

## Politica de reintentos

Los steps principales tienen reintentos para errores transitorios de base de datos:

```java
.retry(TransientDataAccessException.class)
.retryLimit(3)
```

Esto permite reintentar operaciones cuando ocurre una falla temporal de conexion o acceso a datos.

## Procesamiento multihilo

La clase `BatchConfig` configura el `ThreadPoolTaskExecutor`.

Valores por defecto:

- `batch.threads.core`: `3`
- `batch.threads.max`: `3`
- `batch.queue.capacity`: `25`
- Prefijo de hilos: `Batch-Thread-`

Estos valores se pueden ajustar por propiedades al ejecutar la aplicacion.

Ejemplo con 5 hilos:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.batch.job.name=transaccionJob --batch.threads.core=5 --batch.threads.max=5 --batch.queue.capacity=25"
```

Los readers de archivos fueron envueltos con `SynchronizedItemStreamReader` para hacer mas seguro el procesamiento paralelo con `FlatFileItemReader`.

Readers sincronizados:

- `transaccionReader`
- `interesReader`
- `movimientoAnualReader`

## Pruebas de rendimiento

Se realizaron pruebas de rendimiento sobre `transaccionJob` utilizando el conjunto de datos oficial y configuraciones de 1, 3 y 5 hilos.

Condiciones de prueba:

- Mismo archivo de entrada con 1000 registros.
- Chunk de 5 registros.
- Cola de 25 tareas.
- Tres ejecuciones por configuracion.
- Mismo entorno de ejecucion y base de datos.

| Configuracion | Prueba 1 | Prueba 2 | Prueba 3 | Promedio |
|---|---:|---:|---:|---:|
| 1 hilo | 852 ms | 906 ms | 900 ms | 886 ms |
| 3 hilos | 833 ms | 954 ms | 807 ms | 865 ms |
| 5 hilos | 1565 ms | 832 ms | 784 ms | 1060 ms |

La configuracion de 3 hilos obtuvo el mejor tiempo promedio, con aproximadamente 865 ms.

En comparacion con la configuracion de 1 hilo, cuyo promedio fue de 886 ms, el uso de 3 hilos produjo una reduccion aproximada del 2,4% en el tiempo promedio de ejecucion.

La configuracion de 5 hilos alcanzo un promedio aproximado de 1060 ms y presento una mayor variacion entre ejecuciones. Esto demuestra que aumentar la cantidad de hilos no necesariamente produce una mejora de rendimiento, debido a los costos asociados a la concurrencia y a las operaciones de persistencia.

Por estos resultados, se selecciona la configuracion de 3 hilos como la alternativa mas adecuada entre las configuraciones evaluadas.

## Control de calidad de datos

La clase `DataQualityDecider` agrega una decision explicita al finalizar los steps principales.

Regla:

- Si mas del 10% de los registros evaluados fueron omitidos, el job falla con estado `CALIDAD_INSUFICIENTE`.
- Si el porcentaje de omisiones esta dentro del umbral permitido, el job finaliza normalmente.

En `estadoCuentaJob`, `resumenAnualStep` solo se ejecuta cuando la calidad de datos es aceptable.

## Base de datos

El archivo `src/main/resources/schema.sql` crea las tablas:

- `transacciones_procesadas`
- `cuentas_intereses`
- `movimientos_anuales`
- `resumen_anual`

## Consultas utiles

```sql
SELECT * FROM transacciones_procesadas;
SELECT * FROM cuentas_intereses;
SELECT * FROM movimientos_anuales;
SELECT * FROM resumen_anual;
```

## Verificacion

Para validar el proyecto:

```powershell
docker compose up -d
.\mvnw.cmd compile
.\mvnw.cmd test
```

Resultado esperado:

```text
BUILD SUCCESS
```
