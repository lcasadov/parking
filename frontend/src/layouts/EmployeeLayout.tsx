import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { AppShell } from '../components/AppShell';
import { SidebarSection } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Layout de empleado: 3 destinos (Mi Semana, Mis solicitudes, Mis sitios fijos).
// El "Plano" se quitó del nav (change reservas-employee-admin-reassign): la
// orientación se cubre con el botón "Mapa" contextual en las tarjetas. La
// navegación se entrega a AppShell (sidebar en desktop, drawer en móvil).
// "Mi Semana" es la ruta índice del portal.
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
