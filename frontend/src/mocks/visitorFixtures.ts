import type {
  PageVisitor,
  PageVisitorReservation,
  Visitor,
  VisitorReservation,
} from '../types/visitor';
import { todayIso } from '../utils/requests';

// Visitantes de ejemplo para los tests (contrato #/components/schemas/Visitor).
export const visitorCarla: Visitor = {
  id: 30,
  firstName: 'Carla',
  lastName: 'Cortes',
  nationalId: '12345678Z',
  licensePlate: '4567DEF',
  company: 'Aleatica Partners',
  usualReason: 'Reunion comercial',
  createdById: 1,
  createdAt: '2026-02-10T09:00:00Z',
};

export const visitorDiego: Visitor = {
  id: 31,
  firstName: 'Diego',
  lastName: 'Duarte',
  nationalId: '87654321X',
  licensePlate: null,
  company: null,
  usualReason: null,
  createdById: 1,
  createdAt: '2026-02-11T09:00:00Z',
};

// Envuelve una lista de visitantes en una PageVisitor de una sola pagina.
export function pageOfVisitors(content: Visitor[]): PageVisitor {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

export const defaultVisitorPage: PageVisitor = pageOfVisitors([visitorCarla, visitorDiego]);

// Fecha pasada fija (anterior a hoy) para probar el bloqueo de anulacion.
const PAST_DATE = '2020-01-01';

// Reservas de ejemplo (contrato #/components/schemas/VisitorReservation).
export const reservationFuture: VisitorReservation = {
  id: 40,
  visitorId: 30,
  resourceType: 'PARKING',
  resourceId: 1,
  reservationDate: todayIso(),
  notes: 'Aparcar cerca de recepcion',
  createdById: 1,
  createdAt: '2026-02-12T09:00:00Z',
};

export const reservationPast: VisitorReservation = {
  id: 41,
  visitorId: 31,
  resourceType: 'PARKING',
  resourceId: 2,
  reservationDate: PAST_DATE,
  notes: null,
  createdById: 1,
  createdAt: '2020-01-01T09:00:00Z',
};

// Envuelve una lista de reservas en una PageVisitorReservation de una sola pagina.
export function pageOfReservations(content: VisitorReservation[]): PageVisitorReservation {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
  };
}

export const defaultVisitorReservationPage: PageVisitorReservation = pageOfReservations([
  reservationFuture,
  reservationPast,
]);
