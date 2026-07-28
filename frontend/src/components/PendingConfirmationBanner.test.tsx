import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { PendingConfirmationBanner } from './PendingConfirmationBanner';
import { Toast } from './Toast';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { requestPending1 } from '../mocks/requestFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const RESEND_URL = `${MSW_BASE}/requests/:id/resend`;
const HOUR_MS = 60 * 60 * 1000;

// createdAt/lastRemindedAt relativos a "ahora" (Date real, sin fake timers, igual
// que el resto de la suite: utils/requests.test.ts fija `now` via parametro).
function hoursAgo(hours: number): string {
  return new Date(Date.now() - hours * HOUR_MS).toISOString();
}

describe('PendingConfirmationBanner', () => {
  it('should_show_the_pending_message_always', () => {
    renderWithProviders(
      <PendingConfirmationBanner requestId={requestPending1.id} createdAt={hoursAgo(1)} />,
    );

    expect(
      screen.getByText(/pendiente de confirmación|pending confirmation/i),
    ).toBeInTheDocument();
  });

  it('should_hide_the_resend_button_before_24h_since_creation', () => {
    renderWithProviders(
      <PendingConfirmationBanner requestId={requestPending1.id} createdAt={hoursAgo(2)} />,
    );

    expect(
      screen.queryByRole('button', { name: /reenviar solicitud|resend request/i }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(/podrás reavisar en|you can resend in/i)).toBeInTheDocument();
  });

  it('should_show_the_resend_button_after_24h_since_creation', () => {
    renderWithProviders(
      <PendingConfirmationBanner requestId={requestPending1.id} createdAt={hoursAgo(25)} />,
    );

    expect(
      screen.getByRole('button', { name: /reenviar solicitud|resend request/i }),
    ).toBeInTheDocument();
  });

  it('should_use_last_reminded_at_instead_of_creation_when_more_recent', () => {
    renderWithProviders(
      <PendingConfirmationBanner
        requestId={requestPending1.id}
        createdAt={hoursAgo(48)}
        lastRemindedAt={hoursAgo(1)}
      />,
    );

    expect(
      screen.queryByRole('button', { name: /reenviar solicitud|resend request/i }),
    ).not.toBeInTheDocument();
  });

  it('should_call_resend_and_notify_success_when_button_clicked', async () => {
    const user = userEvent.setup();
    const onResent = vi.fn();
    let calledWithId: number | null = null;
    server.use(
      http.post(RESEND_URL, ({ params }) => {
        calledWithId = Number(params.id);
        return HttpResponse.json({ ...requestPending1, lastRemindedAt: new Date().toISOString() });
      }),
    );
    renderWithProviders(
      <>
        <PendingConfirmationBanner
          requestId={requestPending1.id}
          createdAt={hoursAgo(30)}
          onResent={onResent}
        />
        <Toast />
      </>,
    );

    await user.click(screen.getByRole('button', { name: /reenviar solicitud|resend request/i }));

    await waitFor(() => expect(calledWithId).toBe(requestPending1.id));
    expect(await screen.findByText(/reenviada a los administradores|resent to the administrators/i)).toBeInTheDocument();
    expect(onResent).toHaveBeenCalledTimes(1);
  });

  it('should_notify_forbidden_error_when_resend_is_rejected_with_403', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(RESEND_URL, () =>
        HttpResponse.json(
          { error: 'FORBIDDEN', message: 'not yours', timestamp: '2026-03-01T09:00:00Z' },
          { status: 403 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <PendingConfirmationBanner requestId={requestPending1.id} createdAt={hoursAgo(30)} />
        <Toast />
      </>,
    );

    await user.click(screen.getByRole('button', { name: /reenviar solicitud|resend request/i }));

    expect(
      await screen.findByText(/no puedes reenviar el aviso|cannot resend the notice/i),
    ).toBeInTheDocument();
  });

  it('should_notify_too_soon_error_when_resend_is_rejected_with_409', async () => {
    const user = userEvent.setup();
    server.use(
      http.post(RESEND_URL, () =>
        HttpResponse.json(
          { error: 'RESEND_TOO_SOON', message: 'too soon', timestamp: '2026-03-01T09:00:00Z' },
          { status: 409 },
        ),
      ),
    );
    renderWithProviders(
      <>
        <PendingConfirmationBanner requestId={requestPending1.id} createdAt={hoursAgo(30)} />
        <Toast />
      </>,
    );

    await user.click(screen.getByRole('button', { name: /reenviar solicitud|resend request/i }));

    expect(
      await screen.findByText(/no ha pasado tiempo suficiente|not enough time has passed/i),
    ).toBeInTheDocument();
  });
});
