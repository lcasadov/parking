import { useMutation, useQueryClient, type UseMutationResult } from '@tanstack/react-query';
import { adminAssignRequest } from '../api/requestsApi';
import { getApiError, getStatus } from '../api/apiError';
import type { ResourceType } from '../types/request';
import type { BookingOutcome } from '../components/wizard/wizardTypes';

// Claves raíz de caché afectadas por una reserva (nace APPROVED y ocupa recurso).
const AFFECTED_KEYS = ['requests', 'calendar', 'occupancy', 'floor-plan'] as const;

const HTTP_CONFLICT = 409;
const NO_AVAILABILITY = 'NO_AVAILABILITY';

// Motivo i18n de un fallo por fecha: el 409 distingue plaza no disponible
// (auto-asignación) de reserva duplicada; el resto cae en genérico.
function reasonKeyForError(error: unknown): string {
  if (getStatus(error) === HTTP_CONFLICT) {
    return getApiError(error)?.error === NO_AVAILABILITY
      ? 'wizard.result.reasonNoAvailability'
      : 'wizard.result.reasonDuplicate';
  }
  return 'wizard.result.reasonGeneric';
}

// Una fecha a reservar con su recurso concreto opcional (omitido = auto-asignación
// de plaza por categoría). Permite recurso distinto por día (modo PER_DAY).
export interface BookingEntry {
  date: string;
  resourceId?: number;
}

export interface ReservationBookingVars {
  employeeId: number;
  resourceType: ResourceType;
  entries: BookingEntry[];
}

// Reserva ADMIN por lote: recorre las fechas creando un Request APPROVED por cada
// una vía POST /requests/admin (el backend envía el email al empleado). No es
// transaccional: usa allSettled para tolerar fallos parciales y reportar el
// resultado POR FECHA (creadas / no disponibles), en vez de abortar al primero.
export function useReservationBooking(): UseMutationResult<
  BookingOutcome[],
  unknown,
  ReservationBookingVars
> {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (vars: ReservationBookingVars): Promise<BookingOutcome[]> => {
      const settled = await Promise.allSettled(
        vars.entries.map((entry) =>
          adminAssignRequest({
            employeeId: vars.employeeId,
            requestedDate: entry.date,
            resourceType: vars.resourceType,
            resourceId: entry.resourceId,
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
