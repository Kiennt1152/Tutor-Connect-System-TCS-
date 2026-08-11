import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { HomeNavbar } from '../../../shared/components/HomeNavbar';
import { useAuth } from '../../../shared/auth/AuthProvider';
import { MessagingPanel } from '../components/MessagingPanel';
import { ConversationList } from '../components/ConversationList';
import { MessageThread } from '../components/MessageThread';
import { ChatInput } from '../components/ChatInput';
import { UserSearchModal } from '../components/UserSearchModal';
import { GroupInfoPanel } from '../components/GroupInfoPanel';
import { ReportUserDialog } from '../../reviews/components/ReportUserDialog';
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

  const [showSearch, setShowSearch] = useState(false);
  const [showGroupInfo, setShowGroupInfo] = useState(false);
  const [showReportUser, setShowReportUser] = useState(false);

  const {
    conversations,
    loading: convLoading,
    error: convError,
    reload: reloadConversations,
    upsertConversation,
    markConversationRead,
  } = useConversations();

  const requestedConversationId = convIdParam ? Number(convIdParam) : null;
  const selectedConvId = requestedConversationId !== null && Number.isFinite(requestedConversationId)
    ? requestedConversationId
    : conversations[0]?.conversationId ?? null;
  const displayedTab = convIdParam ? 'chat' : tabParam === 'tickets' ? 'tickets' : activeTab;

  const {
    messages,
    loading: messagesLoading,
    hasMore,
    loadMore,
    sendMessage,
  } = useMessages(selectedConvId);

  const handleSelectConv = (conv: ConversationResponse) => {
    setActiveTab('chat');
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
      setActiveTab('chat');
      setSearchParams({ conv: String(conv.conversationId) });
    } catch (err) {
      console.error('Không thể tạo cuộc trò chuyện:', err);
    }
  };

  const handleCreateGroup = async (name: string, members: UserSummaryResponse[]) => {
    const conversation = await messagingApi.createGroup({
      name,
      memberIds: members.map((member) => member.userId),
    });
    upsertConversation(conversation);
    setActiveTab('chat');
    setSearchParams({ conv: String(conversation.conversationId) });
    setShowSearch(false);
  };

  const handleLeftGroup = (conversationId: number) => {
    setShowGroupInfo(false);
    if (selectedConvId === conversationId) {
      setSearchParams({ tab: 'chat' });
    }
    void reloadConversations();
  };

  const activeConv = conversations.find((c) => c.conversationId === selectedConvId) || null;

  return (
    <div className="tcs-page">
      <HomeNavbar />
      <div className="msg-page">
        <div className="msg-page__nav-tabs">
          <button
            type="button"
            className={`msg-page__nav-tab${displayedTab === 'chat' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => {
              setActiveTab('chat');
              setSearchParams({ tab: 'chat' });
            }}
          >
            💬 Tin nhắn trực tiếp
          </button>
          <button
            type="button"
            className={`msg-page__nav-tab${displayedTab === 'tickets' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => {
              setActiveTab('tickets');
              setSearchParams({ tab: 'tickets' });
            }}
          >
            🎫 Yêu cầu hỗ trợ (Tickets)
          </button>
        </div>

        <div className="msg-page__body">
          {displayedTab === 'tickets' ? (
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
                      <div>
                        <span className="msg-thread-header__name">
                          {activeConv.type === 'GROUP'
                            ? activeConv.name
                            : activeConv.otherParticipant?.displayName ?? 'Cuộc trò chuyện'}
                        </span>
                        {activeConv.type === 'GROUP' && (
                          <span className="msg-thread-header__meta">
                            {activeConv.participantCount} thành viên
                          </span>
                        )}
                      </div>
                      {activeConv.type === 'GROUP' && (
                        <button
                          type="button"
                          className="msg-thread-header__info"
                          title="Thông tin nhóm"
                          onClick={() => setShowGroupInfo(true)}
                        >
                          i
                        </button>
                      )}
                      {activeConv.type !== 'GROUP' && activeConv.otherParticipant && (
                        <button type="button" className="msg-thread-header__info" title="Báo cáo người dùng" onClick={() => setShowReportUser(true)}>!</button>
                      )}
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
          open={true}
          onClose={() => setShowSearch(false)}
          onSelectUser={handleSelectUser}
          onCreateGroup={handleCreateGroup}
        />
      )}
      {showReportUser && activeConv?.otherParticipant && (
        <ReportUserDialog userId={activeConv.otherParticipant.userId} displayName={activeConv.otherParticipant.displayName} onClose={() => setShowReportUser(false)} />
      )}

      {showGroupInfo && activeConv?.type === 'GROUP' && (
        <GroupInfoPanel
          conversation={activeConv}
          currentUserId={user?.userId}
          onClose={() => setShowGroupInfo(false)}
          onUpdated={upsertConversation}
          onLeft={handleLeftGroup}
        />
      )}
    </div>
  );
}
