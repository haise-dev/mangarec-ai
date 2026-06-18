import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { ChatPanel } from "./ChatPanel";
import { useAuth } from "@/hooks/useAuth";
import { sendChatMessage } from "@/api/endpoints/chatApi";

vi.mock("@/hooks/useAuth", () => ({ useAuth: vi.fn() }));
vi.mock("@/api/endpoints/chatApi", () => ({ sendChatMessage: vi.fn() }));

describe("ChatPanel", () => {
  it("renders empty state", () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as any);
    render(<ChatPanel />);
    expect(screen.getByText("Hỏi gu đọc của bạn")).toBeInTheDocument();
    expect(screen.getByText("Bắt đầu bằng một câu rất đời thường.")).toBeInTheDocument();
  });

  it("handles submitting a message and getting response", async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as any);
    vi.mocked(sendChatMessage).mockResolvedValue({
      answer: "Đây là gợi ý của tôi",
      recommendations: [{ title: "Naruto", mangaDexId: "123", reason: "Ninja" }]
    } as any);

    render(<ChatPanel />);

    const input = screen.getByPlaceholderText("Bạn muốn đọc manga kiểu gì?");
    fireEvent.change(input, { target: { value: "Tôi thích ninja" } });
    
    const submitBtn = screen.getByRole("button", { name: "Gửi" });
    fireEvent.submit(submitBtn.closest("form")!);

    expect(screen.getByText("Tôi thích ninja")).toBeInTheDocument();
    expect(screen.getByText("MangaRec đang chọn truyện phù hợp...")).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Đây là gợi ý của tôi")).toBeInTheDocument();
      expect(screen.getByText("Naruto")).toBeInTheDocument();
      expect(screen.getByText("Ninja")).toBeInTheDocument();
    });
  });

  it("handles starter prompts", async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as any);
    vi.mocked(sendChatMessage).mockResolvedValue({ answer: "Gợi ý đây", recommendations: [] } as any);

    render(<ChatPanel />);

    const promptBtn = screen.getByText("Gợi ý manga hành động có yếu tố hài hước");
    fireEvent.click(promptBtn);

    await waitFor(() => {
      expect(screen.getAllByText("Gợi ý manga hành động có yếu tố hài hước").length).toBeGreaterThan(0);
      expect(screen.getByText("Gợi ý đây")).toBeInTheDocument();
    });
  });

  it("handles API error", async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as any);
    vi.mocked(sendChatMessage).mockRejectedValue(new Error("API Error"));

    render(<ChatPanel />);

    const input = screen.getByPlaceholderText("Bạn muốn đọc manga kiểu gì?");
    fireEvent.change(input, { target: { value: "Lỗi" } });
    const submitBtn = screen.getByRole("button", { name: "Gửi" });
    fireEvent.submit(submitBtn.closest("form")!);

    await waitFor(() => {
      expect(screen.getByText("API Error")).toBeInTheDocument();
    });
  });
});
