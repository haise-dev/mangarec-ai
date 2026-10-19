export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
}

export interface ApiErrorResponse {
  timestamp?: string;
  status: number;
  path?: string;
  error?: string;
  message: string;
}
