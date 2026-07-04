import { apiClient } from './apiClient';
import type {
  AdministrativeReleaseRequest,
  PageRelease,
  Release,
  ReleaseCreateRequest,
  ReleaseListParams,
} from '../types/release';

// Endpoints de Releases segun docs/openapi.yaml. baseURL relativo del apiClient.

const RELEASES = '/releases';

// Serializa los parametros de listado omitiendo los indefinidos.
function buildListParams(params: ReleaseListParams): Record<string, string | number> {
  const query: Record<string, string | number> = {};
  if (params.page !== undefined) {
    query.page = params.page;
  }
  if (params.size !== undefined) {
    query.size = params.size;
  }
  return query;
}

// GET /releases/mine (EMPLOYEE, paginado): solo las liberaciones propias.
export async function listMyReleases(params: ReleaseListParams = {}): Promise<PageRelease> {
  const { data } = await apiClient.get<PageRelease>(`${RELEASES}/mine`, {
    params: buildListParams(params),
  });
  return data;
}

// POST /releases (EMPLOYEE): liberacion voluntaria del recurso fijo propio.
export async function createRelease(body: ReleaseCreateRequest): Promise<Release> {
  const { data } = await apiClient.post<Release>(RELEASES, body);
  return data;
}

// DELETE /releases/{id} (EMPLOYEE): anula una liberacion futura propia.
export async function cancelRelease(id: number): Promise<void> {
  await apiClient.delete(`${RELEASES}/${id}`);
}

// POST /releases/administrative (ADMIN): liberacion administrativa con motivo.
export async function createAdministrativeRelease(
  body: AdministrativeReleaseRequest,
): Promise<Release> {
  const { data } = await apiClient.post<Release>(`${RELEASES}/administrative`, body);
  return data;
}
