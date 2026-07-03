import { useId, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { getFieldErrors, getStatus } from '../api/apiError';
import { useConfigureParkingSpaces } from '../hooks/useParkingSpaces';

const HTTP_BAD_REQUEST = 400;

// Tarjeta "Configuracion del parking": ajusta el numero total de plazas
// (POST /parking-spaces/configure) con manejo de 400.
export function ConfigureParkingCard() {
  const { t } = useTranslation();
  const [total, setTotal] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const totalId = useId();
  const configureMutation = useConfigureParkingSpaces();

  function handleError(err: unknown): void {
    const fields = getFieldErrors(err);
    if (getStatus(err) === HTTP_BAD_REQUEST && fields.total) {
      setError(fields.total);
    } else {
      setError(t('parkingSpaces.configure.invalidTotal'));
    }
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    setSuccess(false);
    const parsed = Number(total);
    if (total.trim() === '' || !Number.isInteger(parsed) || parsed < 0) {
      setError(t('parkingSpaces.configure.invalidTotal'));
      return;
    }
    setError('');
    configureMutation.mutate(
      { total: parsed },
      {
        onSuccess: () => setSuccess(true),
        onError: handleError,
      },
    );
  }

  return (
    <section className="config-card" aria-labelledby="configure-title">
      <h2 id="configure-title" className="card-title">
        {t('parkingSpaces.configure.title')}
      </h2>
      <p className="card-hint">{t('parkingSpaces.configure.hint')}</p>
      <form className="configure-form" onSubmit={handleSubmit} noValidate>
        <div className="auth-field">
          <label className="field-label" htmlFor={totalId}>
            {t('parkingSpaces.configure.totalLabel')}
          </label>
          <input
            id={totalId}
            type="number"
            min={0}
            className={`field-input${error ? ' danger' : ''}`}
            value={total}
            aria-invalid={error ? true : undefined}
            onChange={(event) => {
              setTotal(event.target.value);
              setError('');
            }}
          />
        </div>
        <Button variant="green" submit disabled={configureMutation.isPending}>
          {t('parkingSpaces.configure.apply')}
        </Button>
      </form>
      {error ? (
        <p className="form-error" role="alert">
          {error}
        </p>
      ) : null}
      {success ? (
        <p className="form-success" role="status">
          {t('parkingSpaces.configure.applied')}
        </p>
      ) : null}
    </section>
  );
}
