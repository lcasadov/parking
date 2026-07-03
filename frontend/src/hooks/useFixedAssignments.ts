import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  getEmployeeFixedAssignments,
  listFixedAssignments,
  revokeEmployeeFixedAssignment,
  setEmployeeFixedAssignments,
} from '../api/fixedAssignmentsApi';
import type {
  FixedAssignment,
  FixedAssignmentListParams,
  FixedAssignmentPutRequest,
  PageFixedAssignment,
} from '../types/fixedAssignment';

// Clave raiz de la cache de asignaciones fijas (S1192: sin literales repetidos).
const FIXED_ASSIGNMENTS_KEY = 'fixed-assignments';
const EMPLOYEE_SCOPE = 'employee';

export function fixedAssignmentsQueryKey(
  params: FixedAssignmentListParams,
): (string | FixedAssignmentListParams)[] {
  return [FIXED_ASSIGNMENTS_KEY, params];
}

export function employeeFixedAssignmentsQueryKey(employeeId: number): (string | number)[] {
  return [FIXED_ASSIGNMENTS_KEY, EMPLOYEE_SCOPE, employeeId];
}

export function useFixedAssignmentsQuery(
  params: FixedAssignmentListParams,
): UseQueryResult<PageFixedAssignment> {
  return useQuery({
    queryKey: fixedAssignmentsQueryKey(params),
    queryFn: () => listFixedAssignments(params),
    placeholderData: (previous) => previous,
  });
}

export function useEmployeeFixedAssignmentsQuery(
  employeeId: number | null,
): UseQueryResult<FixedAssignment[]> {
  return useQuery({
    queryKey: employeeFixedAssignmentsQueryKey(employeeId ?? 0),
    queryFn: () => getEmployeeFixedAssignments(employeeId as number),
    enabled: employeeId !== null,
  });
}

// Invalida toda la cache de asignaciones fijas tras una mutacion con exito.
function useInvalidateFixedAssignments(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [FIXED_ASSIGNMENTS_KEY] });
  };
}

export interface SetFixedAssignmentsVars {
  employeeId: number;
  body: FixedAssignmentPutRequest;
}

export function useSetFixedAssignments(): UseMutationResult<
  FixedAssignment[],
  unknown,
  SetFixedAssignmentsVars
> {
  const invalidate = useInvalidateFixedAssignments();
  return useMutation({
    mutationFn: ({ employeeId, body }: SetFixedAssignmentsVars) =>
      setEmployeeFixedAssignments(employeeId, body),
    onSuccess: invalidate,
  });
}

export function useRevokeFixedAssignment(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateFixedAssignments();
  return useMutation({
    mutationFn: (employeeId: number) => revokeEmployeeFixedAssignment(employeeId),
    onSuccess: invalidate,
  });
}
