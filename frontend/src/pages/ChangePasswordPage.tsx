import { useMutation } from '@tanstack/react-query';
import { useMemo, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { changePassword } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { evaluatePasswordPolicy, isPasswordValid, POLICY_RULES } from '../auth/passwordPolicy';
import { AuthShell } from '../components/AuthShell';
import { Button } from '../components/Button';
import { Input } from '../components/Input';
import { homePathForRole } from '../routes/paths';

export function ChangePasswordPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, refetch } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const policy = useMemo(() => evaluatePasswordPolicy(newPassword), [newPassword]);
  const passwordsMatch = newPassword.length > 0 && newPassword === confirmPassword;
  const canSubmit = isPasswordValid(newPassword) && passwordsMatch && currentPassword.length > 0;

  const mutation = useMutation<void, unknown, void>({
    mutationFn: () => changePassword({ currentPassword, newPassword }),
    onSuccess: async () => {
      await refetch();
      navigate(user ? homePathForRole(user.role) : '/login', { replace: true });
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (canSubmit) {
      mutation.mutate();
    }
  }

  return (
    <AuthShell title={t('auth.changePasswordTitle')}>
      <form onSubmit={handleSubmit} noValidate>
        <Input
          label={t('auth.currentPassword')}
          name="currentPassword"
          type="password"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          required
        />
        <Input
          label={t('auth.newPassword')}
          name="newPassword"
          type="password"
          autoComplete="new-password"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
        />
        <Input
          label={t('auth.confirmPassword')}
          name="confirmPassword"
          type="password"
          autoComplete="new-password"
          error={confirmPassword.length > 0 && !passwordsMatch}
          hint={
            confirmPassword.length > 0 && !passwordsMatch
              ? t('auth.passwordMismatch')
              : undefined
          }
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
        />
        <ul className="policy" aria-label={t('auth.changePasswordTitle')}>
          {POLICY_RULES.map((rule) => (
            <li key={rule} className={policy[rule] ? 'ok' : 'bad'}>
              <i
                className={`ti ti-${policy[rule] ? 'check' : 'x'}`}
                aria-hidden="true"
              />
              {t(`auth.policy.${rule}`)}
            </li>
          ))}
        </ul>
        {mutation.isError ? (
          <p className="form-error" role="alert">
            {t('errors.server')}
          </p>
        ) : null}
        <Button
          variant="green"
          submit
          disabled={!canSubmit || mutation.isPending}
          className="btn-block"
        >
          {t('auth.changePasswordCta')}
        </Button>
      </form>
    </AuthShell>
  );
}
