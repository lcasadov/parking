=== ⚠️ CORRECCIONES GLOBALES — LEER Y APLICAR ANTES DE EJECUTAR CUALQUIER PROMPT DE ESTE FICHERO ===

> Estos prompts (5-8 + briefs) vienen de una plantilla y **no estaban alineados con las decisiones ya tomadas en parking**. Al ejecutar cualquiera, **estas correcciones prevalecen** sobre el texto del prompt.

1. **Nomenclatura** (autoridad: README "Nomenclatura del código"): prosa en español, **identificadores de código en INGLÉS**. Traduce todo lo que los prompts ponen en español como código:
   - **Capabilities / carpetas** `openspec/specs/<cap>/`: `empleados→employees`, `plazas→parking-spaces`, `asignaciones-fijas→fixed-assignments`, `liberaciones→releases`, `solicitudes→requests`, `visitantes→visitors`, `disponibilidad-calendario→availability-calendar`, `auditoria-retencion→audit-retention`, `notificaciones-email→notifications`, `exportaciones→exports`. (`auth-local`, `auth-sso` se mantienen.)
   - **Entidades**: `Empleado→Employee`, `Plaza→ParkingSpace`, `Solicitud→Request`, `AsignacionFija→FixedAssignment`, `Liberacion→Release`, `ReservaVisita→VisitorReservation`.
   - **Endpoints**: la autoridad es `docs/openapi.yaml` (`/requests`, `/employees`, `/parking-spaces`…). `/auth/cambiar-password → /auth/change-password`.
   - **Rol** `EMPLEADO→EMPLOYEE`; **estados** `PENDIENTE→PENDING`, `APROBADA→APPROVED`, etc.
2. **Stack**: **Java 21 LTS** (NO 22). Spring Boot 3.3, SQL Server 2022, Tomcat 10.1 (WAR), Hibernate 6.5, Flyway 10.
3. **Frontend = design system propio** (CSS propio + variables + Tabler Icons; ver `docs/mockups/styles.css`). **NO Tailwind, NO MUI, NO shadcn/ui, NO postcss.**
   - Prompt 6 genera `docs/design-system.md` a partir de `docs/mockups/styles.css` (los tokens ya son variables CSS); ignora/convierte la sección de `tailwind.config.js`.
   - `frontend-bootstrap`: estado **TanStack Query** (servidor) + **React Context** (auth/tema/idioma); i18n **react-i18next**; build/test **Vite + Vitest + RTL**; E2E **Playwright**. React Router y Axios sí.
4. **Documentos**: `docs/PRD.md` **NO existe** → usa `docs/PROJECT.md` (es el PRD; incluye **Anexo A** técnico). "Módulos a implementar" = roadmap del README + Anexo A.
5. **RN-xx NO existen como catálogo numerado.** No inventes códigos `RN-01…`. Referencia las reglas por descripción del README §"Reglas de negocio" (ventana 14 días; unicidad PENDING→`409`; disponibilidad→`409`; motivo de rechazo ≥5; email `AFTER_COMMIT`).
6. **Forma de error** (autoridad `openapi.yaml`): `{ error, message, fields, timestamp }` (EN). NO `{ mensaje, campos }`. Códigos: **409** conflicto/unicidad/disponibilidad; **400** validación de campos. Usa 422 solo si `openapi.yaml` lo refleja.
7. **Mockups**: hay **25** en `docs/mockups/` (no 7). El "design system" es `docs/mockups/styles.css`. Imagen de plano en `docs/assets/`. Rutas reales de docs: `docs/mockups/`, `docs/ui-screens.md`, `docs/ux-flows.md`, `docs/design-system.md` (NO `docs/ux/...`).
8. **Identidad git**: usuario **`lcasadov`** (sin bot `orquestadoria`). `PR_REVIEWER` = `lcasadov`. Ramas **`feat/<área>/<issue>-<slug>`**. Campo de esfuerzo Project v2 = **`Effort (h)`**.
9. **SMTP**: **Ethereal es un servicio hosted** (`smtp.ethereal.email`) configurado vía `.env` (`SMTP_*`); **NO se dockeriza**. `docker-compose` levanta **SQL Server** (+ MailHog opcional si quieres UI de correo local). Corrige cualquier "Ethereal en docker".
10. **Estado de los entregables**:
    - `docs/ui-screens.md` (Prompt 7) y `docs/ux-flows.md` (Prompt 8) **YA EXISTEN** y están alineados (formato `## N.`, 24 pantallas + flujos). Ejecutar los Prompts 7/8 los **regeneraría** en otro formato (`S-XX/M-XX/F-XX`). **Decisión**: conservar los actuales (recomendado) o regenerar en formato `S-XX`; si los regeneras, respáldalos antes.
    - `docs/design-system.md` (Prompt 6) **NO existe** → pendiente, ya corregido a CSS propio.
    - El último prompt (enriquecer specs con UI) usa `S-XX/M-XX/F-XX`: si conservas el `ui-screens.md` actual (formato `## N.`), adapta esas referencias.
11. **`motivos-rechazo`** como capability/pantalla configurable es **especulativo**: hoy el rechazo usa el enum `rejection_reason_code` (`NO_AVAILABILITY`/`OUTSIDE_POLICY`/`OTHER`) + texto libre (ver `data-model.md`/`openapi.yaml`). No crees esa capability/pantalla salvo confirmación de negocio.
12. La lista canónica de capabilities está en el **roadmap del README** (incluye el alcance ampliado de puestos: `generic-resource-refactor`, `desks`, `floor-plan`, que estos prompts no cubren).

=== FIN CORRECCIONES GLOBALES ===

=== INICIO DEL PROMPT 5 ===

# Misión

Genera la estructura inicial de **OpenSpec** para parking:
- `openspec/config.yaml` — contexto y reglas del proyecto.
- `openspec/specs/<capability>/spec.md` — un spec por cada capability
  funcional, con escenarios BDD.

Esto sustituye el típico documento `use-cases.md`: en OpenSpec, los
casos de uso viven dentro de cada spec de capability como **escenarios
Given/When/Then**.

# Entradas

- `README.md` adjunto (roadmap = capabilities; sección "Reglas de negocio").
- `docs/PROJECT.md` adjunto (PRD ejecutivo + **Anexo A** técnico; `docs/PRD.md` NO existe).
- `docs/data-model.md` adjunto (entidades por capability, nombres en inglés).
- `docs/security-design.md` adjunto (RBAC por capability).
- `docs/openapi.yaml` adjunto (endpoints por capability — **autoridad**).

# Capabilities a generar

Una capability por módulo funcional (12):

1. `auth-local` (🟢 Fase 1)
2. `auth-sso` (🔵 Fase 2)
3. `employees`
4. `parking-spaces`
5. `fixed-assignments`
6. `releases`
7. `requests`
8. `visitors`
9. `availability-calendar`
10. `audit-retention`
11. `notifications`
12. `exports`

# Estructura del output
openspec/
├── config.yaml
└── specs/
├── auth-local/spec.md
├── auth-sso/spec.md
├── employees/spec.md
├── parking-spaces/spec.md
├── fixed-assignments/spec.md
├── releases/spec.md
├── requests/spec.md
├── visitors/spec.md
├── availability-calendar/spec.md
├── audit-retention/spec.md
├── notifications/spec.md
└── exports/spec.md
## `openspec/config.yaml`

```yaml
project:
  name: parking
  description: Gestión de plazas de parking ALEATICA
  owner: ALEATICA
  language: es

stack:
  backend: Spring Boot 3.3 (Java 21 LTS) · WAR sobre Tomcat 10.1
  frontend: React 18 + Vite 5 (design system propio, sin Tailwind)
  database: SQL Server 2022
  orm: Spring Data JPA + Hibernate 6.5

conventions:
  api_base_path: /api/v1
  api_spec: docs/openapi.yaml
  data_model: docs/data-model.md
  security_design: docs/security-design.md
  testing_strategy: docs/TESTING-STRATEGY.md

roles:
  - ADMIN
  - EMPLOYEE

phases:
  - id: fase-1
    name: Login local
    status: active
  - id: fase-2
    name: SSO ALEATICA
    status: planned

capabilities:
  - auth-local
  - auth-sso
  - employees
  - parking-spaces
  - fixed-assignments
  - releases
  - requests
  - visitors
  - availability-calendar
  - audit-retention
  - notifications
  - exports

business_rules:
  source: README.md  # sección "Reglas de negocio" (descriptivas; NO hay catálogo RN-xx)
```

## Estructura de cada `spec.md`

Plantilla común para cada capability:

```markdown
# Capability: <nombre>

## Resumen
<2-3 líneas: qué hace esta capability>

## Fase
🟢 Fase 1 | 🔵 Fase 2 | 🟢🔵 ambas

## Reglas de negocio implicadas
- (referencia descriptiva al README §"Reglas de negocio"; **NO hay códigos RN-xx**)

## Entidades implicadas
- Employee
- ParkingSpace
- (...)

## Endpoints
- GET /api/v1/...
- POST /api/v1/...

## Permisos
| Rol | Permisos |
|---|---|
| ADMIN | ... |
| EMPLOYEE | ... |

## Requirements

### Requirement 1: <nombre>
**El sistema DEBE <comportamiento>.**

#### Scenario: <caso feliz>
- **GIVEN** <precondición>
- **AND** <precondición>
- **WHEN** <acción>
- **THEN** <resultado>
- **AND** <resultado>

#### Scenario: <caso de error>
- **GIVEN** ...
- **WHEN** ...
- **THEN** ...

### Requirement 2: <nombre>
...

## Casos límite (edge cases)
- <caso 1>
- <caso 2>

## Dependencias con otras capabilities
- Depende de `auth-local` para autenticación.
- Notifica a `notifications` al aprobar.
```

# Cómo generar los escenarios BDD

Para cada capability, genera **al menos 3 requirements** con **al menos
2 scenarios cada uno** (uno feliz + uno de error). Cubre estos casos
explícitamente cuando sean relevantes:

- Camino feliz.
- Validación de campos (input inválido).
- Autorización (rol incorrecto).
- Reglas de negocio violadas (mensaje de error claro citando la regla del
  README; **no hay códigos RN-xx**).
- Conflictos (409).
- Idempotencia (si aplica).

# Ejemplo: capability `requests`

```markdown
# Capability: requests

## Resumen
Gestión de solicitudes puntuales de recurso por parte de empleados.
Incluye creación, cancelación, aprobación y rechazo.

## Fase
🟢🔵 ambas

## Reglas de negocio implicadas
(README §"Reglas de negocio", por descripción — NO hay códigos RN-xx)
- Días válidos (cualquier día del año).
- Ventana de solicitud: hoy..hoy+14 días.
- Unicidad: máx. una solicitud `PENDING` por empleado y fecha.
- Disponibilidad del recurso al aprobar.
- Motivo de rechazo obligatorio (≥5 caracteres).
- Notificación por email a todos los admins activos.
- Email `AFTER_COMMIT`.

## Entidades implicadas
- Request, Employee, ParkingSpace, FixedAssignment, Release, VisitorReservation

## Endpoints
- POST /api/v1/requests
- GET /api/v1/requests/mine
- GET /api/v1/requests/pending
- POST /api/v1/requests/{id}/cancel
- POST /api/v1/requests/{id}/approve
- POST /api/v1/requests/{id}/reject

## Permisos
| Rol | Permisos |
|---|---|
| EMPLOYEE | Crear/cancelar/ver propias |
| ADMIN | Ver pendientes, aprobar, rechazar |

## Requirements

### Requirement 1: Crear solicitud
**El sistema DEBE permitir a un empleado crear una solicitud para una
fecha dentro de la ventana de 14 días, garantizando unicidad por
empleado-fecha.**

#### Scenario: Empleado crea solicitud válida para mañana
- GIVEN un empleado autenticado sin solicitud `PENDING` para mañana
- WHEN crea una solicitud con `requestedDate = mañana`
- THEN el sistema responde 201 con `status = PENDING`
- AND notifica por email a todos los admins activos
- AND la fila queda en `requests` con `parking_space_id = NULL`

#### Scenario: Empleado intenta crear solicitud duplicada
- GIVEN un empleado con solicitud `PENDING` para la fecha F
- WHEN intenta crear otra solicitud para la fecha F
- THEN el sistema responde 409 con `error = "REQUEST_ALREADY_PENDING"`
- AND el mensaje cita la regla de unicidad

#### Scenario: Empleado solicita fuera de la ventana
- GIVEN un empleado autenticado
- WHEN crea una solicitud con `requestedDate = hoy + 15 días`
- THEN el sistema responde 400 con `error = "OUTSIDE_REQUEST_WINDOW"`
- AND el mensaje cita la regla de la ventana de 14 días

### Requirement 2: Aprobar solicitud
...

### Requirement 3: Rechazar solicitud con motivo
...

## Casos límite
- Empleado con asignación fija que pide otra fecha donde su recurso está
  liberado: se trata como cualquier otra solicitud.
- Admin se aprueba a sí mismo: permitido (es un empleado más con rol
  `ADMIN`).
- Concurrencia: dos admins aprueban a la vez el mismo recurso-fecha →
  el segundo recibe 409 (filtered index).

## Dependencias
- Requiere `auth-local` (empleado autenticado).
- Consulta `availability-calendar` al aprobar.
- Dispara `notifications` en creación, aprobación y rechazo.
```

