import { useEffect, useMemo, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Button } from '../Button';
import { Dialog } from '../Dialog';
import { WizardStepper } from './WizardStepper';
import { WizardRail } from './WizardRail';
import { StepResourceType } from './StepResourceType';
import { StepDates } from './StepDates';
import { StepEmployee } from './StepEmployee';
import { StepLocation } from './StepLocation';
import { StepSummary } from './StepSummary';
import { StepResult } from './StepResult';
import { useReservationBooking } from '../../hooks/useReservationBooking';
import { useVisitorBooking } from '../../hooks/useVisitorBooking';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useVisitorsQuery } from '../../hooks/useVisitors';
import { emitApiErrorToast } from '../../api/events';
import { resolveWizardDates } from '../../utils/wizardDates';
import { isLocationComplete, resolveBookingEntries } from '../../utils/wizardBooking';
import { DUR, EASE } from '../../theme/motion';
import {
  ORDERED_RESOURCE_TYPES,
  buildSteps,
  emptyTypeLocation,
} from './wizardTypes';
import type {
  BeneficiaryType,
  BookingOutcome,
  StepDescriptor,
  TypeLocation,
  WizardState,
} from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface ReservationWizardProps {
  onClose: () => void;
  // Preselección del beneficiario (p. ej. abierto desde Visitantes: 'VISITOR' + el
  // visitante de la fila). Por defecto, empleado sin preselección.
  initialBeneficiaryType?: BeneficiaryType;
  initialVisitorId?: number | null;
}

function makeInitialState(
  beneficiaryType: BeneficiaryType,
  visitorId: number | null,
): WizardState {
  return {
    resourceTypes: [],
    dateMode: 'SINGLE',
    singleDate: '',
    rangeStart: '',
    rangeEnd: '',
    scatterDates: [],
    beneficiaryType,
    employeeId: null,
    visitorId,
    locations: {
      PARKING: emptyTypeLocation(),
      DESK: emptyTypeLocation(),
    },
  };
}

// Etiqueta i18n de cada paso, incluidos los de ubicación por tipo.
function stepLabelKey(step: StepDescriptor): string {
  switch (step.kind) {
    case 'RESOURCE':
      return 'wizard.steps.resource';
    case 'DATES':
      return 'wizard.steps.dates';
    case 'EMPLOYEE':
      return 'wizard.steps.employee';
    case 'LOCATION':
      return step.type === 'DESK' ? 'wizard.steps.locationDesk' : 'wizard.steps.locationParking';
    default:
      return 'wizard.steps.summary';
  }
}

// ¿Está completo el paso para poder avanzar?
function isStepValid(step: StepDescriptor, state: WizardState, dates: string[]): boolean {
  if (step.kind === 'RESOURCE') {
    return state.resourceTypes.length > 0;
  }
  if (step.kind === 'DATES') {
    return dates.length > 0;
  }
  if (step.kind === 'EMPLOYEE') {
    return state.beneficiaryType === 'VISITOR' ? state.visitorId !== null : state.employeeId !== null;
  }
  if (step.kind === 'LOCATION' && step.type) {
    return isLocationComplete(state.locations[step.type], step.type, dates);
  }
  return true;
}

// Índice de paso más alto alcanzable "desde cero" con los datos actuales: recorre los
// pasos en orden y se detiene en el primero inválido (excluido el resumen final).
function computeReachableStep(steps: StepDescriptor[], state: WizardState, dates: string[]): number {
  let max = 0;
  for (let s = 0; s < steps.length - 1; s += 1) {
    if (!isStepValid(steps[s], state, dates)) {
      break;
    }
    max = s + 1;
  }
  return max;
}

