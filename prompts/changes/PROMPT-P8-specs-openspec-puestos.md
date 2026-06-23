# PROMPT-P8 — Actualizar las specs de OpenSpec para el cambio "Puestos"

> **Cuándo ejecutar**: DESPUÉS de haber aplicado los prompts P1-P7
> (README, data-model, openapi, security, design-system, ui-screens,
> ux-flows). Las specs referencian a esos documentos ya actualizados.
>
> **Adjuntar a la sesión**:
> - Todos los `openspec/specs/*/spec.md` existentes.
> - `openspec/config.yaml`.
> - `docs/data-model.md` actualizado.
> - `docs/openapi.yaml` actualizado.
> - `docs/ui-screens.md` actualizado.
> - `docs/ux-flows.md` actualizado.
> - `CHANGE-puestos.md`.
>
> **Importante**: este prompt es grande. Si el contexto se llena, procesa
> por bloques (primero las generalizaciones, luego las capabilities
> nuevas) y pide continuar.

---

```
=== INICIO PROMPT-P8 ===

# Misión

Actualizar las especificaciones de OpenSpec para incorporar el cambio de
alcance "puestos de oficina": (1) generalizar las capabilities existentes
de parking al concepto de "recurso reservable", y (2) crear las
capabilities nuevas `puestos` y `plano`. Todo siguiendo la **postura
mixta SDD** ya adoptada (cada Requirement describe comportamiento
observable de backend + UI, con sección `## UI` que enlaza a
ui-screens.md y ux-flows.md).

# Principios

1. **Generalización, no duplicación**: la lógica de asignación fija,
   solicitud, liberación y disponibilidad es común a plazas y puestos.
   NO se duplica describiendo lo mismo dos veces; se generaliza a
   "recurso" y se parametriza por tipo (PARKING/PUESTO).
2. **Postura mixta**: cada Requirement con Scenarios de backend Y de UI.
   El detalle visual vive en los docs; la spec enlaza.
3. **Preservar lo existente**: no elimines Requirements ni Scenarios que
   ya funcionan para parking. Los generalizas o los complementas.
4. **Idempotencia**: ejecutar dos veces no duplica contenido.

# Parte A — Generalizar capabilities existentes a "recurso"

Las siguientes capabilities deben generalizarse de "plaza" a "recurso
reservable" (plaza o puesto):

### `plazas` → concepto de recurso
- Opción 1: renombrar conceptualmente a `recursos` (si OpenSpec permite
  renombrar capability sin romper changes archivados).
- Opción 2 (más segura): mantener `plazas` como capability pero ampliar
  su Purpose para indicar que es el caso PARKING del recurso reservable,
  y crear `puestos` como capability hermana. El concepto común de
  "recurso" se documenta en ambas.
- **Decide y justifica** cuál usar. Recomendación: Opción 2 para no
  romper la trazabilidad de los changes ya archivados (`plazas` ya está
  implementado). El concepto "recurso" se describe como nota compartida.

### `solicitudes`
- Generalizar los Requirements para que la solicitud apunte a un
  **recurso** (plaza o puesto), no solo a plaza.
- Añadir Requirement: "Empleado solicita plaza y/o puesto desde una
  pantalla unificada generando solicitudes independientes".
  - Scenario backend: dos POST /solicitudes independientes.
  - Scenario UI: una sola pantalla S-26, el empleado marca ambos, se
    crean dos solicitudes; si una falla (duplicada) la otra se crea igual.
- Añadir Requirement: "Empleado solicita un puesto pinchándolo en el
  plano".
  - Scenario UI: en S-25, click en puesto LIBRE → M-24 confirmación →
    solicitud creada.

### `asignaciones-fijas`
- Generalizar a recurso: un empleado puede tener asignación fija de plaza
  Y de puesto simultáneamente.
- Añadir Requirement/Scenario: "Admin asigna un puesto de dirección L-V"
  (categoria DIRECCION, 5 días).

### `liberaciones`
- Generalizar a recurso: se puede liberar tanto plaza como puesto.
- Los puestos DIRECCION son liberables igual que los ESTANDAR.