# Restricciones de generación

- Cada capability con al menos **3 requirements** y **6 scenarios**
  totales mínimo.
- Cada scenario en formato Given/When/Then estricto (no narrativo).
- Referencia las reglas de negocio por descripción (README); **NO inventes códigos RN-xx**.
- No copies código ni DDL — referencia `data-model.md` y `openapi.yaml`.
- **Idioma**: prosa en español; identificadores de código (entidades, enums,
  endpoints, roles, estados) en **inglés** (README "Nomenclatura del código").
- Si una capability es muy pequeña (ej. `notifications`), igual
  necesita 3 requirements: envío exitoso, fallo SMTP, reintento.

# Salida esperada

Genera **13 archivos** (`config.yaml` + 12 `spec.md`). Si no caben en
una respuesta, dame primero `config.yaml` + las 6 primeras capabilities,
luego en mi siguiente turno las 6 restantes.


Genera una copia de cada capability en change poniendo el prefijo init- a la capability con la estructura de change.
=== FIN DEL PROMPT 5 ===

=== INICIO DEL PROMPT 6 ===

# Misión

Genera `docs/design-system.md` — el sistema de diseño canónico de parking.
Lo consume `frontend-engineer` para implementar componentes consistentes
sin reinventar tokens en cada pantalla.

# Entradas

- 7 mockups HTML adjuntos:
  `index.html`, `01-calendario-semanal.html`, `02-solicitudes-pendientes.html`,
  `03-empleados-asignacion-fija.html`, `04-modal-edicion-empleado.html`,
  `05-modal-aprobar-solicitud.html`, `06-modal-rechazar-solicitud.html`,
  `07-empleado-movil.html`.
- `README.md` (i18n, modo oscuro).
- `docs/PROJECT.md` (stack React 18 + Tailwind 3).

# Contrato del documento

- Idioma: español.
- **Fuente única de verdad para los tokens**: si en el código aparece un
  color hex distinto del declarado aquí, el código está mal.
- Cada token debe poder mapearse a una variable CSS y a una clase
  Tailwind (extender `tailwind.config.js`).
- Sin código React; sí ejemplos de uso en JSX/HTML cortos.
- Pensado para **soportar modo oscuro** (toggle manual): cada token
  necesita su par light/dark.

# Secciones obligatorias

## 1. Identidad de marca

- Familia QRIA (ALEATICA): conjunto de aplicaciones corporativas.
- parking es una de ellas.
- Logo: círculo cónico verde → teal → azul (extraer de los mockups).
- Header con curva decorativa verde + naranja en SVG.
- Tono: institucional sobrio, no juguetón.

## 2. Tokens de color

Extrae los colores del CSS de `index.html` y los mockups. Organiza así:

### Paleta base (light mode)
| Token | Hex | Uso |
|---|---|---|
| `--bg-page` | `#f5f4ef` | Fondo de área de contenido |
| `--bg-card` | `#ffffff` | Fondo de cards y tablas |
| `--bg-soft` | `#fafaf7` | Fondo de cabeceras suaves |
| `--border` | `rgba(0,0,0,0.12)` | Bordes finos |
| `--border-strong` | `rgba(0,0,0,0.2)` | Bordes principales |
| `--text` | `#444441` | Texto primario |
| `--text-muted` | `#888780` | Texto secundario |
| `--text-soft` | `#5f5e5a` | Texto en cells |

### Paleta corporativa
| Token | Hex | Uso |
|---|---|---|
| `--green` | `#639922` | Color de marca principal |
| `--green-dark` | `#3b6d11` | Hover/active |
| `--green-soft` | `#eaf3de` | Backgrounds suaves verdes |
| `--green-border` | `#c0dd97` | Bordes suaves verdes |
| `--teal` | `#1d9e75` | Acción secundaria (exportar, success) |
| `--blue` | `#378add` | Acción informativa, filtros |
| `--red` | `#e24b4a` | Acción destructiva |

### Paleta semántica
Mapeo de cada estado del dominio a un color:

| Estado | Background | Border | Text |
|---|---|---|---|
| Asignada / Aprobada | `--green-soft` | `--green-border` | `--green-darker` |
| Liberada | `--pink-soft` | `--pink-border` | `--pink-text` |
| Pdte. asignar (visual) | `--amber-soft` | `--amber-border` | `--amber-text` |
| Solicitud (aprobada esa fecha) | `--blue-soft` | `--blue-border` | `--blue-darker` |
| Libre | `#f1efe8` | `#d3d1c7` | `--text-soft` |
| Rechazada / Error | `--red-soft` | `--red-border` | `--red-text` |
| Pendiente | `--amber-soft` | `--amber-border` | `--amber-text` |

### Paleta de avatares (decorativos, no semánticos)
Lista de combinaciones background/text para iniciales de avatares:
`av-purple`, `av-teal`, `av-pink`, `av-amber`, `av-blue`, `av-coral`.
Extrae los hex de `index.html`.

### Modo oscuro
Para cada token de la paleta base, proporciona un equivalente dark con
suficiente contraste WCAG AA. Los tokens corporativos (verde, teal,
azul, rojo) pueden mantenerse iguales si superan el ratio en dark.

## 3. Tipografía

- **Familia**: stack del sistema (no Google Fonts en MVP):
  `-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, sans-serif`.
- Escala (extrae de los mockups):
  | Token | Tamaño | Uso |
  |---|---|---|
  | `text-xs` | 10-11px | Subtítulos, badges, hints |
  | `text-sm` | 12-13px | Texto base de UI |
  | `text-base` | 14px | Texto cuerpo |
  | `text-lg` | 16px | Encabezados sección |
  | `text-xl` | 18px | Título de página |
  | `text-2xl` | 22px | Títulos H1 |
- Pesos: **400 (normal), 500 (énfasis), nunca 700**. Bold se sustituye
  por weight 500 + color contrastante.
- Line height: 1.4-1.6 según contexto.

## 4. Espaciado y radios

- Escala 4-8-12-16-20-24-32 px.
- Border radius:
  - `radius-sm` 4px (badges, pills internas)
  - `radius-md` 6px (botones, inputs)
  - `radius-lg` 8px (cards, tables)
  - `radius-xl` 12px (modales, contenedores principales)
- Sombras: prácticamente ninguna en mockups, solo borde fino
  `0.5px solid var(--border)`. Modal con `box-shadow: 0 0 0 0.5px`.

## 5. Iconografía

- Librería: **Tabler Icons** vía webfont `@tabler/icons-webfont`.
- Clase: `ti ti-<nombre>` (ej. `ti-edit`, `ti-check`, `ti-x`,
  `ti-calendar`, `ti-filter`, `ti-chevron-down`).
- Tamaños: 14px (inline), 16px (botones), 18px (header).
- Color: hereda del contenedor por defecto; verde para acciones
  primarias del sidebar.

## 6. Componentes

Para cada componente, incluye:
- **Anatomía** (descripción de partes).
- **Estados** (default, hover, focused, disabled, error).
- **Variantes** (primary, secondary, etc.).
- **Reglas de uso** (cuándo SÍ y cuándo NO usarlo).
- **Ejemplo HTML mínimo**.

Componentes obligatorios (extraer de los mockups):

### 6.1 Botones
- `btn-green` (verde, acción positiva principal)
- `btn-blue` (azul, acción informativa)
- `btn-red` (rojo, acción destructiva)
- `btn-white` (neutro, secundario)
- `btn-success` (verde compacto para acciones in-row)
- `btn-danger-outline` (rojo perfilado, in-row)
- `btn-icon-only` (botón de 32px solo icono)

### 6.2 Tablas
- Cabecera con fondo `#444441` y texto blanco.
- Filas con borde inferior `0.5px solid var(--border)`.
- Fila destacada: clase `highlighted` con fondo `#fcf6e6`.
- Padding de celda: 10px 14px.
- Grid CSS para columnas (no `<th>/<td>` semánticos en los mockups,
  pero el frontend final SÍ debe usar HTML semántico).

### 6.3 Pills (badges de estado)
Tabla con cada variante y cuándo usarla:
- `pill-green` (APROBADA, asignada)
- `pill-amber` (PENDIENTE)
- `pill-pink` (RECHAZADA, liberada)
- `pill-blue` (informativa)
- `pill-gray` (CANCELADA, neutra)

### 6.4 Cells de calendario
Variantes para el calendario semanal del admin:
- `cell-assigned`, `cell-released`, `cell-pending`, `cell-request`,
  `cell-free`.
- Cada uno con fondo + texto consistentes con la paleta semántica.

### 6.5 Day chips
Botones cuadrados 28×28px para días de la semana en la tabla de
empleados.
- `day-chip on` (verde sólido, día asignado).
- `day-chip off` (gris perfilado, día libre).

### 6.6 Day cards (modal de edición)
Tarjetas de selección de día en el modal de empleado.
- Default: borde gris, fondo blanco.
- Selected: borde verde 1.5px, fondo `--green-soft`, check verde.

### 6.7 Avatares
- Tamaños: `avatar` (34px header), `avatar-sm` (30px tablas),
  `phone-avatar` (26px móvil).
- Iniciales con paleta decorativa (sección 2).

### 6.8 Search box
Input con icono `ti-search` a la izquierda, ancho 240-280px.

### 6.9 Tabs
- Pill-style con borde fino.
- Active: fondo blanco + texto oscuro.
- Inactive: transparente + texto muted.
- Variante con contador: `<span class="count">5</span>`.

### 6.10 Info banners
4 variantes:
- `info-banner blue` (info neutra: "Se enviará email a...")
- `info-banner green` (confirmación: "Pepe tendrá la plaza...")
- `info-banner red` (advertencia destructiva)
- `info-banner amber` (no presente en mockups pero por simetría)

### 6.11 Form fields
- `field-label` (verde, 11px, letra de etiqueta).
- `field-label.red` (variante para campos en error).
- `field-value` (borde gris, padding 8×10).
- `field-value.readonly` (fondo `#f9f8f4`).
- `field-value.focused` (borde verde).
- `field-value.danger` (borde rojo).
- `field-value.with-icon` (con chevron a la derecha).
- `textarea` (mínimo 64px, redimensionable verticalmente).

### 6.12 Modales
- Anchura: 720px estándar, 480px (`narrow`) para confirmaciones.
- Header verde (positivo) o rojo (destructivo), 48px de alto, texto
  blanco.
- Tabs internas con underline verde.
- Footer con botones alineados a la derecha, fondo `--bg-soft`.
- Overlay: `rgba(0,0,0,0.45)`.

### 6.13 Sidebar
- 200px de ancho, fondo blanco.
- Item activo: fondo `--green-soft`, borde izquierdo verde 3px,
  icono verde, peso 500.
- Badge rojo opcional `badge-red` (contador en sidebar).

### 6.14 App header
- 64px de alto, fondo blanco, con curva decorativa SVG (verde + naranja).
- Logo cónico + nombre parking + subtítulo ALEATICA.
- Título de página y avatar de usuario a la derecha.

### 6.15 Phone frame (móvil)
- 300px de ancho, border-radius 24px, marco oscuro `box-shadow 0 0 0 6px #2c2c2a`.
- Header de 54px con curva SVG.
- Cards de semana (`week-card`) con borde izquierdo de color por estado.

## 7. Layout

- Desktop: sidebar 200px + main flex.
- Mobile: stack vertical, header 54px, sin sidebar (drawer si necesario).
- Breakpoints sugeridos:
  - `sm` 640px (móvil/tablet pequeña)
  - `md` 768px (tablet)
  - `lg` 1024px (escritorio)
  - `xl` 1280px (escritorio amplio)
- Container principal: max-width 1300px, margin auto.

## 8. Internacionalización

- Todos los textos visibles deben venir de claves i18n.
- Claves agrupadas por dominio: `common.*`, `auth.*`, `empleados.*`,
  `solicitudes.*`, `calendario.*`, `visitantes.*`.
- Fallback en español, traducción inglesa obligatoria.
- Fechas con `date-fns` y locale dinámico.

## 9. Accesibilidad

