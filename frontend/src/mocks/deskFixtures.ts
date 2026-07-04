import type { Desk, PageDesk } from '../types/desk';

// Puestos de ejemplo para los tests (contrato #/components/schemas/Desk).
export const deskStandard: Desk = {
  id: 1,
  number: 1,
  category: 'STANDARD',
  coordX: 50,
  coordY: 50,
  active: true,
  createdAt: '2026-01-10T09:00:00Z',
};

export const deskExecutive: Desk = {
  id: 2,
  number: 2,
  category: 'EXECUTIVE',
  coordX: 60,
  coordY: 40,
  active: true,
  createdAt: '2026-01-11T09:00:00Z',
};

export const deskInactive: Desk = {
  id: 3,
  number: 3,
  category: 'STANDARD',
  coordX: 30,
  coordY: 70,
  active: false,
  createdAt: '2026-01-12T09:00:00Z',
};

// Envuelve una lista de puestos en una PageDesk de una sola página.
export function pageOfDesks(content: Desk[]): PageDesk {
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

export const defaultDeskPage: PageDesk = pageOfDesks([deskStandard, deskExecutive]);
