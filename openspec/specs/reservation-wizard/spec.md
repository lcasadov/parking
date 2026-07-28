# reservation-wizard Specification

## Purpose
TBD - created by archiving change reservation-wizard. Update Purpose after archive.
## Requirements
### Requirement: Apertura del asistente de reserva ADMIN a pantalla completa
El sistema DEBE (MUST) ofrecer al `ADMIN` un asistente de reserva multipaso, abierto como modal a **pantalla completa** desde la CTA "Nueva reserva" del topbar, que permita crear una reserva (`Request` `APPROVED`) en nombre de un empleado para una o varias fechas. El asistente DEBE (MUST) reutilizar `POST /requests/admin` (capability `admin-punctual-assignment`) para la creación real; el `EMPLOYEE` que pulsa la misma CTA DEBE (MUST) abrir en su lugar su propio modal de solicitud (no el asistente).

#### Scenario: El admin abre el asistente desde la CTA
- **GIVEN** un `ADMIN` autenticado
- **WHEN** pulsa "Nueva reserva" en el topbar
- **THEN** se abre el asistente de reserva como modal a pantalla completa, en su primer paso (tipo de recurso)

#### Scenario: El empleado abre su propia solicitud, no el asistente
- **GIVEN** un `EMPLOYEE` autenticado
- **WHEN** pulsa "Nueva reserva" en el topbar
- **THEN** se abre su modal de solicitud propia (no el asistente de reserva del admin)

### Requirement: Flujo guiado de 5 pasos con validación y stepper fijo/clicable
El asistente DEBE (MUST) presentar 5 pasos en orden — tipo de recurso, fechas, empleado, ubicación y resumen — con un stepper que permanece visible mientras solo el cuerpo del paso activo scrollea. El paso siguiente NO DEBE (MUST NOT) ser accesible hasta que el paso actual esté completo (tipo elegido, al menos una fecha, empleado elegido, ubicación completa). Los pasos ya completados DEBEN (MUST) ser clicables en el stepper para saltar directamente a ellos; los pasos futuros o cuyo requisito previo dejó de cumplirse NO DEBEN (MUST NOT) ser clicables.

#### Scenario: No se puede avanzar sin completar el paso
- **GIVEN** el asistente en el paso "Tipo de recurso" sin ninguna opción elegida
- **WHEN** el admin intenta avanzar
- **THEN** el botón "Siguiente" permanece deshabilitado

#### Scenario: Saltar a un paso ya completado desde el stepper
- **GIVEN** el admin en el paso "Resumen" con los pasos anteriores completos
- **WHEN** pulsa el paso "Fechas" en el stepper
- **THEN** el asistente navega directamente a "Fechas", conservando el resto del estado ya introducido

#### Scenario: Invalidar un paso previo bloquea el salto a los pasos que dependían de él
- **GIVEN** el admin con fechas y ubicación ya elegidas
- **WHEN** vuelve al paso "Fechas" y quita todas las fechas seleccionadas
- **THEN** el paso "Ubicación" deja de ser clicable desde el stepper hasta que vuelva a haber al menos una fecha

### Requirement: Riel lateral con resumen en vivo en desktop
En viewports de escritorio, el asistente DEBE (MUST) sustituir el stepper horizontal por un riel lateral vertical que muestre, junto a la etiqueta de cada paso, el **valor ya elegido** en ese paso (tipo de recurso, fechas resueltas, empleado con su categoría, ubicación elegida). El riel DEBE (MUST) permitir saltar a cualquier paso ya alcanzado, igual que el stepper horizontal. En móvil el asistente DEBE (MUST) mostrar el stepper horizontal en su lugar.

#### Scenario: El riel muestra el resumen en vivo de cada paso
- **GIVEN** el admin en desktop con tipo de recurso, fechas y empleado ya elegidos
- **WHEN** se renderiza el riel lateral
- **THEN** cada paso completado muestra, bajo su etiqueta, el valor elegido (p. ej. el nombre del empleado y su categoría)

#### Scenario: El riel se pliega al stepper horizontal en móvil
- **GIVEN** el admin en un viewport móvil
- **WHEN** abre el asistente
- **THEN** se muestra el stepper horizontal (no el riel lateral)

