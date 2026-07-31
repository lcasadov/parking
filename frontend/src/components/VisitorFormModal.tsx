import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { Input } from './Input';
import { VisitorVehiclesPanel } from './VisitorVehiclesPanel';
import { getFieldErrors } from '../api/apiError';
import { createVisitorVehicle } from '../api/visitorVehiclesApi';
import { useCreateVisitor, useUpdateVisitor } from '../hooks/useVisitors';
import type { Visitor, VisitorCreateRequest } from '../types/visitor';
import type { DraftVehicle } from '../types/vehicle';

type TabId = 'details' | 'vehicles';

interface VisitorFormModalProps {
  visitor?: Visitor | null;
  onClose: () => void;
  onSaved: () => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  nationalId: string;
  company: string;
  usualReason: string;
}

function initialState(visitor?: Visitor | null): FormState {
  return {
    firstName: visitor?.firstName ?? '',
    lastName: visitor?.lastName ?? '',
    nationalId: visitor?.nationalId ?? '',
    company: visitor?.company ?? '',
    usualReason: visitor?.usualReason ?? '',
  };
}

// Mapea ApiError.fields (409/400) a errores inline por campo, traduciendo el
// conflicto conocido de nationalId (documento unico).
function mapServerErrors(
  fields: Record<string, string>,
  translate: (key: string) => string,
): Record<string, string> {
  const mapped: Record<string, string> = {};
  Object.keys(fields).forEach((field) => {
    if (field === 'nationalId') {
      mapped.nationalId = translate('visitors.form.duplicateNationalId');
    } else {
      mapped[field] = fields[field];
    }
  });
  return mapped;
}

// Construye el cuerpo de la peticion normalizando opcionales vacios a undefined.
function toRequestBody(values: FormState): VisitorCreateRequest {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    nationalId: values.nationalId.trim(),
    company: values.company.trim() || undefined,
    usualReason: values.usualReason.trim() || undefined,
  };
}

const FORM_ID = 'visitor-form';

