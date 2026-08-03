import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { BrandCurve, BrandLogo } from './BrandCurve';
import { StatTile } from './StatTile';
import { FloorPlanThumbnail } from './FloorPlanThumbnail';
import { DayResourceBadges } from './DayResourceBadges';
import { StepResult } from './wizard/StepResult';
import { buildSteps } from './wizard/wizardTypes';
import { renderWithProviders } from '../test/renderWithProviders';

// Smoke tests de componentes presentacionales sin dependencias (render + salida básica).
describe('presentational components', () => {
  it('StatTile renders label, value and unit', () => {
    render(<StatTile label="Ocupados" value={3} unit="plazas" sub="de 20" dot="var(--accent)" icon="car" />);
    expect(screen.getByText('Ocupados')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('plazas')).toBeInTheDocument();
  });

  it('BrandCurve renders both variants', () => {
    const { container: header } = render(<BrandCurve variant="header" />);
    expect(header.querySelector('svg')).not.toBeNull();
    const { container: auth } = render(<BrandCurve variant="auth" className="x" />);
    expect(auth.querySelector('svg')).not.toBeNull();
  });

  it('BrandLogo renders with and without subtitle', () => {
    const { rerender, container } = render(<BrandLogo />);
    expect(container.firstChild).not.toBeNull();
    rerender(<BrandLogo subtitle={false} />);
    expect(container.firstChild).not.toBeNull();
  });

  it('FloorPlanThumbnail renders with and without position', () => {
    const withPos = render(
      <FloorPlanThumbnail desk={{ id: 1, number: 12, coordX: 40, coordY: 60 } as never} />,
    );
    expect(withPos.container.firstChild).not.toBeNull();
    const noPos = render(
      <FloorPlanThumbnail desk={{ id: 2, number: 13, coordX: null, coordY: null } as never} />,
    );
    expect(noPos.container.firstChild).not.toBeNull();
  });

  it('DayResourceBadges renders the resource label for assigned days', () => {
    // map: día(1)->recurso(5); labels: recurso(5)->'D-05'.
    renderWithProviders(<DayResourceBadges map={{ 1: 5 }} labels={new Map([[5, 'D-05']])} />);
    expect(screen.getByText('D-05')).toBeInTheDocument();
  });

  it('StepResult renders success and mixed summaries', () => {
    const ok = renderWithProviders(
      <StepResult
        employeeName="Ana"
        outcomes={[{ date: '2026-05-11', ok: true, resourceType: 'DESK' }]}
      />,
    );
    expect(ok.container.querySelector('.rzw-result-hero.is-success')).not.toBeNull();

    const mixed = renderWithProviders(
      <StepResult
        employeeName="Ana"
        isVisitor
        outcomes={[
          { date: '2026-05-11', ok: true, resourceType: 'DESK' },
          { date: '2026-05-12', ok: false, resourceType: 'PARKING', reasonKey: 'wizard.result.reasonTaken' },
        ]}
      />,
    );
    expect(mixed.container.querySelector('.rzw-result-hero.is-mixed')).not.toBeNull();
  });

  it('buildSteps returns a step per resource type plus fixed steps', () => {
    expect(buildSteps(['PARKING', 'DESK']).length).toBeGreaterThan(0);
  });
});