// Asistente de reserva ADMIN (modal multipaso): crea, en nombre de un empleado o
// visitante, una reserva APPROVED por cada fecha y por cada tipo de recurso elegido
// (plaza y/o puesto) vía POST /requests/admin (email) o /visitor-reservations. La
// ubicación se elige en un paso por tipo (tarea 3: reservar plaza Y puesto a la vez).
export function ReservationWizard({
  onClose,
  initialBeneficiaryType = 'EMPLOYEE',
  initialVisitorId = null,
}: ReservationWizardProps) {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const initial = useMemo(
    () => makeInitialState(initialBeneficiaryType, initialVisitorId),
    [initialBeneficiaryType, initialVisitorId],
  );
  const [state, setState] = useState<WizardState>(initial);
  const [step, setStep] = useState<number>(0);
  const [direction, setDirection] = useState<number>(1);
  const [outcomes, setOutcomes] = useState<BookingOutcome[] | null>(null);

  const booking = useReservationBooking();
  const visitorBooking = useVisitorBooking();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const visitorsQuery = useVisitorsQuery({ page: 0, size: 100 });
  const dates = useMemo(() => resolveWizardDates(state), [state]);
  const steps = useMemo(() => buildSteps(state.resourceTypes), [state.resourceTypes]);
  const lastStep = steps.length - 1;
  const reachableStep = useMemo(
    () => computeReachableStep(steps, state, dates),
    [steps, state, dates],
  );

  // Al cambiar los tipos (y con ellos el número de pasos), no dejar el índice fuera
  // de rango: se recorta al último paso disponible.
  useEffect(() => {
    if (step > lastStep) {
      setStep(lastStep);
    }
  }, [step, lastStep]);

  const isVisitor = state.beneficiaryType === 'VISITOR';
  const employeeName =
    employeesQuery.data?.find((candidate) => candidate.id === state.employeeId)?.fullName ??
    t('wizard.summary.unknownEmployee');
  const visitor = visitorsQuery.data?.content.find((candidate) => candidate.id === state.visitorId);
  const beneficiaryName = isVisitor
    ? (visitor ? `${visitor.firstName} ${visitor.lastName}`.trim() : t('wizard.summary.unknownEmployee'))
    : employeeName;

  function patch(partial: Partial<WizardState>): void {
    setState((prev) => ({ ...prev, ...partial }));
  }

  // Aplica cambios al slice de ubicación de un tipo concreto.
  function patchLocation(type: ResourceType, partial: Partial<TypeLocation>): void {
    setState((prev) => ({
      ...prev,
      locations: { ...prev.locations, [type]: { ...prev.locations[type], ...partial } },
    }));
  }

  // Añade/quita un tipo de recurso manteniendo el orden canónico (Plaza antes que
  // Puesto). Reinicia su ubicación para no arrastrar elecciones de una selección previa.
  function toggleResourceType(type: ResourceType): void {
    setState((prev) => {
      const selected = prev.resourceTypes.includes(type);
      const resourceTypes = selected
        ? prev.resourceTypes.filter((current) => current !== type)
        : ORDERED_RESOURCE_TYPES.filter(
            (current) => current === type || prev.resourceTypes.includes(current),
          );
      return {
        ...prev,
        resourceTypes,
        locations: { ...prev.locations, [type]: emptyTypeLocation() },
      };
    });
  }

  // Cambiar el beneficiario reinicia TODAS las ubicaciones (la disponibilidad y la
  // auto-asignación dependen del beneficiario: el visitante no usa auto).
  function changeBeneficiary(beneficiaryType: BeneficiaryType): void {
    patch({
      beneficiaryType,
      employeeId: null,
      visitorId: null,
      locations: { PARKING: emptyTypeLocation(), DESK: emptyTypeLocation() },
    });
  }

  function goNext(): void {
    setDirection(1);
    setStep((prev) => Math.min(prev + 1, lastStep));
  }

  function goBack(): void {
    setDirection(-1);
    setStep((prev) => Math.max(prev - 1, 0));
  }

  function goToStep(target: number): void {
    if (target === step || target > reachableStep) {
      return;
    }
    setDirection(target > step ? 1 : -1);
    setStep(target);
  }

  async function handleConfirm(): Promise<void> {
    if (state.resourceTypes.length === 0) {
      return;
    }
    if (isVisitor ? state.visitorId === null : state.employeeId === null) {
      return;
    }
    const employeeId = state.employeeId;
    const visitorId = state.visitorId;
    try {
      const all: BookingOutcome[] = [];
      for (const type of state.resourceTypes) {
        const entries = resolveBookingEntries(state.locations[type], type, dates);
        const result =
          isVisitor && visitorId !== null
            ? await visitorBooking.mutateAsync({ visitorId, resourceType: type, entries })
            : await booking.mutateAsync({ employeeId: employeeId as number, resourceType: type, entries });
        all.push(...result.map((outcome) => ({ ...outcome, resourceType: type })));
      }
      setOutcomes(all);
    } catch {
      emitApiErrorToast('wizard.result.reasonGeneric');
    }
  }

  function restart(): void {
    setState(initial);
    setStep(0);
    setDirection(1);
    setOutcomes(null);
  }

  function renderStep(descriptor: StepDescriptor) {
    if (descriptor.kind === 'RESOURCE') {
      return <StepResourceType values={state.resourceTypes} onToggle={toggleResourceType} />;
    }
    if (descriptor.kind === 'DATES') {
      return <StepDates state={state} dates={dates} patch={patch} />;
    }
    if (descriptor.kind === 'EMPLOYEE') {
      return (
        <StepEmployee
          beneficiaryType={state.beneficiaryType}
          employeeId={state.employeeId}
          visitorId={state.visitorId}
          onBeneficiaryTypeChange={changeBeneficiary}
          onEmployeeChange={(employeeId) => patch({ employeeId })}
          onVisitorChange={(visitorId) => patch({ visitorId })}
        />
      );
    }
    if (descriptor.kind === 'LOCATION' && descriptor.type) {
      const type = descriptor.type;
      return (
        <StepLocation
          type={type}
          location={state.locations[type]}
          dates={dates}
          beneficiaryType={state.beneficiaryType}
          employeeId={state.employeeId}
          patchLocation={(partial) => patchLocation(type, partial)}
        />
      );
    }
    return <StepSummary state={state} dates={dates} />;
  }

  const stepLabels = steps.map((descriptor) => t(stepLabelKey(descriptor)));
  const current = steps[Math.min(step, lastStep)];
  const valid = isStepValid(current, state, dates);
  const isPending = booking.isPending || visitorBooking.isPending;
  const slide = reduceMotion ? 0 : 26;
  const enterX = direction * slide;
  const stepContent = renderStep(current);
  const isLast = step === lastStep;

  const footer = outcomes ? (
    <>
      <Button variant="white" onClick={restart}>
        {t('wizard.nav.another')}
      </Button>
      <Button variant="green" icon="check" onClick={onClose}>
        {t('wizard.nav.done')}
      </Button>
    </>
  ) : (
    <>
      <Button variant="white" icon="arrow-left" disabled={step === 0} onClick={goBack}>
        {t('wizard.nav.back')}
      </Button>
      {isLast ? (
        <Button variant="green" icon="check" disabled={isPending} onClick={handleConfirm}>
          {isPending ? t('wizard.nav.confirming') : t('wizard.nav.confirm')}
        </Button>
      ) : (
        <Button variant="green" icon="arrow-right" disabled={!valid} onClick={goNext}>
          {t('wizard.nav.next')}
        </Button>
      )}
    </>
  );

  return (
    <Dialog
      open
      fullScreen
      flushBody
      onOpenChange={(next) => {
        if (!next) {
          onClose();
        }
      }}
      title={t('wizard.title')}
      icon="calendar-plus"
      footer={footer}
    >
      {outcomes ? (
        <div className="rzw-scroll-zone">
          <StepResult outcomes={outcomes} employeeName={beneficiaryName} isVisitor={isVisitor} />
        </div>
      ) : (
        <>
          {/* Móvil: stepper horizontal arriba (el riel lateral se oculta). */}
          <div className="rzw-stepper-zone">
            <WizardStepper
              steps={stepLabels}
              current={step}
              reachable={reachableStep}
              onStepClick={goToStep}
            />
          </div>
          <div className="rzw-body-row">
            {/* Desktop: riel lateral con resumen en vivo + salto de sección. */}
            <WizardRail
              steps={steps}
              labels={stepLabels}
              current={step}
              reachable={reachableStep}
              state={state}
              dates={dates}
              onStepClick={goToStep}
            />
            <div className="rzw-scroll-zone">
              <div className="rzw-viewport">
                <AnimatePresence mode="wait" initial={false} custom={direction}>
                  <motion.div
                    key={step}
                    initial={{ opacity: 0, x: enterX }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -enterX }}
                    transition={{ duration: reduceMotion ? 0 : DUR.base, ease: EASE.out }}
                  >
                    {stepContent}
                  </motion.div>
                </AnimatePresence>
              </div>
            </div>
          </div>
        </>
      )}
    </Dialog>
  );
}
