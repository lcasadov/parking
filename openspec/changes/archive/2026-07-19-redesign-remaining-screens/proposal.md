# redesign-remaining-screens

## Why
El prototipo ALEATICA solo diseñó 5 pantallas (login, asignación semanal, plano admin,
solicitudes + shell). El resto del frontend heredará los tokens y el shell del change
`redesign-design-system`, pero su composición (tablas, formularios, fichas, modales,
vistas móviles) no está diseñada y no encajará "fina" sola.

Este change cierra el gap: define un conjunto de **patrones de UI reutilizables** con la
identidad ALEATICA y los aplica a TODAS las pantallas restantes, de modo que la aplicación
entera tenga el mismo aspecto.

Referencia: `docs/aleatica-design-contract.md`, `docs/ui-screens.md` y el prototipo
(`docs/design/prototipo-aleatica.html`).

## What Changes
Se crean patrones compartidos (sobre los tokens del design system) y se aplican a cada
pantalla:

- **Patrón Tabla + Toolbar**: cabecera de página (eyebrow + título serif + descripción),
  toolbar (buscador, filtros, acción primaria), tabla con cabecera en labels uppercase,
  filas con avatar/pills, paginación, y estados carga/vacío/error.
- **Patrón Pestañas de filtro** (Pendientes/Aprobadas/… , Fichas/Reservas, OK/KO).
- **Patrón Pills/Badges** de estado y categoría (activo/inactivo, VOLUNTARY/administrativa,
  STANDARD/EXECUTIVE, tipo de recurso, resultado de login) mapeados al color por estado.
- **Patrón Modal**: shell de diálogo (cabecera, cuerpo con pestañas/campos, footer de
  acciones), banners informativos (azul/verde/rojo), variante destructiva (rechazo).
- **Patrón Campo de formulario**: label uppercase, input/select/textarea, hint, error,
  toggle, tarjetas de día seleccionables, checklist de política de contraseña.
- **Patrón Tarjeta auth** (centrada): login, cambiar contraseña.
- **Patrón móvil (Portal del Empleado)**: cabecera compacta, tarjetas por día, botonera
  inferior, plano con zoom, listas de recursos disponibles.
- **Tema oscuro**: variante de todos los tokens para `theme-dark`, conmutable desde el
  menú de usuario (Preferencias).

## Impact
- Capa de presentación únicamente. NO cambia API, modelos, SQL, rutas de datos ni lógica.
- Requiere `redesign-design-system` (bloqueante). Idealmente después de login/weekly/
  floor-plan/requests, aunque es independiente de ellos.
- Affected specs: `admin-screens`, `employee-portal`, `auth-screens`, `theming`.
- Affected code (a confirmar por el agente antes de implementar): páginas y modales de
  `frontend/src/pages/*` y `frontend/src/components/*`, y la hoja de tema (claro/oscuro).

## Pantallas cubiertas
Admin: Empleados (+modal edición), Plazas (+modal), Puestos (+modal), Visitantes
(+modal reserva), Disponibilidad, Liberación administrativa, Auditoría, Accesos/logs,
Modal aprobar, Modal rechazar, Reset de contraseña, Editor de posiciones del plano,
Plano empleado (escritorio/móvil).
Empleado: Mi semana, Mis solicitudes, Mis asignaciones fijas, Mis liberaciones,
Solicitud unificada, Solicitar/Liberar plaza (móvil).
Auth/prefs: Login (ya en su change), Cambiar contraseña, Preferencias, Modo oscuro,
Sesión expirada.
