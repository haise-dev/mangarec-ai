import { useEffect, useRef, useState } from "react";
import { FormAlert } from "@/components/common/FormAlert";

const GOOGLE_SCRIPT_ID = "google-identity-services";

type GoogleSignInButtonProps = {
  onCredential: (idToken: string) => Promise<void>;
  disabled?: boolean;
  mode?: "login" | "register";
};

function loadGoogleScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve();
      return;
    }

    const existing = document.getElementById(GOOGLE_SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      existing.addEventListener("load", () => resolve(), { once: true });
      existing.addEventListener("error", () => reject(new Error("Cannot load Google Identity script")), {
        once: true,
      });
      return;
    }

    const script = document.createElement("script");
    script.id = GOOGLE_SCRIPT_ID;
    script.src = "https://accounts.google.com/gsi/client";
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("Cannot load Google Identity script"));
    document.head.appendChild(script);
  });
}

export function GoogleSignInButton({ onCredential, disabled, mode = "login" }: GoogleSignInButtonProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const [status, setStatus] = useState<"idle" | "ready" | "unavailable" | "error">("idle");
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  useEffect(() => {
    let mounted = true;

    async function setupGoogleButton() {
      if (!clientId) {
        setStatus("unavailable");
        return;
      }

      try {
        await loadGoogleScript();
        if (!mounted || !containerRef.current || !window.google?.accounts?.id) return;

        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            if (response.credential) void onCredential(response.credential);
          },
          auto_select: false,
          cancel_on_tap_outside: true,
        });

        containerRef.current.innerHTML = "";
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: "outline",
          size: "large",
          text: mode === "register" ? "signup_with" : "signin_with",
          shape: "pill",
          width: 320,
        });
        setStatus("ready");
      } catch {
        if (mounted) setStatus("error");
      }
    }

    void setupGoogleButton();

    return () => {
      mounted = false;
    };
  }, [clientId, mode, onCredential]);

  if (status === "unavailable") {
    return <FormAlert type="info">Đăng nhập Google chưa được cấu hình cho frontend.</FormAlert>;
  }

  if (status === "error") {
    return <FormAlert type="error">Không thể mở đăng nhập Google. Hãy kiểm tra cấu hình Google Client ID hoặc dùng email.</FormAlert>;
  }

  return (
    <div className={disabled ? "google-button google-button--disabled" : "google-button"}>
      {status === "idle" ? <span>Đang chuẩn bị Google...</span> : null}
      <div ref={containerRef} />
    </div>
  );
}