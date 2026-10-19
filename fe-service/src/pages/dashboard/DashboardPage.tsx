import { Link, useNavigate } from "react-router-dom";
import { ChatPanel } from "@/components/chat/ChatPanel";
import { useAuth } from "@/hooks/useAuth";

function labelSubscription(status?: string) {
  if (status === "PRO") return "Pro";
  return "Free";
}

function labelEmailVerified(value?: boolean) {
  return value ? "Đã xác thực" : "Chưa xác thực";
}

export function DashboardPage() {
  const navigate = useNavigate();
  const { logout, user } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <main className="app-surface dashboard-page">
      <nav className="top-nav top-nav--dashboard">
        <Link to="/" className="brand-mark brand-mark--dark">
          <span>M</span>
          <strong>MangaRec</strong>
        </Link>
        <button className="secondary-button" type="button" onClick={handleLogout}>
          Đăng xuất
        </button>
      </nav>

      <section className="dashboard-grid">
        <aside className="profile-card">
          <span className="surface-label">Tài khoản của bạn</span>
          <h1>Xin chào, {user?.name}</h1>
          <p>Không gian riêng để tiếp tục những cuộc trò chuyện và giữ gu đọc nhất quán.</p>

          <dl className="profile-list">
            <div>
              <dt>Email</dt>
              <dd>{user?.email}</dd>
            </div>
            <div>
              <dt>Gói hiện tại</dt>
              <dd>{labelSubscription(user?.subscriptionStatus)}</dd>
            </div>
            <div>
              <dt>Trạng thái email</dt>
              <dd>{labelEmailVerified(user?.emailVerified)}</dd>
            </div>
          </dl>
        </aside>

        <ChatPanel />
      </section>
    </main>
  );
}