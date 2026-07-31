import { motion, useReducedMotion } from 'framer-motion';
import { useMemo, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router-dom';
import { PageFrame } from '../components/PageFrame';
import { PersonSelect, type PersonOption } from '../components/PersonSelect';
import { SectionSwitch, type SectionSwitchItem } from '../components/SectionSwitch';
import { useSelectableReleaseEmployeesQuery } from '../hooks/useReleaseSelection';
import { addDaysIso } from '../utils/calendar';
import { todayIso } from '../utils/releases';
import { DUR, EASE } from '../theme/motion';
import { AdministrativeReleasesPage } from './AdministrativeReleasesPage';
import { ReleaseByDatePage } from './ReleaseByDatePage';
import { MyAdministrativeReleasesPage } from './MyAdministrativeReleasesPage';

type ReleaseHubTab = 'byEmployee' | 'byDate' | 'history';
const DEFAULT_TAB: ReleaseHubTab = 'byEmployee';

function isReleaseHubTab(value: string | null): value is ReleaseHubTab {
  return value === 'byEmployee' || value === 'byDate' || value === 'history';
}

// Destino "Liberar" (app-shell spec, fusion de secciones): pestañas Por empleado |
// Por fecha | Historial, que montan las paginas ya existentes tal cual (D3: agrupacion
// de UI, cero cambios de contrato). Disponible para ADMIN y AGENCIA (design §D5;
// AGENCIA se orienta con "Historial" = sus propias liberaciones administrativas). La
// pestaña activa se refleja en `?tab=` para que las rutas antiguas redirijan aqui.
export function ReleaseHubPage() {
  const { t } = useTranslation();
  const reduceMotion = useReducedMotion();
  const [searchParams, setSearchParams] = useSearchParams();
  const requested = searchParams.get('tab');
  const tab: ReleaseHubTab = isReleaseHubTab(requested) ? requested : DEFAULT_TAB;

  // Los selectores (empleado en "Por empleado", fecha en "Por fecha") se izan al
  // control-row, a la derecha del conmutador. Su estado vive aqui para compartirlo.
  const [employeeId, setEmployeeId] = useState<number | null>(null);
  const [date, setDate] = useState<string>(todayIso());
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employeePeople = useMemo<PersonOption[]>(
    () =>
      (employeesQuery.data ?? []).map((employee) => ({
        id: employee.id,
        name: employee.fullName,
        sub: employee.category ? t(`employees.category.${employee.category}`) : undefined,
        subIcon: 'stairs-up',
      })),
    [employeesQuery.data, t],
  );

  const items: SectionSwitchItem[] = [
    { id: 'byEmployee', label: t('releases.hub.tabs.byEmployee'), icon: 'user' },
    { id: 'byDate', label: t('releases.hub.tabs.byDate'), icon: 'calendar' },
    { id: 'history', label: t('releases.hub.tabs.history'), icon: 'history' },
  ];

  function handleChange(id: string): void {
    if (isReleaseHubTab(id)) {
      setSearchParams(id === DEFAULT_TAB ? {} : { tab: id }, { replace: true });
    }
  }

  function renderTab(): ReactNode {
    if (tab === 'byDate') {
      return <ReleaseByDatePage embedded date={date} />;
    }
    if (tab === 'history') {
      return <MyAdministrativeReleasesPage embedded />;
    }
    return <AdministrativeReleasesPage embedded employeeId={employeeId} />;
  }

  // Control derecho del control-row según la pestaña: buscador de persona (empleado)
  // en "Por empleado" y selector de fecha en "Por fecha". "Historial" no lleva control.
  function renderControl(): ReactNode {
    if (tab === 'byEmployee') {
      return (
        <PersonSelect
          people={employeePeople}
          value={employeeId}
          onChange={setEmployeeId}
          loading={employeesQuery.isLoading}
          placeholder={t('releases.employeeWeek.selectEmployee')}
          searchPlaceholder={t('releases.hub.searchPerson')}
          emptyLabel={t('releases.hub.noPerson')}
          ariaLabel={t('releases.employeeWeek.employee')}
        />
      );
    }
    if (tab === 'byDate') {
      // El día mínimo es hoy (no se libera en el pasado): la flecha "anterior" se
      // deshabilita al llegar a hoy. Comparación lexicográfica válida en ISO YYYY-MM-DD.
      const atMin = date <= todayIso();
      return (
        <div className="release-date-nav">
          <button
            type="button"
            className="release-date-nav-btn"
            aria-label={t('releases.byDate.prevDay')}
            disabled={atMin}
            onClick={() => setDate(addDaysIso(date, -1))}
          >
            <i className="ti ti-chevron-left" aria-hidden="true" />
          </button>
          <label className="release-date-control">
            <i className="ti ti-calendar" aria-hidden="true" />
            <input
              type="date"
              className="release-date-control-input"
              aria-label={t('releases.byDate.dateLabel')}
              value={date}
              min={todayIso()}
              onChange={(event) => setDate(event.target.value)}
              onClick={(event) => event.currentTarget.showPicker?.()}
            />
          </label>
          <button
            type="button"
            className="release-date-nav-btn"
            aria-label={t('releases.byDate.nextDay')}
            onClick={() => setDate(addDaysIso(date, 1))}
          >
            <i className="ti ti-chevron-right" aria-hidden="true" />
          </button>
        </div>
      );
    }
    return null;
  }

  return (
    <PageFrame
      eyebrow={t('releases.hub.eyebrow')}
      title={t('releases.hub.title')}
      bodyLabel={t('releases.hub.title')}
      resourceSelector={
        <SectionSwitch
          items={items}
          active={tab}
          onChange={handleChange}
          ariaLabel={t('releases.hub.title')}
          className="pf-lead"
        />
      }
      toolbar={renderControl()}
    >
      <motion.div
        key={tab}
        className="tab-fade-panel"
        initial={reduceMotion ? { opacity: 1 } : { opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: DUR.fast, ease: EASE.standard }}
      >
        {renderTab()}
      </motion.div>
    </PageFrame>
  );
}
