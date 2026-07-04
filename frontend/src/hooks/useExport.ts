import { useMutation, type UseMutationResult } from '@tanstack/react-query';
import { getStatus } from '../api/apiError';
import { emitApiErrorToast } from '../api/events';
import { runExport } from '../api/exportApi';
import type { ExportFormat } from '../types/employee';

const RATE_LIMIT_STATUS = 429;

// Hook generico de exportacion: descarga el fichero binario del endpoint `path`
// en el formato elegido. En caso de 429 (limite de tasa) muestra un toast
// especifico; ante cualquier otro error, un toast de error generico. El backend
// es la autoridad de RBAC; este hook solo gestiona la descarga y sus errores.
export function useExport(
  path: string,
  fallbackBase: string,
): UseMutationResult<void, unknown, ExportFormat> {
  return useMutation<void, unknown, ExportFormat>({
    mutationFn: (format: ExportFormat) => runExport(path, format, fallbackBase),
    onError: (error) => {
      const messageKey =
        getStatus(error) === RATE_LIMIT_STATUS ? 'exports.errors.rateLimit' : 'exports.errors.generic';
      emitApiErrorToast(messageKey);
    },
  });
}
