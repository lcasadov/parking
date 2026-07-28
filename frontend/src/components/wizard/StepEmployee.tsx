import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Avatar } from '../Avatar';
import { SearchBox } from '../SearchBox';
import { Spinner } from '../Spinner';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import { useVisitorsQuery } from '../../hooks/useVisitors';
import type { BeneficiaryType } from './wizardTypes';

interface StepEmployeeProps {
  beneficiaryType: BeneficiaryType;
  employeeId: number | null;
  visitorId: number | null;
  onBeneficiaryTypeChange: (type: BeneficiaryType) => void;
  onEmployeeChange: (employeeId: number) => void;
  onVisitorChange: (visitorId: number) => void;
}

// Persona seleccionable (empleado o visitante) normalizada para la lista común.
interface Person {
  id: number;
  name: string;
  // Sub-línea (categoría del empleado / documento del visitante) + su icono.
  sub?: string;
  subIcon?: string;
}

function initialsFromName(fullName: string): string {
  const parts = fullName.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

// Paso 3 — Beneficiario: elige EMPLEADO (interno) o VISITANTE (externo) y luego la
// persona, con buscador. El visitante reserva por su propio flujo y no recibe email.
export function StepEmployee({
  beneficiaryType,
  employeeId,
  visitorId,
  onBeneficiaryTypeChange,
  onEmployeeChange,
  onVisitorChange,
}: StepEmployeeProps) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const isVisitor = beneficiaryType === 'VISITOR';

  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const visitorsQuery = useVisitorsQuery({ page: 0, size: 100 });
  const loading = isVisitor ? visitorsQuery.isLoading : employeesQuery.isLoading;
  const error = isVisitor ? visitorsQuery.isError : employeesQuery.isError;

  const people: Person[] = useMemo(() => {
    if (isVisitor) {
      return (visitorsQuery.data?.content ?? []).map((v) => ({
        id: v.id,
        name: `${v.firstName} ${v.lastName}`.trim(),
        sub: v.nationalId,
        subIcon: 'id',
      }));
    }
    return (employeesQuery.data ?? []).map((e) => ({
      id: e.id,
      name: e.fullName,
      sub: e.category ? t(`employees.category.${e.category}`) : undefined,
      subIcon: 'stairs-up',
    }));
  }, [isVisitor, visitorsQuery.data, employeesQuery.data, t]);

  const selectedId = isVisitor ? visitorId : employeeId;
  const onSelect = isVisitor ? onVisitorChange : onEmployeeChange;

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (q === '') {
      return people;
    }
    return people.filter(
      (p) => p.name.toLowerCase().includes(q) || (p.sub?.toLowerCase().includes(q) ?? false),
    );
  }, [people, query]);

  function setMode(type: BeneficiaryType): void {
    if (type !== beneficiaryType) {
      setQuery('');
      onBeneficiaryTypeChange(type);
    }
  }

  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">{t('wizard.beneficiary.lead')}</p>

      <div className="rzw-segmented rzw-mode-toggle" role="tablist" aria-label={t('wizard.beneficiary.typeLabel')}>
        <button
          type="button"
          role="tab"
          aria-selected={!isVisitor}
          className={!isVisitor ? 'is-active' : ''}
          onClick={() => setMode('EMPLOYEE')}
        >
          <i className="ti ti-user" aria-hidden="true" />
          {t('wizard.beneficiary.employee')}
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={isVisitor}
          className={isVisitor ? 'is-active' : ''}
          onClick={() => setMode('VISITOR')}
        >
          <i className="ti ti-user-plus" aria-hidden="true" />
          {t('wizard.beneficiary.visitor')}
        </button>
      </div>

      <SearchBox
        value={query}
        onValueChange={setQuery}
        label={t('wizard.employee.searchLabel')}
        placeholder={t(isVisitor ? 'wizard.beneficiary.searchVisitor' : 'wizard.employee.searchPlaceholder')}
      />

      {loading ? (
        <div className="rzw-center">
          <Spinner />
        </div>
      ) : null}

      {error ? (
        <p className="form-error" role="alert">
          {t('wizard.employee.error')}
        </p>
      ) : null}

      {!loading && !error ? (
        <ul className="rzw-people" role="listbox" aria-label={t('wizard.employee.listLabel')}>
          {filtered.length === 0 ? (
            <li className="rzw-empty">
              {t(isVisitor ? 'wizard.beneficiary.noVisitor' : 'wizard.employee.noMatch')}
            </li>
          ) : (
            filtered.map((person) => {
              const selected = person.id === selectedId;
              return (
                <li key={person.id}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={selected}
                    className={`rzw-person${selected ? ' is-selected' : ''}`}
                    onClick={() => onSelect(person.id)}
                  >
                    <Avatar initials={initialsFromName(person.name)} label={person.name} size="sm" seed={person.name} />
                    <span className="rzw-person-main">
                      <span className="rzw-person-name">{person.name}</span>
                      {person.sub ? (
                        <span className="rzw-person-cat">
                          <i className={`ti ti-${person.subIcon}`} aria-hidden="true" />
                          {person.sub}
                        </span>
                      ) : null}
                    </span>
                    <span className="rzw-person-check" aria-hidden="true">
                      <i className="ti ti-check" />
                    </span>
                  </button>
                </li>
              );
            })
          )}
        </ul>
      ) : null}
    </div>
  );
}
