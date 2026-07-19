# Handoff — rediseño ALEATICA con OpenSpec

Este árbol contiene los 6 changes de OpenSpec listos para tu repo `parking`.

## Orden de ejecución (con Claude Code en el repo)
1. redesign-design-system   ← BLOQUEANTE, primero
2. redesign-login
3. redesign-weekly-assignment
4. redesign-floor-plan
5. redesign-requests
6. redesign-remaining-screens ← el resto de pantallas (homogeneiza toda la app)

Para cada uno: primero pide "lístame los archivos a tocar" (sin implementar),
revisa, y luego "implementa las tasks, diff por archivo antes de aplicar".
Al terminar y validar: `openspec archive <change-id>`.

## Reglas fijas (repite en cada prompt)
- Solo presentación; nada de API, modelos, SQL ni lógica de negocio.
- Reutilizar tokens del design system; prohibido hex sueltos.
- Diff por archivo antes de aplicar y marcar tasks en tasks.md.

El contenido visual de referencia está en docs/aleatica-design-contract.md.
