# Concepto de diseño — "Wayfinding / Señalización viva"

Exploración visual audaz para la app de reserva de plazas y puestos de **ALEATICA**.
Rompe con el diseño actual (verde apagado corporativo, tarjetas redondeadas, modales
centrados) y lleva la interfaz al lenguaje real del dominio: **señalización de autopista
/ sala de control**. Plazas y puestos como **señales vivas** sobre un tablero.

> Son **mocks estáticos** (HTML autocontenido, sin dependencias). NO son la
> implementación; sirven para decidir la dirección antes de tocar el design system.

## Ficheros

| Fichero | Qué es |
|---|---|
| [`employee-panel.html`](./employee-panel.html) | **Pantalla real montada** del portal del empleado: shell con navegación lateral tipo ruta, cabecera, "HOY" (tu plaza/puesto), Mi Semana, mini-plano radar y Mis sitios fijos. |
| [`employee-styleframes.html`](./employee-styleframes.html) | **Style-frames** (deck de componentes/secciones) del empleado: portada/identidad, Mi Semana, Reservar con plano-radar, Liberar como panel de andén y el kit de componentes. |

Para verlos: abrir el `.html` en el navegador (o servirlos con cualquier estático).

## Principios de la dirección

- **Color** — fondo *asfalto* casi negro (sesgo petróleo); acento **ámbar señal**
  (paneles de mensaje variable de autopista). Ocupación con semántica de **semáforo**:
  verde-go (libre), rojo-stop (ocupado), azul eléctrico (tuyo), ámbar (solicitado),
  cian (liberado). Cero verde apagado.
- **Tipografía** — grotesca pesada a gran escala (titulares) + **monoespaciada** para
  telemetría y numerales tipo marcador kilométrico (`P·1004`, `D·02`). Sin webfonts
  (system stack empujado con peso, tracking y escala).
- **Layout** — paneles a sangre, divisores de **línea de carril**, **lámparas de estado**
  con glow, plano como **radar vivo** (canvas + barra de escaneo), y modales que suben
  como **paneles de andén (gantry)** en lugar del cuadro centrado.
- **Movimiento** — escaneo del plano, lámparas y nodo "HOY" pulsando, gantry al subir,
  latido "en vivo". Todo se desactiva con `prefers-reduced-motion`.

## Estado

- [x] Panel del empleado (pantalla montada) + style-frames
- [ ] Reservar (pantalla completa), Plano a pantalla, Mis sitios
- [ ] Portal de admin en el mismo lenguaje (Ocupación-tablero, Gestionar reserva, Registros)
- [ ] Traducir la dirección a tokens/CSS del design system real (fase de implementación)
