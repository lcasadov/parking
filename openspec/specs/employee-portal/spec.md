# employee-portal Specification

## Purpose
TBD - created by archiving change redesign-remaining-screens. Update Purpose after archive.
## Requirements
### Requirement: Portal del empleado con identidad ALEATICA
Las pantallas del Portal del Empleado (móvil-first y escritorio) DEBEN (MUST) seguir la identidad
ALEATICA usando los patrones compartidos y las variantes móviles, sin cambiar la lógica.

#### Scenario: Mi semana
- **WHEN** el empleado abre "Mi semana"
- **THEN** cada día se muestra con una tarjeta cuyo estado usa el mapa estado→color
- **AND** la botonera Solicitar/Liberar usa los estilos de botón del design system

#### Scenario: Solicitud unificada
- **WHEN** el empleado solicita plaza y/o puesto
- **THEN** los bloques de recurso, los estados de error/validación y el botón de envío
  siguen los patrones comunes
- **AND** el envío usa los endpoints existentes (una solicitud por recurso)

### Requirement: Plano del empleado consistente con el de admin
El plano del empleado (escritorio y móvil) DEBE (MUST) reutilizar el patrón de plano
(imagen real + marcadores por coordenadas coloreados por estado) del design system.

#### Scenario: Puestos disponibles
- **WHEN** el empleado ve el plano de una fecha
- **THEN** los marcadores libres se distinguen por color y el panel/lista de disponibles
  usa el patrón común

