import { avatarColorOf } from '../utils/avatarColor';

type AvatarSize = 'md' | 'sm';

interface AvatarProps {
  // Iniciales ya calculadas (p.ej. "RB").
  initials: string;
  // Nombre accesible completo (lectores de pantalla / tooltip).
  label: string;
  size?: AvatarSize;
  // Semilla del color pastel derivado. Por defecto, el propio label.
  seed?: string;
}

// Avatar circular con iniciales. size='md' usa la variante base fija (.avatar,
// cabecera); size='sm' usa .avatar-sm con un color av-* derivado del seed de
// forma estable (mockup .av-*, celdas de tabla).
export function Avatar({ initials, label, size = 'md', seed }: AvatarProps) {
  const className =
    size === 'sm' ? `avatar-sm ${avatarColorOf(seed ?? label)}` : 'avatar';
  return (
    <span className={className} title={label} aria-label={label} role="img">
      {initials}
    </span>
  );
}
