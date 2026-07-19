import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/useAuth';
import { UserAvatar } from './UserAvatar';

// Tarjeta de usuario al pie del sidebar (contrato §5): avatar + nombre + rol.
// Presentacional: lee el usuario del contexto de auth (sin llamadas a API).
export function SidebarUserCard() {
  const { t } = useTranslation();
  const { user } = useAuth();

  if (!user) {
    return null;
  }

  const fullName =
    user.firstName || user.lastName
      ? `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
      : user.login;

  return (
    <div className="sidebar-user">
      <UserAvatar user={user} label={fullName} />
      <span className="sidebar-user-meta">
        <span className="sidebar-user-name">{fullName}</span>
        <span className="sidebar-user-role">{t(`account.role.${user.role}`)}</span>
      </span>
    </div>
  );
}
