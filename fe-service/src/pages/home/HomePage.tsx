import { Link } from "react-router-dom";
import { ChatPanel } from "@/components/chat/ChatPanel";
import { useAuth } from "@/hooks/useAuth";

export function HomePage() {
  const { isAuthenticated, user } = useAuth();

  return (
    <main className="app-surface home-page">
      <nav className="top-nav">
        <Link to="/" className="brand-mark brand-mark--dark">
          <span>M</span>
          <strong>MangaRec</strong>
        </Link>
        <div>
          {isAuthenticated ? (
            <Link className="nav-button" to="/dashboard">
              Không gian đọc của {user?.name}
            </Link>
          ) : (
            <>
              <Link className="nav-link" to="/login">
                Đăng nhập
              </Link>
              <Link className="nav-button" to="/register">
                Tạo tài khoản
              </Link>
            </>
          )}
        </div>
      </nav>

      <section className="landing-grid">
        <div className="landing-copy">
          <span className="surface-label">AI manga companion</span>
          <h1>Tìm bộ truyện hợp gu bằng một cuộc trò chuyện.</h1>
          <p>
            MangaRec lắng nghe mood, thể loại, độ dài và những bộ bạn từng thích để đề xuất manga dễ bắt đầu hơn.
          </p>
          <div className="landing-actions">
            <Link className="primary-button primary-button--link" to={isAuthenticated ? "/dashboard" : "/login"}>
              {isAuthenticated ? "Tiếp tục tư vấn" : "Bắt đầu với tài khoản"}
            </Link>
            <a className="secondary-button" href="#try-chat">
              Thử hỏi ngay
            </a>
          </div>

          <div className="trust-row" aria-label="MangaRec benefits">
            <span>Gợi ý theo ngữ cảnh</span>
            <span>Lưu lịch sử khi đăng nhập</span>
            <span>Hỗ trợ khách dùng thử</span>
          </div>
        </div>

        <div id="try-chat" className="landing-chat-card">
          <ChatPanel compact />
        </div>
      </section>
    </main>
  );
}