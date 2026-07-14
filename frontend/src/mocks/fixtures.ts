import type { CurrentUser } from '../types/auth';

// CurrentUser exacto al contrato: { employeeId, login, role, passwordMustChange }.
export const adminUser: CurrentUser = {
  employeeId: 1,
  login: 'admin',
  firstName: 'Ada',
  lastName: 'Admin',
  role: 'ADMIN',
  passwordMustChange: false,
};

export const employeeUser: CurrentUser = {
  employeeId: 2,
  login: 'emp',
  firstName: 'Eve',
  lastName: 'Employee',
  role: 'EMPLOYEE',
  passwordMustChange: false,
};

export const employeeMustChange: CurrentUser = {
  ...employeeUser,
  passwordMustChange: true,
};

export const agencyUser: CurrentUser = {
  employeeId: 3,
  login: 'agency',
  firstName: 'Agatha',
  lastName: 'Agency',
  role: 'AGENCIA',
  passwordMustChange: false,
};
