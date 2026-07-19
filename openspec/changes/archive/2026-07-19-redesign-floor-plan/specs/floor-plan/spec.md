# floor-plan

## ADDED Requirements

### Requirement: Presentación del plano del día
El plano DEBE (MUST) mostrar la planta real con marcadores de puesto coloreados por estado,
usando las coordenadas y datos existentes.

#### Scenario: Marcadores sobre la planta
- **WHEN** se carga el plano de un día
- **THEN** cada puesto se dibuja sobre la imagen real en su coordenada (%)
- **AND** su color refleja el estado (ocupado/libre/liberado/solicitado)

#### Scenario: Ocupación del día
- **WHEN** se carga el plano
- **THEN** el panel lateral muestra los contadores por estado y el listado de puestos
- **AND** los datos provienen de los endpoints existentes
