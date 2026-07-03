import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { describe, expect, it, vi } from 'vitest';
import { ResetPasswordModal } from './ResetPasswordModal';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { employeeAlice } from '../mocks/employeeFixtures';
import { renderWithProviders } from '../test/renderWithProviders';

const RESET_URL = `${MSW_BASE}/employees/${employeeAlice.id}/reset-password`;

describe('ResetPasswordModal', () => {
  it('should_show_temp_password_once_when_resetting_in_phase1', async () => {
    server.use(
      http.post(RESET_URL, () =>
        HttpResponse.json({ temporaryPassword: 'Temp0ral!23', mustChange: true }),
      ),
    );
    const user = userEvent.setup();
    renderWithProviders(<ResetPasswordModal employee={employeeAlice} onClose={vi.fn()} />);

    await user.click(
      screen.getByRole('button', { name: /generar contraseña temporal|generate temporary/i }),
    );

    expect(await screen.findByText('Temp0ral!23')).toBeInTheDocument();
    expect(screen.getByText(/se muestra una sola vez|shown only once/i)).toBeInTheDocument();
  });

  it('should_show_email_note_when_phase2_returns_no_temp_password', async () => {
    server.use(http.post(RESET_URL, () => HttpResponse.json({ mustChange: true })));
    const user = userEvent.setup();
    renderWithProviders(<ResetPasswordModal employee={employeeAlice} onClose={vi.fn()} />);

    await user.click(
      screen.getByRole('button', { name: /generar contraseña temporal|generate temporary/i }),
    );

    await waitFor(() => {
      expect(
        screen.getByText(/se enviará por email|sent by email/i),
      ).toBeInTheDocument();
    });
    expect(screen.queryByText('Temp0ral!23')).not.toBeInTheDocument();
  });
});
