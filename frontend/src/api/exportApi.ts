import { apiClient } from './apiClient';
import type { ExportFormat } from '../types/employee';
import { parseContentDispositionFilename, triggerBlobDownload } from '../utils/download';

// Endpoints de exportacion segun docs/openapi.yaml (tag Exports). El parametro
// `format` es el enum ExportFormatParam [csv, xlsx] (por defecto xlsx).
export const EXPORT_PATHS = {
  employees: '/employees/export',
  myData: '/employees/me/export',
  requests: '/requests/export',
  myRequests: '/requests/mine/export',
  audit: '/audit/export',
} as const;

const CONTENT_DISPOSITION = 'content-disposition';

// Descarga una exportacion binaria: GET <path>?format=<csv|xlsx> con responseType
// blob. Respeta el nombre de fichero de Content-Disposition; si falta, usa
// `${fallbackBase}.${format}`. Dispara la descarga en el navegador.
export async function runExport(
  path: string,
  format: ExportFormat,
  fallbackBase: string,
): Promise<void> {
  const response = await apiClient.get<Blob>(path, {
    params: { format },
    responseType: 'blob',
  });
  const filename = parseContentDispositionFilename(
    response.headers[CONTENT_DISPOSITION] as string | undefined,
    `${fallbackBase}.${format}`,
  );
  triggerBlobDownload(response.data, filename);
}
