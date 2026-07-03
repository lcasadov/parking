import { apiClient } from './apiClient';
import type {
  FixedAssignment,
  FixedAssignmentListParams,
  FixedAssignmentPutRequest,
  PageFixedAssignment,
} from '../types/fixedAssignment';

// Endpoints de FixedAssignments segun docs/openapi.yaml. baseURL relativo del apiClient.

const FIXED_ASSIGNMENTS = '/fixed-assignments';

// Serializa los parametros de listado omitiendo los indefinidos.
function buildListParams(params: FixedAssignmentListParams): Record<string, number> {
  const query: Record<string, number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  return query;
}

// GET /fixed-assignments (ADMIN, paginado).
export async function listFixedAssignments(
  params: FixedAssignmentListParams = {},
): Promise<PageFixedAssignment> {
  const { data } = await apiClient.get<PageFixedAssignment>(FIXED_ASSIGNMENTS, {
    params: buildListParams(params),
  });
  return data;
}

// GET /fixed-assignments/employee/{employeeId} (ADMIN cualquiera; EMPLOYEE propias).
export async function getEmployeeFixedAssignments(
  employeeId: number,
): Promise<FixedAssignment[]> {
  const { data } = await apiClient.get<FixedAssignment[]>(
    `${FIXED_ASSIGNMENTS}/employee/${employeeId}`,
  );
  return data;
}

// PUT /fixed-assignments/employee/{employeeId} (ADMIN): reemplaza el conjunto.
export async function setEmployeeFixedAssignments(
  employeeId: number,
  body: FixedAssignmentPutRequest,
): Promise<FixedAssignment[]> {
  const { data } = await apiClient.put<FixedAssignment[]>(
    `${FIXED_ASSIGNMENTS}/employee/${employeeId}`,
    body,
  );
  return data;
}

// DELETE /fixed-assignments/employee/{employeeId} (ADMIN): revocacion logica (204).
export async function revokeEmployeeFixedAssignment(employeeId: number): Promise<void> {
  await apiClient.delete(`${FIXED_ASSIGNMENTS}/employee/${employeeId}`);
}
