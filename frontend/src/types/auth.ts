// Tipos derivados del contrato docs/openapi.yaml (componentes Auth).

export type Role = 'ADMIN' | 'EMPLOYEE';

// CurrentUser: schema #/components/schemas/CurrentUser.
// passwordMustChange se trata como SIEMPRE presente (decision de bootstrap).
export interface CurrentUser {
  employeeId: number;
  login: string;
  firstName?: string;
  lastName?: string;
  role: Role;
  passwordMustChange: boolean;
}

// LoginRequest: schema #/components/schemas/LoginRequest.
export interface LoginRequest {
  login: string;
  password: string;
}

// ChangePasswordRequest: schema #/components/schemas/ChangePasswordRequest.
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

// ApiError: schema #/components/schemas/ApiError.
export interface ApiError {
  error: string;
  message: string;
  timestamp?: string;
  fields?: Record<string, string>;
}
