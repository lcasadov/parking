## Context

El sistema tiene hoy dos roles funcionales (`ADMIN`, `EMPLOYEE`) modelados en el enum `com.aleatica.parking.employee.Role` y respaldados por el CHECK `CK_employees_role` (`docs/data-model.md` §3.1). La autoridad de Spring Security se deriva genéricamente: `AuthController.establishSession` construye `new SimpleGrantedAuthority("ROLE_" + user.role().name())`, y cada endpoint se protege con `@PreAuthorize("hasRole('ADMIN')")` o `hasAnyRole('ADMIN','EMPLOYEE')`. La regla base de `SecurityConfig` es `anyRequest().authenticated()` (fail-closed a nivel de autenticación; la autorización por rol la imponen los `@PreAuthorize`).

El propietario quiere un rol de **privilegio mínimo** para una agencia externa: su única capacidad es la liberación administrativa (`POST /api/v1/releases/administrative`), idéntica a la del ADMIN, sobre plazas de garaje y puestos de oficina de cualquier empleado. No debe poder hacer nada más.

En el frontend, `ProtectedRoute` ya bloquea por rol (`user.role !== requiredRole` → redirige), y `paths.ts` define `type Role = 'ADMIN' | 'EMPLOYEE'` con `homePathForRole`. `AppRoutes` monta dos shells: `AdminLayout` (bajo `/admin`, `requiredRole="ADMIN"`) y `EmployeeLayout` (bajo `/employee`, `requiredRole="EMPLOYEE"`). La pantalla de liberación administrativa ya existe: `AdministrativeReleasesPage`.

## Goals / Non-Goals

**Goals:**
- Introducir el rol `AGENCIA` con la mínima autoridad: solo liberación administrativa.
- Reutilizar el flujo y el contrato existentes del endpoint administrativo sin duplicar lógica.
- Garantizar fail-closed verificable: `AGENCIA` recibe `403` en cualquier otro endpoint admin y no accede al portal de empleado.
- Frontend con shell mínimo de una sola ruta para `AGENCIA`; el guard bloquea el resto.

**Non-Goals:**
- No se crea un endpoint nuevo ni se modifica el contrato de `POST /releases/administrative` (mismos campos, misma validación, mismo `reason` obligatorio).
- No se añade CRUD de usuarios de agencia diferenciado: se crean como empleados con `role = AGENCIA`.
- No se conceden a `AGENCIA` capacidades de lectura de auditoría, disponibilidad, calendario, visitantes ni exportaciones.
- No se toca la mecánica de sesión, bloqueo por intentos fallidos ni cambio de contraseña.

## Decisions

**Decisión 1 — Añadir `AGENCIA` al enum `Role` en lugar de un flag/permiso aparte.**
El authority se emite genéricamente como `ROLE_<name>`, por lo que un nuevo valor de enum propaga automáticamente `ROLE_AGENCIA` a la sesión sin tocar `AuthController`/`AuthService`. Alternativa considerada: un sistema de permisos granular; descartado por sobreingeniería para un único permiso — el propietario decidió "solo liberar y nada más".

**Decisión 2 — Ampliar el `@PreAuthorize` del método concreto a `hasAnyRole('ADMIN','AGENCIA')`, no un rol jerárquico.**
No se usa `RoleHierarchy` (que haría `AGENCIA` heredar/otorgar accesos por inclusión) porque el requisito es de mínimo privilegio: `AGENCIA` no es "un ADMIN reducido" jerárquicamente, es un rol lateral con exactamente una capacidad. Se cambia únicamente la anotación del método `createAdministrativeRelease`. Todos los demás `@PreAuthorize("hasRole('ADMIN')")` quedan intactos, por lo que la exclusión es la posición por defecto (fail-closed): añadir un rol nuevo no concede nada salvo donde se autoriza explícitamente.

**Decisión 3 — Verificación adversarial de exclusión como parte del entregable.**
Dado que es un cambio de RBAC, la prueba de que `AGENCIA` NO accede a lo demás es tan importante como la de que SÍ puede liberar. Se exige una matriz de tests de autorización que ejerza un usuario `AGENCIA` contra endpoints representativos de cada área admin (empleados, plazas, puestos, visitantes, solicitudes, auditoría, login-logs) y del portal de empleado, esperando `403`.

**Decisión 4 — Frontend: shell mínimo reutilizando `AdministrativeReleasesPage`.**
Se añade `AGENCIA` a `type Role`, `homePathForRole` mapea `AGENCIA` a su ruta base, y `AppRoutes` monta un shell propio (una única ruta de liberación administrativa) protegido con `requiredRole="AGENCIA"`. `ProtectedRoute` no cambia de lógica: su comparación estricta `user.role !== requiredRole` ya bloquea a `AGENCIA` fuera de `/admin` y `/employee`, y bloquea a `ADMIN`/`EMPLOYEE` fuera del shell de agencia. Alternativa considerada: reusar el `AdminLayout` con navegación recortada; descartado porque filtrar ítems de menú es fail-open (una ruta directa seguiría montando la página) — un shell separado mantiene el bloqueo por ruta.

## Risks / Trade-offs

- **[Añadir un valor al enum sin actualizar el CHECK `CK_employees_role` rompería el INSERT de usuarios de agencia]** → La migración que amplía el CHECK a `AGENCIA` debe desplegarse antes de crear usuarios con ese rol.
- **[Un nuevo endpoint admin futuro podría olvidarse de excluir a `AGENCIA`]** → El patrón por defecto es fail-closed: `hasRole('ADMIN')` ya excluye a `AGENCIA`. El riesgo real sería usar por error `hasAnyRole(...,'AGENCIA')` o `authenticated()`; la matriz de tests de exclusión actúa como red de seguridad.
- **[Frontend fail-open si se reutiliza un layout compartido]** → Mitigado con shell separado y `requiredRole="AGENCIA"`; se añaden tests RBAC que verifican redirección de `AGENCIA` fuera de rutas admin/employee y de `ADMIN`/`EMPLOYEE` fuera de la ruta de agencia.
- **[Regla de negocio: `AGENCIA` puede liberar cualquier recurso de cualquier empleado]** → Es el comportamiento deseado por el propietario; idéntico alcance de datos que el ADMIN en este endpoint. No se introduce filtrado por subconjunto de empleados.

## Migration Plan

1. Backend: ampliar el CHECK `CK_employees_role` para admitir `AGENCIA` (migración de esquema) antes que el código que lo use.
2. Añadir `AGENCIA` al enum `Role`.
3. Cambiar el `@PreAuthorize` de `createAdministrativeRelease` a `hasAnyRole('ADMIN','AGENCIA')`.
4. Tests de autorización (TDD): AGENCIA puede liberar (2xx) y AGENCIA recibe 403 en el resto.
5. Frontend: `type Role`, `homePathForRole`, shell/ruta mínima, tests RBAC.
6. Rollback: revertir el `@PreAuthorize` a `hasRole('ADMIN')` y el shell frontend deja de recibir usuarios (no habría usuarios `AGENCIA` activos si se revierte la creación). El valor del enum/CHECK puede permanecer sin efecto de seguridad al no autorizarse en ningún endpoint.

## Open Questions

- Ninguna pendiente respecto al alcance funcional. La creación operativa de usuarios de agencia (quién y cómo los da de alta) se realiza con el CRUD de empleados existente asignando `role = AGENCIA`; no requiere UI nueva en este change.
