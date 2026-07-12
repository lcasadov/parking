import { test, expect } from './fixtures';
import { loginUI } from './helpers';
import { seedMustChangeEmployee } from './reset-db';

// =====================================================================
// Puerta 3 — e2e: cambio de contraseña obligatorio (flujo crítico de auth).
// Un empleado con passwordMustChange=true es forzado por el guard a
// /change-password antes de acceder a cualquier área. Se usa un empleado
// dedicado (no los seeds) creado en el setup; el reset del fixture lo elimina.
// =====================================================================

const LABELS = {
  current: 'Contraseña actual',
  next: 'Nueva contraseña',
  confirm: 'Confirmar nueva contraseña',
  submit: 'Cambiar contraseña',
};

const NEW_PASSWORD = 'Nueva-Clave1'; // ≥10, may+min+dígito+símbolo, != login/email

test('should_force_password_change_and_land_on_home_when_must_change', async ({ page }) => {
  const { login, password } = seedMustChangeEmployee();

  await loginUI(page, login, password);
  // El guard rebota a /change-password al detectar passwordMustChange=true.
  await expect(page).toHaveURL(/\/change-password$/);

  // exact: evita casar el botón "Mostrar contraseña" y desambigua
  // "Nueva contraseña" de "Confirmar nueva contraseña".
  await page.getByLabel(LABELS.current, { exact: true }).fill(password);
  await page.getByLabel(LABELS.next, { exact: true }).fill(NEW_PASSWORD);
  await page.getByLabel(LABELS.confirm, { exact: true }).fill(NEW_PASSWORD);
  await page.getByRole('button', { name: LABELS.submit }).click();

  // Tras el cambio, la app navega al home del rol (EMPLOYEE → /employee).
  await expect(page).toHaveURL(/\/employee(\/|$)/);
});