- Contraste mínimo WCAG AA en todos los tokens de texto sobre fondo.
- Focus visible: outline 2px verde con offset 2px.
- Labels asociados a inputs vía `htmlFor`.
- Modales con `role="dialog"` y `aria-labelledby`.
- Iconos decorativos: `aria-hidden="true"`.
- Iconos funcionales (botones icon-only): `aria-label` obligatorio.
- Estados de error en formularios anunciados con `role="alert"`.

## 10. Modo oscuro

- Toggle manual (no detección automática del SO).
- Aplicación con clase `.dark` en `<html>`.
- Persistir preferencia en `localStorage`.
- Todos los componentes deben verificarse en ambos modos antes de
  considerarse terminados.

## 11. Configuración Tailwind

Bloque de ejemplo para `tailwind.config.js`:

```js
module.exports = {
  darkMode: 'class',
  content: ['./src/**/*.{js,jsx,ts,tsx,html}'],
  theme: {
    extend: {
      colors: {
        page: 'var(--bg-page)',
        card: 'var(--bg-card)',
        green: {
          DEFAULT: 'var(--green)',
          dark: 'var(--green-dark)',
          soft: 'var(--green-soft)',
          border: 'var(--green-border)',
        },
        // ... resto
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif'],
      },
      borderRadius: {
        sm: '4px', md: '6px', lg: '8px', xl: '12px',
      },
    },
  },
};
```

## 12. Pendientes

Lista numerada de información que dejaste sin completar.

# Restricciones de generación

- No inventes colores que no estén en los mockups.
- Si dos tokens tienen el mismo valor, consolida en uno solo.
- No incluyas tokens que no aparezcan en ninguna pantalla.
- Cada componente debe estar respaldado por al menos un mockup.

=== FIN DEL PROMPT 6 ===

=== INICIO DEL PROMPT 7 ===

# Misión

Genera `docs/ui-screens.md` — el catálogo canónico de pantallas de
parking. Es el "mapa de pantallas" que el `frontend-engineer` usará para
saber exactamente qué construir, en qué ruta, con qué endpoints y para
qué rol.

# Entradas

- 7 mockups HTML adjuntos.
- `docs/openapi.yaml` adjunto (para ligar pantallas a endpoints).
- `docs/PROJECT.md` (rutas base, módulos, capabilities).
- `docs/security-design.md` (matriz RBAC).
- `openspec/specs/` (capabilities).

# Contrato del documento

- Idioma: español.
- Cada pantalla con ficha estandarizada y consistente.
- Trazabilidad bidireccional: cada pantalla → capability → endpoints.
- Pensado para que un desarrollador pueda implementar la pantalla solo
  leyendo su ficha (sin tener que volver al mockup salvo para detalles
  de pixel).

# Estructura del documento

## 1. Convenciones de routing

- Base path del frontend: `/`.
- Rutas admin: `/admin/<recurso>` (ej. `/admin/empleados`,
  `/admin/solicitudes`).
- Rutas empleado: `/empleado/<recurso>` o `/me/<recurso>`
  (ej. `/empleado/mi-semana`, `/empleado/solicitar`).
- Login: `/login` (Fase 1) o redirección automática a landing (Fase 2).
- Detalle de recurso: `/admin/<recurso>/:id`.
- Modales: NO son rutas propias; se abren sobre la pantalla padre y
  reflejan estado en query string opcional `?modal=editar&id=X`.

## 2. Inventario de pantallas

Para cada pantalla, **ficha estandarizada**:

```markdown
### S-XX · <Nombre>

- **Ruta**: `/admin/...` o `/empleado/...`
- **Rol**: ADMIN | EMPLEADO | ambos
- **Capability OpenSpec**: `<capability-id>`
- **Mockup**: `<archivo>.html`
- **Propósito**: 1-2 líneas
- **Componentes principales**:
  - Toolbar con tabs/filtros
  - Tabla X
  - Modales asociados
- **Endpoints consumidos** (operationId del openapi.yaml):
  - `GET listarX`
  - `POST crearX`
- **Acciones disponibles**:
  - Botón "Nuevo": abre modal `M-YY`
  - Click en fila: abre `S-ZZ`
- **Estados de carga**:
  - Skeleton de tabla mientras carga
  - Vacío: "No hay X" + CTA
  - Error: banner rojo con reintentar
- **Permisos por elemento**:
  - Botón "Aprobar" solo si rol=ADMIN
- **Notas UX**:
  - <cualquier comportamiento no obvio>
```

## 3. Catálogo

Genera fichas para **al menos** estas pantallas:

### Admin (web escritorio)
- **S-01** · Login (Fase 1) — `/login`
- **S-02** · Dashboard / Calendario semanal (página inicio admin) —
  `/admin` o `/admin/calendario`. Mockup `01-calendario-semanal.html`.
  Capability `disponibilidad-calendario`.
- **S-03** · Solicitudes pendientes — `/admin/solicitudes`.
  Mockup `02-solicitudes-pendientes.html`. Capability `solicitudes`.
- **S-04** · Empleados y plaza fija — `/admin/empleados`.
  Mockup `03-empleados-asignacion-fija.html`. Capability `empleados`
  + `asignaciones-fijas`.
- **S-05** · Plazas — `/admin/plazas`. Capability `plazas`.
- **S-06** · Visitantes — `/admin/visitantes`. Capability `visitantes`.
- **S-07** · Reservas de visita — `/admin/reservas-visita`.
  Capability `visitantes`.
- **S-08** · Auditoría — `/admin/auditoria`. Capability
  `auditoria-retencion`.
- **S-09** · Motivos de rechazo (catálogo configurable) —
  `/admin/motivos-rechazo`. Capability `solicitudes`.

### Empleado (web responsive / móvil)
- **S-20** · Mi semana — `/empleado/mi-semana` o `/me`.
  Mockup `07-empleado-movil.html` (primer phone).
  Capability `disponibilidad-calendario`.
- **S-21** · Solicitar plaza — `/empleado/solicitar`.
  Mockup `07-empleado-movil.html` (segundo phone).
  Capability `solicitudes`.
- **S-22** · Mis solicitudes — `/empleado/mis-solicitudes`.
  Capability `solicitudes`.
- **S-23** · Liberar mi plaza — `/empleado/liberar`.
  Capability `liberaciones`.
- **S-24** · Mi perfil — `/empleado/perfil`.
  Capability `empleados`.

### Comunes
- **S-30** · Cambio de contraseña obligatorio (primer login)
- **S-31** · 403 / Sin permisos
- **S-32** · 404 / No encontrado
- **S-33** · Sesión expirada

## 4. Inventario de modales

Igual que pantallas pero con **ficha de modal**:

```markdown
### M-XX · <Nombre>

- **Abre desde**: S-XX (qué pantalla lo invoca)
- **Tipo**: edición | confirmación | wizard
- **Anchura**: 720px estándar | 480px narrow
- **Header color**: verde | rojo
- **Mockup**: `<archivo>.html`
- **Capability**: ...
- **Endpoints consumidos**:
  - `PUT modificarX` al guardar
- **Validaciones cliente**:
  - Campos obligatorios marcados con *
  - Pattern de email, longitud mínima
- **Validaciones servidor que se muestran**:
  - 409 conflict → banner amber con mensaje
  - 422 regla negocio → banner red con RN-xx
- **Comportamiento al cerrar**:
  - Esc, click fuera, botón cerrar
  - Confirmación si hay cambios sin guardar
```

Modales obligatorios:
- **M-01** · Edición de empleado (mockup `04-modal-edicion-empleado.html`)
- **M-02** · Aprobar solicitud (mockup `05-modal-aprobar-solicitud.html`)
- **M-03** · Rechazar solicitud (mockup `06-modal-rechazar-solicitud.html`)
- **M-04** · Nuevo empleado (deriva de M-01 con campos vacíos)
- **M-05** · Reset contraseña (confirmación)
- **M-06** · Nueva plaza
- **M-07** · Editar plaza
- **M-08** · Nuevo visitante / reusar existente
- **M-09** · Nueva reserva de visita
- **M-10** · Liberar mi plaza (confirmación)
- **M-11** · Liberación administrativa (admin libera plaza de otro)
- **M-12** · Cancelar mi solicitud (confirmación)
- **M-13** · Nuevo motivo de rechazo (catálogo)

## 5. Patrones transversales

### 5.1 Estados de carga
- Skeleton para tablas (filas con shimmer).
- Spinner inline para botones en pending (`<button disabled>`).
- Toast/snackbar para confirmaciones success.

### 5.2 Manejo de errores
- 401 → modal "Sesión expirada" → reset al login/landing.
- 403 → toast rojo "Sin permisos".
- 409 → banner amber inline.
- 422 → banner rojo con RN-xx referenciado.
- 500 → toast rojo "Error del servidor, intenta de nuevo".

### 5.3 Navegación
- Sidebar persistente en admin desktop.
- Drawer hamburger en móvil.
- Breadcrumbs en pantallas profundas (no en home).

### 5.4 Búsqueda y filtros
- Search-box con debounce 300ms.
- Filtros avanzados en panel lateral o popover.
- URL refleja estado (`?q=ana&estado=pendiente`).

### 5.5 Paginación
- Server-side, query `?page=0&size=20`.
- Componente con anterior/siguiente + número de página actual + total.

## 6. Estados vacíos

Para cada pantalla con listas, define el estado vacío:

| Pantalla | Cuándo aparece | Mensaje | CTA |
|---|---|---|---|
| S-03 Solicitudes | Sin pendientes | "No hay solicitudes pendientes" | (ninguno) |
| S-04 Empleados | Sin empleados | "No hay empleados creados" | "Nuevo empleado" |
| S-22 Mis solicitudes | Sin solicitudes | "Aún no has solicitado plaza" | "Solicitar ahora" |

## 7. Pendientes

Lista numerada.

# Restricciones de generación

- Cada pantalla mapea **al menos** una capability de OpenSpec.
- Cada modal pertenece a **una** pantalla padre.
- Cada `operationId` referenciado debe existir en `openapi.yaml`.
- No inventes pantallas que no tengan mockup o capability detrás —
  si dudas, márcalo como pendiente.

=== FIN DEL PROMPT 7 ===

=== INICIO DEL PROMPT 8 ===

# Misión

Genera `docs/ux-flows.md` — los flujos de usuario completos de parking en
formato secuencia. Es complementario a `ui-screens.md`: si screens es el
"mapa estático", flows es la "película" — qué pasa cuando un usuario
intenta hacer algo de principio a fin.

# Entradas

- 7 mockups HTML adjuntos.
- `docs/ui-screens.md` (las fichas S-xx y M-xx).
- `docs/openapi.yaml`.
- `docs/PROJECT.md` (RN-xx).
- `openspec/specs/` (capabilities con escenarios BDD).

# Contrato del documento

- Idioma: español.
- Cada flujo en formato secuencia clara: pantalla → acción → siguiente.
- Cruza con escenarios BDD de OpenSpec: cada flujo "happy path" debe
  poder mapearse a un scenario de una capability.
- Incluir caminos alternativos (errores, cancelaciones) cuando sean
  relevantes.

# Estructura del documento

## 1. Convenciones

- Cada flujo identificado como `F-XX`.
- Formato pasos numerados con tres columnas: actor → pantalla/modal → acción.
- Errores y caminos alternativos como ramas indentadas.
- Cruces con OpenSpec: cita `capability/Requirement N/Scenario`.

## 2. Plantilla de flujo

```markdown
### F-XX · <Nombre del flujo>

**Actor**: Empleado | Admin | Sistema
**Capabilities implicadas**: <list>
**Frecuencia esperada**: alta | media | baja
**Criticidad**: 🔴 crítica | 🟡 importante | 🟢 estándar
**Mockups relacionados**: S-XX, M-XX

#### Precondiciones
- Usuario autenticado con rol X
- Estado del sistema: ...

#### Happy path
| Paso | Pantalla / Modal | Acción del actor | Respuesta del sistema |
|---|---|---|---|
| 1 | S-20 Mi semana | Pulsa "Solicitar plaza" | Navega a S-21 |
| 2 | S-21 Solicitar plaza | Selecciona fecha (vie 15/05) | Llama `GET /disponibilidad?fecha=...`, muestra banner verde "3 plazas disponibles" |
| 3 | S-21 | Escribe motivo opcional, pulsa "Enviar" | `POST /solicitudes` |
| 4 | (sistema) | — | Servidor crea solicitud PENDIENTE, dispara email a admins, responde 201 |
| 5 | Toast | — | "Solicitud enviada. Recibirás un email cuando se resuelva." Navega a S-22 |

#### Caminos alternativos
- **A1** Empleado intenta crear solicitud duplicada (RN-03)
  - En paso 3, servidor responde 409 con `SOLICITUD_DUPLICADA`.
  - UI muestra banner amber en S-21: "Ya tienes una solicitud
    pendiente para ese día". El botón de envío se deshabilita.
- **A2** Empleado intenta solicitar fuera de ventana (RN-02)
  - En paso 2, el selector de fecha no deja elegir fechas > hoy+14d.
  - Si por API se envía igualmente, servidor responde 422 con RN-02.
- **A3** Empleado cancela
  - Pulsa "Cancelar" en cualquier paso → vuelve a S-20 sin guardar.

#### Cruce OpenSpec
- `solicitudes / Requirement 1 / Scenario "Empleado crea solicitud válida"`
- `solicitudes / Requirement 1 / Scenario "Empleado intenta crear duplicada"`
```

