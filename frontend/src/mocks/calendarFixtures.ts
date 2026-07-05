import type {
  AdminWeeklyCalendarResponse,
  AvailabilityResponse,
  CalendarCell,
  CalendarRow,
  MyWeekResponse,
} from '../types/calendar';

// Semana fija de ejemplo (lunes .. viernes) para tests deterministas.
export const FIXTURE_WEEK_START = '2026-05-11';
export const FIXTURE_WEEK_DAYS = [
  '2026-05-11',
  '2026-05-12',
  '2026-05-13',
  '2026-05-14',
  '2026-05-15',
];

// ---- Availability ----
export const defaultAvailability: AvailabilityResponse = {
  date: FIXTURE_WEEK_START,
  availableResources: [
    { parkingSpaceId: 1, label: 'P-01' },
    { parkingSpaceId: 3, label: 'P-03' },
  ],
};

export const emptyAvailability: AvailabilityResponse = {
  date: FIXTURE_WEEK_START,
  availableResources: [],
};

// Disponibilidad de puestos (resourceType=DESK): parkingSpaceId = id del puesto,
// label 'D-xx'. La usa la aprobacion admin de una solicitud de puesto.
export const defaultDeskAvailability: AvailabilityResponse = {
  date: FIXTURE_WEEK_START,
  availableResources: [
    { parkingSpaceId: 1, label: 'D-01' },
    { parkingSpaceId: 2, label: 'D-02' },
  ],
};

// ---- Admin weekly calendar ----
// Fila P-01: cubre los 5 estados de CalendarCellState en orden lun..vie.
const rowP01Cells: CalendarCell[] = [
  {
    date: FIXTURE_WEEK_DAYS[0],
    state: 'ASSIGNED',
    employeeId: 10,
    employeeName: 'Alice Andersson',
    requestId: null,
  },
  {
    date: FIXTURE_WEEK_DAYS[1],
    state: 'RELEASED',
    employeeId: 10,
    employeeName: 'Alice Andersson',
    requestId: null,
  },
  {
    date: FIXTURE_WEEK_DAYS[2],
    state: 'REQUEST_PENDING',
    employeeId: null,
    employeeName: null,
    requestId: 700,
  },
  {
    date: FIXTURE_WEEK_DAYS[3],
    state: 'REQUEST_APPROVED',
    employeeId: 12,
    employeeName: 'Bob Beck',
    requestId: 701,
  },
  {
    date: FIXTURE_WEEK_DAYS[4],
    state: 'FREE',
    employeeId: null,
    employeeName: null,
    requestId: null,
  },
];

// Fila P-02: totalmente libre.
const rowP02Cells: CalendarCell[] = FIXTURE_WEEK_DAYS.map((date) => ({
  date,
  state: 'FREE' as const,
  employeeId: null,
  employeeName: null,
  requestId: null,
}));

export const rowP01: CalendarRow = {
  parkingSpaceId: 1,
  label: 'P-01',
  cells: rowP01Cells,
};

export const rowP02: CalendarRow = {
  parkingSpaceId: 2,
  label: 'P-02',
  cells: rowP02Cells,
};

export function adminCalendarFor(weekStart: string): AdminWeeklyCalendarResponse {
  return {
    weekStart,
    days: FIXTURE_WEEK_DAYS,
    rows: [rowP01, rowP02],
  };
}

export const defaultAdminCalendar: AdminWeeklyCalendarResponse =
  adminCalendarFor(FIXTURE_WEEK_START);

export const emptyAdminCalendar: AdminWeeklyCalendarResponse = {
  weekStart: FIXTURE_WEEK_START,
  days: FIXTURE_WEEK_DAYS,
  rows: [],
};

// ---- My Week (nunca incluye nombres de terceros) ----
export const defaultMyWeek: MyWeekResponse = {
  weekStart: FIXTURE_WEEK_START,
  days: [
    {
      date: FIXTURE_WEEK_DAYS[0],
      state: 'ASSIGNED',
      parkingSpaceLabel: 'P-12',
      requestStatus: null,
    },
    {
      date: FIXTURE_WEEK_DAYS[1],
      state: 'RELEASED',
      parkingSpaceLabel: 'P-12',
      requestStatus: null,
    },
    {
      date: FIXTURE_WEEK_DAYS[2],
      state: 'REQUEST_PENDING',
      parkingSpaceLabel: null,
      requestStatus: 'PENDING',
    },
    {
      date: FIXTURE_WEEK_DAYS[3],
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
    },
    {
      date: FIXTURE_WEEK_DAYS[4],
      state: 'FREE',
      parkingSpaceLabel: null,
      requestStatus: null,
    },
  ],
};
