# Tasks — admin-improvements

Lista de tareas del panel ADMIN. Cada ítem que indique el usuario se añade aquí con
su área, descripción y (cuando se conozca) endpoints/ficheros afectados.

## Pendientes

<!-- Formato por tarea:
- [ ] N. (área: frontend|backend|fullstack) Título breve
      Detalle: qué hay que hacer.
      Afecta: ficheros/endpoints (si se conocen).
-->

- [x] 1. (frontend) **Unificar la fuente de los numerales KPI (quitar ceros tachados).**
      Detalle: los numerales usan Geist Mono (`--font-mono`, ceros con barra). Cambiar
      los numerales a Geist (`--font-sans`/`--font-display`) conservando
      `font-variant-numeric: tabular-nums` para la alineación. Revisar los 12 usos de
      `--font-mono` y mantener solo los que sean monospace real (IDs/código).
      Afecta: `src/styles/tokens.css` (o la clase `.mono`), `src/components/StatTile.tsx`
      (`.stat-tile-val.mono`), `src/styles/components.css` (usos de `--font-mono`).

- [x] 2. (frontend) **Traducir "Actividad reciente" (panel admin).**
      Detalle: `humanizeAction()` pinta el enum de auditoría crudo en inglés y el chip
      muestra `entry.entityType` sin traducir. Mapear acciones (p. ej. `VOLUNTARY_RELEASE`,
      `CANCEL_RELEASE`, `CANCEL_APPROVED_REQUEST`, `ADMIN_CANCEL_APPROVED_REQUEST`, …) y
      tipos de entidad (`RELEASE`, `REQUEST`, …) a español vía i18n. Enumerar los códigos
      reales que emite el backend de auditoría.
      Afecta: `src/pages/DashboardPage.tsx` (`humanizeAction`, `ActivityRow`),
      `src/i18n/locales/{es,en}.ts` (`dashboard.activity.*`).

- [x] 3. (fullstack) **Reserva admin de plaza Y puesto a la vez, con ubicación por pasos.**
      Detalle: el formulario admin de "Nueva reserva" debe permitir reservar **plaza y
      puesto en el mismo alta** (hoy es un recurso). Selección de ubicación mediante un
      **stepper dinámico**: si se eligen ambos recursos → paso "Plaza" (elegir plaza en
      plano o auto por categoría) y luego paso "Puesto" (elegir puesto en plano o auto);
      si solo se elige uno, se muestra únicamente ese paso. (Alternativa descartada: 2
      tabs en una sola pantalla — peor cuando se piden ambos.) Backend: el alta admin
      genera un `Request` APPROVED por recurso (reutiliza `POST /requests/admin` /
      `adminAssignRequest`); validar disponibilidad de cada recurso por separado (409 por
      recurso) y auditar ambos.
      Afecta: `src/hooks/useReservationBooking.ts`, `src/components/TopbarReserve.tsx` y el
      modal/wizard de reserva admin; posiblemente `OccupancyAssignModal.tsx`; backend
      `RequestController`/`RequestService` (`adminAssign`), `docs/openapi.yaml`.

- [x] 4. (frontend) **Filtro por tipo de recurso en el listado de solicitudes del admin.**
      Detalle: añadir un selector Plaza / Puesto / Todos en `PendingRequestsPage` (bandeja
      admin), igual que el ya implementado en "Mis solicitudes" del empleado. Filtro en
      cliente por `resourceType`, combinándose con las pestañas y la búsqueda existentes.
      Afecta: `src/pages/PendingRequestsPage.tsx`, `src/i18n/locales/{es,en}.ts`
      (reutilizar `requests.mine.filterResource`/`filterAllResources` o claves admin).

