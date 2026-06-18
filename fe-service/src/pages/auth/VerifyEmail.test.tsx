import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import VerifyEmail from "./VerifyEmail";
import { MemoryRouter } from "react-router-dom";
import { verifyEmail, resendEmailVerification } from "@/api/endpoints/authApi";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
    useSearchParams: () => [new URLSearchParams("?email=test@test.com")],
  };
});
vi.mock("@/api/endpoints/authApi", () => ({ verifyEmail: vi.fn(), resendEmailVerification: vi.fn() }));

describe("VerifyEmail", () => {
  it("submits otp and navigates on success", async () => {
    vi.mocked(verifyEmail).mockResolvedValue(null as any);
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.submit(screen.getByRole("button", { name: "Xác thực email" }).closest("form")!);

    expect(verifyEmail).toHaveBeenCalledWith({ email: "test@test.com", otp: "123456" });
    await waitFor(() => {
      expect(screen.getByText(/Email đã được xác thực/i)).toBeInTheDocument();
    });
  });

  it("handles invalid otp", async () => {
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123" } });
    fireEvent.submit(screen.getByRole("button", { name: "Xác thực email" }).closest("form")!);

    expect(await screen.findByText("OTP phải gồm đúng 6 chữ số.")).toBeInTheDocument();
  });

  it("handles invalid email on submit", async () => {
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "invalid" } });
    fireEvent.submit(screen.getByRole("button", { name: "Xác thực email" }).closest("form")!);

    expect(await screen.findByText("Email không hợp lệ.")).toBeInTheDocument();
  });

  it("handles api error on submit", async () => {
    vi.mocked(verifyEmail).mockRejectedValue(new Error("API Error"));
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.submit(screen.getByRole("button", { name: "Xác thực email" }).closest("form")!);

    expect(await screen.findByText("API Error")).toBeInTheDocument();
  });

  it("handles resend success", async () => {
    vi.mocked(resendEmailVerification).mockResolvedValue(null as any);
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole("button", { name: "Gửi lại OTP" }));

    expect(resendEmailVerification).toHaveBeenCalledWith({ email: "test@test.com" });
    await waitFor(() => {
      expect(screen.getByText(/MangaRec đã gửi mã OTP mới/i)).toBeInTheDocument();
    });
  });

  it("handles resend error", async () => {
    vi.mocked(resendEmailVerification).mockRejectedValue(new Error("Resend Error"));
    render(
      <MemoryRouter>
        <VerifyEmail />
      </MemoryRouter>
    );

    fireEvent.click(screen.getByRole("button", { name: "Gửi lại OTP" }));

    expect(await screen.findByText("Resend Error")).toBeInTheDocument();
  });
});
