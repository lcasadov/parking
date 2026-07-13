## Context

La tabla `employees` (definida en `V4__employees.sql`) modela identidad, credenciales, rol (`ADMIN`/`EMPLOYEE`), estado y datos de contacto, pero no captura el **rango jerárquico** del empleado. El negocio quiere clasificar a cada empleado en una de 7 categorías organizativas para, en un change posterior (`request-auto-assignment`), decidir automáticamente la planta de aparcamiento asignable. Este diseño cubre únicamente la introducción del dato `category`; la lógica de asignación queda fuera de alcance.

Restricciones:
- SQL Server (patrón de migraciones Flyway `V<n>__*.sql`, dominio validado con `CHECK` como en `role`/`auth_origin`).
- El contrato de API es autoritativo en `docs/openapi.yaml`; el frontend consume tipos derivados en `types/employee.ts`.
- Existen filas de empleados en entornos ya desplegados: la columna no puede añadirse `NOT NULL` sin default.

## Goals / Non-Goals

**Goals:**
- Añadir una categoría obligatoria (`category`) a cada empleado, con un dominio cerrado de 7 valores jerárquicos.
- Persistir la categoría de forma segura para filas existentes (default `EMPLEADO`).
- Exponer y validar la categoría en el DTO de empleado (alta/edición/listado).
- Permitir seleccionar la categoría en el formulario (pestaña DETALLES) y mostrarla en el listado, con etiquetas i18n.

**Non-Goals:**
- No se implementa ninguna lógica de asignación automática de plaza/planta por categoría (eso es `request-auto-assignment`).
- No se modela ninguna relación categoría→planta ni jerarquía navegable.
- No se cambia el comportamiento de los requisitos existentes de alta/edición/baja/reset de empleados.
- No se añade filtrado ni ordenación por categoría en el listado (posible mejora futura).

## Decisions

**Enum de dominio con identificadores estables.** Se usa un enum de aplicación `EmployeeCategory` con 7 valores. Los identificadores se eligen coherentes y estables (independientes del idioma), separando concepto y etiqueta de UI:

| Orden (mayor→menor) | Identificador enum | Etiqueta i18n (es) |
|---|---|---|
| 1 | `CEO` | CEO |
| 2 | `CONSEJO` | Consejo |
| 3 | `DIRECTOR_N1` | Director nivel 1 |
| 4 | `DIRECTOR_N2` | Director nivel 2 |
| 5 | `GERENTE` | Gerente |
| 6 | `MANDO_INTERMEDIO` | Mando intermedio |
| 7 | `EMPLEADO` | Empleado |

Alternativa considerada: tabla de catálogo `employee_categories` con FK. Se descarta porque el conjunto es cerrado, pequeño y estable, y el orden jerárquico es una propiedad del código (índice del enum), no un dato editable por el usuario. Un `CHECK` en columna es más simple y consistente con `role`/`auth_origin`.

**Persistencia como VARCHAR + CHECK.** `category VARCHAR(20) NOT NULL` con `CONSTRAINT CK_employees_category CHECK (category IN (...))`, alineado con el patrón de `CK_employees_role`. Se mapea en JPA con `@Enumerated(EnumType.STRING)` para evitar acoplar el orden ordinal a la BD.

**Migración en dos pasos lógicos para filas existentes.** (1) `ADD` de la columna con default `EMPLEADO` para poblar filas existentes y satisfacer `NOT NULL`; (2) `CHECK` de dominio. El default a nivel de columna (`DF_employees_category`) se mantiene como red de seguridad, pero el alta vía API siempre envía un valor explícito validado.

**Orden jerárquico expuesto por el orden de declaración del enum.** El orden mayor→menor lo determina la posición en la declaración de `EmployeeCategory`; los consumidores (futuro `request-auto-assignment`) pueden comparar por `ordinal()`. No se persiste un número de rango en BD.

## Risks / Trade-offs

- **[Filas existentes sin categoría real]** → El default `EMPLEADO` puede no reflejar el rango real de empleados ya cargados. Mitigación: es un valor seguro (menor rango) y la edición permite corregirlo; se documenta que es un backfill conservador.
- **[Divergencia enum ↔ CHECK ↔ i18n]** → Añadir/renombrar un valor exige tocar 3 sitios (enum backend, `CHECK` SQL, etiquetas frontend). Mitigación: tests que verifican que todo valor del enum tiene etiqueta i18n y pasa validación; el conjunto es cerrado y estable.
- **[Acoplamiento futuro por ordinal]** → Si `request-auto-assignment` compara por `ordinal()`, reordenar el enum cambiaría la semántica. Mitigación: documentar que el orden de declaración es jerárquico y no debe alterarse sin revisar consumidores.

## Migration Plan

1. Añadir `V20__employee_category.sql`: `ALTER TABLE dbo.employees ADD category VARCHAR(20) NOT NULL CONSTRAINT DF_employees_category DEFAULT 'EMPLEADO';` seguido de `ALTER TABLE ... ADD CONSTRAINT CK_employees_category CHECK (category IN ('CEO','CONSEJO','DIRECTOR_N1','DIRECTOR_N2','GERENTE','MANDO_INTERMEDIO','EMPLEADO'));`.
2. Desplegar backend (entidad + DTO + validación) que ya conoce el enum.
3. Desplegar frontend con el selector y las etiquetas i18n.
4. Rollback: como es aditivo, revertir código; la columna puede permanecer (ignorada) o eliminarse con una migración de reversión si fuese necesario.
