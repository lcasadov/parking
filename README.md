# 🅿️ PARKIA — Gestión de Plazas de Parking y Puestos de Oficina ALEATICA

Aplicación web para la gestión y asignación de **recursos reservables** corporativos de **ALEATICA**: **plazas de parking** y **puestos de trabajo en oficina** (escritorios). Ambos tipos comparten el mismo modelo de funcionamiento: asignación fija indefinida por día de la semana, solicitud puntual para una fecha concreta y liberación voluntaria o administrativa. Permite a uno o varios administradores configurar recursos, gestionar asignaciones y solicitudes, y a los empleados solicitar o liberar recursos para días concretos desde web o móvil. Soporta además reservas puntuales para visitantes externos.

> **Estado del proyecto**
> La aplicación se desarrolla en **dos fases de autenticación**:
> - 🟢 **Fase 1 (actual)**: login local con usuario y contraseña (BCrypt en BD).
> - 🔵 **Fase 2 (futura)**: integración con la landing corporativa ALEATICA mediante validación de `id_token` (JWT). En esta fase la **landing autentica** (vía EntraID o credenciales propias de la landing) y **PARKIA autoriza** consultando su propia BD.
>
> Salvo indicación expresa, el resto del documento aplica a ambas fases.

---

## Arranque en local (desarrollo)

Prerrequisitos: Java 22, Maven 3.9, Docker Desktop.

```bash
# 1. Clonar y entrar al repositorio
git clone <GITHUB_REMOTE>
cd parkia

# 2. Configurar variables de entorno
cp .env.example .env
# Editar .env si es necesario (cambiar MSSQL_SA_PASSWORD, etc.)

# 3. Levantar infraestructura (SQL Server + MailHog)
docker compose up -d

# 4. Esperar a que sqlserver esté healthy
docker compose ps

# 5. Crear la base de datos (solo la primera vez)
docker exec -it parkia-sqlserver-1 /opt/mssql-tools18/bin/sqlcmd \
  -S localhost -U sa -P YourStrong@Passw0rd \
  -i /dev/stdin <<< "$(cat database/seed/00_create_database.sql)" -No

# 6. Arrancar el backend (Flyway aplica V1, V2, V3 automáticamente)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=des

# 7. Verificar salud
curl http://localhost:8080/parkia-api/api/v1/health
# → {"status":"UP"}

# 8. Ejecutar todos los tests
mvn verify
```

Para más detalles del proyecto, stack tecnológico y arquitectura ver `docs/PROJECT.md`.
Swagger UI disponible en: http://localhost:8080/parkia-api/swagger-ui.html

---

## Tabla de Contenidos

