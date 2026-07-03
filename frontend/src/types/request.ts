// Tipos derivados del contrato docs/openapi.yaml (componentes Request).

// RequestStatus: schema #/components/schemas/RequestStatus.
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

// RejectionReasonCode: schema #/components/schemas/RejectionReasonCode.
export type RejectionReasonCode = 'NO_AVAILABILITY' | 'OUTSIDE_POLICY' | 'OTHER';

// Request: schema #/components/schemas/Request.
export interface Request {
  id: number;
  employeeId: number;
  requestedDate: string;
  status: RequestStatus;
  parkingSpaceId?: number | null;
  approvalNote?: string | null;
  rejectionReasonCode?: RejectionReasonCode | null;
  rejectionReason?: string | null;
  resolvedById?: number | null;
  resolvedAt?: string | null;
  createdAt: string;
}

// RequestCreateRequest: schema #/components/schemas/RequestCreateRequest.
export interface RequestCreateRequest {
  requestedDate: string;
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
