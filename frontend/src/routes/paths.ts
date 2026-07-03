// Rutas en ingles (autoridad del change). Centralizadas para evitar literales
// repetidos (S1192).
export const ROUTES = {
  login: '/login',
  changePassword: '/change-password',
  admin: '/admin',
  adminEmployees: '/admin/employees',
  employee: '/employee',
} as const;

export type Role = 'ADMIN' | 'EMPLOYEE';

export function homePathForRole(role: Role): string {
  return role === 'ADMIN' ? ROUTES.admin : ROUTES.employee;
}