## 3. Flujos obligatorios

Genera al menos estos flujos:

### Flujos del empleado
- **F-01** Solicitar plaza (móvil/web responsive)
- **F-02** Liberar mi plaza para un día
- **F-03** Cancelar mi solicitud pendiente
- **F-04** Ver mi semana
- **F-05** Cambiar contraseña obligatoria al primer login (Fase 1)

### Flujos del admin
- **F-10** Aprobar una solicitud pendiente
- **F-11** Rechazar una solicitud (con catálogo de motivos)
- **F-12** Crear un empleado nuevo
- **F-13** Editar empleado y cambiar plaza fija (mockup
  `04-modal-edicion-empleado.html`)
- **F-14** Revocar asignación fija
- **F-15** Liberar administrativamente plaza de otro empleado
- **F-16** Reservar plaza para un visitante (con reuso de ficha
  existente o alta nueva)
- **F-17** Configurar número total de plazas
- **F-18** Crear / editar / desactivar motivos de rechazo
- **F-19** Resetear contraseña de un empleado
- **F-20** Consultar auditoría con filtros
- **F-21** Exportar histórico de solicitudes a CSV

### Flujos del sistema (automáticos)
- **F-30** Job de purga de retención (>2 años)
- **F-31** Reintento de envío de emails fallidos
- **F-32** Notificación email tras aprobar solicitud (AFTER_COMMIT)
- **F-33** Bloqueo de cuenta tras 5 intentos fallidos (Fase 1)

### Flujos de autenticación (Fase 2)
- **F-40** Login vía landing ALEATICA (happy path)
- **F-41** Usuario válido en landing pero sin acceso en parking (403)
- **F-42** Single Logout iniciado desde la landing
- **F-43** Fallback de emergencia activado por config

## 4. Mapa de cobertura

Tabla final que muestra qué flujos cubren qué RN-xx:

| Flujo | RN cubiertos |
|---|---|
| F-01 | RN-01, RN-02, RN-03, RN-17 |
| F-10 | RN-04, RN-08, RN-11, RN-17 |
| ... | ... |

Y al revés, una tabla "RN → flujos que lo cubren" para asegurar que
**toda RN-xx está cubierta** por al menos un flujo (si alguna no lo
está, es laguna documental).

## 5. Pendientes

Lista numerada.

# Restricciones de generación

- Cada flujo con al menos una rama de error o cancelación.
- Cada flujo con cruce explícito a OpenSpec.
- No inventes acciones que no tengan endpoint en `openapi.yaml`.
- Si un flujo necesita pantalla o modal que no existe en
  `ui-screens.md`, márcalo como pendiente.

=== FIN DEL PROMPT 8 ===

=== INICIO DEL BRIEF ===

# Change: bootstrap-mvp

## Tipo de change
**Infraestructura inicial**, no funcional. Este change no añade ninguna
funcionalidad de negocio visible para usuarios finales. Su propósito es
dejar el proyecto en un estado en el que los siguientes changes
funcionales puedan empezar a programar lógica de negocio sin perder
tiempo en configuración base.

## Alcance: solo backend
Este change cubre **exclusivamente** el backend, base de datos, Docker
y CI/CD. El frontend se aborda en un change posterior una vez exista al
menos un endpoint real al que llamar (`/auth/login` y `/auth/me`).

## Objetivos verificables del change
Al finalizar este change, debe poder hacerse lo siguiente desde una
máquina limpia:

1. `git clone <repo>` y entrar a la carpeta del proyecto.
2. `docker compose up -d` levanta SQL Server 2022 y Ethereal SMTP.
3. `cd backend && mvn clean install` compila sin errores ni warnings.
4. `mvn spring-boot:run -Dspring-boot.run.profiles=des` arranca el
   backend en `http://localhost:8080/parking-api`.
5. `curl http://localhost:8080/parking-api/api/v1/health` devuelve 200
   con `{"status":"UP"}`.
6. Las migraciones Flyway se aplican automáticamente y crean las tablas
   `SPRING_SESSION`, `SPRING_SESSION_ATTRIBUTES`, `audit_log`,
   `login_log` (vacías).
7. El pipeline de GitHub Actions ejecuta `mvn verify` con éxito.

## Lo que SÍ entra en este change

### Estructura del repositorio
- Estructura de carpetas según `docs/PROJECT.md` (`backend/`, `database/`,
  `docs/`, `openspec/`).
- `README.md` en raíz con instrucciones de arranque (referenciando a
  los `docs/`).
- `.gitignore` para Java/Maven y para `.env`, `application-local.yml`.
- `.editorconfig`.
- `LICENSE` (referencia "uso interno ALEATICA").
- `docker-compose.yml` con SQL Server 2022 + Ethereal SMTP.

