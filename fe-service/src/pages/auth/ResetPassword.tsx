import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword } from "@/api/endpoints/authApi";
import { getApiErrorMessage } from "@/api/apiClient";
import { FormAlert } from "@/components/common/FormAlert";
import { PasswordField } from "@/components/common/PasswordField";
import { AuthLayout } from "./AuthLayout";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const otpPattern = /^[0-9]{6}$/;

export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialEmail = useMemo(() => searchParams.get("email") ?? "", [searchParams]);

  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
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

    if (!otpPattern.test(otp)) {
      setError("OTP phải gồm đúng 6 chữ số.");
      return;
    }

    if (newPassword.length < 8) {
      setError("Mật khẩu mới phải có ít nhất 8 ký tự.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsLoading(true);
    try {
      await resetPassword({ email: normalizedEmail, otp, newPassword, confirmPassword });
      setSuccess("Mật khẩu đã được cập nhật. Đang chuyển về đăng nhập...");
      window.setTimeout(() => {
        navigate("/login", {
          replace: true,
          state: { message: "Mật khẩu đã được cập nhật. Vui lòng đăng nhập lại." },
        });
      }, 900);
    } catch (resetError) {
      setError(getApiErrorMessage(resetError, "Không thể đặt lại mật khẩu. Vui lòng kiểm tra mã OTP."));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthLayout
      eyebrow="Mã xác thực"
      title="Đặt lại mật khẩu"
      subtitle="Nhập mã OTP trong email và chọn mật khẩu mới cho tài khoản MangaRec."
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

        <label className="field" htmlFor="otp">
          <span>OTP</span>
          <input
            id="otp"
            type="text"
            value={otp}
            inputMode="numeric"
            maxLength={6}
            placeholder="123456"
            onChange={(event) => setOtp(event.target.value.replace(/\D/g, "").slice(0, 6))}
            required
          />
        </label>

        <PasswordField
          id="newPassword"
          label="Mật khẩu mới"
          value={newPassword}
          autoComplete="new-password"
          placeholder="Tối thiểu 8 ký tự"
          onChange={setNewPassword}
        />

        <PasswordField
          id="confirmPassword"
          label="Nhập lại mật khẩu mới"
          value={confirmPassword}
          autoComplete="new-password"
          placeholder="Nhập lại mật khẩu mới"
          onChange={setConfirmPassword}
        />

        <button className="primary-button" type="submit" disabled={isLoading}>
          {isLoading ? "Đang cập nhật..." : "Đặt lại mật khẩu"}
        </button>
      </form>

      <p className="auth-switch">
        Chưa có mã? <Link to="/forgot-password">Gửi lại OTP</Link>
      </p>
    </AuthLayout>
  );
}