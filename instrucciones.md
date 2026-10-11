# Instrucciones para ejecutar y probar Banco XYZ

## 1. Alcance y requisitos

Esta guía permite construir, ejecutar y verificar los 12 servicios del proyecto mediante Docker Compose. Los ejemplos usan Bash en Ubuntu, WSL o Git Bash. En Windows, ejecutar Docker Desktop y habilitar los contenedores Linux. Para desplegar en AWS, seguir [despliegue.md](despliegue.md).

Herramientas necesarias:

- Git, Docker Engine o Docker Desktop, Docker Compose con soporte para `!reset` y `!override`.
- `curl` y `jq` para las pruebas HTTP y la lectura de JSON.
- Acceso a Internet para descargar imágenes y dependencias Maven durante la primera construcción.
- Puertos de la tabla siguiente disponibles. Para dimensionar los recursos, medir memoria y disco durante el arranque; se propone partir con 8 GiB de RAM y 40 GiB de disco para la demostración completa, sin considerar esos valores una garantía de capacidad.

Java y Maven se ejecutan dentro de las imágenes durante la construcción. Solo se necesitan instalados en el equipo si se elige ejecutar las pruebas Maven fuera de Docker.

En Ubuntu, instalar las herramientas de pruebas:

```bash
sudo apt update
sudo apt install -y git curl jq
docker --version
docker compose version
```

## 2. Componentes y direcciones

| Servicio en Compose | Función | Dirección desde el equipo anfitrión |
| --- | --- | --- |
| `postgres` | Persistencia y metadatos batch | `localhost:5433`, PostgreSQL |
| `kafka` | Broker de eventos | `localhost:9092`, protocolo Kafka |
| `keycloak` | Identidad OAuth2 y roles | `https://localhost:8081` |
| `eureka-server` | Registro y descubrimiento | `http://localhost:8761` |
| `config-server` | Configuración centralizada | `http://localhost:8888` |
| `clientes-service` | Clientes y perfiles | `http://localhost:8091` |
| `cuentas-service` | Cuentas, saldos y operaciones | `http://localhost:8092` |
| `pagos-service` | Pagos, depósitos y transferencias | `http://localhost:8093` |
| `bank-batch` | Jobs y reportes | `https://localhost:8443` |
| `bff-web` | Canal web | `https://localhost:8451` |
| `bff-movil` | Canal móvil | `https://localhost:8452` |
| `bff-cajero` | Canal cajero | `https://localhost:8453` |

Estas direcciones son para ejecución local con `PUBLIC_HOST=localhost`. El Compose principal publica Keycloak por HTTPS. Para acceso directo desde fuera de EC2, configurar `PUBLIC_HOST` con su IP pública o dominio y seguir el apartado 10 de [despliegue.md](despliegue.md); no hace falta un Compose adicional. Dentro de Docker, los servicios utilizan la red de Compose y Eureka; PostgreSQL escucha en `postgres:5432` y Kafka en `kafka:29092`.

## 3. Obtener el código y configurar el entorno

```bash
git clone https://github.com/ValeriaAcevedoC/Backendlll.git
cd Backendlll
```

Si ya se dispone del código, entrar en su raíz: la carpeta que contiene `pom.xml`, `Dockerfile.servicio` y los módulos. Antes de clonar para una evaluación, comprobar que la versión publicada incluye los módulos y archivos actuales.

Crear el archivo de variables sin sobrescribir uno existente:

```bash
cp -n bank-batch/.env.example bank-batch/.env
nano bank-batch/.env
```

Completar con valores propios:

```dotenv
POSTGRES_DB=banco
POSTGRES_USER=banco_app
POSTGRES_PASSWORD=REEMPLAZAR_PASSWORD_BD
KEYCLOAK_ADMIN=administrador
KEYCLOAK_ADMIN_PASSWORD=REEMPLAZAR_PASSWORD_ADMIN
SSL_KEYSTORE_PASSWORD=changeit
PUBLIC_HOST=localhost
```

