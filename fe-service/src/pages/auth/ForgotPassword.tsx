import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { forgotPassword } from "@/api/endpoints/authApi";
import { getApiErrorMessage } from "@/api/apiClient";
import { FormAlert } from "@/components/common/FormAlert";
import { AuthLayout } from "./AuthLayout";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export default function ForgotPassword() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const normalizedEmail = email.trim();
    if (!emailPattern.test(normalizedEmail)) {
      setError("Email không hợp lệ.");
      return;
    }

    setIsLoading(true);
    try {
      await forgotPassword({ email: normalizedEmail });
      setSuccess("Nếu email tồn tại, MangaRec đã gửi mã OTP đến hộp thư của bạn.");
      window.setTimeout(() => {
        navigate(`/reset-password?email=${encodeURIComponent(normalizedEmail)}`);
      }, 900);
    } catch (forgotError) {
      setError(getApiErrorMessage(forgotError, "Không gửi được mã OTP. Vui lòng thử lại."));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      eyebrow="Khôi phục mật khẩu"
      title="Quên mật khẩu"
      subtitle="Nhập email đã đăng ký. Nếu tài khoản tồn tại, bạn sẽ nhận được mã OTP 6 số."
    >
      <form className="auth-form" onSubmit={handleSubmit}>
        {error ? <FormAlert type="error">{error}</FormAlert> : null}
        {success ? <FormAlert type="success">{success}</FormAlert> : null}

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

        <button className="primary-button" type="submit" disabled={isLoading}>
          {isLoading ? "Đang gửi mã..." : "Gửi mã OTP"}
        </button>
      </form>

      <p className="auth-switch">
        Đã có mã? <Link to="/reset-password">Đặt lại mật khẩu</Link>
      </p>
      <p className="auth-switch">
        Nhớ mật khẩu? <Link to="/login">Đăng nhập</Link>
      </p>
    </AuthLayout>
  );
}