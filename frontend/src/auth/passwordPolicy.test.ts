import { describe, expect, it } from 'vitest';
import { evaluatePasswordPolicy, isPasswordValid } from './passwordPolicy';

describe('passwordPolicy', () => {
  it('should_fail_all_rules_when_password_empty', () => {
    const result = evaluatePasswordPolicy('');
    expect(result.length).toBe(false);
    expect(result.upper).toBe(false);
    expect(result.lower).toBe(false);
    expect(result.digit).toBe(false);
    expect(result.symbol).toBe(false);
    expect(isPasswordValid('')).toBe(false);
  });

  it('should_pass_all_rules_when_password_meets_policy', () => {
    const result = evaluatePasswordPolicy('Str0ng!Pass99');
    expect(result.length).toBe(true);
    expect(result.upper).toBe(true);
    expect(result.lower).toBe(true);
    expect(result.digit).toBe(true);
    expect(result.symbol).toBe(true);
    expect(isPasswordValid('Str0ng!Pass99')).toBe(true);
  });

  it('should_be_invalid_when_missing_symbol', () => {
    expect(isPasswordValid('Str0ngPass99')).toBe(false);
  });
});
