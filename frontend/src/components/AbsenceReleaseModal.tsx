import { useMemo, useState } from 'react';
import type { TFunction } from 'i18next';
import { useTranslation } from 'react-i18next';
import { Button } from './Button';
import { Dialog } from './Dialog';
import { InfoBanner } from './InfoBanner';
import { MultiSelectCalendar } from './MultiSelectCalendar';
import { useCreateRelease, useMyReleasesQuery } from '../hooks/useReleases';
import { useToast } from '../hooks/useToast';
import { addDaysIso, isoWeekday } from '../utils/calendar';
import { todayIso } from '../utils/requests';
import type { FixedAssignmentGroup } from '../utils/fixedAssignments';
import type { Release } from '../types/release';
import type { ResourceType } from '../types/request';

// Modo de selección de días: sueltos (toggle por día) o rango (dos clics: inicio y fin).
type SelectMode = 'single' | 'range';

// Un recurso fijo a liberar en una fecha concreta.
interface ReleaseItem {
  date: string;
  parkingSpaceId: number;
  resourceType: ResourceType;
}

// Clave única de una liberación (tipo·recurso·fecha), para no reofrecer lo ya liberado.
function releaseKey(resourceType: ResourceType, resourceId: number, date: string): string {
  return `${resourceType}|${resourceId}|${date}`;
}

// Conjunto de recursos ya liberados (tipo·recurso·fecha) a partir de las liberaciones
// propias vivas: se excluyen del envío para evitar el 409 "ya liberado".
function releasedKeySet(releases: Release[] | undefined): Set<string> {
  const set = new Set<string>();
  for (const release of releases ?? []) {
    set.add(releaseKey(release.resourceType ?? 'PARKING', release.parkingSpaceId, release.releaseDate));
  }
  return set;
}

// Todas las fechas ISO entre dos extremos (inclusive), en orden. El orden de los
// argumentos no importa. Para la selección por rango.
function datesInRange(a: string, b: string): string[] {
  const [start, end] = a <= b ? [a, b] : [b, a];
  const out: string[] = [];
  for (let d = start; d <= end; d = addDaysIso(d, 1)) {
    out.push(d);
  }
  return out;
}

// Para las fechas elegidas y los tipos habilitados, calcula qué recursos fijos
// existen ese día de la semana (los únicos que se pueden liberar) y AÚN no están
// liberados (excluye los ya liberados para no provocar un 409 al reintentar).
function buildReleaseItems(
  dates: string[],
  groups: FixedAssignmentGroup[],
  types: Set<ResourceType>,
  alreadyReleased: Set<string>,
): ReleaseItem[] {
  const items: ReleaseItem[] = [];
  for (const date of dates) {
    const weekday = isoWeekday(date);
    for (const group of groups) {
      const isFixedThatDay = types.has(group.resourceType) && group.days.includes(weekday);
      const key = releaseKey(group.resourceType, group.parkingSpaceId, date);
      if (isFixedThatDay && !alreadyReleased.has(key)) {
        items.push({ date, parkingSpaceId: group.parkingSpaceId, resourceType: group.resourceType });
      }
    }
  }
  return items;
}

// ¿Cuántos días elegidos ya estaban liberados (para los tipos activos)? Alimenta el
// aviso "ya liberaste N de los elegidos" y evita el falso error al reenviar.
function countAlreadyReleased(
  dates: string[],
  groups: FixedAssignmentGroup[],
  types: Set<ResourceType>,
  alreadyReleased: Set<string>,
): number {
  let count = 0;
  for (const date of dates) {
    const weekday = isoWeekday(date);
    for (const group of groups) {
      const isFixedThatDay = types.has(group.resourceType) && group.days.includes(weekday);
      if (isFixedThatDay && alreadyReleased.has(releaseKey(group.resourceType, group.parkingSpaceId, date))) {
        count += 1;
      }
    }
  }
  return count;
}

