import { apiClient } from './apiClient';
import type {
  Employee,
  EmployeeCreate,
  EmployeeListParams,
  EmployeeResetPasswordResponse,
  EmployeeUpdate,
  PageEmployee,
} from '../types/employee';

// Endpoints de Employees segun docs/openapi.yaml. baseURL relativo del apiClient.

const EMPLOYEES = '/employees';

// Serializa los parametros de listado omitiendo los indefinidos/vacios.
function buildListParams(params: EmployeeListParams): Record<string, string | number | boolean> {
  const query: Record<string, string | number | boolean> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  if (params.q !== undefined && params.q !== '') {
    query.q = params.q;
  }
  if (params.active !== undefined) {
    query.active = params.active;
  }
  return query;
}

export async function listEmployees(params: EmployeeListParams = {}): Promise<PageEmployee> {
  const { data } = await apiClient.get<PageEmployee>(EMPLOYEES, {
    params: buildListParams(params),
  });
  return data;
}

export async function createEmployee(body: EmployeeCreate): Promise<Employee> {
  const { data } = await apiClient.post<Employee>(EMPLOYEES, body);
  return data;
}

export async function updateEmployee(id: number, body: EmployeeUpdate): Promise<Employee> {
  const { data } = await apiClient.put<Employee>(`${EMPLOYEES}/${id}`, body);
  return data;
}

export async function deactivateEmployee(id: number): Promise<void> {
  await apiClient.delete(`${EMPLOYEES}/${id}`);
}

export async function reactivateEmployee(id: number): Promise<void> {
  await apiClient.post(`${EMPLOYEES}/${id}/reactivate`);
}

export async function resetEmployeePassword(id: number): Promise<EmployeeResetPasswordResponse> {
  const { data } = await apiClient.post<EmployeeResetPasswordResponse>(
    `${EMPLOYEES}/${id}/reset-password`,
  );
  return data;
}
