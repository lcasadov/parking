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
  adminReleaseByDate: '/admin/release-by-date',
  adminVisitors: '/admin/visitors',
  adminCalendar: '/admin/calendar',
  adminAvailability: '/admin/availability',
  adminAudit: '/admin/audit',
  adminLoginLogs: '/admin/login-logs',
  adminSettings: '/admin/settings',
  employee: '/employee',
  employeeFixedAssignments: '/employee/fixed-assignments',
  employeeRequests: '/employee/requests',
  employeeReleases: '/employee/releases',
  employeeMyWeek: '/employee/my-week',
  employeeFloorPlan: '/employee/floor-plan',
  agency: '/agency',
  agencyReleases: '/agency/releases',
} as const;

export type Role = 'ADMIN' | 'EMPLOYEE' | 'AGENCIA';

export function homePathForRole(role: Role): string {
  if (role === 'ADMIN') {
    return ROUTES.admin;
  }
  if (role === 'AGENCIA') {
    return ROUTES.agency;
  }
  return ROUTES.employee;
}
