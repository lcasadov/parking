import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Tabs } from './Tabs';

const TABS = [
  { id: 'fichas', label: 'Fichas' },
  { id: 'pendientes', label: 'Pendientes', count: 3 },
];

describe('Tabs', () => {
  it('should_mark_active_tab_as_selected', () => {
    render(<Tabs tabs={TABS} active="fichas" onChange={() => {}} ariaLabel="Vistas" />);
    expect(screen.getByRole('tab', { name: 'Fichas' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: /Pendientes/ })).toHaveAttribute(
      'aria-selected',
      'false',
    );
  });

  it('should_render_count_badge', () => {
    render(<Tabs tabs={TABS} active="fichas" onChange={() => {}} ariaLabel="Vistas" />);
    expect(screen.getByText('3')).toHaveClass('count');
  });

  it('should_call_onChange_with_tab_id', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<Tabs tabs={TABS} active="fichas" onChange={onChange} ariaLabel="Vistas" />);

    await user.click(screen.getByRole('tab', { name: /Pendientes/ }));

    expect(onChange).toHaveBeenCalledWith('pendientes');
  });
});
