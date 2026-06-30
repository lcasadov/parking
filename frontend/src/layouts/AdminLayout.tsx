import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { Sidebar } from '../components/Sidebar';

// Layout de administracion (vacio en bootstrap): header + sidebar + <Outlet/>.
export function AdminLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.adminArea')} />
      <div className="layout">
        <Sidebar />
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