- [x] 5. (frontend) **Unificar el icono de puesto (sillón → pantalla) en una única fuente.**
      Detalle: `ResourceIcon` ya usa `device-desktop` para puesto, pero ~8 sitios
      hardcodean `armchair` (sillón). Exponer el nombre del icono en un único lugar
      (p. ej. `export const RESOURCE_ICON: Record<ResourceType,string> = { PARKING:'parking',
      DESK:'device-desktop' }` junto a `ResourceIcon`) y sustituir todos los literales
      `armchair` por esa constante / por `<ResourceIcon>`, de modo que cambiar el icono en
      el futuro sea un único punto. Decisión a confirmar: `FloorPlanSidePanel` usa `armchair`
      solo para puestos EXECUTIVE (distinción ejecutivo vs normal) — ¿unificar también a
      pantalla o mantener un icono propio para ejecutivo?
      Afecta: `src/components/ResourceIcon.tsx` (fuente única), `ResourceModeSwitch.tsx`,
      `FloorPlanSidePanel.tsx`, `DeskFormModal.tsx`, `wizard/StepSummary.tsx`,
      `wizard/StepResourceType.tsx`, `pages/DesksPage.tsx`, `pages/ResourcesPage.tsx`,
      `pages/AdministrativeReleasesPage.tsx`.

- [x] 6. (fullstack) **Rediseñar la pantalla de Plano (admin).** Reorganizar el layout:
      - [x] 6.1 (frontend) **Minimapa debajo del mapa**, no superpuesto (hoy se solapa en la
            esquina inferior derecha del plano).
      - [x] 6.2 (frontend) **Contadores más grandes a la derecha del mapa** (Ocupado / Libre /
            Liberado hoy / Solicitado): fila mapa = [mapa a la izquierda][contadores grandes
            a la derecha].
      - [x] 6.3 (frontend) **Listado de puestos debajo del mapa, a todo el ancho** (mover la
            lista del panel lateral derecho a una sección full-width bajo el mapa, con la
            búsqueda por número).
      - [x] 6.4 (fullstack) **Clic en un puesto → modal de asignación** para elegir **empleado o
            visitante** para el **día seleccionado**. Reutilizar/combinar la asignación
            puntual admin (`adminAssignRequest`) y la reserva de visitante
            (`VisitorReservationModal`), con validación de disponibilidad del puesto ese día.
      Afecta: `src/pages/FloorPlanPage.tsx`, `FloorPlanMinimap.tsx`, `FloorPlanSidePanel.tsx`,
      `FloorPlanStatus.tsx`, `FloorPlanSurface.tsx`, `FloorPlanFilters.tsx`, nuevo modal de
      asignación (empleado/visitante); backend `adminAssign` + visitantes si hace falta.

- [x] 7. (frontend) **Pantalla "Liberar" (admin): ancho, fechas y liberación en lote.**
      - 7.1 **Ancho completo** en "Por empleado" (aprovechar todo el ancho aunque el
            selector de empleado quede más estrecho, no a todo el ancho).
      - 7.2 **Autoscroll al motivo**: al pulsar "Liberar" sin motivo, hacer scroll (y foco)
            hasta el campo Motivo del modal de liberación (obligatorio, min 5 chars).
      - 7.3 **"Por fecha": input de fecha más estrecho pero con la fecha más grande/legible**
            (no a todo el ancho; tipografía mayor).
      - 7.4 **"Por fecha": selección múltiple en la tabla** (checkboxes) + botón **"Liberar
            seleccionadas"** con confirmación (motivo único aplicado a todas).
      Afecta: `src/pages/ReleaseHubPage.tsx`, `AdministrativeReleasesPage.tsx`,
      `ReleaseByDatePage.tsx`, y el modal de liberación (ver tarea 8).

- [x] 8. (frontend) **Rediseñar los modales de Ocupación (Asignar / Liberar), "más
      espectacular" + empleado o visitante.**
      Detalle: los modales "Asignar recurso" (`OccupancyAssignModal`) y "Liberar cancelando
      la solicitud" (`ReleaseResourceModal`) usan una lista de definición desalineada (label
      arriba, valor indentado) que queda pobre. Rediseñarlos con una cabecera/resumen claro
      del recurso+fecha, mejor jerarquía y espaciado. En **Asignar**, añadir un toggle
      **Empleado / Visitante** (reutiliza la reserva de visitante); enlaza con la tarea 6.4
      (mismo modal al clicar un puesto en el Plano).
      Afecta: `src/components/OccupancyAssignModal.tsx`, `src/components/ReleaseResourceModal.tsx`,
      `VisitorReservationModal.tsx` (reutilización), estilos en `components.css`.