1. [Glosario](#-glosario)
2. [Recurso reservable — concepto unificado](#-recurso-reservable--concepto-unificado)
3. [Características](#-características)
4. [Puestos de Oficina](#-puestos-de-oficina)
5. [Reglas de Negocio](#-reglas-de-negocio)
6. [Flujo de Solicitudes](#-flujo-de-solicitudes)
7. [Arquitectura](#-arquitectura)
8. [Stack Tecnológico](#-stack-tecnológico)
9. [Autenticación — Fase 1: Login local 🟢](#-autenticación--fase-1-login-local-)
10. [Autenticación — Fase 2: SSO ALEATICA 🔵](#-autenticación--fase-2-sso-aleatica-)
11. [Reservas para Visitantes](#-reservas-para-visitantes)
12. [Internacionalización y UI](#-internacionalización-y-ui)
13. [Notas clave del modelo](#-notas-clave-del-modelo)
14. [Auditoría y Retención](#-auditoría-y-retención)
15. [Notificaciones por Email](#-notificaciones-por-email)
16. [Configuración del Entorno](#-configuración-del-entorno)
17. [Testing](#-testing)
18. [CI/CD y Migración a Azure DevOps](#-cicd-y-migración-a-azure-devops)
19. [Despliegue](#-despliegue)
20. [Roadmap de implementación](#-roadmap-de-implementación)

---

## 📖 Glosario

Terminología común a documentación, código y specs OpenSpec. **Usar siempre estos términos** para evitar ambigüedad.

| Término | Definición |
|---------|------------|
| **Plaza** | Espacio físico de aparcamiento identificado por un número (ej. `P-08`). Puede estar activa o inactiva. |
| **Asignación fija** | Vínculo indefinido entre un empleado y una plaza para uno o varios días de la semana (L-D). Se mantiene hasta que el admin la revoca. |
| **Día de la semana** | Valor 1-7 (1=Lunes ... 7=Domingo). Aplica a la asignación fija. |
| **Solicitud** | Petición de un empleado de un recurso reservable (plaza o puesto) para una **fecha concreta** (no día de la semana). Estados: `PENDIENTE`, `APROBADA`, `RECHAZADA`, `CANCELADA`. |
| **Liberación** | Acto de poner un recurso reservable asignado como disponible para un día concreto. Puede ser **voluntaria** (la hace el empleado titular) o **administrativa** (la hace el admin por inasistencia). |
| **Empleado corporativo** | Persona de ALEATICA con cuenta en EntraID. En Fase 2 se autentica en la landing vía EntraID. |
| **Empleado no corporativo en EntraID** | Persona de ALEATICA de una localización sin EntraID. En Fase 2 se autentica en la landing con usuario/contraseña local de la landing. |
| **Visitante** | Persona externa que **no es empleado** y no accede a la aplicación. Solo existe como dato de una reserva creada por el admin. |
| **Admin** | Empleado con rol `ADMIN`. Puede haber varios. Tiene acceso completo. |
| **Plaza disponible un día concreto** | Plaza activa que ese día no está ocupada por asignación fija (o lo está pero ha sido liberada), no tiene solicitud aprobada para esa fecha, y no tiene reserva de visitante para esa fecha. El mismo concepto aplica a puestos (sin reserva de visitante). |
| **Ventana de solicitud** | Antelación máxima con la que un empleado puede pedir un recurso: hoy + 14 días naturales. |
| **FIFO informativo** | El admin ve las solicitudes pendientes ordenadas por fecha de creación, pero no está obligado a aprobarlas en ese orden. |
| **Fallback de emergencia** | En Fase 2, capacidad latente del login local activable por config (`parkia.auth.local-fallback.enabled=true`) para acceso de contingencia cuando la landing no esté disponible. Desactivado por defecto. |
| **Recurso reservable** | Concepto común a plazas de parking y puestos de oficina. Tipo `PARKING` o `PUESTO`. Comparte la lógica de asignación fija, solicitud, liberación y cálculo de disponibilidad. |
| **Puesto** | Espacio físico de trabajo en la oficina, identificado por un número (1-65). Categorías: `ESTANDAR` o `DIRECCION`. Puede estar activo o inactivo. |
| **Plano** | Imagen de la planta de la oficina con marcadores superpuestos (uno por puesto), coloreados según el estado del puesto para la fecha seleccionada. Exclusivo de puestos; el parking no tiene plano. |
| **Categoría de puesto** | `ESTANDAR`: reservable por cualquier empleado. `DIRECCION`: habitualmente asignado L-V a un directivo; se distingue visualmente pero es liberable como cualquier otro. |
| **Coordenada relativa** | Posición de un puesto en el plano expresada como porcentaje del ancho (`coord_x`) y alto (`coord_y`) de la imagen, de 0 a 100. Independiente de la resolución. |
| **Editor de plano** | Herramienta de admin que permite arrastrar y posicionar los marcadores de puestos sobre la imagen del plano. Las coordenadas resultantes se almacenan en BD. |

---

## 🗂 Recurso reservable — concepto unificado

PARKIA generaliza su modelo bajo el concepto de **recurso reservable**: una abstracción que engloba tanto las plazas de parking como los puestos de oficina.

| Aspecto | Plazas de parking | Puestos de oficina |
|---------|------------------|--------------------|
| Tipo de recurso | `PARKING` | `PUESTO` |
| Identificación | Número de plaza (ej. `P-08`) | Número 1-65 |
| Categorías | — | `ESTANDAR` / `DIRECCION` |
| Vista visual | Lista numerada | Plano interactivo |
| Asignación fija | ✅ (por día de semana) | ✅ (por día de semana) |
| Solicitud puntual | ✅ | ✅ |
| Liberación | ✅ (voluntaria + administrativa) | ✅ (voluntaria + administrativa) |
| Reserva de visitante | ✅ | ❌ (no aplica) |

La lógica de **asignación fija**, **solicitud**, **liberación** y **cálculo de disponibilidad** es **compartida** entre ambos tipos y parametrizada por el tipo de recurso. Esto se implementará mediante un **refactor previo** que generaliza las entidades existentes antes de añadir los puestos.

---

## ✨ Características

### Panel de Administración (Web, rol `ADMIN`)
- **Gestión de empleados**: alta, modificación, baja lógica y reactivación. Campos: nombre, apellidos, login, email, departamento, teléfono móvil, matrícula, indicador corporativo (sí/no), activado, activo.
- **Reset administrativo de contraseña**:
  - Fase 1: el sistema genera una contraseña temporal que se muestra al admin en pantalla para que la entregue al empleado.
  - Fase 2: el sistema genera la contraseña temporal y se envía por email al empleado (la usará solo en caso de activarse el fallback de emergencia).
- **Gestión de plazas**: alta individual, modificación y configuración masiva del número total.
- **Gestión de puestos de oficina**: alta, modificación y activación/desactivación de los 65 puestos. Asignación de categoría (`ESTANDAR` / `DIRECCION`).
- **Asignación fija** de plaza o puesto a empleado, indefinida en el tiempo, indicando los días de la semana (L-D) que aplica.
- **Revocación o modificación** de la asignación fija en cualquier momento (solo aplica desde la fecha actual; histórico previo intacto).
- **Vista calendario semanal completa** con todos los recursos, titulares y estados. Solo visible para el admin.
- **Gestión de solicitudes pendientes**: aprobar (asignando recurso concreto) o rechazar (con motivo obligatorio). Las solicitudes de plaza y puesto se aprueban/rechazan **por separado**. UI ordenada por fecha de creación (FIFO informativo).
- **Editor de plano**: posicionamiento visual de los puestos sobre la imagen de la planta arrastrando los marcadores.
- **Liberación administrativa**: el admin puede liberar el recurso fijo de un empleado para un día concreto si no se presenta. Queda registrada con tipo `ADMINISTRATIVA` y motivo.
- **Reservas para visitantes**: alta de ficha de visitante (reusable) y creación de reserva puntual para una fecha concreta.
- **Histórico y auditoría**: consulta de auditoría completa y logs de login.
- **Exportación a CSV/XLSX** del histórico.
- **Dashboard de verificación**: vista consolidada de plazas y asignaciones (sin métricas analíticas en el alcance actual).

### Portal del Empleado (Web responsive, rol `EMPLEADO`)
- **Solicitud unificada**: solicitar **plaza de parking y/o puesto de oficina** para una misma fecha desde una sola pantalla. Se generan solicitudes independientes (una por recurso) que el admin aprueba/rechaza por separado.
- **Vista del plano** de puestos: visualización del estado de los 65 puestos para la fecha seleccionada. Permite pinchar un puesto libre para solicitarlo directamente.
- **Liberación voluntaria** de la propia plaza o puesto para un día concreto futuro.
- **Cancelación** de la propia solicitud mientras esté en estado `PENDIENTE`.
- **Vista personal "Mi Semana"**: los propios recursos asignados y los huecos libres. **No se ven nombres de otros empleados**.
- **Mis solicitudes**: histórico personal con estados y motivos de rechazo.
- **Exportación a CSV/XLSX** de las solicitudes propias.
- **Idioma**: español (defecto) e inglés, conmutable.
- **Modo oscuro**: toggle manual.

---

## 🪑 Puestos de Oficina

### Los 65 puestos

La planta de la oficina dispone de **65 puestos numerados** (1-65) reservables mediante PARKIA. Solo estos puestos son gestionados como recursos; salas de reunión, servicios, cafetería y demás elementos del plano son decorativos y no reservables.

### Categorías de puesto

| Categoría | Descripción |
|-----------|-------------|
| **ESTANDAR** | Puesto normal, reservable por cualquier empleado. |
| **DIRECCION** | Puesto de dirección. Habitualmente se le asigna una asignación fija de lunes a viernes. Se distingue visualmente en el plano (color/estilo diferente). Es **liberable** exactamente igual que cualquier otro puesto: si el directivo no acude un día puede liberarlo y quedará disponible para solicitud ese día. |

### Plano interactivo

- El plano es una **imagen de la planta** con **marcadores** superpuestos, uno por puesto, posicionados mediante coordenadas relativas (% del ancho y alto de la imagen).
- Cada marcador se **colorea según el estado** del puesto para la fecha seleccionada: libre, asignado, solicitado (pendiente), mi puesto, liberado, DIRECCION.
- El empleado puede **pinchar un puesto libre** para solicitarlo directamente desde el plano, sin necesidad de usar el formulario de solicitud separado.
- El admin dispone de un **editor visual** para ajustar las posiciones de los marcadores arrastrándolos sobre la imagen. Las coordenadas resultantes se persisten en BD.
- El **parking no tiene plano**: las plazas de parking se siguen gestionando como lista numerada.

### Solicitud unificada (plaza y/o puesto)

Desde una **sola pantalla** el empleado puede solicitar plaza de parking y/o puesto de oficina para la misma fecha. Por debajo se crean **solicitudes independientes** (una por recurso). El admin las aprueba o rechaza **por separado**: puede aprobar el puesto y rechazar la plaza, o viceversa. No existe entidad "solicitud agrupada"; la agrupación es únicamente una conveniencia de UI.

---

## 📜 Reglas de Negocio

### Días válidos
Se puede solicitar plaza **cualquier día del año**, incluidos sábados, domingos y festivos. No existe calendario laboral en el sistema.

### Ventana de solicitud
- Un empleado puede crear solicitudes desde **hoy** hasta **hoy + 14 días naturales** inclusive.
- Solicitar para el mismo día está permitido **sin límite horario**.

### Unicidad de solicitudes
- Máximo **una solicitud `PENDIENTE`** por empleado y fecha. Intentar crear otra devuelve `409 Conflict`.
- Solicitudes `RECHAZADA` o `CANCELADA` no bloquean crear una nueva para esa fecha.

### Máquina de estados de la solicitud
- `PENDIENTE` (inicial, `plaza_id = NULL`): puede ser aprobada o rechazada por admin, o cancelada por el propio empleado.
- `APROBADA` (con `plaza_id`, `resuelto_por_id`, `fecha_resolucion`): estado final.
- `RECHAZADA` (con `motivo_rechazo`, `resuelto_por_id`, `fecha_resolucion`): estado final.
- `CANCELADA`: cerrada por el empleado mientras estaba `PENDIENTE`.

### Disponibilidad de plaza en una fecha F
Una plaza está **disponible para F** si cumple **todo** lo siguiente:
1. `Plaza.activa = true`.
2. **No** tiene `AsignacionFija` activa con `dia_semana = diaSemana(F)`, **o** la tiene pero existe una `Liberacion` para esa plaza y fecha F.
3. **No** existe ninguna `Solicitud APROBADA` para esa plaza y fecha F.
4. **No** existe ninguna `ReservaVisita` para esa plaza y fecha F.

### Asignación fija
- Una plaza no puede estar asignada de forma fija a dos empleados distintos el **mismo día de la semana**.
- Un empleado no puede tener dos plazas asignadas el mismo día de la semana.
- Una misma plaza sí puede compartirse entre empleados en días distintos.
- Modificar una asignación fija **no afecta a días pasados** ni a solicitudes ya aprobadas. Solo aplica desde la fecha actual.
- La revocación es lógica: la fila se marca `activa=false` con `fecha_revocacion` y `revocada_por_id`, no se borra.

### Aprobación / rechazo
- Al aprobar, el sistema valida la disponibilidad de la plaza elegida. Si no está disponible, devuelve `409 Conflict`.
- Al rechazar, el `motivo_rechazo` es **obligatorio** (mínimo 5 caracteres).
- El admin ve las pendientes ordenadas por `fecha_creacion ASC` (FIFO informativo) pero decide libremente.

### Liberación
- **Voluntaria**: solo el dueño de la asignación fija puede liberar, y solo para fechas presentes o futuras.
- **Administrativa**: el admin puede liberar la plaza fija de cualquier empleado para cualquier fecha presente o futura, indicando motivo. Aparece en el histórico con tipo `ADMINISTRATIVA`.

### Reglas específicas de puestos
- **RN-PUESTO-01**: un empleado puede tener asignación fija de plaza (parking) y asignación fija de puesto (oficina) simultáneamente; son recursos distintos.
- **RN-PUESTO-02**: los puestos `DIRECCION` se distinguen visualmente en el plano pero siguen las mismas reglas de liberación que los `ESTANDAR`.
- **RN-PUESTO-03**: solo los 65 puestos numerados son reservables; el resto de elementos del plano no se modelan como recursos.
- **RN-PUESTO-04**: al solicitar desde el plano, solo se pueden seleccionar puestos en estado "libre" para la fecha elegida.

### Multi-admin
- Pueden coexistir varios admins activos.
- Cualquier admin puede aprobar/rechazar cualquier solicitud.
- En la notificación de "nueva solicitud" se envía email a **todos los admins activos**.

---

## 🔄 Flujo de Solicitudes

```
                           ┌──────────────┐
                           │   Empleado   │
                           │  crea sol.   │
                           └──────┬───────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │   PENDIENTE     │
                         │ (sin plaza_id)  │
                         └────────┬────────┘
                                  │
        ┌─────────────────┬───────┴──────┬─────────────────┐
        │                 │              │                 │
   Admin aprueba    Admin rechaza   Empleado cancela     Caduca*
        │                 │              │
        ▼                 ▼              ▼
  ┌──────────┐      ┌──────────┐    ┌──────────┐
  │ APROBADA │      │RECHAZADA │    │CANCELADA │
  └─────┬────┘      └─────┬────┘    └──────────┘
        │                 │
        ▼                 ▼
  📧 Email aprob.   📧 Email rech.
```
\*No hay caducidad automática en el alcance actual; el admin debe resolver manualmente.

📧 Cuando se crea la solicitud, se envía email a **todos los admins activos** informando de la nueva pendiente.

---

## 🏗 Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                     PARKIA Web Application                  │
│                                                             │
│  ┌──────────────────┐         ┌──────────────────────────┐  │
│  │  React SPA       │ ──────► │  Spring Boot 3.3 (WAR)   │  │
│  │  (Admin +        │  HTTPS  │  - Filtro auth           │  │
│  │   Empleado)      │ ◄────── │  - REST Controllers      │  │
│  │  withCredentials │  cookie │  - Services              │  │
│  └──────────────────┘         │  - Hibernate / JPA       │  │
│                                │  - Mail Service          │  │
│                                │  - AOP Auditoría         │  │
│                                │  - @Scheduled purga      │  │
│                                └──────────┬───────────────┘  │
│                                           │                   │
│           ┌───────────────────────────────┼────────────┐      │
│           ▼                               ▼            ▼      │
│   ┌──────────────┐                ┌────────────┐  ┌────────┐ │
│   │ SQL Server   │                │  SMTP      │  │ Spring │ │
│   │ - datos      │                │ Ethereal/  │  │Session │ │
│   │ - sesiones   │                │ Corporat.  │  │  JDBC  │ │
│   │ - auditoría  │                └────────────┘  └────────┘ │
│   └──────────────┘                                            │
└─────────────────────────────────────────────────────────────┘

  Fase 2 añade:
  ┌─────────────────────┐         ┌───────────────────────┐
  │  Landing ALEATICA   │ ──JWT──►│  GET /ssocallback     │
  │  (EntraID o local)  │         └───────────────────────┘
  └─────────────────────┘
  ┌─────────────────────┐         ┌───────────────────────┐
  │  Landing → POST     │ ──JWT──►│  POST /CloseSSO...    │
  │  CloseSSOSessionID  │         └───────────────────────┘
  └─────────────────────┘
```

---

## 🛠 Stack Tecnológico

### Backend
| Componente | Versión | Uso |
|------------|---------|-----|
| Java JDK | 22 | Lenguaje base |
| Spring Boot | 3.3.x | Framework principal |
| Spring Security | 6.x | Filtros, autorización, BCrypt |
| Spring Web | 6.x | API REST |
| Spring Data JPA | 3.x | Persistencia |
| Hibernate | 6.5.x | ORM |
| Spring Session JDBC | 3.x | Sesiones en SQL Server |
| jjwt (`io.jsonwebtoken`) | 0.12.x | Validación JWT (Fase 2) |
| Spring Mail + Thymeleaf | 3.x | Emails con plantillas |
| Flyway | 10.x | Migraciones de BD |
| Apache Tomcat | 10.1.x | Servidor (despliegue WAR) |
| Microsoft JDBC Driver | 12.x | Conector SQL Server |
| Maven | 3.9.x | Gestor de dependencias |
| SpringDoc OpenAPI | 2.x | Swagger UI / contrato OpenAPI |

### Frontend
| Componente | Versión | Uso |
|------------|---------|-----|
| React | 18.x | Framework UI |
| Vite | 5.x | Build tool |
| React Router | 6.x | Enrutamiento |
| Axios | 1.x | Cliente HTTP (`withCredentials: true`) |
| TailwindCSS | 3.x | Estilos (mobile-first) |
| date-fns | 3.x | Manejo de fechas |
| react-i18next | 14.x | Internacionalización ES/EN |

### Base de Datos y Mail
- Microsoft SQL Server 2022
- SMTP: **Ethereal** en DES/QA, SMTP corporativo en PRE/PRO

---

## 🔐 Autenticación — Fase 1: Login local 🟢

Durante la Fase 1 PARKIA gestiona sus propias credenciales. Es una solución **temporal** sustituida por la integración con la landing en Fase 2.

### Características
- Login con **usuario + contraseña**, hash **BCrypt (coste 12)**.
- Cookie de sesión `PARKIA_SESSION` con flags `HttpOnly`, `Secure`, `SameSite=Lax`.
- Persistencia mediante **Spring Session JDBC** sobre SQL Server (misma infra que Fase 2).
- Bloqueo de cuenta tras **5 intentos fallidos consecutivos** durante 15 minutos.
- Política de contraseña:
  - Mínimo 10 caracteres.
  - Mayúscula + minúscula + dígito + símbolo.
  - Distinta del login y del email.
  - Sin caducidad obligatoria en Fase 1 (la rotación de 90 días aplica solo en Fase 2 para cuentas con fallback).
- **Cambio obligatorio** en el primer login tras un reset administrativo.

### Reset de contraseña en Fase 1
- El admin pulsa "Resetear contraseña" en la ficha del empleado.
- El sistema genera una contraseña aleatoria segura, la hashea y marca `password_must_change=true`.
- La contraseña en claro **se muestra al admin una sola vez** con botón de copia. La entrega al empleado es por canal directo (no por email en Fase 1).
- En el siguiente login, el empleado debe cambiarla antes de continuar.

### Endpoints (ver tabla completa en sección [API REST](#-api-rest))
- `POST /auth/login` — público
- `POST /auth/logout` — sesión
- `GET /auth/me` — sesión
- `POST /auth/cambiar-password` — sesión
- `POST /admin/empleados/{id}/reset-password` — ADMIN

---

## 🔐 Autenticación — Fase 2: SSO ALEATICA 🔵

En Fase 2 PARKIA se publica detrás de la landing corporativa ALEATICA y deja de tener pantalla de login propia.

### Reparto de responsabilidades
- **Landing ALEATICA → autentica**: decide internamente cómo verificar al usuario (EntraID para empleados corporativos, usuario/contraseña local de la landing para empleados de localizaciones sin EntraID). Emite un `id_token` JWT con `username`, `client_sid`, `iss=SSOTTS`, `aud=PARKIA`, `exp`.
- **PARKIA → autoriza**: valida la firma del JWT, busca el `username` en su propia tabla `EMPLEADO`, decide acceso y rol. **No se hace provisioning automático**: el empleado debe haber sido dado de alta previamente por el admin.

### Algoritmo de autorización en `/ssocallback`
1. Validar firma, `iss`, `aud`, `exp` del `id_token`.
2. Extraer `username` del claim.
3. Buscar `Empleado` por `username`:
   - **No existe** → 403 "Sin acceso a PARKIA. Contacte con el administrador." Se registra en `LOGIN_LOG` con resultado `SIN_ACCESO`.
   - **Existe pero `activo=false`** → 403 "Cuenta inactiva." Resultado `INACTIVO`.
   - **Existe y activo** → continuar.
4. Tomar rol interno del campo `Empleado.rol` (`ADMIN` o `EMPLEADO`). **El claim `roles` del JWT se ignora**.
5. Crear sesión Spring Session, emitir cookie `PARKIA_SESSION`, guardar atributos `empleado_id`, `rol`, `client_sid`, `slo_token`.
6. Redirigir 302 a `redirect_uri`.

### Flujo completo

```
1. Usuario abre https://parking.aleatica.com
2. React → GET /api/v1/auth/me (withCredentials:true)
3. Backend → 401 (sin cookie)
4. Frontend redirige a:
   https://ccodes.aleaticalabs.com/landpage/authorize?client_id=PARKIA
       &redirect_uri=https://parking.aleatica.com
5. La landing autentica al usuario (EntraID o local)
6. Landing redirige a:
   https://parking.aleatica.com/ssocallback?id_token=...&client_id=PARKIA&redirect_uri=...
7. Backend ejecuta el algoritmo de autorización (ver arriba)
8. Frontend autenticado; resto de llamadas viajan con la cookie
9. Cualquier 401 posterior → modal "Sesión expirada" → vuelta a la landing
```

### Single Logout desde la landing
```http
POST https://parking.aleatica.com/parkia-api/CloseSSOSessionID
Content-Type: application/json

{
  "slo_token": "eyJhbGciOiJIUzI1NiJ9...",
  "client_sid": "d577457c5869e90bf2223230e0f42c58"
}
```
El backend valida el `slo_token`, localiza la sesión en `SPRING_SESSION` por el atributo `client_sid` y la invalida. Devuelve 200 (o 404 si no existe).

### Alta de empleados en Fase 2
El alta sigue siendo **manual por el admin**. Diferencias respecto a Fase 1:
- Los empleados creados en Fase 2 nacen con `password_hash = NULL` (sin login local).
- El `login` debe coincidir **exactamente** con el `username` que emite la landing.
- Si el admin necesita activar el login local de un empleado (para el fallback de emergencia), debe ejecutar el reset administrativo y la contraseña temporal se envía por email al empleado.

### Migración Fase 1 → Fase 2
- Los empleados creados en Fase 1 conservan su `password_hash` y campos relacionados.
- Los endpoints `POST /auth/login` y `POST /admin/empleados/{id}/reset-password` siguen existiendo pero condicionados:
  - `POST /auth/login` solo responde si `parkia.auth.local-fallback.enabled=true`. Si está desactivado, devuelve 404.
  - `POST /admin/empleados/{id}/reset-password` se reorienta a generar contraseña + email (no se muestra al admin).
- **No se eliminan columnas** de la tabla `EMPLEADO`: el fallback se mantiene como capacidad latente.

### Fallback de emergencia (Fase 2)
- **Desactivado por defecto** en todos los entornos.
- Se activa por configuración (`application.yml`) en escenarios de indisponibilidad de la landing. Requiere reinicio del WAR o recarga de configuración externa.
- **Cualquier empleado con contraseña válida** puede usarlo (no solo admins).
- **Rotación obligatoria cada 90 días** para cualquier cuenta con contraseña activa en Fase 2: si han pasado más de 90 días desde el último cambio, el sistema obliga a cambiarla antes de continuar.
- Cada uso del fallback se registra en `LOGIN_LOG` con `fase=FALLBACK` y resultado `FALLBACK_OK` (o el resultado de fallo correspondiente).

### Datos pendientes para Fase 2
- Clave/secreto para validar la firma del `id_token`.
- URLs definitivas para entornos PRE y PRO.
- Especificación exacta del servicio `consultaporlogin` (a incluir en `docs/servicioconsultaporlogin.md` cuando se reciba).

---

## 🚗 Reservas para Visitantes

El admin puede reservar una plaza para una persona que **no es empleado** de ALEATICA. Los visitantes no acceden a la aplicación, no tienen cuenta y no reciben emails.

### Modelo
Dos entidades separadas para permitir reuso de la ficha del visitante en futuras visitas:

- **`VISITANTE`** (ficha reutilizable): `id`, `nombre`, `apellidos`, `dni` (único), `matricula`, `empresa` (opcional), `motivo_habitual` (opcional), `creado_por_id`, `fecha_creacion`.
- **`RESERVA_VISITA`** (instancia de reserva): `id`, `visitante_id` (FK), `plaza_id` (FK), `fecha`, `observaciones`, `creado_por_id` (FK Empleado ADMIN), `fecha_creacion`.

### Reglas
- Una `RESERVA_VISITA` ocupa la plaza para esa fecha en el cálculo de disponibilidad (cuenta como plaza no disponible).
- No tiene estados ni flujo de aprobación: la crea directamente el admin.
- El admin puede:
  - Crear una ficha de visitante nueva.
  - Buscar visitante existente por DNI, nombre, apellidos o matrícula y reusarlo.
  - Editar la ficha (afecta solo a futuras reservas).
  - Anular reservas futuras (no las pasadas).
- No genera notificaciones por email (el visitante no tiene cuenta).
- Cada acción del admin sobre visitantes y reservas queda en `AUDIT_LOG`.

---

## 🌐 Internacionalización y UI

- **Idiomas**: español (defecto) e inglés. Implementación con `react-i18next` desde el inicio.
- **Modo oscuro**: toggle manual en la aplicación (no detección automática).
- **Responsive**: mobile-first con Tailwind. Sin PWA ni apps nativas en el alcance actual.
- **Mockups**: ALEATICA proporcionará los mockups del UI antes de la Fase 5 del roadmap.

---

## 🗒 Notas clave del modelo

> El diagrama completo del modelo de datos vive en `docs/modelo-datos.md`. Aquí se resumen las convenciones críticas que afectan a toda la aplicación.

- **`dia_semana` es 1-7** (1=Lunes ... 7=Domingo), no 1-5.
- **Unicidades activas** mediante *filtered indexes* de SQL Server (cláusula `WHERE activa=true` o `WHERE estado='PENDIENTE'`).
- **Revocaciones lógicas**: nunca se borran filas de `AsignacionFija`, se marcan `activa=false`.
- **`AUDIT_LOG`** se rellena automáticamente por un `@Aspect` Spring AOP sobre los servicios anotados.
- **`LOGIN_LOG`** se rellena en el filtro de autenticación; cubre Fase 1, Fase 2 y fallback.

---

## 🔍 Auditoría y Retención

### Qué se audita
- **Acciones de admin**: alta/modificación/baja de empleados, plazas, asignaciones fijas, reservas de visitante, aprobaciones, rechazos, liberaciones administrativas, reset de contraseña.
- **Acciones de empleado**: creación, cancelación de solicitudes propias; liberaciones voluntarias.
- **Logins**: todos los intentos (OK y fallidos), separados en `LOGIN_LOG` para no contaminar la auditoría funcional.

### Retención y purga
- **Datos vivos** (entidades activas: `Empleado`, `Plaza`, `AsignacionFija` activa, `Visitante`): **sin purga**.
- **Datos históricos** (`Solicitud` cerradas, `Liberacion`, `ReservaVisita`, `AUDIT_LOG`, `LOGIN_LOG`): **purga automática a 2 años** desde su `fecha_creacion`.
- Implementación: job `@Scheduled` diario que ejecuta DELETE en lotes para evitar bloqueos prolongados.
- La política de retención es ajustable por entorno vía `parkia.retention.years`.

---

## 📧 Notificaciones por Email

| Evento | Destinatario | Plantilla |
|--------|--------------|-----------|
| Nueva solicitud creada | Todos los admins activos | `solicitud-nueva.html` |
| Solicitud aprobada | Empleado solicitante | `solicitud-aprobada.html` |
| Solicitud rechazada | Empleado solicitante | `solicitud-rechazada.html` |
| Asignación fija revocada | Empleado afectado | `asignacion-revocada.html` |
| Reset de contraseña (solo Fase 2) | Empleado afectado | `password-reset.html` |

### Reglas de envío
- Plantillas Thymeleaf en `src/main/resources/templates/email/`.
- Envío **`AFTER_COMMIT`** de la transacción que dispara el evento: si la transacción falla, no se manda email.
- Si el envío SMTP falla, se registra en log y se reintenta mediante un job programado. **Nunca debe revertir la operación funcional**.
- En DES/QA se usa **Ethereal** (no se envían correos reales). En PRE/PRO se usa SMTP corporativo.
- No se envían correos al liberar una plaza voluntariamente ni al cancelar la propia solicitud.

---

## ⚙️ Configuración del Entorno

### Entornos
- **DES**, **PRE**, **PRO**.
- Cada uno con su `application-{perfil}.yml`.
- Para PARKIA, los entornos productivos referenciarán a la landing de ALEATICA cuando llegue la Fase 2.

### Configuración relevante (extracto)
```yaml
parkia:
  auth:
    local-fallback:
      enabled: false           # Fase 2: solo true en emergencias
    password-rotation-days: 90 # Solo aplica en Fase 2
  retention:
    years: 2
  cors:
    allowed-origins:
      - https://parking.aleatica.com
```

### Docker para desarrollo local
`docker-compose.yml` levanta:
- **SQL Server 2022** (puerto 1433).
- **Ethereal** (SMTP de pruebas, ver credenciales en https://ethereal.email).

La aplicación backend y frontend se ejecutan en nativo (no en Docker), por simplicidad y velocidad de iteración.

---

## 🧪 Testing

- **Unitarios + integración + E2E** desde el inicio.
- **Cobertura objetivo**: ≥70-80% para pasar el Quality Gate de SonarCloud.
- Stack:
  - Backend: JUnit 5, Mockito, Spring Boot Test, Testcontainers (SQL Server).
  - Frontend: Vitest, React Testing Library.
  - E2E: **Playwright** (a confirmar; alternativa Cypress).
- Mocks del SSO WS y de SMTP en tests de integración.

---

## 🔄 CI/CD y Migración a Azure DevOps

### Estado inicial
- Repositorio en **GitHub** durante el arranque del proyecto.
- **SonarCloud** integrado con GitHub para SAST desde el primer commit.

### Destino
- Migración a **Azure DevOps Repos** cuando se cierre la fase de arranque.
- Pipelines en **Azure Pipelines (YAML)**: build → test → SonarCloud → empaquetado WAR → deploy a Tomcat.
- SonarCloud sigue siendo la herramienta SAST (soporta GitHub y ADO).


---

## 🏭 Despliegue

### Generar el WAR para Tomcat 10.1
`backend/pom.xml`:
```xml
<packaging>war</packaging>
```
Clase principal:
```java
public class ParkingApplication extends SpringBootServletInitializer { ... }
```
Build:
```bash
cd backend
mvn clean package -Pprod
# Genera target/parkia-api.war
```

### Desplegar en Tomcat 10.1
```bash
cp backend/target/parkia-api.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```
> ⚠️ Tomcat 10 requiere Jakarta EE 9+. Spring Boot 3.x ya lo cumple. **No usar Tomcat 9**.

### Frontend
```bash
cd frontend
npm run build
# Servir dist/ desde Nginx/IIS con fallback SPA a index.html.
```

### Reverse proxy (Nginx, orientativo)
```nginx
server {
  listen 443 ssl http2;
  server_name parking.aleatica.com;

  location /parkia-api/ {
    proxy_pass http://tomcat-internal:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto https;
  }

  location / {
    root /var/www/parking-frontend;
    try_files $uri $uri/ /index.html;
  }
}
```

### Observabilidad
- Logs en fichero rotativo (`logback-spring.xml`).
- Sin métricas Prometheus ni dashboards en el alcance actual.
- Spring Actuator habilitado con endpoints mínimos (`health`, `info`) protegidos.

---

## 🗺 Roadmap de implementación

El desarrollo sigue el orden de cambios (OpenSpec changes) detallado a continuación. Cada change es un bloque autónomo con proposal, diseño, specs y tareas.

### Changes completados ✅

| Change | Descripción |
|--------|-------------|
| `bootstrap-mvp` | Infraestructura backend: Spring Boot, seguridad, Flyway, CI. |
| `auth-local` | Autenticación local Fase 1: login, logout, me, cambio de contraseña. |
| `frontend-bootstrap` | Scaffold React 19 + Vite + auth flows + i18n + tema. |

### Changes en curso / pendientes 🔵

| Change | Descripción | Depende de |
|--------|-------------|------------|
| `empleados` | CRUD de empleados, RBAC, reset de contraseña. | `auth-local` |
| `plazas` | CRUD de plazas de parking. | `bootstrap-mvp` |
| `solicitudes` | Solicitud, aprobación y rechazo de plazas. | `plazas`, `empleados` |
| `asignaciones-fijas` | Asignación fija plaza ↔ empleado por día de semana. | `plazas`, `empleados` |
| `liberaciones` | Liberación voluntaria y administrativa de plazas. | `asignaciones-fijas` |
| `disponibilidad-calendario` | Cálculo y vista de disponibilidad semanal. | `solicitudes`, `liberaciones` |
| `visitantes` | Fichas de visitante y reservas de plaza puntual. | `plazas` |
| `notificaciones` | Envío de emails en eventos de solicitud. | `solicitudes` |

### Changes de puestos (alcance ampliado) 🟡

Secuencia acordada en [`docs/changes/CHANGE-puestos.md`](docs/changes/CHANGE-puestos.md):

| # | Change | Descripción |
|---|--------|-------------|
| 1 | `refactor-recurso-generico` | Generalizar `Plaza`, `Solicitud`, `AsignacionFija`, `Liberacion` y `Disponibilidad` al concepto de recurso (`PARKING` / `PUESTO`). Sin cambio de comportamiento visible para el usuario; los tests existentes deben seguir verdes. |
| 2 | `puestos` | Entidad `Puesto` (número 1-65, categorías `ESTANDAR`/`DIRECCION`, coordenadas). CRUD de puestos, asignación fija y solicitud de puestos. |
| 3 | `plano` | Plano interactivo: imagen de planta, marcadores por estado, solicitud desde plano. Editor visual de posicionamiento para el admin. |
