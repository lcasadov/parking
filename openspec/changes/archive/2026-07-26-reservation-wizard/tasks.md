# Tasks — reservation-wizard

Reconstrucción as-built. Todas las tareas ya están implementadas y mergeadas en `develop`
(rama de origen `feat/fullstack/rediseno-completo`); se marcan `[x]` a partir de los commits
`97c2366`, `fec0a3f`, `39e46f4`, `a6cf2c9`, `4bdc6f4`, `300cc10`, `9f1acc2`, `de493c1`,
`865c219`, `5fbc9c4`, `726d83c`, `57b48cd`, `5bb2785`.

## 1. Asistente base (5 pasos) y CTA

- [x] 1.1 `ReservationWizard`: modal multipaso (tipo de recurso · fechas · empleado · ubicación · resumen) con estado local (`WizardState`), navegación adelante/atrás y validación por paso (`isStepValid`) (`fec0a3f`)
- [x] 1.2 `TopbarReserve`: CTA "Nueva reserva" del topbar abre el asistente para ADMIN y el modal de solicitud propia (`CreateRequestModal`) para EMPLOYEE; AGENCIA sin botón (`fec0a3f`)
- [x] 1.3 `StepResourceType`, `StepDates` (modos día/rango/sueltos con `MiniCalendar`), `StepEmployee` (buscador con avatar), `StepLocation`, `StepSummary`, `StepResult` (`fec0a3f`)
- [x] 1.4 Confirmación: `useReservationBooking` invoca `POST /requests/admin` por fecha vía `Promise.allSettled` y agrega el resultado por fecha (`fec0a3f`)

## 2. Pantalla completa, stepper fijo/clicable y riel lateral

- [x] 2.1 `Dialog` gana variante `fullScreen`/`flushBody`; el asistente pasa a modal a pantalla completa con stepper y pie fijos y solo el cuerpo del paso con scroll (`39e46f4`)
- [x] 2.2 `computeReachableStep`/`goToStep`: los pasos ya completados son clicables en el stepper para saltar directamente a ellos (`39e46f4`)
- [x] 2.3 `WizardRail`: riel lateral (desktop) con el valor ya elegido de cada paso como resumen en vivo, y salto de sección; en móvil se pliega al stepper horizontal (`57b48cd`)

## 3. Auto-asignación por categoría y plaza sugerida

- [x] 3.1 Backend: `GET /requests/admin/suggested-space?employeeId&date` (ADMIN) — previsualiza la plaza que la auto-asignación daría, sin crearla; `available=false` si no hay disponibilidad (`a6cf2c9`)
- [x] 3.2 `EmployeeOptionResponse` expone `category`; el selector de empleado del asistente (`StepEmployee`) muestra la categoría bajo el nombre (`a6cf2c9`)
- [x] 3.3 "Cualquier plaza libre" reencuadrado como "Asignación automática (según categoría)" (`LocationParking`) (`a6cf2c9`)
- [x] 3.4 `useSuggestedSpaces` + `StepSummary`: el resumen resuelve y muestra la plaza EXACTA por fecha (o "sin plaza libre esa fecha") ANTES de confirmar, con estados cargando/error (`a6cf2c9`)
- [x] 3.5 Tests backend del endpoint de vista previa (`RequestControllerTest`, `RequestSuggestedSpaceServiceTest`) (`a6cf2c9`)

## 4. Ubicación manual agrupada por prioridad de categoría

- [x] 4.1 `utils/parkingPriority.ts` (`groupEligibleByPriority`): espejo cliente de `RequestService.autoAssignParkingSpace` — ALTA (CEO/Consejo/Director N1-N2) → plantas altas primero, resto → plantas profundas primero, desempate por número (`4bdc6f4`)
- [x] 4.2 `LocationParking`: rejilla manual dividida en "Sugeridas para {categoría} · planta -N" y "Otras plazas"; sin categoría, fallback por número sin apartado de sugeridas (`4bdc6f4`)
- [x] 4.3 Test unitario de `parkingPriority` (5/5) (`4bdc6f4`)
- [x] 4.4 Fix tipográfico: número de plaza en fuente sans con cifras tabulares (`rzw-res-num`), no en `.mono` (Geist Mono, cero tachado) (`300cc10`)

