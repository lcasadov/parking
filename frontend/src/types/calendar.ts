// Tipos derivados del contrato docs/openapi.yaml (Availability / Calendar).

// CalendarCellState: schema #/components/schemas/CalendarCellState (admin).
export type CalendarCellState =
  | 'ASSIGNED'
  | 'RELEASED'
  | 'REQUEST_PENDING'
  | 'REQUEST_APPROVED'
  | 'FREE';

// MyWeekDayState: schema #/components/schemas/MyWeekDayState (mi-semana).
export type MyWeekDayState = 'ASSIGNED' | 'RELEASED' | 'REQUEST_PENDING' | 'FREE';

// RequestStatus se reutiliza en MyWeekDay.requestStatus.
import type { RequestStatus } from './request';

// AvailabilityItem: schema #/components/schemas/AvailabilityItem.
export interface AvailabilityItem {
  parkingSpaceId: number;
  label: string;
}

// AvailabilityResponse: schema #/components/schemas/AvailabilityResponse.
export interface AvailabilityResponse {
  date: string;
  availableResources: AvailabilityItem[];
}

// CalendarCell: schema #/components/schemas/CalendarCell.
export interface CalendarCell {
  date: string;
  state: CalendarCellState;
  employeeId?: number | null;
  employeeName?: string | null;
  requestId?: number | null;
}

// CalendarRow: schema #/components/schemas/CalendarRow.
export interface CalendarRow {
  parkingSpaceId: number;
  label: string;
  cells: CalendarCell[];
}

// AdminWeeklyCalendarResponse: schema #/components/schemas/AdminWeeklyCalendarResponse.
export interface AdminWeeklyCalendarResponse {
  weekStart: string;
  days: string[];
  rows: CalendarRow[];
}

// MyWeekDay: schema #/components/schemas/MyWeekDay.
export interface MyWeekDay {
  date: string;
  state: MyWeekDayState;
  parkingSpaceLabel?: string | null;
  requestStatus?: RequestStatus | null;
}

// MyWeekResponse: schema #/components/schemas/MyWeekResponse.
export interface MyWeekResponse {
  weekStart: string;
  days: MyWeekDay[];
}
