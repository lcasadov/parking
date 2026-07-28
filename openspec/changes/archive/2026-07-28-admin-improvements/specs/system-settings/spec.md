## ADDED Requirements

### Requirement: Coordenadas del punto exacto del parking

El sistema SHALL permitir al `ADMIN` fijar, junto a la dirección postal del parking, las
coordenadas (`parkingLat`, `parkingLng`) del punto exacto elegido en un mapa (Mapbox),
persistidas en `system_settings` como `DECIMAL(9,6)` nullable (migración `V30`). Las
coordenadas SHALL guardarse únicamente junto a una dirección no vacía; borrar la dirección
SHALL descartar las coordenadas. El token de Mapbox es público y se inyecta en el frontend
por entorno (`VITE_MAPBOX_TOKEN`), nunca commiteado.

#### Scenario: El admin fija el punto en el mapa

- **GIVEN** un `ADMIN` en la configuración de la dirección del parking
- **WHEN** coloca el marcador y guarda con `parkingAddress`, `parkingLat` y `parkingLng`
- **THEN** el sistema persiste dirección y coordenadas y responde 200 con los tres valores

#### Scenario: Coordenadas fuera de rango

- **WHEN** un `ADMIN` envía `parkingLat` fuera de `[-90, 90]` o `parkingLng` fuera de `[-180, 180]`
- **THEN** el sistema rechaza la petición con 400 de validación y no persiste nada

#### Scenario: Borrar la dirección descarta las coordenadas

- **GIVEN** una dirección con coordenadas configuradas
- **WHEN** el `ADMIN` guarda con `parkingAddress` null o en blanco
- **THEN** el sistema borra dirección y coordenadas (ambas quedan null)

### Requirement: Navegación del empleado a las coordenadas exactas

El sistema SHALL exponer las coordenadas del parking a cualquier empleado autenticado en
`GET /settings/parking-address` (además de la dirección). El botón "Ir al parking" de "Mi
Semana" SHALL navegar a las coordenadas cuando estén presentes (más preciso que geocodificar
el texto); en su defecto, SHALL caer a la dirección postal.

#### Scenario: Navegación con coordenadas

- **GIVEN** un empleado autenticado y un parking con coordenadas configuradas
- **WHEN** consulta `GET /settings/parking-address`
- **THEN** la respuesta incluye `parkingLat`/`parkingLng` y "Ir al parking" navega a ellas

#### Scenario: Fallback a la dirección postal

- **GIVEN** un parking con dirección pero sin coordenadas
- **WHEN** el empleado pulsa "Ir al parking"
- **THEN** la navegación usa la dirección postal
