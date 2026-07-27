import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { PageHeader } from '../components/PageHeader';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import {
  useSettingsQuery,
  useUpdateApprovalMode,
  useUpdateParkingAddress,
  useUpdateWeekendReservable,
} from '../hooks/useSettings';
import type { ApprovalMode } from '../types/settings';

const MODES: ApprovalMode[] = ['MANUAL', 'AUTOMATIC'];

// Icono Tabler por modo (mapa fijo, sin literales repetidos S1192).
const MODE_ICON: Record<ApprovalMode, string> = {
  MANUAL: 'user-check',
  AUTOMATIC: 'bolt',
};

// Tesela de un modo de aprobación: ahora es SELECCIONABLE (clic elige el modo).
// Muestra un check cuando está elegida; sin selector aparte.
function ModeTile({
  mode,
  active,
  onSelect,
}: {
  mode: ApprovalMode;
  active: boolean;
  onSelect: () => void;
}) {
  const { t } = useTranslation();
  return (
    <button
      type="button"
      className={`settings-mode${active ? ' is-active' : ''}`}
      aria-pressed={active}
      onClick={onSelect}
    >
      <div className="settings-mode-head">
        <i className={`ti ti-${MODE_ICON[mode]}`} aria-hidden="true" />
        <span className="settings-mode-name">{t(`settings.approvalMode.short.${mode}`)}</span>
        <span className="settings-mode-check" aria-hidden="true">
          <i className={`ti ti-${active ? 'circle-check' : 'circle'}`} />
        </span>
      </div>
      <p className="settings-mode-hint">{t(`settings.approvalMode.hints.${mode}`)}</p>
    </button>
  );
}

// Tarjeta ADMIN: activar/desactivar reservas en fin de semana. Guarda al cambiar el
// toggle (self-contained para no cargar la complejidad de SettingsPage, S3776).
function WeekendReservableCard({ enabled }: { enabled: boolean }) {
  const { t } = useTranslation();
  const mutation = useUpdateWeekendReservable();
  // Draft local: NO se autoguarda; se persiste con el botón Guardar.
  const [draft, setDraft] = useState<boolean | null>(null);
  const value = draft ?? enabled;
  const isDirty = value !== enabled;

  async function save(): Promise<void> {
    try {
      await mutation.mutateAsync(value);
      setDraft(null);
      emitApiErrorToast('settings.saved', 'success');
    } catch {
      emitApiErrorToast('settings.saveError');
    }
  }

  return (
    <div className="settings-card settings-approval">
      <div className="settings-hero">
        <span className="settings-hero-icon">
          <i className="ti ti-calendar-week" aria-hidden="true" />
        </span>
        <div>
          <h2 className="settings-hero-title">{t('settings.weekend.heading')}</h2>
          <p className="settings-hero-desc">{t('settings.weekend.description')}</p>
        </div>
      </div>
      <div className="switch-field">
        <button
          type="button"
          role="switch"
          aria-checked={value}
          className={`switch${value ? ' is-on' : ''}`}
          onClick={() => setDraft(!value)}
        >
          <span className="switch-knob" />
        </button>
        <span>{t('settings.weekend.label')}</span>
      </div>
      <p className="hint">{t('settings.weekend.hint')}</p>
      <div className="settings-actions">
        <Button
          variant="green"
          icon="check"
          disabled={!isDirty || mutation.isPending}
          onClick={() => void save()}
        >
          {t('settings.save')}
        </Button>
      </div>
    </div>
  );
}

