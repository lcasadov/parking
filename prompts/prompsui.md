Genera dos documentos Markdown a partir de los mockups en /docs/mockups y
la documentación existente en /docs. No inventes pantallas ni flujos: básate
únicamente en lo que aparezca en los mockups y en los .md de /docs. Si algo
es ambiguo o falta información, márcalo explícitamente con "⚠️ Pendiente de
confirmar" en lugar de rellenarlo a tu criterio.

PASO 1 — Inventario
- Lista todos los archivos de /docs/mockups (imágenes, .fig, .pdf, lo que haya)
  y todos los .md de /docs.
- Lee la documentación de /docs para entender el producto, la terminología
  y los nombres oficiales de cada pantalla. Usa esos nombres de forma
  consistente en ambos documentos.

PASO 2 — Genera /docs/ui-screens.md
Un catálogo de pantallas. Para CADA pantalla incluye:
- ## Nombre de la pantalla (el nombre oficial según /docs)
- **Mockup de referencia:** ruta del archivo en /docs/mockups
- **Propósito:** 1-2 frases sobre qué resuelve esta pantalla
- **Componentes / elementos clave:** lista de los bloques de UI visibles
  (header, formularios, tablas, botones de acción, estados vacíos, etc.)
- **Datos mostrados:** qué información se presenta y de dónde vendría
- **Estados:** loading, vacío, error, éxito... los que apliquen
- **Entradas y salidas de navegación:** desde dónde se llega y hacia dónde
  se puede ir (enlaza con los nombres de otras pantallas)

PASO 3 — Genera /docs/ux-flows.md
Las rutas/recorridos del usuario que conectan esas pantallas. Para CADA flujo:
- ## Nombre del flujo (ej. "Registro de usuario", "Checkout")
- **Objetivo del usuario:** qué quiere conseguir
- **Disparador:** qué inicia el flujo
- **Camino feliz (happy path):** pasos numerados, cada paso referenciando la
  pantalla de ui-screens.md y la acción que realiza el usuario
- **Puntos de decisión / ramificaciones:** condicionales y caminos alternativos
- **Casos límite y errores:** qué pasa si falla algo o el usuario se desvía
- **Resultado final:** estado al completar el flujo

REGLAS
- Mantén coherencia: los nombres de pantalla en ux-flows.md deben coincidir
  exactamente con los títulos de ui-screens.md, idealmente enlazados.
- Cita siempre el archivo fuente (mockup o doc) en el que te basas.
- No dupliques contenido entre los dos archivos: ui-screens.md describe el
  "qué" (cada pantalla aislada); ux-flows.md describe el "cómo se recorren".
- Si detectas pantallas en los mockups que no están documentadas en /docs
  (o viceversa), añade una sección final "## Inconsistencias detectadas".