# ui-screens.md — Catálogo de pantallas

> **Alcance y método.** Catálogo construido **exclusivamente** a partir de los mockups de `docs/mockups/` y de la documentación de `docs/` (README, data-model, openapi, architecture). No se inventan pantallas, campos ni flujos. Lo que el mockup no muestra o la documentación no fija se marca **⚠️ Pendiente de confirmar**.
>
> **Terminología:** se usan los nombres oficiales del README (Panel de Administración, Portal del Empleado, asignación fija, liberación, solicitud, plano, etc.) y los identificadores de código en inglés de la *Nomenclatura del código* (`Request`, `ParkingSpace`, `Desk`, `FixedAssignment`, `Release`…).
>
> **Inventario de fuentes** — Mockups en `docs/mockups/`: `index.html`, `shell.js`, `styles.css`, `01-calendario-semanal.html`, `02-solicitudes-pendientes.html`, `03-empleados-asignacion-fija.html`, `04-modal-edicion-empleado.html`, `05-modal-aprobar-solicitud.html`, `06-modal-rechazar-solicitud.html`, `07-empleado-movil.html`, `Plano admin _ titulares _ estado del d_a.html`, `Escritorio _ plano _ panel lateral.html`, `M_vil _ pinch-zoom _ lista de libres.html`, `Editor de posiciones _ arrastrar marcadores.html`, `Solicitud unificada _ plaza _ puesto.html`, `assets/plano-verificacion.png`. Docs en `docs/`: `README.md`, `PROJECT.md`, `data-model.md`, `architecture.md`, `security-design.md`, `openapi.yaml`, `TESTING-STRATEGY.md`, `SONAR-STANDARDS.md`.
>
> El recorrido entre pantallas se documenta en [`ux-flows.md`](ux-flows.md).

---

## Cobertura de dispositivos

- **Panel de Administración** → **escritorio** (responsive básico en móvil: sidebar colapsable + tablas con scroll horizontal). Pantallas 1-6, 8, 11, 16-19, 21.
- **Portal del Empleado** → **móvil-first** (también escritorio). Pantallas 7, 10, 20 (marcos de teléfono), 9 (plano empleado escritorio) y 12 (solicitud unificada).
- **Autenticación / preferencias** → adaptables a ambos (tarjeta centrada). Pantallas 13, 14, 15, 22, 23, 24.

Detalle de la estrategia responsive (breakpoint `≤768px`, viewport, degradación del shell) en `architecture.md` §10.1. Todos los mockups declaran `viewport`.

## Estructura común (shell de administración)

Presente en todas las pantallas de escritorio del Panel de Administración (`shell.js`).

- **Header**: logo + marca "parking · ALEATICA", título de la página, avatar de usuario.
- **Sidebar** (`shell.js`, unificado): Inicio · Asignación semanal · Solicitudes (badge de pendientes) · Empleados · Plazas · Puestos · Plano · Visitantes · Auditoría · Administración. En móvil colapsa a barra superior con scroll horizontal.

---

## 1. Calendario semanal del administrador

- **Mockup de referencia:** `docs/mockups/01-calendario-semanal.html`
- **Propósito:** vista global, solo para el admin, del estado de todas las plazas día a día durante una semana (README → "Vista calendario semanal completa").
- **Componentes / elementos clave:**
  - Toolbar de semana: navegación anterior/siguiente, etiqueta "Semana 20 · 11 – 15 mayo 2026", botón "Hoy".
  - Acciones: "Filtro avanzado", "Exportar (CSV/XLSX)".
  - Tabla: columna "Plaza" (P-01…P-06) + 5 columnas de día (Lun–Vie). Cada celda muestra un estado.
  - Leyenda: Asignada · Liberada · Pdte. asignar · Solicitud aprobada · Libre.
- **Datos mostrados:** por plaza y día, el titular (`FixedAssignment`), liberaciones (`Release`), solicitudes aprobadas (`Request` `APPROVED`) y huecos libres; cálculo de disponibilidad (`architecture.md` → `AvailabilityService`).
- **Estados:**
  - Celda: `Asignada` (nombre del titular), `Liberada`, `Pdte. asignar`, `Solicitud aprobada` (p. ej. "Sol. Berta K."), `Libre`.
  - ⚠️ Pendiente de confirmar: estados de carga / vacío / error (no aparecen en el mockup).
