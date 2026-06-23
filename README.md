# 🅿️ parking — Gestión de Plazas de Parking y Puestos de Oficina ALEATICA

Aplicación web para la gestión y asignación de **recursos reservables** corporativos de **ALEATICA**: **plazas de parking** y **puestos de trabajo en oficina** (escritorios). Ambos tipos comparten el mismo modelo de funcionamiento: asignación fija indefinida por día de la semana, solicitud puntual para una fecha concreta y liberación voluntaria o administrativa. Permite a uno o varios administradores configurar recursos, gestionar asignaciones y solicitudes, y a los empleados solicitar o liberar recursos para días concretos desde web o móvil. Soporta además reservas puntuales para visitantes externos.

> **Naturaleza de este documento**
> Este README es la **especificación funcional y de negocio** del proyecto. Define qué hace la aplicación, sus reglas y su modelo conceptual. Es la fuente de la que se derivarán el resto de artefactos (modelo de datos, contrato API, planes de tareas, etc.). No contiene detalles de instalación, despliegue ni stack tecnológico.

> **Convención de idioma (importante)**
> - La **prosa funcional y de negocio** se escribe en **español** (es el idioma de ALEATICA y de los usuarios).
> - **Todo identificador de código** —entidades, clases, enumerados, campos, columnas y tablas— se nombra en **inglés**.
> - La equivalencia entre el vocabulario de negocio (ES) y los identificadores de código (EN) está en la sección [Nomenclatura del código](#-nomenclatura-del-código-es--en). **Esa tabla es la autoridad**: cualquier doc o código que se genere a partir de este README debe usar los nombres en inglés que allí se fijan.

> **Estado del proyecto**
> La aplicación se desarrolla en **dos fases de autenticación**:
> - 🟢 **Fase 1 (actual)**: login local con usuario y contraseña (BCrypt en BD).
> - 🔵 **Fase 2 (futura)**: integración con la landing corporativa ALEATICA mediante validación de `id_token` (JWT). En esta fase la **landing autentica** (vía EntraID o credenciales propias de la landing) y **parking autoriza** consultando su propia BD.
>
> Salvo indicación expresa, el resto del documento aplica a ambas fases.

---

## Tabla de Contenidos

1. [Glosario de negocio](#-glosario-de-negocio)
2. [Nomenclatura del código (ES → EN)](#-nomenclatura-del-código-es--en)
3. [Recurso reservable — concepto unificado](#-recurso-reservable--concepto-unificado)
4. [Características](#-características)
5. [Puestos de oficina](#-puestos-de-oficina)
6. [Reglas de negocio](#-reglas-de-negocio)
7. [Flujo de solicitudes](#-flujo-de-solicitudes)
8. [Autenticación — Fase 1: Login local 🟢](#-autenticación--fase-1-login-local-)
9. [Autenticación — Fase 2: SSO ALEATICA 🔵](#-autenticación--fase-2-sso-aleatica-)
10. [Reservas para visitantes](#-reservas-para-visitantes)
11. [Internacionalización y UI](#-internacionalización-y-ui)
12. [Notas clave del modelo](#-notas-clave-del-modelo)
13. [Auditoría y retención](#-auditoría-y-retención)
14. [Notificaciones por email](#-notificaciones-por-email)
15. [Roadmap funcional](#-roadmap-funcional)

---

## 📖 Glosario de negocio

Terminología común a documentación, código y specs. **Usar siempre estos términos** para evitar ambigüedad. El identificador de código en inglés de cada concepto está en [Nomenclatura del código](#-nomenclatura-del-código-es--en).

| Término | Definición |
|---------|------------|
| **Plaza** | Espacio físico de aparcamiento identificado por un número (ej. `P-08`). Puede estar activa o inactiva. Entidad de código: `ParkingSpace`. |
| **Puesto** | Espacio físico de trabajo en la oficina, identificado por un número (1-65). Categorías: `STANDARD` o `EXECUTIVE`. Puede estar activo o inactivo. Entidad de código: `Desk`. |
| **Recurso reservable** | Concepto común a plazas de parking y puestos de oficina. Tipo `PARKING` o `DESK`. Comparte la lógica de asignación fija, solicitud, liberación y cálculo de disponibilidad. Abstracción de código: `BookableResource`. |
| **Asignación fija** | Vínculo indefinido entre un empleado y un recurso para uno o varios días de la semana (L-D). Se mantiene hasta que el admin la revoca. Entidad: `FixedAssignment`. |
| **Día de la semana** | Valor 1-7 (1=Lunes … 7=Domingo). Aplica a la asignación fija. Campo: `day_of_week`. |
| **Solicitud** | Petición de un empleado de un recurso reservable (plaza o puesto) para una **fecha concreta** (no día de la semana). Estados: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`. Entidad: `Request`. |
| **Liberación** | Acto de poner un recurso asignado como disponible para un día concreto. Puede ser **voluntaria** (`VOLUNTARY`, la hace el empleado titular) o **administrativa** (`ADMINISTRATIVE`, la hace el admin por inasistencia). Entidad: `Release`. |
| **Empleado corporativo** | Persona de ALEATICA con cuenta en EntraID. En Fase 2 se autentica en la landing vía EntraID. Campo: `is_corporate = true`. |
| **Empleado no corporativo** | Persona de ALEATICA de una localización sin EntraID. En Fase 2 se autentica en la landing con usuario/contraseña local de la landing. Campo: `is_corporate = false`. |
| **Visitante** | Persona externa que **no es empleado** y no accede a la aplicación. Solo existe como dato de una reserva creada por el admin. Entidad: `Visitor`. |
| **Admin** | Empleado con rol `ADMIN`. Puede haber varios. Tiene acceso completo. |
| **Empleado** | Persona con rol `EMPLOYEE`. Acceso al portal del empleado. Entidad: `Employee`. |
| **Recurso disponible un día concreto** | Recurso activo que ese día no está ocupado por asignación fija (o lo está pero ha sido liberado), no tiene solicitud aprobada para esa fecha, y —solo en plazas— no tiene reserva de visitante para esa fecha. |
| **Ventana de solicitud** | Antelación máxima con la que un empleado puede pedir un recurso: hoy + 14 días naturales. |
| **FIFO informativo** | El admin ve las solicitudes pendientes ordenadas por fecha de creación, pero no está obligado a aprobarlas en ese orden. |
| **Fallback de emergencia** | En Fase 2, capacidad latente del login local activable por configuración para acceso de contingencia cuando la landing no esté disponible. Desactivado por defecto. |
| **Plano** | Imagen de la planta de la oficina con marcadores superpuestos (uno por puesto), coloreados según el estado del puesto para la fecha seleccionada. Exclusivo de puestos; el parking no tiene plano. |
| **Categoría de puesto** | `STANDARD`: reservable por cualquier empleado. `EXECUTIVE`: habitualmente asignado L-V a un directivo; se distingue visualmente pero es liberable como cualquier otro. |
| **Coordenada relativa** | Posición de un puesto en el plano expresada como porcentaje del ancho (`coord_x`) y alto (`coord_y`) de la imagen, de 0 a 100. Independiente de la resolución. |
| **Editor de plano** | Herramienta de admin que permite arrastrar y posicionar los marcadores de puestos sobre la imagen del plano. Las coordenadas resultantes se almacenan en BD. |

---

## 🔤 Nomenclatura del código (ES → EN)

**Autoridad de nombres.** Toda entidad, clase, enumerado, campo, columna y tabla del proyecto usa los identificadores en inglés de estas tablas. La prosa de negocio puede seguir usando los términos en español del glosario.

### Entidades y tablas

| Concepto de negocio (ES) | Clase / Entidad (EN) | Tabla (EN) |
|--------------------------|----------------------|------------|
| Empleado | `Employee` | `employees` |
| Plaza de parking | `ParkingSpace` | `parking_spaces` |
| Puesto de oficina | `Desk` | `desks` |
| Recurso reservable (abstracción) | `BookableResource` | — (se materializa en `parking_spaces` / `desks`) |
| Asignación fija | `FixedAssignment` | `fixed_assignments` |
| Solicitud | `Request` | `requests` |
| Liberación | `Release` | `releases` |
| Visitante | `Visitor` | `visitors` |
| Reserva de visitante | `VisitorReservation` | `visitor_reservations` |
| Registro de auditoría | `AuditLog` | `audit_log` |
| Registro de login | `LoginLog` | `login_log` |

### Enumerados

| Concepto | Enum (EN) | Valores |
|----------|-----------|---------|
| Tipo de recurso | `ResourceType` | `PARKING`, `DESK` |
| Categoría de puesto | `DeskCategory` | `STANDARD`, `EXECUTIVE` |
| Rol | `Role` | `ADMIN`, `EMPLOYEE` |
| Estado de solicitud | `RequestStatus` | `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED` |
| Tipo de liberación | `ReleaseType` | `VOLUNTARY`, `ADMINISTRATIVE` |
| Resultado de login | `LoginResult` | `OK`, `FAILED`, `NO_ACCESS`, `INACTIVE`, `FALLBACK_OK` |
| Fase de login | `LoginPhase` | `PHASE_1`, `PHASE_2`, `FALLBACK` |

### Campos comunes

| Concepto de negocio (ES) | Campo / Columna (EN) |
|--------------------------|----------------------|
| Día de la semana (1-7) | `day_of_week` |
| Activo (no dado de baja lógica) | `active` |
| Cuenta habilitada para login | `enabled` |
| Indicador corporativo (sí/no) | `is_corporate` |
| Creado por | `created_by_id` |
| Fecha de creación | `created_at` |
| Resuelto por | `resolved_by_id` |
| Fecha de resolución | `resolved_at` |
| Motivo de rechazo | `rejection_reason` |
| Revocada por | `revoked_by_id` |
| Fecha de revocación | `revoked_at` |
| Fecha (de la reserva/solicitud/liberación) | `date` |
| Referencia al recurso reservable | `resource_id` |
| Referencia a la plaza | `parking_space_id` |
| Nombre | `first_name` |
| Apellidos | `last_name` |
| Login / usuario | `login` |
| Departamento | `department` |
| Teléfono móvil | `mobile_phone` |
| Matrícula | `license_plate` |
| DNI / documento de identidad | `national_id` |
| Empresa | `company` |
| Motivo habitual | `usual_reason` |
| Observaciones | `notes` |
| Motivo | `reason` |
| Hash de contraseña | `password_hash` |
| Debe cambiar contraseña | `password_must_change` |

### Identificadores de contrato externo (NO traducir)

Estos elementos provienen de sistemas externos (landing ALEATICA, Spring) y **se mantienen tal cual**:

- Claims del JWT: `username`, `client_sid`, `iss=SSOTTS`, `aud=parking`, `exp`.
- Endpoints de la landing: `/ssocallback`, `/CloseSSOSessionID`.
- Cookie de sesión: `parking_SESSION`.
- Tabla de sesiones gestionada por el framework: `SPRING_SESSION`.
- Claves de configuración del prefijo `parking.*`.

---

## 🗂 Recurso reservable — concepto unificado

parking generaliza su modelo bajo el concepto de **recurso reservable** (`BookableResource`): una abstracción que engloba tanto las plazas de parking como los puestos de oficina.

| Aspecto | Plazas de parking (`PARKING`) | Puestos de oficina (`DESK`) |
|---------|-------------------------------|------------------------------|
| Identificación | Número de plaza (ej. `P-08`) | Número 1-65 |
| Categorías | — | `STANDARD` / `EXECUTIVE` |
| Vista visual | Lista numerada | Plano interactivo |
| Asignación fija | ✅ (por día de semana) | ✅ (por día de semana) |
| Solicitud puntual | ✅ | ✅ |
| Liberación | ✅ (voluntaria + administrativa) | ✅ (voluntaria + administrativa) |
| Reserva de visitante | ✅ | ❌ (no aplica) |

La lógica de **asignación fija**, **solicitud**, **liberación** y **cálculo de disponibilidad** es **compartida** entre ambos tipos y parametrizada por el tipo de recurso (`ResourceType`).

---

## ✨ Características

### Panel de Administración (Web, rol `ADMIN`)
- **Gestión de empleados** (`Employee`): alta, modificación, baja lógica y reactivación. Campos: `first_name`, `last_name`, `login`, `email`, `department`, `mobile_phone`, `license_plate`, `is_corporate`, `enabled`, `active`.
- **Reset administrativo de contraseña**:
  - Fase 1: el sistema genera una contraseña temporal que se muestra al admin en pantalla para que la entregue al empleado.
  - Fase 2: el sistema genera la contraseña temporal y se envía por email al empleado (la usará solo en caso de activarse el fallback de emergencia).
- **Gestión de plazas** (`ParkingSpace`): alta individual, modificación y configuración masiva del número total.
- **Gestión de puestos** (`Desk`): alta, modificación y activación/desactivación de los 65 puestos. Asignación de categoría (`STANDARD` / `EXECUTIVE`).
- **Asignación fija** (`FixedAssignment`) de plaza o puesto a empleado, indefinida en el tiempo, indicando los días de la semana (L-D) que aplica.
- **Revocación o modificación** de la asignación fija en cualquier momento (solo aplica desde la fecha actual; histórico previo intacto).
- **Vista calendario semanal completa** con todos los recursos, titulares y estados. Solo visible para el admin.
- **Gestión de solicitudes pendientes**: aprobar (asignando recurso concreto) o rechazar (con motivo obligatorio). Las solicitudes de plaza y puesto se aprueban/rechazan **por separado**. UI ordenada por fecha de creación (FIFO informativo).
- **Editor de plano**: posicionamiento visual de los puestos sobre la imagen de la planta arrastrando los marcadores.
- **Liberación administrativa**: el admin puede liberar el recurso fijo de un empleado para un día concreto si no se presenta. Queda registrada con tipo `ADMINISTRATIVE` y motivo.
- **Reservas para visitantes**: alta de ficha de visitante (reutilizable) y creación de reserva puntual para una fecha concreta.
- **Histórico y auditoría**: consulta de auditoría completa y logs de login.
- **Exportación a CSV/XLSX** del histórico.
- **Dashboard de verificación**: vista consolidada de plazas y asignaciones (sin métricas analíticas en el alcance actual).

### Portal del Empleado (Web responsive, rol `EMPLOYEE`)
- **Solicitud unificada**: solicitar **plaza de parking y/o puesto de oficina** para una misma fecha desde una sola pantalla. Se generan solicitudes independientes (una por recurso) que el admin aprueba/rechaza por separado.
- **Vista del plano** de puestos: visualización del estado de los 65 puestos para la fecha seleccionada. Permite pinchar un puesto libre para solicitarlo directamente.
- **Liberación voluntaria** de la propia plaza o puesto para un día concreto futuro.
- **Cancelación** de la propia solicitud mientras esté en estado `PENDING`.
- **Vista personal "Mi Semana"**: los propios recursos asignados y los huecos libres. **No se ven nombres de otros empleados**.
- **Mis solicitudes**: histórico personal con estados y motivos de rechazo.
- **Exportación a CSV/XLSX** de las solicitudes propias.
- **Idioma**: español (defecto) e inglés, conmutable.
- **Modo oscuro**: toggle manual.

---

## 🪑 Puestos de oficina

### Los 65 puestos

La planta de la oficina dispone de **65 puestos numerados** (1-65) reservables mediante parking. Solo estos puestos son gestionados como recursos; salas de reunión, servicios, cafetería y demás elementos del plano son decorativos y no reservables.

### Categorías de puesto (`DeskCategory`)

| Categoría | Descripción |
|-----------|-------------|
| **`STANDARD`** | Puesto normal, reservable por cualquier empleado. |
| **`EXECUTIVE`** | Puesto de dirección. Habitualmente se le asigna una asignación fija de lunes a viernes. Se distingue visualmente en el plano (color/estilo diferente). Es **liberable** exactamente igual que cualquier otro puesto: si el directivo no acude un día puede liberarlo y quedará disponible para solicitud ese día. |

### Plano interactivo

- El plano es una **imagen de la planta** con **marcadores** superpuestos, uno por puesto, posicionados mediante coordenadas relativas (`coord_x`, `coord_y`, % del ancho y alto de la imagen).
- Cada marcador se **colorea según el estado** del puesto para la fecha seleccionada: libre, asignado, solicitado (pendiente), mi puesto, liberado, `EXECUTIVE`.
- El empleado puede **pinchar un puesto libre** para solicitarlo directamente desde el plano, sin necesidad de usar el formulario de solicitud separado.
- El admin dispone de un **editor visual** para ajustar las posiciones de los marcadores arrastrándolos sobre la imagen. Las coordenadas resultantes se persisten en BD.
- El **parking no tiene plano**: las plazas se siguen gestionando como lista numerada.

### Solicitud unificada (plaza y/o puesto)

Desde una **sola pantalla** el empleado puede solicitar plaza de parking y/o puesto de oficina para la misma fecha. Por debajo se crean **solicitudes (`Request`) independientes** (una por recurso). El admin las aprueba o rechaza **por separado**: puede aprobar el puesto y rechazar la plaza, o viceversa. No existe entidad "solicitud agrupada"; la agrupación es únicamente una conveniencia de UI.

---

## 📜 Reglas de negocio

### Días válidos
Se puede solicitar un recurso **cualquier día del año**, incluidos sábados, domingos y festivos. No existe calendario laboral en el sistema.

### Ventana de solicitud
- Un empleado puede crear solicitudes desde **hoy** hasta **hoy + 14 días naturales** inclusive.
- Solicitar para el mismo día está permitido **sin límite horario**.

### Unicidad de solicitudes
- Máximo **una solicitud `PENDING`** por empleado, recurso y fecha. Intentar crear otra devuelve `409 Conflict`.
- Solicitudes `REJECTED` o `CANCELLED` no bloquean crear una nueva para esa fecha.

### Máquina de estados de la solicitud (`RequestStatus`)
- `PENDING` (inicial, `resource_id = NULL`): puede ser aprobada o rechazada por admin, o cancelada por el propio empleado.
- `APPROVED` (con `resource_id`, `resolved_by_id`, `resolved_at`): estado final.
- `REJECTED` (con `rejection_reason`, `resolved_by_id`, `resolved_at`): estado final.
- `CANCELLED`: cerrada por el empleado mientras estaba `PENDING`.

### Disponibilidad de un recurso en una fecha F
Un recurso está **disponible para F** si cumple **todo** lo siguiente:
1. El recurso está activo (`active = true`).
2. **No** tiene `FixedAssignment` activa con `day_of_week = dayOfWeek(F)`, **o** la tiene pero existe un `Release` para ese recurso y fecha F.
3. **No** existe ninguna `Request` en estado `APPROVED` para ese recurso y fecha F.
4. **(Solo plazas)** **No** existe ninguna `VisitorReservation` para esa plaza y fecha F.

### Asignación fija (`FixedAssignment`)
- Un recurso no puede estar asignado de forma fija a dos empleados distintos el **mismo día de la semana**.
- Un empleado no puede tener dos recursos del mismo tipo asignados el mismo día de la semana.
- Un mismo recurso sí puede compartirse entre empleados en días distintos.
- Modificar una asignación fija **no afecta a días pasados** ni a solicitudes ya aprobadas. Solo aplica desde la fecha actual.
- La revocación es lógica: la fila se marca `active = false` con `revoked_at` y `revoked_by_id`, no se borra.

### Aprobación / rechazo
- Al aprobar, el sistema valida la disponibilidad del recurso elegido. Si no está disponible, devuelve `409 Conflict`.
- Al rechazar, el `rejection_reason` es **obligatorio** (mínimo 5 caracteres).
- El admin ve las pendientes ordenadas por `created_at ASC` (FIFO informativo) pero decide libremente.

### Liberación (`Release`)
- **Voluntaria** (`VOLUNTARY`): solo el dueño de la asignación fija puede liberar, y solo para fechas presentes o futuras.
- **Administrativa** (`ADMINISTRATIVE`): el admin puede liberar el recurso fijo de cualquier empleado para cualquier fecha presente o futura, indicando motivo.

### Reglas específicas de puestos
- **RN-DESK-01**: un empleado puede tener asignación fija de plaza (`PARKING`) y asignación fija de puesto (`DESK`) simultáneamente; son recursos distintos.
- **RN-DESK-02**: los puestos `EXECUTIVE` se distinguen visualmente en el plano pero siguen las mismas reglas de liberación que los `STANDARD`.
- **RN-DESK-03**: solo los 65 puestos numerados son reservables; el resto de elementos del plano no se modelan como recursos.
- **RN-DESK-04**: al solicitar desde el plano, solo se pueden seleccionar puestos en estado "libre" para la fecha elegida.

### Multi-admin
- Pueden coexistir varios admins activos.
- Cualquier admin puede aprobar/rechazar cualquier solicitud.
- En la notificación de "nueva solicitud" se envía email a **todos los admins activos**.

---

## 🔄 Flujo de solicitudes

```
                           ┌──────────────┐
                           │   Empleado   │
                           │  crea sol.   │
                           └──────┬───────┘
                                  │
                                  ▼
                         ┌─────────────────┐
                         │     PENDING     │
                         │(resource_id=NULL)│
                         └────────┬────────┘
                                  │
        ┌─────────────────┬───────┴──────┬─────────────────┐
        │                 │              │                 │
   Admin aprueba    Admin rechaza   Empleado cancela     Caduca*
        │                 │              │
        ▼                 ▼              ▼
  ┌──────────┐      ┌──────────┐    ┌──────────┐
  │ APPROVED │      │ REJECTED │    │CANCELLED │
  └─────┬────┘      └─────┬────┘    └──────────┘
        │                 │
        ▼                 ▼
  📧 Email aprob.   📧 Email rech.
```
\*No hay caducidad automática en el alcance actual; el admin debe resolver manualmente.

📧 Cuando se crea la solicitud, se envía email a **todos los admins activos** informando de la nueva pendiente.

---

## 🔐 Autenticación — Fase 1: Login local 🟢

Durante la Fase 1 parking gestiona sus propias credenciales. Es una solución **temporal** sustituida por la integración con la landing en Fase 2.

### Características
- Login con **usuario + contraseña**, hash **BCrypt (coste 12)**.
- Cookie de sesión `parking_SESSION` con flags `HttpOnly`, `Secure`, `SameSite=Lax`.
- Persistencia de sesión sobre SQL Server (misma infraestructura que Fase 2).
- Bloqueo de cuenta tras **5 intentos fallidos consecutivos** durante 15 minutos.
- Política de contraseña:
  - Mínimo 10 caracteres.
  - Mayúscula + minúscula + dígito + símbolo.
  - Distinta del `login` y del email.
  - Sin caducidad obligatoria en Fase 1 (la rotación de 90 días aplica solo en Fase 2 para cuentas con fallback).
- **Cambio obligatorio** en el primer login tras un reset administrativo (`password_must_change = true`).

### Reset de contraseña en Fase 1
- El admin pulsa "Resetear contraseña" en la ficha del empleado.
- El sistema genera una contraseña aleatoria segura, la hashea y marca `password_must_change = true`.
- La contraseña en claro **se muestra al admin una sola vez** con botón de copia. La entrega al empleado es por canal directo (no por email en Fase 1).
- En el siguiente login, el empleado debe cambiarla antes de continuar.

### Comportamiento de autenticación esperado
- Login público que crea sesión y emite la cookie.
- Logout que invalida la sesión.
- Consulta del usuario autenticado (identidad + rol).
- Cambio de contraseña por el propio empleado.
- Reset administrativo de contraseña (solo `ADMIN`).

---

## 🔐 Autenticación — Fase 2: SSO ALEATICA 🔵

En Fase 2 parking se publica detrás de la landing corporativa ALEATICA y deja de tener pantalla de login propia.

### Reparto de responsabilidades
- **Landing ALEATICA → autentica**: decide internamente cómo verificar al usuario (EntraID para empleados corporativos, usuario/contraseña local de la landing para empleados de localizaciones sin EntraID). Emite un `id_token` JWT con `username`, `client_sid`, `iss=SSOTTS`, `aud=parking`, `exp`.
- **parking → autoriza**: valida la firma del JWT, busca el `username` en su propia tabla `employees` (campo `login`), decide acceso y rol. **No se hace provisioning automático**: el empleado debe haber sido dado de alta previamente por el admin.

### Algoritmo de autorización en `/ssocallback`
1. Validar firma, `iss`, `aud`, `exp` del `id_token`.
2. Extraer `username` del claim.
3. Buscar `Employee` por `login`:
   - **No existe** → 403 "Sin acceso a parking. Contacte con el administrador." Se registra en `login_log` con resultado `NO_ACCESS`.
   - **Existe pero `active = false`** → 403 "Cuenta inactiva." Resultado `INACTIVE`.
   - **Existe y activo** → continuar.
4. Tomar rol interno del campo `Employee.role` (`ADMIN` o `EMPLOYEE`). **El claim `roles` del JWT se ignora**.
5. Crear sesión, emitir cookie `parking_SESSION`, guardar atributos `employee_id`, `role`, `client_sid`, `slo_token`.
6. Redirigir 302 a `redirect_uri`.

### Flujo completo

```
1. Usuario abre https://parking.aleatica.com
2. Frontend consulta la identidad (con cookie de sesión)
3. Backend → 401 (sin cookie)
4. Frontend redirige a:
   https://ccodes.aleaticalabs.com/landpage/authorize?client_id=parking
       &redirect_uri=https://parking.aleatica.com
5. La landing autentica al usuario (EntraID o local)
6. Landing redirige a:
   https://parking.aleatica.com/ssocallback?id_token=...&client_id=parking&redirect_uri=...
7. Backend ejecuta el algoritmo de autorización (ver arriba)
8. Frontend autenticado; resto de llamadas viajan con la cookie
9. Cualquier 401 posterior → modal "Sesión expirada" → vuelta a la landing
```

### Single Logout desde la landing
```http
POST https://parking.aleatica.com/parking-api/CloseSSOSessionID
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
- El login local y el reset administrativo siguen existiendo pero condicionados:
  - El login local solo responde si el fallback de emergencia está activado. Si está desactivado, se rechaza.
  - El reset administrativo se reorienta a generar contraseña + email (no se muestra al admin).
- **No se eliminan columnas** de la tabla `employees`: el fallback se mantiene como capacidad latente.

### Fallback de emergencia (Fase 2)
- **Desactivado por defecto** en todos los entornos.
- Se activa por configuración en escenarios de indisponibilidad de la landing.
- **Cualquier empleado con contraseña válida** puede usarlo (no solo admins).
- **Rotación obligatoria cada 90 días** para cualquier cuenta con contraseña activa en Fase 2: si han pasado más de 90 días desde el último cambio, el sistema obliga a cambiarla antes de continuar.
- Cada uso del fallback se registra en `login_log` con fase `FALLBACK` y resultado `FALLBACK_OK` (o el resultado de fallo correspondiente).

### Datos pendientes para Fase 2
- Clave/secreto para validar la firma del `id_token`.
- URLs definitivas para entornos PRE y PRO.
- Especificación exacta del servicio `consultaporlogin` (se recibirá de ALEATICA).

---

## 🚗 Reservas para visitantes

El admin puede reservar una plaza para una persona que **no es empleado** de ALEATICA. Los visitantes no acceden a la aplicación, no tienen cuenta y no reciben emails.

### Modelo
Dos entidades separadas para permitir reuso de la ficha del visitante en futuras visitas:

- **`Visitor`** (ficha reutilizable): `id`, `first_name`, `last_name`, `national_id` (único), `license_plate`, `company` (opcional), `usual_reason` (opcional), `created_by_id`, `created_at`.
- **`VisitorReservation`** (instancia de reserva): `id`, `visitor_id` (FK), `parking_space_id` (FK), `date`, `notes`, `created_by_id` (FK `Employee` con rol `ADMIN`), `created_at`.

### Reglas
- Una `VisitorReservation` ocupa la plaza para esa fecha en el cálculo de disponibilidad (cuenta como plaza no disponible).
- No tiene estados ni flujo de aprobación: la crea directamente el admin.
- El admin puede:
  - Crear una ficha de visitante nueva.
  - Buscar visitante existente por `national_id`, `first_name`, `last_name` o `license_plate` y reusarlo.
  - Editar la ficha (afecta solo a futuras reservas).
  - Anular reservas futuras (no las pasadas).
- No genera notificaciones por email (el visitante no tiene cuenta).
- Cada acción del admin sobre visitantes y reservas queda en `audit_log`.

---

## 🌐 Internacionalización y UI

- **Idiomas**: español (defecto) e inglés, conmutable desde la aplicación.
- **Modo oscuro**: toggle manual en la aplicación (no detección automática).
- **Responsive**: mobile-first. Sin PWA ni apps nativas en el alcance actual.
- **Mockups**: ALEATICA proporcionará los mockups del UI antes de la fase de implementación de pantallas.

---

## 🗒 Notas clave del modelo

Convenciones críticas que afectan a toda la aplicación:

- **`day_of_week` es 1-7** (1=Lunes … 7=Domingo), no 1-5.
- **Unicidades activas** mediante *filtered indexes* de SQL Server (cláusula `WHERE active = 1` o `WHERE status = 'PENDING'`).
- **Revocaciones lógicas**: nunca se borran filas de `FixedAssignment`; se marcan `active = false`.
- **`audit_log`** se rellena automáticamente sobre los servicios anotados (auditoría transversal).
- **`login_log`** se rellena en el filtro de autenticación; cubre Fase 1, Fase 2 y fallback.

---

## 🔍 Auditoría y retención

### Qué se audita
- **Acciones de admin**: alta/modificación/baja de empleados, plazas, puestos, asignaciones fijas, reservas de visitante, aprobaciones, rechazos, liberaciones administrativas, reset de contraseña.
- **Acciones de empleado**: creación y cancelación de solicitudes propias; liberaciones voluntarias.
- **Logins**: todos los intentos (OK y fallidos), separados en `login_log` para no contaminar la auditoría funcional (`audit_log`).

### Retención y purga
- **Datos vivos** (entidades activas: `Employee`, `ParkingSpace`, `Desk`, `FixedAssignment` activa, `Visitor`): **sin purga**.
- **Datos históricos** (`Request` cerradas, `Release`, `VisitorReservation`, `audit_log`, `login_log`): **purga automática a 2 años** desde su `created_at`.
- Implementación: job programado diario que ejecuta borrado en lotes para evitar bloqueos prolongados.
- La política de retención es ajustable por entorno (clave de configuración `parking.retention.years`).

---

## 📧 Notificaciones por email

| Evento | Destinatario | Plantilla |
|--------|--------------|-----------|
| Nueva solicitud creada | Todos los admins activos | `request-created.html` |
| Solicitud aprobada | Empleado solicitante | `request-approved.html` |
| Solicitud rechazada | Empleado solicitante | `request-rejected.html` |
| Asignación fija revocada | Empleado afectado | `assignment-revoked.html` |
| Reset de contraseña (solo Fase 2) | Empleado afectado | `password-reset.html` |

### Reglas de envío
- Envío **`AFTER_COMMIT`** de la transacción que dispara el evento: si la transacción falla, no se manda email.
- Si el envío SMTP falla, se registra en log y se reintenta mediante un job programado. **Nunca debe revertir la operación funcional**.
- No se envían correos al liberar un recurso voluntariamente ni al cancelar la propia solicitud.

---

## 🗺 Roadmap funcional

El desarrollo se descompone en bloques funcionales autónomos. Cada bloque es un incremento entregable con su propio diseño, specs y tareas. La secuencia respeta las dependencias entre capacidades.

### Plazas de parking (núcleo)

| # | Capacidad | Descripción | Depende de |
|---|-----------|-------------|------------|
| 1 | `auth-local` | Autenticación local Fase 1: login, logout, identidad, cambio de contraseña. | — |
| 2 | `employees` | CRUD de empleados, RBAC, reset de contraseña. | `auth-local` |
| 3 | `parking-spaces` | CRUD de plazas de parking. | — |
| 4 | `requests` | Solicitud, aprobación y rechazo de recursos. | `parking-spaces`, `employees` |
| 5 | `fixed-assignments` | Asignación fija recurso ↔ empleado por día de semana. | `parking-spaces`, `employees` |
| 6 | `releases` | Liberación voluntaria y administrativa. | `fixed-assignments` |
| 7 | `availability-calendar` | Cálculo y vista de disponibilidad semanal. | `requests`, `releases` |
| 8 | `visitors` | Fichas de visitante y reservas de plaza puntual. | `parking-spaces` |
| 9 | `notifications` | Envío de emails en eventos de solicitud. | `requests` |

### Puestos de oficina (alcance ampliado)

| # | Capacidad | Descripción | Depende de |
|---|-----------|-------------|------------|
| 10 | `generic-resource-refactor` | Generalizar `ParkingSpace`, `Request`, `FixedAssignment`, `Release` y disponibilidad al concepto `BookableResource` (`PARKING` / `DESK`). Sin cambio de comportamiento visible. | Bloques 1-9 |
| 11 | `desks` | Entidad `Desk` (número 1-65, categorías `STANDARD`/`EXECUTIVE`, coordenadas). CRUD, asignación fija y solicitud de puestos. | `generic-resource-refactor` |
| 12 | `floor-plan` | Plano interactivo: imagen de planta, marcadores por estado, solicitud desde plano, editor visual de posicionamiento. | `desks` |
