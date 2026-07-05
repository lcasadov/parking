# Tasks: refine-floor-plan-mockup-parity

> **Orden test-first (Vitest + RTL → implementación).** Cada componente nuevo del plano se cubre con test antes/junto a su implementación. Sin cambios de contrato ni backend de dominio; solo presentación + seed dev.

## 1. Fin del apilamiento (datos + defaults)
- [ ] 1.1 Seed dev: migración en `db/seed/dev` que actualiza las coordenadas de los 65 puestos a una rejilla determinista (idempotente); no afecta a PRE/PRO.
- [ ] 1.2 `DeskFormModal`: un puesto nuevo nace **sin coordenadas** (no forzar (50,50)); `isPlaced()` trata `null` como "no colocado".
- [ ] 1.3 Test: puestos con coords distintas se renderizan sin superposición; puesto sin coords aparece en "no colocados".

## 2. Imagen y tokens
- [ ] 2.1 Sustituir `floor-plan-placeholder.svg` (azul, fuera de paleta) por imagen/SVG de planta neutra con tokens del design-system.
- [ ] 2.2 Eliminar `font-weight:700` de la sección plano (usar 400/500) y tokenizar hex sueltos (`#d4a017`, `#fff`).

## 3. Marcadores (paleta pastel + EXECUTIVE)
- [ ] 3.1 `FloorPlanMarker`: paleta pastel semántica por estado (texto oscuro, borde fino) desde tokens; tamaño/borde según mockup.
- [ ] 3.2 EXECUTIVE: anillo ámbar (token) + símbolo ◆ en marcador y leyenda.
- [ ] 3.3 Tests: color/clase por estado y distinción EXECUTIVE.

## 4. Barra de fecha
- [ ] 4.1 `FloorPlanDatebar`: día anterior/siguiente, "Hoy", fecha larga, "Ventana de reserva: 14 días"; recarga el plano dentro de hoy..+14d.
- [ ] 4.2 Tests: navegación de fecha recarga y respeta la ventana.

## 5. Filtros por estado
- [ ] 5.1 `FloorPlanFilters`: chips por estado (Libre, Liberado hoy, Mi puesto, Solicitado, Ocupado) con contadores y `cf-dot`.
- [ ] 5.2 Tests: contador correcto y filtrado/resaltado por estado.

## 6. Zoom y viewport
- [ ] 6.1 `FloorPlanZoom` + viewport (`plano-world`) con transform CSS: botones +/−/restablecer.
- [ ] 6.2 Tests: zoom in/out/reset ajusta la escala.

## 7. Panel lateral
- [ ] 7.1 `FloorPlanSidePanel` (escritorio): lista de puestos con búsqueda por número; variante admin "ocupación del día" con pills de estado.
- [ ] 7.2 Tests: búsqueda filtra la lista; variante admin muestra estado por fila.

## 8. Móvil
- [ ] 8.1 Interacción por Pointer Events (ratón + táctil) para arrastre (admin) y pan.
- [ ] 8.2 Zoom por botones (+/−/restablecer) y, donde sea posible, pinch.
- [ ] 8.3 `FloorPlanMobileList` "Disponibles para solicitar" con botón Solicitar por fila.
- [ ] 8.4 Breakpoints `<768px`: reorganización cabecera/plano/lista.
- [ ] 8.5 Tests: solicitud desde la lista móvil (éxito y 409); arrastre táctil persiste posición.

## 9. Solicitud unificada — disponibilidad
- [ ] 9.1 `CreateRequestModal`: consulta `GET /availability?date&resourceType` y muestra banner de disponibilidad por plaza y por puesto (informativo, no bloqueante).
- [ ] 9.2 Tests: banners reflejan la disponibilidad devuelta; envío sigue permitido.

## 10. Refactor + Quality Gate
- [ ] 10.1 Complejidad <15 por componente, sin duplicación, sin `var`/`eval`; `npm run lint && npm test && npm run build` verdes, cobertura ≥80%.
