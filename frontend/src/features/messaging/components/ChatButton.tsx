import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { messagingApi } from '../api/messagingApi';
import type { ContextType } from '../types/messagingTypes';
import './ChatButton.css';

type ChatButtonProps = {
  contextType: ContextType;
  contextId: string | number;
  label?: string;
  recipientName?: string;
  className?: string;
  size?: 'sm' | 'md';
};

export function ChatButton({
  contextType,
  contextId,
  label,
  recipientName,
  className = '',
  size = 'md',
}: ChatButtonProps) {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleClick = async (e: React.MouseEvent) => {
    e.stopPropagation();
    if (loading) return;

    setLoading(true);
    setError(null);
    try {
      const conv = await messagingApi.getOrCreateContextConversation(contextType, contextId);
      navigate(`/messaging?conv=${conv.conversationId}`);
    } catch (err: unknown) {
      console.error('Không thể mở cuộc trò chuyện:', err);
      setError('Không thể mở chat');
      setTimeout(() => setError(null), 3000);
    } finally {
      setLoading(false);
    }
  };

  const text = label || (recipientName ? `Nhắn tin với ${recipientName}` : 'Nhắn tin');

  return (
    <button
      type="button"
      className={`tcs-chat-btn ${size === 'sm' ? 'tcs-chat-btn--sm' : ''} ${className}`}
      onClick={handleClick}
      disabled={loading}
      title={error || text}
    >
      <span className="tcs-chat-btn__icon">
        {loading ? (
          <svg
            className="tcs-chat-spinner"
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M21 12a9 9 0 1 1-6.219-8.56" />
          </svg>
        ) : (
          <svg
            width="14"
            height="14"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        )}
      </span>
      <span className="tcs-chat-btn__text">{error || (loading ? 'Đang kết nối...' : text)}</span>
    </button>
  );
}
