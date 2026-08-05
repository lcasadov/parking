# employee-absence-release Specification

## Purpose
TBD - created by archiving change reservas-employee-admin-reassign. Update Purpose after archive.
## Requirements
### Requirement: Liberación por ausencia en lote
El sistema SHALL ofrecer al empleado, desde "Mis sitios fijos", una acción destacada para
liberar sus recursos fijos (plaza y/o puesto) en varios días —días sueltos o un rango— en
una sola operación en lote.

#### Scenario: Liberar un rango de días
- **WHEN** el empleado selecciona un rango de fechas de ausencia y confirma
- **THEN** se liberan sus recursos fijos existentes en cada día laborable del rango
- **AND** se muestra un toast/resumen del resultado

#### Scenario: Días sin recurso fijo se ignoran
- **WHEN** el rango incluye días sin asignación fija (findes u otros)
- **THEN** esos días se omiten sin error y solo se libera lo que existe

### Requirement: Selección de recursos a liberar
El sistema SHALL liberar por defecto tanto la plaza como el puesto del empleado en los días
elegidos, permitiendo mediante un toggle limitar la liberación a un solo tipo de recurso.

#### Scenario: Liberar solo el puesto
- **WHEN** el empleado activa el toggle para liberar solo el puesto
- **THEN** solo se liberan sus puestos fijos en los días elegidos, no las plazas

### Requirement: Resumen previo a confirmar
El sistema SHALL mostrar, antes de aplicar, un resumen de qué recursos y qué días se van a
liberar.

#### Scenario: Resumen antes de confirmar
- **WHEN** el empleado ha elegido días y recursos pero aún no confirma
- **THEN** ve un resumen ("liberarás Puesto 1 y Plaza 2003 los días 4, 5 y 6")

