import { useId, type InputHTMLAttributes } from 'react';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: boolean;
  hint?: string;
}

export function Input({ label, error = false, hint, id, className, ...rest }: InputProps) {
  const generatedId = useId();
  const inputId = id ?? generatedId;
  const hintId = hint ? `${inputId}-hint` : undefined;
  const fieldClasses = ['field-input', error ? 'danger' : '', className].filter(Boolean).join(' ');

  return (
    <div className="auth-field">
      <label className={`field-label${error ? ' red' : ''}`} htmlFor={inputId}>
        {label}
      </label>
      <input
        id={inputId}
        className={fieldClasses}
        aria-invalid={error || undefined}
        aria-describedby={hintId}
        {...rest}
      />
      {hint ? (
        <p className="hint" id={hintId}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}
