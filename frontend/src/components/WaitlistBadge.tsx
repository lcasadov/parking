import { useTranslation } from 'react-i18next';

// Distintivo "En lista de espera" para una solicitud PENDING con waitlisted=true
// (capability request-waitlist, employee-portal spec §2). Reutilizable desde
// "Mis solicitudes" y "Mi Semana". NUNCA muestra posición numérica en la cola
// (fuera de alcance, proposal §Out of scope).
export function WaitlistBadge() {
  const { t } = useTranslation();
  return (
    <span className="pill pill-amber waitlist-badge">
      <i className="ti ti-clock" aria-hidden="true" />
      {t('requests.mine.waitlisted')}
    </span>
  );
}
