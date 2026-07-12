import { test as base } from '@playwright/test';
import { resetRequests } from './reset-db';

// =====================================================================
// Runner e2e con aislamiento por test. Extiende el `test` base con un fixture
// auto-use que vacia la tabla `requests` ANTES DE CADA test, de modo que cada
// escenario parte de un baseline limpio de la BD dev compartida (ver reset-db.ts).
//
// Los specs deben importar `test` y `expect` DESDE AQUI (no de @playwright/test)
// para heredar el reset automatico.
// =====================================================================
export const test = base.extend<{ resetRequestsTable: void }>({
  resetRequestsTable: [
    async ({}, use) => {
      resetRequests();
      await use();
    },
    { auto: true },
  ],
});

export { expect } from '@playwright/test';
