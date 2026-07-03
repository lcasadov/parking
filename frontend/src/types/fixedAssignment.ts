// Tipos derivados del contrato docs/openapi.yaml (componentes FixedAssignment).

// FixedAssignment: schema #/components/schemas/FixedAssignment.
// dayOfWeek es un entero ISO 1 (lunes) .. 7 (domingo).
export interface FixedAssignment {
  id: number;
  parkingSpaceId: number;
  employeeId: number;
  dayOfWeek: number;
  active: boolean;
  createdById: number;
  createdAt: string;
  revokedById?: number | null;
  revokedAt?: string | null;
}

// FixedAssignmentPutRequest: schema #/components/schemas/FixedAssignmentPutRequest.
export interface FixedAssignmentPutRequest {
  parkingSpaceId: number;
  daysOfWeek: number[];
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
