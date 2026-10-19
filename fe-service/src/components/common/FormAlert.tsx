import type { ReactNode } from "react";

type FormAlertProps = {
  type: "error" | "success" | "info";
  children: ReactNode;
};

export function FormAlert({ type, children }: FormAlertProps) {
  return <div className={`form-alert form-alert--${type}`}>{children}</div>;
}
