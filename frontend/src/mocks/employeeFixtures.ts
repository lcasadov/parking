import type { Employee, PageEmployee } from '../types/employee';

// Empleados de ejemplo para los tests (contrato #/components/schemas/Employee).
export const employeeAlice: Employee = {
  id: 10,
  firstName: 'Alice',
  lastName: 'Andersson',
  login: 'aandersson',
  email: 'alice@aleatica.com',
  department: 'Finanzas',
  mobilePhone: '+34600000010',
  licensePlate: '1234ABC',
  isCorporate: true,
  authOrigin: 'LOCAL',
  role: 'EMPLOYEE',
  category: 'EMPLEADO',
  enabled: true,
  active: true,
  passwordMustChange: false,
  createdAt: '2026-01-10T09:00:00Z',
  updatedAt: null,
};

export const employeeBob: Employee = {
  id: 11,
  firstName: 'Bob',
  lastName: 'Brown',
  login: 'bbrown',
  email: 'bob@aleatica.com',
  department: 'IT',
  mobilePhone: null,
  licensePlate: null,
  isCorporate: false,
  authOrigin: 'LOCAL',
  role: 'ADMIN',
  category: 'DIRECTOR_N1',
  enabled: true,
  active: false,
  passwordMustChange: false,
  createdAt: '2026-01-11T09:00:00Z',
  updatedAt: '2026-02-01T09:00:00Z',
};

// Envuelve una lista de empleados en una PageEmployee de una sola pagina.
export function pageOf(content: Employee[]): PageEmployee {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

export const defaultEmployeePage: PageEmployee = pageOf([employeeAlice, employeeBob]);
