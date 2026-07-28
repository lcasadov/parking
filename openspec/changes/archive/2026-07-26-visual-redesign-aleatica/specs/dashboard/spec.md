## ADDED Requirements

### Requirement: Dashboard como ruta índice del ADMIN
El sistema DEBE (MUST) presentar un "Dashboard" (panel de inicio) como ruta índice del rol `ADMIN`, sustituyendo a "Empleados" como destino por defecto al entrar en el área de administración. El Dashboard NO DEBE (MUST NOT) introducir endpoints de backend nuevos: DEBE (MUST) calcular su contenido en el cliente combinando las respuestas de endpoints ya existentes (`/occupancy`, `/availability`, `/requests`, `/employees`, `/audit`).

#### Scenario: El admin aterriza en el Dashboard
- **GIVEN** un `ADMIN` autenticado
- **WHEN** entra al área de administración (ruta índice)
- **THEN** la pantalla mostrada es el Dashboard, no Empleados

#### Scenario: El Dashboard no introduce endpoints nuevos
- **WHEN** el Dashboard carga sus datos
- **THEN** solo invoca endpoints ya consumidos por otras pantallas del admin (`/occupancy`, `/availability`, `/requests`, `/employees`, `/audit`)

### Requirement: Saludo, fecha y KPIs del día
El Dashboard DEBE (MUST) mostrar un saludo contextual según la hora local (mañana/tarde/noche) con el nombre del usuario, la fecha larga localizada, y una fila de KPIs: porcentaje de ocupación del día (con subtítulo ocupados/total), solicitudes pendientes (con subtítulo de nuevas hoy), plazas libres y puestos libres — cada uno con su subtítulo de total del recurso.

#### Scenario: Saludo según la hora
- **GIVEN** la hora local del navegador está entre las 00:00 y las 11:59
- **WHEN** se renderiza el Dashboard
- **THEN** el saludo mostrado corresponde a "mañana" (o equivalente localizado), seguido del nombre de pila del usuario si está disponible

#### Scenario: KPI de ocupación del día
- **GIVEN** existen recursos ocupados y disponibles para la fecha de hoy
- **WHEN** se renderiza la fila de KPIs
- **THEN** el KPI de ocupación muestra el porcentaje `ocupados/total` redondeado, con el detalle `ocupados de total` en el subtítulo

#### Scenario: KPIs de plazas y puestos libres
- **GIVEN** la disponibilidad del día para plaza y para puesto
- **WHEN** se renderiza la fila de KPIs
- **THEN** se muestran dos tarjetas separadas (plazas libres, puestos libres) cada una con el total del recurso correspondiente en el subtítulo

### Requirement: Panel de solicitudes pendientes accionable
El Dashboard DEBE (MUST) mostrar un panel con una vista previa de las solicitudes pendientes (primeras N, con enlace "ver todas" a la pantalla de Solicitudes) permitiendo **aprobar in situ** cada una sin salir del Dashboard.

#### Scenario: Aprobar una solicitud desde el Dashboard
- **GIVEN** el panel de pendientes del Dashboard con al menos una solicitud
- **WHEN** el `ADMIN` pulsa "Aprobar" en una fila
- **THEN** se abre el modal de aprobación existente (`ApproveRequestModal`) para esa solicitud
- **AND** al confirmar, la solicitud se aprueba usando la mutación ya existente (sin lógica nueva de backend)

#### Scenario: Panel de pendientes vacío
- **GIVEN** no hay solicitudes pendientes
- **WHEN** se renderiza el panel
- **THEN** se muestra el mensaje de estado vacío del panel

### Requirement: Panel de actividad reciente
El Dashboard DEBE (MUST) mostrar un panel de actividad reciente con las últimas entradas de auditoría (acción humanizada, actor y fecha/hora), sin acciones de mutación sobre ellas.

#### Scenario: Actividad reciente listada
- **GIVEN** existen entradas de auditoría recientes
- **WHEN** se renderiza el panel de actividad
- **THEN** cada entrada muestra la acción en texto legible (no el enum técnico crudo), el actor (o "sistema" si no hay actor) y la fecha/hora formateada
