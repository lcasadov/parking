import type { VehicleStatus } from './vehicle';

// Fila de la bandeja de validación (change employee-vehicle-self-service, Fase 2): vehículo + datos
// del empleado propietario.
export interface VehicleReviewRow {
  vehicleId: number;
  employee: { id: number; fullName: string; department?: string | null };
  licensePlate: string;
  brand?: string | null;
  model?: string | null;
  color?: string | null;
  status: VehicleStatus;
  rejectionReason?: string | null;
  submittedAt: string;
}

// Página de la bandeja (metadatos estables de la API).
export interface VehicleReviewPageData {
  content: VehicleReviewRow[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export type VehicleHistoryEventType = 'CREATED' | 'EDITED' | 'STATUS_CHANGED' | 'DELETION_REQUESTED';

// Entrada del histórico de un vehículo.
export interface VehicleHistoryEntry {
  id: number;
  eventType: VehicleHistoryEventType;
  actorRole?: string | null;
  fromStatus?: VehicleStatus | null;
  toStatus?: VehicleStatus | null;
  note?: string | null;
  previousData?: {
    licensePlate: string;
    brand?: string | null;
    model?: string | null;
    color?: string | null;
  } | null;
  createdAt: string;
}

export interface VehicleReviewParams {
  // Estados a incluir; vacío/undefined = todos. El tab "Pendientes" combina PENDING + IN_PROGRESS
  // + PENDING_DELETION (todo lo que sigue abierto).
  statuses?: VehicleStatus[];
  page?: number;
  size?: number;
}
