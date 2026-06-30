import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { useAuth } from './useAuth';
import { server } from '../mocks/server';
import { MSW_BASE } from '../mocks/handlers';
import { adminUser } from '../mocks/fixtures';
import { renderWithProviders } from '../test/renderWithProviders';

function AuthProbe() {
  const { user, isLoading } = useAuth();
  if (isLoading) {
    return <span>loading</span>;
  }
  return <span data-testid="login">{user ? user.login : 'anonymous'}</span>;
}

describe('AuthProvider', () => {
  it('should_expose_user_when_me_returns_200', async () => {
    server.use(http.get(`${MSW_BASE}/auth/me`, () => HttpResponse.json(adminUser)));

    renderWithProviders(<AuthProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('login')).toHaveTextContent('admin');
    });
  });

  it('should_expose_null_when_me_returns_401', async () => {
    server.use(
      http.get(`${MSW_BASE}/auth/me`, () =>
        HttpResponse.json({ error: 'unauthorized', message: 'no' }, { status: 401 }),
      ),
    );

    renderWithProviders(<AuthProbe />);

    await waitFor(() => {
      expect(screen.getByTestId('login')).toHaveTextContent('anonymous');
    });
  });
});
