import { test, expect } from './fixtures';
import { loginAsAdmin } from './helpers';

// =====================================================================
// Puerta 3 — e2e: gestión de puestos (CRUD admin). Los 65 puestos seed ocupan
// todos los números (1-65), así que en vez de crear se cubre: (1) toggle
// activar/desactivar y (2) edición de categoría, sobre un puesto existente,
// revirtiendo el cambio en el propio test. El reset del fixture restaura además
// `active=1`.
// =====================================================================

// El puesto 13 está en la página 0 (se listan 1-20 por PAGE_SIZE=20) y es único
// como substring dentro de 1-20.
const DESK_NUMBER = '13';

test('should_deactivate_and_reactivate_a_desk', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/desks');

  const row = page.getByRole('row').filter({ hasText: DESK_NUMBER });
  await expect(row).toHaveCount(1);

  // Desactivar → Inactivo (PATCH /desks/{id}/activation).
  await row.getByRole('button', { name: 'Desactivar', exact: true }).click();
  await expect(row.getByText('Inactivo', { exact: true })).toBeVisible();

  // Reactivar → Activo (revierte el cambio).
  await row.getByRole('button', { name: 'Activar', exact: true }).click();
  await expect(row.getByText('Activo', { exact: true })).toBeVisible();
});

test('should_edit_a_desk_category_and_revert', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/desks');

  const row = page.getByRole('row').filter({ hasText: DESK_NUMBER });
  await expect(row).toHaveCount(1);

  // Editar categoría → Dirección (EXECUTIVE); el badge de la fila lo refleja.
  await row.getByRole('button', { name: 'Editar', exact: true }).click();
  const toExecutive = page.getByRole('dialog');
  await toExecutive.getByLabel('Categoría').selectOption('EXECUTIVE');
  await toExecutive.getByRole('button', { name: 'Guardar' }).click();
  await expect(toExecutive).toBeHidden();
  await expect(row.getByText('Dirección', { exact: true })).toBeVisible();

  // Revertir a Estándar (STANDARD).
  await row.getByRole('button', { name: 'Editar', exact: true }).click();
  const toStandard = page.getByRole('dialog');
  await toStandard.getByLabel('Categoría').selectOption('STANDARD');
  await toStandard.getByRole('button', { name: 'Guardar' }).click();
  await expect(toStandard).toBeHidden();
  await expect(row.getByText('Estándar', { exact: true })).toBeVisible();
});
