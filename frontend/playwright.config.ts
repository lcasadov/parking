import { defineConfig, devices } from '@playwright/test';

// Configuracion e2e (Puerta 3): stack REAL integrado, sin mocks.
// Prerrequisitos (ver e2e/README.md): SQL Server (docker compose) y backend
// Spring Boot (perfil des) arrancados. El dev server de Vite lo orquesta
// Playwright via `webServer` (reutiliza uno ya levantado si existe).
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list']],
  timeout: 30_000,
  use: {
    baseURL: 'http://localhost:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    timeout: 60_000,
  },
});
