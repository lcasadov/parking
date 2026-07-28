import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { SelectField, type SelectOption } from './SelectField';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { useAdminReassignRequest, useAdminSwapRequests } from '../hooks/useRequests';
import { longDate } from '../utils/calendar';
import type { ResourceType } from '../types/request';

// Recurso libre (de la misma fecha y tipo) al que puede moverse la reserva.
export interface ReassignTarget {
  resourceId: number;
  label: string;
}

// Otra reserva APPROVED de la misma fecha y tipo, candidata a un intercambio.
export interface SwapTarget {
  requestId: number;
  resourceLabel: string;
  employeeName: string;
}

// Datos de la reserva seleccionada en la rejilla de Ocupación (celda REQUEST_APPROVED).
export interface RequestManagePrefill {
  requestId: number;
  employeeName: string;
  resourceLabel: string;
  date: string;
  resourceType: ResourceType;
}

// Acción activa del modal: reasignar a un recurso libre, intercambiar con otra
// reserva, o liberar (delega en el flujo de cancelación con motivo existente).
type ManageAction = 'REASSIGN' | 'SWAP' | 'CANCEL';

interface RequestManageModalProps {
  prefill: RequestManagePrefill;
  reassignTargets: ReassignTarget[];
  swapTargets: SwapTarget[];
  // Notifica éxito con la clave i18n del toast; el padre refresca y cierra.
  onDone: (messageKey: string) => void;
  // Delega en el modal de cancelación (motivo obligatorio) ya existente.
  onRequestCancel: () => void;
  onClose: () => void;
}

const FORM_ID = 'request-manage-form';
const HTTP_BAD_REQUEST = 400;
const HTTP_CONFLICT = 409;

// Traduce el error del servidor a la clave i18n del toast. El 409 en reasignación
// significa recurso ya no libre; en swap, estado incompatible de alguna reserva.
function toastKeyForError(error: unknown): string {
  const status = getStatus(error);
  if (status === HTTP_CONFLICT) {
    return 'occupancy.manage.errors.conflict';
  }
  if (status === HTTP_BAD_REQUEST) {
    return 'occupancy.manage.errors.badRequest';
  }
  return 'occupancy.manage.errors.generic';
}

// Panel de la acción activa: selector de recurso libre (REASSIGN), selector de otra
// reserva (SWAP) o aviso informativo (CANCEL). Extraído para no cargar la
// complejidad cognitiva del componente principal (S3776).
function ManagePanel({
  action,
  reassignOptions,
  swapOptions,
  value,
  onChange,
  t,
}: {
  action: ManageAction;
  reassignOptions: SelectOption[];
  swapOptions: SelectOption[];
  value: string;
  onChange: (next: string) => void;
  t: TFunction;
}) {
  if (action === 'CANCEL') {
    return <p className="hint">{t('occupancy.manage.cancel.hint')}</p>;
  }
  const isReassign = action === 'REASSIGN';
  const options = isReassign ? reassignOptions : swapOptions;
  const prefix = isReassign ? 'occupancy.manage.reassign' : 'occupancy.manage.swap';
  if (options.length === 0) {
    return <p className="hint">{t(`${prefix}.empty`)}</p>;
  }
  return (
    <>
      <SelectField
        label={t(`${prefix}.select`)}
        value={value}
        onValueChange={onChange}
        options={options}
        placeholder={t(`${prefix}.placeholder`)}
      />
      <p className="hint">{t(`${prefix}.hint`)}</p>
    </>
  );
}

