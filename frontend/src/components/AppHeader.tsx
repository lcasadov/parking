import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { BrandCurve, BrandLogo } from './BrandCurve';
import { ExportMyDataButton } from './ExportMyDataButton';
import { LanguageToggle } from './LanguageToggle';
import { Popover } from './Popover';
import { ThemeToggle } from './ThemeToggle';
import { UserAvatar } from './UserAvatar';

// Cabecera de la app: logo + curva SVG + menu de usuario (mockup 17). Idioma,
// tema y cierre de sesion viven dentro de un Popover anclado bajo el avatar.
export function AppHeader({ pageTitle }: { pageTitle?: string }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, clearUser } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const mutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearUser();
      navigate(ROUTES.login, { replace: true });
    },
  });

  const fullName =
    user && (user.firstName || user.lastName)
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
      : (user?.login ?? '');

  return (
    <header className="app-header">
      <BrandCurve className="curve" />
      <BrandLogo />
      {pageTitle ? <span className="page-title">{pageTitle}</span> : null}
      <div className="header-controls">
        <ExportMyDataButton />
        {user ? (
          <div className="user-menu">
            <button
              type="button"
              className="avatar-trigger"
              aria-haspopup="dialog"
              aria-expanded={menuOpen}
              aria-label={t('account.openMenu')}
              onClick={() => setMenuOpen((open) => !open)}
            >
              <UserAvatar user={user} label={fullName} />
            </button>
            {menuOpen ? (
              <Popover label={t('account.menuLabel')} onClose={() => setMenuOpen(false)}>
                <div className="pop-row identity">
                  <span className="pop-name">{fullName}</span>
                  <span className="pop-sub">
                    {t(`account.role.${user.role}`)} · {user.login}
                  </span>
                </div>
                <div className="pop-row">
                  <span className="pop-label">
                    <i className="ti ti-language" aria-hidden="true" /> {t('common.language')}
                  </span>
                  <LanguageToggle />
                </div>
                <div className="pop-row">
                  <span className="pop-label">
                    <i className="ti ti-moon" aria-hidden="true" /> {t('common.theme')}
                  </span>
                  <ThemeToggle />
                </div>
                <button
                  type="button"
                  className="pop-row pop-danger"
                  onClick={() => mutation.mutate()}
                  disabled={mutation.isPending}
                >
                  <span className="pop-label">
                    <i className="ti ti-logout" aria-hidden="true" /> {t('common.logout')}
                  </span>
                  <i className="ti ti-chevron-right" aria-hidden="true" />
                </button>
              </Popover>
            ) : null}
          </div>
        ) : null}
      </div>
    </header>
  );
}
