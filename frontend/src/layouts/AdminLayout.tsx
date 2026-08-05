import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { AppShell } from '../components/AppShell';
import { PendingRequestsBadge } from '../components/PendingRequestsBadge';
import { VehicleReviewBadge } from '../components/VehicleReviewBadge';
import { SidebarSection } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de administración (app-shell spec, restructure-admin-workflows):
// 9 destinos en dos grupos —Operativa (Solicitudes, Ocupación, Plano, Liberar,
// Visitantes) y Gestión (Empleados, Recursos, Registros, Ajustes)—. La navegación
// se entrega a AppShell, que la fija en el sidebar (desktop) y el drawer (móvil).
// Solo cambia la presentación: los destinos y su orden se conservan.
export function AdminLayout() {
  const { t } = useTranslation();
  return (
    <AppShell
      nav={
        <>
          <SidebarSection label={t('layout.sections.operations')} />
          <NavLink to={ROUTES.admin} end className={navItemClass}>
            <i className="ti ti-layout-dashboard" aria-hidden="true" />
            {t('dashboard.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminRequests} className={navItemClass}>
            <i className="ti ti-inbox" aria-hidden="true" />
            {t('requests.inbox.navLabel')}
            <PendingRequestsBadge />
          </NavLink>
          <NavLink to={ROUTES.adminOccupancy} className={navItemClass}>
            <i className="ti ti-calendar-week" aria-hidden="true" />
            {t('occupancy.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminFloorPlan} className={navItemClass}>
            <i className="ti ti-map-2" aria-hidden="true" />
            {t('floorPlan.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminReleaseHub} className={navItemClass}>
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.hub.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminVisitors} className={navItemClass}>
            <i className="ti ti-user-plus" aria-hidden="true" />
            {t('visitors.navLabel')}
          </NavLink>

          <SidebarSection label={t('layout.sections.management')} />
          <NavLink to={ROUTES.adminEmployees} className={navItemClass}>
            <i className="ti ti-users" aria-hidden="true" />
            {t('employees.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminVehicles} className={navItemClass}>
            <i className="ti ti-car" aria-hidden="true" />
            {t('vehicles.review.navLabel')}
            <VehicleReviewBadge />
          </NavLink>
          <NavLink to={ROUTES.adminResources} className={navItemClass}>
            <i className="ti ti-parking" aria-hidden="true" />
            {t('resources.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminRecords} className={navItemClass}>
            <i className="ti ti-history" aria-hidden="true" />
            {t('records.navLabel')}
          </NavLink>
          <NavLink to={ROUTES.adminSettings} className={navItemClass}>
            <i className="ti ti-settings" aria-hidden="true" />
            {t('settings.navLabel')}
          </NavLink>
        </>
      }
    />
  );
}
