# Despliegue en AWS — EC2 + docker-compose (HTTP, primer despliegue)

> **Alcance:** desplegar la pila completa de **parking** (frontend nginx + backend
> Tomcat/WAR + SQL Server 2022, todo en contenedores) sobre **una sola instancia EC2**
> con **docker compose**, accesible por **IP pública fija (Elastic IP)** sobre **HTTP**.
>
> **Decisiones ya tomadas:** EC2 + docker-compose · SQL Server en contenedor · HTTP primero
> (sin TLS). El endurecimiento (HTTPS, RDS, secretos gestionados) está en la §11.
>
> **Fichero de despliegue:** [`docker-compose.app.yml`](../docker-compose.app.yml) en la raíz
> del repo. Publica **un único puerto: 80**. La BD y el backend son internos de la red de compose.
>
> ⚠️ **Antes de dar acceso a usuarios, lee la §8 (Bootstrap del primer administrador):**
> el perfil `docker`/`pro` **no** crea ningún admin. Es un **bloqueante** hasta resolverlo.

Placeholders usados en los comandos (sustitúyelos por tus valores):

| Placeholder | Significado | Ejemplo |
|---|---|---|
| `<REGION>` | Región AWS | `eu-west-1` |
| `<KEY_NAME>` | Nombre del par de claves SSH en EC2 | `parking-key` |
| `<KEY_FILE>` | Ruta local a la clave privada `.pem` | `~/.ssh/parking-key.pem` |
| `<ELASTIC_IP>` | IP pública fija asociada a la instancia | `52.30.11.22` |
| `<INSTANCE_ID>` | ID de la instancia EC2 | `i-0abc123...` |
| `<MI_IP>` | Tu IP pública (para restringir SSH) | `88.20.30.40/32` |
| `<SG_ID>` | ID del Security Group | `sg-0abc...` |

---

## 1. Requisitos previos

- **Cuenta AWS** con permisos para EC2, VPC y Elastic IP.
- **AWS CLI v2** instalado y configurado:
  ```bash
  aws --version                 # aws-cli/2.x
  aws configure                 # access key, secret, región por defecto, formato json
  aws sts get-caller-identity   # verifica que la identidad es correcta
  ```
- **Par de claves SSH** para acceder a la instancia. Si no tienes uno:
  ```bash
  aws ec2 create-key-pair \
    --region <REGION> \
    --key-name <KEY_NAME> \
    --query 'KeyMaterial' --output text > <KEY_FILE>
  chmod 400 <KEY_FILE>
  ```
- **Región** decidida (usa una cercana a los usuarios; p. ej. `eu-west-1` Irlanda).
- **Credenciales SMTP de Ethereal** (usuario/contraseña) para el `.env` del servidor
  (ver §6). En PRO real se sustituyen por el SMTP corporativo.

---

## 2. Elección de instancia

SQL Server 2022 es el componente que marca el mínimo de memoria: Microsoft recomienda
**≥ 2 GB solo para el motor**, y aquí conviven además el backend (JVM/Tomcat) y nginx.

| Tipo | vCPU | RAM | Apto | Nota |
|---|---|---|---|---|
| `t3.small` | 2 | 2 GB | ❌ | **Insuficiente**: SQL Server no arranca de forma estable. |
| `t3.medium` | 2 | 4 GB | ✅ | **Recomendado** para este primer despliegue. |
| `t3.large` | 2 | 8 GB | ✅✅ | Holgura si vas a cargar datos o ampliar. |

- **Disco (EBS gp3):** mínimo **30 GB**. La imagen de SQL Server ocupa ~1.5 GB, más las
  imágenes de backend/frontend, más el volumen de datos de la BD (`parking-app-mssql-data`).
- **Arquitectura:** las imágenes base usadas (`mcr.microsoft.com/mssql/server:2022-latest`,
  `tomcat`, `nginx`, `node`, `maven`) tienen soporte **x86_64**. Usa una AMI e instancia
  **x86_64** (evita `t4g`/Graviton salvo que verifiques todas las imágenes en arm64;
  SQL Server en contenedor **no** publica imagen arm64 oficial).

---

## 3. Security Group

