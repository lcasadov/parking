import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { AppShell } from '../components/AppShell';
import { SidebarSection } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de empleado (app-shell spec): 4 destinos (Mi Semana, Plano, Mis
// solicitudes, Mis plazas). La navegación se entrega a AppShell, que la fija en el
// sidebar (desktop) y el drawer off-canvas (móvil, uso principal del empleado).
// "Mi Semana" es la ruta índice del portal. Destinos y orden sin cambios.
export function EmployeeLayout() {
  const { t } = useTranslation();
  return (
    <AppShell
      nav={
        <>
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
        </>
      }
    />
  );
}
