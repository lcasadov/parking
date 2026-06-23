# Prompts de actualización — Cambio de alcance "Puestos de Oficina"

> Este archivo contiene los prompts para actualizar la documentación
> existente de parking e incorporar la gestión de **puestos de oficina** y
> el **plano interactivo**.
>
> **Modo de uso**: cada prompt se ejecuta en una sesión nueva de Claude,
> adjuntando el documento existente que se va a modificar (y los que se
> indiquen como contexto). Claude devuelve el documento actualizado, NO
> regenerado desde cero.
>
> **Orden obligatorio** (cada uno alimenta al siguiente):
> 1. README.md
> 2. docs/data-model.md
> 3. docs/openapi.yaml
> 4. docs/security-design.md
> 5. docs/design-system.md
> 6. docs/ui-screens.md
> 7. docs/ux-flows.md
>
> El paso 8 (specs de OpenSpec) tiene su propio prompt aparte, a ejecutar
> DESPUÉS de estos 7.
>
> **Decisiones del cambio (contexto común a todos los prompts)**:
> Antes de cada prompt, ten a mano el documento `CHANGE-puestos.md`. Todos
> los prompts asumen estas decisiones ya tomadas:
> - Generalización a "recurso reservable" (Plaza y Puesto son tipos de
>   Recurso, tipo PARKING/PUESTO).
> - Solicitudes independientes con UI unificada (el empleado pide plaza
>   y/o puesto en una pantalla; se crean solicitudes separadas; el admin
>   resuelve cada una por separado).
> - Puestos con categoría ESTANDAR / DIRECCION. Los DIRECCION se asignan
>   normalmente L-V y se distinguen visualmente; son liberables igual que
>   los demás.
> - 65 puestos, una sola planta, solo los puestos son reservables.
> - Plano = imagen de fondo + overlay de marcadores por coordenadas
>   relativas en % (0-100).
> - Imagen del plano como asset estático del frontend; coordenadas en BD.
> - 1 puesto = 1 persona.
> - El parking NO tiene plano (sigue como lista de números de plaza).
> - Coordenadas seed iniciales + editor visual para ajustar.

---

## Tabla de contenidos

