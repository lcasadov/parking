# Lessons — Orchestrator

## Lesson — 2026-07-03
**Mistake**: Lancé un agente de fix (devops-engineer para #16) trabajando en el working tree principal `C:/proyectos/parking` mientras yo, en la sesión principal, cambiaba de rama en ese mismo árbol para archivar. Colisión: el commit del agente aterrizó en mi rama de archivado. Se resolvió sin pérdida (el agente hizo cherry-pick a su rama + restauró refs), pero fue frágil.
**Rule**: Todo agente que vaya a hacer commits debe trabajar en su PROPIO `git worktree` aislado (`git worktree add /c/tmp/pk-<slug> <branch>`), NUNCA en `C:/proyectos/parking` si la sesión principal u otro agente pueden tocar ese árbol en paralelo. La sesión principal se reserva el working tree principal solo cuando no hay agentes de escritura activos.
**Applies to**: Fase 3 (ejecución paralela) y Fase 4 (bug loop) — cualquier delegación con escritura concurrente.

## Lesson — 2026-07-03
**Mistake**: `openspec archive --sync` de init-auth-local falló porque el header `## REMOVED Requirements` del delta ("Endpoints de autenticacion...") no coincidía carácter a carácter con el del spec principal ("autenticación", con tilde).
**Rule**: El matching de headers REMOVED/MODIFIED en OpenSpec es exacto (incluidas tildes). Antes de archivar, comparar `grep "^### Requirement" openspec/specs/<cap>/spec.md` contra los headers del delta y alinearlos byte a byte.
**Applies to**: Fase 6 (archivado) de cualquier change con REMOVED/MODIFIED Requirements.

## Lesson — 2026-07-03
**Mistake**: N/A (confirmación de práctica correcta).
**Rule**: `git push` plano cuelga en este entorno Windows (prompt interactivo del credential manager). Usar siempre `git -c credential.helper= -c credential.helper='!gh auth git-credential' push`.
**Applies to**: Todo push desde la sesión y desde agentes.

## Lesson — 2026-07-04
**Mistake**: Encadené `gh pr merge` + cleanup + `openspec archive` en un solo comando bash sin verificar el resultado del CI. El backend CI de PR #28 estaba en ROJO y el merge pasó igualmente (el repo `lcasadov/parking` NO tiene branch protection que exija checks), dejando develop roto. Solo lo detecté porque `gh pr checks` imprimió el fail justo antes del merge.
**Rule**: NUNCA mergear en el mismo comando que hace cleanup/archive. Gatear SIEMPRE el merge en CI verde: correr `gh pr checks <n> --watch` y comprobar exit/estado ANTES de `gh pr merge`. Si algún check ≠ pass, entrar en bug loop, no mergear. Como el repo no tiene branch protection, la disciplina del orquestador es la única puerta. (Considerar activar required status checks en develop.)
**Applies to**: Fase 5 (creación/merge de PR) de todo change.

## Lesson — 2026-07-04
**Mistake**: `verification-specialist` y el agente de implementación corrieron `mvn clean verify` en local y pasó, pero el CI (runner Linux) falló: `EmployeeManagementIT` afirmaba sobre `$.content[0]` (primera fila) y otro IT nuevo (`FixedAssignmentManagementIT`) insertó empleados `ittest.fa.*` en la MISMA BD Testcontainers compartida sin limpiarlos. Con distinto orden de ejecución de ITs entre local y CI, la fila ajena se colaba y rompía la aserción. Verde en local ≠ verde en CI cuando los ITs comparten contenedor y las aserciones dependen del orden/estado global.
**Rule**: (1) Los ITs deben aislarse: `@BeforeEach` que trunca en orden FK-safe las tablas de dominio que toca (idealmente en una clase base IT común), y NUNCA asumir que los datos propios son `content[0]` — filtrar/scoping por un identificador propio del test. (2) Un IT que crea entidades de otro módulo (empleados, plazas) DEBE limpiarlas. (3) Instruir explícitamente a impl+verificación que las aserciones de listados paginados sean order-independent y que el estado compartido se resetee por test. Verde local no basta: el gate real es el CI.
**Applies to**: Todo change con ITs Testcontainers sobre la BD compartida (backend).

## Lesson — 2026-07-04
**Mistake**: Pedí a la verificación 3 runs locales consecutivos de la suite completa (`mvn verify` natural + reversealphabetical + alphabetical) con Testcontainers. El 3º colapsó por infraestructura (Docker del host agotado: prelogin de SQL Server fallido, bootstrap de Spring "@SpringBootConfiguration not found", clases que no arrancan) — falsos negativos que NO son del código. Cada `mvn verify` completo tarda ~7 min y arranca contenedores; 3 seguidos saturan el Docker de Windows.
**Rule**: Para probar order-independence de ITs bastan **natural + reversealphabetical en local + el CI** (runner limpio) como señal autoritativa. No exigir un 3er run local (alphabetical); si se hace y colapsa con errores de infra (prelogin/bootstrap/contenedor), es entorno, no regresión — distinguirlo por: (a) ¿los mismos tests pasan en los otros órdenes y en CI?, (b) ¿los fallos son imposibles para el cambio hecho (p.ej. un rename de columna no puede romper el bootstrap de Spring)? Confiar en el CI como gate real.
**Applies to**: Fase QA (verification-specialist) de cualquier change con ITs Testcontainers pesados.

## Lesson — 2026-07-05
**Mistake**: Ordené "TDD estricto (Red→Green→Refactor)" a un `frontend-engineer` para un change puramente **presentacional** (paridad de la UI del plano con mockups: colores pastel desde tokens, símbolo ◆, imagen, breakpoints, quitar font-weight 700). El grueso de ese trabajo es fidelidad **visual**, que RTL/Vitest NO asertan; escribir un "test rojo" primero para "el marcador usa el color pastel del token" no aporta. El usuario lo señaló: "¿no tenía sentido TDD?". Pedir TDD ahí fue inercia, no criterio.
**Rule**: NO apliques TDD por defecto a cambios presentacionales/CSS. Separa: la parte **conductual** (navegación de fecha acotada, contadores de filtros, solicitud móvil éxito/409, banners, isPlaced(null)) SÍ va test-first; la parte **visual** (color, layout, tipografía, símbolos, responsive) se verifica por (a) lint + reglas del design-system (grep de font-weight/hex fuera de tokens), (b) revisión visual en navegador contra el mockup, (c) E2E con viewport. Instruye al agente distinguiendo ambas, y no llames "TDD" a lo que en realidad es componente+test juntos. Además: la paridad visual necesita verificación ocular (extensión Chrome o el usuario); los tests unitarios verdes NO la garantizan.
**Applies to**: Cualquier change de UI presentacional / mockup-parity / CSS.
