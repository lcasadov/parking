import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { InfoBanner } from './InfoBanner';
import { Legend } from './Legend';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';

describe('InfoBanner', () => {
  it('should_render_variant_class_and_status_role', () => {
    render(<InfoBanner variant="amber">Aviso</InfoBanner>);
    const banner = screen.getByRole('status');
    expect(banner).toHaveClass('info-banner', 'amber');
    expect(banner).toHaveTextContent('Aviso');
  });

  it('should_render_optional_icon', () => {
    const { container } = render(
      <InfoBanner variant="red" icon="alert-triangle">
        Error
      </InfoBanner>,
    );
    expect(container.querySelector('i.ti.ti-alert-triangle')).toBeInTheDocument();
  });
});

describe('Legend', () => {
  it('should_render_one_item_per_entry', () => {
    const { container } = render(
      <Legend
        items={[
          { color: 'var(--green)', label: 'Asignada' },
          { color: 'var(--red)', label: 'Liberada' },
        ]}
      />,
    );
    expect(container.querySelectorAll('.legend-item')).toHaveLength(2);
    expect(screen.getByText('Asignada')).toBeInTheDocument();
  });
});

describe('FieldRow', () => {
  it('should_apply_wide_modifier', () => {
    const { container } = render(
      <FieldRow wide>
        <span>a</span>
      </FieldRow>,
    );
    expect(container.querySelector('.field-row')).toHaveClass('field-row-1-4');
  });

  it('should_default_to_two_equal_columns', () => {
    const { container } = render(
      <FieldRow>
        <span>a</span>
      </FieldRow>,
    );
    expect(container.querySelector('.field-row')).not.toHaveClass('field-row-1-4');
  });
});

describe('FieldValue', () => {
  it('should_combine_state_modifiers', () => {
    const { container } = render(
      <FieldValue readOnly focused danger withIcon>
        123
      </FieldValue>,
    );
    expect(container.querySelector('.field-value')).toHaveClass(
      'readonly',
      'focused',
      'danger',
      'with-icon',
    );
  });

  it('should_render_plain_value_without_modifiers', () => {
    const { container } = render(<FieldValue>plain</FieldValue>);
    const el = container.querySelector('.field-value');
    expect(el).toHaveTextContent('plain');
    expect(el?.className.trim()).toBe('field-value');
  });
});
