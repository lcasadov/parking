import { type ReactNode } from 'react';
import { BrandCurve, BrandLogo } from './BrandCurve';
import { LanguageToggle } from './LanguageToggle';
import { ThemeToggle } from './ThemeToggle';

// Contenedor de las pantallas de autenticacion (login / cambio de contrasena):
// auth-card con cabecera de marca + curva, y toggles de tema/idioma.
// `subtitle` es opcional: linea de apoyo bajo el titulo (p. ej. login).
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
}) {
  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-head">
          <BrandCurve className="curve" variant="auth" />
          <BrandLogo />
          <h1 className="auth-title">{title}</h1>
          {subtitle ? <p className="auth-subtitle">{subtitle}</p> : null}
        </div>
        <div className="auth-body">
          {children}
          <div className="auth-controls">
            <LanguageToggle />
            <ThemeToggle />
          </div>
        </div>
      </div>
    </div>
  );
}