Regla de oro: **solo 22 y 80 entran**. Los puertos 8080 (Tomcat), 1433 (SQL Server) y
5173 (Vite dev) son **internos de compose** y **no deben exponerse**.

**Entrada (inbound):**

| Puerto | Protocolo | Origen | Motivo |
|---|---|---|---|
| 22 | TCP | `<MI_IP>` (idealmente `/32`) | SSH de administración. **No** dejar `0.0.0.0/0`. |
| 80 | TCP | `0.0.0.0/0` | HTTP público (la SPA y la API vía nginx). |

**Salida (outbound):** por defecto AWS permite todo el egress (`0.0.0.0/0`), lo que ya
cubre el **SMTP saliente a Ethereal por el puerto 587**. Si tu VPC restringe el egress
(reglas personalizadas), **añade explícitamente** salida TCP 587 hacia
`smtp.ethereal.email`; de lo contrario el envío de correo fallará.

Crear el SG y las reglas por CLI:

```bash
# Crear el Security Group (usa el VPC por defecto de la región)
SG_ID=$(aws ec2 create-security-group \
  --region <REGION> \
  --group-name parking-sg \
  --description "parking app - HTTP + SSH" \
  --query 'GroupId' --output text)
echo "SG_ID=$SG_ID"

# SSH restringido a tu IP
aws ec2 authorize-security-group-ingress --region <REGION> \
  --group-id "$SG_ID" --protocol tcp --port 22 --cidr <MI_IP>

# HTTP público
aws ec2 authorize-security-group-ingress --region <REGION> \
  --group-id "$SG_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0

# (Solo si tu VPC restringe egress) SMTP saliente para Ethereal
aws ec2 authorize-security-group-egress --region <REGION> \
  --group-id "$SG_ID" --protocol tcp --port 587 --cidr 0.0.0.0/0
```

---

## 4. Lanzar la EC2 y asociar Elastic IP

### 4.1 Localizar la AMI de Amazon Linux 2023 (x86_64)

```bash
AMI_ID=$(aws ec2 describe-images --region <REGION> \
  --owners amazon \
  --filters "Name=name,Values=al2023-ami-*-x86_64" "Name=state,Values=available" \
  --query 'reverse(sort_by(Images,&CreationDate))[:1].ImageId' --output text)
echo "AMI_ID=$AMI_ID"
```

### 4.2 Lanzar la instancia (con provisión de Docker vía user-data)

El script [`scripts/aws-user-data.sh`](../scripts/aws-user-data.sh) instala Docker y el
plugin `docker compose` en el primer arranque (ver §5).

```bash
INSTANCE_ID=$(aws ec2 run-instances --region <REGION> \
  --image-id "$AMI_ID" \
  --instance-type t3.medium \
  --key-name <KEY_NAME> \
  --security-group-ids "$SG_ID" \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --user-data file://scripts/aws-user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=parking-app}]' \
  --query 'Instances[0].InstanceId' --output text)
echo "INSTANCE_ID=$INSTANCE_ID"

# Esperar a que esté "running"
aws ec2 wait instance-running --region <REGION> --instance-ids "$INSTANCE_ID"
```

> Alternativa por consola: EC2 → Launch instance → AMI *Amazon Linux 2023* →
> tipo `t3.medium` → par de claves `<KEY_NAME>` → Security Group `parking-sg` →
> Storage 30 GB gp3 → Advanced → *User data*: pega el contenido de `scripts/aws-user-data.sh`.

### 4.3 Elastic IP (IP pública fija)

Sin Elastic IP, la IP pública **cambia** cada vez que se para/arranca la instancia.

```bash
# Reservar una Elastic IP
ALLOC_ID=$(aws ec2 allocate-address --region <REGION> --domain vpc \
  --query 'AllocationId' --output text)

# Asociarla a la instancia
aws ec2 associate-address --region <REGION> \
  --instance-id "$INSTANCE_ID" --allocation-id "$ALLOC_ID"

# Obtener la IP asignada  → esta es tu <ELASTIC_IP>
aws ec2 describe-addresses --region <REGION> --allocation-ids "$ALLOC_ID" \
  --query 'Addresses[0].PublicIp' --output text
```

---

## 5. Provisionar el host (Docker + compose)

