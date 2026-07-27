import {
  useMutation,
  useQuery,
  useQueryClient,
  type UseMutationResult,
  type UseQueryResult,
} from '@tanstack/react-query';
import {
  cancelRelease,
  createAdministrativeRelease,
  createRelease,
  listMyAdministrativeReleases,
  listMyReleases,
} from '../api/releasesApi';
import type {
  AdministrativeReleaseRequest,
  PageRelease,
  Release,
  ReleaseCreateRequest,
  ReleaseListParams,
} from '../types/release';

// Claves raiz de cache (S1192: sin literales repetidos).
const RELEASES_KEY = 'releases';
// Otras raices afectadas por una liberacion: el calendario (Mi Semana +
// disponibilidad, ambos bajo 'calendar'), las solicitudes y la ocupacion admin.
const CALENDAR_KEY = 'calendar';
const REQUESTS_KEY = 'requests';
const OCCUPANCY_KEY = 'occupancy';
const MINE_SCOPE = 'mine';
const ADMINISTRATIVE_MINE_SCOPE = 'administrative-mine';

export function myReleasesQueryKey(
  params: ReleaseListParams,
): (string | ReleaseListParams)[] {
  return [RELEASES_KEY, MINE_SCOPE, params];
}

export function useMyReleasesQuery(params: ReleaseListParams): UseQueryResult<PageRelease> {
  return useQuery({
    queryKey: myReleasesQueryKey(params),
    queryFn: () => listMyReleases(params),
    placeholderData: (previous) => previous,
  });
}

export function myAdministrativeReleasesQueryKey(
  params: ReleaseListParams,
): (string | ReleaseListParams)[] {
  return [RELEASES_KEY, ADMINISTRATIVE_MINE_SCOPE, params];
}

// Historial de las liberaciones administrativas creadas por el propio actor
// (ADMIN/AGENCIA). Consumido por el destino "Liberar" para orientarse.
export function useMyAdministrativeReleasesQuery(
  params: ReleaseListParams,
): UseQueryResult<PageRelease> {
  return useQuery({
    queryKey: myAdministrativeReleasesQueryKey(params),
    queryFn: () => listMyAdministrativeReleases(params),
    placeholderData: (previous) => previous,
  });
}

// Invalida la cache afectada por una liberacion (crear/cancelar). Ademas de las
// liberaciones, liberar/reactivar un recurso cambia el estado del dia en el
// calendario (Mi Semana), su disponibilidad, las solicitudes y la ocupacion
// admin. Sin esto el listado quedaba obsoleto (seguia mostrando ASSIGNED) y no
// se podia volver a reservar el recurso recien liberado.
function useInvalidateReleases(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [RELEASES_KEY] });
    void queryClient.invalidateQueries({ queryKey: [CALENDAR_KEY] });
    void queryClient.invalidateQueries({ queryKey: [REQUESTS_KEY] });
    void queryClient.invalidateQueries({ queryKey: [OCCUPANCY_KEY] });
  };
}

export function useCreateRelease(): UseMutationResult<Release, unknown, ReleaseCreateRequest> {
  const invalidate = useInvalidateReleases();
  return useMutation({
    mutationFn: (body: ReleaseCreateRequest) => createRelease(body),
    onSuccess: invalidate,
  });
}

export function useCancelRelease(): UseMutationResult<void, unknown, number> {
  const invalidate = useInvalidateReleases();
  return useMutation({
    mutationFn: (id: number) => cancelRelease(id),
    onSuccess: invalidate,
  });
}

export function useCreateAdministrativeRelease(): UseMutationResult<
  Release,
  unknown,
  AdministrativeReleaseRequest
> {
  const invalidate = useInvalidateReleases();
  return useMutation({
    mutationFn: (body: AdministrativeReleaseRequest) => createAdministrativeRelease(body),
    onSuccess: invalidate,
  });
}
