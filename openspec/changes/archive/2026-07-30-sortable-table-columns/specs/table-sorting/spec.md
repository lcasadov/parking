## ADDED Requirements

### Requirement: Cabecera de columna ordenable reutilizable
El sistema SHALL ofrecer un componente de cabecera de tabla ordenable reutilizable que exprese, para una columna dada, si está inactiva, ordenada ascendente u ordenada descendente, y que permita al usuario cambiar el orden con el teclado o el ratón.

#### Scenario: Estado visual e accesible de la cabecera
- **WHEN** una columna ordenable no es el criterio de orden activo
- **THEN** su cabecera muestra un indicador neutro y expone `aria-sort="none"`

#### Scenario: Columna activa ascendente
- **WHEN** el orden activo es esa columna en dirección ascendente
- **THEN** la cabecera muestra el indicador ascendente (▲) y expone `aria-sort="ascending"`

#### Scenario: Columna activa descendente
- **WHEN** el orden activo es esa columna en dirección descendente
- **THEN** la cabecera muestra el indicador descendente (▼) y expone `aria-sort="descending"`

### Requirement: Ciclo de orden de tres estados
El sistema SHALL alternar el orden de una columna en el ciclo sin-orden → ascendente → descendente → sin-orden al activar repetidamente su cabecera, y al activar otra columna SHALL empezar esa columna en ascendente descartando el orden anterior.

#### Scenario: Primer clic ordena ascendente
- **WHEN** el usuario activa la cabecera de una columna que no está ordenada
- **THEN** la tabla pasa a ordenarse por esa columna en dirección ascendente

#### Scenario: Segundo clic invierte a descendente
- **WHEN** el usuario activa de nuevo la cabecera de la columna ya ordenada ascendente
- **THEN** la tabla pasa a ordenarse por esa columna en dirección descendente

#### Scenario: Tercer clic vuelve al orden por defecto
- **WHEN** el usuario activa de nuevo la cabecera de la columna ordenada descendente
- **THEN** el orden se limpia y la tabla vuelve a su orden por defecto

#### Scenario: Cambiar de columna reinicia a ascendente
- **GIVEN** la tabla está ordenada por una columna A
- **WHEN** el usuario activa la cabecera de otra columna B
- **THEN** la tabla pasa a ordenarse por B ascendente y deja de estar ordenada por A

### Requirement: Traducción del orden al contrato de servidor
Para las tablas paginadas en servidor, el sistema SHALL traducir el estado de orden al parámetro de consulta `sort=campo,dir` (formato Spring `Pageable`) y SHALL volver a la primera página al cambiar el orden; cuando no hay orden activo, no SHALL enviar el parámetro `sort`.

#### Scenario: El orden se envía como parámetro sort
- **WHEN** el usuario ordena una tabla paginada por una columna en dirección descendente
- **THEN** la siguiente consulta incluye `sort=<campo>,desc` y solicita la página 0

#### Scenario: Sin orden no se envía sort
- **WHEN** no hay ninguna columna ordenada
- **THEN** la consulta no incluye el parámetro `sort` y el servidor aplica su orden por defecto

### Requirement: Whitelist de campos ordenables por endpoint
El servidor SHALL aceptar la ordenación únicamente por un conjunto explícito de campos permitidos por cada endpoint, y SHALL ignorar cualquier campo de orden no permitido cayendo a su orden por defecto, sin exponer ordenación por propiedades arbitrarias de la entidad.

#### Scenario: Campo permitido
- **WHEN** el cliente solicita ordenar por un campo incluido en la whitelist del endpoint
- **THEN** el servidor devuelve los resultados ordenados por ese campo en la dirección indicada

#### Scenario: Campo no permitido se ignora
- **WHEN** el cliente solicita ordenar por un campo que no está en la whitelist
- **THEN** el servidor ignora ese criterio y aplica el orden por defecto del endpoint (sin error)