Si lanzaste con `--user-data file://scripts/aws-user-data.sh`, **el host ya queda
provisionado** (Docker instalado, servicio activo, `ec2-user` en el grupo docker, plugin
`docker compose` v2). Verifica los logs de cloud-init tras conectarte:

```bash
ssh -i <KEY_FILE> ec2-user@<ELASTIC_IP>
sudo tail -n 30 /var/log/cloud-init-output.log
docker --version
docker compose version
```

<details>
<summary>Provisión manual por SSH (si no usaste user-data)</summary>

```bash
ssh -i <KEY_FILE> ec2-user@<ELASTIC_IP>

sudo dnf update -y
sudo dnf install -y docker git
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user

# Plugin docker compose v2 (Amazon Linux 2023 no lo trae con el paquete docker)
DOCKER_CLI_PLUGINS=/usr/libexec/docker/cli-plugins
sudo mkdir -p "$DOCKER_CLI_PLUGINS"
ARCH="$(uname -m)"
sudo curl -fsSL \
  "https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-${ARCH}" \
  -o "$DOCKER_CLI_PLUGINS/docker-compose"
sudo chmod +x "$DOCKER_CLI_PLUGINS/docker-compose"

# Reconecta el SSH para que el grupo docker tenga efecto
exit
```
</details>

> Tras añadir el usuario al grupo `docker`, **reconecta la sesión SSH** (o `newgrp docker`)
> para poder ejecutar `docker` sin `sudo`.

---

## 6. Desplegar la aplicación

### 6.1 Clonar el repositorio en la instancia

```bash
ssh -i <KEY_FILE> ec2-user@<ELASTIC_IP>

# Rama de esta Etapa (o `develop` una vez mergeada la PR #112)
git clone --branch chore/devops/112-containerize-aws \
  https://github.com/lcasadov/parking.git
cd parking
```

> Si el repo es privado, autentícate con un **Personal Access Token** o despliega una
> **deploy key** SSH de solo lectura en la instancia antes del `git clone`.

### 6.2 Crear el fichero `.env` (secretos) — NO se versiona

El `.env` está en `.gitignore` (existe `.env.example` como plantilla, sin valores reales).
**Debes crearlo a mano en el servidor.** El backend lo carga vía `env_file` del compose.

Variables necesarias:

| Variable | Descripción | Usada por |
|---|---|---|
| `SMTP_HOST` | Host SMTP (Ethereal: `smtp.ethereal.email`) | backend |
| `SMTP_PORT` | Puerto SMTP (`587`) | backend |
| `SMTP_USERNAME` | Usuario Ethereal | backend |
| `SMTP_PASSWORD` | Contraseña Ethereal | backend |
| `SMTP_AUTH` | `true` | backend |
| `SMTP_STARTTLS` | `true` | backend |
| `MSSQL_SA_PASSWORD` | Contraseña del usuario `sa` de SQL Server. **Política:** ≥8 chars con mayúsculas, minúsculas, dígitos y símbolos. | sqlserver, db-init, backend (`DB_PASSWORD`) |
| `MAIL_FROM` | Remitente de los correos (p. ej. `no-reply@parking.aleatica.com`) | backend |
| `VAPID_PRIVATE_KEY` | Clave privada VAPID (Web Push). **Secreta.** `npx web-push generate-vapid-keys` | backend (runtime) |
| `VAPID_PUBLIC_KEY` | Clave pública VAPID (backend, para exponerla vía API) | backend (runtime) |
| `VAPID_SUBJECT` | `mailto:...` de contacto VAPID | backend (runtime) |
| `VITE_VAPID_PUBLIC_KEY` | Clave **pública** VAPID (mismo valor que `VAPID_PUBLIC_KEY`) horneada en el bundle | frontend (**build-time**, build-arg) |
| `VITE_MAPBOX_TOKEN` | Token público de Mapbox (mapa del parking, task 18) horneado en el bundle | frontend (**build-time**, build-arg) |
| `VITE_API_URL` | Base URL de la API si difiere del proxy por defecto (opcional) | frontend (**build-time**, build-arg) |

