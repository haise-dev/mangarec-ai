import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { HomePage } from "./HomePage";
import { MemoryRouter } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";

vi.mock("@/hooks/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("@/components/chat/ChatPanel", () => ({ ChatPanel: () => <div data-testid="chat-panel" /> }));

describe("HomePage", () => {
  it("renders guest view when not authenticated", () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false, user: null } as any);
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );
    expect(screen.getByText("Đăng nhập")).toBeInTheDocument();
    expect(screen.getByText("Bắt đầu với tài khoản")).toBeInTheDocument();
    expect(screen.getByTestId("chat-panel")).toBeInTheDocument();
  });

  it("renders user view when authenticated", () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true, user: { name: "Haise" } } as any);
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    );
    expect(screen.getByText("Không gian đọc của Haise")).toBeInTheDocument();
    expect(screen.getByText("Tiếp tục tư vấn")).toBeInTheDocument();
  });
});
