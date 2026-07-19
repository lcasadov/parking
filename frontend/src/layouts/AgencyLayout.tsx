import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { AppHeader } from '../components/AppHeader';
import { Sidebar, SidebarSection } from '../components/Sidebar';
import { SidebarUserCard } from '../components/SidebarUserCard';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Shell minimo de agencia: header + sidebar ALEATICA con una sola entrada
// (liberacion administrativa) + <Outlet/>. Rol AGENCIA solo puede liberar.
export function AgencyLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <AppHeader pageTitle={t('layout.agencyArea')} />
      <div className="layout">
        <Sidebar footer={<SidebarUserCard />}>
          <SidebarSection label={t('layout.sections.navigation')} />
          <NavLink to={ROUTES.agencyReleases} className={navItemClass}>
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
