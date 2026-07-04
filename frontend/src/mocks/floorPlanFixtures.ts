import type { FloorPlanDesk, FloorPlanResponse } from '../types/floorPlan';

// Puestos de ejemplo para el plano (contrato floor-plan). Cubren cada estado y
// ambas categorías, más un puesto sin coordenadas (recién creado, sin posición).
export const floorDeskFree: FloorPlanDesk = {
  deskId: 1,
  deskNumber: 1,
  category: 'STANDARD',
  coordX: 20,
  coordY: 30,
  state: 'FREE',
};

export const floorDeskAssigned: FloorPlanDesk = {
  deskId: 2,
  deskNumber: 2,
  category: 'STANDARD',
  coordX: 40,
  coordY: 50,
  state: 'ASSIGNED',
};

export const floorDeskMine: FloorPlanDesk = {
  deskId: 3,
  deskNumber: 3,
  category: 'STANDARD',
  coordX: 60,
  coordY: 70,
  state: 'MINE',
};

export const floorDeskRequested: FloorPlanDesk = {
  deskId: 4,
  deskNumber: 4,
  category: 'STANDARD',
  coordX: 80,
  coordY: 20,
  state: 'REQUESTED',
};

export const floorDeskReleased: FloorPlanDesk = {
  deskId: 5,
  deskNumber: 5,
  category: 'STANDARD',
  coordX: 25,
  coordY: 80,
  state: 'RELEASED',
};

export const floorDeskExecutive: FloorPlanDesk = {
  deskId: 6,
  deskNumber: 6,
  category: 'EXECUTIVE',
  coordX: 55,
  coordY: 35,
  state: 'FREE',
};

// Puesto sin posición (coordX/coordY null): no se pinta sobre el plano.
export const floorDeskUnplaced: FloorPlanDesk = {
  deskId: 7,
  deskNumber: 7,
  category: 'STANDARD',
  coordX: null,
  coordY: null,
  state: 'FREE',
};

export function floorPlanOf(desks: FloorPlanDesk[]): FloorPlanResponse {
  return { desks };
}

export const defaultFloorPlan: FloorPlanResponse = floorPlanOf([
  floorDeskFree,
  floorDeskAssigned,
  floorDeskMine,
  floorDeskRequested,
  floorDeskReleased,
  floorDeskExecutive,
  floorDeskUnplaced,
]);
