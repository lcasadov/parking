// Tipos derivados del contrato docs/openapi.yaml (componentes Release).

import type { ResourceType } from './request';

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
// del empleado para ese dia). resourceType es opcional (default PARKING en el
// backend); se envia DESK al liberar un puesto fijo (bug: antes se omitia
// siempre y el backend asumia PARKING incluso para puestos).
export interface ReleaseCreateRequest {
  releaseDate: string;
  parkingSpaceId?: number;
  resourceType?: ResourceType;
}

// AdministrativeReleaseRequest: schema #/components/schemas/AdministrativeReleaseRequest.
// resourceType es opcional (por defecto PARKING en el backend); se envia DESK al
// liberar un puesto fijo.
export interface AdministrativeReleaseRequest {
  employeeId: number;
  parkingSpaceId: number;
  releaseDate: string;
  reason: string;
  resourceType?: ResourceType;
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
