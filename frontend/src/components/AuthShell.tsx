import { type ReactNode } from 'react';
import { BrandCurve, BrandLogo } from './BrandCurve';
import { LanguageToggle } from './LanguageToggle';
import { ThemeToggle } from './ThemeToggle';

// Contenedor de las pantallas de autenticacion (login / cambio de contrasena):
// auth-card con cabecera de marca + curva, y toggles de tema/idioma.
export function AuthShell({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="auth-wrap">
      <div className="auth-card">
        <div className="auth-head">
          <BrandCurve className="curve" variant="auth" />
          <BrandLogo />
          <h1 className="auth-title">{title}</h1>
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