### Requirement: Selección de fechas en tres modos
El paso de fechas DEBE (MUST) ofrecer tres modos conmutables sobre el mismo calendario mensual: un día concreto, un rango continuo (inicio y fin, ambos incluidos) y días sueltos (multiselección dispersa). Las fechas resultantes DEBEN (MUST) mostrarse como una lista retirable, y el paso NO DEBE (MUST NOT) considerarse completo sin al menos una fecha.

#### Scenario: Elegir un rango de fechas
- **GIVEN** el admin en el paso de fechas con el modo "Rango" activo
- **WHEN** pincha un día de inicio y después un día posterior como fin
- **THEN** el asistente resuelve todas las fechas del intervalo (ambos extremos incluidos)

#### Scenario: Elegir días sueltos
- **GIVEN** el admin en el paso de fechas con el modo "Días sueltos" activo
- **WHEN** pincha varios días no consecutivos
- **THEN** cada fecha pinchada se añade a la selección; pinchar una fecha ya elegida la retira

#### Scenario: Cambiar de modo reinicia la selección de ese modo
- **GIVEN** el admin con fechas elegidas en un modo
- **WHEN** cambia a otro modo de fechas
- **THEN** la lista de fechas resueltas refleja únicamente la selección del nuevo modo activo

### Requirement: Selección del empleado destinatario
El paso de empleado DEBE (MUST) ofrecer un buscador por nombre sobre el catálogo de empleados seleccionables, mostrando avatar, nombre y categoría de cada candidato. El paso NO DEBE (MUST NOT) considerarse completo sin un empleado elegido.

#### Scenario: Buscar y elegir un empleado
- **GIVEN** el admin en el paso de empleado
- **WHEN** escribe parte de un nombre en el buscador
- **THEN** la lista se filtra a los empleados cuyo nombre coincide
- **AND** al elegir uno, su avatar y nombre quedan marcados como seleccionados

### Requirement: Ubicación de puesto mediante el plano interactivo
Cuando el tipo de recurso elegido es PUESTO, el paso de ubicación DEBE (MUST) ofrecer el plano interactivo como selector: solo son pulsables los puestos libres en **todas** las fechas elegidas (intersección de disponibilidad); el resto se muestran bloqueados. DEBE (MUST) ofrecerse también una rejilla alternativa con los mismos puestos elegibles.

#### Scenario: Solo los puestos libres en todas las fechas son seleccionables
- **GIVEN** varias fechas elegidas y un puesto libre en unas pero ocupado en otra
- **WHEN** se renderiza el plano del paso de ubicación
- **THEN** ese puesto aparece bloqueado (no seleccionable)
- **AND** solo los puestos libres en TODAS las fechas elegidas son pulsables

#### Scenario: Elegir un puesto en el plano rellena la ubicación
- **GIVEN** el plano del paso de ubicación con puestos elegibles
- **WHEN** el admin pincha un puesto libre
- **THEN** el paso de ubicación queda completo con ese puesto y su etiqueta (número) para el resumen

### Requirement: Ubicación de plaza: automática o manual agrupada por prioridad de categoría
Cuando el tipo de recurso elegido es PLAZA, el paso de ubicación DEBE (MUST) ofrecer una opción "Asignación automática (según categoría)" y una rejilla manual de plazas libres en todas las fechas elegidas. La rejilla manual DEBE (MUST) ordenar las plazas por la misma prioridad de categoría que usa la auto-asignación del backend, dividiéndolas en un apartado "Sugeridas para {categoría} · planta -N" (la planta que la auto-asignación elegiría) y un apartado "Otras plazas". Sin categoría conocida del empleado, la rejilla DEBE (MUST) mostrar todas las plazas ordenadas por número, sin apartado de sugeridas.

#### Scenario: Plazas agrupadas por planta preferente de la categoría
- **GIVEN** un empleado de categoría `DIRECTOR_N1` (planta preferente alta) y plazas elegibles en varias plantas
- **WHEN** se renderiza la rejilla manual de plazas
- **THEN** las plazas de la planta preferente aparecen en el apartado "Sugeridas para Director N1 · planta -N"
- **AND** el resto aparecen en "Otras plazas", ambos apartados ordenados por prioridad

