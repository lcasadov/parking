import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Avatar } from './Avatar';
import { SearchBox } from './SearchBox';

// Opción de persona (empleado o visitante) normalizada para el selector con búsqueda.
export interface PersonOption {
  id: number;
  name: string;
  // Sub-línea opcional (categoría del empleado / documento del visitante) + icono.
  sub?: string;
  subIcon?: string;
}

interface PersonSelectProps {
  people: PersonOption[];
  value: number | null;
  onChange: (id: number | null) => void;
  // Texto del disparador cuando no hay selección.
  placeholder: string;
  searchPlaceholder: string;
  emptyLabel: string;
  ariaLabel: string;
  loading?: boolean;
}

// Iniciales (max 2) del nombre para el avatar.
function initialsFromName(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return '?';
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

// Una fila del listado (option). Extraída para mantener baja la complejidad del render.
function PersonRow({
  person,
  selected,
  onPick,
}: {
  person: PersonOption;
  selected: boolean;
  onPick: (id: number) => void;
}) {
  return (
    <li>
      <button
        type="button"
        role="option"
        aria-selected={selected}
        className={`person-select-option${selected ? ' is-selected' : ''}`}
        onClick={() => onPick(person.id)}
      >
        <Avatar initials={initialsFromName(person.name)} label={person.name} size="sm" seed={person.name} />
        <span className="person-select-option-main">
          <span className="person-select-option-name">{person.name}</span>
          {person.sub ? (
            <span className="person-select-option-sub">
              <i className={`ti ti-${person.subIcon}`} aria-hidden="true" />
              {person.sub}
            </span>
          ) : null}
        </span>
        {selected ? <i className="ti ti-check person-select-option-check" aria-hidden="true" /> : null}
      </button>
    </li>
  );
}

// Selector de persona con búsqueda (combobox): disparador tipo "pill" (avatar +
// nombre + caret) que abre un popover con buscador y listado filtrable. Pensado para
// la fila de controles del marco. Soporta empleados y visitantes en una lista común.
export function PersonSelect({
  people,
  value,
  onChange,
  placeholder,
  searchPlaceholder,
  emptyLabel,
  ariaLabel,
  loading = false,
}: PersonSelectProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const rootRef = useRef<HTMLDivElement>(null);

  const selected = people.find((person) => person.id === value) ?? null;

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (q === '') {
      return people;
    }
    return people.filter(
      (person) =>
        person.name.toLowerCase().includes(q) || (person.sub?.toLowerCase().includes(q) ?? false),
    );
  }, [people, query]);

  // Cierra el popover al pulsar fuera o al presionar Escape.
  useEffect(() => {
    if (!open) {
      return undefined;
    }
    function onPointer(event: MouseEvent): void {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function onKey(event: KeyboardEvent): void {
      if (event.key === 'Escape') {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', onPointer);
    document.addEventListener('keydown', onKey);
    // Al abrir, el foco va al buscador para poder teclear de inmediato.
    rootRef.current?.querySelector<HTMLInputElement>('.person-select-pop input')?.focus();
    return () => {
      document.removeEventListener('mousedown', onPointer);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  function pick(id: number): void {
    onChange(id);
    setOpen(false);
    setQuery('');
  }

  return (
    <div className="person-select" ref={rootRef}>
      <button
        type="button"
        className="person-select-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-label={ariaLabel}
        onClick={() => setOpen((previous) => !previous)}
      >
        {selected ? (
          <Avatar initials={initialsFromName(selected.name)} label={selected.name} size="sm" seed={selected.name} />
        ) : (
          <span className="person-select-avatar-empty" aria-hidden="true">
            <i className="ti ti-user-search" />
          </span>
        )}
        <span className={`person-select-value${selected ? '' : ' is-placeholder'}`}>
          {selected ? selected.name : placeholder}
        </span>
        <i className="ti ti-selector person-select-caret" aria-hidden="true" />
      </button>

      {open ? (
        <div className="person-select-pop">
          <SearchBox
            value={query}
            onValueChange={setQuery}
            label={ariaLabel}
            placeholder={searchPlaceholder}
          />
          <ul className="person-select-list" role="listbox" aria-label={ariaLabel}>
            {loading ? (
              <li className="person-select-empty">{t('common.loading')}</li>
            ) : filtered.length === 0 ? (
              <li className="person-select-empty">{emptyLabel}</li>
            ) : (
              filtered.map((person) => (
                <PersonRow
                  key={person.id}
                  person={person}
                  selected={person.id === value}
                  onPick={pick}
                />
              ))
            )}
          </ul>
        </div>
      ) : null}
    </div>
  );
}
