// Paleta de avatares del design-system (mockup .av-*). El color se deriva de
// forma estable (hash determinista) del nombre, de modo que un mismo usuario
// siempre obtiene el mismo color en toda la app.
export const AVATAR_COLORS = [
  'av-purple',
  'av-teal',
  'av-pink',
  'av-amber',
  'av-blue',
  'av-coral',
] as const;

export type AvatarColor = (typeof AVATAR_COLORS)[number];

// Hash entero estable (variante djb2/31) sobre el seed; |0 fuerza int32.
export function avatarColorOf(seed: string): AvatarColor {
  let hash = 0;
  for (let i = 0; i < seed.length; i += 1) {
    hash = (hash * 31 + seed.charCodeAt(i)) | 0;
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}
