## 1. Base de datos (migración)

- [ ] 1.1 Crear `backend/src/main/resources/db/migration/V20__employee_category.sql`: `ALTER TABLE dbo.employees ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_employees_category DEFAULT 'EMPLEADO'`
- [ ] 1.2 Añadir `CONSTRAINT CK_employees_category CHECK (category IN ('CEO','CONSEJO','DIRECTOR_N1','DIRECTOR_N2','GERENTE','MANDO_INTERMEDIO','EMPLEADO'))` en la misma migración
- [ ] 1.3 Verificar que las filas existentes quedan con `category = 'EMPLEADO'` tras aplicar la migración (backfill vía default)

## 2. Backend — dominio y persistencia

- [ ] 2.1 (TDD) Escribir test unitario del enum `EmployeeCategory`: 7 valores en el orden jerárquico exacto (CEO … EMPLEADO)
- [ ] 2.2 Crear el enum `EmployeeCategory` con los 7 identificadores en orden mayor→menor
- [ ] 2.3 Añadir el campo `category` a la entidad `Employee` con `@Enumerated(EnumType.STRING)` y `@Column(nullable = false)`
- [ ] 2.4 Actualizar el mapeo/repositorio JPA para leer y escribir `category`

## 3. Backend — API (DTO + validación)

- [ ] 3.1 (TDD) Test de validación: `POST /employees` sin `category` → 400 con `fields.category`
- [ ] 3.2 (TDD) Test de validación: `POST /employees` con `category` fuera del dominio → 400 con `fields.category`
- [ ] 3.3 (TDD) Test de alta: `POST /employees` con `category = DIRECTOR_N1` → 201 y categoría persistida/devuelta
- [ ] 3.4 (TDD) Test de edición: `PUT /employees/{id}` cambiando `category` → 200 y categoría actualizada
- [ ] 3.5 (TDD) Test de listado: `GET /employees` incluye `category` en cada elemento
- [ ] 3.6 Añadir `category` a `EmployeeCreate`, `EmployeeUpdate` y al DTO de respuesta con `@NotNull` sobre el enum
- [ ] 3.7 Actualizar mapeadores DTO↔entidad para incluir `category`
- [ ] 3.8 Actualizar `docs/openapi.yaml`: `category` en schemas `Employee`, `EmployeeCreate`, `EmployeeUpdate` (enum con los 7 valores)
- [ ] 3.9 Ejecutar `mvn clean verify` — 0 failures, cobertura ≥80% líneas / ≥75% branches

## 4. Frontend — tipos e i18n

- [ ] 4.1 Añadir el tipo `EmployeeCategory` (unión de los 7 valores) en `frontend/src/types/employee.ts` y el campo `category` en `Employee`/`EmployeeCreate`/`EmployeeUpdate`
- [ ] 4.2 Añadir el bloque `employees.category.*` en `frontend/src/i18n/locales/es.ts` con la etiqueta de cada valor (CEO, Consejo, Director nivel 1, Director nivel 2, Gerente, Mando intermedio, Empleado)

## 5. Frontend — formulario y listado

- [ ] 5.1 (TDD) Test del `EmployeeFormModal`: el selector de categoría aparece en la pestaña DETALLES y su valor se envía en el body de alta/edición
- [ ] 5.2 Añadir `category` a `FormState`/`initialState` (default `EMPLEADO`) y al `createBody`/`updateBody` en `EmployeeFormModal.tsx`
- [ ] 5.3 Añadir el `<select>` de categoría en `DetailsPanel` (pestaña DETALLES) con las opciones etiquetadas por i18n en orden jerárquico
- [ ] 5.4 (TDD) Test del listado: cada fila muestra la etiqueta de la categoría del empleado
- [ ] 5.5 Mostrar la categoría (columna/etiqueta) en el listado de empleados
- [ ] 5.6 Ejecutar `npm run lint && npm test && npm run build` — 0 errores, cobertura ≥80%

## 6. Verificación

- [ ] 6.1 `openspec validate "employee-category"` sin errores
- [ ] 6.2 Verificación end-to-end: alta de empleado con cada categoría, edición y listado reflejan el valor correcto
