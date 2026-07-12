import type { APIRequestContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { ADMIN, EMPLOYEE, apiLogin, loginAsEmployee } from './helpers';

// =====================================================================
// Puerta 3 — e2e: liberación voluntaria. Setup por API (admin asigna una plaza
// fija Lun-Vie al empleado); luego el empleado libera un día desde "Mi Semana".
// El reset del fixture limpia fixed_assignments y releases entre tests.
// =====================================================================

const API = '/parking-api/api/v1';

// Próximo día laborable (Lun-Vie) en ISO; coincide con daysOfWeek [1..5]
// (1=Lunes) de la asignación fija.
function nextWeekdayIso(): string {
  const d = new Date();
  do {
    d.setDate(d.getDate() + 1);
  } while (d.getDay() === 0 || d.getDay() === 6);
  return d.toISOString().slice(0, 10);
}

// Setup: crea (por API) una asignación fija de PARKING Lun-Vie para el empleado.
async function setupFixedAssignment(request: APIRequestContext): Promise<void> {
  await apiLogin(request, EMPLOYEE.login, EMPLOYEE.password);
  const me = await (await request.get(`${API}/auth/me`)).json();
  const employeeId = me.employeeId;

  await apiLogin(request, ADMIN.login, ADMIN.password);
  const spacesBody = await (await request.get(`${API}/parking-spaces?active=true&size=1`)).json();
  const spaces = spacesBody.content ?? spacesBody;
  const parkingSpaceId = spaces[0].id;

  const put = await request.put(`${API}/fixed-assignments/employee/${employeeId}`, {
    data: { parkingSpaceId, daysOfWeek: [1, 2, 3, 4, 5] },
  });
  expect(put.ok(), `crear asignación fija (${put.status()})`).toBeTruthy();
}

test('should_release_a_fixed_parking_day_from_my_week', async ({ page, request }) => {
  await setupFixedAssignment(request);
  const releaseDate = nextWeekdayIso();

  await loginAsEmployee(page);
  await page.goto('/employee/my-week');

  // El botón "Liberar" está habilitado gracias a la asignación fija.
  await page.getByRole('button', { name: 'Liberar' }).first().click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByLabel('Fecha a liberar').fill(releaseDate);
  await dialog.getByRole('button', { name: 'Liberar' }).click();
  await expect(dialog).toBeHidden();

  // La liberación aparece en "Mis liberaciones".
  await page.goto('/employee/releases');
  await expect(page.getByRole('cell', { name: releaseDate, exact: true })).toHaveCount(1);
});
