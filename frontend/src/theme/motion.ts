// Constantes de MOTION para Framer Motion (Ola A · fundaciones del rediseño).
// Equivalente JS de los tokens --dur-* / --ease-* / --press-* de styles/tokens.css.
// Regla de craft (apple-design / emil-design-eng): resortes por defecto sin rebote
// (critically damped); rebote SOLO cuando la interacción llevó momento (flick/drag).
// Todo lo interactivo respeta prefers-reduced-motion vía useReducedMotion.
import type { Transition, Variants } from 'framer-motion';

// Duraciones (segundos) — espejo de los tokens CSS en ms.
export const DUR = {
  instant: 0.09,
  fast: 0.14,
  base: 0.2,
  slow: 0.28,
} as const;

// Curvas de bézier (mismas que --ease-* en CSS).
export const EASE = {
  out: [0.22, 1, 0.36, 1],
  in: [0.4, 0, 1, 1],
  standard: [0.4, 0, 0.2, 1],
} as const;

// Resorte por defecto para UI: sin overshoot, respuesta rápida (~0.28s).
export const SPRING: Transition = { type: 'spring', bounce: 0, duration: 0.28 };

// Resorte con momento (drag/flick): overshoot sutil.
export const SPRING_MOMENTUM: Transition = { type: 'spring', bounce: 0.2, duration: 0.32 };

// Resorte del efecto de pulsado (Button whileTap): firme y snappy.
export const SPRING_PRESS: Transition = { type: 'spring', stiffness: 620, damping: 30, mass: 0.7 };

// Factor de hundimiento del pulsado táctil (== --press-scale).
export const PRESS_SCALE = 0.96;

// Variantes reutilizables para superficies que entran/salen (drawer off-canvas).
export const drawerVariants: Variants = {
  hidden: { x: '-100%' },
  visible: { x: 0 },
};

export const scrimVariants: Variants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1 },
};
