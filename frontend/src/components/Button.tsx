import { type ButtonHTMLAttributes } from 'react';

type Variant = 'green' | 'blue' | 'red' | 'white';

interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'type'> {
  variant?: Variant;
  icon?: string;
  submit?: boolean;
}

export function Button({
  variant = 'white',
  icon,
  submit = false,
  children,
  className,
  ...rest
}: ButtonProps) {
  const classes = ['btn', `btn-${variant}`, className].filter(Boolean).join(' ');
  return submit ? (
    <button type="submit" className={classes} {...rest}>
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      {children}
    </button>
  ) : (
    <button type="button" className={classes} {...rest}>
      {icon ? <i className={`ti ti-${icon}`} aria-hidden="true" /> : null}
      {children}
    </button>
  );
}
