import { describe, it, expect, vi } from "vitest";
import { sendChatMessage } from "./chatApi";
import apiClient, { publicApiClient } from "@/api/apiClient";
import { storage } from "@/utils/storage";
import { getOrCreateGuestId } from "@/api/endpoints/guestApi";

vi.mock("@/api/apiClient", () => ({
  default: { post: vi.fn() },
  publicApiClient: { post: vi.fn() },
}));

vi.mock("@/utils/storage", () => ({
  storage: { getAccessToken: vi.fn() }
}));

vi.mock("@/api/endpoints/guestApi", () => ({
  getOrCreateGuestId: vi.fn()
}));

describe("chatApi", () => {
  it("should use authenticated client if token exists", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue("token");
    vi.mocked(apiClient.post).mockResolvedValue({ data: { data: "resp" } });
    
    const result = await sendChatMessage({ message: "hi" });
    expect(apiClient.post).toHaveBeenCalled();
    expect(result).toBe("resp");
  });

  it("should fallback to public client with guest id if unauthenticated", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(getOrCreateGuestId).mockResolvedValue("guest-1");
    vi.mocked(publicApiClient.post).mockResolvedValue({ data: { data: "guest-resp" } });
    
    const result = await sendChatMessage({ message: "hi" });
    expect(publicApiClient.post).toHaveBeenCalled();
    expect(result).toBe("guest-resp");
  });

  it("should throw if cannot get guest id", async () => {
    vi.mocked(storage.getAccessToken).mockReturnValue(null);
    vi.mocked(getOrCreateGuestId).mockResolvedValue(undefined);
    
    await expect(sendChatMessage({ message: "hi" })).rejects.toThrow("Không thể tạo phiên khách. Vui lòng thử lại.");
  });
});
