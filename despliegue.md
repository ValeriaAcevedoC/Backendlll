# Despliegue de Banco XYZ en AWS EC2 con Ubuntu

## 1. Alcance del despliegue

Esta guía instala los 12 servicios del proyecto en una instancia EC2 con Ubuntu y Docker Compose. El Compose principal permite obtener tokens desde fuera de EC2 por HTTPS, configurando `PUBLIC_HOST` con la IP pública o el dominio. SSH se utiliza para administración y, opcionalmente, para acceder a los servicios internos mediante túneles.

La guía describe pasos para ejecutar; no constituye evidencia de que el despliegue ya se haya realizado. Registrar los resultados reales de los comandos y de [instrucciones.md](instrucciones.md) al preparar la entrega.

La solución usa una sola máquina, un broker Kafka y una base PostgreSQL compartida por los módulos. Ofrece contenedores y pruebas de varias instancias, pero este despliegue no proporciona alta disponibilidad frente a la pérdida de EC2. El apartado 10 permite probar directamente por IP pública con HTTPS; el apartado 11 identifica los cambios adicionales para producción.

## 2. Crear o preparar la instancia

Si ya existe una EC2 Ubuntu, comprobar sus recursos y continuar en el apartado 3. Para crear una nueva desde la consola AWS:

1. Seleccionar una región y entrar en EC2 > Instances > Launch instances.
2. Elegir una AMI oficial de Ubuntu Server 24.04 LTS de 64 bits, arquitectura `x86_64` para esta guía.
3. Como punto de partida para construir y ejecutar todos los contenedores, seleccionar recursos equivalentes a 2 vCPU y 8 GiB de RAM, y un volumen EBS de 40 GiB. Es una estimación de demostración que debe ajustarse tras medir memoria, disco y tiempos; comprobar el precio y las condiciones de la cuenta antes de crear recursos.
4. Crear o elegir un par de claves SSH y guardar el `.pem` en el equipo local.
5. Para conexión SSH directa, utilizar una subred con ruta a un Internet Gateway y una dirección IPv4 pública. La instancia necesita salida a Internet para descargar paquetes, imágenes y dependencias.
6. Configurar un Security Group con entrada TCP `22` desde la dirección pública del operador, por ejemplo `MI_IP/32`. Para obtener tokens sin túnel, permitir también TCP `8081` desde el equipo cliente; para probar los BFF y batch, permitir `8451`, `8452`, `8453` y `8443` desde ese mismo origen. Mantener privados los demás puertos. Si cambia la IP del operador, actualizar las reglas.
7. Lanzar la instancia y esperar a que sus comprobaciones de estado finalicen correctamente.

Los Security Groups controlan el tráfico permitido a la instancia; referencia: [documentación de Amazon EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html). El acceso externo a tokens requiere el puerto `8081`; PostgreSQL, Kafka, Eureka, Config Server y los servicios de dominio no necesitan reglas de entrada pública.

## 3. Conectarse por SSH

Desde el equipo local, sustituir la ruta de la clave y `IP_PUBLICA_EC2`:

```bash
chmod 400 banco-xyz.pem
ssh -i banco-xyz.pem ubuntu@IP_PUBLICA_EC2
```

En PowerShell, OpenSSH permite la conexión con:

```powershell
ssh -i "C:\ruta\banco-xyz.pem" ubuntu@IP_PUBLICA_EC2
```

Si OpenSSH rechaza la clave por permisos demasiado amplios en Windows, restringir sus permisos al usuario propietario según la [guía de conexión SSH de AWS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/connect-linux-inst-ssh.html). El nombre de usuario de las AMI Ubuntu es `ubuntu`.

Ya dentro de EC2, comprobar el sistema y los recursos:

```bash
cat /etc/os-release
uname -m
free -h
df -h /
```

## 4. Instalar Docker Engine, Compose y herramientas

