import React, { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import dayjs from 'dayjs';
import toast from 'react-hot-toast';
import { useOrders, Order } from '@/context/OrderContext';
import { useAuth } from '@/context';
import { getOrders } from '@/api/orderApi';
import { formatVND } from '../utils/currency';
import { getStatusLabel, getStatusColors } from '../utils/orderStatus';
import { getPaymentStatusInfo } from '../utils/paymentStatus';

const Page = styled.div`
  max-width: 800px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);
`;

const Title = styled.h2`
  margin: 0 0 20px 0;
  color: var(--text);
`;

const SearchForm = styled.div`
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  padding: 20px;
  background: var(--card);
  border-radius: 12px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
`;

const Input = styled.input`
  padding: 12px 16px;
  border-radius: 8px;
  border: 2px solid var(--border);
  min-width: 220px;
  flex: 1;
  background: var(--card);
  color: var(--text);
  font-size: 14px;

  &:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(255, 102, 0, 0.1);
  }
`;

const Button = styled.button`
  padding: 12px 20px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  box-shadow: var(--shadow);
  transition: transform .15s ease, filter .15s ease;

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    filter: brightness(1.03);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

const OrderCardLink = styled(Link)`
  display: block;
  text-decoration: none;
  color: inherit;
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: var(--shadow);
  padding: 16px 20px;
  margin-bottom: 14px;
  transition: transform .15s ease, box-shadow .15s ease, border-color .15s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
    border-color: var(--primary);
  }
`;

const CardHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
`;

const OrderCode = styled.div`
  font-weight: 700;
  color: var(--text);
`;

const OrderDate = styled.div`
  font-size: 13px;
  color: var(--secondaryText);
  margin-top: 2px;
`;

const OrderTotal = styled.div`
  font-weight: 800;
  color: var(--primary);
  font-size: 16px;
`;

const StatusBadge = styled.span<{ $bg: string; $color: string }>`
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: ${props => props.$bg};
  color: ${props => props.$color};
`;

const CardFooter = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
`;

const BadgeGroup = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`;

const ViewDetails = styled.span`
  color: var(--primary);
  font-weight: 600;
  font-size: 14px;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  background: var(--card);
  border-radius: 12px;
  border: 1px solid var(--border);
  color: var(--secondaryText);

  h3 {
    color: var(--text);
    margin-bottom: 8px;
  }
`;

const ShopNowButton = styled(Link)`
  display: inline-block;
  margin-top: 16px;
  background: var(--primary);
  color: #fff;
  padding: 12px 24px;
  border-radius: var(--radius-md);
  text-decoration: none;
  font-weight: 700;
  transition: transform .15s ease, filter .15s ease;

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.03);
  }
`;

const sortNewestFirst = (list: Order[]) =>
  [...list].sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0));

const OrderCardItem: React.FC<{ order: Order | any }> = ({ order }) => {
  const colors = getStatusColors(order.status);
  const paymentStatus = getPaymentStatusInfo(order.paymentMethod, order.paymentStatus);
  return (
    <OrderCardLink to={`/orders/${order.id}`}>
      <CardHeader>
        <div>
          <OrderCode>{order.id}</OrderCode>
          <OrderDate>
            {order.createdAt ? dayjs(order.createdAt).format('HH:mm - DD/MM/YYYY') : ''}
          </OrderDate>
        </div>
        <OrderTotal>{formatVND(order.total)}</OrderTotal>
      </CardHeader>
      <CardFooter>
        <BadgeGroup>
          <StatusBadge $bg={colors.bg} $color={colors.color}>
            {getStatusLabel(order.status)}
          </StatusBadge>
          <StatusBadge $bg={paymentStatus.bg} $color={paymentStatus.color}>
            {paymentStatus.label}
          </StatusBadge>
        </BadgeGroup>
        <ViewDetails>Xem chi tiết →</ViewDetails>
      </CardFooter>
    </OrderCardLink>
  );
};

const MyOrders: React.FC = () => {
  const { user } = useAuth();
  const { orders } = useOrders();

  const [phone, setPhone] = useState('');
  const [guestOrders, setGuestOrders] = useState<any[]>([]);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const myOrders = useMemo(() => {
    if (!user) return [];
    return sortNewestFirst(orders.filter((o) => o.userId === user.id));
  }, [orders, user]);

  const handleSearch = async () => {
    if (!phone.trim()) {
      toast.error('Vui lòng nhập số điện thoại');
      return;
    }
    setSearching(true);
    try {
      const result = await getOrders({ phone: phone.trim() });
      setGuestOrders(sortNewestFirst(result as any));
      setHasSearched(true);
      if (result.length === 0) {
        toast('Không tìm thấy đơn hàng nào cho số điện thoại này', { icon: 'ℹ️' });
      }
    } catch (error) {
      console.error('Failed to fetch orders by phone:', error);
      toast.error('Không thể tải đơn hàng. Vui lòng thử lại.');
    } finally {
      setSearching(false);
    }
  };

  if (user) {
    return (
      <Page>
        <Title>Đơn hàng của tôi</Title>
        {myOrders.length === 0 ? (
          <EmptyState>
            <h3>Bạn chưa có đơn hàng nào</h3>
            <p>Hãy đặt món để theo dõi đơn hàng tại đây.</p>
            <ShopNowButton to="/menu">Đặt món ngay</ShopNowButton>
          </EmptyState>
        ) : (
          myOrders.map((order) => <OrderCardItem key={order.id} order={order} />)
        )}
      </Page>
    );
  }

  return (
    <Page>
      <Title>Tra cứu đơn hàng</Title>
      <SearchForm>
        <Input
          placeholder="Nhập số điện thoại đã dùng để đặt hàng"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
          disabled={searching}
        />
        <Button onClick={handleSearch} disabled={searching}>
          {searching ? 'Đang tìm...' : 'Xem đơn hàng'}
        </Button>
      </SearchForm>

      {hasSearched && guestOrders.length === 0 && (
        <EmptyState>
          <h3>Bạn chưa có đơn hàng nào</h3>
          <p>Hãy đặt món để theo dõi đơn hàng tại đây.</p>
          <ShopNowButton to="/menu">Đặt món ngay</ShopNowButton>
        </EmptyState>
      )}

      {guestOrders.map((order) => (
        <OrderCardItem key={order.id} order={order} />
      ))}
    </Page>
  );
};

export default MyOrders;
