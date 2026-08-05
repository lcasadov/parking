import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  isIOS,
  isStandalone,
  notificationPermission,
  pushSupported,
  urlBase64ToUint8Array,
} from './pushSupport';

describe('pushSupport', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', vi.fn(() => ({ matches: false })));
  });
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('urlBase64ToUint8Array decodes base64url to bytes', () => {
    const out = urlBase64ToUint8Array('AAAA');
    expect(out).toBeInstanceOf(Uint8Array);
    expect(out.length).toBe(3);
    expect(Array.from(out)).toEqual([0, 0, 0]);
  });

  it('urlBase64ToUint8Array handles padding and url-safe chars', () => {
    // "-_" son los caracteres url-safe de "+/"; no debe lanzar.
    expect(() => urlBase64ToUint8Array('a-_b')).not.toThrow();
  });

  it('isStandalone / isIOS / pushSupported return booleans', () => {
    expect(typeof isStandalone()).toBe('boolean');
    expect(typeof isIOS()).toBe('boolean');
    expect(typeof pushSupported()).toBe('boolean');
  });

  it('notificationPermission returns a valid state', () => {
    expect(['default', 'granted', 'denied', 'unsupported']).toContain(notificationPermission());
  });
});
