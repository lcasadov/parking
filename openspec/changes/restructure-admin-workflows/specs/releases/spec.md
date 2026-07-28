## MODIFIED Requirements

### Requirement: AGENCIA limitada a la liberación administrativa en el área de releases
**El sistema DEBE (MUST) permitir al rol `AGENCIA`, dentro del área de liberaciones, la liberación administrativa por ambos pivotes (por-empleado y por-fecha), una vista de ocupación de solo lectura para orientarse, y la consulta del historial de las liberaciones administrativas que él mismo ha creado. El rol NO DEBE (MUST NOT) poder crear liberaciones voluntarias, ni listar o cancelar liberaciones del portal de empleado (fail-closed en los endpoints `/releases`, `/releases/mine`, `DELETE /releases/{id}`).**

#### Scenario: AGENCIA libera por el pivote por-fecha
- **GIVEN** un usuario `AGENCIA` autenticado y un recurso ocupado la fecha F
- **WHEN** usa el pivote por-fecha para liberar ese recurso
- **THEN** el sistema aplica la liberación administrativa (o admin-cancel según el origen) y responde con éxito

#### Scenario: AGENCIA no puede crear una liberación voluntaria
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `POST /releases` (liberación voluntaria de recurso propio)
- **THEN** el sistema responde 403
- **AND** no crea ninguna `Release`

#### Scenario: AGENCIA no puede listar liberaciones del portal de empleado
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `GET /releases/mine`
- **THEN** el sistema responde 403
- **AND** no devuelve ninguna liberación

#### Scenario: AGENCIA no puede cancelar una liberación del portal de empleado
- **GIVEN** un usuario `AGENCIA` autenticado
- **WHEN** envía `DELETE /releases/{id}`
- **THEN** el sistema responde 403
- **AND** no modifica ninguna `Release`

## ADDED Requirements

### Requirement: Liberación unificada por-empleado y por-fecha en un solo destino
El sistema DEBE (MUST) presentar la liberación administrativa como un único destino ("Liberar") con dos pivotes de entrada seleccionables —por-empleado (semana del titular) y por-fecha (recursos ocupados de un día)— que operan sobre la misma lógica de liberación de backend. Ambos pivotes DEBEN (MUST) estar disponibles para `ADMIN` y `AGENCIA`.

#### Scenario: Cambiar de pivote sin cambiar de pantalla
- **GIVEN** un `ADMIN` en el destino "Liberar"
- **WHEN** alterna entre el pivote por-empleado y el pivote por-fecha
- **THEN** ambos operan sobre los mismos endpoints de liberación (`/releases/administrative` o `/requests/{id}/admin-cancel` según el origen) sin navegar a otra ruta

#### Scenario: El pivote por-fecha muestra los recursos ocupados del día
- **GIVEN** un `ADMIN` o `AGENCIA` en el pivote por-fecha con una fecha F
- **WHEN** consulta los recursos ocupados de F
- **THEN** el sistema lista cada recurso ocupado con su titular y origen, y ofrece liberarlo
