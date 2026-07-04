// Tipos derivados del contrato docs/openapi.yaml (componentes Release).

// ReleaseType: schema #/components/schemas/ReleaseType.
export type ReleaseType = 'VOLUNTARY' | 'ADMINISTRATIVE';

// Release: schema #/components/schemas/Release.
export interface Release {
  id: number;
  parkingSpaceId: number;
  employeeId: number;
  releaseDate: string;
  type: ReleaseType;
  reason?: string | null;
  releasedById: number;
  createdAt: string;
}

// ReleaseCreateRequest: schema #/components/schemas/ReleaseCreateRequest.
// parkingSpaceId es opcional (si se omite, el backend resuelve la plaza fija
// del empleado para ese dia).
export interface ReleaseCreateRequest {
  releaseDate: string;
  parkingSpaceId?: number;
}

// AdministrativeReleaseRequest: schema #/components/schemas/AdministrativeReleaseRequest.
export interface AdministrativeReleaseRequest {
  employeeId: number;
  parkingSpaceId: number;
  releaseDate: string;
  reason: string;
}

// PageMeta + PageRelease: schemas #/components/schemas/PageMeta y PageRelease.
export interface PageRelease {
  content: Release[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam y SizeParam.
export interface ReleaseListParams {
  page?: number;
  size?: number;
}
