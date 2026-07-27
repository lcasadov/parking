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

// Tesela explicativa de un modo de aprobación (presentacional): icono + nombre
// corto + explicación; resalta el modo actualmente seleccionado con la insignia
// «Modo actual». Refleja el valor elegido; el control real es el <select>.
function ModeTile({ mode, active }: { mode: ApprovalMode; active: boolean }) {
  const { t } = useTranslation();
  return (
    <div className={`settings-mode${active ? ' is-active' : ''}`}>
      <div className="settings-mode-head">
        <i className={`ti ti-${MODE_ICON[mode]}`} aria-hidden="true" />
        <span className="settings-mode-name">{t(`settings.approvalMode.short.${mode}`)}</span>
        {active ? (
          <span className="settings-mode-badge">{t('settings.approvalMode.activeBadge')}</span>
        ) : null}
      </div>
      <p className="settings-mode-hint">{t(`settings.approvalMode.hints.${mode}`)}</p>
    </div>
  );
}

// Tarjeta ADMIN: activar/desactivar reservas en fin de semana. Guarda al cambiar el
// toggle (self-contained para no cargar la complejidad de SettingsPage, S3776).
function WeekendReservableCard({ enabled }: { enabled: boolean }) {
  const { t } = useTranslation();
  const mutation = useUpdateWeekendReservable();

  async function toggle(next: boolean): Promise<void> {
    try {
      await mutation.mutateAsync(next);
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
      <label className="checkbox-field">
        <input
          type="checkbox"
          checked={enabled}
          disabled={mutation.isPending}
          onChange={(event) => void toggle(event.target.checked)}
        />
        <span>{t('settings.weekend.label')}</span>
      </label>
      <p className="hint">{t('settings.weekend.hint')}</p>
    </div>
  );
}

// Tarjeta ADMIN: dirección del parking (form con guardado). Self-contained (S3776).
function ParkingAddressCard({ current }: { current: string }) {
  const { t } = useTranslation();
  const mutation = useUpdateParkingAddress();
  const [draft, setDraft] = useState<string | null>(null);
  const value = draft ?? current;
  const isDirty = value.trim() !== current.trim();

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    if (!isDirty) {
      return;
    }
    try {
      await mutation.mutateAsync(value.trim() === '' ? null : value.trim());
      setDraft(null);
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
        <p className="hint">{t('settings.parkingAddress.hint')}</p>
      </div>
      <div className="settings-actions">
        <Button variant="green" icon="check" submit disabled={!isDirty || mutation.isPending}>
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
              <ModeTile key={mode} mode={mode} active={value === mode} />
            ))}
          </div>

          <div className="settings-control">
            <label className="field-label" htmlFor="approval-mode">
              {t('settings.approvalMode.label')}
            </label>
            <select
              id="approval-mode"
              className="field-input settings-select"
              value={value}
              onChange={(event) => setSelected(event.target.value as ApprovalMode)}
            >
              {MODES.map((mode) => (
                <option key={mode} value={mode}>
                  {t(`settings.approvalMode.options.${mode}`)}
                </option>
              ))}
            </select>
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
