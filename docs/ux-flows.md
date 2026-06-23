# ux-flows.md — Recorridos de usuario (UX flows)

> **Alcance y método.** Flujos construidos **exclusivamente** desde los mockups de `docs/mockups/` y la documentación de `docs/`. No se inventan pasos ni pantallas. Lo que el mockup no muestra o la documentación no fija se marca **⚠️ Pendiente de confirmar**.
>
> Cada paso referencia una pantalla del **catálogo** [`ui-screens.md`](ui-screens.md) (el "qué"); aquí se describe el "cómo se recorren" (no se duplica la descripción de cada pantalla).
>
> **Reglas de negocio citadas** (fuente: `README.md` → "Reglas de negocio" y "Notificaciones por email"): ventana de solicitud hoy…+14 días; máximo una solicitud `PENDING` por empleado y fecha (`409 Conflict`); la aprobación valida disponibilidad (`409` si no disponible); `rejection_reason` obligatorio (≥5 caracteres); emails `AFTER_COMMIT` a admins/empleado.

---

## 1. Empleado solicita una plaza (móvil)

- **Objetivo del usuario:** conseguir una plaza de parking para un día concreto en el que no tiene plaza fija.
- **Disparador:** desde [Portal del empleado (móvil): Mi Semana](ui-screens.md#7-portal-del-empleado-móvil-mi-semana-y-solicitar-plaza), el empleado ve un día "Sin plaza" o pulsa "Solicitar plaza".
- **Camino feliz** (fuente: `07-empleado-movil.html`):
  1. En [Mi Semana](ui-screens.md#7-portal-del-empleado-móvil-mi-semana-y-solicitar-plaza), pulsa "Solicitar plaza" (o "Solicitar" sobre un día sin plaza).
  2. En la pantalla "Solicitar plaza", elige la **Fecha**; el sistema muestra "Hay N plazas potencialmente disponibles ese día".
  3. (Opcional) escribe un "Motivo".
  4. Revisa el bloque "RESUMEN" (empleado, día, estado inicial **PENDIENTE**) y pulsa "Enviar solicitud".
  5. Se crea la solicitud (`Request` en `PENDING`) y se notifica por email a los admins activos (`README` → notificaciones).
- **Puntos de decisión / ramificaciones:**
  - Día con plaza fija ya asignada → no necesita solicitar (la tarjeta muestra "asignada").
  - "Cancelar" aborta sin crear solicitud.
- **Casos límite y errores** (fuente: `README` reglas de negocio; el mockup no los renderiza → ⚠️):
  - Fecha fuera de la ventana de 14 días → ⚠️ Pendiente de confirmar el mensaje (regla documentada, UI no mostrada).
  - Ya existe una solicitud `PENDING` del empleado para esa fecha → `409 Conflict` (⚠️ UI no mostrada).
- **Resultado final:** solicitud en estado `PENDING`, visible para el admin en la [Bandeja de solicitudes pendientes](ui-screens.md#2-bandeja-de-solicitudes-pendientes); email enviado a los admins.

---

## 2. Empleado solicita plaza y/o puesto (solicitud unificada, escritorio)

- **Objetivo del usuario:** pedir, para una misma fecha, plaza de parking y/o puesto de oficina en un solo paso.
- **Disparador:** acceso "Solicitud unificada" desde [Plano de puestos — vista empleado (escritorio)](ui-screens.md#9-plano-de-puestos--vista-empleado-escritorio-panel-de-disponibles).
- **Camino feliz** (fuente: `Solicitud unificada _ plaza _ puesto.html`):
  1. Abre la [Solicitud unificada](ui-screens.md#12-solicitud-unificada-plaza-yo-puesto).
  2. Elige la **Fecha** (dentro de "Ventana de reserva: 14 días").
  3. Marca **Plaza de parking** (ve "Hay N plazas potencialmente disponibles") y/o **Puesto de oficina**.
  4. Pulsa "Enviar solicitudes": se genera **una solicitud independiente por cada recurso marcado** (`README` → solicitud unificada).
- **Puntos de decisión / ramificaciones:**
  - Marca solo plaza, solo puesto, o ambos (solicitudes independientes; el admin las resuelve por separado).
  - "Cambiar en el plano" lleva al [Plano de puestos](ui-screens.md#9-plano-de-puestos--vista-empleado-escritorio-panel-de-disponibles) para elegir otro puesto.
- **Casos límite y errores** (mostrados en el mockup):
  - El puesto elegido deja de estar libre → mensaje "El puesto … ya no está libre esta fecha. Elige otro."
  - Ningún recurso disponible marcado → "Marca al menos un recurso disponible para continuar" (botón de envío bloqueado).
- **Resultado final:** una o dos solicitudes (`Request`) en `PENDING`, una por recurso; emails a admins.

---

## 3. Empleado solicita un puesto desde el plano (móvil)

- **Objetivo del usuario:** reservar un puesto de oficina concreto eligiéndolo visualmente en el plano.
- **Disparador:** abrir el plano de puestos en móvil para una fecha.
- **Camino feliz** (fuente: `M_vil _ pinch-zoom _ lista de libres.html`):
  1. En [Plano de puestos — vista empleado (móvil)](ui-screens.md#10-plano-de-puestos--vista-empleado-móvil-pinch-zoom-y-lista-de-libres), ajusta el zoom (pinch / + / −) y consulta los marcadores por estado.
  2. En "DISPONIBLES PARA SOLICITAR", pulsa "Solicitar" sobre un puesto libre.
  3. Se solicita ese puesto (`README` → "pinchar un puesto libre para solicitarlo directamente").
- **Puntos de decisión / ramificaciones:** elegir Estándar o Dirección (◆) entre los libres.
- **Casos límite y errores:** solo se pueden seleccionar puestos en estado "libre" para la fecha (`README` → RN-DESK-04). ⚠️ Pendiente de confirmar si "Solicitar" abre la [Solicitud unificada](ui-screens.md#12-solicitud-unificada-plaza-yo-puesto) o envía directamente.
- **Resultado final:** solicitud de puesto en `PENDING`. ⚠️ Pendiente de confirmar la confirmación visual de éxito.

---

## 4. Empleado libera su plaza/puesto

- **Objetivo del usuario:** dejar libre su recurso fijo un día que no acudirá, para que otro lo pueda solicitar.
- **Disparador:** botón "Liberar mi plaza" en [Mi Semana](ui-screens.md#7-portal-del-empleado-móvil-mi-semana-y-solicitar-plaza); en el mockup, un día aparece como "Plaza liberada".
- **Camino feliz** (fuente: `07-empleado-movil.html` — parcial):
  1. En [Mi Semana](ui-screens.md#7-portal-del-empleado-móvil-mi-semana-y-solicitar-plaza), pulsa "Liberar mi plaza".
  2. ⚠️ **Pendiente de confirmar:** el flujo de selección de fecha y confirmación de la liberación **no tiene mockup** (solo existe el botón y el estado resultante "Plaza liberada").
- **Puntos de decisión / ramificaciones:** según `README` (liberación voluntaria), solo el titular y solo para fechas presentes o futuras. ⚠️ UI no mostrada.
- **Casos límite y errores:** ⚠️ Pendiente de confirmar (no hay mockup).
- **Resultado final:** el recurso queda como `Release` para esa fecha; pasa a estar disponible para solicitud (no genera email, según `README`).

---

## 5. Admin aprueba una solicitud

- **Objetivo del usuario:** conceder una plaza concreta a una solicitud pendiente.
- **Disparador:** botón "Aprobar" en la [Bandeja de solicitudes pendientes](ui-screens.md#2-bandeja-de-solicitudes-pendientes).
- **Camino feliz** (fuente: `02-solicitudes-pendientes.html`, `05-modal-aprobar-solicitud.html`):
  1. En la [Bandeja de solicitudes pendientes](ui-screens.md#2-bandeja-de-solicitudes-pendientes) (orden FIFO informativo), localiza la solicitud y pulsa "Aprobar".
  2. Se abre el [Modal de aprobación de solicitud](ui-screens.md#5-modal-de-aprobación-de-solicitud) con los datos (empleado, día, estado PENDIENTE).
  3. Selecciona una "Plaza disponible" (hint con nº de plazas libres ese día).
  4. (Opcional) añade "Observaciones para el empleado". *(⚠️ campo no modelado en `/docs` — ver `ui-screens.md` §5.)*
  5. Pulsa "APROBAR": la solicitud pasa a `APPROVED` y se envía email automático al empleado (banner azul del modal; `README` notificaciones).
- **Puntos de decisión / ramificaciones:**
  - Desde el mismo modal puede "RECHAZAR" → [Flujo 6](#6-admin-rechaza-una-solicitud).
  - "CERRAR" cancela sin resolver.
- **Casos límite y errores** (fuente: `README` → aprobación):
  - La plaza elegida no está disponible al confirmar → `409 Conflict` (⚠️ UI del error no mostrada).
  - Concurrencia entre dos admins sobre la misma plaza/fecha → `409` (resuelto en BD; `architecture.md`).
- **Resultado final:** `Request` en `APPROVED` con plaza asignada; empleado notificado por email; la solicitud aparece en la pestaña "Aprobadas".

---

## 6. Admin rechaza una solicitud

- **Objetivo del usuario:** denegar una solicitud indicando el motivo.
- **Disparador:** botón "Rechazar" en la [Bandeja de solicitudes pendientes](ui-screens.md#2-bandeja-de-solicitudes-pendientes) o "RECHAZAR" en el [Modal de aprobación](ui-screens.md#5-modal-de-aprobación-de-solicitud).
- **Camino feliz** (fuente: `06-modal-rechazar-solicitud.html`):
  1. Se abre el [Modal de rechazo de solicitud](ui-screens.md#6-modal-de-rechazo-de-solicitud) con el banner de confirmación (empleado + fecha).
  2. Selecciona/escribe el "Motivo del rechazo" (obligatorio).
  3. (Opcional) añade "Comentario adicional".
  4. Pulsa "CONFIRMAR RECHAZO": la solicitud pasa a `REJECTED` y se envía email con el motivo (banner azul; `README` notificaciones).
- **Puntos de decisión / ramificaciones:** motivo de catálogo o personalizado (⚠️ catálogo no definido en `/docs`); "CANCELAR" aborta.
- **Casos límite y errores:** motivo obligatorio, mínimo 5 caracteres (`README`); ⚠️ la validación no se muestra en el mockup.
- **Resultado final:** `Request` en `REJECTED` con `rejection_reason`; empleado notificado; aparece en la pestaña "Rechazadas".

---

## 7. Admin crea/edita un empleado y su asignación fija

- **Objetivo del usuario:** dar de alta o modificar un empleado y configurar su plaza fija por días de la semana.
- **Disparador:** "Nuevo empleado" o el icono de edición en [Empleados y plaza fija](ui-screens.md#3-empleados-y-plaza-fija).
- **Camino feliz** (fuente: `03-empleados-asignacion-fija.html`, `04-modal-edicion-empleado.html`):
  1. En [Empleados y plaza fija](ui-screens.md#3-empleados-y-plaza-fija) pulsa "Nuevo empleado" o editar.
  2. Se abre el [Modal de edición de empleado](ui-screens.md#4-modal-de-edición-de-empleado), pestaña **DETALLES** (Nombre, Apellidos, Email, Departamento).
  3. En la pestaña **PLAZA FIJA**, elige la "Plaza" y marca los "Días de la semana con plaza" (vigencia "Indefinida"). El banner resume la asignación.
  4. Pulsa "GUARDAR": se persiste el `Employee` y su `FixedAssignment`.
- **Puntos de decisión / ramificaciones:**
  - Empleado sin plaza fija (deberá solicitar) vs con plaza fija (chips de días).
  - Pestaña **HISTÓRICO** → ⚠️ Pendiente de confirmar (sin contenido en el mockup).
- **Casos límite y errores** (fuente: `README` → asignación fija):
  - Una plaza no puede asignarse a dos empleados el mismo día; un empleado no puede tener dos recursos el mismo día → conflicto. ⚠️ UI del error no mostrada.
  - ⚠️ Campos definidos en `/docs` pero ausentes del mockup (`login`, `mobile_phone`, `license_plate`, `is_corporate`, reset de contraseña) — ver `ui-screens.md` §4.
- **Resultado final:** empleado guardado con (o sin) asignación fija; refleja en la tabla y en el calendario.

---

## 8. Admin consulta el calendario semanal

- **Objetivo del usuario:** ver de un vistazo el estado de todas las plazas durante la semana.
- **Disparador:** sidebar "Asignación semanal".
- **Camino feliz** (fuente: `01-calendario-semanal.html`):
  1. Abre el [Calendario semanal del administrador](ui-screens.md#1-calendario-semanal-del-administrador).
  2. Navega entre semanas (anterior/siguiente/"Hoy").
  3. Interpreta cada celda por su estado (Asignada / Liberada / Pdte. asignar / Solicitud aprobada / Libre).
  4. (Opcional) exporta con "Exportar" (CSV/XLSX).
- **Puntos de decisión / ramificaciones:** "Filtro avanzado" → ⚠️ Pendiente de confirmar (criterios no detallados en el mockup).
- **Casos límite y errores:** ⚠️ Pendiente de confirmar (estados de carga/vacío/error no mostrados).
- **Resultado final:** el admin obtiene la foto semanal; opcionalmente un fichero exportado.

---

## 9. Admin consulta el plano de puestos del día

- **Objetivo del usuario:** ver el estado y los titulares de los 65 puestos para una fecha.
- **Disparador:** sidebar "Puestos"/"Plano".
- **Camino feliz** (fuente: `Plano admin _ titulares _ estado del d_a.html`):
  1. Abre el [Plano de puestos — vista admin](ui-screens.md#8-plano-de-puestos--vista-admin-titulares-y-estado-del-día) para una fecha ("Hoy" o la seleccionada).
  2. Lee los marcadores por estado (Libre / Liberado hoy / Mi puesto / Solicitado / Ocupado) y el panel "Ocupación del día" (contadores + titulares).
  3. (Opcional) "Exportar".
- **Puntos de decisión / ramificaciones:** "Editar posiciones" → [Flujo 10](#10-admin-edita-las-posiciones-del-plano).
- **Casos límite y errores:** ⚠️ Pendiente de confirmar (carga/error no mostrados).
- **Resultado final:** el admin conoce ocupación, libres y titulares por puesto para la fecha.

---

## 10. Admin edita las posiciones del plano

- **Objetivo del usuario:** ajustar dónde se dibuja cada puesto sobre la imagen del plano.
- **Disparador:** "Editar posiciones" desde el [Plano de puestos — vista admin](ui-screens.md#8-plano-de-puestos--vista-admin-titulares-y-estado-del-día).
- **Camino feliz** (fuente: `Editor de posiciones _ arrastrar marcadores.html`):
  1. Abre el [Editor de posiciones del plano](ui-screens.md#11-editor-de-posiciones-del-plano).
  2. Arrastra los marcadores 1–65; ajusta o consulta "coord X (%)" / "coord Y (%)".
  3. Pulsa "Guardar todo" para persistir las coordenadas (`coord_x`, `coord_y`).
- **Puntos de decisión / ramificaciones:** "Descartar" desecha cambios; "Volver al plano" regresa a la vista admin.
- **Casos límite y errores:** ⚠️ Pendiente de confirmar (confirmación de guardado / error no mostrados).
- **Resultado final:** las posiciones de los puestos quedan actualizadas y se reflejan en el plano.

---

## Flujos documentados en `/docs` sin mockup (no desarrollados aquí)

Por completitud, estos recorridos están descritos en `/docs` pero **no tienen mockup** que los soporte, por lo que no se detallan como flujo (marcados ⚠️ en [`ui-screens.md` §Inconsistencias](ui-screens.md#inconsistencias-detectadas)):

- **Autenticación**: login local, cambio de contraseña, reset administrativo, callback SSO (Fase 2).
- **Gestión de plazas (CRUD)** y configuración masiva del total.
- **Reservas para visitantes** (alta de ficha y reserva puntual).
- **Consulta de auditoría y logs de login**, y sus exportaciones.

⚠️ Pendiente de confirmar si se diseñarán mockups para estos flujos antes de implementarlos.
