// Tipos derivados del contrato docs/openapi.yaml (componentes Request).

// RequestStatus: schema #/components/schemas/RequestStatus.
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

// ResourceType: schema #/components/schemas/ResourceType. Discrimina el recurso
// reservable de la solicitud (plaza de parking o puesto de oficina).
export type ResourceType = 'PARKING' | 'DESK';

// RejectionReasonCode: schema #/components/schemas/RejectionReasonCode.
export type RejectionReasonCode = 'NO_AVAILABILITY' | 'OUTSIDE_POLICY' | 'OTHER';

// Request: schema #/components/schemas/Request.
export interface Request {
  id: number;
  employeeId: number;
  requestedDate: string;
  status: RequestStatus;
  resourceType?: ResourceType;
  parkingSpaceId?: number | null;
  deskId?: number | null;
  approvalNote?: string | null;
  rejectionReasonCode?: RejectionReasonCode | null;
  rejectionReason?: string | null;
  resolvedById?: number | null;
  resolvedAt?: string | null;
  createdAt: string;
}

// RequestCreateRequest: schema #/components/schemas/RequestCreateRequest.
// La solicitud unificada genera un Request independiente por cada recurso elegido
// (PARKING y/o DESK). `resourceType` opcional: si se omite el backend asume PARKING
// (retrocompatibilidad con el flujo de solo plaza).
export interface RequestCreateRequest {
  requestedDate: string;
  resourceType?: ResourceType;
  // Puesto concreto elegido en el plano (solo DESK). En modo AUTOMATIC el backend
  // auto-aprueba ese puesto; en MANUAL lo ignora. Opcional: sin puesto elegido la
  // solicitud se envia sin `resourceId` (retrocompatibilidad con el flujo actual).
  resourceId?: number;
}

// RequestApproveRequest: schema #/components/schemas/RequestApproveRequest.
export interface RequestApproveRequest {
  parkingSpaceId: number;
  approvalNote?: string;
}

// RequestRejectRequest: schema #/components/schemas/RequestRejectRequest.
export interface RequestRejectRequest {
  reasonCode: RejectionReasonCode;
  rejectionReason?: string;
}

// PageMeta + PageRequest: schemas #/components/schemas/PageMeta y PageRequest.
export interface PageRequest {
  content: Request[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam, SizeParam y filtro status opcional.
export interface RequestListParams {
  page?: number;
  size?: number;
  status?: RequestStatus;
}
