export interface ChatRequest {
  message: string;
}

export interface ChatRecommendation {
  mangaDexId: string;
  title: string;
  reason: string;
}

export interface ChatResponse {
  answer: string;
  recommendations: ChatRecommendation[];
  servedAt: string;
}