### `disponibilidad-calendario`
- Generalizar el cálculo a recurso, parametrizado por tipo.
- Añadir Scenario: disponibilidad de puestos para una fecha (alimenta el
  plano).

Para cada una, añade/actualiza la sección `## UI` con las pantallas y
flujos nuevos relacionados (S-25, S-26, M-24, F-50, F-51, F-52, etc.).

# Parte B — Capabilities nuevas

## Capability `puestos`

Crea `openspec/specs/puestos/spec.md` con:

### Purpose
Gestión de los 65 puestos de trabajo de la oficina como recursos
reservables de tipo PUESTO. Incluye categorías (ESTANDAR/DIRECCION),
activación y datos del puesto. La lógica de asignación/solicitud/
liberación/disponibilidad es la común de recurso (ver capabilities
correspondientes).

### Requirements mínimos

**Requirement: Admin gestiona el catálogo de puestos**
- Scenario backend: crear puesto (POST /puestos) con numero único.
- Scenario backend: numero duplicado → 409.
- Scenario backend: editar categoría de un puesto (PUT /puestos/{id}).
- Scenario backend: baja lógica (activo=false).
- Scenario UI: admin en S-40 crea/edita/desactiva puestos vía M-20/M-21.

**Requirement: Distinción de puestos de dirección**
- Scenario: un puesto categoria=DIRECCION se marca como tal.
- Scenario UI: en el plano (S-25/S-41) los DIRECCION se pintan distintos
  (ver design-system).
- Scenario: un DIRECCION es liberable igual que un ESTANDAR (no tiene
  reglas especiales de liberación).

**Requirement: Activación/desactivación de puestos**
- Scenario: un puesto inactivo no aparece como disponible ni en el plano
  como reservable.

### Sección `## UI`
- Pantallas: S-40 (gestión puestos), parte de S-25/S-41 (plano).
- Modales: M-20, M-21, M-22.
- Flujos: F-60, F-61, F-64.
- Enlaces a docs/ui-screens.md, docs/ux-flows.md.

### Dependencias
- Requiere `empleados` (para asignaciones y solicitudes).
- Es un tipo de recurso; comparte lógica con `solicitudes`,
  `asignaciones-fijas`, `liberaciones`, `disponibilidad-calendario`.
- Consumida por `plano`.

## Capability `plano`

Crea `openspec/specs/plano/spec.md` con:

### Purpose
Visualización interactiva de los puestos sobre el plano de la oficina.
Imagen de fondo (asset estático) + overlay de marcadores posicionados por
coordenadas relativas en %. Permite al empleado ver el estado de cada
puesto y solicitar uno libre pinchándolo; permite al admin posicionar los
puestos (editor visual). Solo aplica a puestos (el parking no tiene plano).

### Requirements mínimos

**Requirement: Empleado consulta el plano con estado por fecha**
- Scenario backend: GET /plano?fecha=... devuelve puestos con coordenadas
  y estado (LIBRE/ASIGNADO/LIBERADO/SOLICITADO/MIO).
- Scenario UI: S-25 muestra el plano coloreado; leyenda; selector de
  fecha; responsive con zoom/paneo en móvil + lista de libres.
- Scenario privacidad: el empleado ve estado pero NO el nombre del titular
  (solo ocupado/libre); el admin sí ve el nombre.

**Requirement: Empleado solicita un puesto desde el plano**
- Scenario UI: click en puesto LIBRE → M-24 confirma fecha → se crea la
  solicitud (cruza con capability solicitudes).
- Scenario: no se puede iniciar solicitud sobre un puesto no-LIBRE.

**Requirement: Admin posiciona los puestos (editor visual)**
- Scenario backend: PUT /puestos/posiciones actualiza coord_x/coord_y en
  bloque.
- Scenario UI: S-42 editor, arrastrar marcadores, guardar.
- Scenario: las coordenadas se almacenan en % (0-100), independientes de
  la resolución.

**Requirement: Carga inicial de coordenadas (seed)**
- Scenario: existe un seed inicial de coordenadas de los 65 puestos
  (puestos-seed.json) que se carga al inicializar, ajustable luego con el
  editor.

