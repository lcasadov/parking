import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useCreateRequest } from '../hooks/useRequests';
import { isWithinWindow, maxRequestDateIso, todayIso } from '../utils/requests';

interface CreateRequestModalProps {
  onClose: () => void;
  onCreated: () => void;
}

const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (400 ventana / 409 duplicado) a la clave i18n del
// toast correspondiente (tasks §4.4).
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'requests.errors.duplicate';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'requests.errors.window';
  }
  return 'requests.errors.generic';
}

// Modal EMPLOYEE: crea una solicitud para una fecha dentro de la ventana
// hoy..hoy+14 (POST /requests). El input restringe min/max y se valida ademas
// en cliente antes de enviar.
export function CreateRequestModal({ onClose, onCreated }: CreateRequestModalProps) {
  const { t } = useTranslation();
  const [date, setDate] = useState('');
  const [error, setError] = useState<string | null>(null);
  const createMutation = useCreateRequest();

  function validate(): string | null {
    if (date === '') {
      return t('requests.create.requiredDate');
    }
    if (!isWithinWindow(date)) {
      return t('requests.create.outsideWindow');
    }
    return null;
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    createMutation.mutate(
      { requestedDate: date },
      {
        onSuccess: onCreated,
        onError: (mutationError) => emitApiErrorToast(toastKeyForError(mutationError)),
      },
    );
  }

  return (
    <Modal title={t('requests.create.title')} onClose={onClose}>
      <form id="create-request-form" onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="create-request-date">
          {t('requests.create.date')}
        </label>
        <input
          id="create-request-date"
          type="date"
          className="field-input"
          value={date}
          min={todayIso()}
          max={maxRequestDateIso()}
          onChange={(event) => setDate(event.target.value)}
        />
        <p className="hint">{t('requests.create.hint')}</p>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}

        <div className="modal-footer-inline">
          <Button variant="white" onClick={onClose}>
            {t('requests.create.cancel')}
          </Button>
          <Button variant="green" submit disabled={createMutation.isPending}>
            {t('requests.create.submit')}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
