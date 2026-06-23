# Brief para `/opsx:propose refactor-nomenclatura-ingles`

> **Cómo usar este brief**
>
> 1. Asegúrate de tener `GLOSARIO-nomenclatura-ES-EN.md` en el repo
>    (recomendado: `docs/GLOSARIO-nomenclatura-ES-EN.md`).
> 2. Lanza `/opsx:propose refactor-nomenclatura-ingles`.
> 3. Pega el contenido entre `=== INICIO/FIN DEL BRIEF ===`.
> 4. Revisa con MUCHO cuidado el `tasks.md` y el `design.md` antes de
>    aprobar: este change toca código ya aplicado.

---

```
=== INICIO DEL BRIEF ===

# Change: refactor-nomenclatura-ingles

## Tipo de change
**Refactor técnico transversal**, sin cambio de comportamiento. Traduce
toda la nomenclatura técnica del proyecto de español a inglés (tablas,
columnas, entidades JPA, repositorios, servicios, controllers, DTOs,
mappers, valores de enum, rutas de API, operationIds), según el glosario
congelado `docs/GLOSARIO-nomenclatura-ES-EN.md`.

## Principio rector
**El comportamiento NO cambia.** La aplicación hace exactamente lo mismo
antes y después. Solo cambian los nombres. Criterio de éxito: todos los
tests existentes siguen verdes (tras adaptarlos a los nuevos nombres) y
los endpoints responden igual (en sus rutas nuevas en inglés).

## Fuente de verdad
`docs/GLOSARIO-nomenclatura-ES-EN.md` es la ÚNICA referencia de traducción.
NO improvisar traducciones: si un término no está en el glosario, PARAR y
pedir que se añada al glosario antes de continuar.

## Contexto del estado actual
- Ya están aplicados los changes `bootstrap-mvp`, `auth-local`,
  `empleados` con nomenclatura en español.
- Hay migraciones Flyway aplicadas, entidades JPA, repositorios,
  servicios, controllers en español.
- El entorno es de DESARROLLO: la BD es descartable, NO hay datos de
  producción que preservar.

## Estrategia de BD: regenerar, no renombrar
Dado que la BD es descartable (DES) y no hay datos reales:
- **Reescribir las migraciones Flyway existentes** (V1, V2, V3...) con los
  nombres en inglés, en lugar de añadir migraciones de renombrado encima.
- Recrear la BD desde cero aplicando las migraciones reescritas.
- Esto deja un historial Flyway limpio en inglés, sin arrastrar el español.
- **Justificación**: en producción futura nunca habrá existido el español,
  así que el historial debe nacer en inglés.

> ALTERNATIVA (si por algún motivo hay que preservar datos): migraciones
> de renombrado con sp_rename. NO es el caso aquí, pero documentarlo en
> design.md como alternativa considerada.

## Alcance del refactor

### 1. Migraciones Flyway
- Reescribir todas las migraciones existentes con nombres de tabla y
  columna en inglés según glosario.
- Mantener la numeración (V1, V2, V3...).
- Verificar que el schema de Spring Session (tablas SPRING_SESSION) NO se
  traduce: son tablas estándar de Spring, mantienen su nombre oficial.
- audit_log y login_log: revisar glosario (se mantienen esos nombres).

### 2. Entidades JPA
- Renombrar clases: `Empleado` → `Employee`, etc. (ver glosario sección 1).
- Renombrar campos: `nombre` → `firstName`, etc. (glosario sección 3).
- Actualizar anotaciones `@Table(name=...)` y `@Column(name=...)` a los
  nombres de BD en inglés.
- Renombrar archivos .java acordemente.

### 3. Repositorios
- `EmpleadoRepository` → `EmployeeRepository`, etc.
- Renombrar métodos de query derivados (findByNombre → findByFirstName).
- Actualizar @Query JPQL/nativas con los nombres nuevos.

### 4. Servicios
- `EmpleadoService` → `EmployeeService`, etc.
- Renombrar métodos y variables internas.

### 5. Controllers y rutas
- `EmpleadoController` → `EmployeeController`.
- Rutas: `/empleados` → `/employees`, etc. (glosario sección 5).
- operationIds y anotaciones OpenAPI: glosario sección 6.

### 6. DTOs y mappers
- Renombrar DTOs (`EmpleadoCrearReq` → `CreateEmployeeRequest` o el patrón
  que ya use el proyecto, mantener consistencia con el estilo existente).
- Actualizar MapStruct mappers.

### 7. Enums
- `EstadoSolicitud` → `RequestStatus` con valores PENDING/APPROVED/etc.
- `TipoLiberacion` → `ReleaseType` (VOLUNTARY/ADMIN).
- (glosario sección 4).

### 8. Tests
- Adaptar todos los tests a los nombres nuevos.
- Los tests deben seguir verificando lo MISMO (mismo comportamiento),
  solo con nombres traducidos.
- Tras el refactor, la cobertura no debe bajar.

### 9. Configuración
- Revisar application*.yml por si hay referencias a nombres de tabla.
- Revisar logback, OpenAPI config.

## Lo que NO entra en este change
- NO se añade funcionalidad nueva.
- NO se generaliza a recurso (eso es el siguiente change,
  `refactor-recurso-generico`).
- NO se añaden puestos ni plano.
- NO se traducen textos de cara al usuario (van por i18n).
- NO se traduce la documentación de producto (README, PRD) salvo
  data-model.md y openapi.yaml que SÍ se actualizan a inglés.

## Documentos a actualizar en este change
- `docs/data-model.md` → nomenclatura en inglés.
- `docs/openapi.yaml` → schemas, rutas, operationIds en inglés.
- Specs de OpenSpec afectadas (`empleados`, `auth-local`, etc.) →
  actualizar referencias a entidades/endpoints con nombres en inglés.
- NO tocar README/PRD (siguen en español como docs de producto).

## Objetivos verificables
1. `mvn clean install` compila sin errores.
2. La BD se regenera desde cero con Flyway y todas las tablas/columnas
   están en inglés.
3. Todos los tests existentes pasan (adaptados a nombres nuevos).
4. Los endpoints responden en sus rutas nuevas en inglés
   (ej. GET /employees devuelve lo mismo que antes GET /empleados).
5. No queda ninguna referencia en español a nivel de nomenclatura técnica
   (verificable con grep de los términos del glosario).
6. La cobertura de tests no baja respecto a antes del refactor.

## Criterios de aceptación
1. ✅ Todo en inglés según el glosario (sin excepciones no documentadas).
2. ✅ `verification-specialist` da PASS (compila + tests verdes).
3. ✅ `reality-checker` confirma que la app funciona igual: login,
   listar empleados, crear empleado, etc., todo operativo en las rutas
   nuevas.
4. ✅ grep de términos españoles del glosario en /src no devuelve
   nomenclatura técnica (solo, como mucho, comentarios o i18n).
5. ✅ PR aprobado.

## Tareas estimadas (orientación)
1. Congelar y referenciar el glosario en el repo.
2. Reescribir migración V1 (Spring Session — verificar qué se traduce).
3. Reescribir migración V2/V3 (audit, login, índices).
4. Reescribir migraciones de empleados/auth a inglés.
5. Renombrar entidad Employee + campos + anotaciones.
6. Renombrar EmployeeRepository + queries.
7. Renombrar EmployeeService + métodos.
8. Renombrar EmployeeController + rutas + OpenAPI.
9. Renombrar DTOs y mappers de empleado.
10. Renombrar enums (RequestStatus, etc.) y sus usos.
11. Refactor de auth (si tiene nomenclatura española).
12. Adaptar todos los tests a nombres nuevos.
13. Regenerar BD desde cero y verificar Flyway.
14. Actualizar data-model.md a inglés.
15. Actualizar openapi.yaml a inglés.
16. Actualizar specs OpenSpec afectadas.
17. grep de verificación de que no queda español técnico.
18. Verification + reality-check completos.

## Notas para los agentes
- **El glosario manda.** Cualquier nombre que no esté en él se consulta,
  no se inventa.
- **Cero cambios de comportamiento.** Si un test cambia de resultado (no
  solo de nombre), algo está mal: parar y revisar.
- **Commits por capa**: `refactor(db): migraciones en inglés`,
  `refactor(entity): Employee y campos`, `refactor(api): rutas en inglés`.
- **Branch**: `feature/<issue-id>-refactor-nomenclatura-ingles`.
- **PR target**: `develop`.
- Hacer el refactor de forma incremental y compilable: idealmente cada
  commit deja el proyecto compilando, no romperlo todo y arreglarlo al
  final.

=== FIN DEL BRIEF ===
```

