import { test, expect } from './fixtures';
import { ADMIN, EMPLOYEE, loginUI } from './helpers';

// =====================================================================
// Puerta 3 — e2e: RBAC en la UI. El guard `ProtectedRoute` redirige a /login
// cuando no hay sesión o el rol no coincide con el requerido por la ruta.
// Valida el aislamiento real de las áreas /admin y /employee (cruza router +
// sesión), algo que los tests de componente mockean.
// =====================================================================

test('should_redirect_to_login_when_unauthenticated_hits_protected_route', async ({ page }) => {
  await page.goto('/admin/employees');
  await expect(page).toHaveURL(/\/login$/);

  await page.goto('/employee/requests');
  await expect(page).toHaveURL(/\/login$/);
});

test('should_block_employee_from_admin_area', async ({ page }) => {
  await loginUI(page, EMPLOYEE.login, EMPLOYEE.password);
  await expect(page).toHaveURL(/\/employee(\/|$)/);

  // El empleado intenta un área de admin: el guard lo saca de /admin.
  await page.goto('/admin/employees');
  await expect(page).not.toHaveURL(/\/admin\//);
});

test('should_block_admin_from_employee_area', async ({ page }) => {
  await loginUI(page, ADMIN.login, ADMIN.password);
  await expect(page).toHaveURL(/\/admin\/employees$/);

  // El admin intenta un área de empleado: el guard lo saca de /employee.
  await page.goto('/employee/requests');
  await expect(page).not.toHaveURL(/\/employee\//);
});