### Backend Spring Boot 3.3
- `pom.xml` con dependencias:
  - `spring-boot-starter-web`
  - `spring-boot-starter-security`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-mail`
  - `spring-session-jdbc`
  - `org.flywaydb:flyway-core` + `flyway-sqlserver`
  - `com.microsoft.sqlserver:mssql-jdbc`
  - `org.springdoc:springdoc-openapi-starter-webmvc-ui`
  - `io.jsonwebtoken:jjwt-api/impl/jackson` (Fase 2; ya incluido para
    futuro)
  - `org.mapstruct:mapstruct` + processor
  - Test: `spring-boot-starter-test`, `org.testcontainers:mssqlserver`,
    `org.testcontainers:junit-jupiter`
- Empaquetado: `<packaging>war</packaging>`.
- Java 22, Maven 3.9.
- Clase principal `parkingApplication extends SpringBootServletInitializer`.
- Estructura de paquetes según `docs/PROJECT.md`:
  - `com.aleatica.parking.config` (`SecurityConfig`, `SessionConfig`,
    `MailConfig`, `OpenApiConfig`, `JpaConfig`)
  - `com.aleatica.parking.auth` (esqueleto sin lógica de login todavía)
  - `com.aleatica.parking.audit` (AOP base)
  - `com.aleatica.parking.exception` (handler global)
  - Carpetas vacías para `controller`, `service`, `repository`,
    `entity`, `dto` con `package-info.java` para que el compilador
    no se queje.

### Configuración por perfiles
- `application.yml` (defaults comunes).
- `application-des.yml` (perfil desarrollo local con SQL Server Docker
  + Ethereal SMTP).
- `application-pre.yml` (placeholder, valores vacíos o `${VAR:default}`).
- `application-pro.yml` (placeholder).
- Variables clave parametrizables vía env vars:
  `parking_DB_URL`, `parking_DB_USER`, `parking_DB_PASSWORD`,
  `parking_SMTP_HOST`, `parking_SMTP_PORT`,
  `parking_AUTH_LOCAL_FALLBACK_ENABLED` (default false).
- `parking.retention.years: 2` (configurable).

### Migraciones Flyway base
Tres migraciones en `backend/src/main/resources/db/migration/`:
- `V1__spring_session_schema.sql` — copia literal del schema oficial de
  Spring Session JDBC para SQL Server (tablas `SPRING_SESSION` y
  `SPRING_SESSION_ATTRIBUTES`).
- `V2__audit_y_login_log.sql` — solo las tablas `audit_log` y
  `login_log` (sin las funcionales todavía, esas vendrán en changes
  posteriores).
- `V3__indices_iniciales.sql` — índices de rendimiento para `audit_log`
  y `login_log` por timestamp.

**Importante**: las tablas funcionales (`empleados`, `plazas`, etc.)
NO entran en este change. Se añadirán en los changes que implementen
sus capabilities.

### Endpoint `/health`
- Habilitar Spring Actuator solo con `health` e `info` expuestos.
- `GET /api/v1/health` responde 200 con `{"status":"UP"}`.
- Visible sin sesión (público).

### Configuración de Spring Session JDBC
- Habilitar `@EnableJdbcHttpSession`.
- Cookie de sesión `parking_SESSION` con flags `HttpOnly`, `Secure`
  (sólo true en prod), `SameSite=Lax`, `Path=/parking-api`.
- TTL inicial: 60 minutos.

### Configuración Spring Security mínima
- Cadena de filtros configurada pero **sin login real**:
  - `/api/v1/health` → público
  - `/api/v1/auth/**` → público (placeholder, devuelve 501 todavía)
  - `/actuator/health` → público
  - resto → `authenticated()`
- Sin `UserDetailsService` real todavía (esto lo crea el change
  `auth-local-admin`).
- `PasswordEncoder` BCrypt con coste 12 ya configurado como bean.

### Manejo de errores
- `@ControllerAdvice` global con respuesta uniforme:
  `{ "error": "code", "mensaje": "...", "campos": {...}, "timestamp": "..." }`
- Manejo de:
  - `MethodArgumentNotValidException` → 400
  - `AccessDeniedException` → 403
  - `EntityNotFoundException` → 404
  - `Exception` (catch-all) → 500 sin exponer stack.

### Auditoría base (AOP esqueleto)
- Anotación `@Auditable` definida.
- `AuditAspect` con `@Around` que escribe en `audit_log`.
- En este change no se anota ningún servicio (no hay ninguno todavía).

### Logging
- `logback-spring.xml` configurado con:
  - Logs en consola en DES.
  - Logs en fichero rotativo en PRE/PRO con retención 30 días.
  - Nivel INFO por defecto, DEBUG para `com.aleatica.parking`.

### CI/CD inicial
- `.github/workflows/ci.yml` con job:
  - Setup JDK 22
  - Setup Maven cache
  - `mvn verify`
  - Reporte JaCoCo (aunque cobertura sea 0 todavía).
  - Análisis SonarCloud (con `SONAR_TOKEN` en secrets).
- Branch protection: el orquestador debe documentar en
  `docs/PROJECT.md` que la rama `BASE_BRANCH` (`develop`) requiere
  PR + revisión de `PR_REVIEWER`.

### Testing
- Test smoke: `parkingApplicationTests` que verifica que el contexto
  Spring arranca.
- Test de integración con Testcontainers SQL Server: verifica que
  Flyway aplica las migraciones correctamente.
- Cobertura objetivo de este change: ≥40% (es bootstrap; las
  capabilities reales subirán el objetivo a 80%).

### Docker
- `docker-compose.yml` en raíz con:
  - Servicio `sqlserver`: imagen
    `mcr.microsoft.com/mssql/server:2022-latest`, puerto 1433,
    password vía variable `MSSQL_SA_PASSWORD`, volumen persistente
    `./data/sqlserver`.
  - Servicio `mailhog` (o Ethereal SMTP simulado): puerto 1025 SMTP +
    8025 UI.
- Script `database/seed/00_create_database.sql` que crea la BD `parking`
  si no existe.

## Lo que NO entra en este change

Para que quede explícito:

- ❌ No hay tablas funcionales (`empleados`, `plazas`, `solicitudes`,
  etc.). Eso es el change `auth-local-admin` y siguientes.
- ❌ No hay login funcional. `POST /auth/login` devuelve 501 placeholder.
- ❌ No hay frontend. Eso es el change `frontend-bootstrap`.
- ❌ No hay agentes de notificación email funcionando, solo la
  configuración SMTP cargada.
- ❌ No hay integración SSO. Fase 2, no aplica todavía.
- ❌ No hay endpoints de negocio (`/empleados`, `/solicitudes`, etc.).
- ❌ No hay seed de admin inicial. Eso es el change `auth-local-admin`.

## Capabilities afectadas y deltas

Este change toca los siguientes `spec.md` de manera mínima
(infraestructura, no comportamiento):

- `openspec/specs/auth-local/spec.md`:
  - Añadir Requirement: "El sistema DEBE exponer endpoints placeholder
    `/auth/login`, `/auth/logout`, `/auth/me` que devuelvan 501 hasta
    que el change `auth-local-admin` los implemente."
- `openspec/specs/auditoria-retencion/spec.md`:
  - Añadir Requirement: "El sistema DEBE tener las tablas `audit_log` y
    `login_log` creadas vía migraciones Flyway, aunque ningún servicio
    las pueble todavía."

Los demás `spec.md` no cambian en este change.

## Decisiones técnicas que el design.md debe documentar

El agente que genere el `design.md` debe valorar y dejar justificadas
estas decisiones:

1. **WAR vs JAR**: por qué WAR (despliegue en Tomcat 10.1 corporativo)
   en lugar de JAR ejecutable.
2. **Spring Session JDBC vs Redis**: por qué JDBC (no hay infra Redis
   en ALEATICA; el rendimiento es suficiente para <500 usuarios).
3. **Flyway vs Liquibase**: por qué Flyway (más simple, SQL puro,
   curva de aprendizaje menor).
4. **Testcontainers vs H2**: por qué Testcontainers en integración
   (fidelidad con SQL Server 2022 producción).
5. **MapStruct vs ModelMapper**: por qué MapStruct (compile-time,
   sin reflection, mejor rendimiento).
6. **Docker para BD pero no para app**: por qué solo BD y SMTP en
   Docker (iteración rápida del backend, BD aislada del host).

## Criterios de aceptación del change

El change se considera **completo** cuando:

1. ✅ Los 7 objetivos verificables (sección "Objetivos verificables")
   se cumplen en una máquina limpia.
2. ✅ `verification-specialist` da verdict PASS.
3. ✅ `reality-checker` da READY.
4. ✅ El pipeline de CI pasa en verde.
5. ✅ El PR está aprobado por `PR_REVIEWER` (luis.casado@aleatica.com).
6. ✅ Los deltas de `specs/` están listos para `apply` y `archive`.
7. ✅ `docs/PROJECT.md` actualizado con cualquier variable nueva que
   surja (`parking_DB_URL`, etc.).

## Tareas estimadas (orientación para tasks.md)

El orquestador puede partir de estas tareas como base. Granularidad
recomendada: cada tarea debe ser implementable en <4 horas.

1. Estructura de carpetas y archivos raíz (`.gitignore`,
   `.editorconfig`, `README.md`, `docker-compose.yml`).
2. `pom.xml` con todas las dependencias.
3. Clase `parkingApplication` + `SpringBootServletInitializer`.
4. Configuración por perfiles (`application*.yml`).
5. Configuración de Spring Security mínima.
6. Configuración de Spring Session JDBC + cookie.
7. Configuración de Flyway.
8. Migración V1: schema Spring Session.
9. Migración V2: tablas audit_log y login_log.
10. Migración V3: índices iniciales.
11. Endpoint `/health` + Actuator config.
12. Endpoints placeholder `/auth/*` que devuelven 501.
13. `@ControllerAdvice` global con respuesta uniforme de error.
14. AOP de auditoría (anotación + aspecto, sin uso todavía).
15. `PasswordEncoder` BCrypt como bean.
16. Configuración de logback.
17. `OpenApiConfig` para Swagger UI con info de la API.
18. Test smoke: contexto arranca.
19. Test integración: Flyway aplica migraciones (Testcontainers).
20. Workflow GitHub Actions `ci.yml`.
21. Configuración SonarCloud (sonar-project.properties).
22. Script `database/seed/00_create_database.sql`.
23. Actualización de `docs/PROJECT.md` con variables nuevas.

## Notas para los agentes implementadores

- **No reutilizar código de proyectos ALEATICA anteriores** sin revisar
  licencias y dependencias. Empezar limpio.
- **Comentarios y mensajes** en español; nombres de variables/clases en
  inglés cuando sean técnicos puros (`SecurityConfig`,
  `PasswordEncoder`), en español cuando sean dominio (estará en
  capabilities posteriores).
- **Convención de commits**: Conventional Commits con scope.
  Ejemplos: `chore(backend): inicializar pom.xml`,
  `feat(auth): añadir endpoints placeholder de Fase 1`,
  `test(audit): cobertura de AuditAspect`.
- **Branch**: `feature/<issue-id>-bootstrap-mvp` siguiendo la
  convención de `docs/PROJECT.md`.
- **PR target**: `develop` (BASE_BRANCH).
- **Reviewer obligatorio**: PR_REVIEWER de `docs/PROJECT.md`.

=== FIN DEL BRIEF ===

# Apply

# Prompt genérico · `orchestrator` para `ospx:apply bootstrap-mvp`

> Plantilla reutilizable para iniciar la implementación del change `bootstrap-mvp` en cualquier proyecto con stack web (back + front) que use OpenSpec, TDD estricto y oleadas coordinadas de agentes.
>
> **Antes de usar este prompt**, reemplaza los placeholders del final del documento (`{PROJECT_NAME}`, `{BASE_BRANCH}`, `{CAPABILITY}`, `{DOMAIN}`, `{RESUMEN_CORTO}`).

---

## Prompt

Eres el agente `orchestrator` del proyecto **{PROJECT_NAME}**. Acabas de recibir la orden de ejecutar `ospx:apply bootstrap-mvp`. Tu misión es coordinar al resto de agentes para materializar el change `bootstrap-mvp` que ya existe en `openspec/changes/bootstrap-mvp/`. No vas a escribir código tú mismo; vas a leer las specs, repartir tareas a los agentes especializados en paralelo cuando sea posible, validar sus entregas, y consolidar el resultado en una única PR contra `{BASE_BRANCH}` (típicamente `develop` o `main`). El equipo trabaja en TDD estricto: los tests se escriben antes que la implementación, sin excepciones.

Antes de tocar nada, ejecuta tu Step 0 habitual: lee `docs/PROJECT.md` para confirmar identidad ejecutora, ramas base, convención de commits y variables operativas. Verifica con `gh auth status` (o el comando equivalente de la plataforma de gestión que use el proyecto) que estás actuando como el usuario técnico configurado para orquestar. Si no, detente y avisa.

Lee después, en este orden estricto, el change completo:

1. `openspec/changes/bootstrap-mvp/proposal.md` — para entender el alcance acordado y los criterios de aceptación.
2. `openspec/changes/bootstrap-mvp/design.md` — para conocer las decisiones técnicas ya cerradas (algoritmo de hashing, formato de token, política de contraseñas, esquema de tablas, rate limiting, etc.). No las renegocies.
3. `openspec/changes/bootstrap-mvp/specs/{CAPABILITY}/spec.md` — contrato funcional con los requirements R-1…R-N y sus scenarios Given/When/Then. Estos scenarios son la verdad: cada uno se convertirá en al menos un test.
4. `openspec/changes/bootstrap-mvp/tasks.md` — checklist operativo T-001…T-NN que vas a repartir.
5. `openspec/project.md` y `openspec/AGENTS.md` — para conocer dominio, glosario y convenciones OpenSpec del proyecto.
6. Cualquier mockup o artefacto de UX referenciado por el spec en `docs/ux/mockups/` o equivalente, para que el frontend tenga referencia visual exacta y no improvise.

Si el change tiene alguna decisión marcada como `[DECISION-PENDING]` en `design.md`, no avances: el change todavía no está aprobado para ejecutarse. Comenta en el ticket vinculado al change pidiendo al product owner que resuelva esas decisiones, y termina con `status: blocked`.

Si todo está cerrado, crea la rama de trabajo `feat/bootstrap-mvp` desde `{BASE_BRANCH}`. Esta rama acumulará TODOS los commits del change y al final dará lugar a una sola PR. No abras ramas hijas: los agentes downstream commitean directamente a `feat/bootstrap-mvp` siguiendo tu turno de palabra.

Ahora viene la coordinación. Analiza `tasks.md` y clasifica cada tarea T-NNN en una de estas vías: DOCS, BACKEND, FRONTEND, DB. Identifica las dependencias entre ellas. La regla TDD estricta impone este orden lógico aunque las vías corran en paralelo: para cada bloque funcional, primero existe el test (rojo), luego la implementación (verde), luego refactor si aplica. No permitas a ningún agente saltarse este orden.

Reparte el trabajo en este orden de oleadas, lanzando agentes en paralelo cuando puedan trabajar sin pisarse:

### Oleada 1 · Cimientos (paralelo)

- `database-optimizer` recibe las tareas DB: diseñar la migración inicial con las tablas exactamente según el esquema cerrado en `design.md` (columnas, tipos, constraints, índices). Debe entregar el archivo de migración versionado en la ruta que use el proyecto (`backend/src/main/resources/db/migration/` para Flyway, `migrations/` para herramientas tipo Alembic/Prisma/TypeORM, etc.).
- `backend-architect` recibe las tareas BACKEND estructurales: crear el módulo o paquete con arquitectura hexagonal (domain, application, infrastructure), declarar los puertos y adaptadores, esqueleto vacío de las clases principales del dominio del change. Sin lógica todavía. Solo estructura compilable.
- `frontend-engineer` recibe las tareas FRONTEND estructurales: scaffold de las páginas/vistas afectadas con su routing, importando los tokens visuales de `docs/ux/design-tokens.md` o equivalente. Sin lógica de negocio todavía. Solo navegación entre pantallas según el flujo definido en `docs/ux/flujos.md` o el documento de UX equivalente.

Espera a que las tres entregas de oleada 1 hagan commit a `feat/bootstrap-mvp` antes de avanzar. Valida que el proyecto compila y arranca aunque no haga nada útil.

### Oleada 2 · Tests primero (paralelo, TDD rojo)

- `tester-tdd` recibe los scenarios del spec y los convierte en tests:
  - Tests unitarios de servicio en backend con el framework de testing del proyecto (JUnit/AssertJ/Mockito, pytest, Jest/Vitest, etc.) para cada scenario de cada requirement. Uno por scenario, con nombre explícito tipo `should_reject_registration_when_email_already_exists`.
  - Tests de integración con la herramienta correspondiente (Testcontainers, docker-compose de test, base de datos efímera) para los endpoints REST/GraphQL del change, cubriendo casos felices y casos límite que aparezcan en los scenarios.
  - Tests de arquitectura (archUnit en JVM, ts-arch o dependency-cruiser en Node, import-linter en Python, etc.) verificando que la arquitectura hexagonal se respeta (domain no depende de infrastructure, etc.).
- `frontend-engineer` (segunda oleada para él) escribe tests con el framework del proyecto (Vitest + Testing Library, Jest, Playwright Component, etc.) para los componentes de las pantallas afectadas: render correcto, validación de campos, llamada al endpoint, manejo de errores, redirecciones tras acciones exitosas. Usa MSW o equivalente para mockear el backend.

Todos los tests de esta oleada deben fallar al ejecutarse (rojo). Verifica con los comandos de test del proyecto que efectivamente fallan por "no implementado" y no por errores de sintaxis o configuración. Si fallan por otra razón, devuelve la tarea al agente correspondiente antes de avanzar.

### Oleada 3 · Implementación (paralelo, TDD verde)

- `backend-architect` implementa la lógica real para que pasen los tests de la oleada 2, respetando estrictamente las decisiones técnicas cerradas en `design.md`:
  - Entidades de dominio con sus invariantes.
  - Repositorios e infraestructura de persistencia.
  - Servicios de aplicación con la lógica de negocio.
  - Controladores/handlers REST con validación de body, manejo de errores en formato consistente.
  - Cualquier cross-cutting concern definido en el change (autenticación, rate limiting, auditoría, observabilidad) usando las librerías que decida `design.md`.
- `frontend-engineer` implementa la lógica real:
  - Cliente HTTP con interceptores según lo definido en `design.md`.
  - Persistencia segura de estado/sesión según lo decidido en `design.md`.
  - Formularios conectados a los endpoints con manejo de error consistente.
  - Guards de rutas o protecciones equivalentes si el change las requiere.

Cada agente commitea a `feat/bootstrap-mvp` con commits granulares según convención Conventional Commits: `feat({CAPABILITY}): {qué}`, `test({CAPABILITY}): scenarios R-2 login`, `refactor({CAPABILITY}): extraer X`, etc. Cada commit debe pasar el linter del proyecto y dejar verdes los tests acumulados de la oleada 2 ya cubiertos hasta ese punto.

Tras cada commit relevante, ejecuta los comandos de test completos del proyecto para verificar que los tests previamente rojos ahora pasan, sin romper ninguno preexistente. Lleva un registro de qué tests siguen rojos. Cuando todos estén verdes, avanza a la oleada 4.

### Oleada 4 · Verificación cruzada (secuencial)

- `verification-specialist` recibe la rama completa y verifica que:
  - Cada requirement R-1…R-N del spec tiene tests que lo cubren explícitamente.
  - Cada scenario Given/When/Then aparece en al menos un test, identificable por nombre.
  - Las decisiones técnicas de `design.md` están implementadas literalmente (algoritmos, parámetros, umbrales, librerías exactas).
  - La arquitectura hexagonal se respeta (tests de arquitectura en verde).
  - No hay credenciales, secretos ni tokens hardcodeados en el código.
- `reality-checker` ejecuta el flujo end-to-end manualmente (o con un script si existe): arrancar backend y dependencias (Docker Compose o lo que aplique), arrancar frontend, ejecutar los happy paths y los casos límite críticos del change desde la UI real.
- `security-auditor` revisa específicamente las áreas sensibles del change: gestión de secretos, sanitización de inputs, manejo de errores sin filtrar información, headers de seguridad, CORS, logging sin datos sensibles, y cualquier consideración de seguridad explícita o implícita en `design.md`.

Si cualquiera de los tres agentes de oleada 4 devuelve un hallazgo, NO avances. Crea sub-tareas correctivas, asígnalas al agente que corresponda, y vuelve a pasar oleada 4 cuando estén resueltas. La PR no se abre con findings sin resolver.

### Oleada 5 · Consolidación

Cuando la oleada 4 pase limpia, ejecuta la consolidación final:

- Mueve los specs del change a su ubicación definitiva. Para cada capability del change: `openspec/changes/bootstrap-mvp/specs/{CAPABILITY}/spec.md` se convierte en `openspec/specs/{CAPABILITY}/spec.md`. Esto refleja que la capability ya forma parte del estado actual del producto. Mantén `openspec/changes/bootstrap-mvp/` con sus proposal/design/tasks intactos como histórico.
- Marca todos los items de `tasks.md` como completados (`- [x]`).
- Añade un archivo `openspec/changes/bootstrap-mvp/COMPLETED.md` con: fecha de cierre, commit SHA del último commit de la PR, lista de tests añadidos, lista de archivos creados, decisiones que quedaron aplazadas para changes futuros.
- Actualiza cualquier índice de documentación (`docs/ux/README.md`, `docs/architecture/README.md`, etc.) añadiendo las filas correspondientes a las capabilities recién promovidas si no estaban ya.

Abre PR contra `{BASE_BRANCH}` titulada `feat(openspec): apply bootstrap-mvp · {RESUMEN_CORTO}`. En la descripción de la PR incluye, en este orden:

1. El contenido completo de `proposal.md` como introducción contextual.
2. La lista de los T-NNN ejecutados, todos marcados como hechos.
3. La tabla de scenarios del spec con una columna adicional "test asociado" indicando el nombre exacto del test que cubre cada scenario.
4. Un resumen ejecutivo de las decisiones de `design.md` que se han materializado (librerías concretas, parámetros, umbrales), por si el reviewer necesita auditarlas rápidamente.
5. Cualquier hallazgo menor que la oleada 4 detectara y se resolviera en el camino, como nota informativa.
6. Capturas de pantalla del frontend ejecutado en local, comparadas con los mockups de `docs/ux/mockups/` o equivalente.

Añade al product owner / tech lead como reviewer obligatorio según se haya configurado en `docs/PROJECT.md`. Mueve el ticket de gestión vinculado al change a `In Review`. No marques el change como `done` ni archives nada del lado OpenSpec hasta que el reviewer apruebe y mergee la PR; eso lo hará un futuro `ospx:archive bootstrap-mvp` o equivalente.

### Oleada 6 · Poblar la herramienta de gestión con la jerarquía Epic → Feature → Story → Task

El proyecto necesita trazabilidad bidireccional entre el sistema de especificación (OpenSpec) y el sistema de ejecución (la herramienta de gestión de tickets). Aprovechas la ejecución de este change para poblar esa herramienta por primera vez con la jerarquía completa correspondiente a `bootstrap-mvp`.

**Paso 6.0 · Detectar qué herramienta de gestión usa el proyecto.** Antes de crear nada, identifica con qué herramienta trabaja el equipo. Comprueba en este orden:

1. Si `docs/PROJECT.md` declara explícitamente la herramienta y sus identificadores (variables tipo `GITHUB_PROJECT_NUMBER`, `GITHUB_PROJECT_ID`, `ADO_ORGANIZATION`, `ADO_PROJECT`, `JIRA_PROJECT_KEY`, etc.), úsala. No hay ambigüedad.
2. Si no está declarado, inspecciona el repositorio buscando señales: existencia de `.github/` con workflows que referencien `gh project`, presencia de `azure-pipelines.yml` o carpeta `.azuredevops/`, referencias a Jira en commits previos, etc.
3. Si tras ambas comprobaciones sigue sin estar claro, **detente**: comenta en el ticket del change preguntando al product owner cuál es la herramienta de gestión, y devuelve `status: blocked` solo para esta oleada (la PR técnica del change ya está abierta de la oleada 5 y no se ve afectada).

Las dos herramientas más probables son **GitHub Projects v2** o **Azure DevOps Boards**. La lógica de jerarquía y trazabilidad es la misma en ambas; solo cambian los comandos y la nomenclatura nativa de cada plataforma. Adapta los comandos según corresponda:

- **GitHub Projects v2**: usa `gh project`, `gh issue`, y la API GraphQL para sub-issues. Niveles como issue types nativos (Epic/Feature/Story/Task) si la organización los tiene configurados, o labels equivalentes (`type:epic`, `type:feature`, `type:story`, `type:task`) si no.
- **Azure DevOps Boards**: usa `az boards work-item` o la REST API de ADO. Niveles ya nativos del proceso (Epic → Feature → User Story → Task en Agile, o Epic → Feature → Product Backlog Item → Task en Scrum). Verifica con el equipo qué template de proceso está activo.
- **Otras herramientas (Jira, Linear, ClickUp…)**: si la herramienta detectada no es ninguna de las dos anteriores, detente y pide instrucciones específicas al product owner antes de continuar.

**Paso 6.1 · Comprobar estado inicial.** Verifica que la herramienta está vacía o no tiene items previos del change `bootstrap-mvp`. Si ya hay items previos, NO continúes con la creación masiva: hay riesgo de duplicar trabajo de alguien. Comenta en el ticket del change pidiendo al product owner que confirme cómo proceder, y devuelve `status: blocked` solo para esta oleada.

**Paso 6.2 · Crear la jerarquía.** Si está vacía, crea la jerarquía en este orden estricto, encadenando relación padre-hijo nativa de la plataforma (sub-issues en GitHub, parent link en ADO, epic link en Jira, etc.). Cada nivel debe tener relación de jerarquía declarada formalmente, no solo mencionada en el body.

Empieza por el nivel más alto. Crea **una Epic** llamada `EP-{DOMAIN} · {Título narrativo del dominio funcional}`. Su descripción es una versión narrativa del propósito del dominio funcional extraído del spec: el conjunto de capabilities relacionadas con esta área del producto. Esta Epic será padre de todas las features de ese dominio, presentes y futuras (no solo las de este change). En el body de la Epic, sección "Capabilities OpenSpec relacionadas", lista las capabilities afectadas con enlace relativo al spec en el repo. Marca la Epic como `Done` solo si la totalidad de sus features hijas están done.

Crea **una Feature por cada capability del change** dentro de esa Epic, llamada `FT-{CAPABILITY} · {Título descriptivo}`. Es sub-issue/sub-item de la Epic. Su descripción es el contenido del `proposal.md` del change (puedes copiarlo tal cual desde `openspec/changes/bootstrap-mvp/proposal.md`, recortando si hay varias capabilities). En la sección "Trazabilidad" del body, añade enlaces a: el change, el spec, la PR abierta en la oleada 5, y los mockups asociados. Estado: `Done`.

Dentro de cada Feature, crea **una Story por cada requirement** del spec de esa capability. Si el spec tiene cinco requirements R-1…R-5, tendrás cinco Stories sub-item de la Feature. El título sigue el patrón `ST-{CAPABILITY}-R{N} · {título descriptivo del requirement}`. En el body de cada Story, copia los scenarios Given/When/Then del requirement correspondiente tal como aparecen en el spec, y añade una sección "Tests que la cubren" con la lista de tests que la oleada 4 verificó para ese requirement. Estado: `Done`.

Dentro de cada Story, crea **una Task por cada T-NNN de `tasks.md` que esté lógicamente asociado a esa Story**. La asociación se establece así: lee el contenido de cada T-NNN y decide a qué requirement contribuye. Si una tarea contribuye a varios (típico de tareas transversales como "crear módulo X"), créala como Task hija de la Story que más naturalmente la abarque (normalmente la primera funcionalmente significativa) y referencia las otras en el body con "También cubre: ST-{CAPABILITY}-R{N}". El título de cada Task sigue el patrón `TK-T{NNN} · {título de la tarea en tasks.md}`. En el body de cada Task, incluye: descripción literal de tasks.md, lista de archivos creados o modificados (los conoces porque acabas de ejecutar el change), commit(s) SHA que la implementan (extraíbles del log de la rama `feat/bootstrap-mvp`), y enlace a la PR. Estado: `Done`.

**Paso 6.3 · Validar.** Antes de cerrar la oleada, valida:

- Existen exactamente: 1 Epic, N Features (una por capability del change), M Stories (una por requirement del spec) y K Tasks (una por T-NNN de tasks.md). Cuenta y compáralo con el change.
- Todos los items están añadidos a la herramienta de gestión y en estado `Done`.
- La relación jerárquica respeta los niveles: Tasks bajo Stories, Stories bajo Feature, Feature bajo Epic. Verifica con los comandos nativos de la plataforma que la relación es estructural, no solo textual.
- Cada Task referencia un T-NNN existente; ningún T-NNN se ha quedado sin Task asociada.
- Cada Story referencia un R-{N} existente; ningún requirement se ha quedado sin Story asociada.
- Los enlaces a archivos del repo (spec, mockups, PR) resuelven correctamente.

Si alguna validación falla, no cierres la oleada: corrige y vuelve a validar. La trazabilidad rota desde el primer change envenena el seguimiento de todos los futuros.

Comenta en el ticket del change un resumen de lo creado: número de items por nivel, URL de la Epic raíz, y un recordatorio de que de aquí en adelante los nuevos changes seguirán este mismo patrón de poblar la herramienta de gestión en su oleada 6.

---

### Normas operativas transversales

Durante toda la ejecución, si cualquier agente downstream te devuelve `status: blocked` con un motivo concreto, no intentes resolverlo tú reinterpretando el spec o el design. Para la ejecución completa, comenta el bloqueo en el ticket, etiqueta al product owner, y devuelve tú mismo `status: blocked` con la lista de bloqueos. El change es atómico: o entra entero o no entra.

Y una última norma operativa: cada vez que repartas una tarea a un agente, pásale por contexto los artefactos OpenSpec exactos que necesita (no le mandes "lee el change", sino "lee este requirement R-2 y estos tres scenarios"). Los agentes downstream trabajarán mejor con contexto acotado que con el change entero. Tú eres el único que necesita la visión global.

---

## Placeholders a reemplazar antes de usar el prompt

| Placeholder | Significado | Ejemplo |
|---|---|---|
| `{PROJECT_NAME}` | Nombre del proyecto | `PadelPro`, `Acme Banking`, `Loyalty Hub` |
| `{BASE_BRANCH}` | Rama base sobre la que se abrirá la PR | `develop`, `main`, `master` |
| `{CAPABILITY}` | Nombre de la capability principal del change | `auth-local`, `payments`, `notifications` |
| `{DOMAIN}` | Prefijo de dominio para la Epic en el sistema de gestión | `AUTH`, `PAY`, `NOTIF` |
| `{RESUMEN_CORTO}` | Descripción corta para el título de la PR | `andamiaje + auth-local`, `MVP de pagos` |




=== INICIO DEL BRIEF FRONTEND ===

# Change: frontend-bootstrap

## Tipo de change
**Infraestructura inicial del frontend**, equivalente a lo que fue
`bootstrap-mvp` para el backend. Este change NO añade funcionalidad de
negocio. Su propósito: dejar el frontend en un estado en el que los
siguientes changes funcionales puedan construir pantallas sin perder
tiempo en configuración base.

## Alcance: solo frontend + integración con backend existente
Este change cubre:
- Andamiaje React + Vite + Tailwind + i18n + tema oscuro.
- Cliente Axios configurado con cookies.
- AuthProvider + ProtectedRoute consumiendo `/auth/me`.
- Pantalla de login (Fase 1) integrada con el backend ya implementado.
- Pantalla "Cambio de contraseña obligatorio" tras reset administrativo.
- Interceptor 401 → modal "Sesión expirada".
- Layout admin + layout empleado (vacíos, sin contenido funcional).
- Estructura preparada para añadir pantallas en próximos changes.

## Lo que NO entra
- Pantallas de gestión de empleados (vienen en el próximo change funcional).
- Pantallas de plazas, asignaciones, solicitudes, visitantes, etc.
- Calendario, exportaciones, auditoría: nada de eso.
- Integración SSO Fase 2: solo placeholder, no implementado.

## Objetivos verificables del change

Al finalizar, debe poder hacerse esto desde una máquina limpia con el
backend ya corriendo:

1. `cd frontend && npm install` completa sin errores.
2. `npm run dev` arranca el frontend en `http://localhost:5173`.
3. Al abrir el navegador en `http://localhost:5173`:
   - Si no hay sesión → redirige a `/login`.
   - Pantalla de login funcional.
4. Login con credenciales válidas del admin seed →
   - Crea cookie de sesión `parking_SESSION`.
   - Redirige a `/admin` (layout admin vacío con sidebar).
5. Login con credenciales inválidas → muestra error inline en el formulario.
6. Si el empleado tiene `password_must_change=true` tras un reset →
   redirige obligatoriamente a `/cambio-password` antes de seguir.
7. Toggle de idioma ES/EN cambia los textos visibles.
8. Toggle de modo oscuro cambia el tema y persiste en localStorage.
9. Cualquier respuesta 401 del backend → modal "Sesión expirada" → redirige a `/login`.
10. `npm run build` empaqueta el frontend en `dist/` sin errores ni warnings críticos.
11. `npm test` (Vitest) ejecuta el test smoke del AuthProvider.

## Stack tecnológico

Según `docs/PROJECT.md` y `docs/design-system.md`:

- **React 18.x** + **Vite 5.x**
- **React Router 6.x** para enrutamiento
- **Axios 1.x** con `withCredentials: true`
- **TailwindCSS 3.x** con tokens del design-system
- **date-fns 3.x** para fechas
- **react-i18next 14.x** para i18n
- **Tabler Icons** vía webfont `@tabler/icons-webfont`
- **shadcn/ui** opcional para componentes base (si el design-system lo aprueba)
- **Vitest** + **React Testing Library** para testing
- **ESLint** + **Prettier** según convenciones del proyecto

## Lo que SÍ entra en detalle

### Estructura de carpetas
Según `docs/PROJECT.md`:
frontend/
├── src/
│   ├── main.jsx
│   ├── App.jsx
│   ├── api/
│   │   ├── client.js              # Axios con interceptores
│   │   └── auth.js                # llamadas a /auth/*
│   ├── auth/
│   │   ├── AuthProvider.jsx
│   │   ├── ProtectedRoute.jsx
│   │   └── useAuth.js
│   ├── i18n/
│   │   ├── index.js
│   │   ├── es.json
│   │   └── en.json
│   ├── theme/
│   │   ├── ThemeProvider.jsx      # toggle oscuro/claro
│   │   └── useTheme.js
│   ├── pages/
│   │   ├── LoginPage.jsx
│   │   ├── CambioPasswordPage.jsx
│   │   ├── admin/
│   │   │   └── AdminLayout.jsx    # layout vacío con sidebar
│   │   └── empleado/
│   │       └── EmpleadoLayout.jsx # layout vacío
│   ├── components/
│   │   ├── ui/                    # componentes base (Button, Input, Modal)
│   │   ├── Sidebar.jsx
│   │   ├── AppHeader.jsx
│   │   └── SessionExpiredModal.jsx
│   └── hooks/
├── public/
├── package.json
├── vite.config.js
├── tailwind.config.js
├── postcss.config.js
├── .eslintrc.cjs
├── .prettierrc
└── index.html
### Configuración Tailwind
- Aplicar los tokens del design-system:
  - Variables CSS en `:root` y `.dark`.
  - Extender `theme.colors` con paleta corporativa (green, teal, blue,
    red, pink-soft, amber-soft, etc.).
  - Configurar `font-family` con el stack del sistema.
  - `borderRadius` (sm 4px, md 6px, lg 8px, xl 12px).
  - `darkMode: 'class'`.

### Componentes base obligatorios
Sólo los necesarios para login + layouts vacíos. NO componentes de
pantallas funcionales:
- `Button` (variantes green, blue, red, white).
- `Input` con label y mensaje de error.
- `Modal` base reutilizable.
- `Sidebar` (sin items funcionales, solo estructura).
- `AppHeader` con logo parking y curva decorativa SVG.

### AuthProvider
- React Context con estado `{ user, isLoading, isAuthenticated }`.
- Al montar: llama a `GET /auth/me` con cookie.
- Si responde 200 → guarda user en estado.
- Si responde 401 → user=null, isAuthenticated=false.
- Expone funciones `login(credentials)`, `logout()`, `refresh()`.
- Si `user.password_must_change === true`, marca flag en contexto.

### ProtectedRoute
- HOC que envuelve rutas privadas.
- Si `!isAuthenticated` → redirige a `/login` preservando la URL destino.
- Si `password_must_change` → redirige a `/cambio-password` salvo que ya
  esté en esa ruta.
- Soporta restricción por rol opcional: `<ProtectedRoute role="ADMIN">`.

### Cliente Axios
- Base URL del backend (configurable vía variable de entorno `VITE_API_URL`).
- Default `withCredentials: true`.
- Interceptor de response:
  - Si 401 y no es la ruta de login → emite evento `session-expired`.
  - Si 403 → toast rojo "Sin permisos".
  - Si 5xx → toast rojo genérico.
- Componente global `<SessionExpiredModal>` escucha el evento y muestra
  el modal, con botón "Volver a iniciar sesión" que redirige a `/login`.

### Internacionalización
- Configuración `react-i18next` con `es` (default) y `en`.
- Carga estática de los JSON.
- Componente o hook para cambiar idioma en runtime.
- Persistir preferencia en localStorage (`parking.locale`).
- Claves agrupadas: `common.*`, `auth.*`, `errors.*`.

### Modo oscuro
- ThemeProvider con estado `'light' | 'dark'`.
- Toggle manual (sin detección automática).
- Aplica clase `.dark` al `<html>`.
- Persiste en localStorage (`parking.theme`).

### Pantallas concretas (mínimas)
- `LoginPage` (mockup-friendly, pero suficiente):
  - Campos: login, password.
  - Botón submit.
  - Mensaje de error inline si 401.
  - Sin "recordarme" en Fase 1 (cookie persistente ya lo cubre).
- `CambioPasswordPage`:
  - Campos: password actual, password nueva, confirmar nueva.
  - Validación cliente de complejidad (mín 10, mayúscula, minúscula,
    dígito, símbolo).
  - Llama a `POST /auth/cambiar-password`.
  - Al éxito, redirige a `/admin` o `/empleado` según rol.

### Layouts (vacíos)
- `AdminLayout`: sidebar (sin items todavía) + header + área de contenido
  con `<Outlet />` de React Router.
- `EmpleadoLayout`: equivalente, ajustado a móvil responsive.

Ambos layouts incluyen el botón de logout y el toggle de tema/idioma en
el header.

### Testing
- **Smoke test** del `AuthProvider` con Vitest + RTL.
- **Smoke test** del flujo de login mockeando Axios.
- **Smoke test** de los interceptores 401.
- Configuración base de Vitest con `jsdom` y setup de `@testing-library`.
- Cobertura objetivo de este change: ≥60% (es bootstrap; las pantallas
  funcionales subirán a 80%).

### CI/CD
- Añadir al pipeline existente (`.github/workflows/ci.yml`) un nuevo job:
  - Setup Node.js 20.
  - `npm ci` en `frontend/`.
  - `npm run lint`.
  - `npm run build`.
  - `npm test -- --coverage`.
- Job depende del de backend pero no del deploy.
- Sin deploy del frontend en este change (lo haremos en uno posterior
  cuando definamos hosting: Nginx propio, GitHub Pages, etc.).

## Capabilities afectadas y deltas

Este change toca mínimamente las siguientes specs (postura mixta — el
frontend forma parte del comportamiento observable):

### `openspec/specs/auth-local/spec.md` — Añadir Requirement
```markdown
### Requirement: Frontend de autenticación local disponible
El sistema DEBE proporcionar una interfaz web (pantalla de login + cambio
de contraseña obligatorio) que permita autenticarse vía el endpoint
`POST /auth/login` y gestionar la sesión con la cookie `parking_SESSION`.

#### Scenario: Login exitoso desde la UI
- GIVEN un empleado con credenciales válidas
- WHEN abre /login en el navegador y envía login + password
- THEN el sistema crea sesión y redirige a /admin o /empleado según rol
- AND la cookie parking_SESSION queda activa

#### Scenario: Login fallido desde la UI
- GIVEN credenciales inválidas
- WHEN intenta hacer login
- THEN el sistema muestra error inline en el formulario
- AND no crea sesión

#### Scenario: Cambio obligatorio tras reset
- GIVEN un empleado cuyo password_must_change=true
- WHEN inicia sesión
- THEN el sistema redirige obligatoriamente a /cambio-password
- AND no permite navegar a otras rutas hasta cambiarla

#### Scenario: Sesión expirada
- GIVEN un usuario con sesión activa
- WHEN el backend responde 401 a cualquier petición
- THEN el frontend muestra modal "Sesión expirada"
- AND al cerrarlo redirige a /login

Implementación:
- Endpoints existentes: POST /auth/login, POST /auth/cambiar-password,
  GET /auth/me (ver openapi.yaml)
- Pantallas: S-01 (Login), S-30 (Cambio password obligatorio), S-33
  (Sesión expirada) — ver ui-screens.md
- Flujo F-05 — ver ux-flows.md
```

### `openspec/specs/empleados/spec.md` — NO se toca
El frontend de empleados (CRUD desde panel admin) vendrá en el siguiente
change funcional, no en este. Este bootstrap solo deja preparado el
layout admin vacío con el sidebar.

## Decisiones técnicas que el design.md debe documentar

1. **Vite vs Create React App**: por qué Vite (build más rápido, mejor
   DX, future-proof). CRA está en mantenimiento.
2. **TailwindCSS vs styled-components / CSS modules**: por qué Tailwind
   (mantiene consistencia visual del design-system, atómico, menos coste
   de mantenimiento).
3. **React Router vs Tanstack Router / Wouter**: por qué React Router
   (estándar de facto, agentes lo conocen mejor).
4. **Axios vs fetch nativo**: por qué Axios (interceptores, manejo de
   cookies más limpio).
5. **react-i18next vs alternativas**: por qué (ecosistema maduro,
   integración React idiomática).
6. **Vitest vs Jest**: por qué Vitest (nativo de Vite, sin config extra).
7. **shadcn/ui vs Material UI vs Ant Design**: si se decide usar, por
   qué shadcn (componentes copia-pega, no librería pesada, personalizable
   con Tailwind).

## Criterios de aceptación del change

Se considera completo cuando:
1. ✅ Los 11 objetivos verificables se cumplen.
2. ✅ `verification-specialist` da PASS.
3. ✅ `reality-checker` da READY (puede hacer login real contra el
   backend ya implementado y ver el layout admin vacío).
4. ✅ Pipeline CI pasa: backend (ya verde) + frontend nuevo.
5. ✅ PR aprobado por PR_REVIEWER.
6. ✅ Deltas de `specs/` listos para `sync` y `archive`.

## Tareas estimadas (orientación para tasks.md)

Granularidad: cada tarea <4 horas.

1. Inicializar Vite + React 18 en `frontend/`.
2. Configurar ESLint + Prettier + EditorConfig.
3. Configurar TailwindCSS con tokens del design-system (variables CSS
   light + dark).
4. Configurar React Router con rutas mínimas.
5. Implementar cliente Axios con `withCredentials` y base URL configurable.
6. Implementar AuthProvider + useAuth.
7. Implementar ProtectedRoute con soporte de rol opcional.
8. Componentes base: Button, Input, Modal.
9. AppHeader con logo parking + curva decorativa SVG + toggle tema + toggle
   idioma + botón logout.
10. Sidebar base (estructura vacía, sin items).
11. AdminLayout y EmpleadoLayout.
12. Implementar i18n con ES/EN y persistencia en localStorage.
13. Implementar ThemeProvider con persistencia en localStorage.
14. Pantalla LoginPage funcional integrada con backend.
15. Pantalla CambioPasswordPage con validación de complejidad.
16. SessionExpiredModal + interceptor 401.
17. Smoke tests: AuthProvider, LoginPage, interceptor 401.
18. Workflow CI: job de frontend (lint + build + test).
19. Actualizar `docs/PROJECT.md` si surgen variables nuevas
    (`VITE_API_URL`, etc.).
20. Actualizar `openspec/specs/auth-local/spec.md` con los 4 Scenarios
    de UI propuestos arriba.

## Notas para los agentes implementadores

- **No implementar pantallas de gestión** (empleados, plazas, etc.). Eso
  es trabajo de los siguientes changes funcionales.
- **Respeta el design-system al pie de la letra**. Si el design dice
  `--green: #639922`, no improvises otro verde porque "queda mejor".
- **Convención de commits**: Conventional Commits con scope `frontend`:
  - `chore(frontend): inicializar Vite + React 18`
  - `feat(frontend): AuthProvider con cookies`
  - `feat(frontend): LoginPage integrada con backend`
  - `test(frontend): cobertura del AuthProvider`
- **Branch**: `feature/<issue-id>-frontend-bootstrap` según convención
  de `docs/PROJECT.md`.
- **PR target**: `develop`.
- **Reviewer obligatorio**: el de `PR_REVIEWER`.
- **Idioma**: textos en español por defecto (claves i18n agrupadas), pero
  nombres de variables/componentes en inglés cuando sean técnicos puros
  (`Button`, `Input`, `useAuth`).

=== FIN DEL BRIEF ===

=== INICIO DEL PROMPT ===

# Misión

Adoptar la **postura mixta de SDD** sobre las 13 specs de OpenSpec
existentes: cada Requirement debe incluir, además de su comportamiento
de backend (que ya tiene), Scenarios de UI que describan el
comportamiento observable por el usuario en las pantallas
correspondientes. Además, añadir una sección `## UI` al final de cada
spec.md con enlaces a `docs/ui-screens.md` y `docs/ux-flows.md`.

# Principio rector

La spec describe **comportamiento observable**. Un Requirement no está
completo si solo describe la API; debe describir también cómo se
manifiesta ese comportamiento en la UI. El detalle de implementación
(componentes React, CSS, anatomía visual) NO va en la spec — va en los
documentos de detalle, a los que la spec apunta.

## Lo que SÍ va en la spec
- Scenarios de UI en formato Given/When/Then describiendo lo que el
  usuario ve y hace.
- Lista de pantallas, modales y flujos que materializan los Requirements.
- Enlaces a `docs/ui-screens.md`, `docs/ux-flows.md` para el detalle.

## Lo que NO va en la spec
- Código React, JSX, CSS, clases de Tailwind.
- Anatomía visual de componentes (eso vive en `docs/design-system.md`).
- Descripción pixel-perfect de pantallas (eso vive en `docs/ui-screens.md`).

# Entradas obligatorias

- `openspec/specs/*/spec.md` — las 13 specs canónicas (algunas pueden
  estar vacías o con poco contenido, depende del estado del proyecto).
- `docs/ui-screens.md` — catálogo de pantallas S-XX y modales M-XX con
  fichas que indican qué capability implementa cada uno.
- `docs/ux-flows.md` — flujos F-XX con sus capabilities asociadas.
- `docs/design-system.md` — sistema visual (solo lectura, para entender
  contexto; no se cita aquí salvo de pasada).
- `docs/openapi.yaml` — endpoints por capability.
- `docs/PROJECT.md` — RN-XX y módulos.

# Las 13 capabilities (no inventes ni elimines)

1. `auth-local` (🟢 Fase 1) — **YA TIENE Scenarios de UI** del change
   `frontend-bootstrap`. Revisar que esté coherente con el patrón.
2. `auth-sso` (🔵 Fase 2) — el SSO casi no tiene UI propia (la landing
   es externa). Solo Scenarios mínimos sobre `/ssocallback` y el modal
   de sesión expirada.
3. `empleados`
4. `plazas`
5. `asignaciones-fijas`
6. `liberaciones`
7. `solicitudes`
8. `visitantes`
9. `motivos-rechazo`
10. `disponibilidad-calendario`
11. `auditoria-retencion`
12. `notificaciones-email` — capability transversal sin UI propia.
    Solo añadir Scenarios mínimos si los emails generan algo observable
    para el usuario (un toast, un banner, etc.).
13. `exportaciones` — botones de exportación dispersos en distintas
    pantallas. Los Scenarios de UI describen "el usuario pulsa exportar
    y recibe un fichero".

# Metodología — cómo enriquecer cada spec

Para cada capability, sigue estos pasos en orden:

### Paso 1 — Identifica las pantallas y modales asociados

En `docs/ui-screens.md` busca todas las fichas S-XX y M-XX cuya sección
"Capability OpenSpec" mencione esta capability. Por ejemplo:

- `empleados` → S-04, M-01, M-04, M-05.
- `solicitudes` → S-02, S-22, M-02, M-03.
- `plazas` → S-05, M-06, M-07.
- etc.

Si una pantalla menciona dos capabilities (ej. S-04 toca `empleados` y
`asignaciones-fijas`), incluye esa pantalla en ambas specs.

### Paso 2 — Identifica los flujos asociados

En `docs/ux-flows.md` busca todos los flujos F-XX cuya sección
"Capabilities implicadas" mencione esta capability.

### Paso 3 — Para cada Requirement existente, añade Scenarios de UI

Toma los Requirements actuales (que describen comportamiento de API) y
**añade Scenarios adicionales** que describan el mismo comportamiento
desde la UI. NO elimines los Scenarios de API existentes — los
mantienes intactos y añades los de UI al lado.

#### Formato de un Scenario de UI

```markdown
#### Scenario: [descripción del comportamiento observable]
- **GIVEN** [estado inicial visible al usuario en alguna pantalla]
- **WHEN** [acción del usuario en la UI: pulsar botón, rellenar campo,
  abrir modal]
