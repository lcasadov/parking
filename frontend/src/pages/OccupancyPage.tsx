import { useTranslation } from 'react-i18next';
import { AdminCalendarPage } from './AdminCalendarPage';

// Destino "Ocupación": la rejilla semanal accionable (Semanal). La antigua pestaña
// "Disponibilidad" se retiró: su función ("ver solo lo libre") es ahora el filtro
// rápido "Solo libres" de la propia rejilla, siempre visible (ver AdminCalendarPage).
// Al quedar un único contenido, el contenedor de pestañas sobra: la página monta la
// rejilla directamente. Las rutas antiguas (/admin/calendar, /admin/availability)
// redirigen aquí (AppRoutes.tsx).
export function OccupancyPage() {
  const { t } = useTranslation();

  return (
    <section className="occupancy-page" aria-label={t('occupancy.title')}>
      <AdminCalendarPage />
    </section>
  );
}