// Mensaje del resumen: qué se liberará, o (si nada nuevo) por qué — todo ya
// liberado vs. sin fijo esos días.
function summaryMessage(
  itemCount: number,
  alreadyReleasedCount: number,
  resources: string,
  t: TFunction,
): string {
  if (itemCount > 0) {
    return t('myResources.absence.summary', { count: itemCount, resources });
  }
  if (alreadyReleasedCount > 0) {
    return t('myResources.absence.allAlreadyReleased');
  }
  return t('myResources.absence.nothing');
}

// Etiqueta legible del recurso de un item (para el resumen).
function labelForItem(
  item: ReleaseItem,
  groups: FixedAssignmentGroup[],
  labelFor: (group: FixedAssignmentGroup) => string,
): string {
  const group = groups.find(
    (g) => g.parkingSpaceId === item.parkingSpaceId && g.resourceType === item.resourceType,
  );
  return group ? labelFor(group) : String(item.parkingSpaceId);
}

interface AbsenceReleaseModalProps {
  groups: FixedAssignmentGroup[];
  labelFor: (group: FixedAssignmentGroup) => string;
  onClose: () => void;
  onDone: () => void;
}

// Modal de ausencia/vacaciones (Feature E): el empleado elige días en un CALENDARIO
// (selección múltiple, días sueltos) y libera en lote sus recursos fijos (plaza y/o
// puesto) de esos días. Solo libera lo que realmente existe cada día. Sin email.
export function AbsenceReleaseModal({ groups, labelFor, onClose, onDone }: AbsenceReleaseModalProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const release = useCreateRelease();

  const [dates, setDates] = useState<Set<string>>(new Set());
  const [mode, setMode] = useState<SelectMode>('single');
  // Primer extremo pendiente en modo rango (null = aún sin empezar rango).
  const [rangeAnchor, setRangeAnchor] = useState<string | null>(null);
  const hasParking = groups.some((g) => g.resourceType === 'PARKING');
  const hasDesk = groups.some((g) => g.resourceType === 'DESK');
  const [types, setTypes] = useState<Set<ResourceType>>(new Set(['PARKING', 'DESK']));

  // Liberaciones propias vivas: para no reofrecer (y por tanto no fallar con 409)
  // los días que ya tienes liberados.
  const myReleases = useMyReleasesQuery({ size: 200 });
  const alreadyReleased = useMemo(
    () => releasedKeySet(myReleases.data?.content),
    [myReleases.data],
  );

  const sortedDates = useMemo(() => [...dates].sort((a, b) => a.localeCompare(b)), [dates]);
  const items = useMemo(
    () => buildReleaseItems(sortedDates, groups, types, alreadyReleased),
    [sortedDates, groups, types, alreadyReleased],
  );
  const alreadyReleasedCount = useMemo(
    () => countAlreadyReleased(sortedDates, groups, types, alreadyReleased),
    [sortedDates, groups, types, alreadyReleased],
  );
  // Días ISO ya liberados (de los tipos activos) → fondo azul en el calendario.
  const releasedDays = useMemo(() => {
    const set = new Set<string>();
    for (const release of myReleases.data?.content ?? []) {
      if (types.has(release.resourceType ?? 'PARKING')) {
        set.add(release.releaseDate);
      }
    }
    return set;
  }, [myReleases.data, types]);

  function toggleDate(iso: string): void {
    setDates((prev) => {
      const next = new Set(prev);
      if (next.has(iso)) {
        next.delete(iso);
      } else {
        next.add(iso);
      }
      return next;
    });
  }

  // Clic en el calendario: en "sueltos" alterna el día; en "rango" el 1er clic fija
  // el inicio y el 2º completa el rango (reemplazando la selección previa).
  function handleCalendarClick(iso: string): void {
    if (mode === 'single') {
      toggleDate(iso);
      return;
    }
    if (rangeAnchor === null) {
      setDates(new Set([iso]));
      setRangeAnchor(iso);
    } else {
      setDates(new Set(datesInRange(rangeAnchor, iso)));
      setRangeAnchor(null);
    }
  }

  // Cambio de modo: limpia el extremo pendiente del rango (la selección se conserva).
  function changeMode(next: SelectMode): void {
    setMode(next);
    setRangeAnchor(null);
  }

  // Vacía toda la selección de días (y el extremo pendiente del rango).
  function clearDates(): void {
    setDates(new Set());
    setRangeAnchor(null);
  }

  function toggleType(type: ResourceType): void {
    setTypes((prev) => {
      const next = new Set(prev);
      if (next.has(type)) {
        next.delete(type);
      } else {
        next.add(type);
      }
      return next;
    });
  }

  async function handleConfirm(): Promise<void> {
    const settled = await Promise.allSettled(
      items.map((item) =>
        release.mutateAsync({
          releaseDate: item.date,
          parkingSpaceId: item.parkingSpaceId,
          ...(item.resourceType === 'DESK' ? { resourceType: item.resourceType } : {}),
        }),
      ),
    );
    const released = settled.filter((r) => r.status === 'fulfilled').length;
    if (released > 0) {
      toast.success('myResources.absence.done');
    }
    if (released === 0 && settled.length > 0) {
      toast.error('myResources.absence.failed');
    }
    onDone();
  }

  const summaryResources = [...new Set(items.map((i) => labelForItem(i, groups, labelFor)))].join(
    ', ',
  );

  const footer = (
    <>
      <Button variant="white" onClick={onClose}>
        {t('myResources.absence.cancel')}
      </Button>
      <Button
        variant="green"
        icon="calendar-off"
        loading={release.isPending}
        disabled={items.length === 0}
        onClick={() => void handleConfirm()}
      >
        {t('myResources.absence.confirm', { count: items.length })}
      </Button>
    </>
  );

  return (
    <Dialog
      open
      onOpenChange={(open) => {
        if (!open) {
          onClose();
        }
      }}
      title={t('myResources.absence.title')}
      icon="beach"
      footer={footer}
    >
      <p className="hint">{t('myResources.absence.intro')}</p>

      <div
        className="absence-mode"
        role="radiogroup"
        aria-label={t('myResources.absence.mode.label')}
      >
        <label className="radio-field">
          <input
            type="radio"
            name="absence-mode"
            checked={mode === 'single'}
            onChange={() => changeMode('single')}
          />
          <span>{t('myResources.absence.mode.single')}</span>
        </label>
        <label className="radio-field">
          <input
            type="radio"
            name="absence-mode"
            checked={mode === 'range'}
            onChange={() => changeMode('range')}
          />
          <span>{t('myResources.absence.mode.range')}</span>
        </label>
        <button
          type="button"
          className="absence-clear"
          onClick={clearDates}
          disabled={dates.size === 0}
        >
          <i className="ti ti-eraser" aria-hidden="true" />
          {t('myResources.absence.clear')}
        </button>
      </div>

      <MultiSelectCalendar
        selected={dates}
        onToggle={handleCalendarClick}
        minIso={todayIso()}
        releasedDays={releasedDays}
      />

      {hasParking && hasDesk ? (
        <div className="absence-types">
          <span className="field-label">{t('myResources.absence.whatToRelease')}</span>
          <label className="checkbox-field">
            <input
              type="checkbox"
              checked={types.has('PARKING')}
              onChange={() => toggleType('PARKING')}
            />
            <span>{t('requests.create.resourceParking')}</span>
          </label>
          <label className="checkbox-field">
            <input type="checkbox" checked={types.has('DESK')} onChange={() => toggleType('DESK')} />
            <span>{t('requests.create.resourceDesk')}</span>
          </label>
        </div>
      ) : null}

      {sortedDates.length > 0 ? (
        <div className="absence-summary">
          <InfoBanner variant={items.length > 0 ? 'green' : 'amber'} icon="info-circle">
            {summaryMessage(items.length, alreadyReleasedCount, summaryResources, t)}
          </InfoBanner>
          {items.length > 0 && alreadyReleasedCount > 0 ? (
            <p className="rc-note" role="status">
              <i className="ti ti-info-circle" aria-hidden="true" />
              <span>{t('myResources.absence.someAlreadyReleased', { count: alreadyReleasedCount })}</span>
            </p>
          ) : null}
        </div>
      ) : null}
    </Dialog>
  );
}
