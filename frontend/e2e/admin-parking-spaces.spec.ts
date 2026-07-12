import { test, expect } from './fixtures';
import { loginAsAdmin } from './helpers';

// =====================================================================
// Puerta 3 — e2e: alta de plaza (CRUD admin). La etiqueta lleva prefijo `E2E-`
// para que el reset del fixture la elimine entre tests.
//
// La lista se ordena por id y pagina (PAGE_SIZE=20) y el buscador filtra solo la
// página cargada, así que la plaza recién creada (id más alto → última página)
// no es visible de forma determinista. Verificamos la persistencia por la vía del
// guard de duplicados: reintentar la misma etiqueta debe dar 409 inline.
// =====================================================================

const LABEL = 'E2E-P1';
const DUPLICATE_ERROR = 'Ya existe una plaza con esa etiqueta';

test('should_create_a_parking_space_and_persist_it', async ({ page }) => {
  await loginAsAdmin(page);
  await page.goto('/admin/parking-spaces');

  // Alta de la plaza.
  await page.getByRole('button', { name: 'Nueva plaza' }).click();
  const createDialog = page.getByRole('dialog');
  await createDialog.getByLabel('Etiqueta de la plaza').fill(LABEL);
  await createDialog.getByRole('button', { name: 'Guardar' }).click();
  await expect(createDialog).toBeHidden();

  // Persistencia: reintentar la MISMA etiqueta debe dar error de duplicado.
  await page.getByRole('button', { name: 'Nueva plaza' }).click();
  const dupDialog = page.getByRole('dialog');
  await dupDialog.getByLabel('Etiqueta de la plaza').fill(LABEL);
  await dupDialog.getByRole('button', { name: 'Guardar' }).click();
  await expect(page.getByText(DUPLICATE_ERROR)).toBeVisible();
});
