import { Link, useNavigate } from "react-router-dom";
import type { ReactNode } from "react";

interface AuthLayoutProps {
  title: string;
  eyebrow: string;
  subtitle: string;
  children: ReactNode;
}

export function AuthLayout({ title, eyebrow, subtitle, children }: AuthLayoutProps) {
  const navigate = useNavigate();

  return (
    <main className="auth-shell">
      <section className="auth-hero" aria-label="MangaRec intro">
        <div className="hero-aura hero-aura--one" />
        <div className="hero-aura hero-aura--two" />
        <div className="manga-panel manga-panel--one" />
        <div className="manga-panel manga-panel--two" />
        <div className="auth-hero__content">
          <Link to="/" className="brand-mark" aria-label="MangaRec home">
            <span>M</span>
            <strong>MangaRec</strong>
          </Link>
          <div className="hero-copy">
            <span className="surface-label surface-label--dark">AI manga companion</span>
            <h1>Đăng nhập để giữ mạch gợi ý của riêng bạn.</h1>
            <p>
              Lưu những gì bạn thích, quay lại cuộc trò chuyện trước đó và để MangaRec hiểu gu đọc ngày càng tốt hơn.
            </p>
          </div>
          <button type="button" className="ghost-button" onClick={() => navigate("/")}>
            Khám phá trước
          </button>
        </div>
      </section>

      <section className="auth-card-wrap">
        <div className="auth-card">
          <div className="auth-card__header">
            <span className="surface-label">{eyebrow}</span>
            <h2>{title}</h2>
            <p>{subtitle}</p>
          </div>
          {children}
        </div>
      </section>
    </main>
  );
}