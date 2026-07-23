import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet, useLocation } from 'react-router-dom';
import aleaticaLogo from '../assets/aleatica-logo.png';
import { useAuth } from '../auth/useAuth';
import { DUR, EASE, drawerVariants, scrimVariants } from '../theme/motion';
import { Sidebar } from './Sidebar';
import { SidebarUserCard } from './SidebarUserCard';
import { UserAvatar } from './UserAvatar';

interface AppShellProps {
  // Contenido de navegación (secciones + NavLinks) provisto por cada layout.
  // Se renderiza en el sidebar fijo (desktop) y en el drawer off-canvas (móvil).
  nav: ReactNode;
}

// Shell de la app (Ola A · rediseño). Chrome FIJO en las dos resoluciones:
//  · Desktop (≥769px): sidebar ALEATICA fijo a la izquierda (position:fixed,
//    100vh); el área de contenido desplaza y el menú permanece anclado.
//  · Móvil (≤768px): header de cristal fijo arriba (marca + hamburguesa + avatar)
//    con safe-area de iOS; la navegación vive en un drawer off-canvas que entra
//    con resorte (Framer Motion) y scrim que atenúa el fondo. El contenido queda
//    bajo el header fijo. Respeta prefers-reduced-motion (sin deslizamiento).
// Re-viste los 3 layouts (Admin/Employee/Agency) sin tocar sus destinos: cada
// layout pasa sus NavLinks por `nav`; el wiring de rutas/datos no cambia.
export function AppShell({ nav }: AppShellProps) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const location = useLocation();
  const reduceMotion = useReducedMotion();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const drawerRef = useRef<HTMLDivElement>(null);

  const closeDrawer = useCallback(() => setDrawerOpen(false), []);
  const openDrawer = useCallback(() => setDrawerOpen(true), []);

  // Cerrar el drawer al navegar (cambia la ruta al pulsar un destino).
  useEffect(() => {
    setDrawerOpen(false);
  }, [location.pathname]);

  // Bloquear el scroll del body y escuchar Escape mientras el drawer está abierto.
  useEffect(() => {
    if (!drawerOpen) {
      return;
    }
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    drawerRef.current?.focus();
    function handleKey(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        setDrawerOpen(false);
      }
    }
    window.addEventListener('keydown', handleKey);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKey);
    };
  }, [drawerOpen]);

  const fullName = user
    ? user.firstName || user.lastName
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
      : user.login
    : '';

  const drawerTransition = reduceMotion
    ? { duration: 0 }
    : { type: 'spring' as const, bounce: 0, duration: DUR.slow };

  return (
    <div className="app-shell">
      {/* Sidebar fijo (desktop). Oculto en móvil vía CSS. */}
      <Sidebar className="shell-sidebar" footer={<SidebarUserCard />}>
        {nav}
      </Sidebar>

      {/* Header de cristal fijo (móvil). Oculto en desktop vía CSS. */}
      <header className="shell-topbar">
        <button
          type="button"
          className="shell-topbar-btn"
          aria-label={t('layout.nav.open')}
          aria-expanded={drawerOpen}
          aria-controls="app-drawer"
          onClick={openDrawer}
        >
          <i className="ti ti-menu-2" aria-hidden="true" />
        </button>
        <img src={aleaticaLogo} alt="ALEATICA" className="shell-topbar-logo" />
        {user ? (
          <button
            type="button"
            className="shell-topbar-avatar"
            aria-label={t('layout.nav.open')}
            onClick={openDrawer}
          >
            <UserAvatar user={user} label={fullName} />
          </button>
        ) : (
          <span className="shell-topbar-spacer" />
        )}
      </header>

      {/* Drawer off-canvas (móvil) + scrim. */}
      <AnimatePresence>
        {drawerOpen ? (
          <div className="shell-drawer-root">
            <motion.button
              type="button"
              className="shell-scrim"
              aria-label={t('layout.nav.close')}
              onClick={closeDrawer}
              variants={scrimVariants}
              initial="hidden"
              animate="visible"
              exit="hidden"
              transition={{ duration: reduceMotion ? 0 : DUR.base, ease: EASE.out }}
            />
            <motion.div
              id="app-drawer"
              ref={drawerRef}
              className="shell-drawer"
              role="dialog"
              aria-modal="true"
              aria-label={t('layout.nav.menuLabel')}
              tabIndex={-1}
              variants={drawerVariants}
              initial="hidden"
              animate="visible"
              exit="hidden"
              transition={drawerTransition}
            >
              {/* Al pulsar un destino, el cambio de ruta cierra el drawer vía el
                  efecto de location.pathname (no requiere handler en el contenedor). */}
              <div className="shell-drawer-inner">
                <button
                  type="button"
                  className="shell-drawer-close"
                  aria-label={t('layout.nav.close')}
                  onClick={closeDrawer}
                >
                  <i className="ti ti-x" aria-hidden="true" />
                </button>
                <Sidebar ariaLabel="mobile" footer={<SidebarUserCard />}>
                  {nav}
                </Sidebar>
              </div>
            </motion.div>
          </div>
        ) : null}
      </AnimatePresence>

      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
