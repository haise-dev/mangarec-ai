import apiClient, { publicApiClient } from "@/api/apiClient";
import { getOrCreateGuestId } from "@/api/endpoints/guestApi";
import type { ApiResponse, ChatRequest, ChatResponse } from "@/types";
import { storage } from "@/utils/storage";

type SendChatOptions = {
  authenticated?: boolean;
};

export async function sendChatMessage(
  payload: ChatRequest,
  options: SendChatOptions = {},
): Promise<ChatResponse> {
  if (options.authenticated ?? Boolean(storage.getAccessToken())) {
    const response = await apiClient.post<ApiResponse<ChatResponse>>("/api/chat", payload);
    return response.data.data;
  }

  const guestId = await getOrCreateGuestId();
  if (!guestId) {
    throw new Error("Không thể tạo phiên khách. Vui lòng thử lại.");
  }

  const response = await publicApiClient.post<ApiResponse<ChatResponse>>("/api/chat", payload, {
    headers: {
      "X-Guest-Id": guestId,
    },
  });
  return response.data.data;
}
