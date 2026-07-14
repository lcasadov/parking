import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '../components/Button';
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
    <section className="settings-page" aria-labelledby="settings-title">
      <header className="page-header">
        <h1 id="settings-title" className="section-title">
          {t('settings.title')}
        </h1>
      </header>

      {query.isLoading ? <Spinner /> : null}

      {query.isError ? (
        <p className="form-error" role="alert">
          {t('settings.loadError')}
        </p>
      ) : null}

      {!query.isLoading && !query.isError ? (
        <form className="settings-form" onSubmit={handleSubmit}>
          <p className="hint">{t('settings.approvalMode.description')}</p>

          <label className="field-label" htmlFor="approval-mode">
            {t('settings.approvalMode.label')}
          </label>
          <select
            id="approval-mode"
            className="field-input"
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

          <div className="page-actions">
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
