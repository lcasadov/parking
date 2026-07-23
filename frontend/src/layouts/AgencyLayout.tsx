import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { Sidebar, SidebarSection } from '../components/Sidebar';
import { SidebarUserCard } from '../components/SidebarUserCard';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Shell minimo de agencia: sidebar ALEATICA con el destino "Liberar" (ambos
// pivotes por-empleado/por-fecha + historial) + area de usuario al pie + <Outlet/>.
// Rol AGENCIA solo opera sobre liberaciones administrativas (design §D5). Sin top-bar.
export function AgencyLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <div className="layout">
        <Sidebar footer={<SidebarUserCard />}>
          <SidebarSection label={t('layout.sections.navigation')} />
          <NavLink to={ROUTES.agencyReleases} className={navItemClass}>
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.hub.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
