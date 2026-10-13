import React, { useEffect, useMemo, useState } from 'react';
import { useLocation } from 'react-router-dom';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  createSupportContact,
  listMySupportTickets,
  sendSupportMessage,
  type SupportIssueType,
  type SupportTicket,
} from '@/api/supportApi';
import SupportConversation from '@/components/SupportConversation';
import { useAuth } from '@/context/AuthContext';
import { useOrders } from '@/context/OrderContext';
import { getStatusLabel } from '@/utils/orderStatus';

type ChatMessage = {
  role: 'assistant' | 'user';
  text: string;
  handoffTarget?: 'RESTAURANT' | 'ADMIN';
  supportTicketId?: string;
  suggestions?: string[];
  requiresOrder?: boolean;
};

type View = 'assistant' | 'contact' | 'tickets' | 'conversation';

const Toggle = styled.button`
  position: fixed; right: 22px; bottom: 22px; z-index: 1100;
  width: 58px; height: 58px; border: 0; border-radius: 50%;
  background: var(--primary, #ff6600); color: white; font-size: 25px;
  cursor: pointer; box-shadow: 0 8px 24px rgba(0,0,0,.22);
`;

const Panel = styled.section`
  position: fixed; right: 22px; bottom: 92px; z-index: 1100;
  width: min(410px, calc(100vw - 24px)); height: min(650px, calc(100vh - 120px));
  display: flex; flex-direction: column; background: white; border-radius: 16px;
  box-shadow: 0 14px 42px rgba(0,0,0,.24); overflow: hidden;
`;

const Header = styled.header`
  padding: 15px 16px; background: var(--primary, #ff6600); color: white;
  h3 { margin: 0 0 3px; font-size: 18px; }
  p { margin: 0; font-size: 12px; opacity: .92; }
`;

const Navigation = styled.nav`
  display: grid; grid-template-columns: 1fr 1fr; gap: 7px;
  padding: 9px 12px; border-bottom: 1px solid #eee;
`;

const NavButton = styled.button<{ $active?: boolean }>`
  border: 1px solid ${p => p.$active ? '#ff6b00' : '#e0e3e7'};
  border-radius: 8px; padding: 8px; background: ${p => p.$active ? '#fff2e8' : '#fff'};
  color: ${p => p.$active ? '#a34d00' : '#4b5563'}; font-weight: 700; cursor: pointer;
`;

const Content = styled.div`
  flex: 1; min-height: 0; overflow-y: auto; padding: 12px; background: #f7f8fa;
`;

const OrderSelect = styled.select`
  width: 100%; padding: 10px; border: 1px solid #d6dae0; border-radius: 8px;
  background: white; font: inherit;
`;

const QuickActions = styled.div`
  display: flex; gap: 6px; margin: 10px 0; overflow-x: auto;
`;

const QuickAction = styled.button`
  flex: 0 0 auto; padding: 7px 10px; border: 1px solid #ffd0ad;
  border-radius: 999px; background: #fff7f0; color: #9a4a00;
  font-size: 12px; font-weight: 600; cursor: pointer;
  &:disabled { opacity: .55; cursor: default; }
`;

const Bubble = styled.div<{ $role: 'assistant' | 'user' }>`
  max-width: 86%; margin: ${p => p.$role === 'user' ? '8px 0 8px auto' : '8px auto 8px 0'};
  padding: 10px 12px; border-radius: 12px;
  background: ${p => p.$role === 'user' ? 'var(--primary, #ff6600)' : 'white'};
  color: ${p => p.$role === 'user' ? 'white' : '#333'};
  box-shadow: 0 2px 8px rgba(0,0,0,.06); white-space: pre-wrap;
  line-height: 1.45; font-size: 14px;
`;

const TicketLink = styled.button`
  display: block; margin-top: 8px; border: 1px solid #ffb27a; border-radius: 7px;
  padding: 6px 9px; background: #fff; color: #9a4a00; font-weight: 700; cursor: pointer;
`;

const Form = styled.form`
  display: flex; gap: 8px; padding: 10px 12px; border-top: 1px solid #eee;
`;

const Input = styled.input`
  flex: 1; min-width: 0; padding: 10px 11px; border: 1px solid #ddd; border-radius: 9px;
`;

const PrimaryButton = styled.button`
  border: 0; border-radius: 9px; padding: 10px 14px; background: var(--primary, #ff6600);
  color: white; font-weight: 700; cursor: pointer;
  &:disabled { opacity: .55; cursor: default; }
`;

const ContactCallout = styled.button`
  width: 100%; margin-top: 12px; border: 1px solid #f59f5b; border-radius: 10px;
  padding: 11px; background: #fff3e9; color: #8c4300; font-weight: 800; cursor: pointer;
`;

