import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Modal } from './Modal';
import { useRevokeFixedAssignment } from '../hooks/useFixedAssignments';

interface RevokeFixedAssignmentModalProps {
  employeeId: number;
  employeeName: string;
  onClose: () => void;
  onRevoked: () => void;
}

// Modal ADMIN de confirmacion de revocacion logica (DELETE por empleado).
export function RevokeFixedAssignmentModal({
  employeeId,
  employeeName,
  onClose,
  onRevoked,
}: RevokeFixedAssignmentModalProps) {
  const { t } = useTranslation();
  const revokeMutation = useRevokeFixedAssignment();

  function handleConfirm(): void {
    revokeMutation.mutate(employeeId, { onSuccess: onRevoked });
  }

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('fixedAssignments.revoke.cancel')}
      </Button>
      <Button variant="red" onClick={handleConfirm} disabled={revokeMutation.isPending}>
        {t('fixedAssignments.revoke.confirm')}
      </Button>
    </>
  );

  return (
    <Modal title={t('fixedAssignments.revoke.title')} onClose={onClose} variant="red" footer={footer}>
      <p>{t('fixedAssignments.revoke.body', { name: employeeName })}</p>
    </Modal>
  );
}
