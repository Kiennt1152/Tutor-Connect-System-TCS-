import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { MessagingPanel } from '../components/MessagingPanel';
import { ConversationList } from '../components/ConversationList';
import { MessageThread } from '../components/MessageThread';
import { ChatInput } from '../components/ChatInput';
import { UserSearchModal } from '../components/UserSearchModal';
import { useConversations } from '../hooks/useConversations';
import { useMessages } from '../hooks/useMessages';
import { messagingApi } from '../api/messagingApi';
import type { ConversationResponse, UserSummaryResponse } from '../types/messagingTypes';
import '../components/MessagingPanel.css';
import './MessagingPage.css';

type MessagingPageProps = {
  initialTab?: 'chat' | 'tickets';
};

export default function MessagingPage({ initialTab }: MessagingPageProps) {
  const { user } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  const convIdParam = searchParams.get('conv');
  const tabParam = searchParams.get('tab');

  const [activeTab, setActiveTab] = useState<'chat' | 'tickets'>(() => {
    if (convIdParam) return 'chat';
    if (tabParam === 'tickets' || initialTab === 'tickets') return 'tickets';
    return 'chat';
  });

  const [selectedConvId, setSelectedConvId] = useState<number | null>(() => {
    return convIdParam ? Number(convIdParam) : null;
  });

  const [showSearch, setShowSearch] = useState(false);

  const {
    conversations,
    loading: convLoading,
    error: convError,
    upsertConversation,
    markConversationRead,
  } = useConversations();

  const {
    messages,
    loading: messagesLoading,
    hasMore,
    loadMore,
    sendMessage,
  } = useMessages(selectedConvId);

  useEffect(() => {
    if (convIdParam) {
      const id = Number(convIdParam);
      if (!isNaN(id)) {
        setSelectedConvId(id);
        setActiveTab('chat');
      }
    }
  }, [convIdParam]);

  useEffect(() => {
    if (!selectedConvId && conversations.length > 0 && !convIdParam) {
      setSelectedConvId(conversations[0].conversationId);
    }
  }, [conversations, selectedConvId, convIdParam]);

  const handleSelectConv = (conv: ConversationResponse) => {
    setSelectedConvId(conv.conversationId);
    setSearchParams({ conv: String(conv.conversationId) });
    if (conv.unreadCount > 0) {
      messagingApi.markAsRead(conv.conversationId).then(() => {
        markConversationRead(conv.conversationId);
      }).catch(() => {});
    }
  };

  const handleSelectUser = async (targetUser: UserSummaryResponse) => {
    setShowSearch(false);
    try {
      const conv = await messagingApi.startOrGetConversation(targetUser.userId);
      upsertConversation(conv);
      setSelectedConvId(conv.conversationId);
      setSearchParams({ conv: String(conv.conversationId) });
    } catch (err) {
      console.error('Không thể tạo cuộc trò chuyện:', err);
    }
  };

  const activeConv = conversations.find((c) => c.conversationId === selectedConvId) || null;

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="msg-page">
        <div className="msg-page__nav-tabs">
          <button
            type="button"
            className={`msg-page__nav-tab${activeTab === 'chat' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => setActiveTab('chat')}
          >
            💬 Tin nhắn trực tiếp
          </button>
          <button
            type="button"
            className={`msg-page__nav-tab${activeTab === 'tickets' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => setActiveTab('tickets')}
          >
            🎫 Yêu cầu hỗ trợ (Tickets)
          </button>
        </div>

        <div className="msg-page__body">
          {activeTab === 'tickets' ? (
            <MessagingPanel />
          ) : (
            <div className="msg-layout">
              <ConversationList
                conversations={conversations}
                activeConversationId={selectedConvId}
                loading={convLoading}
                error={convError}
                onSelect={handleSelectConv}
                onNewConversation={() => setShowSearch(true)}
              />

              <div className="msg-thread-panel">
                {activeConv ? (
                  <>
                    <div className="msg-thread-header">
                      <span className="msg-thread-header__name">
                        {activeConv.otherParticipant?.displayName ?? 'Cuộc trò chuyện'}
                      </span>
                    </div>
                    <MessageThread
                      messages={messages}
                      currentUserId={user?.userId}
                      loading={messagesLoading}
                      hasMore={hasMore}
                      onLoadMore={loadMore}
                    />
                    <ChatInput disabled={messagesLoading} onSend={sendMessage} />
                  </>
                ) : (
                  <div className="msg-thread-panel__empty">
                    <span style={{ fontSize: '2.5rem' }}>💬</span>
                    <p>Chọn một cuộc trò chuyện từ danh sách bên trái để bắt đầu nhắn tin.</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {showSearch && (
        <UserSearchModal
          onClose={() => setShowSearch(false)}
          onSelectUser={handleSelectUser}
        />
      )}
    </div>
  );
}