1. [PROMPT-P1 — README.md](#prompt-p1--readmemd)
2. [PROMPT-P2 — data-model.md](#prompt-p2--data-modelmd)
3. [PROMPT-P3 — openapi.yaml](#prompt-p3--openapiyaml)
4. [PROMPT-P4 — security-design.md](#prompt-p4--security-designmd)
5. [PROMPT-P5 — design-system.md](#prompt-p5--design-systemmd)
6. [PROMPT-P6 — ui-screens.md](#prompt-p6--ui-screensmd)
7. [PROMPT-P7 — ux-flows.md](#prompt-p7--ux-flowsmd)

---

## PROMPT-P1 — README.md

**Adjuntar**: `README.md` actual + `CHANGE-puestos.md`.

```
=== INICIO PROMPT-P1 ===

# Misión

Actualiza el `README.md` maestro de parking para incorporar la gestión de
puestos de oficina y el plano interactivo, según las decisiones del
documento `CHANGE-puestos.md` adjunto. NO regeneres el README entero:
aplica los cambios preservando todo lo existente sobre parking.

# Cambios a aplicar

## 1. Visión / Resumen del producto
Amplía la descripción para indicar que parking gestiona dos tipos de
recursos reservables: **plazas de parking** y **puestos de oficina**.
Ambos comparten el mismo modelo de funcionamiento (asignación fija por
días, solicitud puntual, liberación).

## 2. Concepto de "recurso reservable"
Añade una explicación del concepto generalizado: una plaza y un puesto
son dos tipos del mismo concepto (recurso reservable), con tipo PARKING
o PUESTO. La lógica de asignación/solicitud/liberación/disponibilidad es
común.

## 3. Sección nueva: Puestos de oficina
Añade una sección que describa:
- 65 puestos numerados en una sola planta.
- Categorías ESTANDAR y DIRECCION.
- Los puestos DIRECCION se asignan normalmente L-V y se distinguen
  visualmente; son liberables como cualquier otro.
- El plano interactivo: imagen de la planta con los puestos coloreados
  por estado; el empleado pincha un puesto libre para solicitarlo.
- El parking NO tiene plano (sigue como lista de plazas).

## 4. Solicitud unificada
Explica que el empleado puede solicitar plaza y/o puesto para una misma
fecha desde una sola pantalla, pero se generan solicitudes
independientes que el admin aprueba/rechaza por separado.

## 5. Glosario
Añade términos: Recurso reservable, Puesto, Plano, Categoría de puesto
(ESTANDAR/DIRECCION), Coordenada relativa, Editor de plano.

## 6. Alcance / Roadmap
Si el README tiene sección de alcance o fases, añade los puestos y el
plano. Indica que la implementación seguirá: refactor a recurso genérico
→ puestos → plano.

# Restricciones
- No elimines nada de lo existente sobre parking.
- Mantén el tono, estructura y formato del README original.
- Devuelve el README completo actualizado.
- Si algo ya estaba contemplado, no lo dupliques.

=== FIN PROMPT-P1 ===
```

---

## PROMPT-P2 — data-model.md

**Adjuntar**: `docs/data-model.md` actual + `CHANGE-puestos.md` +
`README.md` actualizado.

```
=== INICIO PROMPT-P2 ===

# Misión

Actualiza `docs/data-model.md` para incorporar el modelo de datos de
puestos de oficina y el plano, y la generalización a "recurso reservable",
según `CHANGE-puestos.md`. NO regeneres el documento entero.

# Decisión de herencia/modelado a tomar y documentar

Debes decidir y justificar la estrategia de generalización de recurso.
Opciones:
- (a) Tabla única `recursos` con campo discriminador `tipo`
  (PARKING/PUESTO) y columnas nullable específicas de cada tipo.
- (b) Herencia JPA JOINED: tabla base `recursos` + tablas hijas `plazas`
  y `puestos`.
- (c) Herencia JPA SINGLE_TABLE.

Recomendación a evaluar: opción (b) JOINED si los atributos divergen
bastante (el puesto tiene coordenadas y categoría que la plaza no tiene),
o (a) si se quiere simplicidad. Decide, justifica brevemente, y modela en
consecuencia.

# Cambios a aplicar

## 1. Entidad/tabla Recurso (generalización)
Crea el modelo del recurso reservable común. Atributos compartidos:
id, tipo (PARKING/PUESTO), identificador visible (numero/codigo), activo,
fecha_creacion. Las entidades Plaza y Puesto heredan o extienden de aquí
según la estrategia elegida.

## 2. Entidad/tabla Puesto
- id (o FK a recurso según estrategia)
- numero (1-65, único)
- categoria (ESTANDAR / DIRECCION) — enum o CHECK constraint
- coord_x DECIMAL(5,2) — 0 a 100, porcentaje relativo
- coord_y DECIMAL(5,2) — 0 a 100, porcentaje relativo
- activo BIT
- fecha_creacion

## 3. Entidad/tabla Plano (configuración)
- id
- nombre / descripcion
- imagen_ref (referencia al asset estático del frontend, ej.
  'plano-oficina.png')
- ancho, alto (opcional, informativo)
- version
Una sola fila por ahora (una planta).

## 4. Refactor de Plaza
Generaliza Plaza para que sea un recurso de tipo PARKING. Mantén su
numero/codigo. Documenta el cambio respecto al modelo anterior.

## 5. Refactor de Solicitud
La solicitud pasa a apuntar a un recurso genérico (plaza o puesto) en
lugar de solo a plaza. Añade el mecanismo (FK a recurso + tipo, o como
resulte de la estrategia de herencia). Mantén el resto de campos
(empleado, fecha, estado, motivo, motivo_rechazo_id, comentario_rechazo).

## 6. Refactor de AsignacionFija y Liberacion
Generaliza ambas para operar sobre recurso (plaza o puesto). Un empleado
puede tener asignación fija de plaza Y de puesto simultáneamente.

## 7. Diagrama ER
Actualiza el diagrama ER (si existe) con Recurso, Puesto, Plano y las
relaciones generalizadas.

## 8. Mapeo JPA
Actualiza la tabla de mapeo entidad→clase→repositorio con las nuevas
entidades (Recurso, Puesto, Plano) y los repositorios correspondientes.

## 9. Migraciones Flyway
Añade las migraciones nuevas necesarias (numéralas según las existentes):
- Creación de tabla(s) de recurso/puesto/plano según estrategia.
- Migración de datos: convertir las plazas existentes en recursos
  PARKING (si ya había plazas en el modelo).
- Refactor de FKs en solicitudes, asignaciones_fijas, liberaciones.
- Seed de los 65 puestos con sus coordenadas (referencia al archivo
  puestos-seed.json; las coordenadas pueden insertarse vía script de
  seed, no necesariamente en la migración).

## 10. Política de retención
Añade Puesto y Plano a las entidades vivas que NO se purgan.

# Restricciones
- DDL ejecutable en SQL Server 2022.
- No rompas el modelo existente de parking; generalízalo.
- Documenta claramente qué cambia respecto a la versión anterior (sección
  "Cambios respecto a la versión sin puestos" al final, para trazabilidad).
- Devuelve el documento completo actualizado.

=== FIN PROMPT-P2 ===
```

---

## PROMPT-P3 — openapi.yaml

**Adjuntar**: `docs/openapi.yaml` actual + `docs/data-model.md`
actualizado + `CHANGE-puestos.md`.

```
=== INICIO PROMPT-P3 ===

# Misión

Actualiza `docs/openapi.yaml` para incorporar los endpoints de puestos,
plano y la generalización de solicitudes/asignaciones/liberaciones a
recurso, según el data-model actualizado y `CHANGE-puestos.md`. NO
regeneres el archivo entero.

# Cambios a aplicar

## 1. Tags nuevos
- `Puestos`
- `Plano`

## 2. Endpoints de Puestos
- GET /puestos — listar puestos (con filtro por categoria, activo).
- GET /puestos/{id} — detalle.
- POST /puestos — crear (ADMIN).
- PUT /puestos/{id} — editar: numero, categoria, coordenadas, activo
  (ADMIN).
- DELETE /puestos/{id} — baja lógica (ADMIN).
- PUT /puestos/{id}/posicion — actualizar solo coord_x, coord_y (ADMIN,
  usado por el editor visual).
- PUT /puestos/posiciones — actualización masiva de coordenadas (ADMIN,
  para guardar todo el editor de una vez). Body: array de {id, coord_x,
  coord_y}.

## 3. Endpoints de Plano
- GET /plano — obtener configuración del plano (imagen_ref, dimensiones,
  versión) + lista de puestos con sus coordenadas y estado para una
  fecha dada (query ?fecha=YYYY-MM-DD).
  Respuesta pensada para que el frontend pinte el overlay: cada puesto con
  numero, categoria, coord_x, coord_y, estado (LIBRE/ASIGNADO/LIBERADO/
  SOLICITADO/MIO).

## 4. Generalizar endpoints de disponibilidad y solicitud
- GET /disponibilidad debe aceptar parámetro `tipoRecurso`
  (PARKING/PUESTO) para filtrar por tipo.
- POST /solicitudes debe aceptar el recurso solicitado (plaza o puesto)
  vía un campo que identifique recurso + tipo.
- La pantalla unificada de solicitud generará dos llamadas POST
  /solicitudes independientes (una PARKING, una PUESTO); el contrato del
  endpoint no cambia más allá de aceptar cualquier tipo de recurso.

## 5. Schemas nuevos
- `Puesto`: {id, numero, categoria, coordX, coordY, activo}
- `PuestoCrearReq`, `PuestoModificarReq`
- `PosicionPuesto`: {id, coordX, coordY}
- `Plano`: {id, imagenRef, ancho, alto, version}
- `PlanoConEstado`: {plano, puestos: [PuestoConEstado]}
- `PuestoConEstado`: {numero, categoria, coordX, coordY, estado,
  titularNombre?, esMio}
- enum `CategoriaPuesto`: [ESTANDAR, DIRECCION]
- enum `TipoRecurso`: [PARKING, PUESTO]
- enum `EstadoPuestoDia`: [LIBRE, ASIGNADO, LIBERADO, SOLICITADO, MIO]

## 6. Schemas modificados
- `Solicitud`: generalizar el recurso solicitado (que indique tipo y
  referencia).
- `DisponibilidadItem`: añadir `tipoRecurso`.

# Restricciones
- YAML válido, todos los $ref resuelven.
- No rompas los endpoints de parking existentes; generalízalos.
- Devuelve el archivo completo actualizado.
- Si hay conflicto con algo existente, para y explica antes de tocar.

=== FIN PROMPT-P3 ===
```

---

## PROMPT-P4 — security-design.md

**Adjuntar**: `docs/security-design.md` actual + `docs/openapi.yaml`
actualizado + `CHANGE-puestos.md`.

```
=== INICIO PROMPT-P4 ===

# Misión

Actualiza `docs/security-design.md` para incorporar el RBAC de puestos y
plano. NO regeneres el documento entero.

# Cambios a aplicar

## 1. Matriz RBAC — filas nuevas
| Área | Acción | ADMIN | EMPLEADO |
|---|---|---|---|
| Puestos | Listar | ✅ | ✅ (para ver el plano y solicitar) |
| Puestos | Crear/editar/baja | ✅ | ❌ |
| Puestos | Posicionar (editor) | ✅ | ❌ |
| Plano | Ver con estado | ✅ | ✅ |
| Plano | Editar configuración | ✅ | ❌ |

## 2. Justificación
- El empleado necesita listar puestos y ver el plano con estado para
  poder solicitar un puesto libre pinchándolo.
- El empleado NO puede crear/editar/posicionar puestos ni editar el plano.
- El editor visual de posiciones es exclusivo del admin.

## 3. Consideraciones de seguridad del plano
- La imagen del plano es un asset estático del frontend (no expone datos
  sensibles por sí misma).
- El endpoint /plano con estado por fecha revela quién ocupa cada puesto
  (titularNombre). Evaluar si el empleado debe ver el nombre del titular
  o solo el estado ocupado/libre (decisión de privacidad). Documenta la
  decisión: por defecto, el empleado ve solo estado (libre/ocupado) sin
  nombre; el admin ve el nombre del titular.

# Restricciones
- No toques otras secciones (auth, OWASP, CORS, etc.).
- Devuelve el documento completo actualizado.

=== FIN PROMPT-P4 ===
```

---

## PROMPT-P5 — design-system.md

**Adjuntar**: `docs/design-system.md` actual + `CHANGE-puestos.md` +
(opcional) la imagen del plano.

```
=== INICIO PROMPT-P5 ===

# Misión

Actualiza `docs/design-system.md` para añadir los tokens y patrones
visuales del plano de puestos. NO regeneres el documento entero.

# Cambios a aplicar

## 1. Estados visuales del puesto en el plano
Define los colores/estilos para cada estado de un marcador de puesto
sobre el plano, usando la paleta existente:
| Estado | Relleno | Borde | Significado |
|---|---|---|---|
| LIBRE | verde-soft | green-border | disponible para solicitar |
| ASIGNADO | gris/neutro | border-strong | ocupado por titular fijo |
| LIBERADO | pink-soft | pink-border | liberado, disponible ese día |
| SOLICITADO | amber-soft | amber-border | con solicitud pendiente |
| MIO | blue-soft / green sólido | blue/green | el puesto del usuario actual |

## 2. Marcador de puesto DIRECCION
Define la distinción visual de los puestos categoría DIRECCION (ej. borde
dorado, icono de estrella, o color de número rojo coherente con el plano
original). Debe distinguirse de un vistazo de los ESTANDAR.

## 3. Anatomía del marcador
- Forma (círculo), tamaño en desktop vs móvil.
- Posición del número dentro del marcador.
- Comportamiento hover/focus (resaltado, tooltip con número y estado).
- Estado seleccionado (cuando el empleado pincha para solicitar).

## 4. Leyenda del plano
Define el componente de leyenda que explica los colores de estado.

## 5. Componente plano-contenedor
- Comportamiento responsive: zoom/paneo en móvil, vista completa en
  desktop.
- Imagen de fondo + capa de marcadores posicionados en %.

# Restricciones
- Usa SOLO tokens de color ya existentes en el design-system. Si necesitas
  uno nuevo (ej. dorado para DIRECCION), decláralo explícitamente en la
  paleta.
- No rompas nada existente.
- Devuelve el documento completo actualizado.

=== FIN PROMPT-P5 ===
```

---

## PROMPT-P6 — ui-screens.md

**Adjuntar**: `docs/ui-screens.md` actual + `docs/openapi.yaml`
actualizado + `docs/design-system.md` actualizado + `CHANGE-puestos.md`.
(Opcional: mockups del plano si ya están hechos.)

```
=== INICIO PROMPT-P6 ===

# Misión

Actualiza `docs/ui-screens.md` para añadir las pantallas y modales nuevos
del módulo de puestos y plano. NO regeneres el documento entero. Usa la
misma ficha estandarizada que las pantallas existentes (S-XX / M-XX con
ruta, rol, capability, endpoints, etc.).

# Pantallas y modales a añadir

## Empleado
- **S-25** · Plano de puestos (empleado) — `/empleado/plano`
  - Muestra el plano con puestos coloreados por estado para una fecha.
  - Selector de fecha. Leyenda. Click en puesto LIBRE → inicia solicitud.
  - Responsive: zoom/paneo en móvil + lista de puestos libres como apoyo.
  - Capability: `plano`, `solicitudes`.
- **S-26** · Solicitud unificada (empleado) — `/empleado/solicitar`
  - Reemplaza/amplía la pantalla de solicitar plaza: ahora permite pedir
    plaza Y/O puesto para una fecha.
  - Genera solicitudes independientes (una por recurso).
  - Capability: `solicitudes`.
- **S-27** · Mi puesto / mi semana ampliada — integrar puestos en la vista
  "Mi semana" del empleado (S-20 existente): mostrar tanto plaza como
  puesto asignado por día.

## Admin
- **S-40** · Gestión de puestos — `/admin/puestos`
  - Listado de los 65 puestos: número, categoría, titular fijo, estado.
  - Filtros por categoría. Acciones: crear, editar, activar/desactivar.
  - Capability: `puestos`, `asignaciones-fijas`.
- **S-41** · Plano de puestos (admin) — `/admin/plano`
  - Vista del plano con todos los puestos y sus titulares.
  - Acceso al editor de posiciones.
  - Capability: `plano`.
- **S-42** · Editor de plano (admin) — `/admin/plano/editor`
  - Posicionar/mover los marcadores de puestos sobre la imagen
    (drag & drop o ajuste por coordenadas).
  - Guarda coordenadas vía PUT /puestos/posiciones.
  - Capability: `plano`.

## Modales
- **M-20** · Nuevo puesto (admin)
- **M-21** · Editar puesto (admin) — número, categoría, activo
- **M-22** · Asignar puesto fijo a empleado (admin) — días de la semana
- **M-23** · Liberar mi puesto (empleado) — confirmación
- **M-24** · Confirmar solicitud de puesto desde el plano (empleado) —
  al pinchar un puesto libre, modal de confirmación con la fecha.

# Para cada pantalla/modal, ficha completa
Ruta, rol, capability OpenSpec, propósito, componentes, endpoints
consumidos (operationId del openapi), acciones, estados de carga, estados
vacíos, permisos por elemento, notas UX.

# Notas transversales a añadir
- En móvil, el plano usa zoom/paneo + lista complementaria de puestos
  libres.
- El estado de cada puesto se obtiene de GET /plano?fecha=...
- Privacidad: el empleado ve estado (libre/ocupado) sin nombre del
  titular; el admin ve el nombre.

# Restricciones
- No elimines pantallas existentes; intégrate con ellas (especialmente
  S-20 Mi semana y la antigua pantalla de solicitar plaza).
- Usa la misma convención de numeración y formato de ficha.
- Devuelve el documento completo actualizado.

=== FIN PROMPT-P6 ===
```

---

## PROMPT-P7 — ux-flows.md

**Adjuntar**: `docs/ux-flows.md` actual + `docs/ui-screens.md`
actualizado + `CHANGE-puestos.md`.

```
=== INICIO PROMPT-P7 ===

# Misión

Actualiza `docs/ux-flows.md` para añadir los flujos de usuario del módulo
de puestos y plano. NO regeneres el documento entero. Usa la misma
plantilla de flujo que los existentes (F-XX con actor, capabilities,
happy path, caminos alternativos, cruce OpenSpec).

# Flujos a añadir

## Empleado
- **F-50** · Solicitar puesto desde el plano
  - Abre S-25, selecciona fecha, ve puestos libres, pincha uno, confirma
    en M-24, se crea la solicitud.
  - Alternativas: puesto se ocupa mientras decide (409), fuera de ventana
    (422), ya tiene solicitud pendiente ese día (409).
- **F-51** · Solicitud unificada plaza + puesto
  - Abre S-26, marca que quiere plaza y puesto para una fecha, envía.
  - Se crean DOS solicitudes independientes.
  - Alternativas: una se puede crear y la otra no (ej. duplicada) — mostrar
    resultado por separado.
- **F-52** · Liberar mi puesto
  - Desde "Mi semana" o desde el plano, libera su puesto para un día.
  - El puesto pasa a LIBERADO y queda disponible para otros.
- **F-53** · Consultar el plano sin solicitar
  - El empleado solo mira el plano para ver ocupación/disponibilidad.

## Admin
- **F-60** · Crear y posicionar un puesto nuevo
  - Crea el puesto (M-20), luego lo posiciona en el editor (S-42).
- **F-61** · Asignar puesto fijo de dirección (L-V)
  - Asigna un puesto DIRECCION a un directivo de lunes a viernes.
- **F-62** · Reposicionar puestos en el editor
  - Abre S-42, arrastra varios marcadores, guarda en bloque.
- **F-63** · Liberación administrativa de un puesto
  - El admin libera el puesto de un empleado para una fecha.
- **F-64** · Aprobar/rechazar solicitud de puesto
  - Igual que solicitudes de plaza, pero para puesto. Si venía de una
    solicitud unificada, se resuelve independientemente de la de plaza.

# Mapa de cobertura
Actualiza (o añade) la tabla flujo→RN cubriendo las RN-PUESTO-XX nuevas.

# Restricciones
- No elimines flujos existentes.
- Cada flujo con al menos una rama de error/cancelación y cruce a OpenSpec.
- No inventes endpoints que no estén en openapi.yaml actualizado.
- Devuelve el documento completo actualizado.

=== FIN PROMPT-P7 ===
```

---

## Notas finales

### Orden y dependencias
Respeta el orden 1→7. Cada documento referencia a los anteriores. Si
cambias algo en data-model después de haber hecho openapi, vuelve a
revisar openapi.

### Verificación tras cada prompt
- **README**: busca "puesto" y "recurso reservable" — deben aparecer.
- **data-model**: existe tabla/entidad Puesto, Plano, modelo de Recurso,
  migraciones nuevas, y plazas generalizadas.
- **openapi**: existen tags Puestos y Plano, endpoints /puestos, /plano,
  schemas nuevos.
- **security**: filas RBAC de Puestos y Plano en la matriz.
- **design-system**: estados visuales del puesto + marcador DIRECCION.
- **ui-screens**: pantallas S-25, S-26, S-40, S-41, S-42 + modales M-20
  a M-24.
- **ux-flows**: flujos F-50 a F-53 y F-60 a F-64.

### Commit por documento
Haz commit en git después de cada documento actualizado, con mensaje tipo
`docs(puestos): actualizar data-model con recurso, puesto y plano`. Así
tienes rollback granular.

### Lo que queda para después
- **Specs de OpenSpec** (paso 8): prompt aparte, tras estos 7.
- **Mockups del plano con Claude Design**: pueden hacerse en paralelo a
  los prompts 5-7, y sus resultados realimentan design-system y
  ui-screens.
- **Carga del seed**: el archivo `puestos-seed.json` se usará en el script
  de seed de la BD durante la implementación (change de puestos).
