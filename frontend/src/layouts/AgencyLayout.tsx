import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { Sidebar } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

// Shell minimo de agencia: header + sidebar con una sola entrada (liberacion
// administrativa) + <Outlet/>. Rol AGENCIA solo puede liberar (fail-closed).
export function AgencyLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.agencyArea')} />
      <div className="layout">
        <Sidebar>
          <NavLink
            to={ROUTES.agencyReleases}
            className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
          >
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.admin.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
