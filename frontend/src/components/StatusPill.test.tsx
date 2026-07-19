import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatusPill } from './StatusPill';

describe('StatusPill', () => {
  it('should_map_tone_to_state_color_class', () => {
    render(<StatusPill tone="occupied">Ocupada</StatusPill>);
    const pill = screen.getByText('Ocupada');
    expect(pill).toHaveClass('pill', 'pill-state-occupied');
  });

  it('should_render_optional_decorative_icon', () => {
    const { container } = render(
      <StatusPill tone="released" icon="unlink">
        Liberada
      </StatusPill>,
    );
    expect(container.querySelector('i.ti.ti-unlink')).toHaveAttribute('aria-hidden', 'true');
  });

  it('should_support_every_contract_tone', () => {
    const tones = ['occupied', 'released', 'pending', 'request', 'free'] as const;
    tones.forEach((tone) => {
      const { container } = render(<StatusPill tone={tone}>{tone}</StatusPill>);
      expect(container.querySelector(`.pill-state-${tone}`)).toBeInTheDocument();
    });
  });
});
