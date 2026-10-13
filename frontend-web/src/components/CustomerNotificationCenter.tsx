import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import dayjs from 'dayjs';
import {
  CustomerNotification,
  getCustomerNotifications,
  markAllCustomerNotificationsRead,
  markCustomerNotificationRead,
} from '@/api/notificationApi';
import { subscribeNotificationUpdates } from '@/realtime/socket';

const Wrap = styled.div`
  position: relative;
`;

const Bell = styled.button`
  position: relative;
  width: 42px;
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 50%;
  background: var(--card);
  color: var(--text);
  cursor: pointer;
  font-size: 20px;
`;

const Badge = styled.span`
  position: absolute;
  top: -5px;
  right: -5px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: #d92d20;
  color: white;
  font-size: 11px;
  font-weight: 800;
`;

const Panel = styled.section`
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 100;
  width: min(390px, calc(100vw - 32px));
  max-height: 520px;
  overflow: auto;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--card);
  box-shadow: var(--shadow-lg, 0 14px 36px rgba(0, 0, 0, 0.2));
`;

const PanelHeader = styled.header`
  position: sticky;
  top: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
`;

const MarkAll = styled.button`
  border: none;
  background: transparent;
  color: var(--primary);
  font-weight: 700;
  cursor: pointer;
`;

const Item = styled.button<{ $unread: boolean }>`
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-bottom: 1px solid var(--border);
  background: ${({ $unread }) => ($unread ? 'rgba(255, 102, 0, 0.08)' : 'transparent')};
  color: var(--text);
  text-align: left;
  cursor: pointer;

  &:hover { background: rgba(255, 102, 0, 0.12); }
`;

const Empty = styled.p`
  margin: 0;
  padding: 32px 18px;
  color: var(--secondaryText);
  text-align: center;
`;

const CustomerNotificationCenter: React.FC = () => {
  const navigate = useNavigate();
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const [items, setItems] = useState<CustomerNotification[]>([]);
  const unread = items.filter(item => !item.read).length;

  const load = async () => {
    try {
      const result = await getCustomerNotifications();
      setItems(result.notifications);
    } catch (error) {
      console.error('[Notifications] Không thể tải thông báo khách hàng', error);
    }
  };

  useEffect(() => {
    void load();
    return subscribeNotificationUpdates((notification: CustomerNotification) => {
      setItems(current => [notification, ...current.filter(item => item.id !== notification.id)]);
    });
  }, []);

  useEffect(() => {
    if (!open) return undefined;
    const close = (event: MouseEvent) => {
      if (rootRef.current && !rootRef.current.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, [open]);

  const select = async (item: CustomerNotification) => {
    if (!item.read) {
      await markCustomerNotificationRead(item.id);
      setItems(current => current.map(value => value.id === item.id ? { ...value, read: true } : value));
    }
    setOpen(false);
    if (item.actionUrl) navigate(item.actionUrl);
  };

  const markAll = async () => {
    await markAllCustomerNotificationsRead();
    setItems(current => current.map(item => ({ ...item, read: true })));
  };

  return (
    <Wrap ref={rootRef}>
      <Bell
        type="button"
        aria-label={`Thông báo${unread ? `, ${unread} chưa đọc` : ''}`}
        aria-expanded={open}
        onClick={() => setOpen(value => !value)}
      >
        🔔
        {unread > 0 && <Badge>{unread > 99 ? '99+' : unread}</Badge>}
      </Bell>
      {open && (
        <Panel aria-label="Trung tâm thông báo khách hàng">
          <PanelHeader>
            <strong>Thông báo</strong>
            {unread > 0 && <MarkAll type="button" onClick={() => void markAll()}>Đánh dấu đã đọc</MarkAll>}
          </PanelHeader>
          {items.length === 0 ? <Empty>Chưa có thông báo nào.</Empty> : items.map(item => (
            <Item key={item.id} type="button" $unread={!item.read} onClick={() => void select(item)}>
              <strong>{item.title}</strong>
              <div style={{ marginTop: 4, lineHeight: 1.4 }}>{item.message}</div>
              <small style={{ display: 'block', marginTop: 6, color: 'var(--secondaryText)' }}>
                {dayjs(item.createdAt).format('HH:mm · DD/MM/YYYY')}
              </small>
            </Item>
          ))}
        </Panel>
      )}
    </Wrap>
  );
};

export default CustomerNotificationCenter;
