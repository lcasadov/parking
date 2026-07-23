import { motion, useReducedMotion } from 'framer-motion';
import { type ButtonHTMLAttributes } from 'react';
import { PRESS_SCALE, SPRING_PRESS } from '../theme/motion';

type Variant = 'green' | 'blue' | 'red' | 'white';

// Se omiten 'type' (lo fija `submit`) y los handlers de drag/animación nativos,
// cuyas firmas chocan con las de Framer Motion al pasar por motion.button.
type NativeButtonProps = Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  | 'type'
  | 'onDrag'
  | 'onDragStart'
  | 'onDragEnd'
  | 'onDragEnter'
  | 'onDragExit'
  | 'onDragLeave'
  | 'onDragOver'
  | 'onDrop'
  | 'onAnimationStart'
  | 'onAnimationEnd'
  | 'onAnimationIteration'
>;

interface ButtonProps extends NativeButtonProps {
  variant?: Variant;
  icon?: string;
  submit?: boolean;
}

// Botón del design system ALEATICA con EFECTO DE PULSADO TÁCTIL (Ola A):
// al presionar (whileTap) se hunde a --press-scale (0.96) con un resorte firme
// (SPRING_PRESS) y CSS realza una sombra al :active. El feedback nace en el
// pointer-down (apple-design §1: responder al press, no al release) y se anula
// bajo prefers-reduced-motion. Misma API pública que antes (variant/icon/submit):
// cualquier <Button> existente adopta el efecto sin cambios de código.
export function Button({
  variant = 'white',
  icon,
  submit = false,
  children,
  className,
  disabled,
  ...rest
}: ButtonProps) {
  const reduceMotion = useReducedMotion();
  const classes = ['btn', `btn-${variant}`, className].filter(Boolean).join(' ');
  const tap = reduceMotion || disabled ? undefined : { scale: PRESS_SCALE };

  return (
    <motion.button
      type={submit ? 'submit' : 'button'}
      className={classes}
      disabled={disabled}
      whileTap={tap}
      transition={SPRING_PRESS}
      {...rest}
    >
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      {children}
    </motion.button>
  );
}
