import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { storage } from "@/utils/storage";
import type { ApiErrorResponse } from "@/types";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

export const publicApiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30_000,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = storage.getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
    return config;
  }

  const guestId = storage.getGuestId();
  if (guestId) {
    config.headers["X-Guest-Id"] = guestId;
  }

  return config;
});

export function getApiErrorMessage(error: unknown, fallback = "Yêu cầu chưa thực hiện được"): string {
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    const retryAfter = axiosError.response?.headers?.["retry-after"];
    if (axiosError.response?.status === 429) {
      return retryAfter
        ? `Bạn đang gửi quá nhanh. Vui lòng thử lại sau ${retryAfter} giây.`
        : "Bạn đã dùng hết lượt hôm nay. Vui lòng thử lại sau.";
    }

    return axiosError.response?.data?.message || axiosError.response?.data?.error || fallback;
  }

  if (error instanceof Error) return error.message;
  return fallback;
}

export default apiClient;
