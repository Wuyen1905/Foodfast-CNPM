import React, { useCallback, useEffect, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  listSupportTickets,
  resolveSupportTicket,
  SupportTicket,
} from '@/api/supportApi';
import SupportConversation from '@/components/SupportConversation';

type Props = {
  target: 'RESTAURANT' | 'ADMIN';
  restaurantId?: string;
};

const Container = styled.section`
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  h2 { margin: 0; color: #2f3542; }
`;

const RefreshButton = styled.button`
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fff;
  cursor: pointer;
`;

const TicketCard = styled.article`
  border: 1px solid #e6e8ec;
  border-left: 4px solid #ff7a00;
  border-radius: 10px;
  padding: 14px 16px;
  margin-top: 12px;
`;

const Meta = styled.div`
  color: #657080;
  font-size: 13px;
  margin: 6px 0;
`;

const ResolveButton = styled.button`
  border: 0;
  border-radius: 8px;
  padding: 8px 12px;
  background: #198754;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
`;

const ActionRow = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
`;

const ConversationButton = styled.button`
  border: 1px solid #ff7a00;
  border-radius: 8px;
  padding: 8px 12px;
  background: #fff4ea;
  color: #9a4a00;
  font-weight: 700;
  cursor: pointer;
`;

const SupportTicketInbox: React.FC<Props> = ({ target, restaurantId }) => {
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedTicketId, setExpandedTicketId] = useState<string | null>(null);

  const loadTickets = useCallback(async () => {
    if (target === 'RESTAURANT' && !restaurantId) return;
    setLoading(true);
    try {
      const result = await listSupportTickets(target, restaurantId);
      setTickets(result);
    } catch {
      toast.error('Không thể tải yêu cầu hỗ trợ');
    } finally {
      setLoading(false);
    }
  }, [restaurantId, target]);

  useEffect(() => {
    loadTickets();
  }, [loadTickets]);

  const handleResolve = async (ticketId: string) => {
    try {
      await resolveSupportTicket(ticketId);
      setTickets(previous => previous.filter(ticket => ticket.id !== ticketId));
      toast.success('Đã đánh dấu yêu cầu hỗ trợ là hoàn tất');
    } catch {
      toast.error('Không thể cập nhật yêu cầu hỗ trợ');
    }
  };

  return (
    <Container>
      <Header>
        <div>
          <h2>
            🎫 {target === 'RESTAURANT'
              ? 'Khách cần nhà hàng hỗ trợ'
              : 'Khách cần quản trị viên hỗ trợ'}
          </h2>
          <Meta>
            Đây là các câu hỏi hoặc sự cố mà trợ lý AI không tự giải quyết và đã chuyển cho
            {target === 'RESTAURANT' ? ' nhà hàng' : ' quản trị viên'}. Khiếu nại và hoàn tiền được xử lý ở mục riêng bên dưới.
          </Meta>
        </div>
        <RefreshButton onClick={loadTickets}>Làm mới</RefreshButton>
      </Header>
      {loading && <p>Đang tải...</p>}
      {!loading && tickets.length === 0 && <p>Không có yêu cầu hỗ trợ nào đang chờ xử lý.</p>}
      {tickets.map(ticket => (
        <TicketCard key={ticket.id}>
          <strong>{ticket.id}</strong>
          <Meta>
            {ticket.orderId ? `Đơn ${ticket.orderId} · ` : ''}
            {ticket.customerName ? `${ticket.customerName} · ` : ''}
            {new Date(ticket.createdAt).toLocaleString('vi-VN')}
          </Meta>
          <p>{ticket.message}</p>
          <ActionRow>
            <ConversationButton onClick={() => setExpandedTicketId(current => current === ticket.id ? null : ticket.id)}>
              {expandedTicketId === ticket.id ? 'Ẩn trao đổi' : 'Trao đổi với khách'}
            </ConversationButton>
            <ResolveButton onClick={() => handleResolve(ticket.id)}>
              Hoàn tất phiếu
            </ResolveButton>
          </ActionRow>
          {expandedTicketId === ticket.id && (
            <div style={{ marginTop: 12 }}>
              <SupportConversation ticket={ticket} viewerRole="RESTAURANT" onMessageSent={loadTickets} />
            </div>
          )}
        </TicketCard>
      ))}
    </Container>
  );
};

export default SupportTicketInbox;
