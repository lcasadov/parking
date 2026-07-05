import { describe, expect, it } from 'vitest';
import { initialsOf } from './initials';

describe('initialsOf', () => {
  it('should_build_initials_from_first_and_last_name', () => {
    expect(initialsOf({ firstName: 'Ada', lastName: 'Admin', login: 'admin' })).toBe('AA');
  });

  it('should_fallback_to_login_when_names_missing', () => {
    expect(initialsOf({ login: 'roberto' })).toBe('RO');
  });

  it('should_use_available_name_part_when_only_one_present', () => {
    expect(initialsOf({ firstName: 'Eve', login: 'emp' })).toBe('E');
  });
});
