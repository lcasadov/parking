import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';
import { I18nextProvider } from 'react-i18next';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './auth/AuthProvider';
import { SessionExpiredModal } from './components/SessionExpiredModal';
import { Toast } from './components/Toast';
import i18n from './i18n';
import { AppRoutes } from './routes/AppRoutes';
import { ThemeProvider } from './theme/ThemeProvider';

export function App() {
  // QueryClient por montaje (no module-scope): evita que la cache de sesion se
  // comparta entre instancias de <App/> (aislamiento en tests de composicion).
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } },
      }),
  );
  return (
    <QueryClientProvider client={queryClient}>
      <I18nextProvider i18n={i18n}>
        <ThemeProvider>
          <BrowserRouter
            future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
          >
            <AuthProvider>
              <AppRoutes />
              <SessionExpiredModal />
              <Toast />
            </AuthProvider>
          </BrowserRouter>
        </ThemeProvider>
      </I18nextProvider>
    </QueryClientProvider>
  );
}
