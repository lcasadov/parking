import type { PageRelease, Release } from '../types/release';
import { toIsoDate, todayIso } from '../utils/requests';

// Liberaciones de ejemplo (contrato #/components/schemas/Release).
// Empleado 2 (employeeUser). Se usan fechas relativas para reflejar el estado
// cancelable (>= hoy) frente a no cancelable (pasada).
function daysFromNow(delta: number): string {
  const date = new Date();
  date.setDate(date.getDate() + delta);
  return toIsoDate(date);
}

// Liberacion voluntaria futura (cancelable).
export const releaseFuture: Release = {
  id: 701,
  parkingSpaceId: 3,
  employeeId: 2,
  releaseDate: daysFromNow(3),
  type: 'VOLUNTARY',
  reason: null,
  releasedById: 2,
  createdAt: '2026-03-01T08:00:00Z',
};

// Liberacion voluntaria de hoy (cancelable: release_date >= hoy).
export const releaseToday: Release = {
  id: 703,
  parkingSpaceId: 3,
  employeeId: 2,
  releaseDate: todayIso(),
  type: 'VOLUNTARY',
  reason: null,
  releasedById: 2,
  createdAt: '2026-03-01T08:30:00Z',
};

// Liberacion administrativa pasada (no cancelable).
export const releasePast: Release = {
  id: 702,
  parkingSpaceId: 3,
  employeeId: 2,
  releaseDate: daysFromNow(-5),
  type: 'ADMINISTRATIVE',
  reason: 'No acudió a la oficina',
  releasedById: 1,
  createdAt: '2026-02-20T08:00:00Z',
};

// Envuelve una lista en una PageRelease de una sola pagina.
export function pageOfReleases(
  content: Release[],
  overrides: Partial<PageRelease> = {},
): PageRelease {
  return {
    content,
    totalElements: content.length,
    totalPages: 1,
    size: 20,
    number: 0,
    first: true,
    last: true,
    ...overrides,
  };
}

// Mis liberaciones por defecto (empleado 2): una futura + una pasada.
export const defaultMyReleasesPage: PageRelease = pageOfReleases([releaseFuture, releasePast]);
