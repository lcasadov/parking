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
const MINE_SCOPE = 'mine';

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

// Invalida toda la cache de liberaciones tras una mutacion con exito.
function useInvalidateReleases(): () => void {
  const queryClient = useQueryClient();
  return () => {
    void queryClient.invalidateQueries({ queryKey: [RELEASES_KEY] });
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
