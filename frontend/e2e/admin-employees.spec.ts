import { test, expect } from './fixtures';
import { loginAsAdmin } from './helpers';

// =====================================================================
// Puerta 3 — e2e: alta de empleado (CRUD admin). El reset del fixture elimina
// los empleados no-seed entre tests, así que el login/email son estables.
// =====================================================================

test('should_create_an_employee_and_show_it_in_the_list', async ({ page }) => {
  await loginAsAdmin(page); // landing en /admin/employees

  await page.getByRole('button', { name: 'Nuevo empleado' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();

  await dialog.getByLabel('Nombre', { exact: true }).fill('E2E');
  await dialog.getByLabel('Apellidos', { exact: true }).fill('Nuevo');
  await dialog.getByLabel('Usuario / login').fill('e2e_newemp');
  await dialog.getByLabel('Email', { exact: true }).fill('e2e_newemp@test.local');
  await dialog.getByRole('button', { name: 'Guardar' }).click();
  await expect(dialog).toBeHidden();

  // Filtra por el email y comprueba que la fila aparece.
  await page.getByLabel('Buscar empleado').fill('e2e_newemp');
  await expect(page.getByText('e2e_newemp@test.local')).toBeVisible();
});
