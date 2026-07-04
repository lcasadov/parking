import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { AdministrativeReleasesPage } from './AdministrativeReleasesPage';
import { Toast } from '../components/Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { renderWithProviders } from '../test/renderWithProviders';
import { todayIso } from '../utils/releases';

async function openForm(): Promise<void> {
  const user = userEvent.setup();
  await user.click(await screen.findByRole('button', { name: /nueva liberación|new release/i }));
  await screen.findByRole('dialog');
}

async function fillEmployeeSpaceDate(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.selectOptions(dialog.getByLabelText(/empleado|employee/i), '10');
  await user.selectOptions(dialog.getByLabelText(/plaza|space/i), '1');
  fireEvent.change(dialog.getByLabelText(/fecha a liberar|date to release/i), {
    target: { value: todayIso() },
  });
}

async function submitForm(): Promise<void> {
  const user = userEvent.setup();
  const dialog = within(screen.getByRole('dialog'));
  await user.click(dialog.getByRole('button', { name: /^liberar$|^release$/i }));
}

describe('AdministrativeReleasesPage (ADMIN)', () => {
  it('should_create_administrative_release_when_admin_and_reason_present', async () => {
    const user = userEvent.setup();
    let sentReason: string | null = null;
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, async ({ request }) => {
        const body = (await request.json()) as { reason: string };
        sentReason = body.reason;
        return HttpResponse.json({ id: 998, type: 'ADMINISTRATIVE' }, { status: 201 });
      }),
    );
    renderWithProviders(
      <>
        <AdministrativeReleasesPage />
        <Toast />
      </>,
    );

    await openForm();
    await fillEmployeeSpaceDate();
    const dialog = within(screen.getByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'No acude esta semana');
    await submitForm();

    await waitFor(() => expect(sentReason).toBe('No acude esta semana'));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(
      await screen.findByText(/liberación administrativa creada|administrative release created/i),
    ).toBeInTheDocument();
  });

  it('should_reject_when_administrative_release_missing_reason', async () => {
    let requested = false;
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, () => {
        requested = true;
        return HttpResponse.json({ id: 1 }, { status: 201 });
      }),
    );
    renderWithProviders(<AdministrativeReleasesPage />);

    await openForm();
    await fillEmployeeSpaceDate();
    await submitForm();

    const dialog = within(screen.getByRole('dialog'));
    expect(dialog.getByRole('alert')).toHaveTextContent(
      /indica el motivo|provide the reason/i,
    );
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(requested).toBe(false);
  });

  it('should_surface_conflict_when_server_returns_409', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(`${MSW_BASE}/releases/administrative`, () =>
        HttpResponse.json(
          { error: 'RELEASE_CONFLICT', message: 'no assignment', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <AdministrativeReleasesPage />
        <Toast />
      </>,
    );

    await openForm();
    await fillEmployeeSpaceDate();
    const dialog = within(screen.getByRole('dialog'));
    await user.type(dialog.getByLabelText(/motivo|reason/i), 'Motivo válido');
    await submitForm();

    expect(
      await screen.findByText(/no tiene asignación fija ese día|has no fixed assignment that day/i),
    ).toBeInTheDocument();
  });
});
