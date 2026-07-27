import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { useParkingAddressQuery } from '../hooks/useSettings';

interface ParkingDirectionsButtonProps {
  className?: string;
}

// Botón "Ir al parking" (Feature C): abre Google Maps con la dirección del parking
// configurada por el admin (system-settings.parkingAddress). Se muestra SOLO si hay
// dirección configurada; si no, no renderiza nada (no tiene sentido navegar a nada).
export function ParkingDirectionsButton({ className }: ParkingDirectionsButtonProps) {
  const { t } = useTranslation();
  const { data } = useParkingAddressQuery();
  const address = data?.trim();

  if (!address) {
    return null;
  }

  const href = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(address)}`;

  return (
    <Button
      variant="white"
      icon="navigation"
      className={className}
      onClick={() => window.open(href, '_blank', 'noopener,noreferrer')}
    >
      {t('calendar.myWeek.goToParking')}
    </Button>
  );
}
