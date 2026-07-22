# ALEATICA · Design Contract (rediseño frontend)

Fuente de verdad visual para los changes de OpenSpec. El prototipo aprobado es
`Parking Solárium.dc.html`. Este documento traduce ese prototipo a tokens y reglas
para el frontend real (React 18 + TS + Vite, CSS con variables, @tabler/icons-webfont).

Alcance: SOLO capa de presentación. No se tocan contratos de API, modelos, SQL,
routing de datos ni lógica de negocio.

## 1. Paleta (muestreada del logo oficial)
--brand-green:      #77B829   /* primario / ocupado */
--brand-green-deep: #527F16
--brand-green-soft: #E9F3D8
--brand-blue:       #00A8E2   /* liberado */
--brand-blue-deep:  #06678A
--brand-blue-soft:  #DCF0FA
--brand-yellow:     #FBDC17   /* pendiente asignar */
--amber-deep:       #8A6A00
--amber-soft:       #FCF3CC
--brand-orange:     #F6A016   /* solicitud */
--orange-deep:      #A55E05
--orange-soft:      #FDE9CE
--taupe:            #867870

## 2. Neutros / superficie
--bg:       #EEEDE7
--panel:    #FFFFFF
--panel-2:  #F6F5F0
--ink:      #2B2A28
--ink-soft: #6E6A63
--ink-faint:#9A968D
--line:      rgba(43,42,40,.14)
--line-soft: rgba(43,42,40,.08)

## 3. Tipografía
- Títulos y numerales: 'Cormorant Garamond' (600), serif editorial.
- Cuerpo / UI: 'Mulish' (300–800).
- Cargar por <link> de Google Fonts (o self-host en /public/fonts).
- Escala: h1 52px, h2 34px, numerales grandes 34–42px, cuerpo 16px, labels 11px
  uppercase letter-spacing .13–.22em.

## 4. Estado de plaza/puesto → color (regla única, reutilizar en todas las vistas)
| estado       | fondo         | borde         | uso            |
|--------------|---------------|---------------|----------------|
| ocupado      | green-soft    | #c3e08f       | titular activo |
| liberado     | blue-soft     | #a6dbf2       | liberado hoy   |
| pendiente    | amber-soft    | #eed873       | pdte. asignar  |
| solicitud    | orange-soft   | #f6c988       | solicitud      |
| libre        | panel-2       | dashed        | disponible     |

## 5. Componentes base
- Radios: 8px controles, 12–14px tarjetas/paneles, 20px pills, 50% avatares.
- Sombra paneles: 0 30px 60px -48px rgba(43,42,40,.5).
- Botón primario: fondo green, texto #fff, peso 800.
- Botón secundario: panel + border line.
- Sidebar 252px, item activo con border-left 3px accent + fondo accent-soft.

## 6. Pantallas ya diseñadas (referencia en el prototipo)
1. Shell (sidebar + header + tarjeta de usuario).
2. Asignación semanal (grid plaza × día, celdas por estado).
3. Plano del día (imagen real floor-plan.png + marcadores por coordenadas).
4. Solicitudes (tabla con avatares de color, aprobar/rechazar).
5. Login (logo, campos, avisos de bloqueo).

## 7. Assets
- Logo: assets/aleatica-logo.png (ya en el repo: frontend/src/assets/).
- Plano: frontend/src/assets/floor-plan.png (coordenadas de puestos en el seed
  V-desks; marcadores en % sobre la imagen).