> **build-time vs runtime.** Las variables `VITE_*` las hornea Vite en el bundle **al construir la imagen del frontend** (`docker compose build frontend`), no en ejecución. `docker-compose.app.yml` las pasa como `build.args` interpolándolas desde este mismo `.env`, y el `Dockerfile` las materializa en `.env.production` antes de `vite build`. Por eso, **si cambias una `VITE_*` hay que reconstruir la imagen del frontend** (no basta con reiniciar el contenedor). La pública VAPID va en `VAPID_PUBLIC_KEY` (backend) **y** en `VITE_VAPID_PUBLIC_KEY` (frontend) con el mismo valor.

```bash
# En la instancia, dentro de ~/parking
cat > .env <<'EOF'
# --- SMTP (Ethereal) ---
SMTP_HOST=smtp.ethereal.email
SMTP_PORT=587
SMTP_USERNAME=<usuario-ethereal>
SMTP_PASSWORD=<password-ethereal>
SMTP_AUTH=true
SMTP_STARTTLS=true

# --- SQL Server ---
MSSQL_SA_PASSWORD=<contrasena-sa-fuerte>

# --- Correo ---
MAIL_FROM=no-reply@parking.aleatica.com

# --- Web Push (VAPID) — npx web-push generate-vapid-keys ---
VAPID_PRIVATE_KEY=<clave-privada-vapid>
VAPID_PUBLIC_KEY=<clave-publica-vapid>
VAPID_SUBJECT=mailto:no-reply@parking.aleatica.com

# --- Variables de build del frontend (VITE_) ---
VITE_VAPID_PUBLIC_KEY=<misma-clave-publica-vapid>
VITE_MAPBOX_TOKEN=<token-publico-mapbox>
EOF

chmod 600 .env   # solo el propietario puede leer los secretos
```

> ⚠️ **Nunca** hagas commit del `.env`. Sustituye `<...>` por valores reales solo en el servidor.

### 6.3 Construir y arrancar la pila

```bash
docker compose -f docker-compose.app.yml up -d --build
```

Primer arranque: compila el WAR (Maven) y la SPA (Vite) dentro de las imágenes, descarga
SQL Server 2022 (~1.5 GB) y espera a que la BD esté *healthy* antes de crear la base de
datos (`db-init`) y arrancar el backend. Puede tardar varios minutos.

```bash
# Estado de los 4 servicios (sqlserver, db-init, backend, frontend)
docker compose -f docker-compose.app.yml ps

# Seguir el arranque del backend hasta que Tomcat despliegue el WAR
docker compose -f docker-compose.app.yml logs -f backend
```

---

## 7. Verificación

```bash
# Desde tu máquina local (o desde la instancia con localhost)
curl -I http://<ELASTIC_IP>/                                   # → HTTP/1.1 200 OK (SPA)
curl -s http://<ELASTIC_IP>/parking-api/actuator/health        # → {"status":"UP"}
```

- En el navegador: `http://<ELASTIC_IP>/` debe cargar la SPA de parking.
- Si el health no responde: revisa `docker compose -f docker-compose.app.yml logs backend`
  y confirma que `sqlserver` está *healthy* (`... ps`).

---

## 8. Bootstrap del primer administrador ⚠️ BLOQUEANTE

