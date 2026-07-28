## Why

Tras el rediseño del portal de empleado, el panel de ADMIN acumula varios ajustes
de usabilidad, arreglos y pequeñas mejoras funcionales detectados en uso real. Se
agrupan en un único change incremental para poder priorizarlos y ejecutarlos de
forma ordenada sobre una misma rama de integración.

## What Changes

<!-- Lista viva: cada punto que el usuario indique se añade aquí y como tarea en
     tasks.md. Se irá detallando conforme se concreten los ítems. -->

1. **Fuente de los numerales KPI.** Los números grandes del panel (Ocupación, KPIs,
   etc.) usan Geist Mono (`--font-mono`), que pinta los **ceros tachados** y desentona
   con el resto (Geist). Unificar a la fuente Geist manteniendo la alineación tabular,
   y revisar todos los usos de `--font-mono` para distinguir numerales (cambiar) de
   monospace real (IDs/código, mantener).
2. **Traducir "Actividad reciente".** Las entradas muestran el enum crudo en inglés
   (`humanizeAction`: "Voluntary release", "Cancel release", …) y el tipo de entidad
   sin traducir (chip "RELEASE"). Mapear acciones y tipos de entidad de auditoría a
   textos en español vía i18n.
3. **Reserva admin de plaza y puesto a la vez.** El alta admin de "Nueva reserva"
   pasa a permitir ambos recursos en un mismo alta, con la ubicación resuelta por un
   stepper dinámico (paso de plaza y paso de puesto, solo los seleccionados). Backend:
   un `Request` APPROVED por recurso con validación de disponibilidad independiente.
4. **Filtro por tipo de recurso en la bandeja de solicitudes del admin.** Selector
   Plaza / Puesto / Todos en `PendingRequestsPage`, combinado con pestañas y búsqueda.
5. **Icono de puesto unificado (sillón → pantalla) en una única fuente.** Centralizar
   el nombre del icono junto a `ResourceIcon` y sustituir los ~8 literales `armchair`
   sueltos, para que el icono se cambie desde un único punto.
6. **Rediseño de la pantalla de Plano.** Minimapa debajo del mapa (sin solaparse),
   contadores más grandes a la derecha del mapa, listado de puestos a todo el ancho
   debajo del mapa, y clic en un puesto abre un modal para asignar empleado o visitante
   para el día seleccionado.
7. **Pantalla "Liberar": ancho, fechas y liberación en lote.** Ancho completo en "Por
   empleado", autoscroll al motivo al liberar sin motivo, fecha más estrecha pero más
   legible en "Por fecha", y selección múltiple + liberar seleccionadas con confirmación.
8. **Rediseño de los modales de Ocupación (Asignar / Liberar).** Layout más cuidado y,
   en Asignar, elección entre empleado o visitante (mismo modal que el clic en el Plano).
9. **Quitar la acción "Ver" en Visitantes.** Con Editar y Reservar basta.
10. **Acciones de Empleados en un menú de 3 puntos.** "Dar de baja" y "Restablecer
    contraseña" pasan a un menú kebab para que la fila quede en una sola línea.
11. **Inputs de texto más visibles en toda la app.** Reforzar el borde de `.field-input`
    (hoy 0.5px, casi imperceptible) para que los campos se distingan sobre blanco.
12. **Rediseño del editor de asignación fija (Plaza / Puesto).** Quitar el selector de
    vigencia (siempre indefinida) y sustituir el flujo confuso por una lista de días con
    un selector de recurso por día (+ "sin recurso"); en puesto, icono para abrir el plano
    con la animación que marca el sitio.
13. **Mejorar el tab "Histórico" del modal de empleado.** Rediseñar el resumen de solo
    lectura (asignaciones vigentes + metadatos) con mejor jerarquía y alineación.
14. **Confirmación al dar de baja un empleado.** El botón de baja pide confirmación antes
    de desactivar.
15. **Recursos: nombres completos y limpieza.** Selectores "Plazas de parking" / "Puestos
    de trabajo" y quitar la card "Configuración del parking".
16. **Desactivar recurso comprueba asignaciones futuras.** Si la plaza/puesto está asignado
    (empleado/visitante) hoy o en el futuro, avisar en vez de desactivar sin más.
17. **Rediseño de Configuración.** Ancho completo, cards Manual/Automático seleccionables
    con check (quitar el dropdown), separación entre cards, y toggle custom + Guardar propio
    para "reservas en fin de semana" (sin autoguardado).
18. **Dirección del parking con mapa Mapbox.** Elegir el punto exacto en un mapa y guardar
    coordenadas para el botón "Ir al parking".

## Capabilities

### New Capabilities
<!-- Se añadirán si algún ítem introduce una capacidad nueva. -->

### Modified Capabilities
<!-- Se añadirán las capacidades existentes cuyos requisitos cambien (delta spec). -->

## Impact

- **Frontend:** vistas y modales del panel ADMIN (Ocupación, Solicitudes, Recursos,
  Registros, Ajustes, Visitantes…), según los ítems concretos.
- **Backend:** endpoints/servicios afectados por cada ítem (a detallar).
- **Docs/OpenSpec:** `docs/openapi.yaml` y specs delta de las capacidades tocadas.
