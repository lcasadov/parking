import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import {
  getApprovalMode,
  getParkingAddress,
  getSettings,
  getWeekendReservable,
  updateApprovalMode,
  updateParkingAddress,
  updateWeekendReservable,
} from './settingsApi';

// Tests unitarios del cliente HTTP de SystemSettings: cada función contra su endpoint
// (con MSW), verificando el mapeo de/ a la forma del contrato.
describe('settingsApi', () => {
  it('getSettings maps the full admin payload', async () => {
    server.use(
      http.get(`${MSW_BASE}/admin/settings`, () =>
        HttpResponse.json({
          approvalMode: 'MANUAL',
          parkingAddress: 'Av. de Europa 18',
          parkingLat: 40.5405,
          parkingLng: -3.651,
          weekendReservable: true,
          updatedById: 1,
          updatedAt: null,
        }),
      ),
    );
    const s = await getSettings();
    expect(s.approvalMode).toBe('MANUAL');
    expect(s.parkingLat).toBe(40.5405);
    expect(s.weekendReservable).toBe(true);
  });

  it('getApprovalMode returns the vigent mode', async () => {
    server.use(
      http.get(`${MSW_BASE}/settings/approval-mode`, () =>
        HttpResponse.json({ approvalMode: 'AUTOMATIC' }),
      ),
    );
    expect(await getApprovalMode()).toBe('AUTOMATIC');
  });

  it('updateApprovalMode PUTs the mode and returns settings', async () => {
    let body: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/admin/settings`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ approvalMode: 'AUTOMATIC', updatedById: 1, updatedAt: null });
      }),
    );
    const r = await updateApprovalMode('AUTOMATIC');
    expect(body).toEqual({ approvalMode: 'AUTOMATIC' });
    expect(r.approvalMode).toBe('AUTOMATIC');
  });

  it('getParkingAddress maps address and coordinates', async () => {
    server.use(
      http.get(`${MSW_BASE}/settings/parking-address`, () =>
        HttpResponse.json({ parkingAddress: 'Av', parkingLat: 40.5, parkingLng: -3.6 }),
      ),
    );
    expect(await getParkingAddress()).toEqual({ address: 'Av', lat: 40.5, lng: -3.6 });
  });

  it('getParkingAddress defaults missing coordinates to null', async () => {
    server.use(
      http.get(`${MSW_BASE}/settings/parking-address`, () =>
        HttpResponse.json({ parkingAddress: null, parkingLat: null, parkingLng: null }),
      ),
    );
    expect(await getParkingAddress()).toEqual({ address: null, lat: null, lng: null });
  });

  it('updateParkingAddress PUTs address and coordinates', async () => {
    let body: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/admin/settings/parking-address`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({
          approvalMode: 'MANUAL',
          parkingAddress: 'Av',
          parkingLat: 40.5,
          parkingLng: -3.6,
          weekendReservable: false,
        });
      }),
    );
    const r = await updateParkingAddress({ address: 'Av', lat: 40.5, lng: -3.6 });
    expect(body).toEqual({ parkingAddress: 'Av', parkingLat: 40.5, parkingLng: -3.6 });
    expect(r.parkingAddress).toBe('Av');
  });

  it('getWeekendReservable returns the flag', async () => {
    server.use(
      http.get(`${MSW_BASE}/settings/weekend-reservable`, () =>
        HttpResponse.json({ weekendReservable: true }),
      ),
    );
    expect(await getWeekendReservable()).toBe(true);
  });

  it('updateWeekendReservable PUTs the flag', async () => {
    let body: unknown = null;
    server.use(
      http.put(`${MSW_BASE}/admin/settings/weekend-reservable`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ approvalMode: 'MANUAL', weekendReservable: true });
      }),
    );
    const r = await updateWeekendReservable(true);
    expect(body).toEqual({ weekendReservable: true });
    expect(r.weekendReservable).toBe(true);
  });
});