En un despliegue limpio la tabla `dbo.employees` nace **vacía** (el seed de desarrollo
`V5__seed_dev_admin.sql` **solo** se carga en el perfil `des`; los perfiles `docker`/`pro`
cargan `spring.flyway.locations: classpath:db/migration`, solo esquema, para no filtrar la
credencial de dev — *bug #11 / CWE-798*). Como `POST /parking-api/employees` exige rol
`ADMIN`, sin un admin previo **nadie puede iniciar sesión** (huevo y gallina).

### 8.1 Método recomendado — bootstrap por variables de entorno (idempotente)

La aplicación crea el **primer administrador al arrancar** a partir de configuración inyectada
por entorno. Es **opt-in** (desactivado por defecto) e **idempotente**: solo crea el admin si
**no existe ya ningún `ADMIN`**; si ya hay uno, es un no-op. La contraseña se persiste
**hasheada con BCrypt** (el mismo encoder de la app, coste 12) y el admin nace con
`password_must_change = true` (debe cambiarla en el primer login).

**Paso 1 — añade las variables al `.env`** de la instancia (ver §6.2). Ejemplo:

```dotenv
PARKING_BOOTSTRAP_ADMIN_ENABLED=true
PARKING_BOOTSTRAP_ADMIN_EMAIL=admin@parking.aleatica.com
PARKING_BOOTSTRAP_ADMIN_PASSWORD=<PASSWORD_FUERTE>   # ≥10, may+min+dígito+símbolo, ≠ login/email
# Opcionales:
PARKING_BOOTSTRAP_ADMIN_FIRST_NAME=Admin
PARKING_BOOTSTRAP_ADMIN_LAST_NAME=Parking
PARKING_BOOTSTRAP_ADMIN_LOGIN=            # vacío → se deriva de la parte local del email
```

**Paso 2 — arranca la pila** (§6.3). En el log del backend verás
`[admin-bootstrap] Administrador inicial creado (...)`. Inicia sesión con ese admin, cambia la
contraseña forzada y crea el resto de usuarios desde la UI.

**Paso 3 — endurece (recomendado):** tras el primer arranque, pon
`PARKING_BOOTSTRAP_ADMIN_ENABLED=false` (o vacía email/password) para no reintentar en cada
reinicio. Aunque siga activo es un no-op si ya existe un admin, pero mantener la contraseña en
el `.env` es innecesario una vez creado.

> Si `ENABLED=true` pero faltan email/password, el arranque **no falla**: registra un WARN
> `[admin-bootstrap] ... faltan email/password` y no crea nada.

### 8.2 Alternativa de emergencia (manual) — INSERT directo en la BD

> Úsalo solo si el bootstrap por entorno (§8.1) no es viable. Inserta un admin con un hash
> **BCrypt** válido y `password_must_change = 1`.

Inserta un admin con un hash **BCrypt** válido. El esquema exige los campos NOT NULL de
`dbo.employees` (ver `V4__employees.sql`): `first_name`, `last_name`, `login`, `email`,
`role`, etc. La contraseña se guarda como `password_hash VARCHAR(72)` en formato BCrypt.

**Paso 1 — generar el hash BCrypt** (fuera del servidor, con tu propia contraseña fuerte).
Ejemplo con Python:

```bash
python3 -c "import bcrypt; print(bcrypt.hashpw(b'<TU_PASSWORD_FUERTE>', bcrypt.gensalt(rounds=12)).decode())"
# → $2b$12$....  (cópialo para el INSERT)
```

> La contraseña debe cumplir la política (≥10, mayúsc.+minúsc.+dígito+símbolo, distinta de
> login/email). El backend valida BCrypt variantes `$2a`/`$2b`.

**Paso 2 — insertar el admin** ejecutando `sqlcmd` **dentro** del contenedor de SQL Server
(no hay puerto 1433 expuesto; se accede por la red interna de compose):

```bash
docker compose -f docker-compose.app.yml exec sqlserver \
  /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -d parking -Q "
IF NOT EXISTS (SELECT 1 FROM dbo.employees WHERE login = N'admin')
INSERT INTO dbo.employees
  (first_name, last_name, login, email, password_hash, password_must_change,
   is_corporate, auth_origin, role, enabled, active, last_password_change_at, created_at)
VALUES
  (N'Admin', N'Parking', N'admin', N'admin@parking.aleatica.com',
   '<HASH_BCRYPT_DEL_PASO_1>', 0,
   0, 'LOCAL', 'ADMIN', 1, 1, SYSUTCDATETIME(), SYSUTCDATETIME());"
```

> `$MSSQL_SA_PASSWORD` debe estar disponible en tu shell (o sustitúyelo por el valor del
> `.env`). Ajusta `email`/`login` a los reales. Tras esto, inicia sesión con ese admin y
> crea el resto de usuarios desde la UI.

---

## 9. Operación

Todos los comandos desde `~/parking` en la instancia, con el flag `-f docker-compose.app.yml`.

```bash
# Ver logs (todos / un servicio)
docker compose -f docker-compose.app.yml logs -f
docker compose -f docker-compose.app.yml logs -f backend

# Estado
docker compose -f docker-compose.app.yml ps

# Reiniciar un servicio
docker compose -f docker-compose.app.yml restart backend

# Actualizar a la última versión del código
git pull
docker compose -f docker-compose.app.yml up -d --build

# Parar la pila (conserva datos: el volumen parking-app-mssql-data persiste)
docker compose -f docker-compose.app.yml down

# Parar y BORRAR los datos de la BD (¡destructivo!)
docker compose -f docker-compose.app.yml down -v
```

> El volumen `parking-app-mssql-data` guarda la base de datos. `down` **no** lo borra;
> `down -v` **sí**. Haz backup del volumen antes de operaciones destructivas (ver §11).

---

## 10. Coste aproximado (orientativo)

Estimación mensual en `eu-west-1`, **on-demand** (los precios varían por región y con el
tiempo; consulta la calculadora de AWS para cifras exactas):

| Concepto | Aprox. USD/mes |
|---|---|
| EC2 `t3.medium` (on-demand, 24/7) | ~30 |
| EBS gp3 30 GB | ~2.5 |
| Elastic IP (asociada a instancia en marcha) | 0 (gratis mientras esté asociada; se cobra si queda sin asociar) |
| Transferencia de datos saliente | variable (bajo para uso interno) |
| **Total estimado** | **~33–35 USD/mes** |

Ahorros posibles: parar la instancia fuera de horario (pierdes disponibilidad; la Elastic
IP entonces se factura), o una *Savings Plan*/Reserved Instance si el uso es estable.

---

## 11. Siguientes pasos / hardening (fuera del alcance actual)

1. **HTTPS con dominio.** Poner un reverse proxy con TLS (Caddy o nginx + Let's Encrypt,
   o un ALB de AWS con certificado ACM) delante de la pila. Al terminar TLS:
   - Cambiar al perfil **`pro`** (`SPRING_PROFILES_ACTIVE=pro`), que marca la cookie de
     sesión como **`Secure: true`** (`application-pro.yml`).
   - Configurar **`server.forward-headers-strategy: framework`** en el backend para que
     Spring interprete las cabeceras `X-Forwarded-Proto`/`X-Forwarded-For` del proxy.
     ⚠️ **Verificado: esta propiedad NO está configurada hoy en ningún `application*.yml`.**
     Sin ella, detrás de un proxy que termina TLS, Spring no detectará HTTPS correctamente
     (afecta a redirecciones y al flag `Secure` de la cookie). Añadirla es parte de este paso.
2. **Base de datos gestionada (RDS for SQL Server)** en lugar del contenedor: backups
   automáticos, alta disponibilidad, parches gestionados. Apunta `DB_URL` al endpoint de RDS.
3. **Secretos en SSM Parameter Store / Secrets Manager** en vez del `.env` en disco:
   inyectar SMTP y `MSSQL_SA_PASSWORD` en el arranque en lugar de guardarlos en texto plano.
4. **Imágenes con tags inmutables en ECR:** construir en CI y publicar `backend:<sha>` /
   `frontend:<sha>` en Amazon ECR; el compose de producción referencia tags fijos (no
   `build:` ni `latest`), para despliegues reproducibles y rollbacks limpios.
5. **Backups del volumen de datos.** Snapshot periódico del EBS y/o `docker run --rm -v
   parking-app-mssql-data:/data ...` para copiar el volumen; o backups nativos de SQL Server.
6. **Rotación de credenciales del admin inicial:** una vez creado el primer admin (§8.1),
   retira `PARKING_BOOTSTRAP_ADMIN_*` del `.env`/gestor de secretos.
7. **Observabilidad:** rotación/agregación de logs (CloudWatch Logs), alarmas de CPU/memoria/disco.

---

## Anexo — Resumen de la arquitectura desplegada

```
Internet ──HTTP:80──▶ [ EC2 t3.medium — Amazon Linux 2023 ]
                         └─ docker compose (proyecto: parking-app)
                              ├─ frontend  (nginx)     :80  ← único puerto público
                              │     └─ reverse-proxy /parking-api ─▶ backend
                              ├─ backend   (Tomcat/WAR) :8080  (interno)
                              ├─ sqlserver (MSSQL 2022) :1433  (interno, sin puerto host)
                              └─ db-init   (crea la BD `parking`, termina)
                         Volumen: parking-app-mssql-data (datos de la BD)
```

Perfil Spring activo: **`docker`** (HTTP, cookie no-Secure). Correo saliente: **Ethereal**
(SMTP 587). Secretos: fichero **`.env`** en la raíz del proyecto en la instancia (no versionado).
