import { test, expect } from './fixtures';
import {
  EMPLOYEE,
  apiLogin,
  cancelEmployeePending,
  isoDatePlus,
  loginAsEmployee,
} from './helpers';

// =====================================================================
// Puerta 3 — e2e: el empleado solicita PLAZA y PUESTO para la misma fecha
// y ve ambas como pendientes. Stack real (SPA + backend + SQL Server).
// =====================================================================

// La BD dev es compartida: se limpian las solicitudes pendientes del empleado
// antes de cada test para que el flujo sea idempotente entre ejecuciones.
test.beforeEach(async ({ request }) => {
  await apiLogin(request, EMPLOYEE.login, EMPLOYEE.password);
  await cancelEmployeePending(request);
});

test('should_create_parking_and_desk_requests_when_employee_submits_both_for_same_date', async ({
  page,
}) => {
  const date = isoDatePlus(7);

  await loginAsEmployee(page);
  await page.goto('/employee/requests');

  await page.getByRole('button', { name: 'Nueva solicitud' }).click();
  await page.getByLabel('Fecha de la solicitud').fill(date);
  // "Plaza de parking" viene marcada por defecto; se añade "Puesto de oficina".
  await page.getByLabel('Puesto de oficina').check();
  await page.getByRole('button', { name: 'Enviar solicitud' }).click();

  // Dos solicitudes (plaza + puesto) para la misma fecha, ambas PENDING.
  const dateCells = page.getByRole('cell', { name: date, exact: true });
  await expect(dateCells).toHaveCount(2);
  await expect(page.getByText('Pendiente')).toHaveCount(2);
});