### Sección `## UI`
- Pantallas: S-25 (plano empleado), S-41 (plano admin), S-42 (editor).
- Modales: M-24.
- Flujos: F-50, F-53, F-62.
- Enlaces a docs/ui-screens.md, docs/ux-flows.md, docs/design-system.md
  (estados visuales).

### Dependencias
- Requiere `puestos` (los recursos a pintar).
- Invoca `disponibilidad-calendario` (estado de cada puesto por fecha).
- Invoca `solicitudes` (al solicitar desde el plano).
- Transversal `auditoria-retencion`.

# Parte C — Actualizar config.yaml

Añade `puestos` y `plano` a la lista de capabilities en
`openspec/config.yaml`. La lista pasa de 13 a 15 capabilities.

# Parte D — Resumen de salida

Al terminar, devuelve:
1. Lista de specs modificadas (generalizadas) y su cambio principal.
2. Lista de specs nuevas creadas (`puestos`, `plano`).
3. config.yaml actualizado (15 capabilities).
4. Un grafo Mermaid actualizado de dependencias incluyendo puestos y plano.
5. Propuesta de orden de changes para implementar (ver más abajo).

# Parte E — Propuesta de orden de changes (no implementar, solo listar)

Sugiere el orden de changes OpenSpec para implementar el cambio, alineado
con la decisión del CHANGE-puestos.md (refactor primero):

1. `refactor-recurso-generico` — generaliza Plaza/Solicitud/Asignacion/
   Liberacion/Disponibilidad a recurso. Sin cambio visible. Tests
   existentes siguen verdes.
2. `puestos-backend` — entidad Puesto, categorías, CRUD, seed.
3. `puestos-frontend` — pantallas S-40, gestión, modales (si no van
   juntas con backend según postura mixta; valorar).
4. `plano-mvp` — endpoint /plano, vista S-25 empleado, estados, solicitud
   desde plano.
5. `plano-editor` — S-42 editor de posiciones admin.
6. `solicitud-unificada` — pantalla S-26 plaza+puesto.

(Si la postura mixta hace que backend+frontend vayan en el mismo change,
fusiona los pares correspondientes.)

# Restricciones

- No elimines Requirements/Scenarios existentes de parking.
- No dupliques la lógica común; generaliza a recurso.
- Mantén la postura mixta: Scenarios de backend Y de UI, con sección
  `## UI` enlazando a los docs.
- No inventes pantallas/endpoints que no existan en los docs actualizados.
- Si encuentras contradicción entre un spec y los docs actualizados,
  márcala con `_[verificar]_` y no la resuelvas inventando.
- Idempotente.
- Idioma: español.

=== FIN PROMPT-P8 ===
```

---

## Notas

### Cuándo ejecutarlo
Solo después de P1-P7. Las specs enlazan a los documentos; si no están
actualizados, los enlaces (S-25, F-50, etc.) apuntarían a contenido
inexistente.

### Decisión que el prompt te hará tomar
**Cómo tratar la capability `plazas`**: renombrarla a `recursos` o
mantenerla y crear `puestos` hermana. El prompt recomienda mantenerla
(Opción 2) para no romper la trazabilidad de los changes ya archivados de
parking. Confírmalo cuando lo ejecutes.

### Verificación tras ejecutar
```powershell
# Confirmar que existen las 2 capabilities nuevas
Test-Path openspec\specs\puestos\spec.md
Test-Path openspec\specs\plano\spec.md

# Confirmar que config.yaml tiene 15 capabilities
Select-String -Path openspec\config.yaml -Pattern "puestos|plano"
```

### Después de P8: implementación
Con las specs actualizadas, lanzas los changes en el orden de la Parte E.
El primero (`refactor-recurso-generico`) es el más delicado: toca código
ya implementado. Su criterio de aceptación es claro: **el parking sigue
funcionando idéntico y los tests existentes siguen en verde**. Cuando
llegues a ese punto, conviene que preparemos juntos el brief de ese
change con cuidado.

### Sobre la carga del seed
El `puestos-seed.json` se usará en el change `puestos-backend` como script
de seed de la BD. El prompt P8 ya lo menciona en la capability `plano`
(Requirement de carga inicial).
