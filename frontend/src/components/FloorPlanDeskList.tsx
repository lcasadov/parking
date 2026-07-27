import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  deskStatePillClass,
  markerStateClass,
  matchesDeskSearch,
} from '../utils/floorPlan';
import type { FloorPlanDesk } from '../types/floorPlan';

interface FloorPlanDeskListProps {
  desks: FloorPlanDesk[];
  // Acción sobre un puesto LIBRE: el ADMIN abre el modal de asignación; el empleado
  // abre la confirmación de solicitud. Los puestos no libres se muestran pero no son
  // accionables (informativos). Si se omite, la lista es de solo lectura.
  onSelect?: (desk: FloorPlanDesk) => void;
  // Etiqueta accesible de la acción por fila (varía por rol: "Asignar" / "Solicitar").
  actionLabelKey?: string;
}

// Contenido común de una fila (icono + estado + número + categoría + pill), usado
// tanto por la fila-botón (puesto libre accionable) como por la fila estática.
function DeskRowContent({ desk }: { desk: FloorPlanDesk }) {
  const { t } = useTranslation();
  return (
    <>
      <span className="plano-card-ico" aria-hidden="true">
        <i className={`ti ${desk.category === 'EXECUTIVE' ? 'ti-armchair' : 'ti-device-desktop'}`} />
        <span className={`plano-side-dot ${markerStateClass(desk.state)}`} />
      </span>
      <span className="side-meta">
        <span className="plano-side-desk">
          {t('floorPlan.deskNumber', { number: desk.deskNumber })}
        </span>
        <span className="sub">{t(`desks.category.${desk.category}`)}</span>
      </span>
      <span className={`pill ${deskStatePillClass(desk.state)}`}>
        {t(`floorPlan.states.${desk.state}`)}
      </span>
    </>
  );
}

// Listado de puestos a TODO EL ANCHO bajo el mapa (rediseño Plano §6.3), con búsqueda
// por número. Cada puesto LIBRE es un botón que dispara `onSelect` (asignar/solicitar
// según el rol); los no libres se listan como filas informativas.
export function FloorPlanDeskList({ desks, onSelect, actionLabelKey }: FloorPlanDeskListProps) {
  const { t } = useTranslation();
  const [search, setSearch] = useState('');
  const filtered = useMemo(
    () => desks.filter((desk) => matchesDeskSearch(desk, search)),
    [desks, search],
  );

  return (
    <section className="plano-desks" aria-label={t('floorPlan.side.label')}>
      <div className="plano-desks-head">
        <h2 className="plano-desks-title">{t('floorPlan.side.title')}</h2>
        <div className="search-box plano-desks-search">
          <i className="ti ti-search" aria-hidden="true" />
          <input
            type="search"
            className="field-input"
            value={search}
            aria-label={t('floorPlan.side.searchLabel')}
            placeholder={t('floorPlan.side.searchPlaceholder')}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <p className="plano-side-empty">{t('floorPlan.side.empty')}</p>
      ) : (
        <ul className="plano-desks-grid" aria-label={t('floorPlan.side.title')}>
          {filtered.map((desk) => {
            const actionable = onSelect !== undefined && desk.state === 'FREE';
            return (
              <li key={desk.deskId} className="plano-side-row">
                {actionable ? (
                  <button
                    type="button"
                    className="plano-side-row-btn plano-card is-actionable"
                    aria-label={t(actionLabelKey ?? 'floorPlan.side.rowAction', {
                      number: desk.deskNumber,
                    })}
                    onClick={() => onSelect(desk)}
                  >
                    <DeskRowContent desk={desk} />
                  </button>
                ) : (
                  <div className="plano-card is-static">
                    <DeskRowContent desk={desk} />
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
