import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { InfoBanner } from './InfoBanner';
import { useResetEmployeePassword } from '../hooks/useEmployees';
import type { Employee } from '../types/employee';

interface ResetPasswordModalProps {
  employee: Employee;
  onClose: () => void;
}

// Modal de reset de contraseña (mockup 15), sobre la primitiva Dialog (Ola A).
// Fase 1: tras confirmar, muestra la contraseña temporal UNA sola vez. Fase 2:
// llega sin temporal (email).
export function ResetPasswordModal({ employee, onClose }: ResetPasswordModalProps) {
  const { t } = useTranslation();
  const resetMutation = useResetEmployeePassword();
  const [copied, setCopied] = useState(false);
  const fullName = `${employee.firstName} ${employee.lastName}`.trim();
  const result = resetMutation.data;

  function onOpenChange(next: boolean): void {
    if (!next) {
      onClose();
    }
  }

  function handleConfirm(): void {
    resetMutation.mutate(employee.id);
  }

  async function handleCopy(password: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(password);
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }

  if (result) {
    const tempPassword = result.temporaryPassword;
    const doneFooter = (
      <>
        <Button variant="white" onClick={onClose}>
          {t('employees.reset.close')}
        </Button>
        <Button variant="green" icon="check" onClick={onClose}>
          {t('employees.reset.done')}
        </Button>
      </>
    );
    return (
      <Dialog
        open
        onOpenChange={onOpenChange}
        title={t('employees.reset.title')}
        icon="key"
        tone="green"
        narrow
        footer={doneFooter}
      >
        <p className="muted">{t('employees.reset.intro', { name: fullName })}</p>
        {tempPassword ? (
          <>
            <InfoBanner variant="amber" icon="alert-triangle">
              {t('employees.reset.onceWarning')}
            </InfoBanner>
            <div className="field-label">{t('employees.reset.tempPasswordLabel')}</div>
            <div className="field-value with-icon temp-password">
              <span aria-label={t('employees.reset.tempPasswordLabel')}>{tempPassword}</span>
              <Button variant="white" icon="copy" onClick={() => void handleCopy(tempPassword)}>
                {copied ? t('employees.reset.copied') : t('employees.reset.copy')}
              </Button>
            </div>
            <p className="hint">{t('employees.reset.mustChangeNote')}</p>
          </>
        ) : (
          <InfoBanner variant="blue" icon="mail">
            {t('employees.reset.phase2Note')}
          </InfoBanner>
        )}
      </Dialog>
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('employees.reset.close')}
      </Button>
      <Button variant="green" icon="key" onClick={handleConfirm} disabled={resetMutation.isPending}>
        {t('employees.reset.confirm')}
      </Button>
    </>
  );

  return (
    <Dialog
      open
      onOpenChange={onOpenChange}
      title={t('employees.reset.title')}
      icon="key"
      tone="green"
      narrow
      footer={footer}
    >
      <p>{t('employees.reset.confirmBody', { name: fullName })}</p>
      <InfoBanner variant="blue" icon="info-circle">
        {t('employees.reset.phase2Note')}
      </InfoBanner>
      {resetMutation.isError ? (
        <p className="form-error" role="alert">
          {t('employees.reset.error')}
        </p>
      ) : null}
    </Dialog>
  );
}
