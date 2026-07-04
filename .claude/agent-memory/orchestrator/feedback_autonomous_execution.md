---
name: feedback_autonomous_execution
description: Usuario quiere ejecución autónoma del plan multi-change sin pausas salvo bloqueo real
type: feedback
---

Cuando el usuario da luz verde a un plan multi-change, ejecuta **de forma autónoma y encadenada** todos los changes del plan (uno tras otro) sin volver a pedir aprobación entre ellos.

**Why:** Lo pidió explícitamente ("no pares salvo que necesites alguna solución de mi parte"). Valora el avance continuo sobre el control paso a paso una vez aprobado el plan inicial.

**How to apply:** Por cada change ejecuta el flujo OpenSpec completo (propose → apply → verify → archive), crea **una PR por change** (implementación) + su PR de archive, gatea el merge en **CI verde** (nunca mergear sin `gh pr checks --watch` pass — el repo no tiene branch protection), y arranca el siguiente change desde `develop` actualizado. Solo detente y pregunta cuando: (a) un change está bloqueado por inputs externos (p.ej. D1 SSO necesita datos de ALEATICA), (b) QA reporta bugs que no puedes resolver, o (c) una decisión de diseño con impacto que no puedes inferir del código/spec. Agentes con commits → siempre en git worktree aislado (`/c/tmp/pk-<slug>`).
