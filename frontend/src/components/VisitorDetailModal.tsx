import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { Spinner } from './Spinner';
import { useVisitorQuery } from '../hooks/useVisitors';

interface VisitorDetailModalProps {
  visitorId: number;
  onClose: () => void;
}

// Modal ADMIN: detalle de una ficha de visitante (GET /visitors/{id}, tasks §4.3).
export function VisitorDetailModal({ visitorId, onClose }: VisitorDetailModalProps) {
  const { t } = useTranslation();
  const query = useVisitorQuery(visitorId);
  const visitor = query.data;
  const none = t('visitors.detail.none');

  const footer = (
    <Button variant="white" onClick={onClose}>
      {t('visitors.detail.close')}
    </Button>
  );

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t('visitors.detail.title')}
      footer={footer}
    >
      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('visitors.detail.loadError')}
        </p>
      ) : null}

      {visitor ? (
        <dl className="detail-list">
          <dt>{t('visitors.detail.firstName')}</dt>
          <dd>{visitor.firstName}</dd>
          <dt>{t('visitors.detail.lastName')}</dt>
          <dd>{visitor.lastName}</dd>
          <dt>{t('visitors.detail.nationalId')}</dt>
          <dd>{visitor.nationalId}</dd>
          <dt>{t('visitors.detail.licensePlate')}</dt>
          <dd>{visitor.licensePlate ?? none}</dd>
          <dt>{t('visitors.detail.company')}</dt>
          <dd>{visitor.company ?? none}</dd>
          <dt>{t('visitors.detail.usualReason')}</dt>
          <dd>{visitor.usualReason ?? none}</dd>
        </dl>
      ) : null}
    </Dialog>
  );
}
