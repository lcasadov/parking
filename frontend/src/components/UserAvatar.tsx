import type { CurrentUser } from '../types/auth';
import { initialsOf } from '../utils/initials';
import { Avatar } from './Avatar';

// Avatar del usuario autenticado (cabecera). Delega en <Avatar> con las
// iniciales calculadas; mantiene la API publica previa (user + label).
export function UserAvatar({
  user,
  label,
}: {
  user: Pick<CurrentUser, 'firstName' | 'lastName' | 'login'>;
  label: string;
}) {
  return <Avatar initials={initialsOf(user)} label={label} size="md" />;
}
