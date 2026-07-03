import '@testing-library/jest-dom';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { setSessionActive } from './api/sessionState';
import { server } from './mocks/server';

// MSW: arranca antes de los tests, resetea handlers entre tests, cierra al final.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
  document.body.className = '';
  // Flag module-scope del interceptor 401: reset entre tests.
  setSessionActive(false);
});
afterAll(() => server.close());
