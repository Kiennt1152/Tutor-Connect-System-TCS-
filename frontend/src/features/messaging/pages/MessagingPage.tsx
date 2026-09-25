/**
 * ====================================================================================================
 * [UC-50 / UC-51] MÀN HÌNH HỘI THOẠI TRỰC TUYẾN (MESSAGING PAGE)
 * ====================================================================================================
 * Nghiệp vụ chính:
 * 1. Trò chuyện trực tuyến thời gian thực giữa Phụ huynh, Gia sư và Trung tâm.
 * 2. Hỗ trợ gửi hình ảnh, bài tập đính kèm và kiểm tra trạng thái đã đọc.
 * 3. Tích hợp bộ lọc cảnh báo giao dịch ngoài sàn bảo vệ quyền lợi người dùng.
 * * @author Hoàng Minh Đức (mduc1011-swp)
 * @author Nguyễn Trung Kiên (Kiennt1152)
 */

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

  /**
   * [UC-50, UC-51] Chọn một cuộc hội thoại từ danh sách, chuyển tab sang chat và tự động đánh dấu đã đọc.
   */
  const handleSelectConv = (conv: ConversationResponse) => {
    setActiveTab('chat');
    setSearchParams({ conv: String(conv.conversationId) });
    if (conv.unreadCount > 0) {
      messagingApi.markAsRead(conv.conversationId).then(() => {
        markConversationRead(conv.conversationId);
      }).catch(() => {});
    }
  };

  /**
   * [UC-50] Bắt đầu hoặc truy xuất cuộc hội thoại 1-1 với người dùng được chọn từ modal tìm kiếm.
   */
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

  /**
   * [UC-50] Khởi tạo nhóm chat mới với danh sách thành viên được chỉ định.
   */
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

  /**
   * [UC-50] Xử lý khi người dùng rời khỏi nhóm chat, đóng bảng thông tin nhóm và tải lại danh sách hội thoại.
   */
  const handleLeftGroup = (conversationId: number) => {
    setShowGroupInfo(false);
    if (selectedConvId === conversationId) {
      setSearchParams({ tab: 'chat' });
    }
    void reloadConversations();
  };

  /**
   * [UC-50] Xóa cuộc hội thoại khỏi danh sách hiển thị và chuyển sang hội thoại kế tiếp hoặc tab mặc định.
   */
  const handleDeleteConversation = (conversationId: number) => {
    if (selectedConvId === conversationId) {
      const remaining = conversations.filter((c) => c.conversationId !== conversationId);
      if (remaining.length > 0) {
        setSearchParams({ conv: String(remaining[0].conversationId) });
      } else {
        setSearchParams({ tab: 'chat' });
      }
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
            className={`msg-page__nav-tab${displayedTab === 'chat' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => {
              setActiveTab('chat');
              setSearchParams({ tab: 'chat' });
            }}
          >
            Tin nhắn trực tiếp
          </button>
          <button
            type="button"
            className={`msg-page__nav-tab${displayedTab === 'tickets' ? ' msg-page__nav-tab--active' : ''}`}
            onClick={() => {
              setActiveTab('tickets');
              setSearchParams({ tab: 'tickets' });
            }}
          >
            Yêu cầu hỗ trợ (Tickets)
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
                onDeleteConversation={handleDeleteConversation}
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
                    <div className="msg-thread-panel__empty-icon">
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
                      </svg>
                    </div>
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
