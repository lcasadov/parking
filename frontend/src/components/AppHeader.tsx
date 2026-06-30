import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { logout } from '../api/authApi';
import { useAuth } from '../auth/useAuth';
import { ROUTES } from '../routes/paths';
import { BrandCurve, BrandLogo } from './BrandCurve';
import { Button } from './Button';
import { LanguageToggle } from './LanguageToggle';
import { ThemeToggle } from './ThemeToggle';

// Cabecera de la app: logo + curva SVG + toggles tema/idioma + logout.
export function AppHeader({ pageTitle }: { pageTitle?: string }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { user, clearUser } = useAuth();

  const mutation = useMutation({
    mutationFn: logout,
    onSettled: () => {
      clearUser();
      navigate(ROUTES.login, { replace: true });
    },
  });

  return (
    <header className="app-header">
      <BrandCurve className="curve" />
      <BrandLogo />
      {pageTitle ? <span className="page-title">{pageTitle}</span> : null}
      <div className="header-controls">
        <LanguageToggle />
        <ThemeToggle />
        {user ? (
          <Button
            variant="white"
            icon="logout"
            onClick={() => mutation.mutate()}
            disabled={mutation.isPending}
          >
            {t('common.logout')}
          </Button>
        ) : null}
      </div>
    </header>
  );
}
