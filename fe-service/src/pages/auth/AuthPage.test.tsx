import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import AuthPage from "./AuthPage";
import { MemoryRouter } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: { message: "Welcome back", from: { pathname: "/custom" } } }),
  };
});

vi.mock("@/hooks/useAuth", () => ({ useAuth: vi.fn() }));

describe("AuthPage", () => {
  it("renders login form and handles success", async () => {
    const mockLogin = vi.fn().mockResolvedValue({});
    vi.mocked(useAuth).mockReturnValue({ login: mockLogin, register: vi.fn(), loginWithGoogle: vi.fn() } as any);

    render(
      <MemoryRouter>
        <AuthPage type="login" />
      </MemoryRouter>
    );

    expect(screen.getByText("Welcome back")).toBeInTheDocument();

    const emailInput = screen.getByLabelText("Email");
    // PasswordField inside has input mapped by id, which matches label "Mật khẩu"
    // Wait, PasswordField has: <label htmlFor={id}><span>{label}</span>...<input id={id}...
    const pwdInput = screen.getByLabelText("Mật khẩu");
    
    fireEvent.change(emailInput, { target: { value: "test@test.com" } });
    fireEvent.change(pwdInput, { target: { value: "password123" } });

    const submitBtn = screen.getByRole("button", { name: "Đăng nhập" });
    fireEvent.submit(submitBtn.closest("form")!);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({ email: "test@test.com", password: "password123" });
      expect(mockNavigate).toHaveBeenCalledWith("/custom", { replace: true });
    });
  });

  it("handles login validation error", async () => {
    vi.mocked(useAuth).mockReturnValue({ login: vi.fn(), register: vi.fn(), loginWithGoogle: vi.fn() } as any);

    render(
      <MemoryRouter>
        <AuthPage type="login" />
      </MemoryRouter>
    );

    const emailInput = screen.getByLabelText("Email");
    const pwdInput = screen.getByLabelText("Mật khẩu");
    
    fireEvent.change(emailInput, { target: { value: "invalid" } });
    fireEvent.change(pwdInput, { target: { value: "short" } });

    const submitBtn = screen.getByRole("button", { name: "Đăng nhập" });
    fireEvent.submit(submitBtn.closest("form")!);

    expect(await screen.findByText("Email không hợp lệ.")).toBeInTheDocument();
  });

  it("renders register form and handles success", async () => {
    const mockRegister = vi.fn().mockResolvedValue({ email: "new@test.com" });
    vi.mocked(useAuth).mockReturnValue({ login: vi.fn(), register: mockRegister, loginWithGoogle: vi.fn() } as any);

    render(
      <MemoryRouter>
        <AuthPage type="register" />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Tên hiển thị"), { target: { value: "New User" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "new@test.com" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "password123" } });

    const submitBtn = screen.getByRole("button", { name: "Tạo tài khoản" });
    fireEvent.submit(submitBtn.closest("form")!);

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith({ name: "New User", email: "new@test.com", password: "password123" });
      expect(mockNavigate).toHaveBeenCalledWith("/verify-email?email=new%40test.com", { replace: true });
    });
  });

  it("handles register validation: name empty", async () => {
    vi.mocked(useAuth).mockReturnValue({ login: vi.fn(), register: vi.fn(), loginWithGoogle: vi.fn() } as any);

    render(
      <MemoryRouter>
        <AuthPage type="register" />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "new@test.com" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "password123" } });

    const submitBtn = screen.getByRole("button", { name: "Tạo tài khoản" });
    fireEvent.submit(submitBtn.closest("form")!);

    expect(await screen.findByText("Vui lòng nhập tên hiển thị.")).toBeInTheDocument();
  });

  it("handles register validation: password mismatch", async () => {
    vi.mocked(useAuth).mockReturnValue({ login: vi.fn(), register: vi.fn(), loginWithGoogle: vi.fn() } as any);

    render(
      <MemoryRouter>
        <AuthPage type="register" />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Tên hiển thị"), { target: { value: "Name" } });
    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "new@test.com" } });
    fireEvent.change(screen.getByLabelText("Mật khẩu"), { target: { value: "password123" } });
    fireEvent.change(screen.getByLabelText("Nhập lại mật khẩu"), { target: { value: "password1234" } });

    fireEvent.click(screen.getByRole("button", { name: "Tạo tài khoản" }));

    expect(await screen.findByText("Mật khẩu xác nhận không khớp.")).toBeInTheDocument();
  });
});
