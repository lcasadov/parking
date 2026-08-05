import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { ZOOM_MAX, ZOOM_MIN } from '../utils/floorPlan';

interface FloorPlanZoomProps {
  scale: number;
  onZoomIn: () => void;
  onZoomOut: () => void;
  onReset: () => void;
}

// Controles de zoom del plano: acercar / alejar / restablecer. La escala se
// aplica como transform CSS sobre el viewport (ver FloorPlanSurface).
export function FloorPlanZoom({ scale, onZoomIn, onZoomOut, onReset }: FloorPlanZoomProps) {
  const { t } = useTranslation();
  const percent = Math.round(scale * 100);
  return (
    <div className="plano-zoom" role="group" aria-label={t('floorPlan.zoom.label')}>
      <Button
        variant="white"
        icon="minus"
        aria-label={t('floorPlan.zoom.out')}
        disabled={scale <= ZOOM_MIN}
        onClick={onZoomOut}
      />
      <span className="plano-zoom-value" aria-live="polite">
        {t('floorPlan.zoom.level', { percent })}
      </span>
      <Button
        variant="white"
        icon="plus"
        aria-label={t('floorPlan.zoom.in')}
        disabled={scale >= ZOOM_MAX}
        onClick={onZoomIn}
      />
      <Button
        variant="white"
        icon="refresh"
        aria-label={t('floorPlan.zoom.reset')}
        title={t('floorPlan.zoom.reset')}
        onClick={onReset}
      />
    </div>
  );
}
