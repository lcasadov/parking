// Tipos derivados del contrato docs/openapi.yaml (componentes FixedAssignment).

import type { ResourceType } from './request';

// FixedAssignment: schema #/components/schemas/FixedAssignment.
// dayOfWeek es un entero ISO 1 (lunes) .. 7 (domingo). `parkingSpaceId` transporta
// el resource_id generico (plaza o puesto) segun `resourceType` (default PARKING).
export interface FixedAssignment {
  id: number;
  parkingSpaceId: number;
  employeeId: number;
  dayOfWeek: number;
  resourceType?: ResourceType;
  active: boolean;
  createdById: number;
  createdAt: string;
  revokedById?: number | null;
  revokedAt?: string | null;
}

// FixedAssignmentPutRequest: schema #/components/schemas/FixedAssignmentPutRequest.
// `resourceType` opcional: si se omite el backend asume PARKING (retrocompatible).
export interface FixedAssignmentPutRequest {
  parkingSpaceId: number;
  daysOfWeek: number[];
  resourceType?: ResourceType;
}

// PageMeta + PageFixedAssignment: schemas #/components/schemas/PageMeta y PageFixedAssignment.
export interface PageFixedAssignment {
  content: FixedAssignment[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam y SizeParam.
export interface FixedAssignmentListParams {
  page?: number;
  size?: number;
}
