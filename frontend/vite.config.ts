/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import { configDefaults } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Dev-proxy + baseURL relativo: el navegador habla siempre con el mismo origen
// (localhost:5173) y Vite reenvia /parking-api al backend. Sin CORS en ningun
// entorno; en produccion la SPA se sirve same-origin desde Tomcat.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/parking-api': {
        target: process.env.VITE_PROXY_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  test: {
    // Los e2e de Playwright (frontend/e2e) NO son tests de Vitest: excluirlos
    // para que `npm test` no intente ejecutar los .spec de Playwright.
    exclude: [...configDefaults.exclude, 'e2e/**'],
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
    css: true,
    testTimeout: 15000,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      thresholds: { lines: 80, branches: 75, functions: 80, statements: 80 },
      include: ['src/**/*.{ts,tsx}'],
      // App.tsx y AppRoutes.tsx SI se cubren (App.test.tsx ejercita la
      // composicion real de rutas + SessionExpiredModal — bug #9/#10).
      // main.tsx queda excluido: solo llama a createRoot (entry point sin logica).
      exclude: [
        '**/*.config.*',
        'src/main.tsx',
        'src/types/**',
        'src/**/*.d.ts',
        'src/i18n/**',
        'src/test/**',
        'src/mocks/**',
        'src/**/*.test.{ts,tsx}',
        'src/vite-env.d.ts',
      ],
    },
  },
});
