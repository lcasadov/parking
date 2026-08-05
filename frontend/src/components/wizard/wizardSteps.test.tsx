import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ReservationWizard } from './ReservationWizard';
import { StepResourceType } from './StepResourceType';
import { StepDates } from './StepDates';
import { StepLocation } from './StepLocation';
import { StepLocationPerDay } from './StepLocationPerDay';
import { StepEmployee } from './StepEmployee';
import { StepSummary } from './StepSummary';
import { emptyTypeLocation, type WizardState } from './wizardTypes';
import { renderWithProviders } from '../../test/renderWithProviders';

const DATE = '2026-08-10';
const DATE2 = '2026-08-11';
// Día futuro e intra-mes: en el grid del calendario los números 7–26 sólo
// aparecen una vez (mes en curso), así que el nombre "15" es inequívoco y clicable.
const FUTURE_DAY = '15';

function makeState(overrides: Partial<WizardState> = {}): WizardState {
  return {
    resourceTypes: ['PARKING', 'DESK'],
    dateMode: 'SINGLE',
    singleDate: DATE,
    rangeStart: DATE,
    rangeEnd: DATE2,
    scatterDates: [],
    beneficiaryType: 'EMPLOYEE',
    employeeId: 10,
    visitorId: null,
    locations: { PARKING: emptyTypeLocation(), DESK: emptyTypeLocation() },
    ...overrides,
  };
}

function pickFutureDay(): HTMLElement {
  const days = screen.getAllByRole('button', { name: FUTURE_DAY });
  return days.find((d) => !(d as HTMLButtonElement).disabled) as HTMLElement;
}

describe('reservation wizard', () => {
  it('mounts on the first step with navigation controls', async () => {
    renderWithProviders(<ReservationWizard onClose={vi.fn()} />);
    // Sin recurso elegido, "Siguiente" arranca deshabilitado (isStepValid del paso RECURSO).
    expect(
      await screen.findByRole('button', { name: /^siguiente$|^next$/i }),
    ).toBeDisabled();
  });

  it('navigates the step machine forward and back', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReservationWizard onClose={vi.fn()} />);

    // Paso RECURSO → elegir plaza habilita avanzar (toggleResourceType + isStepValid).
    await user.click(screen.getByRole('button', { name: /plaza de parking|parking spot/i }));
    await user.click(screen.getByRole('button', { name: /^siguiente$|^next$/i }));

    // Paso FECHAS (transición async de framer-motion) → elegir un día (goNext + patch).
    await user.click(await screen.findByRole('button', { name: FUTURE_DAY }));
    await user.click(screen.getByRole('button', { name: /^siguiente$|^next$/i }));

    // Paso EMPLEADO → alternar beneficiario ejercita changeBeneficiary en ambos sentidos.
    const tablist = await screen.findByRole('tablist');
    await user.click(within(tablist).getByRole('tab', { name: /visitante|visitor/i }));
    await user.click(within(tablist).getByRole('tab', { name: /empleado|employee/i }));

    // Volver dos veces ejercita goBack y el recálculo del paso alcanzable.
    await user.click(screen.getByRole('button', { name: /^atrás$|^back$/i }));
    await user.click(await screen.findByRole('button', { name: /^atrás$|^back$/i }));

    // De vuelta en el paso RECURSO: reaparecen las opciones de recurso.
    expect(
      await screen.findByRole('button', { name: /puesto de oficina|office desk/i }),
    ).toBeInTheDocument();
  });

  it('StepResourceType toggles each resource choice', async () => {
    const user = userEvent.setup();
    const onToggle = vi.fn();
    renderWithProviders(<StepResourceType values={[]} onToggle={onToggle} />);

    await user.click(screen.getByRole('button', { name: /plaza de parking|parking spot/i }));
    await user.click(screen.getByRole('button', { name: /puesto de oficina|office desk/i }));
    expect(onToggle).toHaveBeenCalledWith('PARKING');
    expect(onToggle).toHaveBeenCalledWith('DESK');
  });

  it('StepDates switches every mode, navigates months and picks a day', async () => {
    const user = userEvent.setup();
    const patch = vi.fn();
    renderWithProviders(
      <StepDates state={makeState({ singleDate: '' })} dates={[]} patch={patch} />,
    );

    // Cada modo dispara switchMode limpiando el estado del modo anterior.
    await user.click(screen.getByRole('button', { name: /rango|range/i }));
    expect(patch).toHaveBeenCalledWith({ dateMode: 'RANGE' });
    await user.click(screen.getByRole('button', { name: /días sueltos|scattered/i }));
    expect(patch).toHaveBeenCalledWith({ dateMode: 'SCATTER' });
    await user.click(screen.getByRole('button', { name: /día concreto|single day/i }));

    // Navegar de mes ejercita onAnchorChange (prev/next month).
    await user.click(screen.getByRole('button', { name: /mes siguiente|next month/i }));
    await user.click(screen.getByRole('button', { name: /mes anterior|previous month/i }));

    // Clicar un día llama onPick → patch con el fragmento del modo activo.
    await user.click(pickFutureDay());
    expect(patch.mock.calls.length).toBeGreaterThanOrEqual(4);
  });

  it('StepDates in RANGE mode marks selected edges', () => {
    // Render con un rango ya fijado: cubre las ramas is-edge / is-range-mid del calendario.
    const { container } = renderWithProviders(
      <StepDates state={makeState({ dateMode: 'RANGE' })} dates={[DATE, DATE2]} patch={vi.fn()} />,
    );
    expect(container.querySelector('.rzw-cal-cell.is-selected')).not.toBeNull();
  });

  it('StepEmployee toggles the beneficiary type', async () => {
    const user = userEvent.setup();
    const onBeneficiaryTypeChange = vi.fn();
    renderWithProviders(
      <StepEmployee
        beneficiaryType="EMPLOYEE"
        employeeId={10}
        visitorId={null}
        onBeneficiaryTypeChange={onBeneficiaryTypeChange}
        onEmployeeChange={vi.fn()}
        onVisitorChange={vi.fn()}
      />,
    );

    const tablist = screen.getByRole('tablist');
    await user.click(within(tablist).getByRole('tab', { name: /visitante|visitor/i }));
    expect(onBeneficiaryTypeChange).toHaveBeenCalledWith('VISITOR');
  });

  it('renders StepLocation (parking, single date)', () => {
    const { container } = renderWithProviders(
      <StepLocation
        type="PARKING"
        location={emptyTypeLocation()}
        dates={[DATE]}
        beneficiaryType="EMPLOYEE"
        employeeId={10}
        patchLocation={vi.fn()}
      />,
    );
    expect(container.firstChild).not.toBeNull();
  });

  it('renders StepLocationPerDay (parking, multiple dates)', () => {
    const { container } = renderWithProviders(
      <StepLocationPerDay
        type="PARKING"
        location={emptyTypeLocation()}
        dates={[DATE, DATE2]}
        beneficiaryType="EMPLOYEE"
        employeeId={10}
        patchLocation={vi.fn()}
      />,
    );
    expect(container.firstChild).not.toBeNull();
  });

  it('renders StepSummary', () => {
    const { container } = renderWithProviders(<StepSummary state={makeState()} dates={[DATE]} />);
    expect(container.firstChild).not.toBeNull();
  });
});
