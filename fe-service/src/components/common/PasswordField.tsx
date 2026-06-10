import { useState } from "react";

type PasswordFieldProps = {
  id: string;
  label: string;
  value: string;
  placeholder?: string;
  autoComplete?: string;
  onChange: (value: string) => void;
};

export function PasswordField({
  id,
  label,
  value,
  placeholder,
  autoComplete,
  onChange,
}: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <label className="field" htmlFor={id}>
      <span>{label}</span>
      <div className="password-input">
        <input
          id={id}
          type={visible ? "text" : "password"}
          value={value}
          autoComplete={autoComplete}
          placeholder={placeholder}
          onChange={(event) => onChange(event.target.value)}
          required
        />
        <button type="button" onClick={() => setVisible((current) => !current)}>
          {visible ? "Ẩn" : "Hiện"}
        </button>
      </div>
    </label>
  );
}
