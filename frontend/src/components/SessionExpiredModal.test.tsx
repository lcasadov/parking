import { act, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { SessionExpiredModal } from './SessionExpiredModal';
import { emitSessionExpired } from '../api/events';
import { renderWithProviders } from '../test/renderWithProviders';

describe('SessionExpiredModal', () => {
  it('should_emit_session_expired_when_response_401', async () => {
    renderWithProviders(<SessionExpiredModal />);

    // No visible hasta que el interceptor emite el evento.
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    act(() => emitSessionExpired());

    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument();
    });
  });
});
