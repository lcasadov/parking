import { test, expect } from './fixtures';
import { loginAsEmployee } from './helpers';

// =====================================================================
// Puerta 3 — e2e (escritorio): el plano de puestos renderiza en viewport de
// escritorio (el proyecto `mobile` cubre el mismo plano en Pixel 5). Aseveramos
// sobre la imagen/heading del plano, señales estables independientes del viewport.
// =====================================================================

test('should_render_the_floor_plan_on_desktop', async ({ page }) => {
  await loginAsEmployee(page);
  await page.goto('/employee/floor-plan');

  await expect(page.getByRole('heading', { name: 'Plano de puestos' })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Plano de la planta de oficina' })).toBeVisible();
});
