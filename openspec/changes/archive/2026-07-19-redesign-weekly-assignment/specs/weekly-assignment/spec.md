# weekly-assignment

## ADDED Requirements

### Requirement: Presentación de la asignación semanal
La vista semanal DEBE (MUST) presentar el estado de cada plaza por día usando el mapa
estado→color del design system, sin cambiar el origen de datos.

#### Scenario: Lectura de estado por color
- **WHEN** se carga una semana
- **THEN** cada celda plaza×día se colorea según su estado (ocupado/liberado/pendiente/solicitud/libre)
- **AND** la columna del día actual queda resaltada

#### Scenario: Navegación de semana
- **WHEN** el usuario cambia de semana o pulsa Hoy
- **THEN** el grid se actualiza con los datos de la semana seleccionada usando los endpoints existentes
