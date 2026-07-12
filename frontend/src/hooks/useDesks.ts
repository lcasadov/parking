import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import { createDesk, listDesks, setDeskActivation, updateDesk } from '../api/desksApi';
import type { Desk, DeskCreate, DeskListParams, PageDesk } from '../types/desk';

// Clave raíz de la caché de puestos (S1192: sin literales repetidos).
const DESKS_KEY = 'desks';

export function desksQueryKey(params: DeskListParams): (string | DeskListParams)[] {
  return [DESKS_KEY, params];
}

export function useDesksQuery(params: DeskListParams): UseQueryResult<PageDesk> {
  return useQuery({
    queryKey: desksQueryKey(params),
    queryFn: () => listDesks(params),
    placeholderData: (previous) => previous,
  });
}

// Invalida toda la caché de puestos tras una mutación con éxito.
function useInvalidateDesks(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [DESKS_KEY] });
  };
}

export function useCreateDesk(): UseMutationResult<Desk, unknown, DeskCreate> {
  const invalidate = useInvalidateDesks();
  return useMutation({
    mutationFn: (body: DeskCreate) => createDesk(body),
    onSuccess: invalidate,
  });
}

export interface UpdateDeskVars {
  id: number;
  body: DeskCreate;
}

export function useUpdateDesk(): UseMutationResult<Desk, unknown, UpdateDeskVars> {
  const invalidate = useInvalidateDesks();
  return useMutation({
    mutationFn: ({ id, body }: UpdateDeskVars) => updateDesk(id, body),
    onSuccess: invalidate,
  });
}

export interface SetDeskActivationVars {
  id: number;
  active: boolean;
}

export function useSetDeskActivation(): UseMutationResult<Desk, unknown, SetDeskActivationVars> {
  const invalidate = useInvalidateDesks();
  return useMutation({
    mutationFn: ({ id, active }: SetDeskActivationVars) => setDeskActivation(id, active),
    onSuccess: invalidate,
  });
}
