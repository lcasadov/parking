import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  createEmployee,
  deactivateEmployee,
  listEmployees,
  reactivateEmployee,
  resetEmployeePassword,
  updateEmployee,
} from '../api/employeesApi';
import type {
  Employee,
  EmployeeCreate,
  EmployeeListParams,
  EmployeeResetPasswordResponse,
  EmployeeUpdate,
  PageEmployee,
} from '../types/employee';

// Clave raiz de la cache de empleados (S1192: sin literales repetidos).
const EMPLOYEES_KEY = 'employees';

export function employeesQueryKey(params: EmployeeListParams): (string | EmployeeListParams)[] {
  return [EMPLOYEES_KEY, params];
}

export function useEmployeesQuery(params: EmployeeListParams): UseQueryResult<PageEmployee> {
  return useQuery({
    queryKey: employeesQueryKey(params),
    queryFn: () => listEmployees(params),
    placeholderData: (previous) => previous,
  });
}

// Invalida toda la cache de empleados tras una mutacion con exito.
function useInvalidateEmployees(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [EMPLOYEES_KEY] });
  };
}

export function useCreateEmployee(): UseMutationResult<Employee, unknown, EmployeeCreate> {
  const invalidate = useInvalidateEmployees();
  return useMutation({
    mutationFn: (body: EmployeeCreate) => createEmployee(body),
    onSuccess: invalidate,
  });
}

export interface UpdateEmployeeVars {
  id: number;
  body: EmployeeUpdate;
}

export function useUpdateEmployee(): UseMutationResult<Employee, unknown, UpdateEmployeeVars> {
  const invalidate = useInvalidateEmployees();
  return useMutation({
    mutationFn: ({ id, body }: UpdateEmployeeVars) => updateEmployee(id, body),
    onSuccess: invalidate,
  });
}

export function useDeactivateEmployee(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateEmployees();
  return useMutation({
    mutationFn: (id: number) => deactivateEmployee(id),
    onSuccess: invalidate,
  });
}

export function useReactivateEmployee(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateEmployees();
  return useMutation({
    mutationFn: (id: number) => reactivateEmployee(id),
    onSuccess: invalidate,
  });
}

export function useResetEmployeePassword(): UseMutationResult<
  EmployeeResetPasswordResponse,
  unknown,
  number
> {
  return useMutation({
    mutationFn: (id: number) => resetEmployeePassword(id),
  });
}
