# Delta spec: audit-retention (change bootstrap-mvp)

> Delta infraestructural: crea las tablas de auditoría vía Flyway. El
> registro automático y la consulta los implementa el change funcional
> de `audit-retention`.

## ADDED Requirements

### Requirement: Tablas de auditoría creadas por migración
**El sistema DEBE (MUST) crear las tablas `audit_log` y `login_log` mediante migraciones Flyway durante el arranque, aunque ningún servicio las pueble todavía.**

#### Scenario: Flyway crea las tablas de auditoría vacías
- **GIVEN** una base de datos SQL Server limpia
- **WHEN** el backend arranca y Flyway aplica las migraciones
- **THEN** existen las tablas `audit_log` y `login_log` con su esquema (según `docs/data-model.md`)
- **AND** ambas están vacías (0 filas)
- **AND** existen sus índices por `occurred_at`
