import * as Select from '@radix-ui/react-select';
import { useId } from 'react';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface SelectFieldProps {
  label: string;
  value: string;
  onValueChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  disabled?: boolean;
}

// Select accesible sobre Radix Select (Ola A · primitivas). Sustituye a los
// <select> nativos cuando se necesita un panel con el look ALEATICA y navegación
// por teclado consistente (flechas/typeahead) en todas las plataformas. La lógica
// no cambia: onValueChange devuelve el value igual que onChange del select nativo.
export function SelectField({
  label,
  value,
  onValueChange,
  options,
  placeholder,
  disabled = false,
}: SelectFieldProps) {
  const id = useId();
  return (
    <div className="auth-field">
      <label className="field-label" htmlFor={id}>
        {label}
      </label>
      <Select.Root value={value} onValueChange={onValueChange} disabled={disabled}>
        <Select.Trigger id={id} className="rx-select-trigger" aria-label={label}>
          <Select.Value placeholder={placeholder} />
          <Select.Icon className="rx-select-icon">
            <i className="ti ti-chevron-down" aria-hidden="true" />
          </Select.Icon>
        </Select.Trigger>
        <Select.Portal>
          <Select.Content className="rx-select-content" position="popper" sideOffset={6}>
            <Select.Viewport className="rx-select-viewport">
              {options.map((option) => (
                <Select.Item
                  key={option.value}
                  value={option.value}
                  disabled={option.disabled}
                  className="rx-select-item"
                >
                  <Select.ItemText>{option.label}</Select.ItemText>
                  <Select.ItemIndicator className="rx-select-indicator">
                    <i className="ti ti-check" aria-hidden="true" />
                  </Select.ItemIndicator>
                </Select.Item>
              ))}
            </Select.Viewport>
          </Select.Content>
        </Select.Portal>
      </Select.Root>
    </div>
  );
}
