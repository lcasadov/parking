import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { UserAvatar } from './UserAvatar';

describe('UserAvatar', () => {
  it('should_render_avatar_with_accessible_label_and_initials', () => {
    render(
      <UserAvatar user={{ firstName: 'Eve', lastName: 'Employee', login: 'emp' }} label="Eve Employee" />,
    );
    const avatar = screen.getByRole('img', { name: 'Eve Employee' });
    expect(avatar).toHaveTextContent('EE');
  });
});
