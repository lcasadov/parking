import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { useParkingAddressQuery } from '../hooks/useSettings';

interface ParkingDirectionsButtonProps {
  className?: string;
}

// Botón "Ir al parking" (Feature C): abre Google Maps con la ubicación del parking
// configurada por el admin. Si hay coordenadas exactas (punto fijado en el mapa) navega
// a ellas —más preciso que geocodificar el texto—; si no, cae a la dirección postal. Se
// muestra SOLO si hay ubicación configurada; si no, no renderiza nada.
export function ParkingDirectionsButton({ className }: ParkingDirectionsButtonProps) {
  const { t } = useTranslation();
  const { data } = useParkingAddressQuery();
  const address = data?.address?.trim();
  const hasCoords = data?.lat != null && data?.lng != null;

  if (!hasCoords && !address) {
    return null;
  }

  const destination = hasCoords ? `${data?.lat},${data?.lng}` : (address as string);
  const href = `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(destination)}`;

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
