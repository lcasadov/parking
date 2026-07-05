import type { CurrentUser } from '../types/auth';

// Iniciales del usuario autenticado (mockup .avatar "RB"): primera letra del
// nombre + primera del apellido; fallback a las 2 primeras del login.
export function initialsOf(user: Pick<CurrentUser, 'firstName' | 'lastName' | 'login'>): string {
  const first = user.firstName?.trim() ?? '';
  const last = user.lastName?.trim() ?? '';
  const fromName = `${first.charAt(0)}${last.charAt(0)}`.trim();
  const initials = fromName.length > 0 ? fromName : user.login.slice(0, 2);
  return initials.toUpperCase();
}