// Tablist interno del dialogo (mismo patron/markup que EmployeeFormModal).
function TabBar({
  tabs,
  active,
  onChange,
  ariaLabel,
}: {
  tabs: { id: TabId; label: string }[];
  active: TabId;
  onChange: (id: TabId) => void;
  ariaLabel: string;
}) {
  return (
    <div className="modal-tabs" role="tablist" aria-label={ariaLabel}>
      {tabs.map((tab) => (
        <button
          key={tab.id}
          type="button"
          role="tab"
          aria-selected={active === tab.id}
          className={`modal-tab${active === tab.id ? ' active' : ''}`}
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  );
}

// Modal ADMIN: alta/edicion de ficha de visitante con tabs (Detalles / Vehiculos).
// La matricula ya no es un campo del visitante: se gestiona en el tab "Vehiculos"
// (varios vehiculos por visitante, change visitor-vehicles). nationalId unico -> 409 inline.
export function VisitorFormModal({ visitor, onClose, onSaved }: VisitorFormModalProps) {
  const { t } = useTranslation();
  const isEdit = Boolean(visitor);
  const [activeTab, setActiveTab] = useState<TabId>('details');
  const [values, setValues] = useState<FormState>(() => initialState(visitor));
  const [errors, setErrors] = useState<Record<string, string>>({});
  // Vehículos acumulados en memoria durante el alta (visitante aún sin id); se persisten al crear.
  const [draftVehicles, setDraftVehicles] = useState<DraftVehicle[]>([]);
  const createMutation = useCreateVisitor();
  const updateMutation = useUpdateVisitor();
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function setField(field: keyof FormState, value: string): void {
    setValues((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => {
      if (!previous[field]) {
        return previous;
      }
      const next = { ...previous };
      delete next[field];
      return next;
    });
  }

  function validate(): Record<string, string> {
    const required = t('visitors.form.required');
    const next: Record<string, string> = {};
    if (!values.firstName.trim()) {
      next.firstName = required;
    }
    if (!values.lastName.trim()) {
      next.lastName = required;
    }
    if (!values.nationalId.trim()) {
      next.nationalId = required;
    }
    return next;
  }

  function handleServerError(error: unknown): void {
    const mapped = mapServerErrors(getFieldErrors(error), t);
    if (Object.keys(mapped).length > 0) {
      setErrors(mapped);
      setActiveTab('details');
    } else {
      setErrors({ form: t('visitors.form.genericError') });
    }
  }

  // Persiste los vehículos acumulados en el alta contra el visitante recién creado. Best-effort:
  // aunque algún vehículo fallase, el visitante ya está creado, así que se cierra igualmente.
  async function persistDraftVehicles(visitorId: number): Promise<void> {
    if (draftVehicles.length === 0) {
      return;
    }
    await Promise.allSettled(
      draftVehicles.map((vehicle) =>
        createVisitorVehicle(visitorId, {
          licensePlate: vehicle.licensePlate,
          brand: vehicle.brand,
          model: vehicle.model,
          color: vehicle.color,
        }),
      ),
    );
  }

  function persist(): void {
    const body = toRequestBody(values);
    if (isEdit) {
      updateMutation.mutate({ id: visitor!.id, body }, { onSuccess: onSaved, onError: handleServerError });
    } else {
      createMutation.mutate(body, {
        onSuccess: (created) => {
          void persistDraftVehicles(created.id).then(onSaved);
        },
        onError: handleServerError,
      });
    }
  }

  function handleSubmit(event: FormEvent): void {
    event.preventDefault();
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      setActiveTab('details');
      return;
    }
    persist();
  }

  const tabs: { id: TabId; label: string }[] = [
    { id: 'details', label: t('visitors.form.tabs.details') },
    { id: 'vehicles', label: t('visitors.form.tabs.vehicles') },
  ];

  const footer =
    activeTab === 'details' ? (
      <>
        <Button variant="white" onClick={onClose}>
          {t('visitors.form.cancel')}
        </Button>
        <Button variant="green" submit form={FORM_ID} disabled={isSaving}>
          {t('visitors.form.save')}
        </Button>
      </>
    ) : (
      <Button variant="white" onClick={onClose}>
        {t('visitors.detail.close')}
      </Button>
    );

  return (
    <Dialog
      open
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t(isEdit ? 'visitors.form.editTitle' : 'visitors.form.createTitle')}
      footer={footer}
    >
      <TabBar
        tabs={tabs}
        active={activeTab}
        onChange={setActiveTab}
        ariaLabel={t(isEdit ? 'visitors.form.editTitle' : 'visitors.form.createTitle')}
      />
      {activeTab === 'details' ? (
        <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
          <Input
            label={t('visitors.form.firstName')}
            value={values.firstName}
            error={Boolean(errors.firstName)}
            hint={errors.firstName}
            onChange={(event) => setField('firstName', event.target.value)}
          />
          <Input
            label={t('visitors.form.lastName')}
            value={values.lastName}
            error={Boolean(errors.lastName)}
            hint={errors.lastName}
            onChange={(event) => setField('lastName', event.target.value)}
          />
          <Input
            label={t('visitors.form.nationalId')}
            value={values.nationalId}
            error={Boolean(errors.nationalId)}
            hint={errors.nationalId}
            onChange={(event) => setField('nationalId', event.target.value)}
          />
          <Input
            label={t('visitors.form.company')}
            value={values.company}
            onChange={(event) => setField('company', event.target.value)}
          />
          <Input
            label={t('visitors.form.usualReason')}
            value={values.usualReason}
            onChange={(event) => setField('usualReason', event.target.value)}
          />
          {errors.form ? (
            <p className="form-error" role="alert">
              {errors.form}
            </p>
          ) : null}
        </form>
      ) : (
        <VisitorVehiclesPanel
          visitorId={visitor?.id ?? null}
          draft={isEdit ? undefined : { vehicles: draftVehicles, onChange: setDraftVehicles }}
        />
      )}
    </Dialog>
  );
}
