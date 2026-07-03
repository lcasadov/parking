import { useMutation } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { login } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { AuthShell } from '../components/AuthShell';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { homePathForRole } from '../routes/paths';
import type { CurrentUser } from '../types/auth';

export function LoginPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { setUser } = useAuth();
  const [loginValue, setLoginValue] = useState('');
  const [password, setPassword] = useState('');

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

  return (
    <AuthShell title={t('auth.loginTitle')}>
      <form onSubmit={handleSubmit} noValidate>
        <Input
          label={t('auth.loginField')}
          name="login"
          autoComplete="username"
          value={loginValue}
          onChange={(e) => setLoginValue(e.target.value)}
          required
        />
        <Input
          label={t('auth.passwordField')}
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {mutation.isError ? (
          <p className="form-error" role="alert">
            {t('auth.invalidCredentials')}
          </p>
        ) : null}
        <Button variant="green" submit disabled={mutation.isPending} className="btn-block">
          {t('auth.signIn')}
        </Button>
      </form>
    </AuthShell>
  );
}
