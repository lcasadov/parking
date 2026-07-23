import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { ExportMyDataButton } from './ExportMyDataButton';
import { LanguageToggle } from './LanguageToggle';
import { Popover } from './Popover';
import { ThemeToggle } from './ThemeToggle';
import { UserAvatar } from './UserAvatar';

// Area de usuario al pie del sidebar (prototipo `docs/design/prototipo-aleatica.html`):
// tarjeta con avatar + nombre + rol que abre un panel de preferencias con idioma,
// tema, exportar mis datos y cerrar sesion. Sustituye al antiguo top-bar (AppHeader):
// aqui vive ahora TODA la marca y los controles de usuario. Solo reubica logica ya
// existente (mutacion de logout de authApi, ThemeToggle, LanguageToggle,
// ExportMyDataButton, Popover) — no toca contratos de API ni datos.
export function SidebarUserCard() {
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

  if (!user) {
    return null;
  }

  const fullName =
    user.firstName || user.lastName
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
      : user.login;

  return (
    <div className="sidebar-user">
      <button
        type="button"
        className="sidebar-user-trigger"
        aria-haspopup="dialog"
        aria-expanded={menuOpen}
        aria-label={t('account.openMenu')}
        onClick={() => setMenuOpen((open) => !open)}
      >
        <UserAvatar user={user} label={fullName} />
        <span className="sidebar-user-meta">
          <span className="sidebar-user-name">{fullName}</span>
          <span className="sidebar-user-role">{t(`account.role.${user.role}`)}</span>
        </span>
        <i className="ti ti-logout" aria-hidden="true" />
      </button>
      {menuOpen ? (
        <Popover
          label={t('account.menuLabel')}
          className="sidebar-user-popover"
          onClose={() => setMenuOpen(false)}
        >
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
          <div className="pop-row pop-export">
            <ExportMyDataButton />
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
  );
}
