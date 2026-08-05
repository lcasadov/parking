import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/useAuth';
import { useExport } from '../hooks/useExport';
import type { Role } from '../types/auth';
import type { ExportFormat } from '../types/employee';
import { Button } from './Button';

interface ExportMenuProps {
  // Endpoint de exportacion (ver EXPORT_PATHS en api/exportApi).
  path: string;
  // Base del nombre de fichero si el backend no envia Content-Disposition.
  fallbackBase: string;
  // Si se indica, el menu solo se renderiza para ese rol (defensa en profundidad;
  // el backend sigue siendo la autoridad). tasks §4.6.
  requiredRole?: Role;
  // Estilo de los botones (por defecto blanco; verde para la acción primaria).
  variant?: 'white' | 'green';
  // Formatos ofrecidos (por defecto CSV + XLSX). Permite ocultar formatos puntualmente.
  formats?: ExportFormat[];
}

// Menu reutilizable de exportacion: selector de formato (CSV / XLSX) que dispara
// la descarga del fichero binario. Deshabilita los botones mientras descarga y
// delega el manejo de 429/errores en useExport. tasks §4.1.
export function ExportMenu({
  path,
  fallbackBase,
  requiredRole,
  variant = 'white',
  formats = ['csv', 'xlsx'],
}: ExportMenuProps) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const mutation = useExport(path, fallbackBase);

  if (requiredRole && user?.role !== requiredRole) {
    return null;
  }

  const isBusy = mutation.isPending;

  function download(format: ExportFormat): void {
    mutation.mutate(format);
  }

  // Con un único formato el botón dice simplemente "Exportar" (consistente en todas las
  // pantallas); con varios, cada uno lleva su formato ("Exportar CSV" / "Exportar XLSX").
  const single = formats.length === 1;

  return (
    <div className="export-menu" role="group" aria-label={t('exports.groupLabel')}>
      {formats.includes('csv') ? (
        <Button variant={variant} icon="download" disabled={isBusy} onClick={() => download('csv')}>
          {single ? t('exports.export') : t('exports.csv')}
        </Button>
      ) : null}
      {formats.includes('xlsx') ? (
        <Button variant={variant} icon="download" disabled={isBusy} onClick={() => download('xlsx')}>
          {single ? t('exports.export') : t('exports.xlsx')}
        </Button>
      ) : null}
    </div>
  );
}