---

## Por qué este change va primero

Recordatorio del orden acordado:
```
1. refactor-nomenclatura-ingles   ← este (todo el modelo actual a inglés)
2. refactor-recurso-generico       ← generalización (ya nace en inglés)
3. puestos
4. plano
```

Hacer inglés primero evita generalizar a `Recurso/recurso` en español y
luego tener que traducir `Recurso`→`BookableResource`. Cuando llegues al
refactor de recurso, partes de un código ya en inglés y solo te ocupas de
la generalización.

## Riesgo y mitigación

Este es un refactor de "nombres por todas partes", de los que parecen
fáciles pero tienen muchos puntos de fallo (una query nativa olvidada, un
nombre de columna en un yml, un test con el nombre viejo). Por eso el brief
insiste en:
- Glosario como única fuente (no improvisar).
- Verificación por grep al final.
- Cada commit compilable.
- reality-check que confirme que la app funciona igual.

## Antes de implementarlo

Cuando lances este change y los agentes generen el `tasks.md`, **revísalo
con calma**. Es el tipo de change donde conviene ir commit a commit y
verificar que compila en cada paso, en vez de soltar a los agentes a hacer
todo de golpe. Si quieres, cuando tengas el tasks.md generado me lo
enseñas y lo revisamos juntos antes de que empiecen.
