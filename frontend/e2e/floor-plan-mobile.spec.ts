import { test, expect } from '@playwright/test';
import { EMPLOYEE, apiLogin, cancelEmployeePending, loginAsEmployee } from './helpers';

// =====================================================================
// Puerta 3 — e2e MÓVIL (proyecto `mobile`, device Pixel 5): el empleado
// solicita un puesto desde la lista "Disponibles para solicitar" del plano,
// que es la vía real de solicitud en móvil. Stack real.
// =====================================================================

test.beforeEach(async ({ request }) => {
  await apiLogin(request, EMPLOYEE.login, EMPLOYEE.password);
  await cancelEmployeePending(request);
});

test('should_request_a_desk_from_the_mobile_list_when_employee_taps_solicitar', async ({ page }) => {
  await loginAsEmployee(page);
  await page.goto('/employee/floor-plan');

  // La lista móvil "Disponibles para solicitar" es la vía de solicitud en móvil.
  const mobileList = page.getByRole('region', { name: 'Disponibles para solicitar' });
  await expect(mobileList).toBeVisible();

  // Solicita el primer puesto libre.
  await mobileList.getByRole('button', { name: 'Solicitar' }).first().click();

  // Feedback de éxito del plano.
  await expect(
    page.getByText('Solicitud creada. Queda pendiente de aprobación.'),
  ).toBeVisible();
});