- [x] 9. (frontend) **Quitar la acción "Ver" en Visitantes.** En la tabla de fichas de
      visitante sobra el botón Ver (ojo); con Editar y Reservar basta. Eliminar el botón y,
      si queda sin uso, el modal de detalle asociado (`setDetailId`/detail view).
      Afecta: `src/components/VisitorsPanel.tsx` (y limpieza del estado/modal de detalle),
      posibles claves i18n `visitors.actions.view` sin uso.

- [x] 10. (frontend) **Acciones del listado de Empleados en un menú de 3 puntos.** Hoy la
      columna Acciones apila Editar / Dar de baja / Restablecer contraseña y se rompe en
      varias líneas (queda feo). Dejar la fila en una sola línea: mover "Dar de baja" y
      "Restablecer contraseña" (y quizá Editar) a un menú kebab (⋮) que se abre al pulsar,
      reutilizando `Menu`/`Popover`.
      Afecta: `src/pages/EmployeesPage.tsx`, `src/components/Menu.tsx`/`Popover.tsx`.

- [x] 11. (frontend) **Resaltar más los inputs de texto en toda la app.** `.field-input`
      usa `border: 0.5px solid var(--border)` (medio píxel, casi invisible sobre blanco).
      Subir a 1–1.5px con un color de borde más visible (p. ej. `--line-strong` o un token
      de campo dedicado) y, si hace falta, un relleno sutil, para que inputs/selects/textarea
      se distingan claramente. Cambio global vía la clase base.
      Afecta: `src/styles/components.css` (`.field-input` y variantes), `tokens.css` (token de
      borde de campo si se crea).

