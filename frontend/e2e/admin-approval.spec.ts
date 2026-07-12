import type { APIRequestContext } from '@playwright/test';
import { test, expect } from './fixtures';
import { EMPLOYEE, apiLogin, cancelEmployeePending, isoDatePlus, loginAsAdmin } from './helpers';

// =====================================================================
// Puerta 3 — e2e: el admin resuelve solicitudes pendientes: APRUEBA una
// (asignando plaza) y RECHAZA otra. Stack real.
// =====================================================================

const API = '/parking-api/api/v1';

// Crea una solicitud PARKING del empleado para `date` vía API (setup).
async function createParkingRequest(request: APIRequestContext, date: string): Promise<void> {
  const res = await request.post(`${API}/requests`, {
    data: { requestedDate: date, resourceType: 'PARKING' },
  });
  expect(res.status(), `crear solicitud PARKING (${date})`).toBe(201);
}

const dateApprove = isoDatePlus(9);
const dateReject = isoDatePlus(10);

// Setup: el empleado deja exactamente dos pendientes (fechas distintas) que el
// admin resolverá. Se limpia primero para idempotencia.
test.beforeEach(async ({ request }) => {
  await apiLogin(request, EMPLOYEE.login, EMPLOYEE.password);
  await cancelEmployeePending(request);
  await createParkingRequest(request, dateApprove);
  await createParkingRequest(request, dateReject);
});

test('should_approve_and_reject_pending_requests_as_admin', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/requests');

  // --- Aprobar la solicitud de dateApprove ---
  const approveRow = page.getByRole('row').filter({ hasText: dateApprove });
  await expect(approveRow).toHaveCount(1);
  await approveRow.getByRole('button', { name: 'Aprobar' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  // Selecciona la primera plaza real (índice 0 es el placeholder).
  await dialog.getByLabel('Plaza disponible').selectOption({ index: 1 });
  await dialog.getByRole('button', { name: 'Aprobar' }).click();
  await expect(dialog).toBeHidden();

  // La fila aprobada desaparece de la bandeja de pendientes.
  await expect(page.getByRole('cell', { name: dateApprove, exact: true })).toHaveCount(0);

  // --- Rechazar la solicitud de dateReject ---
  const rejectRow = page.getByRole('row').filter({ hasText: dateReject });
  await expect(rejectRow).toHaveCount(1);
  await rejectRow.getByRole('button', { name: 'Rechazar' }).click();

  const rejectDialog = page.getByRole('dialog');
  await expect(rejectDialog).toBeVisible();
  await rejectDialog.getByLabel('Motivo del rechazo').selectOption('NO_AVAILABILITY');
  await rejectDialog.getByRole('button', { name: 'Confirmar rechazo' }).click();
  await expect(rejectDialog).toBeHidden();

  // La fila rechazada también desaparece de pendientes.
  await expect(page.getByRole('cell', { name: dateReject, exact: true })).toHaveCount(0);
});