const ContactForm = styled.form`
  display: grid; gap: 11px;
  label { display: grid; gap: 5px; color: #374151; font-size: 13px; font-weight: 700; }
  select, textarea { border: 1px solid #cfd5dc; border-radius: 8px; padding: 10px; font: inherit; background: #fff; }
  textarea { min-height: 100px; resize: vertical; }
  p { margin: 0; color: #6b7280; font-size: 12px; }
`;

const TicketCard = styled.button`
  width: 100%; text-align: left; border: 1px solid #e0e4e9; border-left: 4px solid #ff6b00;
  border-radius: 10px; padding: 11px; margin-bottom: 9px; background: #fff; cursor: pointer;
  strong, span { display: block; }
  span { margin-top: 4px; color: #68707e; font-size: 12px; }
`;

const Back = styled.button`
  border: 0; background: transparent; color: #a34d00; font-weight: 700;
  padding: 0 0 10px; cursor: pointer;
`;

const issueLabels: Record<SupportIssueType, string> = {
  PAYMENT: 'Thanh toán hoặc hoàn tiền',
  ACCOUNT: 'Tài khoản và đăng nhập',
  ORDER_CHANGE: 'Thay đổi hoặc hủy đơn',
  FOOD_ISSUE: 'Món ăn thiếu, sai hoặc có vấn đề',
  DELIVERY: 'Giao hàng',
  OTHER: 'Vấn đề khác',
};

const ticketStatus = {
  OPEN: 'Đang chờ tiếp nhận',
  IN_PROGRESS: 'Đang trao đổi',
  RESOLVED: 'Đã hoàn tất',
};

