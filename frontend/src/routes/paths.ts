// Rutas en ingles (autoridad del change). Centralizadas para evitar literales
// repetidos (S1192).
export const ROUTES = {
  login: '/login',
  changePassword: '/change-password',
  admin: '/admin',
  adminEmployees: '/admin/employees',
  adminParkingSpaces: '/admin/parking-spaces',
  adminFixedAssignments: '/admin/fixed-assignments',
  adminRequests: '/admin/requests',
  adminReleases: '/admin/releases',
  employee: '/employee',
  employeeFixedAssignments: '/employee/fixed-assignments',
  employeeRequests: '/employee/requests',
  employeeReleases: '/employee/releases',
} as const;

export type Role = 'ADMIN' | 'EMPLOYEE';

export function homePathForRole(role: Role): string {
  return role === 'ADMIN' ? ROUTES.admin : ROUTES.employee;
}
