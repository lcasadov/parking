import { motion, useReducedMotion } from 'framer-motion';
import { type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { BrandLogo } from './BrandCurve';
import { LanguageToggle } from './LanguageToggle';
import { ThemeToggle } from './ThemeToggle';
import { DUR, EASE } from '../theme/motion';

// Iconos Tabler de los tres puntos de valor del panel editorial (sin prefijo "ti-").
const SHOWCASE_ICONS = ['map-pin', 'calendar-event', 'shield-check'] as const;

// Contenedor de las pantallas de autenticacion (login / cambio de contrasena),
// OLA B4: en escritorio ancho (>=900px) se divide en un panel editorial de marca
// (izquierda, oculto en movil) + la auth-card (derecha); en movil SOLO se ve la
// card, centrada, a pantalla completa (mobile-first: es la experiencia real de
// la mayoria de accesos). La card materializa con una entrada suave (fade + un
// leve ascenso) que se anula bajo prefers-reduced-motion.
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
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();

  return (
    <div className="auth-viewport">
      <aside className="auth-showcase">
        <div className="auth-showcase-glow" aria-hidden="true" />
        <div className="auth-showcase-content">
          <span className="auth-showcase-mark" aria-hidden="true" />
          <p className="auth-showcase-eyebrow">{t('common.appName')}</p>
          <h2 className="auth-showcase-title">{t('auth.showcaseTagline')}</h2>
          <ul className="auth-showcase-points">
            {SHOWCASE_ICONS.map((icon, index) => (
              <li key={icon}>
                <i className={`ti ti-${icon}`} aria-hidden="true" />
                {t(`auth.showcasePoint${index + 1}`)}
              </li>
            ))}
          </ul>
        </div>
      </aside>
      <main className="auth-main">
        <motion.div
          className="auth-card"
          initial={reduceMotion ? { opacity: 1, y: 0 } : { opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: reduceMotion ? 0 : DUR.slow, ease: EASE.out }}
        >
          <div className="auth-head">
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
        </motion.div>
      </main>
    </div>
  );
}
