import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import ResetPassword from "./ResetPassword";
import { MemoryRouter } from "react-router-dom";
import { resetPassword } from "@/api/endpoints/authApi";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
    useSearchParams: () => [new URLSearchParams("?email=test@test.com")],
  };
});
vi.mock("@/api/endpoints/authApi", () => ({ resetPassword: vi.fn() }));

describe("ResetPassword", () => {
  it("submits and navigates on success", async () => {
    vi.mocked(resetPassword).mockResolvedValue(null as any);
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu mới"), { target: { value: "password123" } });
    
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(resetPassword).toHaveBeenCalledWith({ email: "test@test.com", otp: "123456", newPassword: "password123", confirmPassword: "password123" });
    await waitFor(() => {
      expect(screen.getByText(/Mật khẩu đã được cập nhật/i)).toBeInTheDocument();
    });
  });

  it("handles invalid email", async () => {
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "invalid" } });
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(await screen.findByText("Email không hợp lệ.")).toBeInTheDocument();
  });

  it("handles invalid otp", async () => {
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123" } });
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(await screen.findByText("OTP phải gồm đúng 6 chữ số.")).toBeInTheDocument();
  });

  it("handles short password", async () => {
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: "123" } });
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(await screen.findByText("Mật khẩu mới phải có ít nhất 8 ký tự.")).toBeInTheDocument();
  });

  it("handles password mismatch", async () => {
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu mới"), { target: { value: "password1234" } });
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(await screen.findByText("Mật khẩu xác nhận không khớp.")).toBeInTheDocument();
  });

  it("handles api error", async () => {
    vi.mocked(resetPassword).mockRejectedValue(new Error("API Error"));
    render(
      <MemoryRouter>
        <ResetPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("OTP"), { target: { value: "123456" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu mới"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu mới"), { target: { value: "password123" } });
    fireEvent.submit(screen.getByRole("button", { name: "Đặt lại mật khẩu" }).closest("form")!);

    expect(await screen.findByText("API Error")).toBeInTheDocument();
  });
});
