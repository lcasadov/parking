// Tipos derivados del contrato del backend (Occupancy — Liberar por fecha).

import type { ResourceType } from './request';

// OccupancyOrigin: origen por el que un recurso queda ocupado una fecha.
export type OccupancyOrigin = 'FIXED_ASSIGNMENT' | 'REQUEST_APPROVED';

// OccupancyItem: recurso ocupado una fecha con su titular y origen.
export interface OccupancyItem {
  resourceType: ResourceType;
  resourceId: number;
  resourceNumber: number;
  floor?: number | null;
  employeeId: number;
  employeeName: string;
  origin: OccupancyOrigin;
  requestId?: number | null;
}

// OccupancyResponse: recursos ocupados para una fecha (los libres se omiten).
export interface OccupancyResponse {
  date: string;
  occupiedResources: OccupancyItem[];
}
