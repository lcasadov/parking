// Tipos derivados del contrato docs/openapi.yaml (componentes Employee).

import type { Role } from './auth';

export type { Role };

// AuthOrigin: schema #/components/schemas/AuthOrigin.
export type AuthOrigin = 'LOCAL' | 'ENTRA_ID';

// Formato de exportacion: schema ExportFormatParam (enum [csv, xlsx]).
export type ExportFormat = 'csv' | 'xlsx';

// EmployeeCategory: schema #/components/schemas/EmployeeCategory.
// Orden jerarquico de mayor a menor rango.
export type EmployeeCategory =
  | 'CEO'
  | 'CONSEJO'
  | 'DIRECTOR_N1'
  | 'DIRECTOR_N2'
  | 'GERENTE'
  | 'MANDO_INTERMEDIO'
  | 'EMPLEADO';

// Valores de EmployeeCategory en orden jerarquico (mayor -> menor rango).
export const EMPLOYEE_CATEGORIES: EmployeeCategory[] = [
  'CEO',
  'CONSEJO',
  'DIRECTOR_N1',
  'DIRECTOR_N2',
  'GERENTE',
  'MANDO_INTERMEDIO',
  'EMPLEADO',
];

// Employee: schema #/components/schemas/Employee.
export interface Employee {
  id: number;
  firstName: string;
  lastName: string;
  login: string;
  email: string;
  department?: string | null;
  mobilePhone?: string | null;
  licensePlate?: string | null;
  isCorporate: boolean;
  emailNotificationsEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
  authOrigin: AuthOrigin;
  role: Role;
  category: EmployeeCategory;
  enabled: boolean;
  active: boolean;
  passwordMustChange: boolean;
  createdAt: string;
  updatedAt?: string | null;
}

// EmployeeCreate: schema #/components/schemas/EmployeeCreate.
export interface EmployeeCreate {
  firstName: string;
  lastName: string;
  login: string;
  email: string;
  department?: string;
  mobilePhone?: string;
  licensePlate?: string;
  isCorporate?: boolean;
  emailNotificationsEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
  authOrigin?: AuthOrigin;
  role: Role;
  category: EmployeeCategory;
}

// EmployeeUpdate: schema #/components/schemas/EmployeeUpdate (sin login).
export interface EmployeeUpdate {
  firstName?: string;
  lastName?: string;
  email?: string;
  department?: string;
  mobilePhone?: string;
  licensePlate?: string;
  isCorporate?: boolean;
  emailNotificationsEnabled?: boolean;
  pushNotificationsEnabled?: boolean;
  role?: Role;
  category: EmployeeCategory;
}

// EmployeeResetPasswordResponse: schema #/components/schemas/EmployeeResetPasswordResponse.
export interface EmployeeResetPasswordResponse {
  temporaryPassword?: string | null;
  mustChange: boolean;
}

// PageMeta + PageEmployee: schemas #/components/schemas/PageMeta y PageEmployee.
export interface PageEmployee {
  content: Employee[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam, SizeParam, QParam y filtro active.
export interface EmployeeListParams {
  page?: number;
  size?: number;
  q?: string;
  active?: boolean;
}
