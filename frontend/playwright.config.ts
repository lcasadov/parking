import { defineConfig, devices } from '@playwright/test';

// Configuracion e2e (Puerta 3): stack REAL integrado, sin mocks.
// Prerrequisitos (ver e2e/README.md): SQL Server (docker compose) y backend
// Spring Boot (perfil des) arrancados. El dev server de Vite lo orquesta
// Playwright via `webServer` (reutiliza uno ya levantado si existe).
export default defineConfig({
  testDir: './e2e',
  // Serial: los specs comparten el empleado dev y la BD (estado mutable);
  // el paralelismo provoca carreras al crear/cancelar solicitudes.
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list']],
  timeout: 30_000,
  use: {
    // baseURL configurable para e2e cuando 5173 está ocupado (Vite auto-incrementa).
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
      // Los specs móviles corren solo en el proyecto `mobile`.
      testIgnore: '**/floor-plan-mobile.spec.ts',
    },
    {
      name: 'mobile',
      use: { ...devices['Pixel 5'] },
      testMatch: '**/floor-plan-mobile.spec.ts',
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