## 5. Multi-día con recurso distinto por día

- [x] 5.1 `wizardTypes`: `LocationMode` (`ALL`/`PER_DAY`) + `DayChoice` (mapa fecha→elección) (`865c219`)
- [x] 5.2 `StepLocation`: segmentado "Misma para todos"/"Distinta por día" (visible solo con >1 fecha); al activar `PER_DAY` sin elecciones previas se siembra con la elección de `ALL` (`865c219`)
- [x] 5.3 `StepLocationPerDay`: tira de fechas con su elección; el día activo abre su editor (plano/rejilla) filtrado por la disponibilidad de ESE día; atajos "aplicar a todos" y "auto en los vacíos" (solo plaza) (`865c219`)
- [x] 5.4 `utils/wizardBooking.ts` (`isLocationComplete`, `resolveBookingEntries`, `allModeAsDayChoice`); `useReservationBooking` pasa a recibir `entries` (fecha + recurso opcional) en vez de un único `resourceId` (`865c219`)
- [x] 5.5 `StepSummary`: modo `PER_DAY` muestra una lista fecha→recurso (auto resuelto a la plaza concreta vía `useSuggestedSpaces`) (`865c219`)

## 6. Plano del paso de ubicación: cabida, zoom y navegación explorar

- [x] 6.1 El lienzo del plano en el asistente se acota a un ratio fijo para verse entero sin scroll dentro del modal (`9f1acc2`)
- [x] 6.2 `useFloorPlanViewport.zoomAtPoint`: zoom relativo hacia un punto; `FloorPlanSurface` añade zoom con rueda/trackpad (listener nativo no pasivo) y doble clic hacia el cursor (acerca; si ya ampliado, vuelve al encuadre) (`9f1acc2`)
- [x] 6.3 Modo "explorar" (`FloorPlanSurface`, `explore` prop): el lienzo NO se arrastra — arrastrar dibuja un rectángulo (marquee) que hace zoom a esa zona (`zoomToRect`); clic en marcador sigue seleccionando (`de493c1`)
- [x] 6.4 `FloorPlanMinimap`: overview con recuadro del viewport; arrastrarlo panea el lienzo grande (`centerOnPoint`) (`de493c1`)
- [x] 6.5 Escape (fuera de modales) vuelve al encuadre en modo explorar; `emphasizeFree`: los puestos libres laten y el resto se atenúa, activo en el asistente (`de493c1`)
- [x] 6.6 Fix: `FloorPlanMinimap` declaraba un hook (`useCallback`) tras un `return null` condicional → "rendered more hooks than during the previous render", crash al montar el plano del paso de ubicación; se mueven todos los hooks antes de cualquier `return` (`5fbc9c4`)
- [x] 6.7 De paso: complejidad cognitiva en `StepSummary` (`autoDates`/`perDayText` extraídos) y `FloorPlanMarker` (`markerClasses` extraído), `eqeqeq` (`5fbc9c4`)
- [x] 6.8 Botón de pantalla completa nativo (`Fullscreen API`) superpuesto en el lienzo del plano (`726d83c`), sustituido después por maximizado in-app (ver 6.9)
- [x] 6.9 Fix: la Fullscreen API nativa sube el lienzo por encima de los diálogos Radix (el modal de confirmación de asignación quedaba oculto); se sustituye por maximizado in-app (`overlay position:fixed`, z-index por debajo de los diálogos); Escape restaura el tamaño (`5bb2785`)

## 7. UX de modales: Escape no cierra

- [x] 7.1 `Dialog` (Radix): `onEscapeKeyDown` anulado; `Modal`/`ConfirmDialog` (legacy): se retira el cierre por Escape; se cierran con (x)/pie/overlay (`726d83c`)
- [x] 7.2 Test de `Modal` actualizado (`no_close_on_escape`) (`726d83c`)

## 8. Cierre

- [x] 8.1 Build frontend verde y lint 0 en cada commit de la serie (verificado en los mensajes de commit)
- [x] 8.2 Backend: 922 tests verdes (696 UT + 226 IT) tras añadir el endpoint `suggested-space` (`a6cf2c9`)
