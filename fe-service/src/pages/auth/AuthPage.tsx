import { useCallback, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { getApiErrorMessage } from "@/api/apiClient";
import { FormAlert } from "@/components/common/FormAlert";
import { GoogleSignInButton } from "@/components/common/GoogleSignInButton";
import { PasswordField } from "@/components/common/PasswordField";
import { useAuth } from "@/hooks/useAuth";
import { AuthLayout } from "./AuthLayout";

interface AuthPageProps {
  type: "login" | "register";
}

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function AuthPage({ type }: AuthPageProps) {
  const isLogin = type === "login";
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register, loginWithGoogle } = useAuth();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState((location.state as { message?: string } | null)?.message ?? "");

  const validateCommon = () => {
    const trimmedEmail = email.trim();
    if (!emailPattern.test(trimmedEmail)) return "Email không hợp lệ.";
    if (password.length < 8) return "Mật khẩu phải có ít nhất 8 ký tự.";
    return "";
  };

  const handleLogin = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const validationError = validateCommon();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsLoading(true);
    try {
      await login({ email: email.trim(), password });
      setSuccess("Đăng nhập thành công. Đang mở không gian của bạn...");
      const from = (location.state as { from?: { pathname?: string } } | null)?.from?.pathname;
      navigate(from || "/dashboard", { replace: true });
    } catch (loginError) {
      setError(getApiErrorMessage(loginError, "Đăng nhập thất bại. Vui lòng kiểm tra email hoặc mật khẩu."));
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegister = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const validationError = validateCommon();
    if (validationError) {
      setError(validationError);
      return;
    }

    if (!name.trim()) {
      setError("Vui lòng nhập tên hiển thị.");
      return;
    }

    if (password !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsLoading(true);
    try {
      const user = await register({ name: name.trim(), email: email.trim(), password });
      navigate(`/verify-email?email=${encodeURIComponent(user.email)}`, { replace: true });
    } catch (registerError) {
      setError(getApiErrorMessage(registerError, "Đăng ký thất bại. Vui lòng thử lại."));
    } finally {
      setIsLoading(false);
    }
  };

  const handleGoogleCredential = useCallback(
    async (idToken: string) => {
      setError("");
      setSuccess("");
      setIsLoading(true);
      try {
        await loginWithGoogle(idToken);
        navigate("/dashboard", { replace: true });
      } catch (googleError) {
        setError(getApiErrorMessage(googleError, isLogin ? "Không thể đăng nhập bằng Google lúc này." : "Không thể đăng ký bằng Google lúc này."));
      } finally {
        setIsLoading(false);
      }
    },
    [isLogin, loginWithGoogle, navigate],
  );

  return (
    <AuthLayout
      eyebrow={isLogin ? "Chào mừng trở lại" : "Tạo hồ sơ đọc"}
      title={isLogin ? "Đăng nhập MangaRec" : "Tạo tài khoản MangaRec"}
      subtitle={
        isLogin
          ? "Tiếp tục những gợi ý đang dang dở và giữ lịch sử đọc của bạn an toàn."
          : "Tạo hồ sơ để MangaRec nhớ gu đọc, mood và những bộ truyện bạn đã được tư vấn."
      }
    >
      <form className="auth-form" onSubmit={isLogin ? handleLogin : handleRegister}>
        {error ? <FormAlert type="error">{error}</FormAlert> : null}
        {success ? <FormAlert type="success">{success}</FormAlert> : null}

        {!isLogin ? (
          <label className="field" htmlFor="name">
            <span>Tên hiển thị</span>
            <input
              id="name"
              type="text"
              value={name}
              autoComplete="name"
              placeholder="Manga Reader"
              onChange={(event) => setName(event.target.value)}
              required
            />
          </label>
        ) : null}

        <label className="field" htmlFor="email">
          <span>Email</span>
          <input
            id="email"
            type="email"
            value={email}
            autoComplete="email"
            placeholder="reader@example.com"
            onChange={(event) => setEmail(event.target.value)}
            required
          />
        </label>

        <PasswordField
          id="password"
          label="Mật khẩu"
          value={password}
          autoComplete={isLogin ? "current-password" : "new-password"}
          placeholder="Tối thiểu 8 ký tự"
          onChange={setPassword}
        />

        {!isLogin ? (
          <PasswordField
            id="confirmPassword"
            label="Nhập lại mật khẩu"
            value={confirmPassword}
            autoComplete="new-password"
            placeholder="Nhập lại mật khẩu"
            onChange={setConfirmPassword}
          />
        ) : null}

        {isLogin ? (
          <div className="form-row form-row--split">
            <span>Phiên đăng nhập sẽ được ghi nhớ trên thiết bị này.</span>
            <Link to="/forgot-password">Quên mật khẩu?</Link>
          </div>
        ) : null}

        <button className="primary-button" type="submit" disabled={isLoading}>
          {isLoading ? "Đang xử lý..." : isLogin ? "Đăng nhập" : "Tạo tài khoản"}
        </button>
      </form>

      <div className="auth-divider">
        <span />
        <small>hoặc</small>
        <span />
      </div>

      <GoogleSignInButton onCredential={handleGoogleCredential} disabled={isLoading} mode={isLogin ? "login" : "register"} />

      <p className="auth-switch">
        {isLogin ? "Bạn chưa có tài khoản?" : "Bạn đã có tài khoản?"} {" "}
        <Link to={isLogin ? "/register" : "/login"}>{isLogin ? "Tạo tài khoản" : "Đăng nhập"}</Link>
      </p>
    </AuthLayout>
  );
}
