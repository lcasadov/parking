import type { FixedAssignment, PageFixedAssignment } from '../types/fixedAssignment';

// Asignaciones fijas de ejemplo (contrato #/components/schemas/FixedAssignment).
// Empleado 10 (Alice) tiene la plaza 1 los lunes, martes y miercoles.
function assignmentFor(id: number, employeeId: number, spaceId: number, day: number): FixedAssignment {
  return {
    id,
    parkingSpaceId: spaceId,
    employeeId,
    dayOfWeek: day,
    active: true,
    createdById: 1,
    createdAt: '2026-02-01T09:00:00Z',
    revokedById: null,
    revokedAt: null,
  };
}

export const aliceMonday = assignmentFor(101, 10, 1, 1);
export const aliceTuesday = assignmentFor(102, 10, 1, 2);
export const aliceWednesday = assignmentFor(103, 10, 1, 3);
export const bobThursday = assignmentFor(104, 11, 2, 4);

export const aliceAssignments: FixedAssignment[] = [aliceMonday, aliceTuesday, aliceWednesday];

// Envuelve una lista en una PageFixedAssignment de una sola pagina.
export function pageOfFixedAssignments(
  content: FixedAssignment[],
  overrides: Partial<PageFixedAssignment> = {},
): PageFixedAssignment {
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

export const defaultFixedAssignmentPage: PageFixedAssignment = pageOfFixedAssignments([
  aliceMonday,
  aliceTuesday,
  aliceWednesday,
  bobThursday,
]);
