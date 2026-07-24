import { useMemo, useState } from 'react';
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Button } from '../Button';
import { Dialog } from '../Dialog';
import { WizardStepper } from './WizardStepper';
import { StepResourceType } from './StepResourceType';
import { StepDates } from './StepDates';
import { StepEmployee } from './StepEmployee';
import { StepLocation } from './StepLocation';
import { StepSummary } from './StepSummary';
import { StepResult } from './StepResult';
import { useReservationBooking } from '../../hooks/useReservationBooking';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { emitApiErrorToast } from '../../api/events';
import { resolveWizardDates } from '../../utils/wizardDates';
import { isLocationComplete, resolveBookingEntries } from '../../utils/wizardBooking';
import { DUR, EASE } from '../../theme/motion';
import {
  STEP_DATES,
  STEP_EMPLOYEE,
  STEP_LOCATION,
  STEP_RESOURCE,
  STEP_SUMMARY,
} from './wizardTypes';
import type { BookingOutcome, WizardState } from './wizardTypes';
import type { ResourceType } from '../../types/request';

interface ReservationWizardProps {
  onClose: () => void;
}

const INITIAL_STATE: WizardState = {
  resourceType: null,
  dateMode: 'SINGLE',
  singleDate: '',
  rangeStart: '',
  rangeEnd: '',
  scatterDates: [],
  employeeId: null,
  locationMode: 'ALL',
  deskId: null,
  parkingChoice: null,
  chosenLabel: null,
  perDay: {},
};

const STEP_KEYS = [
  'wizard.steps.resource',
  'wizard.steps.dates',
  'wizard.steps.employee',
  'wizard.steps.location',
  'wizard.steps.summary',
] as const;

const LAST_STEP = STEP_SUMMARY;

// ¿Está completo el paso actual para poder avanzar?
function isStepValid(step: number, state: WizardState, dates: string[]): boolean {
  if (step === STEP_RESOURCE) {
    return state.resourceType !== null;
  }
  if (step === STEP_DATES) {
    return dates.length > 0;
  }
  if (step === STEP_EMPLOYEE) {
    return state.employeeId !== null;
  }
  if (step === STEP_LOCATION) {
    return isLocationComplete(state, dates);
  }
  return true;
}

// Índice de paso más alto alcanzable "desde cero" con los datos actuales:
// recorre los pasos en orden y se detiene en el primero inválido. Se recalcula
// en cada render, así que si el usuario invalida un paso previo (p.ej. borra
// todas las fechas) los pasos que dependían de él dejan de ser clicables.
function computeReachableStep(state: WizardState, dates: string[]): number {
  let max = STEP_RESOURCE;
  for (let s = STEP_RESOURCE; s < LAST_STEP; s += 1) {
    if (!isStepValid(s, state, dates)) {
      break;
    }
    max = s + 1;
  }
  return max;
}

// Asistente de reserva ADMIN (modal multipaso): crea, en nombre de un empleado,
// una reserva APPROVED por cada fecha elegida vía POST /requests/admin (que envía
// el email). Reutiliza las primitivas Dialog/Button, el plano y los hooks de datos.
export function ReservationWizard({ onClose }: ReservationWizardProps) {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const [state, setState] = useState<WizardState>(INITIAL_STATE);
  const [step, setStep] = useState<number>(STEP_RESOURCE);
  const [direction, setDirection] = useState<number>(1);
  const [outcomes, setOutcomes] = useState<BookingOutcome[] | null>(null);

  const booking = useReservationBooking();
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const dates = useMemo(() => resolveWizardDates(state), [state]);
  const reachableStep = useMemo(() => computeReachableStep(state, dates), [state, dates]);

  const employeeName =
    employeesQuery.data?.find((candidate) => candidate.id === state.employeeId)?.fullName ??
    t('wizard.summary.unknownEmployee');

  function patch(partial: Partial<WizardState>): void {
    setState((prev) => ({ ...prev, ...partial }));
  }

  // Cambiar el tipo de recurso invalida la ubicación elegida (plaza/puesto distintos),
  // incluidas las elecciones por día.
  function setResourceType(resourceType: ResourceType): void {
    setState((prev) => ({
      ...prev,
      resourceType,
      deskId: null,
      parkingChoice: null,
      chosenLabel: null,
      perDay: {},
    }));
  }

  function goNext(): void {
    setDirection(1);
    setStep((prev) => Math.min(prev + 1, LAST_STEP));
  }

  function goBack(): void {
    setDirection(-1);
    setStep((prev) => Math.max(prev - 1, STEP_RESOURCE));
  }

  // Navegación directa desde el stepper: solo a pasos ya alcanzados (clic en
  // el propio paso actual no hace nada).
  function goToStep(target: number): void {
    if (target === step || target > reachableStep) {
      return;
    }
    setDirection(target > step ? 1 : -1);
    setStep(target);
  }

  async function handleConfirm(): Promise<void> {
    if (state.resourceType === null || state.employeeId === null) {
      return;
    }
    try {
      const result = await booking.mutateAsync({
        employeeId: state.employeeId,
        resourceType: state.resourceType,
        entries: resolveBookingEntries(state, dates),
      });
      setOutcomes(result);
    } catch {
      emitApiErrorToast('wizard.result.reasonGeneric');
    }
  }

  function restart(): void {
    setState(INITIAL_STATE);
    setStep(STEP_RESOURCE);
    setDirection(1);
    setOutcomes(null);
  }

  function renderStep() {
    if (step === STEP_RESOURCE) {
      return <StepResourceType value={state.resourceType} onChange={setResourceType} />;
    }
    if (step === STEP_DATES) {
      return <StepDates state={state} dates={dates} patch={patch} />;
    }
    if (step === STEP_EMPLOYEE) {
      return (
        <StepEmployee
          employeeId={state.employeeId}
          onChange={(employeeId) => patch({ employeeId })}
        />
      );
    }
    if (step === STEP_LOCATION) {
      return <StepLocation state={state} dates={dates} patch={patch} />;
    }
    return <StepSummary state={state} dates={dates} />;
  }

  const stepLabels = STEP_KEYS.map((key) => t(key));
  const valid = isStepValid(step, state, dates);
  const slide = reduceMotion ? 0 : 26;
  const enterX = direction * slide;
  const stepContent = renderStep();

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
      <Button variant="white" icon="arrow-left" disabled={step === STEP_RESOURCE} onClick={goBack}>
        {t('wizard.nav.back')}
      </Button>
      {step === LAST_STEP ? (
        <Button variant="green" icon="check" disabled={booking.isPending} onClick={handleConfirm}>
          {booking.isPending ? t('wizard.nav.confirming') : t('wizard.nav.confirm')}
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
          <StepResult outcomes={outcomes} employeeName={employeeName} />
        </div>
      ) : (
        <>
          <div className="rzw-stepper-zone">
            <WizardStepper
              steps={stepLabels}
              current={step}
              reachable={reachableStep}
              onStepClick={goToStep}
            />
          </div>
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
        </>
      )}
    </Dialog>
  );
}
