// Politica de contrasena (cliente) segun docs/openapi.yaml ChangePasswordRequest:
// >=10 chars, mayuscula + minuscula + digito + simbolo.
// La verificacion "distinta de login/email" la valida el backend.

export interface PolicyResult {
  length: boolean;
  upper: boolean;
  lower: boolean;
  digit: boolean;
  symbol: boolean;
}

export function evaluatePasswordPolicy(password: string): PolicyResult {
  return {
    length: password.length >= 10,
    upper: /[A-Z]/.test(password),
    lower: /[a-z]/.test(password),
    digit: /\d/.test(password),
    symbol: /[^A-Za-z0-9]/.test(password),
  };
}

export function isPasswordValid(password: string): boolean {
  const r = evaluatePasswordPolicy(password);
  return r.length && r.upper && r.lower && r.digit && r.symbol;
}

export const POLICY_RULES = ['length', 'upper', 'lower', 'digit', 'symbol'] as const;
export type PolicyRule = (typeof POLICY_RULES)[number];