Ejecutar dentro de EC2. Para una instalación nueva de Ubuntu, configurar el repositorio y los paquetes de Docker según su [guía oficial](https://docs.docker.com/engine/install/ubuntu/):

```bash
sudo apt update
sudo apt install -y ca-certificates curl git jq
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker run --rm hello-world
sudo docker compose version
```

Esperar `Hello from Docker!`. Si existen paquetes Docker de otra distribución, revisar la sección de conflictos de la guía oficial antes de instalar. Los comandos siguientes usan `sudo docker`, por lo que no es necesario añadir el usuario al grupo Docker. No se requiere instalar Java ni Maven en EC2 para construir las imágenes.

## 5. Obtener el proyecto y configurar las variables

```bash
git clone https://github.com/ValeriaAcevedoC/Backendlll.git
cd Backendlll
git rev-parse HEAD
cp -n bank-batch/.env.example bank-batch/.env
chmod 600 bank-batch/.env
nano bank-batch/.env
```

Si el repositorio requiere autenticación, utilizar el acceso autorizado de GitHub sin incorporar tokens en archivos de entrega. Si ya se tiene el código, entrar en la raíz y conservar los cambios locales al actualizar. Registrar el commit desplegado para relacionar la evidencia con la versión evaluada.

Completar `.env`:

```dotenv
POSTGRES_DB=banco
POSTGRES_USER=banco_app
POSTGRES_PASSWORD=REEMPLAZAR_PASSWORD_BD
KEYCLOAK_ADMIN=administrador
KEYCLOAK_ADMIN_PASSWORD=REEMPLAZAR_PASSWORD_ADMIN
SSL_KEYSTORE_PASSWORD=changeit
PUBLIC_HOST=IP_PUBLICA_EC2
```

Elegir contraseñas propias para PostgreSQL y la administración de Keycloak. La contraseña del PKCS12 debe coincidir con el certificado incluido: cambiar `SSL_KEYSTORE_PASSWORD` exige reemplazar o modificar el almacén de claves correspondiente.

Reemplazar `IP_PUBLICA_EC2` por la IP pública o el dominio real, sin protocolo, puerto ni barras. Keycloak y las siete API calculan el mismo emisor `https://PUBLIC_HOST:8081/realms/banco`. Las antiguas variables `OIDC_BASE_URL` y `OIDC_ISSUER` ya no se utilizan en Compose. Para ejecución local se puede usar `PUBLIC_HOST=localhost`. En Docker, la descarga de claves usa la URL interna `http://keycloak:8080/realms/banco/protocol/openid-connect/certs`, definida por Compose; ese puerto HTTP interno no se publica en el host.

## 6. Comprobar archivos y construir todos los servicios

Verificar que la versión del repositorio contiene los módulos actuales, los Dockerfiles y los certificados:

```bash
test -f Dockerfile.servicio
test -f bank-batch/docker-compose.yml
test -f bank-batch/keycloak/realm-banco.json
for modulo in bank-batch bff-web bff-movil bff-cajero; do
  test -f "$modulo/src/main/resources/bank-batch.p12" || {
    printf 'Falta el certificado de %s\n' "$modulo"
    exit 1
  }
done
sudo docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml config --quiet
sudo docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml config --services
sudo docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml \
  up -d --build --wait --wait-timeout 900
```

El listado debe contener los 12 servicios. La construcción usa `Dockerfile.servicio` para clientes, cuentas, pagos, batch y los BFF; Config Server y Eureka utilizan Dockerfiles propios. `--wait` comprueba el estado de salud, aunque la primera descarga o construcción puede requerir ampliar el tiempo. La construcción omite tests; ejecutarlos siguiendo el apartado 13 de `instrucciones.md`.

Definir un atajo para los siguientes comandos dentro de EC2, desde la raíz:

```bash
dc() {
  sudo docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml "$@"
}
dc ps -a
```

Si un servicio no arranca:

```bash
dc logs --tail=100
dc logs --tail=100 bank-batch
sudo docker stats --no-stream
free -h
df -h /
```

No considerar exitoso el despliegue hasta que los 12 servicios estén saludables y las pruebas funcionales sean satisfactorias. La aplicación batch debe estar disponible, pero sus jobs todavía no se ejecutan por el mero arranque.

## 7. Verificar desde EC2 y acceder desde el equipo local

### Comprobación desde la instancia

```bash
curl -kfsS https://localhost:8081/realms/banco/.well-known/openid-configuration | jq '{issuer,token_endpoint}'
curl -fsS http://localhost:8888/actuator/health | jq
curl -fsS http://localhost:8761/actuator/health | jq
for port in 8091 8092 8093; do
  curl -fsS "http://localhost:${port}/actuator/health" | jq
done
for port in 8443 8451 8452 8453; do
  curl -kfsS "https://localhost:${port}/actuator/health" | jq
done
```

### Abrir un túnel SSH opcional

Para obtener tokens y llamar a los BFF desde fuera de EC2, utilizar directamente las URL públicas del apartado 10. El túnel siguiente permite además diagnosticar los servicios internos. Para usar todas las URL locales de `instrucciones.md` mediante este túnel, configurar previamente `PUBLIC_HOST=localhost` y reaplicar Compose; ese ajuste cambia el emisor y exige obtener tokens nuevos.

En una terminal distinta del equipo local, detener previamente cualquier despliegue local que utilice esos mismos puertos. Ejecutar y mantener abierta esta conexión:

```bash
ssh -i banco-xyz.pem -N -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:8081:127.0.0.1:8081 \
  -L 127.0.0.1:8761:127.0.0.1:8761 \
  -L 127.0.0.1:8888:127.0.0.1:8888 \
  -L 127.0.0.1:8091:127.0.0.1:8091 \
  -L 127.0.0.1:8092:127.0.0.1:8092 \
  -L 127.0.0.1:8093:127.0.0.1:8093 \
  -L 127.0.0.1:8443:127.0.0.1:8443 \
  -L 127.0.0.1:8451:127.0.0.1:8451 \
  -L 127.0.0.1:8452:127.0.0.1:8452 \
  -L 127.0.0.1:8453:127.0.0.1:8453 \
  ubuntu@IP_PUBLICA_EC2
```

En PowerShell, el mismo comando en una sola línea:

```powershell
ssh -i "C:\ruta\banco-xyz.pem" -N -o ExitOnForwardFailure=yes -L 127.0.0.1:8081:127.0.0.1:8081 -L 127.0.0.1:8761:127.0.0.1:8761 -L 127.0.0.1:8888:127.0.0.1:8888 -L 127.0.0.1:8091:127.0.0.1:8091 -L 127.0.0.1:8092:127.0.0.1:8092 -L 127.0.0.1:8093:127.0.0.1:8093 -L 127.0.0.1:8443:127.0.0.1:8443 -L 127.0.0.1:8451:127.0.0.1:8451 -L 127.0.0.1:8452:127.0.0.1:8452 -L 127.0.0.1:8453:127.0.0.1:8453 ubuntu@IP_PUBLICA_EC2
```

Con `PUBLIC_HOST=localhost`, las URL de `instrucciones.md` apuntan a EC2 a través de la conexión cifrada. Abrir Eureka en `http://localhost:8761` y Keycloak en `https://localhost:8081`. Los certificados HTTPS de demostración requieren una excepción local o `curl -k` en las pruebas. No se necesitan túneles para PostgreSQL ni Kafka: sus diagnósticos se ejecutan por `docker compose exec` dentro de EC2.

## 8. Ejecutar las pruebas de aceptación y guardar evidencia

Para el acceso externo, obtener los tokens y probar los BFF y batch según el apartado 10. Las pruebas de los servicios de dominio de `instrucciones.md` se ejecutan desde EC2 o con el túnel opcional del apartado 7. Con `PUBLIC_HOST=localhost` y el túnel abierto, se pueden ejecutar directamente los apartados 6 a 10 de `instrucciones.md`: obtener los tres tokens, gestionar clientes/cuentas, hacer depósitos/pagos/transferencias/retiros, verificar idempotencia y ejecutar los jobs.

Los comandos `dc exec`, logs, comprobaciones Kafka, escalado y backups se ejecutan en la terminal SSH de EC2, usando la función `dc` de esta guía. Las variables como `TOKEN_WEB` y `RUN_ID` pertenecen a cada terminal: volver a definirlas si se hacen pruebas HTTP desde EC2.

| Comprobación | Resultado de aceptación |
| --- | --- |
| Contenedores | Los 12 servicios saludables. |
| Config Server y Eureka | Configuración central y registro de los siete servicios de aplicación. |
| OAuth2 | Tokens por canal; acceso sin token `401` y con rol incorrecto `403`. |
| Clientes/cuentas | Consultas, creación, actualización y cierre de cuenta sin saldo. |
| Pagos/cajero | Operaciones confirmadas, saldos coherentes y repetición idempotente. |
| Batch | Tres jobs terminados y reportes consultables; documentar cualquier fallo de calidad. |
| Kafka | Eventos publicados, auditoría sin duplicados y grupo consumidor al día. |
| Resiliencia | Respuestas de fallback al detener una dependencia y recuperación posterior. |

Capturar el identificador de instancia, commit desplegado, estado de Compose, Eureka, respuestas de API y reportes. Los logs pueden contener datos personales: seleccionar evidencia pertinente y ocultar tokens, credenciales y la clave privada.

## 9. Persistencia, respaldo y operación

### PostgreSQL

Compose monta el volumen `banco_data` en `/var/lib/postgresql/data`; el nombre efectivo incluye el proyecto Compose. Verificar el montaje:

```bash
sudo docker inspect banco-postgres --format '{{json .Mounts}}' | jq
```

Crear un respaldo lógico, desde la raíz del repositorio en EC2:

```bash
mkdir -p respaldos
BACKUP="respaldos/banco-$(date +%Y%m%d-%H%M%S).sql"
dc exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB"' > "$BACKUP"
chmod 600 "$BACKUP"
test -s "$BACKUP"
```

Copiar el respaldo fuera de EC2 para que sobreviva a la pérdida del disco. Una restauración debe probarse en una base aislada; este comando crea una nueva base de verificación con nombre único y carga el respaldo, sin reemplazar la base operativa:

```bash
RESTORE_DB="banco_restore_$(date +%Y%m%d%H%M%S)"
dc exec -T postgres sh -c 'createdb -U "$POSTGRES_USER" "$1"' sh "$RESTORE_DB"
dc exec -T postgres sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$1"' sh "$RESTORE_DB" < "$BACKUP"
dc exec -T postgres sh -c 'psql -U "$POSTGRES_USER" -d "$1" -c "SELECT COUNT(*) FROM cuentas;"' sh "$RESTORE_DB"
```

La base de verificación consume espacio adicional. Conservarla o retirarla después de la revisión según la política del entorno.

### Reinicios y actualizaciones

```bash
dc stop
dc start
dc logs --tail=100
```

El Compose actual no define políticas de reinicio automático para los contenedores. Después de un reinicio de EC2, entrar por SSH, ubicarse en el repositorio y ejecutar `dc up -d --wait --wait-timeout 900`. Activar Docker con systemd no garantiza por sí solo el arranque de estos contenedores.

Para actualizar, respaldar primero, revisar los cambios locales con `git status`, obtener la versión autorizada del repositorio y ejecutar `dc up -d --build --wait --wait-timeout 900`. Registrar el nuevo commit y repetir las pruebas. Cambios en archivos empaquetados, incluidos CSV y configuración del Config Server, requieren reconstrucción.

Para retirar contenedores manteniendo PostgreSQL:

```bash
dc down
```

No utilizar `down --volumes` como parada normal. Kafka y Keycloak no tienen volúmenes de persistencia configurados en este Compose: recrear sus contenedores puede perder mensajes, estado y cambios de administración; Keycloak vuelve a importar el realm de demostración. La consistencia outbox no recupera por sí sola mensajes ya marcados publicados si se pierde el almacenamiento del broker.

### Escala en la misma instancia

```bash
sudo docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml -f bank-batch/docker-compose.escala.yml \
  up -d --scale cuentas-service=2 --scale clientes-service=2 --scale pagos-service=2
```

Comprobar las dos instancias en Eureka y usar los BFF; el archivo de escala elimina los puertos directos de clientes, cuentas y pagos. Este procedimiento escala contenedores dentro de la misma EC2 y no crea más instancias AWS ni una base o broker redundantes. Volver al modo normal siguiendo el apartado 12 de `instrucciones.md`.

## 10. Acceso directo por IP pública, sin túnel SSH

El Compose principal ya incluye el acceso externo: Keycloak sirve HTTPS en el puerto público `8081`; dentro de su contenedor utiliza `8443`. Reutiliza el PKCS12 de demostración de batch, mantiene el HTTP interno para descargar claves JWT y ajusta el emisor esperado por los siete servicios de aplicación. No se necesita `docker-compose.publico.yml`; se conserva para compatibilidad con comandos anteriores. Configurar un keystore HTTPS está documentado en la [guía TLS de Keycloak](https://www.keycloak.org/server/enabletls).

El certificado incluido no es de confianza pública ni corresponde necesariamente a la IP. Esta alternativa es para pruebas con `curl -k` o Postman; para un servicio público estable, sustituirlo por un certificado propio confiable y aplicar el apartado 11. No deshabilita la exigencia de HTTPS del realm.

### Preparar archivos y variables en EC2

Actualizar `bank-batch/docker-compose.yml` en EC2 con esta versión del proyecto. Desde la raíz del repositorio, editar:

```bash
nano bank-batch/.env
```

Añadir una única línea con la IP pública de EC2, sin protocolo, puerto ni barras:

```dotenv
PUBLIC_HOST=IP_PUBLICA_EC2
```

También se puede usar un dominio que resuelva a la instancia. El emisor se calcula como `https://PUBLIC_HOST:8081/realms/banco` directamente en el Compose principal. Si cambia la IP pública, actualizar `PUBLIC_HOST`, aplicar Compose de nuevo y obtener tokens nuevos. La configuración de hostname determina las URL públicas de Keycloak; referencia: [hostname de Keycloak](https://www.keycloak.org/server/hostname).

### Permitir los puertos necesarios

En el Security Group agregar reglas TCP desde `My IP` para `8081` (Keycloak HTTPS), `8451` (BFF web), `8452` (móvil), `8453` (cajero) y `8443` (batch). Mantener `22` para administración desde la IP autorizada. No hace falta habilitar los puertos de los servicios de dominio, PostgreSQL ni Kafka. Si cambia la IP del operador, actualizar el origen autorizado.

### Aplicar la configuración

Los cambios recrean Keycloak y los servicios cuyo emisor cambia. Keycloak no tiene volumen de datos en este proyecto: si se modificaron usuarios o el realm desde la consola, conservar esos cambios antes de recrearlo; al recrearse vuelve a importar los usuarios de demostración. Los tokens emitidos antes del cambio de emisor deben reemplazarse.

```bash
sudo docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml config --quiet
sudo docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml \
  up -d --wait --wait-timeout 900
sudo docker compose --env-file bank-batch/.env \
  -f bank-batch/docker-compose.yml ps -a
```

Si faltan imágenes construidas o cambió el código, añadir `--build` a `up`. Basta utilizar el Compose principal para operar este modo. Desde EC2 verificar:

```bash
curl -kfsS https://localhost:8081/realms/banco/.well-known/openid-configuration \
  | jq '{issuer,token_endpoint}'
```

Ambos valores deben referenciar la IP o dominio público y HTTPS. El healthcheck existente de Keycloak comprueba conectividad TCP en su puerto de gestión; esta consulta adicional verifica la disponibilidad del realm.

### Obtener un token desde el equipo local

En PowerShell, sustituir la IP y ejecutar sin túnel:

```powershell
$respuesta = curl.exe -k -f -sS -X POST "https://IP_PUBLICA_EC2:8081/realms/banco/protocol/openid-connect/token" --data-urlencode "client_id=bank-batch-client" --data-urlencode "grant_type=password" --data-urlencode "username=web" --data-urlencode "password=web123" | ConvertFrom-Json
if (-not $respuesta.access_token) { throw "No se obtuvo un token; revisar la respuesta de Keycloak" }
$token = $respuesta.access_token
curl.exe -k -f -sS -H "Authorization: Bearer $token" "https://IP_PUBLICA_EC2:8451/api/bff/web/resumen"
```

Esperar un token y un resumen JSON de clientes y cuentas. Para móvil usar `movil`/`movil123` y su puerto `8452`; para cajero usar `cajero`/`cajero123` y `8453`. Batch utiliza el token del usuario web y el puerto `8443`.

En Postman: `POST https://IP_PUBLICA_EC2:8081/realms/banco/protocol/openid-connect/token`, Body `x-www-form-urlencoded`, campos `client_id`, `grant_type`, `username` y `password` con los valores anteriores. Para el certificado de demostración, desactivar la verificación SSL solamente durante estas pruebas y volver a habilitarla después. Copiar `access_token` a Authorization > Bearer Token de las peticiones a los BFF o batch.

Un timeout apunta a conectividad o reglas de entrada; `401` requiere revisar token, expiración y emisor; `403` requiere revisar el rol del canal. Consultar los logs de Keycloak si no responde y comprobar que no haya vuelto a caer por falta de memoria.

### Regresar al acceso local o por túnel

Cambiar `PUBLIC_HOST=localhost` en `bank-batch/.env` y utilizar solamente el Compose principal:

```bash
sudo docker compose --env-file bank-batch/.env -f bank-batch/docker-compose.yml \
  up -d --wait --wait-timeout 900
```

Esto configura Keycloak HTTPS y el emisor `https://localhost:8081/realms/banco` para las pruebas locales o por túnel. Obtener tokens nuevos. Si solo se usaba `8081` públicamente para esta prueba, retirar su regla de entrada del Security Group.

## 11. Cambios necesarios para producción

El acceso externo de demostración ya está incluido en Compose. Para ofrecer un servicio público estable con dominio y certificados confiables, preparar además estos cambios, que no se activan al ejecutar el Compose actual:

1. Publicar los BFF y el proveedor de identidad mediante un proxy o balanceador HTTPS. Añadir DNS y un certificado que corresponda al dominio; limitar las reglas públicas del Security Group a las entradas necesarias, normalmente `443`.
2. Adaptar el hostname público HTTPS de Keycloak y los emisores de las siete API a la URL del proxy, por ejemplo `https://auth.DOMINIO/realms/banco`, en lugar de la dirección directa `https://PUBLIC_HOST:8081/realms/banco`. Coordinar proxy, hostname y encabezados; verificar que el `issuer` publicado y el claim `iss` coincidan con los servicios.
3. Sustituir el modo `start-dev` de Keycloak por una configuración de servidor con base persistente y TLS adecuado. Cambiar usuarios de demostración y utilizar un flujo de autorización apropiado para los clientes reales.
4. Mantener privados PostgreSQL, Kafka, Config Server, Eureka y los microservicios de dominio. El Compose actual publica sus puertos en el host, por lo que la exposición requiere revisar también los mapeos de red.
5. Incorporar persistencia de Kafka y Keycloak, respaldos externos, rotación de secretos, políticas de reinicio y monitoreo. Los BFF usan HTTPS en el proyecto actual, pero las llamadas internas HTTP y el broker PLAINTEXT necesitan una configuración adicional para cifrado interno.
6. Implementar controles de propiedad de cuentas por usuario antes de uso con clientes reales, además de los roles de canal existentes.
7. Para alta disponibilidad, distribuir servicios en más de una máquina o plataforma de contenedores y usar una base y mensajería con redundancia. Validar integridad, rendimiento y comportamiento ante fallos con esa topología.

No sustituir las URL OIDC por una IP pública HTTP esperando que el despliegue quede listo: el realm requiere SSL para accesos externos y el emisor del token debe coincidir exactamente. El túnel SSH del apartado 7 y el modo HTTPS de demostración del apartado 10 permiten evaluar el proyecto; producción requiere las modificaciones de este apartado.

## 12. Diagnóstico y cierre de la demostración

| Síntoma | Acción |
| --- | --- |
| SSH no conecta | Verificar estado EC2, usuario `ubuntu`, clave, dirección pública, ruta de la subred y origen autorizado en el Security Group. |
| Fallo al abrir túnel | Revisar puertos locales ocupados y detener la ejecución local equivalente. |
| Servicio `unhealthy` | Consultar sus logs y las dependencias; comprobar memoria y disco. |
| Build terminado con código `137` | Revisar eventos de falta de memoria y aumentar recursos o reducir concurrencia de construcción tras confirmar la causa. |
| `401` en APIs | Renovar token y verificar coincidencia de emisor y `.env`. |
| Error de HTTPS | Verificar certificado y contraseña; usar `-k` únicamente para la demostración. |
| PostgreSQL rechaza la contraseña nueva | La base persistida conserva credenciales iniciales; aplicar un cambio de contraseña en la base de manera coordinada. |
| Faltan módulos al construir | Publicar la versión completa del código y volver a obtenerla en EC2. |

Al terminar, cerrar el túnel con `Ctrl+C`. Si la instancia solo se utiliza para la evaluación, detenerla desde AWS cuando no se necesite y revisar los recursos que continúan facturándose. Antes de terminar definitivamente la instancia, conservar respaldos y evidencias y comprobar el comportamiento configurado para sus volúmenes.
