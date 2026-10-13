import React, { useCallback, useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  listSupportMessages,
  sendTicketMessage,
  type SupportMessage,
  type SupportTicket,
} from '@/api/supportApi';

const Frame = styled.section`
  border: 1px solid #e2e6eb;
  border-radius: 12px;
  overflow: hidden;
  background: #fff;
`;

const ConversationHeader = styled.header`
  padding: 12px 14px;
  background: #fff8f2;
  border-bottom: 1px solid #f0e0d4;
  strong { display: block; color: #8b4200; }
  span { color: #6b7280; font-size: 12px; }
`;

const MessageList = styled.div`
  max-height: 270px;
  min-height: 130px;
  overflow-y: auto;
  padding: 12px;
  background: #f7f8fa;
`;

const Bubble = styled.div<{ $own: boolean; $system: boolean }>`
  width: fit-content;
  max-width: 84%;
  margin: ${p => p.$own ? '8px 0 8px auto' : '8px auto 8px 0'};
  padding: 9px 11px;
  border-radius: 12px;
  background: ${p => p.$system ? '#fff4d6' : p.$own ? '#ff6b00' : '#fff'};
  color: ${p => p.$own && !p.$system ? '#fff' : '#28303d'};
  box-shadow: 0 1px 5px rgba(0, 0, 0, .08);
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  small { display: block; margin-bottom: 3px; opacity: .75; font-weight: 700; }
  time { display: block; margin-top: 4px; font-size: 10px; opacity: .68; text-align: right; }
`;

const ReplyForm = styled.form`
  display: flex;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #e9ecef;
`;

const ReplyInput = styled.input`
  flex: 1;
  min-width: 0;
  border: 1px solid #cfd5dc;
  border-radius: 8px;
  padding: 10px;
  font: inherit;
`;

const Send = styled.button`
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  background: #ff6b00;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  &:disabled { opacity: .55; cursor: default; }
`;

const Closed = styled.p`
  margin: 0;
  padding: 11px 14px;
  border-top: 1px solid #e9ecef;
  background: #edf9f0;
  color: #20683a;
  font-size: 13px;
  font-weight: 600;
`;

type Props = {
  ticket: SupportTicket;
  viewerRole: 'CUSTOMER' | 'RESTAURANT' | 'ADMIN';
  compact?: boolean;
  onMessageSent?: () => void;
};

const statusLabel = {
  OPEN: 'Đang chờ tiếp nhận',
  IN_PROGRESS: 'Đang trao đổi',
  RESOLVED: 'Đã hoàn tất',
};

const SupportConversation: React.FC<Props> = ({ ticket, viewerRole, onMessageSent }) => {
  const [messages, setMessages] = useState<SupportMessage[]>([]);
  const [reply, setReply] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);

  const load = useCallback(async (quiet = false) => {
    if (!quiet) setLoading(true);
    try {
      setMessages(await listSupportMessages(ticket.id));
    } catch {
      if (!quiet) toast.error('Không thể tải cuộc trao đổi');
    } finally {
      if (!quiet) setLoading(false);
    }
  }, [ticket.id]);

  useEffect(() => {
    void load();
    const timer = window.setInterval(() => void load(true), 5000);
    return () => window.clearInterval(timer);
  }, [load]);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: 'smooth' });
  }, [messages]);

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    const text = reply.trim();
    if (!text || sending || ticket.status === 'RESOLVED') return;
    setSending(true);
    try {
      const saved = await sendTicketMessage(ticket.id, text);
      setMessages(current => [...current, saved]);
      setReply('');
      onMessageSent?.();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể gửi tin nhắn');
    } finally {
      setSending(false);
    }
  };

  return (
    <Frame>
      <ConversationHeader>
        <strong>{ticket.id}{ticket.orderId ? ` · Đơn ${ticket.orderId}` : ''}</strong>
        <span>{statusLabel[ticket.status]} · {ticket.target === 'ADMIN' ? 'Bộ phận hỗ trợ FoodFast' : 'Nhà hàng'}</span>
      </ConversationHeader>
      <MessageList ref={listRef} aria-live="polite">
        {loading && <p>Đang tải trao đổi...</p>}
        {!loading && messages.length === 0 && <p>Chưa có tin nhắn.</p>}
        {messages.map(item => {
          const own = item.senderRole === viewerRole;
          return (
            <Bubble key={item.id} $own={own} $system={item.senderRole === 'SYSTEM'}>
              <small>{item.senderName}</small>
              {item.message}
              <time>{new Date(item.createdAt).toLocaleString('vi-VN')}</time>
            </Bubble>
          );
        })}
      </MessageList>
      {ticket.status === 'RESOLVED' ? (
        <Closed>Phiếu đã hoàn tất. Nội dung trao đổi được giữ lại để bạn tra cứu.</Closed>
      ) : (
        <ReplyForm onSubmit={submit}>
          <ReplyInput
            aria-label="Nhập tin nhắn hỗ trợ"
            placeholder="Nhập nội dung trao đổi..."
            maxLength={2000}
            value={reply}
            onChange={event => setReply(event.target.value)}
          />
          <Send disabled={sending || !reply.trim()}>{sending ? 'Đang gửi' : 'Gửi'}</Send>
        </ReplyForm>
      )}
    </Frame>
  );
};

export default SupportConversation;
