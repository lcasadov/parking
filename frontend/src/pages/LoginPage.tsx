import { useMutation } from '@tanstack/react-query';
import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { login } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { AuthShell } from '../components/AuthShell';
import { Button } from '../components/Button';
import { InfoBanner } from '../components/InfoBanner';
import { Input } from '../components/Input';
import { homePathForRole } from '../routes/paths';
import type { ApiError, CurrentUser } from '../types/auth';

// Extrae los intentos restantes del error del backend, si los expone en
// ApiError.fields.remainingAttempts. Devuelve null cuando no esta disponible.
function remainingAttemptsFromError(error: unknown): number | null {
  if (!isAxiosError(error)) {
    return null;
  }
  const data = error.response?.data as ApiError | undefined;
  const raw = data?.fields?.remainingAttempts;
  if (raw === undefined) {
    return null;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [loginValue, setLoginValue] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const passwordId = useId();

  const mutation = useMutation<CurrentUser, unknown, void>({
    mutationFn: () => login({ login: loginValue, password }),
    onSuccess: (user) => {
      setUser(user);
      navigate(homePathForRole(user.role), { replace: true });
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    mutation.mutate();
  }

  const remainingAttempts = mutation.isError
    ? remainingAttemptsFromError(mutation.error)
    : null;

  return (
    <AuthShell title={t('auth.loginTitle')}>
      <form onSubmit={handleSubmit} noValidate>
        {mutation.isError ? (
          <div role="alert">
            <InfoBanner variant="red" icon="alert-circle">
              {remainingAttempts !== null
                ? t('auth.invalidCredentialsAttempts', { count: remainingAttempts })
                : t('auth.invalidCredentials')}
            </InfoBanner>
          </div>
        ) : null}
        <Input
          label={t('auth.loginField')}
          name="login"
          autoComplete="username"
          value={loginValue}
          onChange={(e) => setLoginValue(e.target.value)}
          required
        />
        <div className="auth-field">
          <label className="field-label" htmlFor={passwordId}>
            {t('auth.passwordField')}
          </label>
          <div className="field-value with-icon">
            <input
              id={passwordId}
              className="field-input"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <button
              type="button"
              className="pw-toggle"
              aria-pressed={showPassword}
              aria-label={showPassword ? t('auth.hidePassword') : t('auth.showPassword')}
              onClick={() => setShowPassword((shown) => !shown)}
            >
              <i className={`ti ti-${showPassword ? 'eye-off' : 'eye'}`} aria-hidden="true" />
            </button>
          </div>
        </div>
        <Button variant="green" submit icon="login-2" disabled={mutation.isPending} className="btn-block">
          {t('auth.signIn')}
        </Button>
        <p className="auth-help">{t('auth.forgotPassword')}</p>
        <p className="auth-lockout">
          <i className="ti ti-shield-lock" aria-hidden="true" /> {t('auth.lockoutNote')}
        </p>
      </form>
    </AuthShell>
  );
}
