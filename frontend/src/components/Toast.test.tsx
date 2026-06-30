import { act, screen, waitFor } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Toast } from './Toast';
import { emitApiErrorToast } from '../api/events';
import { renderWithProviders } from '../test/renderWithProviders';

describe('Toast', () => {
  it('should_show_message_when_api_error_event_emitted', async () => {
    renderWithProviders(<Toast />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    act(() => emitApiErrorToast('errors.server'));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });
});