#### Scenario: Auto-asignación deshabilitada sin ninguna plaza libre
- **GIVEN** ninguna plaza libre en alguna de las fechas elegidas
- **WHEN** se renderiza la tarjeta "Asignación automática"
- **THEN** la tarjeta aparece deshabilitada

### Requirement: Vista previa de la plaza sugerida antes de confirmar
Cuando la ubicación elegida es "Asignación automática" (o, en el modo por-día, un día concreto marcado como automático), el paso de resumen DEBE (MUST) resolver y mostrar, para cada fecha correspondiente, la plaza EXACTA que la auto-asignación asignaría, consultando el endpoint de vista previa (`GET /requests/admin/suggested-space`) antes de que el admin confirme. Mientras se resuelve DEBE (MUST) mostrarse un estado de carga por fecha, y si no hay ninguna plaza libre esa fecha DEBE (MUST) indicarlo explícitamente en vez de fallar silenciosamente.

#### Scenario: El resumen muestra la plaza exacta por fecha en auto-asignación
- **GIVEN** el admin con "Asignación automática" elegida para varias fechas, con plaza disponible en todas
- **WHEN** llega al paso de resumen
- **THEN** cada fecha muestra la plaza y planta exactas que se asignarían, antes de confirmar

#### Scenario: Sin plaza libre esa fecha, el resumen lo indica
- **GIVEN** el admin con "Asignación automática" elegida y ninguna plaza libre para una de las fechas
- **WHEN** el resumen resuelve la vista previa de esa fecha
- **THEN** muestra "sin plaza libre esa fecha" para esa fecha en vez de un valor vacío o un error genérico

### Requirement: Recurso distinto por día con varias fechas
Con más de una fecha elegida, el paso de ubicación DEBE (MUST) ofrecer un segmentado "Misma para todos" / "Distinta por día". En "Distinta por día", el sistema DEBE (MUST) presentar una fila por fecha con su elección y permitir editar la elección de la fecha activa (plano o rejilla), filtrada por la disponibilidad de ESA fecha en concreto; DEBE (MUST) ofrecer los atajos "aplicar a todos los días" y, solo para plaza, "auto-asignar en los días sin elegir". Al activar "Distinta por día" sin elecciones previas, el sistema DEBE (MUST) sembrar todos los días con la elección vigente de "Misma para todos", si la hay.

#### Scenario: Activar "distinta por día" siembra con la elección previa
- **GIVEN** el admin con una plaza concreta ya elegida en modo "Misma para todos" y varias fechas
- **WHEN** cambia a "Distinta por día"
- **THEN** todas las fechas quedan sembradas con esa misma plaza, lista para editar día a día

#### Scenario: Editar la elección de un día concreto
- **GIVEN** el modo "Distinta por día" activo con varias fechas
- **WHEN** el admin selecciona una fecha de la tira y elige un recurso distinto en su editor
- **THEN** solo la elección de esa fecha cambia; el resto conservan la suya

#### Scenario: El paso no está completo si falta un día por elegir
- **GIVEN** el modo "Distinta por día" activo con alguna fecha sin elección
- **WHEN** el admin intenta avanzar al resumen
- **THEN** el paso de ubicación permanece incompleto hasta que todas las fechas tengan una elección válida

### Requirement: Confirmación tolerante a fallos parciales por fecha
Al confirmar, el asistente DEBE (MUST) crear una reserva por cada fecha invocando `POST /requests/admin` de forma independiente (sin transacción entre fechas), y DEBE (MUST) mostrar el resultado agregado por fecha (creada, duplicada, sin disponibilidad, o error) en un paso final, en lugar de abortar toda la operación si una fecha falla.

#### Scenario: Éxito total
- **GIVEN** varias fechas elegidas, todas con recurso disponible
- **WHEN** el admin confirma
- **THEN** el paso final muestra todas las fechas como creadas con éxito

#### Scenario: Fallo parcial no aborta las demás fechas
- **GIVEN** varias fechas elegidas donde una ya tiene una reserva `PENDING`/`APPROVED` duplicada para ese empleado
- **WHEN** el admin confirma
- **THEN** las fechas sin conflicto se crean con éxito
- **AND** la fecha en conflicto se reporta como fallida con el motivo, sin impedir la creación de las demás

