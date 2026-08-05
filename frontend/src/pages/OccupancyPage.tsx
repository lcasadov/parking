import { AdminCalendarPage } from './AdminCalendarPage';

// Destino "Ocupación": la rejilla semanal accionable (Semanal). La antigua pestaña
// "Disponibilidad" se retiró: su función ("ver solo lo libre") es ahora el filtro
// rápido "Solo libres" de la propia rejilla, siempre visible (ver AdminCalendarPage).
// AdminCalendarPage ya monta su propio PageFrame (marco sticky), que debe ser hijo
// DIRECTO de .main para que el marco funcione; por eso NO se envuelve en otra sección.
// Las rutas antiguas (/admin/calendar, /admin/availability) redirigen aquí (AppRoutes.tsx).
export function OccupancyPage() {
  return <AdminCalendarPage />;
}
