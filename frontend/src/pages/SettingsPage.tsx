import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { PageHeader } from '../components/PageHeader';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import { useSettingsQuery, useUpdateApprovalMode } from '../hooks/useSettings';
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

      {!query.isLoading && !query.isError ? (
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
    </section>
  );
}
