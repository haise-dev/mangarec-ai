import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import ForgotPassword from "./ForgotPassword";
import { MemoryRouter } from "react-router-dom";
import { forgotPassword } from "@/api/endpoints/authApi";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
  };
});
vi.mock("@/api/endpoints/authApi", () => ({ forgotPassword: vi.fn() }));

describe("ForgotPassword", () => {
  it("submits email and navigates on success", async () => {
    vi.mocked(forgotPassword).mockResolvedValue(null as any);
    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "test@test.com" } });
    fireEvent.submit(screen.getByRole("button", { name: "Gửi mã OTP" }).closest("form")!);

    expect(forgotPassword).toHaveBeenCalledWith({ email: "test@test.com" });
    await waitFor(() => {
      expect(screen.getByText(/Nếu email tồn tại/i)).toBeInTheDocument();
    });
  });

  it("handles invalid email", async () => {
    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "invalid" } });
    fireEvent.submit(screen.getByRole("button", { name: "Gửi mã OTP" }).closest("form")!);

    expect(await screen.findByText("Email không hợp lệ.")).toBeInTheDocument();
  });

  it("handles api error", async () => {
    vi.mocked(forgotPassword).mockRejectedValue(new Error("API Error"));
    render(
      <MemoryRouter>
        <ForgotPassword />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText("Email"), { target: { value: "test@test.com" } });
    fireEvent.submit(screen.getByRole("button", { name: "Gửi mã OTP" }).closest("form")!);

    expect(await screen.findByText("API Error")).toBeInTheDocument();
  });
});
