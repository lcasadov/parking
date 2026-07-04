import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/useAuth';
import { useExport } from '../hooks/useExport';
import { EXPORT_PATHS } from '../api/exportApi';
import { Button } from './Button';

// Accion RGPD "Exportar mis datos" (derecho de acceso, security-design §12):
// cualquier usuario autenticado descarga sus propios datos personales en XLSX.
// Solo visible con sesion activa. tasks §4.5.
export function ExportMyDataButton() {
  const { t } = useTranslation();
  const { user } = useAuth();
  const mutation = useExport(EXPORT_PATHS.myData, 'my-data');

  if (!user) {
    return null;
  }

  return (
    <Button
      variant="white"
      icon="file-download"
      disabled={mutation.isPending}
      onClick={() => mutation.mutate('xlsx')}
    >
      {t('exports.myData')}
    </Button>
  );
}
