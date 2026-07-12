import { test as base } from '@playwright/test';
import { resetToBaseline } from './reset-db';

// =====================================================================
// Runner e2e con aislamiento por test. Extiende el `test` base con un fixture
// auto-use que restablece la BD dev al baseline de seeds ANTES DE CADA test, de
// modo que cada escenario parte de un estado limpio y determinista de la BD dev
// compartida (ver reset-db.ts).
//
// Los specs deben importar `test` y `expect` DESDE AQUI (no de @playwright/test)
// para heredar el reset automatico.
// =====================================================================
export const test = base.extend<{ resetDbBaseline: void }>({
  resetDbBaseline: [
    // eslint-disable-next-line no-empty-pattern -- patrón de fixture Playwright sin dependencias
    async ({}, use) => {
      resetToBaseline();
      await use();
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
