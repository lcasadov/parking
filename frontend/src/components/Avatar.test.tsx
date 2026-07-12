import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Avatar } from './Avatar';
import { AVATAR_COLORS, avatarColorOf } from '../utils/avatarColor';

describe('avatarColorOf', () => {
  it('should_be_deterministic_for_the_same_seed', () => {
    expect(avatarColorOf('Ada Lovelace')).toBe(avatarColorOf('Ada Lovelace'));
  });

  it('should_always_return_a_known_palette_class', () => {
    for (const seed of ['a', 'Bob', 'Zoe Q', '', '12345']) {
      expect(AVATAR_COLORS).toContain(avatarColorOf(seed));
    }
  });
});

describe('Avatar', () => {
  it('should_render_md_avatar_with_base_class_and_initials', () => {
    render(<Avatar initials="RB" label="Rosa Blanco" size="md" />);
    const avatar = screen.getByRole('img', { name: 'Rosa Blanco' });
    expect(avatar).toHaveClass('avatar');
    expect(avatar).toHaveTextContent('RB');
  });

  it('should_derive_stable_palette_class_for_sm_avatar', () => {
    const { rerender } = render(<Avatar initials="AL" label="Ada" size="sm" seed="Ada Lovelace" />);
    const first = screen.getByRole('img', { name: 'Ada' }).className;
    rerender(<Avatar initials="AL" label="Ada" size="sm" seed="Ada Lovelace" />);
    const second = screen.getByRole('img', { name: 'Ada' }).className;
    expect(first).toBe(second);
    expect(first).toContain('avatar-sm');
    expect(first).toContain(avatarColorOf('Ada Lovelace'));
  });

  it('should_fall_back_to_label_as_seed_when_seed_is_absent', () => {
    render(<Avatar initials="EE" label="Eve Employee" size="sm" />);
    expect(screen.getByRole('img', { name: 'Eve Employee' })).toHaveClass(
      avatarColorOf('Eve Employee'),
    );
  });
});