- **Entradas y salidas de navegación:**
  - Entra desde: sidebar "Asignación semanal".
  - Sale hacia: ⚠️ Pendiente de confirmar si una celda abre detalle/edición (el mockup no muestra interacción de celda).

---

## 2. Bandeja de solicitudes pendientes

- **Mockup de referencia:** `docs/mockups/02-solicitudes-pendientes.html`
- **Propósito:** que el admin revise y resuelva (aprobar/rechazar) las solicitudes, ordenadas por antigüedad (README → "FIFO informativo").
- **Componentes / elementos clave:**
  - Pestañas: "Pendientes (5)", "Aprobadas", "Rechazadas", "Todas".
  - Buscador "Buscar empleado…".
  - Tabla: Empleado (avatar + departamento), Fecha solicitada, Día, Creada (tiempo relativo, p. ej. "hace 2 h"), Acciones ("Aprobar", "Rechazar").
- **Datos mostrados:** solicitudes (`Request`) con empleado, `requestedDate`, día de la semana y `createdAt`; departamento del `Employee`.
- **Estados:**
  - Filtro por estado vía pestañas (Pendientes/Aprobadas/Rechazadas/Todas).
  - ⚠️ Pendiente de confirmar: estados de carga / lista vacía / error.
- **Entradas y salidas de navegación:**
  - Entra desde: sidebar "Solicitudes" (badge con nº de pendientes).
  - Sale hacia: [Modal de aprobación de solicitud](#5-modal-de-aprobación-de-solicitud) (botón "Aprobar") · [Modal de rechazo de solicitud](#6-modal-de-rechazo-de-solicitud) (botón "Rechazar").

---

## 3. Empleados y plaza fija

- **Mockup de referencia:** `docs/mockups/03-empleados-asignacion-fija.html`
- **Propósito:** listar empleados y ver/gestionar su plaza fija y los días asignados (README → "Gestión de empleados" + "Asignación fija").
- **Componentes / elementos clave:**
  - Toolbar: buscador, "Filtro avanzado", "Nuevo empleado".
  - Tabla: Empleado (avatar + email), Departamento, Plaza fija (p. ej. "P-01" o "—"), "Días asignados (todo el año)" como chips L·M·X·J·V (activo/inactivo), icono de edición.
  - Leyenda: "Día con plaza fija asignada (todo el año)" · "Sin plaza fija (debe solicitar)".
- **Datos mostrados:** `Employee` (nombre, email, departamento) y su `FixedAssignment` activa (plaza + `day_of_week`).
- **Estados:** empleado con plaza fija vs sin plaza fija ("—", todos los chips apagados). ⚠️ Pendiente de confirmar: carga / vacío / error.
- **Entradas y salidas de navegación:**
  - Entra desde: sidebar "Empleados".
  - Sale hacia: [Modal de edición de empleado](#4-modal-de-edición-de-empleado) (icono editar / "Nuevo empleado").

---

## 4. Modal de edición de empleado

- **Mockup de referencia:** `docs/mockups/04-modal-edicion-empleado.html`
- **Propósito:** crear/editar un empleado y configurar su asignación fija de plaza por días de la semana.
- **Componentes / elementos clave:**
  - Cabecera "Empleado"; pestañas: **DETALLES** · **PLAZA FIJA** · **HISTÓRICO**.
  - DETALLES: Nombre*, Apellidos*, Email*, Departamento (desplegable).
  - PLAZA FIJA: texto explicativo (asignación indefinida; el empleado puede liberar), Plaza* (desplegable, p. ej. "P-08 · Planta baja, fila norte"), Vigencia ("Indefinida", solo lectura), "Días de la semana con plaza*" (tarjetas LUN–VIE seleccionables), banner informativo de resumen ("…tendrá la plaza P-08 los martes y jueves de forma indefinida").
  - Footer: "CERRAR" · "GUARDAR".
- **Datos mostrados:** `Employee` (nombre, apellidos, email, departamento) y `FixedAssignment` (plaza + días).
- **Estados:** día seleccionado vs no seleccionado (tarjetas). ⚠️ Pendiente de confirmar: validación de errores, estado de guardado.
- **Entradas y salidas de navegación:** se abre desde [Empleados y plaza fija](#3-empleados-y-plaza-fija); "GUARDAR"/"CERRAR" vuelven a ella.
- ⚠️ **Pendiente de confirmar (discrepancia con `/docs`):** el mockup solo recoge Nombre/Apellidos/Email/Departamento, pero el README y `data-model.md` definen además `login`, `mobile_phone`, `license_plate`, `is_corporate`, `enabled`, `active`; tampoco aparece el botón de **reset de contraseña** (README/`security-design.md`). La pestaña **HISTÓRICO** no muestra contenido. Ver [Inconsistencias](#inconsistencias-detectadas).

---

## 5. Modal de aprobación de solicitud

- **Mockup de referencia:** `docs/mockups/05-modal-aprobar-solicitud.html`
- **Propósito:** que el admin apruebe una solicitud asignando una plaza concreta disponible.
- **Componentes / elementos clave:**
  - Cabecera "Solicitud de plaza"; pestañas: DETALLES · HISTÓRICO · COMENTARIOS.
  - Campos (solo lectura): Empleado, Departamento, Día solicitado, Estado (pill "PENDIENTE").
  - Bloque "Asignación": "Plaza disponible*" (desplegable, p. ej. "P-07 · Junto a entrada principal", hint "4 plazas libres ese día"), "Origen de la plaza" (solo lectura, p. ej. "Plaza liberada por Diego Castaño"), "Observaciones para el empleado" (textarea).
  - Banner azul: "Al aprobar, se enviará un email automático a <email>".
  - Footer: "CERRAR" · "RECHAZAR" · "APROBAR".
- **Datos mostrados:** `Request` (empleado, fecha, estado) y lista de plazas disponibles para esa fecha (`AvailabilityService`).
- **Estados:** estado `PENDIENTE` mostrado. ⚠️ Pendiente de confirmar: estado tras aprobar, error si la plaza deja de estar disponible (README → `409 Conflict`).
- **Entradas y salidas de navegación:** se abre desde [Bandeja de solicitudes pendientes](#2-bandeja-de-solicitudes-pendientes). "APROBAR" confirma (envía email); "RECHAZAR" lleva al [Modal de rechazo](#6-modal-de-rechazo-de-solicitud); "CERRAR" vuelve a la bandeja.
- ⚠️ **Pendiente de confirmar (discrepancia con `/docs`):** los campos "Observaciones para el empleado" y "Origen de la plaza" no figuran en el modelo de `Request` de `data-model.md` ni en `RequestApproveRequest` de `openapi.yaml` (que solo define `parkingSpaceId`).

---

## 6. Modal de rechazo de solicitud

- **Mockup de referencia:** `docs/mockups/06-modal-rechazar-solicitud.html`
- **Propósito:** rechazar una solicitud indicando un motivo (obligatorio según README).
- **Componentes / elementos clave:**
  - Cabecera roja "Rechazar solicitud".
  - Banner rojo de confirmación ("Estás a punto de rechazar la solicitud de … para el …").
  - "Motivo del rechazo*" (desplegable, p. ej. "No quedan plazas disponibles para ese día"), hint "Selecciona un motivo o escribe uno personalizado".
  - "Comentario adicional" (textarea opcional).
  - Banner azul: "El empleado recibirá un email con el motivo del rechazo".
  - Footer: "CANCELAR" · "CONFIRMAR RECHAZO".
- **Datos mostrados:** `Request` a rechazar (empleado + fecha) y motivo (`rejectionReason`).
- **Estados:** ⚠️ Pendiente de confirmar: validación del motivo (README exige `rejection_reason` ≥ 5 caracteres; el mockup no muestra el error).
- **Entradas y salidas de navegación:** se abre desde [Bandeja de solicitudes pendientes](#2-bandeja-de-solicitudes-pendientes) o desde [Modal de aprobación](#5-modal-de-aprobación-de-solicitud) ("RECHAZAR"). "CONFIRMAR RECHAZO" cierra y envía email; "CANCELAR" vuelve.
- ⚠️ **Pendiente de confirmar:** el catálogo de motivos predefinidos del desplegable no está definido en `/docs`.

---

## 7. Portal del empleado (móvil): Mi Semana y Solicitar plaza

- **Mockup de referencia:** `docs/mockups/07-empleado-movil.html` (dos marcos de teléfono)
- **Propósito:** que el empleado consulte su semana y solicite/libere plaza desde el móvil (README → "Portal del Empleado", "Mi Semana", "Solicitud unificada", "Liberación voluntaria").
- **Componentes / elementos clave:**
  - **Pantalla "Mi semana":** saludo ("Bienvenido <nombre>"), título "MI SEMANA · 11–15 MAY", tarjetas por día con estado: "Plaza P-12 asignada", "Plaza liberada", "Solicitud pendiente", "Sin plaza" (con botón "Solicitar"). Botones inferiores: "Solicitar plaza", "Liberar mi plaza".
  - **Pantalla "Solicitar plaza":** "Día*" (selector de fecha), banner verde "Hay N plazas potencialmente disponibles ese día", "Motivo (opcional)" (textarea), bloque "RESUMEN" (Empleado, Día, "Estado inicial" pill "PENDIENTE"), botón "Enviar solicitud", "Cancelar".
- **Datos mostrados:** recursos propios del empleado por día (`FixedAssignment`, `Release`, `Request`); disponibilidad para la fecha elegida. **No** muestra nombres de otros empleados (README).
- **Estados:** por día → asignada / liberada / solicitud pendiente / sin plaza. Estado inicial de la nueva solicitud: PENDIENTE.
- **Entradas y salidas de navegación:**
  - "Solicitar plaza" / botón "Solicitar" de un día → pantalla "Solicitar plaza".
  - "Liberar mi plaza" → ⚠️ Pendiente de confirmar (no hay mockup del flujo de liberación voluntaria, solo el botón).
  - En escritorio, el equivalente de solicitud para puesto/plaza es la [Solicitud unificada](#12-solicitud-unificada-plaza-yo-puesto).

---

## 8. Plano de puestos — vista admin (titulares y estado del día)

- **Mockup de referencia:** `docs/mockups/Plano admin _ titulares _ estado del d_a.html` (fondo: `docs/assets/plano-verificacion.png`)
- **Propósito:** que el admin vea el plano interactivo de los 65 puestos con su estado y titular para una fecha (README → "Plano interactivo", "Puestos de oficina"). *(Alcance ampliado.)*
- **Componentes / elementos clave:**
  - Cabecera "Plano de puestos", fecha ("lunes 18 may"), "Hoy", "Gestión de puestos", "Editar posiciones", "Exportar".
  - Plano (imagen de planta) con **marcadores numerados 1–65**; marcador de **Dirección** distinguido (◆).
  - Leyenda de estados: Libre · Liberado hoy · Mi puesto · Solicitado · Ocupado.
  - Panel lateral "Ocupación del día": contadores (Ocupado, Libre, Liberado hoy, Solicitado) y lista de puestos con titular (`Estándar`/`Dirección`, "— Libre —").
  - Sidebar amplía con "Puestos" y "Plano".
- **Datos mostrados:** `Desk` (número, categoría `STANDARD`/`EXECUTIVE`, coordenadas), estado por fecha (asignado/liberado/solicitado/ocupado/libre) y titular.
- **Estados:** por marcador → Libre / Liberado hoy / Mi puesto / Solicitado / Ocupado. ⚠️ Pendiente de confirmar: carga / error.
- **Entradas y salidas de navegación:** entra desde sidebar "Puestos"/"Plano"; "Editar posiciones" → [Editor de posiciones del plano](#11-editor-de-posiciones-del-plano).

---

## 9. Plano de puestos — vista empleado (escritorio, panel de disponibles)

- **Mockup de referencia:** `docs/mockups/Escritorio _ plano _ panel lateral.html`
- **Propósito:** que el empleado vea el plano para una fecha y elija un puesto libre para solicitar (README → "pinchar un puesto libre para solicitarlo directamente").
- **Componentes / elementos clave:**
  - Cabecera "Plano de puestos", fecha ("jueves 21 may"), "Hoy", "Ventana de reserva: 14 días", accesos "Mi semana" y "Solicitud unificada".
  - Plano con marcadores 1–65 y ◆ Dirección; leyenda con contadores (Libre 21, Liberado hoy 6, Mi puesto 1, Solicitado 2, Ocupado 35).
  - Panel lateral "Puestos disponibles": lista de puestos (Estándar / Dirección ◆).
- **Datos mostrados:** estado de los `Desk` para la fecha; puestos disponibles.
- **Estados:** Libre / Liberado hoy / Mi puesto / Solicitado / Ocupado. ⚠️ Pendiente de confirmar: carga / error.
- **Entradas y salidas de navegación:** "Solicitud unificada" → [Solicitud unificada](#12-solicitud-unificada-plaza-yo-puesto); "Mi semana" → vista personal; pinchar un puesto libre → solicitud (README).

---

## 10. Plano de puestos — vista empleado (móvil, pinch-zoom y lista de libres)

- **Mockup de referencia:** `docs/mockups/M_vil _ pinch-zoom _ lista de libres.html`
- **Propósito:** equivalente móvil del plano, con zoom táctil y lista de puestos disponibles para solicitar.
- **Componentes / elementos clave:**
  - Cabecera "Plano de puestos", fecha; plano con marcadores 1–65 y controles de zoom (+ / −, pinch-zoom).
  - Leyenda: Libre · Liberado hoy · Mi puesto · Ocupado.
  - Sección "DISPONIBLES PARA SOLICITAR": lista de puestos (Estándar / Dirección ◆) con botón "Solicitar".
- **Datos mostrados:** estado de los `Desk` por fecha; puestos disponibles.
- **Estados:** Libre / Liberado hoy / Mi puesto / Ocupado. ⚠️ Pendiente de confirmar: carga / error / lista vacía.
- **Entradas y salidas de navegación:** botón "Solicitar" de un puesto → solicitud de ese puesto. ⚠️ Pendiente de confirmar si abre la [Solicitud unificada](#12-solicitud-unificada-plaza-yo-puesto) o un envío directo.

---

## 11. Editor de posiciones del plano

- **Mockup de referencia:** `docs/mockups/Editor de posiciones _ arrastrar marcadores.html`
- **Propósito:** herramienta de admin para posicionar los marcadores de los puestos sobre la imagen del plano (README → "Editor de plano"; coordenadas relativas `coord_x`/`coord_y`).
- **Componentes / elementos clave:**
  - Cabecera "Editor de plano", acciones "Volver al plano", "Descartar", "Guardar todo".
  - Plano con marcadores 1–65 **arrastrables**; ◆ Dirección.
  - Panel de propiedades del puesto seleccionado: categoría (Estándar / Dirección ◆), "coord X (%)", "coord Y (%)" con valores numéricos.
- **Datos mostrados:** `Desk` y sus coordenadas relativas (`coord_x`, `coord_y` en %).
- **Estados:** edición no guardada (acciones "Descartar"/"Guardar todo"). ⚠️ Pendiente de confirmar: confirmación de guardado / error.
- **Entradas y salidas de navegación:** entra desde [Plano de puestos — vista admin](#8-plano-de-puestos--vista-admin-titulares-y-estado-del-día) ("Editar posiciones"); "Volver al plano"/"Guardar todo"/"Descartar" regresan a ella.

---

## 12. Solicitud unificada (plaza y/o puesto)

- **Mockup de referencia:** `docs/mockups/Solicitud unificada _ plaza _ puesto.html`
- **Propósito:** solicitar **plaza de parking y/o puesto de oficina** para una misma fecha desde una sola pantalla (README → "Solicitud unificada"); se generan solicitudes independientes por recurso.
- **Componentes / elementos clave:**
  - Cabecera "Solicitar"; título "Solicitar plaza y/o puesto"; aviso "Se generará una solicitud independiente por cada recurso marcado".
  - "Fecha*" (selector), "Hoy", "Ventana de reserva: 14 días".
  - Bloque "Plaza de parking": "Hay N plazas potencialmente disponibles ese día".
  - Bloque "Puesto de oficina": puesto elegido (nº + categoría), estado ("Ocupado"), enlace "Cambiar en el plano".
  - Botón "Enviar solicitudes".
- **Datos mostrados:** disponibilidad de plaza y de puesto para la fecha; recurso(s) seleccionado(s).
- **Estados:**
  - **Error (mostrado en el mockup):** "El puesto … ya no está libre esta fecha. Elige otro." (el puesto seleccionado dejó de estar disponible).
  - **Validación (mostrada):** "Marca al menos un recurso disponible para continuar".
  - ⚠️ Pendiente de confirmar: estado de éxito tras enviar.
- **Entradas y salidas de navegación:** entra desde [Plano de puestos — vista empleado (escritorio)](#9-plano-de-puestos--vista-empleado-escritorio-panel-de-disponibles) ("Solicitud unificada"); "Cambiar en el plano" → plano de puestos.

---

## 13. Login local (Fase 1)

- **Mockup de referencia:** `docs/mockups/12-login.html` *(creado para cerrar gap)*
- **Propósito:** acceso con usuario y contraseña en Fase 1 (`README` → Autenticación Fase 1; `security-design.md`).
- **Componentes:** tarjeta centrada con cabecera QRIA; campos Usuario y Contraseña (con ojo mostrar/ocultar); botón "Entrar"; aviso de bloqueo tras 5 intentos/15 min; texto "restablecimiento al administrador".
- **Datos mostrados:** credenciales (`login`, contraseña); no expone datos sensibles.
- **Estados:** **error** (banner "Usuario o contraseña incorrectos · te quedan N intentos"); ⚠️ Pendiente de confirmar: estado de cuenta bloqueada y de carga.
- **Navegación:** entrada al sistema. Éxito → Panel/Portal según rol; si `passwordMustChange` → [Cambiar contraseña](#14-cambiar-contraseña).

## 14. Cambiar contraseña

- **Mockup de referencia:** `docs/mockups/13-cambiar-password.html` *(creado)*
- **Propósito:** establecer una nueva contraseña, obligatorio en el primer acceso tras un reset (`security-design.md`).
- **Componentes:** banner "debes cambiar tu contraseña"; campos Contraseña actual, Nueva, Repetir; **checklist de política** (≥10, mayús+minús, dígito, símbolo, distinta de login/email); botón "Guardar y continuar".
- **Estados:** checklist con reglas cumplidas/incumplidas (ok/bad). ⚠️ Pendiente de confirmar: error de repetición no coincidente.
- **Navegación:** se llega desde [Login](#13-login-local-fase-1) (o tras reset). Éxito → destino del rol.

## 15. Reset administrativo de contraseña (modal)

- **Mockup de referencia:** `docs/mockups/15-modal-reset-password.html` *(creado)*
- **Propósito:** que el admin genere una contraseña temporal para un empleado (`README`/`security-design.md`).
- **Componentes:** datos del empleado; aviso 🟢 "se muestra una sola vez"; contraseña temporal monoespaciada con botón "Copiar"; nota `password_must_change`; aviso 🔵 "en Fase 2 se envía por email".
- **Navegación:** se abriría desde la ficha del empleado ([Modal de edición de empleado](#4-modal-de-edición-de-empleado)). ⚠️ Pendiente de confirmar: el mockup 04 aún no incluye el botón que lo dispara.

## 16. Gestión de plazas

- **Mockup de referencia:** `docs/mockups/08-plazas.html` *(creado)*
- **Propósito:** alta, edición, activación/desactivación de plazas y configuración del total (`README` → gestión de plazas).
- **Componentes:** tarjeta "Configuración del parking" (total + "Aplicar"); toolbar (buscador, "Nueva plaza"); tabla Plaza · Ubicación · Titular fijo · Estado (pill Activa/Inactiva) · Acciones; leyenda.
- **Datos mostrados:** `ParkingSpace` (`label`, `active`) + titular (`FixedAssignment`). ⚠️ "Ubicación" no es un campo de `data-model.md` (ver [Inconsistencias](#inconsistencias-detectadas)).
- **Estados:** Activa / Inactiva. ⚠️ Pendiente de confirmar: carga/vacío/error.
- **Navegación:** sidebar "Plazas". "Nueva plaza"/editar → ⚠️ formulario/modal no mockeado aún.

## 17. Visitantes y reservas

- **Mockup de referencia:** `docs/mockups/09-visitantes.html` *(creado)*
- **Propósito:** gestionar fichas reutilizables de visitante y lanzar reservas (`README` → reservas para visitantes).
- **Componentes:** pestañas "Fichas de visitante" / "Reservas futuras"; buscador (DNI/nombre/matrícula); botones "Nueva reserva" / "Nuevo visitante"; tabla Visitante · DNI · Matrícula · Empresa · Motivo habitual · Acciones; aviso "no generan emails".
- **Datos mostrados:** `Visitor` (`first_name`, `last_name`, `national_id`, `license_plate`, `company`, `usual_reason`).
- **Navegación:** sidebar "Visitantes". "Nueva reserva"/icono calendario → [Modal de reserva de visita](#18-modal-de-reserva-de-visita).

## 18. Modal de reserva de visita

- **Mockup de referencia:** `docs/mockups/10-modal-reserva-visita.html` *(creado)*
- **Propósito:** crear una reserva de plaza puntual para un visitante en una fecha (`README`/`data-model.md` → `VisitorReservation`).
- **Componentes:** selector de Visitante; Fecha; Plaza (con hint de libres); Observaciones; banner "la plaza queda ocupada · sin email".
- **Datos mostrados:** `VisitorReservation` (`visitor_id`, `parking_space_id`, `reservation_date`, `notes`).
- **Estados:** ⚠️ Pendiente de confirmar: error si la plaza ya está ocupada (`409`).
- **Navegación:** se abre desde [Visitantes y reservas](#17-visitantes-y-reservas).

## 19. Auditoría y accesos

- **Mockup de referencia:** `docs/mockups/11-auditoria.html` *(creado)*
- **Propósito:** consulta del registro de auditoría funcional y de los intentos de login (`README` → auditoría; `data-model.md` → `audit_log`/`login_log`).
- **Componentes:** pestañas "Auditoría de acciones" / "Accesos (login)"; buscador; "Rango de fechas"; "Exportar"; tabla Fecha · Actor · Acción (pill) · Entidad · Detalle; nota de retención 2 años.
- **Datos mostrados:** `AuditLog` (`occurred_at`, actor, `action`, `entity_type`/`entity_id`, `details`). La pestaña "Accesos" mostraría `LoginLog`.
- **Estados:** ⚠️ Pendiente de confirmar: carga/vacío/error.
- **Navegación:** sidebar "Auditoría". "Exportar" → fichero CSV/XLSX.

## 20. Liberar mi plaza (móvil)

- **Mockup de referencia:** `docs/mockups/14-liberar-plaza-movil.html` *(creado)*
- **Propósito:** liberación voluntaria de la plaza fija propia para un día concreto (`README` → liberación voluntaria).
- **Componentes:** dos marcos de teléfono — (1) lista de días con plaza fija y botón "Liberar" por día + nota "solo días presentes/futuros"; (2) confirmación con resumen (Plaza, Día, Tipo `VOLUNTARIA`) y aviso "quedará disponible para otro empleado".
- **Datos mostrados:** `FixedAssignment` propia y `Release` resultante (`VOLUNTARY`).
- **Estados:** día con plaza fija vs sin plaza. ⚠️ Pendiente de confirmar: éxito/errores.
- **Navegación:** se llega desde [Portal del empleado (móvil): Mi Semana](#7-portal-del-empleado-móvil-mi-semana-y-solicitar-plaza) ("Liberar mi plaza").

## 21. Modal de alta/edición de plaza

- **Mockup de referencia:** `docs/mockups/16-modal-plaza.html` *(creado)*
- **Propósito:** crear o editar una plaza individual (`README` → gestión de plazas).
- **Componentes:** Identificador* (`label`, único), Estado (toggle Activa), Ubicación/descripción, banner "el titular y los días se asignan después desde la ficha del empleado".
- **Datos mostrados:** `ParkingSpace` (`label`, `active`). ⚠️ "Ubicación" no está en `data-model.md` (señalado en el propio mockup y en [Inconsistencias](#inconsistencias-detectadas)).
- **Estados:** Activa/Inactiva (toggle). ⚠️ Pendiente de confirmar: validación de identificador duplicado (`409`).
- **Navegación:** se abre desde [Gestión de plazas](#16-gestión-de-plazas) ("Nueva plaza"/editar).

## 22. Preferencias (idioma y tema)

- **Mockup de referencia:** `docs/mockups/17-preferencias.html` *(creado)*
- **Propósito:** conmutar idioma (ES/EN) y tema (claro/oscuro) desde el menú del avatar (`README` → i18n y modo oscuro).
- **Componentes:** popover de usuario anclado al avatar con datos del usuario, control segmentado **ES/EN**, toggle **Claro/Oscuro**, "Cerrar sesión"; tarjeta explicativa en el contenido.
- **Datos mostrados:** identidad del usuario (`CurrentUser`); preferencia de idioma/tema (estado de UI, persistido en el navegador).
- **Estados:** idioma activo (ES), tema (Claro). La preferencia se recuerda entre sesiones.
- **Navegación:** accesible desde el avatar del header en cualquier pantalla. "Oscuro" → ver [Modo oscuro](#23-modo-oscuro-demo); "Cerrar sesión" → [Login](#13-login-local-fase-1) (Fase 1) o landing (Fase 2).

## 23. Modo oscuro (demo)

- **Mockup de referencia:** `docs/mockups/18-modo-oscuro.html` *(creado)*
- **Propósito:** demostrar el tema oscuro aplicado a una pantalla real (el calendario semanal). Conmutación **manual**, no automática (`README`).
- **Componentes:** mismo shell y calendario que la [pantalla 1](#1-calendario-semanal-del-administrador), con la paleta oscura (`body.theme-dark` en `styles.css`); menú de usuario con el tema "Oscuro" activado.
- **Estados:** representa el estado "tema oscuro"; el resto de pantallas comparten el mismo tema vía `body.theme-dark`.
- **Navegación:** se activa desde [Preferencias](#22-preferencias-idioma-y-tema).

## 24. Sesión expirada (modal)

- **Mockup de referencia:** `docs/mockups/19-modal-sesion-expirada.html` *(creado)*
- **Propósito:** informar de que la sesión ha caducado/invalidado y ofrecer re-autenticación (`README` → "Cualquier 401 posterior → modal 'Sesión expirada'").
- **Componentes:** modal con icono y mensaje; banner que aclara 🟢 Fase 1 (vuelve al login) / 🔵 Fase 2 (redirige a la landing de ALEATICA); botón "Volver a iniciar sesión".
- **Datos mostrados:** ninguno sensible; solo el aviso.
- **Estados:** se dispara ante cualquier `401` tras estar autenticado.
- **Navegación:** "Volver a iniciar sesión" → [Login](#13-login-local-fase-1) (Fase 1) o landing ALEATICA (Fase 2).

---

## Inconsistencias detectadas

Diferencias entre lo que muestran los mockups y lo documentado en `/docs` (o ausencias en uno u otro). Ninguna se ha resuelto "a criterio"; se listan para confirmar.

1. ✅ **Navegación unificada (resuelto).** `shell.js` se actualizó para incluir un sidebar común con Plazas, Puestos, Plano, Visitantes y Auditoría (con `key` para resaltado). Pendiente: alinear los mockups de puestos (que traen su propio sidebar inline) con `shell.js`.
2. ✅ **Modal de edición de empleado completado (resuelto).** El mockup `04` ahora incluye `login`, `mobile_phone`, `license_plate`, `is_corporate`, `active`, `enabled` (toggles), el botón **Resetear contraseña** (abre [pantalla 15](#15-reset-administrativo-de-contraseña-modal)), la sección de PLAZA FIJA (selector de días) y la pestaña HISTÓRICO con contenido.
3. ✅ **Modal de aprobación (resuelto).** "Observaciones para el empleado" → modelado como `approval_note` en `requests` (`data-model.md`) y `RequestApproveRequest` (`openapi.yaml`); viaja en el email de aprobación. "Origen de la plaza" se mantiene como **dato derivado de solo lectura** (no se persiste; se calcula de la liberación que dejó libre la plaza).
4. ✅ **Exportación PDF (resuelto).** Decisión: solo **CSV/XLSX**. Se eliminó el botón "PDF" del calendario; el contrato no cambia.
5. ✅ **Motivos de rechazo (resuelto).** Decisión: **catálogo + texto libre**. Modelado como `rejection_reason_code` (`NO_AVAILABILITY`, `OUTSIDE_POLICY`, `OTHER`) + `rejection_reason` libre (obligatorio si el código es `OTHER`). ⚠️ El conjunto exacto de códigos es un catálogo inicial, a confirmar con negocio.
6. ✅ **"Ubicación" de la plaza (resuelto).** Decisión: las plazas se identifican **solo por número (`label`)**. Se eliminó la ubicación de todos los mockups (`08`, `16`, `04`, `05`, `10`); `data-model.md` no cambia.
7. **Pantallas creadas en esta iteración** (antes faltaban): login, cambiar contraseña, reset (modal), gestión de plazas, visitantes, reserva de visita (modal), auditoría/accesos y liberación voluntaria (móvil). Ver pantallas [13](#13-login-local-fase-1)–[20](#20-liberar-mi-plaza-móvil).
8. ✅ **Idioma, tema y sesión expirada (resuelto).** Creados: [Preferencias](#22-preferencias-idioma-y-tema) (conmutador ES/EN + claro/oscuro), [Modo oscuro](#23-modo-oscuro-demo) (tema `body.theme-dark` en `styles.css`) y [Sesión expirada](#24-sesión-expirada-modal) (re-autenticación Fase 1/Fase 2). El callback SSO en sí no necesita pantalla (es una redirección); el efecto visible para el usuario es el modal de sesión expirada.
9. ✅ **Índice actualizado (resuelto).** `index.html` ahora lista las pantallas de parking, autenticación y puestos (incluidos los 5 mockups de plano que antes no figuraban).
