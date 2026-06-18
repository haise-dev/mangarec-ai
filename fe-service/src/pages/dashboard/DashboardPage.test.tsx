import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { DashboardPage } from "./DashboardPage";
import { MemoryRouter } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...(actual as any),
    useNavigate: () => mockNavigate,
  };
});

vi.mock("@/hooks/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("@/components/chat/ChatPanel", () => ({ ChatPanel: () => <div data-testid="chat-panel" /> }));

describe("DashboardPage", () => {
  it("renders user info and handles logout", () => {
    const mockLogout = vi.fn();
    vi.mocked(useAuth).mockReturnValue({
      logout: mockLogout,
      user: { name: "Haise", email: "haise@test.com", subscriptionStatus: "PRO", emailVerified: true }
    } as any);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(screen.getByText("Xin chào, Haise")).toBeInTheDocument();
    expect(screen.getByText("haise@test.com")).toBeInTheDocument();
    expect(screen.getByText("Pro")).toBeInTheDocument();
    expect(screen.getByText("Đã xác thực")).toBeInTheDocument();

    const logoutBtn = screen.getByText("Đăng xuất");
    fireEvent.click(logoutBtn);

    expect(mockLogout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith("/login", { replace: true });
  });

  it("renders default labels for empty user info", () => {
    vi.mocked(useAuth).mockReturnValue({
      logout: vi.fn(),
      user: null
    } as any);

    render(
      <MemoryRouter>
        <DashboardPage />
      </MemoryRouter>
    );

    expect(screen.getByText("Free")).toBeInTheDocument();
    expect(screen.getByText("Chưa xác thực")).toBeInTheDocument();
  });
});
