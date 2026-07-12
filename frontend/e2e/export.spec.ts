import { test, expect } from './fixtures';
import { loginAsEmployee, loginAsAdmin } from './helpers';

// =====================================================================
// Puerta 3 — e2e: exportaciones. Verifican que la UI dispara una descarga real
// (blob → <a download>) con el nombre de fichero correcto, cruzando la cadena
// completa proxy Vite → backend (Content-Disposition).
// =====================================================================

test('should_download_csv_when_employee_exports_my_requests', async ({ page }) => {
  await loginAsEmployee(page);
  await page.goto('/employee/requests');

  const group = page.getByRole('group', { name: 'Opciones de exportación' });
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    group.getByRole('button', { name: 'Exportar CSV' }).click(),
  ]);
  expect(download.suggestedFilename()).toMatch(/^my-requests.*\.csv$/);
});

test('should_download_xlsx_when_employee_exports_personal_data', async ({ page }) => {
  await loginAsEmployee(page);

  // "Exportar mis datos" (RGPD) vive en la cabecera de cualquier página con sesión.
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Exportar mis datos' }).click(),
  ]);
  expect(download.suggestedFilename()).toMatch(/^my-data.*\.xlsx$/);
});

test('should_download_xlsx_when_admin_exports_requests', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/requests');

  const group = page.getByRole('group', { name: 'Opciones de exportación' });
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    group.getByRole('button', { name: 'Exportar XLSX' }).click(),
  ]);
  expect(download.suggestedFilename()).toMatch(/^requests.*\.xlsx$/);
});
