import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../test/renderWithProviders';

const mockUsePush = vi.fn();
const mockUseInstallPrompt = vi.fn();
vi.mock('../hooks/usePush', () => ({
  usePush: () => mockUsePush(),
  useInstallPrompt: () => mockUseInstallPrompt(),
}));

// Importar tras el mock.
// eslint-disable-next-line import/first
import { NotificationOnboarding } from './NotificationOnboarding';

describe('NotificationOnboarding', () => {
  beforeEach(() => {
    window.localStorage.clear();
    mockUsePush.mockReturnValue({ state: 'default', busy: false, enable: vi.fn(), disable: vi.fn() });
    mockUseInstallPrompt.mockReturnValue({ canInstall: false, install: vi.fn() });
  });

  it('shows the activate card when supported and not subscribed', () => {
    renderWithProviders(<NotificationOnboarding />);
    expect(
      screen.getByRole('button', { name: /activar notificaciones|turn on notifications/i }),
    ).toBeInTheDocument();
  });

  it('renders nothing when already subscribed', () => {
    mockUsePush.mockReturnValue({ state: 'subscribed', busy: false, enable: vi.fn(), disable: vi.fn() });
    const { container } = renderWithProviders(<NotificationOnboarding />);
    expect(container.querySelector('.push-onboarding')).toBeNull();
  });

  it('shows iOS install instructions when not installed', () => {
    mockUsePush.mockReturnValue({
      state: 'ios-not-installed',
      busy: false,
      enable: vi.fn(),
      disable: vi.fn(),
    });
    renderWithProviders(<NotificationOnboarding />);
    expect(screen.getByText(/pantalla de inicio|home screen/i)).toBeInTheDocument();
  });

  it('shows the Android install card when installable', () => {
    mockUseInstallPrompt.mockReturnValue({ canInstall: true, install: vi.fn() });
    renderWithProviders(<NotificationOnboarding />);
    expect(screen.getByRole('button', { name: /instalar|install/i })).toBeInTheDocument();
  });

  it('dismisses the card and remembers it', async () => {
    const user = userEvent.setup();
    renderWithProviders(<NotificationOnboarding />);
    await user.click(screen.getByRole('button', { name: /cerrar|close/i }));
    expect(screen.queryByText(/activa las notificaciones|turn on notifications/i)).not.toBeInTheDocument();
  });
});
