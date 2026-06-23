# Design: init-employees

## Context
`employees` es el maestro de personas de parking. Mantiene credenciales
locales (Fase 1 / fallback) y el rol autoritativo (`Employee.role`), del que
depende toda la autorización. La gestión es exclusiva de `ADMIN`. La
arquitectura es hexagonal: el dominio (casos de uso de alta/edición/baja/
reset) no depende de Spring; la unicidad se garantiza tanto en dominio como
por índice único en BD.

## Goals
- CRUD completo de empleados con unicidad fuerte de `login`/`email`.
- Baja reversible (lógica) sin pérdida de histórico ni de integridad referencial.
- Reset administrativo coherente con la política de contraseña y con las dos fases.

## Decisions
- **Baja lógica, no física**: `active = false`. *Por qué:* el `Employee` es
  titular/actor de `fixed_assignments`, `requests`, `releases`, `login_log`
  y `audit_log`; borrar la fila rompería el histórico y la auditoría.
- **Unicidad en dos capas**: índices `UX_employees_login` / `UX_employees_email`
  (ver `docs/data-model.md`) + comprobación previa en el caso de uso, traduciendo
  la violación a 409 con `fields`. *Por qué:* el índice es la garantía dura
  frente a concurrencia; la comprobación previa da un mensaje de error claro.
- **Reset según fase**: 🟢 Fase 1 devuelve la contraseña temporal en la
  respuesta (mostrar una sola vez); 🔵 Fase 2 la envía por email y no la
  devuelve. En ambos casos `password_must_change = true`. *Por qué:* en Fase 1
  no hay canal de email garantizado; en Fase 2 no se expone secreto en la API.
- **Reset reutiliza `PasswordPolicy` de `auth-local`** para generar y validar
  la contraseña temporal. *Por qué:* una sola fuente de verdad de la política.
- **`enabled` y `active` independientes**: el reset no altera ninguno.
  *Por qué:* separar "puede iniciar sesión" de "está dado de baja" evita
  reactivaciones accidentales.

## Risks
- **Colisión de unicidad bajo concurrencia** → mitigada por el índice único
  (la segunda transacción recibe violación → 409).
- **Exposición de la contraseña temporal (Fase 1)** → mitigada mostrándola una
  sola vez y forzando el cambio en el primer acceso (`password_must_change`).
- **Baja de un `ADMIN` dejando el sistema sin administradores** → riesgo
  operativo; se mitiga a nivel de proceso (no se modela bloqueo aquí)
  _[verificar con README.md]_.

## Migration Plan
- Flyway: la tabla `employees` y sus índices únicos pertenecen a
  `V1__initial_schema.sql` (ver `docs/data-model.md` §3.1).
- Sin migración de datos específica de este change (capability del esquema inicial).
