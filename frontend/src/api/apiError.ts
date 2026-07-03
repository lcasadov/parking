import { AxiosError } from 'axios';
import type { ApiError } from '../types/auth';

// Extrae el cuerpo ApiError { error, message, fields, timestamp } de un error de
// Axios. Devuelve null si no es un AxiosError con cuerpo ApiError reconocible.
export function getApiError(error: unknown): ApiError | null {
  if (error instanceof AxiosError && error.response?.data) {
    const data = error.response.data as Partial<ApiError>;
    if (typeof data.error === 'string' && typeof data.message === 'string') {
      return data as ApiError;
    }
  }
  return null;
}

// Codigo de estado HTTP del error, o undefined si no aplica.
export function getStatus(error: unknown): number | undefined {
  return error instanceof AxiosError ? error.response?.status : undefined;
}

// Errores por campo (ApiError.fields) de un 409/400, o objeto vacio.
export function getFieldErrors(error: unknown): Record<string, string> {
  return getApiError(error)?.fields ?? {};
}
