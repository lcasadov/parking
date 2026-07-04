// Utilidades de descarga de ficheros binarios en el navegador.
// Reutilizadas por las exportaciones CSV/XLSX (init-exports, tasks §4).

const FILENAME_STAR = /filename\*=(?:UTF-8'')?([^;]+)/i;
const FILENAME_PLAIN = /filename="?([^";]+)"?/i;

// Extrae el nombre de fichero de una cabecera Content-Disposition
// ("attachment; filename=..."). Soporta filename* (RFC 5987) y filename simple.
// Devuelve `fallback` si la cabecera falta o no contiene un nombre reconocible.
export function parseContentDispositionFilename(
  header: string | undefined | null,
  fallback: string,
): string {
  if (!header) {
    return fallback;
  }
  const starMatch = FILENAME_STAR.exec(header);
  if (starMatch?.[1]) {
    return decodeURIComponent(starMatch[1].trim());
  }
  const plainMatch = FILENAME_PLAIN.exec(header);
  if (plainMatch?.[1]) {
    return plainMatch[1].trim();
  }
  return fallback;
}

// Dispara la descarga de un Blob via un anchor temporal y libera el object URL.
export function triggerBlobDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(url);
}
