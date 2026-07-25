import { useEffect, useRef, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { LanguageToggle } from './LanguageToggle';
import { ThemeToggle } from './ThemeToggle';

// Contenedor de las pantallas de autenticacion (login / cambio de contrasena).
// Composicion centrada de marca: fondo con orbes de color Aleatica que derivan,
// spotlight que sigue al cursor, logo mini flotante y una card compacta con el
// formulario (children). El titulo/subtitulo de marca son fijos (identidad de la
// app); `title`/`subtitle` opcionales se muestran como encabezado DENTRO de la
// card (p. ej. "Cambiar contrasena"). Todo el movimiento se desactiva bajo
// prefers-reduced-motion y en punteros no finos (movil/tactil).
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title?: ReactNode;
  subtitle?: string;
  children: ReactNode;
}) {
  const { t } = useTranslation();
  const rootRef = useRef<HTMLDivElement>(null);
  const badgeRef = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const root = rootRef.current;
    if (!root) {
      return;
    }
    if (typeof window.matchMedia !== 'function') {
      return;
    }
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const fine = window.matchMedia('(pointer: fine)').matches;
    if (reduce || !fine) {
      return;
    }
    const badge = badgeRef.current;
    let raf = 0;
    let mx = 50;
    let my = 40;
    let nx = 0;
    let ny = 0;
    let live = false;

    function apply() {
      raf = 0;
      root!.style.setProperty('--mx', `${mx}%`);
      root!.style.setProperty('--my', `${my}%`);
      if (!live) {
        root!.classList.add('is-live');
        live = true;
      }
      if (badge) {
        badge.style.transform =
          `perspective(700px) translate(${(nx * 9).toFixed(1)}px,${(ny * 9).toFixed(1)}px)` +
          ` rotateX(${(ny * -9).toFixed(2)}deg) rotateY(${(nx * 9).toFixed(2)}deg)`;
      }
    }

    function onMove(event: PointerEvent) {
      mx = (event.clientX / window.innerWidth) * 100;
      my = (event.clientY / window.innerHeight) * 100;
      nx = (event.clientX / window.innerWidth - 0.5) * 2;
      ny = (event.clientY / window.innerHeight - 0.5) * 2;
      if (!raf) {
        raf = window.requestAnimationFrame(apply);
      }
    }

    window.addEventListener('pointermove', onMove);
    return () => {
      window.removeEventListener('pointermove', onMove);
      if (raf) {
        window.cancelAnimationFrame(raf);
      }
    };
  }, []);

  return (
    <div className="auth2" ref={rootRef}>
      <div className="auth2-bg" aria-hidden="true">
        <span className="auth2-orb o1" />
        <span className="auth2-orb o2" />
        <span className="auth2-orb o3" />
      </div>
      <div className="auth2-spot" aria-hidden="true" />
      <div className="auth2-controls">
        <LanguageToggle />
        <ThemeToggle />
      </div>
      <div className="auth2-wrap">
        <div className="auth2-stack">
          <span className="auth2-badge" ref={badgeRef}>
            <img src="/logo-aleatica-mini.png" alt="Aleatica" />
          </span>
          <h1 className="auth2-title">
            {t('auth.brandTitleLead')} <span className="g">{t('auth.brandTitleHighlight')}</span>
          </h1>
          <p className="auth2-sub">{t('auth.brandSubtitle')}</p>
          <div className="auth2-cardwrap">
            <div className="auth2-card">
              {title ? <p className="auth2-cardhead">{title}</p> : null}
              {subtitle ? <p className="auth2-cardsub">{subtitle}</p> : null}
              {children}
            </div>
          </div>
          <p className="auth2-foot">
            <b>{t('common.appName')}</b> · {t('auth.secureFooter')}
          </p>
        </div>
      </div>
    </div>
  );
}
