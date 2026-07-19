import { useTranslation } from 'react-i18next';
import { NavLink, Outlet } from 'react-router-dom';
import { PendingRequestsBadge } from '../components/PendingRequestsBadge';
import { Sidebar, SidebarSection } from '../components/Sidebar';
import { SidebarUserCard } from '../components/SidebarUserCard';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de administracion (prototipo `docs/design/prototipo-aleatica.html`):
// sidebar ALEATICA con la marca, dos secciones — Gestión y Operativa — en el orden
// del prototipo, y area de usuario al pie (logout + preferencias) + <Outlet/>.
// Sin top-bar: toda la marca y los controles viven en el sidebar.
export function AdminLayout() {
  const { t } = useTranslation();
  return (
    <div className="app-shell">
      <div className="layout">
        <Sidebar footer={<SidebarUserCard />}>
          <SidebarSection label={t('layout.sections.management')} />
          <NavLink to={ROUTES.adminCalendar} className={navItemClass}>
            <i className="ti ti-calendar-week" aria-hidden="true" />
            {t('calendar.admin.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminAvailability} className={navItemClass}>
            <i className="ti ti-calendar-search" aria-hidden="true" />
            {t('availability.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminEmployees} className={navItemClass}>
            <i className="ti ti-users" aria-hidden="true" />
            {t('employees.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminParkingSpaces} className={navItemClass}>
            <i className="ti ti-parking" aria-hidden="true" />
            {t('parkingSpaces.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminDesks} className={navItemClass}>
            <i className="ti ti-armchair" aria-hidden="true" />
            {t('desks.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminFloorPlan} className={navItemClass}>
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>

          <SidebarSection label={t('layout.sections.operations')} />
          <NavLink to={ROUTES.adminRequests} className={navItemClass}>
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.inbox.navLabel')}
            <PendingRequestsBadge />
          </NavLink>
          <NavLink to={ROUTES.adminReleases} className={navItemClass}>
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.admin.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminReleaseByDate} className={navItemClass}>
            <i className="ti ti-calendar-off" aria-hidden="true" />
            {t('releases.byDate.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminVisitors} className={navItemClass}>
            <i className="ti ti-user-plus" aria-hidden="true" />
            {t('visitors.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminAudit} className={navItemClass}>
            <i className="ti ti-history" aria-hidden="true" />
            {t('audit.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminLoginLogs} className={navItemClass}>
            <i className="ti ti-key" aria-hidden="true" />
            {t('loginLogs.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminSettings} className={navItemClass}>
            <i className="ti ti-settings" aria-hidden="true" />
            {t('settings.navLabel')}
          </NavLink>
        </Sidebar>
        <main className="main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
