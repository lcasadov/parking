import type { CurrentUser } from '../types/auth';
import { initialsOf } from '../utils/initials';

// Avatar circular con iniciales del usuario (mockup .avatar). label accesible
// (nombre completo o login) para lectores de pantalla.
export function UserAvatar({
  user,
  label,
}: {
  user: Pick<CurrentUser, 'firstName' | 'lastName' | 'login'>;
  label: string;
}) {
  return (
    <span className="avatar" title={label} aria-label={label} role="img">
      {initialsOf(user)}
    </span>
  );
}
