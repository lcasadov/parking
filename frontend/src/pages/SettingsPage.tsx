import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
import { PageHeader } from '../components/PageHeader';
import { Spinner } from '../components/Spinner';
import { emitApiErrorToast } from '../api/events';
import { useSettingsQuery, useUpdateApprovalMode } from '../hooks/useSettings';
import type { ApprovalMode } from '../types/settings';

const MODES: ApprovalMode[] = ['MANUAL', 'AUTOMATIC'];

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
        <form className="settings-card" onSubmit={handleSubmit}>
          <div className="settings-field">
            <div className="settings-field-head">
              <i className="ti ti-checklist" aria-hidden="true" />
              <div>
                <label className="settings-field-title" htmlFor="approval-mode">
                  {t('settings.approvalMode.label')}
                </label>
                <p className="settings-field-desc">{t('settings.approvalMode.description')}</p>
              </div>
            </div>

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
            <p className="hint">{t(`settings.approvalMode.hints.${value}`)}</p>
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
