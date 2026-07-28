import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { UserAvatar } from './UserAvatar';

// Area de usuario al pie del sidebar. La píldora de perfil (avatar + nombre + rol)
// dispara DIRECTAMENTE un diálogo de confirmación de cierre de sesión. Se eliminó
// el popover de preferencias: idioma y tema ya viven en el pie del sidebar
// (LanguageToggle/ThemeToggle) y "Exportar mis datos" se retiró por no aportar al
// empleado (change reservas-employee-admin-reassign, Feature F).
export function SidebarUserCard() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, clearUser } = useAuth();
  const [confirmOpen, setConfirmOpen] = useState(false);

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
        aria-label={t('common.logout')}
        onClick={() => setConfirmOpen(true)}
      >
        <UserAvatar user={user} label={fullName} />
        <span className="sidebar-user-meta">
          <span className="sidebar-user-name">{fullName}</span>
          <span className="sidebar-user-role">{t(`account.role.${user.role}`)}</span>
        </span>
        <i className="ti ti-logout" aria-hidden="true" />
      </button>
      {confirmOpen ? (
        <Dialog
          open
          narrow
          tone="green"
          icon="logout"
          title={t('account.logoutConfirm.title')}
          onOpenChange={(open) => {
            if (!open) {
              setConfirmOpen(false);
            }
          }}
          footer={
            <>
              <Button variant="white" onClick={() => setConfirmOpen(false)}>
                {t('account.logoutConfirm.cancel')}
              </Button>
              <Button variant="red" onClick={() => mutation.mutate()} loading={mutation.isPending}>
                {t('account.logoutConfirm.confirm')}
              </Button>
            </>
          }
        >
          <p>{t('account.logoutConfirm.body')}</p>
        </Dialog>
      ) : null}
    </div>
  );
}
