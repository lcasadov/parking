## MODIFIED Requirements

### Requirement: Elección de un puesto concreto en la solicitud unificada
En la **reserva rápida del empleado**, el sistema NO DEBE (MUST NOT) exigir ni ofrecer la elección de un puesto en el plano: al marcar "Puesto", la solicitud se envía **sin `resourceId`** y el backend auto-asigna por categoría (capability `desk-auto-assignment`). El plano como selector de puesto se mantiene únicamente en el asistente del ADMIN/visitante, no en la reserva rápida.

#### Scenario: La reserva rápida no muestra el selector de puesto en el plano
- **GIVEN** un empleado en el modal "Nueva reserva" con "Puesto" marcado
- **WHEN** revisa las opciones del formulario
- **THEN** no aparece ningún selector de puesto sobre el plano
- **AND** al enviar, la solicitud de puesto viaja sin `resourceId`

#### Scenario: El modal de reserva rápida es fecha + recurso
- **GIVEN** un empleado que abre "Nueva reserva"
- **WHEN** ve el formulario
- **THEN** solo elige fecha y tipo(s) de recurso (plaza y/o puesto)
- **AND** el modal se muestra a pantalla completa y el "atrás" del móvil lo cierra sin cambiar de ruta