// Tarjeta ADMIN: dirección del parking (form con guardado). Self-contained (S3776).
function ParkingAddressCard({ current }: { current: string }) {
  const { t } = useTranslation();
  const mutation = useUpdateParkingAddress();
  // El botón "Ir al parking" se activa con un toggle; solo entonces aparece el
  // campo de dirección. Draft local (no autoguarda): se persiste con Guardar.
  const currentActive = current.trim() !== '';
  const [active, setActive] = useState<boolean | null>(null);
  const [draft, setDraft] = useState<string | null>(null);
  const isActive = active ?? currentActive;
  const value = draft ?? current;
  const trimmed = value.trim();
  const isDirty = isActive !== currentActive || (isActive && trimmed !== current.trim());
  // Con el botón activo hay que dar una dirección para poder guardar.
  const canSave = isDirty && (!isActive || trimmed !== '');

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    if (!canSave) {
      return;
    }
    try {
      await mutation.mutateAsync(isActive ? trimmed : null);
      setDraft(null);
      setActive(null);
      emitApiErrorToast('settings.saved', 'success');
    } catch {
      emitApiErrorToast('settings.saveError');
    }
  }

  return (
    <form className="settings-card settings-approval" onSubmit={handleSubmit}>
      <div className="settings-hero">
        <span className="settings-hero-icon">
          <i className="ti ti-map-pin" aria-hidden="true" />
        </span>
        <div>
          <h2 className="settings-hero-title">{t('settings.parkingAddress.heading')}</h2>
          <p className="settings-hero-desc">{t('settings.parkingAddress.description')}</p>
        </div>
      </div>
      <div className="switch-field">
        <button
          type="button"
          role="switch"
          aria-checked={isActive}
          className={`switch${isActive ? ' is-on' : ''}`}
          onClick={() => setActive(!isActive)}
        >
          <span className="switch-knob" />
        </button>
        <span>{t('settings.parkingAddress.enableLabel')}</span>
      </div>
      {isActive ? (
        <div className="settings-control">
          <label className="field-label" htmlFor="parking-address">
            {t('settings.parkingAddress.label')}
          </label>
          <input
            id="parking-address"
            type="text"
            className="field-input"
            maxLength={500}
            placeholder={t('settings.parkingAddress.placeholder')}
            value={value}
            onChange={(event) => setDraft(event.target.value)}
          />
        </div>
      ) : null}
      <div className="settings-actions">
        <Button variant="green" icon="check" submit disabled={!canSave || mutation.isPending}>
          {t('settings.save')}
        </Button>
      </div>
    </form>
  );
}

// Vista ADMIN: configuracion global del sistema. Permite consultar y conmutar el
// modo de aprobacion de solicitudes (MANUAL/AUTOMATIC) contra GET/PUT /admin/settings
// (tasks §6.2). RBAC ADMIN garantizado por la ruta protegida.
export function SettingsPage() {
  const { t } = useTranslation();
  const query = useSettingsQuery();
  const updateMutation = useUpdateApprovalMode();
  const [selected, setSelected] = useState<ApprovalMode | null>(null);

  const currentMode = query.data?.approvalMode ?? null;
  // Hasta que el admin toque el selector, refleja el valor del servidor.
  const value: ApprovalMode = selected ?? currentMode ?? 'MANUAL';
  const isDirty = currentMode !== null && value !== currentMode;
  const ready = !query.isLoading && !query.isError;

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    if (!isDirty) {
      return;
    }
    try {
      await updateMutation.mutateAsync(value);
      setSelected(null);
      emitApiErrorToast('settings.saved');
    } catch {
      emitApiErrorToast('settings.saveError');
    }
  }

  return (
    <section className="settings-page" aria-label={t('settings.title')}>
      <PageHeader
        eyebrow={t('settings.eyebrow')}
        title={t('settings.title')}
        description={t('settings.description')}
      />

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('settings.loadError')}
        </p>
      ) : null}

      {ready ? (
        <form className="settings-card settings-approval" onSubmit={handleSubmit}>
          <div className="settings-hero">
            <span className="settings-hero-icon">
              <i className="ti ti-checklist" aria-hidden="true" />
            </span>
            <div>
              <h2 className="settings-hero-title">{t('settings.approvalMode.heading')}</h2>
              <p className="settings-hero-desc">{t('settings.approvalMode.description')}</p>
            </div>
          </div>

          <div className="settings-modes">
            {MODES.map((mode) => (
              <ModeTile
                key={mode}
                mode={mode}
                active={value === mode}
                onSelect={() => setSelected(mode)}
              />
            ))}
          </div>

          <div className="settings-actions">
            <Button
              variant="green"
              icon="check"
              submit
              disabled={!isDirty || updateMutation.isPending}
            >
              {t('settings.save')}
            </Button>
          </div>
        </form>
      ) : null}

      {ready ? (
        <ParkingAddressCard current={query.data?.parkingAddress ?? ''} />
      ) : null}

      {ready ? (
        <WeekendReservableCard enabled={query.data?.weekendReservable ?? false} />
      ) : null}
    </section>
  );
}
