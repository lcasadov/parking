## Why

Actualmente un `Employee` no tiene ninguna noción de **rango jerárquico** dentro de la organización. El negocio necesita clasificar a cada empleado en una **categoría** (de CEO a Empleado) para poder, más adelante, decidir automáticamente en qué planta se le asigna la plaza de aparcamiento. Sin esta categoría, la futura asignación automática de plaza por categoría→planta no tiene un dato de entrada sobre el que operar.

## What Changes

- Se añade a cada `Employee` una **categoría** obligatoria, modelada como un enum con 7 valores jerárquicos (de mayor a menor): `CEO`, `CONSEJO`, `DIRECTOR_N1`, `DIRECTOR_N2`, `GERENTE`, `MANDO_INTERMEDIO`, `EMPLEADO`.
- **Modelo/BD**: nueva columna `category` en `employees` (`NOT NULL`), con migración que fija `EMPLEADO` como valor por defecto para las filas existentes.
- **Backend**: la categoría se expone en el DTO de empleado (alta, edición y listado) y se valida contra el conjunto cerrado de valores del enum.
- **Frontend**: selector de categoría en el alta/edición de empleado (pestaña **DETALLES**) y presentación de la categoría en el listado, con etiquetas i18n para cada valor.
- Se define una única **etiqueta i18n** por valor de enum para la UI (ej. `CEO` → "CEO", `MANDO_INTERMEDIO` → "Mando intermedio").

Esta categoría es un dato **consumido** por la futura capability `request-auto-assignment` (asignación automática de plaza según categoría→planta). Este change **solo** define y persiste la categoría del empleado; **no** implementa ninguna lógica de asignación por planta.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities
- `employees`: se añade un nuevo requisito de **clasificación por categoría** del empleado (columna, DTO, validación y UI). No se modifica el comportamiento de los requisitos existentes de alta/edición/baja.

## Impact

- **BD**: nueva migración `V20__employee_category.sql` (columna `category` + `CHECK` de dominio + default `EMPLEADO` para filas existentes).
- **Backend**: entidad `Employee` (nuevo campo `category` + enum `EmployeeCategory`), `EmployeeCreate`/`EmployeeUpdate`/response DTO, mapeadores y validación (`@NotNull` sobre el enum). Contrato `docs/openapi.yaml` (schema `Employee`, `EmployeeCreate`, `EmployeeUpdate`).
- **Frontend**: `types/employee.ts` (tipo `EmployeeCategory`), `EmployeeFormModal.tsx` (selector en pestaña DETALLES), listado de empleados (columna/etiqueta), `i18n/locales/es.ts` (bloque `employees.category.*`).
- **Consumidores futuros**: capability `request-auto-assignment` (fuera de alcance de este change) leerá `employee.category`.
