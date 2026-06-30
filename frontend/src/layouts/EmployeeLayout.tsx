import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';

// Layout de empleado (vacio en bootstrap): header + <Outlet/>.
export function EmployeeLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.employeeArea')} />
      <main className="main">
        <Outlet />
      </main>
    </div>
  );
}