const SupportChatWidget: React.FC = () => {
  const { user } = useAuth();
  const { orders } = useOrders();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [view, setView] = useState<View>('assistant');
  const [input, setInput] = useState('');
  const [selectedOrderId, setSelectedOrderId] = useState('');
  const [sending, setSending] = useState(false);
  const [tickets, setTickets] = useState<SupportTicket[]>([]);
  const [activeTicket, setActiveTicket] = useState<SupportTicket | null>(null);
  const [issueType, setIssueType] = useState<SupportIssueType>('OTHER');
  const [contactMessage, setContactMessage] = useState('');
  const [messages, setMessages] = useState<ChatMessage[]>([
    { role: 'assistant', text: 'Chào bạn! Mình có thể kiểm tra đơn, hướng dẫn VNPay, mã giảm giá, đặt lại đơn và chuyển đúng vấn đề tới nhân viên hỗ trợ.' },
  ]);

  const customerOrders = useMemo(
    () => orders.filter(order =>
      (user?.id && order.userId === user.id) || (user?.phone && order.phone === user.phone)),
    [orders, user?.id, user?.phone],
  );
  const issueNeedsOrder = ['ORDER_CHANGE', 'FOOD_ISSUE', 'DELIVERY'].includes(issueType);

  useEffect(() => {
    const routeOrderId = location.pathname.match(/^\/orders\/([^/]+)$/)?.[1];
    if (routeOrderId && customerOrders.some(order => order.id === routeOrderId)) {
      setSelectedOrderId(routeOrderId);
    }
  }, [location.pathname, customerOrders]);

  useEffect(() => {
    const openFromOrder = (event: Event) => {
      const detail = (event as CustomEvent<{ issueType?: SupportIssueType; orderId?: string }>).detail;
      const requestedOrderId = detail?.orderId || '';
      setOpen(true);
      setView('contact');
      setIssueType(detail?.issueType || 'ORDER_CHANGE');
      if (requestedOrderId) {
        setSelectedOrderId(requestedOrderId);
        setContactMessage(`Tôi cần hỗ trợ thay đổi hoặc hủy đơn ${requestedOrderId}.`);
      }
    };
    window.addEventListener('foodfast:open-support', openFromOrder);
    return () => window.removeEventListener('foodfast:open-support', openFromOrder);
  }, []);

  const loadTickets = async () => {
    if (!user) return [];
    try {
      const result = await listMySupportTickets();
      setTickets(result);
      setActiveTicket(current => current
        ? result.find(item => item.id === current.id) || current
        : current);
      return result;
    } catch {
      toast.error('Không thể tải các yêu cầu hỗ trợ');
      return [];
    }
  };

  useEffect(() => {
    if (!open || view !== 'conversation' || !user) return;
    const timer = window.setInterval(() => void loadTickets(), 5000);
    return () => window.clearInterval(timer);
  // Chỉ khởi tạo lại bộ hẹn giờ khi khách đổi màn hình hoặc đổi phiếu.
  }, [open, view, user?.id, activeTicket?.id]);

  const openTicket = async (ticketId: string) => {
    const result = tickets.length ? tickets : await loadTickets();
    const ticket = result.find(item => item.id === ticketId);
    if (!ticket) {
      toast.error('Không tìm thấy phiếu hỗ trợ');
      return;
    }
    setActiveTicket(ticket);
    setView('conversation');
  };

  const submitMessage = async (message: string) => {
    if (!message || sending) return;
    if (!user) {
      setMessages(previous => [...previous, { role: 'assistant', text: 'Bạn cần đăng nhập để tra cứu đơn và liên hệ bộ phận hỗ trợ.' }]);
      return;
    }
    setMessages(previous => [...previous, { role: 'user', text: message }]);
    setInput('');
    setSending(true);
    try {
      const response = await sendSupportMessage({ message, orderId: selectedOrderId || undefined });
      setMessages(previous => [...previous, {
        role: 'assistant', text: response.reply,
        handoffTarget: response.handoffTarget, supportTicketId: response.supportTicketId,
        suggestions: response.suggestions, requiresOrder: response.requiresOrder,
      }]);
      if (response.requiresOrder && customerOrders.length > 0 && !selectedOrderId) {
        toast('Hãy chọn đơn hàng ở đầu khung chat để mình kiểm tra chính xác.');
      }
      if (response.supportTicketId) await loadTickets();
    } catch (error: any) {
      setMessages(previous => [...previous, {
        role: 'assistant',
        text: error?.response?.data?.message || 'Hiện tại mình chưa kết nối được bộ phận hỗ trợ. Bạn vui lòng thử lại sau.',
      }]);
    } finally {
      setSending(false);
    }
  };

  const createContact = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!user) {
      toast.error('Vui lòng đăng nhập để liên hệ nhân viên hỗ trợ');
      return;
    }
    if (issueNeedsOrder && !selectedOrderId) {
      toast.error('Vui lòng chọn đơn hàng liên quan');
      return;
    }
    if (contactMessage.trim().length < 10) {
      toast.error('Vui lòng mô tả vấn đề ít nhất 10 ký tự');
      return;
    }
    setSending(true);
    try {
      const transcript = messages
        .map(item => `${item.role === 'user' ? 'Khách hàng' : 'Trợ lý'}: ${item.text}`)
        .join('\n')
        .slice(-5000);
      const ticket = await createSupportContact({
        issueType,
        orderId: selectedOrderId || undefined,
        message: contactMessage.trim(),
        aiTranscript: transcript || undefined,
      });
      setTickets(current => [ticket, ...current.filter(item => item.id !== ticket.id)]);
      setActiveTicket(ticket);
      setContactMessage('');
      setView('conversation');
      toast.success(`Đã tạo phiếu ${ticket.id}`);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể tạo yêu cầu hỗ trợ');
    } finally {
      setSending(false);
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    await submitMessage(input.trim());
  };

  if (user?.role === 'admin' || user?.role === 'restaurant') return null;

  return (
    <>
      {open && (
        <Panel aria-label="Trợ lý và hỗ trợ FoodFast">
          <Header>
            <h3>✨ Hỗ trợ FoodFast</h3>
            <p>Trợ lý trả lời nhanh; nhân viên xử lý các vấn đề cần kiểm tra.</p>
          </Header>
          <Navigation>
            <NavButton $active={view === 'assistant'} onClick={() => setView('assistant')}>Trợ lý AI</NavButton>
            <NavButton $active={view !== 'assistant'} onClick={async () => { await loadTickets(); setView('tickets'); }}>
              Phiếu của tôi{tickets.length ? ` (${tickets.length})` : ''}
            </NavButton>
          </Navigation>

          {view === 'assistant' && (
            <>
              <Content>
                {customerOrders.length > 0 && (
                  <OrderSelect aria-label="Chọn đơn cần hỗ trợ" value={selectedOrderId} onChange={event => setSelectedOrderId(event.target.value)}>
                    <option value="">Không chọn đơn cụ thể</option>
                    {customerOrders.map(order => <option key={order.id} value={order.id}>{order.id} · {getStatusLabel(order.status)}</option>)}
                  </OrderSelect>
                )}
                <QuickActions aria-label="Câu hỏi nhanh">
                  <QuickAction disabled={sending} onClick={() => submitMessage('Trạng thái đơn')}>Trạng thái đơn</QuickAction>
                  <QuickAction disabled={sending} onClick={() => submitMessage('Khi nào giao tới?')}>Khi nào giao tới?</QuickAction>
                  <QuickAction disabled={sending} onClick={() => submitMessage('Phí giao hàng')}>Phí giao hàng</QuickAction>
                  <QuickAction disabled={sending} onClick={() => submitMessage('Thanh toán VNPay')}>Thanh toán VNPay</QuickAction>
                  <QuickAction disabled={sending} onClick={() => submitMessage('Mã giảm giá')}>Mã giảm giá</QuickAction>
                  <QuickAction disabled={sending} onClick={() => submitMessage('Đặt lại đơn cũ')}>Đặt lại đơn</QuickAction>
                </QuickActions>
                {messages.map((message, index) => (
                  <Bubble key={index} $role={message.role}>
                    {message.text}
                    {message.supportTicketId && (
                      <TicketLink onClick={() => void openTicket(message.supportTicketId!)}>
                        Mở phiếu {message.supportTicketId}
                      </TicketLink>
                    )}
                    {message.suggestions && message.suggestions.length > 0 && (
                      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 9 }}>
                        {message.suggestions.map(suggestion => (
                          <QuickAction
                            type="button"
                            key={suggestion}
                            disabled={sending}
                            onClick={() => void submitMessage(suggestion)}
                          >
                            {suggestion}
                          </QuickAction>
                        ))}
                      </div>
                    )}
                  </Bubble>
                ))}
                {sending && <Bubble $role="assistant">Đang kiểm tra...</Bubble>}
                <ContactCallout onClick={() => setView('contact')}>🎧 Liên hệ nhân viên hỗ trợ</ContactCallout>
              </Content>
              <Form onSubmit={handleSubmit}>
                <Input aria-label="Nội dung cần hỗ trợ" placeholder="Nhập câu hỏi..." value={input} maxLength={1000} onChange={event => setInput(event.target.value)} />
                <PrimaryButton disabled={sending || !input.trim()}>Gửi</PrimaryButton>
              </Form>
            </>
          )}

          {view === 'contact' && (
            <Content>
              <Back type="button" onClick={() => setView('assistant')}>← Quay lại trợ lý</Back>
              <h3 style={{ marginTop: 0 }}>Liên hệ nhân viên hỗ trợ</h3>
              <ContactForm onSubmit={createContact}>
                <label>Vấn đề cần hỗ trợ
                  <select value={issueType} onChange={event => setIssueType(event.target.value as SupportIssueType)}>
                    {Object.entries(issueLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                </label>
                <label>Đơn hàng {issueNeedsOrder ? '*' : '(nếu có)'}
                  <OrderSelect value={selectedOrderId} onChange={event => setSelectedOrderId(event.target.value)}>
                    <option value="">Chưa chọn đơn</option>
                    {customerOrders.map(order => <option key={order.id} value={order.id}>{order.id} · {getStatusLabel(order.status)}</option>)}
                  </OrderSelect>
                </label>
                <label>Mô tả vấn đề *
                  <textarea value={contactMessage} maxLength={1000} placeholder="Cho nhân viên biết điều gì đã xảy ra và bạn cần hỗ trợ gì..." onChange={event => setContactMessage(event.target.value)} />
                </label>
                <p>Hệ thống tự chuyển yêu cầu tới nhà hàng hoặc bộ phận FoodFast phù hợp. Đoạn chat với trợ lý sẽ được đính kèm làm ngữ cảnh.</p>
                <PrimaryButton disabled={sending || contactMessage.trim().length < 10}>Gửi yêu cầu</PrimaryButton>
              </ContactForm>
            </Content>
          )}

          {view === 'tickets' && (
            <Content>
              <h3 style={{ marginTop: 0 }}>Phiếu hỗ trợ của tôi</h3>
              {!user && <p>Vui lòng đăng nhập để xem yêu cầu hỗ trợ.</p>}
              {user && tickets.length === 0 && <p>Bạn chưa có phiếu hỗ trợ nào.</p>}
              {tickets.map(ticket => (
                <TicketCard key={ticket.id} onClick={() => { setActiveTicket(ticket); setView('conversation'); }}>
                  <strong>{ticket.id}{ticket.orderId ? ` · ${ticket.orderId}` : ''}</strong>
                  <span>{ticketStatus[ticket.status]} · {ticket.target === 'ADMIN' ? 'FoodFast' : 'Nhà hàng'}</span>
                  <span>{ticket.message}</span>
                </TicketCard>
              ))}
              <ContactCallout onClick={() => setView('contact')}>+ Tạo yêu cầu mới</ContactCallout>
            </Content>
          )}

          {view === 'conversation' && activeTicket && (
            <Content>
              <Back type="button" onClick={async () => { await loadTickets(); setView('tickets'); }}>← Danh sách phiếu</Back>
              <SupportConversation ticket={activeTicket} viewerRole="CUSTOMER" onMessageSent={() => void loadTickets()} />
            </Content>
          )}
        </Panel>
      )}
      <Toggle aria-label={open ? 'Đóng hỗ trợ' : 'Mở hỗ trợ'} onClick={() => setOpen(value => !value)}>
        {open ? '×' : '💬'}
      </Toggle>
    </>
  );
};

export default SupportChatWidget;
