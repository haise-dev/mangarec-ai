import { useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { getApiErrorMessage } from "@/api/apiClient";
import { resendEmailVerification, verifyEmail } from "@/api/endpoints/authApi";
import { FormAlert } from "@/components/common/FormAlert";
import { AuthLayout } from "./AuthLayout";

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const otpPattern = /^[0-9]{6}$/;

export default function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const initialEmail = useMemo(() => searchParams.get("email") ?? "", [searchParams]);

  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isResending, setIsResending] = useState(false);

  const normalizedEmail = email.trim();

  const validateEmail = () => {
    if (!emailPattern.test(normalizedEmail)) {
      setError("Email không hợp lệ.");
      return false;
    }
    return true;
  };

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!validateEmail()) return;
    if (!otpPattern.test(otp)) {
      setError("OTP phải gồm đúng 6 chữ số.");
      return;
    }

    setIsSubmitting(true);
    try {
      await verifyEmail({ email: normalizedEmail, otp });
      setSuccess("Email đã được xác thực. Đang chuyển sang đăng nhập...");
      window.setTimeout(() => {
        navigate("/login", {
          replace: true,
          state: { message: "Email đã xác thực. Bạn có thể đăng nhập ngay." },
        });
      }, 900);
    } catch (verifyError) {
      setError(getApiErrorMessage(verifyError, "Không thể xác thực email. Vui lòng kiểm tra mã OTP."));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleResend = async () => {
    setError("");
    setSuccess("");
    if (!validateEmail()) return;

    setIsResending(true);
    try {
      await resendEmailVerification({ email: normalizedEmail });
      setSuccess("Nếu tài khoản tồn tại và chưa xác thực, MangaRec đã gửi mã OTP mới.");
    } catch (resendError) {
      setError(getApiErrorMessage(resendError, "Không thể gửi lại mã OTP lúc này."));
    } finally {
      setIsResending(false);
    }
  };

  return (
    <AuthLayout
      eyebrow="Xác thực email"
      title="Nhập mã OTP"
      subtitle="MangaRec đã gửi mã 6 số đến email bạn dùng khi đăng ký. Xác thực email trước khi đăng nhập."
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

        <button className="primary-button" type="submit" disabled={isSubmitting || isResending}>
          {isSubmitting ? "Đang xác thực..." : "Xác thực email"}
        </button>
      </form>

      <p className="auth-switch">
        Chưa nhận được mã?{" "}
        <button className="link-button" type="button" onClick={handleResend} disabled={isSubmitting || isResending}>
          {isResending ? "Đang gửi..." : "Gửi lại OTP"}
        </button>
      </p>
      <p className="auth-switch">
        Đã xác thực? <Link to="/login">Đăng nhập</Link>
      </p>
    </AuthLayout>
  );
}