- **THEN** [resultado observable en la UI: toast, navegación, nuevo
  estado de pantalla, mensaje de error inline]
- **AND** [efectos adicionales: cookie, email enviado, registro en
  auditoría]
```

#### Ejemplo bueno

```markdown
#### Scenario: Admin crea empleado desde el panel
- **GIVEN** un admin autenticado en la pantalla S-04 (Empleados y plaza
  fija)
- **WHEN** pulsa "Nuevo empleado" abriendo el modal M-04
- **AND** rellena los campos obligatorios (nombre, apellidos, login,
  email, departamento) y pulsa "Guardar"
- **THEN** el sistema crea el empleado
- **AND** muestra un toast verde "Empleado creado correctamente"
- **AND** el nuevo empleado aparece en la lista de la pantalla S-04
- **AND** se cierra el modal
- **AND** se registra la acción en AUDIT_LOG (RN transversal de
  auditoría)
```

#### Ejemplo malo (NO hagas esto)

```markdown
#### Scenario: Admin crea empleado
- WHEN admin envía POST /empleados con body {nombre, apellidos, ...}
- THEN responde 201 con el empleado en formato JSON
```

(Eso es Scenario de API, ya lo tienes. Lo que falta es el de UI.)

### Paso 4 — Cubre los casos importantes

Para cada Requirement, intenta cubrir al menos:

- **Camino feliz desde la UI**: el usuario hace la acción correctamente
  y ve el resultado esperado.
- **Validación de cliente**: el usuario introduce datos inválidos y la
  UI muestra error inline sin llegar al backend.
- **Error del backend**: el backend responde 4xx/5xx y la UI muestra
  el error correspondiente (banner amber para 409, banner rojo para
  validación de negocio, modal de sesión expirada para 401).

No hace falta cubrir el 100% de los casos en UI, solo los más
representativos. Para el resto, sigue valiendo el Scenario de API.

### Paso 5 — Añade sección `## UI` al final del spec.md

