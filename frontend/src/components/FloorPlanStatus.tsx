import { useTranslation } from 'react-i18next';
import { Spinner } from './Spinner';
import { isOutsideWindowError } from '../utils/floorPlan';

interface FloorPlanStatusProps {
  isDateValid: boolean;
  isLoading: boolean;
  isError: boolean;
  error: unknown;
}

// Estados no-felices del plano: fecha inválida, carga y error (ventana vs genérico).
// Devuelve null cuando el plano puede renderizarse.
export function FloorPlanStatus({ isDateValid, isLoading, isError, error }: FloorPlanStatusProps) {
  const { t } = useTranslation();

  if (!isDateValid) {
    return (
      <p className="form-hint" role="status">
        {t('floorPlan.invalidDate')}
      </p>
    );
  }
  if (isLoading) {
    return <Spinner />;
  }
  if (isError) {
    return (
      <p className="form-error" role="alert">
        {t(isOutsideWindowError(error) ? 'floorPlan.outsideWindow' : 'floorPlan.loadError')}
      </p>
    );
  }
  return null;
}
