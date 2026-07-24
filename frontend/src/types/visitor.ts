// Tipos derivados del contrato docs/openapi.yaml (componentes Visitor y
// VisitorReservation). Los nombres de campo replican exactamente el contrato:
// firstName, lastName, nationalId, licensePlate, company, usualReason /
// visitorId, parkingSpaceId, reservationDate, notes.
import type { ResourceType } from './request';

// Visitor: schema #/components/schemas/Visitor.
export interface Visitor {
  id: number;
  firstName: string;
  lastName: string;
  nationalId: string;
  licensePlate?: string | null;
  company?: string | null;
  usualReason?: string | null;
  createdById: number;
  createdAt: string;
}

// VisitorCreateRequest: schema #/components/schemas/VisitorCreateRequest
// (usado tambien por PUT /visitors/{id}). Requeridos: firstName, lastName, nationalId.
export interface VisitorCreateRequest {
  firstName: string;
  lastName: string;
  nationalId: string;
  licensePlate?: string;
  company?: string;
  usualReason?: string;
}

// PageMeta + PageVisitor: schemas #/components/schemas/PageMeta y PageVisitor.
export interface PageVisitor {
  content: Visitor[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado: PageParam, SizeParam y QParam (nationalId/nombre/matricula).
export interface VisitorListParams {
  page?: number;
  size?: number;
  q?: string;
}

// VisitorReservation: schema #/components/schemas/VisitorReservation. El recurso es
// genérico (plaza o puesto): resourceType + resourceId (migración backend V25).
export interface VisitorReservation {
  id: number;
  visitorId: number;
  resourceType: ResourceType;
  resourceId: number;
  reservationDate: string;
  notes?: string | null;
  createdById: number;
  createdAt: string;
}

// VisitorReservationCreateRequest: schema #/components/schemas/VisitorReservationCreateRequest.
// Requeridos: visitorId, resourceType, resourceId, reservationDate.
export interface VisitorReservationCreateRequest {
  visitorId: number;
  resourceType: ResourceType;
  resourceId: number;
  reservationDate: string;
  notes?: string;
}

// PageMeta + PageVisitorReservation: schemas #/components/schemas/PageMeta y
// PageVisitorReservation.
export interface PageVisitorReservation {
  content: VisitorReservation[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Parametros de listado de reservas: PageParam, SizeParam y filtros date/parkingSpaceId.
export interface VisitorReservationListParams {
  page?: number;
  size?: number;
  date?: string;
  parkingSpaceId?: number;
}
