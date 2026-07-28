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
  // En lista de espera (capability request-waitlist): PENDING nacida sin hueco en
  // modo AUTOMATICO (opt-in `waitlist`) o en MANUAL sin disponibilidad al crearla.
  // La UI NUNCA muestra posicion numerica en la cola (fuera de alcance).
  waitlisted?: boolean;
  // Numero humano del recurso asignado (plaza/puesto). El backend solo lo resuelve
  // para solicitudes APPROVED con recurso; null en el resto -> la UI muestra "—".
  // Es el numero real (p.ej. 3005), NO el `parkingSpaceId` (resource_id interno).
  resourceNumber?: number | null;
  // Planta del recurso (solo PARKING); null para puestos o cuando no se resuelve.
  floor?: number | null;
  deskId?: number | null;
  approvalNote?: string | null;
  rejectionReasonCode?: RejectionReasonCode | null;
  rejectionReason?: string | null;
  resolvedById?: number | null;
  resolvedAt?: string | null;
  createdAt: string;
  // Ultimo reenvio de aviso a los admins (POST /requests/{id}/resend). null/undefined
  // si nunca se ha reenviado; alimenta la ventana de 24h de canResendRequest.
  lastRemindedAt?: string | null;
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
  // Opt-in de lista de espera (capability request-waitlist, default false): solo
  // altera el comportamiento en modo AUTOMATICO sin hueco -> crea PENDING
  // waitlisted=true en vez de 409 NO_AVAILABILITY. Con hueco disponible se ignora.
  waitlist?: boolean;
}

// RequestAdminAssignRequest: schema #/components/schemas/RequestAdminAssignRequest.
// Asignacion puntual del admin (capability admin-punctual-assignment): crea un
// Request que nace APPROVED para el empleado y la fecha indicados. `resourceId`
// es opcional en PARKING (auto-asignacion si se omite) y obligatorio en DESK.
export interface RequestAdminAssignRequest {
  employeeId: number;
  requestedDate: string;
  resourceType?: ResourceType;
  resourceId?: number;
}

// RequestAdminReassignRequest: schema #/components/schemas/RequestAdminReassignRequest.
// Reasigna el recurso de una solicitud APPROVED de fecha futura a otro recurso libre
// (capability admin-resource-reassignment). Avisa por email al empleado afectado.
export interface RequestAdminReassignRequest {
  requestId: number;
  newResourceId: number;
}

// RequestAdminSwapRequest: schema #/components/schemas/RequestAdminSwapRequest.
// Intercambia los recursos de DOS solicitudes APPROVED de la misma fecha y tipo
// (capability admin-resource-reassignment). Avisa por email a ambos empleados.
export interface RequestAdminSwapRequest {
  requestIdA: number;
  requestIdB: number;
}

// RequestSwapResponse: schema #/components/schemas/RequestSwapResponse.
// Devuelve las DOS solicitudes ya intercambiadas para refrescar ambas filas.
export interface RequestSwapResponse {
  requestA: Request;
  requestB: Request;
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
  // Rango de fechas (ISO yyyy-MM-dd) sobre requestedDate, inclusive. Lo usa el
  // selector de mes de "Mis solicitudes".
  from?: string;
  to?: string;
}

// SuggestedParkingSpace: vista previa de la plaza que la auto-asignacion daria a
// un empleado para una fecha, segun su categoria/planta, SIN crear la asignacion.
// Schema #/components/schemas/SuggestedParkingSpaceResponse. `available: false`
// (resto en null) significa "sin plaza libre esa fecha" (no es error, es 200).
export interface SuggestedParkingSpace {
  available: boolean;
  parkingSpaceId: number | null;
  number: number | null;
  floor: number | null;
}

// SuggestedResource: preview EMPLOYEE-safe del recurso (plaza o puesto) que la
// auto-asignación daría al propio empleado para una fecha (incluye la preferencia
// por el fijo propio). `available: false` → resourceLabel null. Schema
// #/components/schemas/SuggestedResource. GET /requests/suggested?date&resourceType.
export interface SuggestedResource {
  available: boolean;
  resourceLabel: string | null;
}
