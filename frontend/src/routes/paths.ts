// Rutas en ingles (autoridad del change). Centralizadas para evitar literales
// repetidos (S1192).
//
// restructure-admin-workflows (Fase 1): la navegacion se reagrupa por tarea
// (app-shell spec). Los destinos fusionados en pestañas viven en una URL nueva
// (canonica, claves nuevas); las claves de las URLs antiguas se conservan tal
// cual (mismo nombre, mismo valor) porque AppRoutes.tsx las redirige a la nueva
// con la pestaña correspondiente via `?tab=`, sin romper enlaces internos
// existentes ni los tests de RBAC que ya las referencian (tasks §1.11).
export const ROUTES = {
  login: '/login',
  changePassword: '/change-password',
  admin: '/admin',
  // Operativa (destinos nuevos/canonicos)
  adminRequests: '/admin/requests',
  adminOccupancy: '/admin/occupancy',
  adminFloorPlan: '/admin/floor-plan',
  adminReleaseHub: '/admin/release',
  adminVisitors: '/admin/visitors',
  // Gestión (destinos nuevos/canonicos)
  adminEmployees: '/admin/employees',
  adminResources: '/admin/resources',
  adminRecords: '/admin/records',
  adminSettings: '/admin/settings',
  // Rutas antiguas: siguen existiendo como redireccion a su destino fusionado.
  adminParkingSpaces: '/admin/parking-spaces',
  adminDesks: '/admin/desks',
  adminReleases: '/admin/releases',
  adminReleaseByDate: '/admin/release-by-date',
  adminCalendar: '/admin/calendar',
  adminAvailability: '/admin/availability',
  adminAudit: '/admin/audit',
  adminLoginLogs: '/admin/login-logs',
  employee: '/employee',
  employeeMyWeek: '/employee/my-week',
  employeeFloorPlan: '/employee/floor-plan',
  employeeRequests: '/employee/requests',
  employeeMyResources: '/employee/my-resources',
  // Rutas antiguas: redirigen a "Mis plazas" con la pestaña correspondiente.
  employeeFixedAssignments: '/employee/fixed-assignments',
  employeeReleases: '/employee/releases',
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
