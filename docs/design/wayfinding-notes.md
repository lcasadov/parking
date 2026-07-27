# Wayfinding — notas de implementación y DUDAS para revisar

Rediseño visual "Wayfinding / señalización viva" aplicado a la app real (rama
`design/wayfinding-concept`). Este documento recoge decisiones tomadas de forma
autónoma y **dudas razonables** que conviene repasar juntos. No bloquean el avance.

## Estado
- **Tema global** (`:root` claro + `body.theme-dark` oscuro) en `frontend/src/styles/wayfinding.css`:
  toda la app (empleado + admin + agencia) y los modales adoptan el lenguaje en ambos temas.
- **Firmas de componentes** (mw-shield, línea de tránsito, mfa-card, badges con lámpara)
  scoped a `.portal-way` → hoy solo en páginas de EMPLEADO. Admin hereda tokens (colores)
  pero aún NO tiene sus firmas propias.
- Pantallas de empleado rediseñadas: Mi Semana, Mis sitios fijos, Mis solicitudes.
- Pendiente cuando se retome: Reservar (asistente/modal → ruta + gantry), Plano (radar),
  y firmas admin (Ocupación, Liberar, Recursos, Registros, Ajustes, Visitantes, Solicitudes).

## Dudas a repasar (no bloquean)
1. **Semántica de color en admin (Ocupación).** El wayfinding usa VERDE = libre/positivo.
   La app admin actual usa VERDE = *ocupado/asignado* y neutro = libre. Al aplicar el tema
   global, "ocupado" seguirá saliendo verde por los tokens `--state-occupied`. ¿Mantenemos la
   semántica actual de la app (ocupado=verde) o la invertimos hacia semáforo puro
   (libre=verde, ocupado=rojo)? Afecta a la rejilla de Ocupación y su leyenda.
2. **Plano real vs radar.** El "radar" (canvas con barrido) es del mock. El plano real usa una
   imagen PNG con marcadores. ¿Reemplazamos el PNG por un radar generativo, o solo re-skin de la
   superficie actual (asfalto + marcadores como lámparas)? El radar es más "epic" pero pierde la
   ubicación real de los puestos.
3. **Modales — firma "gantry".** Hoy los modales reciben solo tokens (superficies + acento). La
   firma de "panel de andén" (gantry que sube con filo ámbar) del mock aún no está. ¿La aplicamos
   a todos los modales (Dialog) globalmente, o solo a algunos?
4. **Marcador de plaza.** Formateo `P·<número>` (p. ej. `P·1004`). Los números de plaza codifican
   planta (1004 → planta 1). ¿Mostramos planta junto al marcador en algún sitio, o el número basta?
5. **Tipografía mono.** Los marcadores usan `ui-monospace, "SF Mono", Menlo, …` (sin webfont, sin
   cero tachado en macOS). En Windows caería a Consolas. ¿OK, o empaquetamos una mono propia?
6. **Rejilla de ingeniería (tema claro).** Se pinta en `.main` global. Verificar que no choque con
   pantallas admin a sangre o con fondos propios.
7. **Tests.** Varias pruebas de empleado (MyWeekPage, MyFixedAssignmentsPage) han quedado
   desalineadas por los cambios de markup del rediseño. **Pendientes de actualizar antes de
   integrar** (se pospusieron a petición: fase de diseño, iterando en visual).

## Puntos de retorno (git)
- `72ce828` Mi Semana (aprobada)
- `932a4fd` Mis sitios fijos + Mis solicitudes
- `b350064` Tema Wayfinding GLOBAL (app + modales)

## Auditorías automáticas (agentes) — resultado

Se lanzaron 2 agentes críticos. Resumen:

### Contraste (WCAG) — CORREGIDO
3 fallos AA, todos en **tema claro**, por usar `--accent`/`--info` puros como texto pequeño:
labels HOY/MAÑANA, badge MAÑANA y badge HOY (blanco sobre ámbar). Corregidos en
`d9675d6` + calibración. Tema oscuro: sin fallos. Numerales grandes: sobra contraste.

### Consistencia/robustez UI — corregido lo crítico, resto diferido
CORREGIDO ya:
- Recorte de la línea de tiempo por `overflow-y` (más padding + reposicionado nodos/labels/pulso).
- Carril/pulso ya no se pintan sobre el esqueleto de carga (scoped a `.mw-week-scroll`).
- a11y: el número de plaza/puesto (`mfa-card-num`) ya NO está oculto a lectores de pantalla.
- Foco visible con grosor/estilo explícito (`outline: 2px solid`).
- Comentario de cabecera del CSS corregido (deja claro que tokens+grid son GLOBALES).
- Glow calibrado por tema (lámparas planas en día; halo solo de noche).

DIFERIDO (repasar juntos, no bloqueante):
1. **Semántica de color en admin (Ocupación):** hoy ocupado=verde (como la app actual).
   ¿Mantener o invertir a semáforo puro (libre=verde/ocupado=rojo)? Decisión de producto.
2. **Grid de ingeniería sobre tablas densas de admin:** verificar que no sea ruido; si molesta,
   acotar la rejilla a páginas concretas.
3. **Código muerto:** `renderHeroResource` + CSS `.mw-res*`/`.mw-hero-resources` ya no se montan
   (se quitó la tarjeta MAÑANA del héroe). Decidir: reintroducir MAÑANA o borrar el código muerto.
4. **Bloques `.mfa-card*` duplicados** en el CSS (un override "compacto" pisa al primero): fusionar.
5. **Sistematizar:** tamaños de lámpara/nodo y radios de glow como tokens (`--lamp-*`); radios/espaciados
   hardcodeados → escala `--radius-*`/`--space-*`.
6. **`--rel/--rel-soft` (ámbar) sin uso** (liberado usa azul): eliminar o documentar.
7. **Acabado:** el carril sobresale ~100px antes del primer nodo y tras el último (cuadrar a los
   centros de la 1ª/última estación); tracking mono unificar por rol tipográfico.
