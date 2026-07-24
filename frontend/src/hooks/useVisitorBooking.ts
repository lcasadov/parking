import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query';
import { createVisitorReservation } from '../api/visitorReservationsApi';
import { getStatus } from '../api/apiError';
import type { ResourceType } from '../types/request';
import type { BookingEntry } from './useReservationBooking';
import type { BookingOutcome } from '../components/wizard/wizardTypes';

// Claves raíz de caché afectadas por una reserva de visitante (ocupa el recurso su
// fecha: cuenta en disponibilidad, calendario, ocupación y plano).
const AFFECTED_KEYS = ['visitor-reservations', 'calendar', 'occupancy', 'floor-plan'] as const;

const HTTP_CONFLICT = 409;

// Motivo i18n del fallo por fecha: 409 = recurso ya ocupado esa fecha; resto genérico.
function reasonKeyForError(error: unknown): string {
  return getStatus(error) === HTTP_CONFLICT
    ? 'wizard.result.reasonNoAvailability'
    : 'wizard.result.reasonGeneric';
}

export interface VisitorBookingVars {
  visitorId: number;
  resourceType: ResourceType;
  // Cada fecha con su recurso CONCRETO (los visitantes no usan auto-asignación).
  entries: BookingEntry[];
}

// Reserva de VISITANTE por lote: crea una reserva por cada fecha vía
// POST /visitor-reservations con {resourceType, resourceId}. No transaccional
// (allSettled): reporta el resultado por fecha. NO se envían emails a visitantes.
export function useVisitorBooking(): UseMutationResult<BookingOutcome[], unknown, VisitorBookingVars> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (vars: VisitorBookingVars): Promise<BookingOutcome[]> => {
      const settled = await Promise.allSettled(
        vars.entries.map((entry) =>
          createVisitorReservation({
            visitorId: vars.visitorId,
            resourceType: vars.resourceType,
            resourceId: entry.resourceId as number,
            reservationDate: entry.date,
          }),
        ),
      );
      return settled.map((outcome, index) => {
        const date = vars.entries[index].date;
        if (outcome.status === 'fulfilled') {
          return { date, ok: true };
        }
        return { date, ok: false, reasonKey: reasonKeyForError(outcome.reason) };
      });
    },
    onSettled: () => {
      AFFECTED_KEYS.forEach((key) => {
        void queryClient.invalidateQueries({ queryKey: [key] });
      });
    },
  });
}
