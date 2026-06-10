import { useState } from "react";
import { getApiErrorMessage } from "@/api/apiClient";
import { sendChatMessage } from "@/api/endpoints/chatApi";
import { useAuth } from "@/hooks/useAuth";
import type { ChatRecommendation } from "@/types";

type ChatEntry = {
  id: string;
  role: "user" | "assistant";
  content: string;
  recommendations?: ChatRecommendation[];
};

type ChatPanelProps = {
  compact?: boolean;
};

const starterPrompts = [
  "Gợi ý manga hành động có yếu tố hài hước",
  "Tôi muốn manga hoàn thành, không quá dài",
  "Có bộ nào giống Spy x Family nhưng nhiều phiêu lưu hơn không?",
];

export function ChatPanel({ compact = false }: ChatPanelProps) {
  const { isAuthenticated } = useAuth();
  const [message, setMessage] = useState("");
  const [entries, setEntries] = useState<ChatEntry[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const submitMessage = async (value: string) => {
    const trimmed = value.trim();
    if (!trimmed || isLoading) return;

    setError("");
    setMessage("");
    const userEntry: ChatEntry = {
      id: crypto.randomUUID(),
      role: "user",
      content: trimmed,
    };
    setEntries((current) => [...current, userEntry]);
    setIsLoading(true);

    try {
      const response = await sendChatMessage({ message: trimmed }, { authenticated: isAuthenticated });
      const assistantEntry: ChatEntry = {
        id: crypto.randomUUID(),
        role: "assistant",
        content: response.answer,
        recommendations: response.recommendations,
      };
      setEntries((current) => [...current, assistantEntry]);
    } catch (chatError) {
      setError(getApiErrorMessage(chatError, "MangaRec chưa thể trả lời lúc này. Vui lòng thử lại."));
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    void submitMessage(message);
  };

  return (
    <section className={compact ? "chat-panel chat-panel--compact" : "chat-panel"}>
      <div className="chat-panel__header">
        <span className="surface-label">AI Manga Guide</span>
        <h2>Hỏi gu đọc của bạn</h2>
        <p>Mô tả thể loại, mood, độ dài hoặc bộ truyện bạn thích. MangaRec sẽ gợi ý theo ngữ cảnh.</p>
      </div>

      <div className="chat-window" aria-live="polite">
        {entries.length === 0 ? (
          <div className="empty-chat">
            <div className="empty-chat__orb" />
            <strong>Bắt đầu bằng một câu rất đời thường.</strong>
            <span>Ví dụ: “Tôi muốn manga nhẹ nhàng, ít drama, đọc cuối tuần.”</span>
          </div>
        ) : (
          entries.map((entry) => (
            <article className={`chat-bubble chat-bubble--${entry.role}`} key={entry.id}>
              <p>{entry.content}</p>
              {entry.recommendations?.length ? (
                <div className="recommendation-grid">
                  {entry.recommendations.map((item) => (
                    <div className="recommendation-card" key={`${entry.id}-${item.mangaDexId}`}>
                      <span>{item.title}</span>
                      <p>{item.reason}</p>
                    </div>
                  ))}
                </div>
              ) : null}
            </article>
          ))
        )}
        {isLoading ? <div className="typing-pill">MangaRec đang chọn truyện phù hợp...</div> : null}
      </div>

      {error ? <div className="chat-error">{error}</div> : null}

      <div className="prompt-row">
        {starterPrompts.map((prompt) => (
          <button type="button" key={prompt} onClick={() => void submitMessage(prompt)} disabled={isLoading}>
            {prompt}
          </button>
        ))}
      </div>

      <form className="chat-input" onSubmit={handleSubmit}>
        <input
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          placeholder="Bạn muốn đọc manga kiểu gì?"
          maxLength={2000}
          aria-label="Nội dung hỏi MangaRec"
        />
        <button type="submit" disabled={isLoading || !message.trim()}>
          Gửi
        </button>
      </form>
    </section>
  );
}
