import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { useResetEmployeePassword } from '../hooks/useEmployees';
import type { Employee } from '../types/employee';

interface ResetPasswordModalProps {
  employee: Employee;
  onClose: () => void;
}

// Modal de reset de contraseña. Fase 1: tras confirmar, muestra la contraseña
// temporal UNA sola vez (ui-screens §12). Fase 2: llega sin temporal (email).
export function ResetPasswordModal({ employee, onClose }: ResetPasswordModalProps) {
  const { t } = useTranslation();
  const resetMutation = useResetEmployeePassword();
  const [copied, setCopied] = useState(false);
  const fullName = `${employee.firstName} ${employee.lastName}`.trim();
  const result = resetMutation.data;

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
    return (
      <Modal title={t('employees.reset.title')} onClose={onClose}>
        <p>{t('employees.reset.intro', { name: fullName })}</p>
        {tempPassword ? (
          <>
            <p className="reset-warning">{t('employees.reset.onceWarning')}</p>
            <div className="reset-password-row">
              <code className="reset-password" aria-label={t('employees.reset.tempPasswordLabel')}>
                {tempPassword}
              </code>
              <Button variant="blue" icon="copy" onClick={() => void handleCopy(tempPassword)}>
                {copied ? t('employees.reset.copied') : t('employees.reset.copy')}
              </Button>
            </div>
            <p className="hint">{t('employees.reset.mustChangeNote')}</p>
          </>
        ) : (
          <p className="hint">{t('employees.reset.phase2Note')}</p>
        )}
        <div className="modal-footer-inline">
          <Button variant="green" onClick={onClose}>
            {t('employees.reset.close')}
          </Button>
        </div>
      </Modal>
    );
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('employees.reset.close')}
      </Button>
      <Button variant="green" onClick={handleConfirm} disabled={resetMutation.isPending}>
        {t('employees.reset.confirm')}
      </Button>
    </>
  );

  return (
    <Modal title={t('employees.reset.title')} onClose={onClose} footer={footer}>
      <p>{t('employees.reset.confirmBody', { name: fullName })}</p>
      <p className="hint">{t('employees.reset.phase2Note')}</p>
      {resetMutation.isError ? (
        <p className="form-error" role="alert">
          {t('employees.reset.error')}
        </p>
      ) : null}
    </Modal>
  );
}
