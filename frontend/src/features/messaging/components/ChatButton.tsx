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
      <span className="tcs-chat-btn__icon">{loading ? '⌛' : '💬'}</span>
      <span className="tcs-chat-btn__text">{error || (loading ? 'Đang kết nối...' : text)}</span>
    </button>
  );
}