Después de todos los Requirements, antes de "Casos límite" o
"Dependencias", añade una sección con esta estructura exacta:

```markdown
## UI

### Pantallas que implementan esta capability
- **S-XX** · [Nombre breve] — ver `docs/ui-screens.md`
- **S-YY** · [Nombre breve] — ver `docs/ui-screens.md`

### Modales asociados
- **M-XX** · [Nombre breve] — ver `docs/ui-screens.md`
- **M-YY** · [Nombre breve] — ver `docs/ui-screens.md`

### Flujos de usuario relacionados
- **F-XX** · [Nombre del flujo] — ver `docs/ux-flows.md`
- **F-YY** · [Nombre del flujo] — ver `docs/ux-flows.md`

### Notas de implementación frontend
- Las validaciones cliente deben reflejar las del servidor.
- En caso de 409 conflict, mostrar banner amber inline con el mensaje
  del servidor.
- En caso de 422 (regla de negocio violada), mostrar banner rojo
  inline citando RN-XX si aplica.
- El sistema de diseño y los tokens están en `docs/design-system.md`.
```

Si la capability no tiene UI propia (ej. `notificaciones-email`,
`auditoria-retencion` desde la perspectiva del usuario final), pon una
nota explícita:

```markdown
## UI

Esta capability es **transversal/de infraestructura** y no tiene
pantallas propias. Sus efectos se observan en:
- [Listar dónde se observan los efectos, ej. "toasts disparados por
  notificaciones-email tras aprobar solicitudes (S-03)"]
```

