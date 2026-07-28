import { AxiosError } from 'axios';
import { describe, expect, it } from 'vitest';
import { deactivationErrorKey } from './resourceDeactivation';

function apiError(code: string): AxiosError {
  const error = new AxiosError('conflict');
  error.response = {
    data: { error: code, message: 'blocked', fields: {}, timestamp: '' },
    status: 409,
    statusText: 'Conflict',
    headers: {},
    config: {} as never,
  };
  return error;
}

describe('deactivationErrorKey', () => {
  it('should_return_future_assignments_key_on_resource_block', () => {
    expect(deactivationErrorKey(apiError('RESOURCE_HAS_FUTURE_ASSIGNMENTS'), 'desks')).toBe(
      'desks.errors.hasFutureAssignments',
    );
  });

  it('should_fall_back_to_toggle_key_on_other_errors', () => {
    expect(deactivationErrorKey(apiError('CONFLICT'), 'parkingSpaces')).toBe(
      'parkingSpaces.errors.toggle',
    );
    expect(deactivationErrorKey(new Error('boom'), 'parkingSpaces')).toBe(
      'parkingSpaces.errors.toggle',
    );
  });
});
