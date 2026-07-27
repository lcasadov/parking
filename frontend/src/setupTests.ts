import '@testing-library/jest-dom';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { setSessionActive } from './api/sessionState';
import { server } from './mocks/server';

// jsdom no propaga las coordenadas de PointerEvent (clientX/clientY), que sí usan
// los Pointer Events del plano (arrastre/pan/pinch). Se sustituye por un polyfill
// que hereda de MouseEvent (preserva clientX/clientY) y añade pointerId/pointerType.
if (typeof window !== 'undefined') {
  class PointerEventPolyfill extends MouseEvent {
    public readonly pointerId: number;
    public readonly pointerType: string;
    public constructor(type: string, params: PointerEventInit = {}) {
      super(type, params);
      this.pointerId = params.pointerId ?? 0;
      this.pointerType = params.pointerType ?? 'mouse';
    }
  }
  window.PointerEvent = PointerEventPolyfill as unknown as typeof PointerEvent;

  // Radix UI (Select y otras primitivas) usa APIs de pointer capture y scroll que
  // jsdom no implementa; sin estos stubs el trigger no abre el panel en los tests.
  if (!Element.prototype.hasPointerCapture) {
    Element.prototype.hasPointerCapture = () => false;
  }
  if (!Element.prototype.setPointerCapture) {
    Element.prototype.setPointerCapture = () => {};
  }
  if (!Element.prototype.releasePointerCapture) {
    Element.prototype.releasePointerCapture = () => {};
  }
  if (!Element.prototype.scrollIntoView) {
    Element.prototype.scrollIntoView = () => {};
  }
}

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
