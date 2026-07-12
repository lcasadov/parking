// Rutas en ingles (autoridad del change). Centralizadas para evitar literales
// repetidos (S1192).
export const ROUTES = {
  login: '/login',
  changePassword: '/change-password',
  admin: '/admin',
  adminEmployees: '/admin/employees',
  adminParkingSpaces: '/admin/parking-spaces',
  adminDesks: '/admin/desks',
  adminFloorPlan: '/admin/floor-plan',
  adminRequests: '/admin/requests',
  adminReleases: '/admin/releases',
  adminVisitors: '/admin/visitors',
  adminCalendar: '/admin/calendar',
  adminAvailability: '/admin/availability',
  adminAudit: '/admin/audit',
  adminLoginLogs: '/admin/login-logs',
  employee: '/employee',
  employeeFixedAssignments: '/employee/fixed-assignments',
  employeeRequests: '/employee/requests',
  employeeReleases: '/employee/releases',
  employeeMyWeek: '/employee/my-week',
  employeeFloorPlan: '/employee/floor-plan',
} as const;

export type Role = 'ADMIN' | 'EMPLOYEE';

export function homePathForRole(role: Role): string {
  return role === 'ADMIN' ? ROUTES.admin : ROUTES.employee;
}
