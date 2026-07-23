import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { Sidebar, SidebarSection } from '../components/Sidebar';
import { SidebarUserCard } from '../components/SidebarUserCard';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de empleado (app-shell spec, restructure-admin-workflows): sidebar
// ALEATICA con 4 destinos (Mi Semana, Plano, Mis solicitudes, Mis plazas) +
// area de usuario al pie (logout/preferencias) + <Outlet/>. Sin top-bar
// (prototipo aprobado). "Mi Semana" es la ruta indice del portal.
export function EmployeeLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <div className="layout">
        <Sidebar footer={<SidebarUserCard />}>
          <SidebarSection label={t('layout.sections.navigation')} />
          <NavLink to={ROUTES.employeeMyWeek} className={navItemClass}>
            <i className="ti ti-calendar-event" aria-hidden="true" />
            {t('calendar.myWeek.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeFloorPlan} className={navItemClass}>
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeRequests} className={navItemClass}>
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.mine.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.employeeMyResources} className={navItemClass}>
            <i className="ti ti-pin" aria-hidden="true" />
            {t('myResources.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
