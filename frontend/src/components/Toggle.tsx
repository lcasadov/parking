interface ToggleProps {
  checked: boolean;
  onChange: (next: boolean) => void;
  label: string;
  disabled?: boolean;
  id?: string;
}

// Switch booleano accesible (role=switch + aria-checked). Al ser un <button>,
// el teclado (Espacio/Enter) y el foco funcionan de forma nativa. El texto
// visible dentro del boton actua como nombre accesible.
export function Toggle({ checked, onChange, label, disabled = false, id }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      id={id}
      aria-checked={checked}
      className={`toggle${checked ? ' on' : ''}`}
      disabled={disabled}
      onClick={() => onChange(!checked)}
    >
      <span className="track" aria-hidden="true">
        <span className="knob" />
      </span>
      <span>{label}</span>
    </button>
  );
}