// Modal ADMIN "Gestionar reserva" (capability admin-resource-reassignment): se abre
// desde una celda REQUEST_APPROVED de la rejilla de Ocupación y ofrece tres acciones:
//   - REASSIGN: POST /requests/admin/reassign → mueve la reserva a un recurso libre.
//   - SWAP: POST /requests/admin/swap → intercambia el recurso con otra reserva.
//   - CANCEL: delega en el modal de cancelación (motivo) ya existente.
// Los candidatos (recursos libres / otras reservas) se derivan de la propia rejilla,
// por lo que el modal no dispara peticiones extra al abrirse.
export function RequestManageModal({
  prefill,
  reassignTargets,
  swapTargets,
  onDone,
  onRequestCancel,
  onClose,
}: RequestManageModalProps) {
  const { t, i18n } = useTranslation();
  const [action, setAction] = useState<ManageAction>('REASSIGN');
  const [value, setValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const reassignMutation = useAdminReassignRequest();
  const swapMutation = useAdminSwapRequests();

  const reassignOptions: SelectOption[] = reassignTargets.map((target) => ({
    value: String(target.resourceId),
    label: target.label,
  }));
  const swapOptions: SelectOption[] = swapTargets.map((target) => ({
    value: String(target.requestId),
    label: t('occupancy.manage.swap.option', {
      resource: target.resourceLabel,
      employee: target.employeeName,
    }),
  }));

  function selectAction(next: ManageAction): void {
    setAction(next);
    setValue('');
    setError(null);
  }

  async function runMutation(): Promise<void> {
    if (action === 'REASSIGN') {
      await reassignMutation.mutateAsync({
        requestId: prefill.requestId,
        newResourceId: Number(value),
      });
      onDone('occupancy.manage.reassign.done');
    } else {
      await swapMutation.mutateAsync({
        requestIdA: prefill.requestId,
        requestIdB: Number(value),
      });
      onDone('occupancy.manage.swap.done');
    }
  }

  async function handleSubmit(event: FormEvent): Promise<void> {
    event.preventDefault();
    if (value === '') {
      const prefix = action === 'REASSIGN' ? 'occupancy.manage.reassign' : 'occupancy.manage.swap';
      setError(t(`${prefix}.required`));
      return;
    }
    setError(null);
    setPending(true);
    try {
      await runMutation();
    } catch (mutationError) {
      emitApiErrorToast(toastKeyForError(mutationError));
    } finally {
      setPending(false);
    }
  }

  const primaryLabel =
    action === 'REASSIGN' ? t('occupancy.manage.reassign.submit') : t('occupancy.manage.swap.submit');

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('occupancy.manage.close')}
      </Button>
      {action === 'CANCEL' ? (
        <Button variant="red" onClick={onRequestCancel}>
          {t('occupancy.manage.cancel.submit')}
        </Button>
      ) : (
        <Button variant="green" submit form={FORM_ID} disabled={pending} loading={pending}>
          {primaryLabel}
        </Button>
      )}
    </>
  );

  const actions: ManageAction[] = ['REASSIGN', 'SWAP', 'CANCEL'];

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t('occupancy.manage.title')}
      icon="arrows-exchange"
      footer={footer}
    >
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <dl className="release-prefill" aria-label={t('occupancy.manage.summary')}>
          <div className="release-prefill-row">
            <dt>{t('occupancy.manage.employee')}</dt>
            <dd>{prefill.employeeName}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t(`occupancy.manage.resourceType.${prefill.resourceType}`)}</dt>
            <dd>{prefill.resourceLabel}</dd>
          </div>
          <div className="release-prefill-row">
            <dt>{t('occupancy.manage.date')}</dt>
            <dd>{longDate(prefill.date, i18n.language)}</dd>
          </div>
        </dl>

        <span className="field-label">{t('occupancy.manage.actionLabel')}</span>
        <div className="segmented" role="group" aria-label={t('occupancy.manage.actionLabel')}>
          {actions.map((candidate) => (
            <button
              key={candidate}
              type="button"
              className={action === candidate ? 'active' : ''}
              aria-pressed={action === candidate}
              onClick={() => selectAction(candidate)}
            >
              {t(`occupancy.manage.action.${candidate.toLowerCase()}`)}
            </button>
          ))}
        </div>

        <ManagePanel
          action={action}
          reassignOptions={reassignOptions}
          swapOptions={swapOptions}
          value={value}
          onChange={setValue}
          t={t}
        />

        {error ? (
          <p className="form-error" role="alert">
            {error}
          </p>
        ) : null}
      </form>
    </Dialog>
  );
}
