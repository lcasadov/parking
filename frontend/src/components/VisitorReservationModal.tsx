import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { FieldRow } from './FieldRow';
import { InfoBanner } from './InfoBanner';
import { getStatus } from '../api/apiError';
import { useCreateVisitorReservation } from '../hooks/useVisitorReservations';
import { useVisitorsQuery } from '../hooks/useVisitors';
import { useParkingSpacesQuery } from '../hooks/useParkingSpaces';
import { todayIso } from '../utils/visitors';
import type { Visitor } from '../types/visitor';

interface VisitorReservationModalProps {
  visitor?: Visitor | null;
  onClose: () => void;
  onCreated: () => void;
}

const FORM_ID = 'visitor-reservation-form';
const LIST_SIZE = 100;
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor (409 plaza ocupada / 400 validacion) a la clave
// i18n del mensaje inline (tasks §4.4).
function errorKeyForStatus(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'visitors.reservations.errors.occupied';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'visitors.reservations.errors.validation';
  }
  return 'visitors.reservations.errors.generic';
}

// Modal ADMIN: crea una reserva de plaza para un visitante en una fecha
// (POST /visitor-reservations). La plaza ocupada devuelve 409 -> error inline.
export function VisitorReservationModal({
  visitor,
  onClose,
  onCreated,
}: VisitorReservationModalProps) {
  const { t } = useTranslation();
  const [visitorId, setVisitorId] = useState<number | ''>(visitor?.id ?? '');
  const [parkingSpaceId, setParkingSpaceId] = useState<number | ''>('');
  const [date, setDate] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);

  const visitorsQuery = useVisitorsQuery({ page: 0, size: LIST_SIZE });
  const spacesQuery = useParkingSpacesQuery({ page: 0, size: LIST_SIZE, active: true });
  const createMutation = useCreateVisitorReservation();

  const visitors = visitorsQuery.data?.content ?? [];
  const spaces = spacesQuery.data?.content ?? [];
  const lockVisitor = Boolean(visitor);

  function validate(): string | null {
    if (visitorId === '') {
      return t('visitors.reservations.create.requiredVisitor');
    }
    if (date === '') {
      return t('visitors.reservations.create.requiredDate');
    }
    if (parkingSpaceId === '') {
      return t('visitors.reservations.create.requiredSpace');
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
      {
        visitorId: Number(visitorId),
        parkingSpaceId: Number(parkingSpaceId),
        reservationDate: date,
        notes: notes.trim() || undefined,
      },
      {
        onSuccess: onCreated,
        onError: (mutationError) => setError(t(errorKeyForStatus(mutationError))),
      },
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('visitors.reservations.create.cancel')}
      </Button>
      <Button variant="green" submit form={FORM_ID} disabled={createMutation.isPending}>
        {t('visitors.reservations.create.submit')}
      </Button>
    </>
  );

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t('visitors.reservations.create.title')}
      footer={footer}
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <label className="field-label" htmlFor="visitor-reservation-visitor">
          {t('visitors.reservations.create.visitor')}
        </label>
        <select
          id="visitor-reservation-visitor"
          className="field-input"
          value={visitorId}
          disabled={lockVisitor}
          onChange={(event) =>
            setVisitorId(event.target.value === '' ? '' : Number(event.target.value))
          }
        >
          <option value="">{t('visitors.reservations.create.selectVisitor')}</option>
          {visitors.map((item) => (
            <option key={item.id} value={item.id}>
              {`${item.firstName} ${item.lastName} · ${item.nationalId}`}
            </option>
          ))}
        </select>

        <FieldRow>
          <div className="auth-field">
            <label className="field-label" htmlFor="visitor-reservation-date">
              {t('visitors.reservations.create.date')}
            </label>
            <input
              id="visitor-reservation-date"
              type="date"
              className="field-input"
              value={date}
              min={todayIso()}
              onChange={(event) => setDate(event.target.value)}
            />
          </div>
          <div className="auth-field">
            <label className="field-label" htmlFor="visitor-reservation-space">
              {t('visitors.reservations.create.space')}
            </label>
            <select
              id="visitor-reservation-space"
              className="field-input"
              value={parkingSpaceId}
              onChange={(event) =>
                setParkingSpaceId(event.target.value === '' ? '' : Number(event.target.value))
              }
            >
              <option value="">{t('visitors.reservations.create.selectSpace')}</option>
              {spaces.map((space) => (
                <option key={space.id} value={space.id}>
                  {space.label}
                </option>
              ))}
            </select>
            <p className="hint">{t('visitors.reservations.create.spaceHint')}</p>
          </div>
        </FieldRow>

        <label className="field-label" htmlFor="visitor-reservation-notes">
          {t('visitors.reservations.create.notes')}
        </label>
        <textarea
          id="visitor-reservation-notes"
          className="field-input"
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
        />

        <InfoBanner variant="blue" icon="info-circle">
          {t('visitors.reservations.create.banner')}
        </InfoBanner>

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
