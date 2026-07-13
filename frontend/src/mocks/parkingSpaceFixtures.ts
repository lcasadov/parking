import type { ParkingSpace, PageParkingSpace } from '../types/parkingSpace';

// Plazas de ejemplo para los tests (contrato #/components/schemas/ParkingSpace).
export const spaceP01: ParkingSpace = {
  id: 1,
  number: 1001,
  label: 'P-01',
  floor: 1,
  active: true,
  createdAt: '2026-01-10T09:00:00Z',
};

export const spaceP02: ParkingSpace = {
  id: 2,
  number: 2002,
  label: 'P-02',
  floor: 2,
  active: false,
  createdAt: '2026-01-11T09:00:00Z',
};

// Envuelve una lista de plazas en una PageParkingSpace de una sola pagina.
export function pageOfSpaces(content: ParkingSpace[]): PageParkingSpace {
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

export const defaultParkingSpacePage: PageParkingSpace = pageOfSpaces([spaceP01, spaceP02]);