Para EC2, reemplazar `localhost` por la IP pública o el dominio, sin protocolo, puerto ni barras. Compose calcula el hostname de Keycloak y el emisor de todas las API como `https://PUBLIC_HOST:8081/realms/banco`; las antiguas variables `OIDC_BASE_URL` y `OIDC_ISSUER` ya no se utilizan en Compose. `SSL_KEYSTORE_PASSWORD` debe coincidir con la contraseña del certificado PKCS12 incluido; cambiar solamente esta variable impide arrancar los servicios HTTPS. El archivo `.env` contiene secretos y está excluido de Git. Cambiar las variables de PostgreSQL no cambia las credenciales de una base ya inicializada en su volumen.

## 4. Construir y arrancar

Ejecutar desde la raíz del repositorio:

```bash
docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml config --quiet
docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml up -d --build --wait --wait-timeout 900
```

`Dockerfile.servicio` compila los módulos Java y selecciona el JAR según `MODULE`. Config Server y Eureka usan sus propios Dockerfiles. El arranque utiliza comprobaciones de salud y dependencias; la primera construcción puede tardar por las descargas. Este comportamiento de dependencias se describe en la [documentación de Docker Compose](https://docs.docker.com/compose/how-tos/startup-order/).

Para abreviar los siguientes comandos, definir en esta misma terminal Bash:

```bash
dc() {
  docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml "$@"
}
dc ps -a
```

Resultado esperado: 12 servicios en ejecución y sus comprobaciones de salud en estado `healthy`. Un contenedor `running` todavía puede estar inicializándose. Si la espera falla:

```bash
dc logs --tail=100
dc logs --tail=100 cuentas-service
```

La aplicación batch arranca con `spring.batch.job.enabled=false`: los tres jobs se lanzan expresamente mediante la API del apartado 10.

## 5. Verificar infraestructura y salud

### PostgreSQL

```bash
dc exec -T postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) AS clientes FROM clientes; SELECT COUNT(*) AS cuentas FROM cuentas;"'
```

En una base recién creada se cargan cinco clientes (`101` a `105`) y cinco cuentas (`201` a `205`). Las pruebas siguientes crean más registros y modifican saldos, por lo que los conteos cambian después de ejecutarlas.

### Config Server y Eureka

```bash
curl -fsS http://localhost:8888/actuator/health | jq
curl -fsS http://localhost:8888/bank-batch/default | jq
curl -fsS http://localhost:8761/actuator/health | jq
curl -fsS -H 'Accept: application/json' http://localhost:8761/eureka/apps | jq
```

Esperar `status: UP`, una respuesta de configuración con `propertySources` y el registro de clientes, cuentas, pagos, batch y los tres BFF. Puede ser necesario esperar a la siguiente actualización del registro. También se puede abrir `http://localhost:8761` en el navegador.

### Keycloak y servicios HTTP

```bash
curl -kfsS https://localhost:8081/realms/banco/.well-known/openid-configuration | jq '{issuer,token_endpoint}'
for port in 8091 8092 8093; do
  curl -fsS "http://localhost:${port}/actuator/health" | jq
done
for port in 8443 8451 8452 8453; do
  curl -kfsS "https://localhost:${port}/actuator/health" | jq
done
```

Los endpoints de salud no requieren token. `-k` permite probar los certificados de demostración sin validar su confianza; utilizar un certificado confiable y quitar esta opción para un entorno público.

## 6. Obtener tokens OAuth2 por canal

El realm `banco` importa el cliente público `bank-batch-client` y estos usuarios de demostración:

| Usuario | Contraseña de demostración | Rol | Canal permitido |
| --- | --- | --- | --- |
| `web` | `web123` | `WEB` | Web y API batch |
| `movil` | `movil123` | `MOVIL` | Móvil |
| `cajero` | `cajero123` | `CAJERO` | Cajero |

La cuenta administradora definida en `.env` administra Keycloak; no equivale automáticamente a un usuario de la aplicación con rol `ADMIN`. El flujo de contraseña siguiente está habilitado para las pruebas del proyecto.

```bash
obtener_token() {
  curl -kfsS -X POST https://localhost:8081/realms/banco/protocol/openid-connect/token \
    --data-urlencode 'client_id=bank-batch-client' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "username=$1" \
    --data-urlencode "password=$2" | jq -er '.access_token'
}
TOKEN_WEB=$(obtener_token web web123)
TOKEN_MOVIL=$(obtener_token movil movil123)
TOKEN_CAJERO=$(obtener_token cajero cajero123)
RUN_ID=$(date +%s)-${RANDOM}
```

En EC2 con acceso externo, sustituir `localhost` en la URL de tokens por `PUBLIC_HOST`. En Postman usar Body `x-www-form-urlencoded` con los mismos cuatro campos y copiar `access_token` a Authorization > Bearer Token. No se requiere `client_secret`. No compartir ni registrar los tokens. Si una petición empieza a devolver `401` por expiración, obtener nuevos tokens. `RUN_ID` identifica esta sesión y evita reutilizar claves de operaciones anteriores.

## 7. Probar Gestión de Clientes

Consultar el listado y el cliente inicial `101`:

```bash
curl -fsS -H "Authorization: Bearer $TOKEN_WEB" http://localhost:8091/api/clientes | jq
curl -fsS -H "Authorization: Bearer $TOKEN_WEB" http://localhost:8091/api/clientes/101 | jq
```

Crear y actualizar un cliente para esta sesión:

```bash
CLIENTE=$(curl -fsS -X POST http://localhost:8091/api/clientes \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"nombre\":\"Cliente de prueba\",\"rut\":\"$RUN_ID\",\"email\":\"prueba@example.com\",\"perfil\":\"PERSONA\"}")
printf '%s\n' "$CLIENTE" | jq
CLIENTE_ID=$(printf '%s' "$CLIENTE" | jq -er '.id')
curl -fsS -X PATCH "http://localhost:8091/api/clientes/$CLIENTE_ID" \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d '{"nombre":"Cliente actualizado","email":"actualizado@example.com","perfil":"EMPRESA"}' | jq
```

Esperar `201` en la creación y `200` en consultas y actualización. El RUT de prueba es un identificador sintético: la validación actual comprueba longitud y presencia, no su dígito verificador.

## 8. Probar Gestión de Cuentas

```bash
curl -fsS -H "Authorization: Bearer $TOKEN_WEB" http://localhost:8092/api/cuentas | jq
curl -fsS -H "Authorization: Bearer $TOKEN_WEB" http://localhost:8092/api/cuentas/201 | jq
CUENTA=$(curl -fsS -X POST http://localhost:8092/api/cuentas \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"clienteId\":$CLIENTE_ID,\"tipo\":\"ahorro\"}")
printf '%s\n' "$CUENTA" | jq
CUENTA_ID=$(printf '%s' "$CUENTA" | jq -er '.id')
curl -fsS -X PATCH "http://localhost:8092/api/cuentas/$CUENTA_ID" \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d '{"tipo":"corriente"}' | jq
curl -fsS -X POST "http://localhost:8092/api/cuentas/$CUENTA_ID/cierre" \
  -H "Authorization: Bearer $TOKEN_WEB" | jq
```

Esperar cuenta creada con saldo `0`, cambio de tipo y estado final `CERRADA`. El cierre requiere saldo cero. Con un token válido, consultar un ID inexistente devuelve `404`; intentar cerrar la cuenta inicial `201` mientras tenga saldo devuelve `409`.

## 9. Probar Procesamiento de Pagos y los BFF

Estas pruebas usan cuentas iniciales activas. Para evitar depender de su saldo actual, primero se deposita dinero en la cuenta `201`.

### Depósito, pago y transferencia mediante pagos-service

```bash
curl -fsS -X POST http://localhost:8093/api/pagos/depositos \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"deposito-$RUN_ID\",\"tipo\":\"DEPOSITO\",\"cuentaDestino\":201,\"monto\":50000.00}" | jq
curl -fsS -X POST http://localhost:8093/api/pagos \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"pago-$RUN_ID\",\"tipo\":\"PAGO\",\"cuentaOrigen\":201,\"monto\":1000.00,\"referencia\":\"Servicio de prueba\"}" | jq
curl -fsS -X POST http://localhost:8093/api/pagos/transferencias \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"transferencia-$RUN_ID\",\"tipo\":\"TRANSFERENCIA\",\"cuentaOrigen\":201,\"cuentaDestino\":202,\"monto\":2000.00}" | jq
curl -fsS -H "Authorization: Bearer $TOKEN_WEB" \
  'http://localhost:8092/api/cuentas/201/movimientos?pagina=0&limite=20' | jq
```

Esperar `200` y operaciones `CONFIRMADA`. Depósito: el saldo aumenta en `50000`; pago: disminuye en `1000`; transferencia: origen disminuye en `2000` y destino aumenta en `2000`. Revisar los saldos mediante las consultas de cuentas.

### BFF Web

```bash
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8451/api/bff/web/resumen | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8451/api/bff/web/cuentas/201 | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" \
  'https://localhost:8451/api/bff/web/cuentas/201/movimientos?pagina=0&limite=5' | jq
curl -kfsS -X POST https://localhost:8451/api/bff/web/transferencias \
  -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"web-$RUN_ID\",\"tipo\":\"TRANSFERENCIA\",\"cuentaOrigen\":201,\"cuentaDestino\":202,\"monto\":100.00}" | jq
```

El resumen devuelve `canal: web`, clientes y cuentas completas. El canal también expone creación/actualización de clientes, apertura/mantenimiento/cierre de cuentas y pagos/depósitos; las rutas están bajo `/api/bff/web` y utilizan los cuerpos de los apartados anteriores.

### BFF Móvil

```bash
curl -kfsS -H "Authorization: Bearer $TOKEN_MOVIL" https://localhost:8452/api/bff/movil/resumen | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_MOVIL" https://localhost:8452/api/bff/movil/cuentas/201 | jq
curl -kfsS -X POST https://localhost:8452/api/bff/movil/pagos \
  -H "Authorization: Bearer $TOKEN_MOVIL" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"movil-$RUN_ID\",\"tipo\":\"PAGO\",\"cuentaOrigen\":201,\"monto\":100.00,\"referencia\":\"Pago movil de prueba\"}" | jq
```

El resumen contiene identificadores y saldos; el detalle contiene un máximo de cinco movimientos con tipo, monto y fecha. Comparar el JSON con el resumen web para verificar la reducción de campos. También se admite `/api/bff/movil/transferencias`.

### BFF Cajero e idempotencia

```bash
curl -kfsS -H "Authorization: Bearer $TOKEN_CAJERO" https://localhost:8453/api/bff/cajero/saldo/201 | jq
RETIRO_BODY="{\"idempotencia\":\"retiro-$RUN_ID\",\"monto\":10000.00}"
curl -kfsS -X POST https://localhost:8453/api/bff/cajero/retiro/201 \
  -H "Authorization: Bearer $TOKEN_CAJERO" -H 'Content-Type: application/json' \
  -d "$RETIRO_BODY" | jq
curl -kfsS -X POST https://localhost:8453/api/bff/cajero/retiro/201 \
  -H "Authorization: Bearer $TOKEN_CAJERO" -H 'Content-Type: application/json' \
  -d "$RETIRO_BODY" | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_CAJERO" https://localhost:8453/api/bff/cajero/saldo/201 | jq
```

Las dos respuestas del retiro corresponden a la misma operación: el saldo se descuenta una sola vez. Reutilizar la clave con otro monto devuelve `409`. Este canal ofrece consulta de saldo y retiro; cuentas-service rechaza otras operaciones cuando el token solo tiene el rol `CAJERO`.

### Autenticación, autorización y validaciones

```bash
curl -ksS -o /dev/null -w 'Sin token: %{http_code}\n' https://localhost:8451/api/bff/web/resumen
curl -ksS -o /dev/null -w 'Rol movil en web: %{http_code}\n' \
  -H "Authorization: Bearer $TOKEN_MOVIL" https://localhost:8451/api/bff/web/resumen
curl -ksS -o /dev/null -w 'Monto invalido: %{http_code}\n' \
  -X POST https://localhost:8453/api/bff/cajero/retiro/201 \
  -H "Authorization: Bearer $TOKEN_CAJERO" -H 'Content-Type: application/json' \
  -d "{\"idempotencia\":\"invalido-$RUN_ID\",\"monto\":-1}"
```

Esperar respectivamente `401`, `403` y `400`. Un retiro superior al saldo devuelve `409`. La autorización actual distingue roles de canal; no implementa asociación del usuario autenticado con la propiedad de una cuenta.

## 10. Ejecutar y verificar los tres jobs batch

Los archivos de entrada están incluidos en `bank-batch/src/main/resources/data/`. La API actual selecciona estos archivos empaquetados; no acepta un archivo externo en el cuerpo de la solicitud.

| Job | Entrada | Salida consultable |
| --- | --- | --- |
| `transaccionJob` | `transacciones.csv` | `/api/batch/reportes/diario` |
| `interesJob` | `intereses.csv` | `/api/batch/reportes/intereses?periodo=2024-06` |
| `estadoCuentaJob` | `cuentas_anuales.csv` | `/api/batch/reportes/anual?anio=2024` |

Los CSV incluidos contienen fechas de 2024. El período identifica los resultados mensuales de intereses; no filtra los CSV diarios ni anuales. Ejecutar los jobs uno a uno y comprobar su respuesta antes de continuar:

```bash
for JOB in transaccionJob interesJob estadoCuentaJob; do
  RESPUESTA=$(curl -kfsS -X POST "https://localhost:8443/api/batch/jobs/$JOB" \
    -H "Authorization: Bearer $TOKEN_WEB" -H 'Content-Type: application/json' \
    -d "{\"periodo\":\"2024-06\",\"ejecucion\":\"$JOB-$RUN_ID\"}") || break
  printf '%s\n' "$RESPUESTA" | jq
  JOB_ID=$(printf '%s' "$RESPUESTA" | jq -er '.id') || break
  curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" \
    "https://localhost:8443/api/batch/ejecuciones/$JOB_ID" | jq
done
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8443/api/batch/reportes/diario | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" \
  'https://localhost:8443/api/batch/reportes/intereses?periodo=2024-06' | jq
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" \
  'https://localhost:8443/api/batch/reportes/anual?anio=2024' | jq
```

El resultado satisfactorio es `estado: COMPLETED`. Si aparece `STARTED`, repetir la consulta de la ejecución hasta que termine. La respuesta incluye lecturas, escrituras, omisiones y fechas por step. Los resúmenes diarios y anuales se generan después del control de calidad, por lo que un fallo puede dejar datos de detalle escritos sin publicar un nuevo resumen.

Las reglas configuradas toleran hasta 100 omisiones y detienen el flujo si la proporción de omisiones supera el 10 %. Un resultado `FAILED` debe investigarse con los logs y metadatos; no se considera automáticamente un arranque fallido de todo el sistema ni una ejecución satisfactoria del job. Las anomalías marcadas por los procesadores se diferencian de las filas omitidas por errores.

Los intereses válidos utilizan aritmética `BigDecimal`, tasa mensual de `0.01` para ahorro y `0.02` para préstamo. Estos resultados son históricos y no modifican los saldos operativos de cuentas-service. Los escritores usan claves y actualizaciones por conflicto; comparar conteos y totales al reprocesar para verificar que no se dupliquen resultados.

Reenviar una ejecución ya completada con el mismo nombre, período y `ejecucion` devuelve `409`; para una nueva ejecución manual cambiar `ejecucion`. Un job fallido por causas temporales de acceso a datos puede reejecutarse automáticamente cada 30 segundos, hasta tres ejecuciones totales de la instancia. Los fallos de calidad no se reintentan automáticamente. Verificar la recuperación con las pruebas del apartado 13.

Consultar las últimas ejecuciones en PostgreSQL:

```bash
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT job_execution_id, job_instance_id, status, exit_code FROM batch_job_execution ORDER BY job_execution_id DESC LIMIT 15;"'
```

## 11. Probar eventos Kafka y auditoría

Listar y describir los tópicos:

```bash
dc exec -T kafka /opt/bitnami/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list
dc exec -T kafka /opt/bitnami/kafka/bin/kafka-topics.sh \
  --bootstrap-server kafka:29092 --describe --topic banco.operaciones
```

`banco.operaciones` tiene tres particiones y se crea junto con `banco.operaciones.DLT`. Las operaciones de pagos y cajero generan registros en una tabla outbox, que se publica periódicamente; el consumidor almacena auditoría por clave idempotente.

```bash
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT id, publicado, intentos FROM eventos_pendientes ORDER BY fecha DESC LIMIT 10; SELECT id, fecha FROM historial_eventos ORDER BY fecha DESC LIMIT 10;"'
dc exec -T kafka /opt/bitnami/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:29092 --describe --group cuentas-auditoria
```

Esperar algunos segundos después de las operaciones: sus eventos deben pasar a `publicado=true`, aparecer una sola vez en `historial_eventos` y dejar `LAG=0` cuando el grupo se haya puesto al día. Revisar logs si no ocurre.

El servicio batch también dispone de un productor y consumidor de prueba de texto:

```bash
curl -kfsS -X POST -G https://localhost:8443/api/kafka/enviar \
  -H "Authorization: Bearer $TOKEN_WEB" --data-urlencode "mensaje=PruebaKafka-$RUN_ID"
dc logs --since=2m bank-batch
```

Esperar la confirmación HTTP y el mensaje consumido en los logs sobre `banco.transacciones`. Esta prueba de texto es independiente de la auditoría operativa de `banco.operaciones`.

## 12. Probar resiliencia, configuración y escalabilidad

### Configuración y fallback

```bash
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8443/api/debug/config
curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8443/api/debug/clientes | jq
dc stop clientes-service
for intento in 1 2 3 4; do
  curl -kfsS -H "Authorization: Bearer $TOKEN_WEB" https://localhost:8443/api/debug/clientes | jq
done
dc start clientes-service
```

Realizar esta interrupción en el entorno de pruebas. Configuración esperada: `Configuracion centralizada desde Config Server`. Con clientes disponible se obtiene el listado; durante el fallo, el endpoint de depuración devuelve un JSON con `error` y mantiene HTTP `200`. En cambio, las llamadas operativas de los BFF devuelven `503` si no pueden acceder al servicio. Después de recuperar clientes, esperar su salud, registro en Eureka y transición del circuito antes de repetir la consulta.

### Varias instancias

```bash
docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml -f bank-batch/docker-compose.escala.yml \
  up -d --scale cuentas-service=2 --scale clientes-service=2 --scale pagos-service=2
docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml -f bank-batch/docker-compose.escala.yml ps -a
```

El archivo de escala elimina los puertos publicados de estos tres servicios para evitar conflictos. Verificar dos instancias por servicio en Eureka y consultar los BFF para comprobar el acceso por descubrimiento y balanceo. Las llamadas directas a `localhost:8091`, `8092` y `8093` dejan de estar disponibles en este modo. La existencia de dos registros demuestra registro y acceso; para medir el reparto por instancia hacen falta logs o métricas por instancia.

Volver al despliegue normal:

```bash
docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml -f bank-batch/docker-compose.escala.yml \
  up -d --scale cuentas-service=1 --scale clientes-service=1 --scale pagos-service=1
dc up -d
```

## 13. Ejecutar pruebas automatizadas

Desde la raíz, con Java 21 y Maven instalados:

```bash
java -version
mvn -version
mvn -B -ntp test
```

Alternativa con Maven y Java dentro de Docker, desde Bash:

```bash
docker run --rm -v "$(pwd):/workspace" -w /workspace \
  maven:3.9-eclipse-temurin-21 mvn -B -ntp test
```

En Git Bash, si la conversión automática de rutas altera el montaje, usar WSL o la alternativa PowerShell:

```powershell
docker run --rm --mount "type=bind,source=$($PWD.Path),target=/workspace" -w /workspace maven:3.9-eclipse-temurin-21 mvn -B -ntp test
```

Esperar `BUILD SUCCESS`, sin fallos ni errores. Las pruebas cubren seguridad por canal, validación, operaciones e idempotencia, concurrencia de retiros, procesadores y recuperación batch mediante un fallo temporal simulado. Los reportes se escriben en `target/surefire-reports` de cada módulo. Los tests usan H2 y servicios simulados: complementarlos con las pruebas de integración manuales contra PostgreSQL, Keycloak y Kafka descritas aquí. La construcción de imágenes utiliza `-DskipTests` y no sustituye este paso.

Existe un Compose alternativo para aislar las pruebas manuales en otro proyecto y volumen:

```bash
docker compose -p eft-pruebas --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml -f bank-batch/docker-compose.pruebas.yml \
  up -d --build --wait --wait-timeout 900
```

En este entorno, los puertos son PostgreSQL `15433`, Kafka `19092`, Keycloak `18081`, Eureka `18761`, Config `18888`, clientes `18091`, cuentas `18092`, pagos `18093`, batch `18443` y BFF `18451`/`18452`/`18453`. Adaptar las URL y obtener tokens en `http://localhost:18081`; el emisor también cambia. La función `dc` anterior sigue apuntando al entorno normal: para este entorno repetir el prefijo completo con `-p eft-pruebas` y ambos archivos.

## 14. Detener, actualizar y diagnosticar

```bash
dc logs --tail=100 bff-web
dc restart bff-web
dc stop
dc start
```

Para retirar los contenedores y la red manteniendo el volumen de PostgreSQL:

```bash
dc down
```

Después de cambiar el código, reconstruir con `dc up -d --build`. Los CSV se incluyen en la imagen y sus cambios requieren reconstrucción. No añadir `--volumes` a `down` para una parada habitual: esa opción elimina también la base persistida.

| Problema | Comprobación y solución |
| --- | --- |
| Variables vacías o autenticación PostgreSQL fallida | Revisar `.env` y el uso de `--env-file`; una base existente conserva sus credenciales iniciales. |
| Puerto ocupado | Detener el proceso que lo usa o ajustar el puerto publicado; mantener coherentes las URL OIDC. |
| `401` | Obtener un token vigente del realm correcto y enviar `Authorization: Bearer`. |
| `403` | Usar el usuario del canal correspondiente; móvil y cajero no acceden al BFF web. |
| `503` | Revisar salud, logs y registro de la dependencia en Eureka; esperar recuperación del circuito. |
| `FAILED` en batch | Revisar omisiones, excepciones, metadatos y estado de la base; distinguir fallo temporal y calidad. |
| Error de certificado | `-k` solo en las pruebas de demostración; revisar contraseña y archivo PKCS12. |
| Error al compilar o archivo ausente | Verificar la versión del repositorio, todos los módulos y los certificados de los BFF. |
| Kafka sin auditoría | Revisar outbox, salud del broker, grupo consumidor y tópicos; repetir la consulta después del intervalo de publicación. |

Conservar para la evaluación: estado de los 12 servicios, registro Eureka, configuración centralizada, respuestas por canal, códigos de rechazo, estados y conteos de los tres jobs, saldos antes/después, auditoría Kafka y resultado real de las pruebas. Evitar incluir contraseñas y tokens en las capturas.
