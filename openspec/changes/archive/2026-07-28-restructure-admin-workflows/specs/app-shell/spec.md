## ADDED Requirements

### Requirement: Navegación agrupada por tarea del usuario
La aplicación DEBE (MUST) organizar la navegación de cada rol **por tarea** y no por objeto de datos. El menú del `ADMIN` DEBE (MUST) presentar 9 destinos en dos grupos —Operativa (Solicitudes, Ocupación, Plano, Liberar, Visitantes) y Gestión (Empleados, Recursos, Registros, Ajustes)—. El portal del `EMPLOYEE` DEBE (MUST) presentar 4 destinos con **"Mi Semana" como ruta índice**. Ninguna capacidad existente se elimina: las pantallas se agrupan, no se borran.

#### Scenario: El admin ve el menú reagrupado en 9 destinos
- **GIVEN** un `ADMIN` autenticado
- **WHEN** carga la aplicación
- **THEN** el sidebar muestra los grupos Operativa (Solicitudes, Ocupación, Plano, Liberar, Visitantes) y Gestión (Empleados, Recursos, Registros, Ajustes)
- **AND** no existen entradas separadas para "Disponibilidad" ni "Liberar por fecha" (quedan integradas en Ocupación y Liberar)

#### Scenario: El empleado aterriza en "Mi Semana"
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** entra al portal (ruta índice del rol)
- **THEN** la pantalla mostrada es "Mi Semana"
- **AND** el menú presenta 4 destinos (Mi Semana, Plano, Mis solicitudes, Mis plazas)

### Requirement: Fusión de secciones afines en pestañas sin pérdida de páginas
La aplicación DEBE (MUST) presentar como pestañas de un mismo destino las secciones que comparten tarea: "Recursos" (Plazas | Puestos), "Registros" (Auditoría | Accesos) y, en el empleado, "Mis plazas" (Asignaciones fijas | Liberaciones). Cada pestaña DEBE (MUST) conservar la funcionalidad de su pantalla original.

#### Scenario: Recursos agrupa plazas y puestos
- **GIVEN** un `ADMIN` en el destino "Recursos"
- **WHEN** alterna entre las pestañas Plazas y Puestos
- **THEN** cada pestaña ofrece el CRUD completo del recurso correspondiente (el mismo que hoy tienen las pantallas separadas)

#### Scenario: Registros agrupa auditoría y accesos
- **GIVEN** un `ADMIN` en el destino "Registros"
- **WHEN** alterna entre las pestañas Auditoría y Accesos
- **THEN** cada pestaña muestra su tabla filtrable y paginada como hoy
