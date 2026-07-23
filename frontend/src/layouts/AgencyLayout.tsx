import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { AppShell } from '../components/AppShell';
import { SidebarSection } from '../components/Sidebar';
import { ROUTES } from '../routes/paths';

const navItemClass = ({ isActive }: { isActive: boolean }) =>
  `nav-item${isActive ? ' active' : ''}`;

// Shell mínimo de agencia (design §D5): un único destino "Liberar" (pivotes
// por-empleado / por-fecha + historial). Rol AGENCIA solo opera liberaciones
// administrativas. La navegación se fija vía AppShell (sidebar desktop / drawer
// móvil). Destino sin cambios.
export function AgencyLayout() {
  const { t } = useTranslation();
  return (
    <AppShell
      nav={
        <>
          <SidebarSection label={t('layout.sections.navigation')} />
          <NavLink to={ROUTES.agencyReleases} className={navItemClass}>
            <i className="ti ti-arrow-back-up" aria-hidden="true" />
            {t('releases.hub.navLabel')}
          </NavLink>
        </>
      }
    />
  );
}