# Reglas anti-duplicación

- **NO copies el contenido detallado** de `ui-screens.md` o
  `ux-flows.md` dentro del spec.md. Solo enlazas con el código
  identificador (S-XX, M-XX, F-XX) y un nombre breve. Si alguien quiere
  el detalle, abre el documento correspondiente.
- **NO copies definiciones de design-system**. Si necesitas mencionar
  un color o componente, hazlo con su nombre semántico ("toast verde",
  "banner amber") y referencia `docs/design-system.md`.
- **NO copies endpoints** de `openapi.yaml`. Si necesitas mencionarlos,
  cita el `operationId` y remite a `docs/openapi.yaml`.

# Reglas de preservación del contenido existente

- **NO elimines** Scenarios de API existentes.
- **NO modifiques** Requirements existentes salvo para añadir los
  Scenarios de UI.
- **NO cambies** las secciones "Entidades implicadas", "Endpoints",
  "Permisos", "Dependencias" que ya tengas.
- Si un spec.md tiene una sección "Dependencias" rellenada por el
  prompt anterior, **respétala íntegra**.
- Si encuentras inconsistencias entre un spec y `ui-screens.md` (ej. la
  pantalla S-XX dice cosas distintas a lo que dice el spec), **NO
  inventes** la resolución. Márcalo con `_[verificar inconsistencia
  con ui-screens.md]_` y déjalo a revisión humana.

# Casos especiales

### `auth-local` ya enriquecida
Esta capability tuvo Scenarios de UI añadidos en el change
`frontend-bootstrap`. **Revisa que el patrón coincida** con el de este
prompt:
- Si los Scenarios siguen el formato Given/When/Then ✅, no toques.
- Si la sección `## UI` existe ✅, déjala.
- Si falta cualquiera de las dos cosas, complétalo siguiendo el mismo
  patrón que las demás capabilities.

### `auth-sso` 🔵 Fase 2
- La autenticación ocurre en la landing externa, no en la app.
- Los únicos comportamientos observables en parking son:
  - El callback redirige tras autorización exitosa.
  - 403 si el usuario no existe o está inactivo.
  - Modal de sesión expirada (igual que Fase 1).
- Genera Scenarios mínimos solo para esos casos. Marca claramente que
  es Fase 2 en cada Scenario.

### `notificaciones-email`
- No tiene pantallas propias.
- Sus efectos visibles son toasts en las pantallas que las disparan
  (S-02, S-22, etc.).
- Sección `## UI` solo con la nota de "transversal".

### `auditoria-retencion`
- Tiene UI propia para consulta del admin (S-08).
- Pero la mayoría de su comportamiento es transversal (registro
  automático de acciones).
- Documenta Scenarios de UI solo para S-08 (consulta y filtros).

### `exportaciones`
- No tiene pantalla propia, son botones dispersos en S-04, S-22, S-08.
- Los Scenarios de UI describen "usuario pulsa botón Exportar, elige
  formato CSV/XLSX, recibe descarga del navegador".

# Salida esperada

Modifica los 13 archivos `openspec/specs/<capability>/spec.md`:

1. Añade Scenarios de UI a los Requirements existentes (sin eliminar
   los de API).
2. Añade sección `## UI` al final con la estructura indicada.
3. No toques ninguna otra sección.

Al terminar, devuelve un **resumen** con:
- Lista de las 13 capabilities procesadas.
- Para cada una, cuántos Scenarios de UI nuevos se añadieron.
- Para cada una, qué pantallas/modales/flujos quedaron enlazados.
- Capabilities marcadas con `_[verificar inconsistencia]_` para
  revisión humana, si hubo.

# Restricciones de generación

- **Idempotente**: ejecutar este prompt dos veces sobre los mismos specs
  no debería duplicar Scenarios ni secciones `## UI`.
- **No inventes pantallas o modales** que no existan en
  `docs/ui-screens.md`. Si una capability necesita una pantalla que no
  está, márcalo como pendiente y NO la inventes.
- **Idioma**: español, mismo estilo que las specs existentes.
- **Si una capability tiene 5+ Requirements**, no es necesario añadir
  un Scenario de UI a cada uno. Cubre los más representativos
  (3-4 Scenarios de UI por spec suelen ser suficientes).
- Si el contexto se llena, **procesa por bloques** de 4-5 specs y pide
  continuar. Empieza por las que tienen más UI: `empleados`,
  `solicitudes`, `plazas`, `asignaciones-fijas`, `visitantes`.

=== FIN DEL PROMPT ===