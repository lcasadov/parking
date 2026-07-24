import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Avatar } from '../Avatar';
import { SearchBox } from '../SearchBox';
import { Spinner } from '../Spinner';
import { useSelectableReleaseEmployeesQuery } from '../../hooks/useReleaseSelection';
import type { EmployeeOption } from '../../types/releaseSelection';

interface StepEmployeeProps {
  employeeId: number | null;
  onChange: (employeeId: number) => void;
}

// Iniciales a partir del nombre completo: primera letra de las dos primeras
// palabras (o dos de la única palabra). Se usa como semilla estable del avatar.
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

function matches(employee: EmployeeOption, query: string): boolean {
  return employee.fullName.toLowerCase().includes(query.trim().toLowerCase());
}

// Paso 3 — Empleado: selector buscable con avatar + nombre (modo ADMIN). Reutiliza
// GET /releases/employees vía useSelectableReleaseEmployeesQuery.
export function StepEmployee({ employeeId, onChange }: StepEmployeeProps) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const employeesQuery = useSelectableReleaseEmployeesQuery();
  const employeesData = employeesQuery.data;

  const filtered = useMemo(
    () => (employeesData ?? []).filter((employee) => matches(employee, query)),
    [employeesData, query],
  );

  return (
    <div className="rzw-step-body">
      <p className="rzw-lead">{t('wizard.employee.lead')}</p>
      <SearchBox value={query} onValueChange={setQuery} label={t('wizard.employee.searchLabel')} placeholder={t('wizard.employee.searchPlaceholder')} />

      {employeesQuery.isLoading ? (
        <div className="rzw-center">
          <Spinner />
        </div>
      ) : null}

      {employeesQuery.isError ? (
        <p className="form-error" role="alert">
          {t('wizard.employee.error')}
        </p>
      ) : null}

      {!employeesQuery.isLoading && !employeesQuery.isError ? (
        <ul className="rzw-people" role="listbox" aria-label={t('wizard.employee.listLabel')}>
          {filtered.length === 0 ? (
            <li className="rzw-empty">{t('wizard.employee.noMatch')}</li>
          ) : (
            filtered.map((employee) => {
              const selected = employee.id === employeeId;
              return (
                <li key={employee.id}>
                  <button
                    type="button"
                    role="option"
                    aria-selected={selected}
                    className={`rzw-person${selected ? ' is-selected' : ''}`}
                    onClick={() => onChange(employee.id)}
                  >
                    <Avatar
                      initials={initialsFromName(employee.fullName)}
                      label={employee.fullName}
                      size="sm"
                      seed={employee.fullName}
                    />
                    <span className="rzw-person-name">{employee.fullName}</span>
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