- [x] 12. (frontend) **Rediseñar el editor de asignación fija (Plaza fija / Puesto fijo).**
      - 12.1 **Quitar el selector "Vigencia"** (solo existe "Indefinida"): eliminarlo y dejar
             la asignación siempre indefinida (mantener la nota explicativa).
      - 12.2 **Asignación por día simple**: sustituir el flujo confuso ("elige recurso → marca
             días → cambia de recurso → marca otros días") por una **lista de días de la
             semana con un selector por día**, cuyas opciones son los recursos disponibles +
             "Sin recurso para este día". Permite distintos recursos por día de forma directa.
             Mantener "Aplicar a toda la semana" como atajo (mismo recurso todos los días).
      - 12.3 Igual para **Puesto fijo**, y además un **icono de mapa** junto al puesto elegido
             que abre el plano enfocado en ese puesto con la **animación que marca el lugar**
             (reutilizar `FloorPlanFocusModal`/`ViewInPlanTrigger`).
      Afecta: `src/components/EmployeeFormModal.tsx`, `DayResourceBadges.tsx`,
      `FloorPlanFocusModal.tsx`/`ViewInPlanTrigger.tsx`; backend fixed-assignments si cambia
      el contrato de guardado por día.

- [x] 13. (frontend) **Mejorar el tab "Histórico" del modal de empleado.** Resumen de solo
      lectura (plaza/puesto fijos vigentes por días + metadatos Origen de autenticación /
      Alta / Última modificación) con el mismo layout de lista de definición desalineado.
      Rediscñar con jerarquía y espaciado claros (p. ej. filas recurso→días, y metadatos en
      pares etiqueta/valor bien alineados o en tarjetitas). Revisar si "Histórico" es el
      nombre adecuado (hoy muestra estado actual, no un registro cronológico).
      Afecta: `src/components/EmployeeFormModal.tsx` (tab Histórico), estilos en `components.css`.

- [x] 14. (frontend) **Confirmación al "Dar de baja" un empleado.** El botón de baja en el
      listado de empleados debe pedir confirmación (acción sensible) antes de desactivar,
      reutilizando `ConfirmDialog`. (Va con la tarea 10: la baja vivirá en el menú kebab.)
      Afecta: `src/pages/EmployeesPage.tsx`, `src/components/ConfirmDialog.tsx`.

- [x] 15. (frontend) **Recursos: nombres completos en los selectores + quitar card de config.**
      - 15.1 Renombrar los selectores principales a **"Plazas de parking"** y **"Puestos de
             trabajo"** (hoy "Plazas" / "Puestos").
      - 15.2 **Quitar la card "Configuración del parking"** (ajustar nº total de plazas): no
             aporta valor.
      Afecta: `src/pages/ResourcesPage.tsx` (tabs), `src/pages/ParkingSpacesPage.tsx` (card),
      `src/i18n/locales/{es,en}.ts` (`resources.tabs.*`).

- [x] 16. (fullstack) **Desactivar plaza/puesto: comprobar asignaciones futuras y avisar.**
      Al desactivar un recurso, verificar que no esté asignado en el futuro (asignación fija
      de empleado, reserva/solicitud aprobada, o reserva de visitante) para hoy o fechas
      futuras. Si lo está, **avisar** (y bloquear o pedir confirmación explícita indicando a
      quién/qué afecta), en vez de desactivar silenciosamente.
      Afecta: backend (endpoint de desactivación de plaza/puesto + consulta de asignaciones
      futuras), `src/pages/ParkingSpacesPage.tsx`, `src/pages/DesksPage.tsx` (aviso), i18n.

- [x] 17. (frontend) **Rediseñar la pantalla de Configuración.**
      - 17.1 **Ancho completo** para toda la pantalla de Configuración.
      - 17.2 **Cards Manual / Automático seleccionables con check**: al pulsar la card se elige
             el modo (con indicador de check/seleccionado). **Quitar el selector (dropdown)
             "Modo de aprobación de solicitudes"** de debajo (redundante).
      - 17.3 **Separación entre las cards** Manual/Automático (hoy pegadas).
      - 17.4 **Fin de semana reservable**: sustituir el checkbox feo por un **toggle
             customizado**, y **añadir botón Guardar a esa card** (que NO se autoguarde).
      Afecta: `src/pages/SettingsPage.tsx` (form de modo aprobación, `WeekendReservableCard`),
      estilos en `components.css`.

- [x] 18. (fullstack) **Dirección del parking con mapa (Mapbox) para elegir el punto exacto.**
      Añadir un mapa Mapbox en la card "Dirección del parking" para seleccionar el punto
      exacto (marcador arrastrable / clic). El botón "Ir al parking" del empleado usaría esas
      coordenadas.
      Decisiones (confirmadas por el usuario):
      - **Guardar latitud y longitud** en `SystemSettings` (nueva migración; `lat`/`lng`
        nullable), junto a la dirección textual. No geocodificar en runtime.
      - **Token de Mapbox** lo aporta el usuario en un archivo de properties/entorno (no se
        commitea). El mapa renderiza en el frontend, así que el token se inyecta como variable
        de build/runtime de Vite (p. ej. `VITE_MAPBOX_TOKEN` vía `.env`), con token público
        restringido por dominio; alternativa: servirlo desde el backend si se prefiere.
      Afecta: `src/pages/SettingsPage.tsx` (`ParkingAddressCard`), nuevo componente de mapa,
      `useSettings`/`settingsApi`, backend `SystemSettings` (+ migración `lat`/`lng`),
      `openapi.yaml`; config de entorno (`.env`/properties) para el token.

- [x] 19. (fullstack, sobre todo frontend) **Reasignar / intercambiar recurso con otro
      empleado — falta TODO el frontend.** El backend ya expone `POST /requests/admin/reassign`
      ({requestId,newResourceId}) y `POST /requests/admin/swap` ({requestIdA,requestIdB})
      (auditoría `ADMIN_REASSIGN_RESOURCE` / `ADMIN_SWAP_RESOURCES`), pero NO hay hooks ni UI:
      no se puede reasignar ni intercambiar desde la app. Añadir hooks
      (`useAdminReassignRequest`/`useAdminSwapRequests`), acción en la rejilla/modal de
      Ocupación sobre una celda APPROVED (reasignar a otro recurso libre; intercambiar con
      otro empleado de la misma fecha/tipo) y avisos por email a ambos afectados (ya
      contemplado en el backend). Enlaza con el modal de Ocupación (tarea 8).
      Afecta: `src/hooks/useRequests.ts`, `OccupancyAssignModal.tsx`/nuevo modal, páginas de
      Ocupación; contrato ya existente en `openapi.yaml`.

- [x] 20. (frontend) **Editar empleado: tabs sticky + modal más grande.** [HECHO junto a esta
      petición] Tabs fijas al hacer scroll y Dialog `wide` (900px) para que el plano se vea bien.

## Completadas

_(ninguna todavía)_
