import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { Order, getOrder } from '@/api/orderApi';
import { getAllProducts, Product } from '../data/products';
import { formatVND } from '../utils/currency';
import { getStatusLabel, getStatusColors } from '../utils/orderStatus';
import { getPaymentStatusInfo } from '../utils/paymentStatus';

const NO_IMAGE_SVG =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80'%3E%3Crect width='80' height='80' fill='%23e8e8e8'/%3E%3Ctext x='50%25' y='54%25' font-size='11' text-anchor='middle' fill='%23aaa' font-family='sans-serif'%3EKh%C3%B4ng%20c%C3%B3%20%E1%BA%A3nh%3C/text%3E%3C/svg%3E";

const PAYMENT_LABELS: Record<string, string> = {
  cod: 'Thanh toán khi nhận hàng (COD)',
  cash: 'Thanh toán khi nhận hàng (COD)',
  vnpay: 'VNPay',
  momo: 'MoMo',
  zalopay: 'ZaloPay',
  visa: 'Thẻ Visa',
};

const Page = styled.div`
  max-width: 640px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);

  @media (max-width: 480px) {
    padding: 16px 12px;
  }
`;

const Card = styled(motion.div)`
  background: var(--card);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
  padding: 32px;

  @media (max-width: 480px) {
    padding: 16px;
  }
`;

const SuccessIcon = styled.div`
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--success);
  color: #fff;
  font-size: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
`;

const Title = styled.h2`
  text-align: center;
  margin: 0 0 4px 0;
  color: var(--text);
`;

const OrderCode = styled.div`
  text-align: center;
  color: var(--secondaryText);
  margin-bottom: 16px;
`;

const StatusBadge = styled.span<{ $bg: string; $color: string }>`
  display: inline-block;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  background: ${props => props.$bg};
  color: ${props => props.$color};
`;

const BadgeWrap = styled.div`
  text-align: center;
  margin-bottom: 24px;
`;

const SectionTitle = styled.h3`
  margin: 24px 0 12px 0;
  color: var(--text);
  font-size: 16px;
  border-bottom: 2px solid var(--border);
  padding-bottom: 8px;
`;

const ItemRow = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);

  &:last-child {
    border-bottom: none;
  }

  @media (max-width: 480px) {
    gap: 8px;
    padding: 6px 0;
  }
`;

const Thumb = styled.img`
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  flex-shrink: 0;

  @media (max-width: 480px) {
    width: 40px;
    height: 40px;
  }
`;

const ItemName = styled.div`
  flex: 1;
  font-weight: 600;
  color: var(--text);

  @media (max-width: 480px) {
    font-size: 13px;
  }
`;

const ItemQty = styled.div`
  color: var(--secondaryText);
  font-size: 14px;

  @media (max-width: 480px) {
    font-size: 12px;
  }
`;

const ItemPrice = styled.div`
  font-weight: 700;
  color: var(--text);
  min-width: 90px;
  text-align: right;

  @media (max-width: 480px) {
    min-width: 68px;
    font-size: 13px;
  }
`;

const SummaryRow = styled.div<{ $bold?: boolean }>`
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  color: ${props => (props.$bold ? 'var(--text)' : 'var(--secondaryText)')};
  font-weight: ${props => (props.$bold ? 700 : 400)};
  font-size: ${props => (props.$bold ? '18px' : '14px')};

  ${props => props.$bold && `
    border-top: 1px solid var(--border);
    margin-top: 8px;
    padding-top: 12px;
    color: var(--primary);
  `}
`;

const InfoRow = styled.div`
  margin-bottom: 6px;
  color: var(--text);

  span {
    color: var(--secondaryText);
  }
`;

const ButtonsWrap = styled.div`
  display: flex;
  gap: 12px;
  margin-top: 32px;

  @media (max-width: 480px) {
    gap: 8px;
    margin-top: 20px;
  }
`;

const PrimaryButton = styled(Link)`
  flex: 1;
  text-align: center;
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  font-weight: 700;
  text-decoration: none;
  transition: transform .15s ease, filter .15s ease;

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.03);
  }
`;

const SecondaryButton = styled(Link)`
  flex: 1;
  text-align: center;
  background: var(--border);
  color: var(--text);
  border: none;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  font-weight: 700;
  text-decoration: none;
  transition: transform .15s ease, filter .15s ease;

  &:hover {
    transform: translateY(-1px);
    filter: brightness(1.03);
  }
`;

const Centered = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: var(--secondaryText);
`;

type LocationState = {
  order?: Order;
  cartItems?: { id: string; productId: string; name: string; image?: string; price: number; qty: number }[];
};

const OrderConfirmation: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const state = (location.state || {}) as LocationState;

  const [order, setOrder] = useState<Order | undefined>(state.order);
  const [loading, setLoading] = useState(!state.order);
  const [products, setProducts] = useState<Product[]>([]);

  useEffect(() => {
    if (order || !id) return;
    let mounted = true;
    getOrder(id)
      .then((data) => {
        if (mounted) setOrder(data);
      })
      .catch((err) => console.error('Failed to load order:', err))
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [id, order]);

  useEffect(() => {
    getAllProducts()
      .then(setProducts)
      .catch((err) => console.error('Failed to load products:', err));
  }, []);

  const productMap = useMemo(
    () => Object.fromEntries(products.map((p) => [p.id, p])),
    [products]
  );

  if (loading) {
    return (
      <Page>
        <Centered>Đang tải thông tin đơn hàng...</Centered>
      </Page>
    );
  }

  if (!order) {
    return (
      <Page>
        <Centered>
          Không tìm thấy thông tin đơn hàng.
          <div style={{ marginTop: 16 }}>
            <Link to="/menu" style={{ color: 'var(--primary)' }}>Quay lại thực đơn</Link>
          </div>
        </Centered>
      </Page>
    );
  }

  const cartItems = state.cartItems;
  const orderItems = order.items || [];

  const calculatedSubtotal = orderItems.reduce((sum, item) => {
    const qty = item.qty ?? item.quantity ?? 0;
    return sum + item.price * qty;
  }, 0);
  const subtotal = order.subtotal ?? calculatedSubtotal;
  const shipping = order.shippingFee ?? 15000;
  const discount = order.discountAmount ?? 0;

  const statusColors = getStatusColors(order.status);
  const paymentLabel = order.paymentMethod
    ? PAYMENT_LABELS[order.paymentMethod] || order.paymentMethod
    : 'Thanh toán khi nhận hàng (COD)';
  const paymentStatusInfo = getPaymentStatusInfo(order.paymentMethod, order.paymentStatus);

  return (
    <Page>
      <Card initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
        <SuccessIcon>✓</SuccessIcon>
        <Title>Đặt hàng thành công!</Title>
        <OrderCode>Mã đơn hàng: <strong>{order.id}</strong></OrderCode>

        <BadgeWrap>
          <StatusBadge $bg={statusColors.bg} $color={statusColors.color}>
            {getStatusLabel(order.status)}
          </StatusBadge>
        </BadgeWrap>

        <SectionTitle>Món đã đặt</SectionTitle>
        {orderItems.map((item, idx) => {
          const qty = item.qty ?? item.quantity ?? 0;
          const cartItem = cartItems?.find((c) => c.name === item.name);
          const product = cartItem ? productMap[cartItem.productId] : undefined;
          const imgSrc = item.productImage || product?.image || cartItem?.image || NO_IMAGE_SVG;
          return (
            <ItemRow key={idx}>
              <Thumb
                src={imgSrc}
                alt={item.name}
                onError={(e) => {
                  e.currentTarget.onerror = null;
                  e.currentTarget.src = NO_IMAGE_SVG;
                }}
              />
              <ItemName>
                {item.name}
                {(item.selectedOptions?.length ?? 0) > 0 && (
                  <small style={{ display: 'block', fontWeight: 400, color: 'var(--secondaryText)' }}>
                    {item.selectedOptions?.map((option: any) => `${option.groupName}: ${option.optionName}`).join(' · ')}
                  </small>
                )}
                {item.itemNote && (
                  <small style={{ display: 'block', fontWeight: 400, color: 'var(--secondaryText)' }}>
                    Ghi chú: {item.itemNote}
                  </small>
                )}
              </ItemName>
              <ItemQty>x{qty}</ItemQty>
              <ItemPrice>{formatVND(item.price * qty)}</ItemPrice>
            </ItemRow>
          );
        })}

        <SectionTitle>Chi tiết thanh toán</SectionTitle>
        <SummaryRow>
          <span>Tạm tính</span>
          <span>{formatVND(subtotal)}</span>
        </SummaryRow>
        <SummaryRow>
          <span>Phí vận chuyển</span>
          <span>{formatVND(shipping)}</span>
        </SummaryRow>
        {discount > 0 && (
          <SummaryRow>
            <span>Ưu đãi {order.voucherCode ? `(${order.voucherCode})` : ''}</span>
            <span style={{ color: '#15803d' }}>−{formatVND(discount)}</span>
          </SummaryRow>
        )}
        <SummaryRow $bold>
          <span>Tổng cộng</span>
          <span>{formatVND(order.total)}</span>
        </SummaryRow>
        <InfoRow><span>Phương thức: </span>{paymentLabel}</InfoRow>
        <InfoRow>
          <span>Trạng thái: </span>
          <StatusBadge $bg={paymentStatusInfo.bg} $color={paymentStatusInfo.color}>
            {paymentStatusInfo.label}
          </StatusBadge>
        </InfoRow>
        {order.paidAt && <InfoRow><span>Thanh toán lúc: </span>{new Date(order.paidAt).toLocaleString('vi-VN')}</InfoRow>}
        {order.vnpayTransactionId && <InfoRow><span>Mã giao dịch VNPay: </span>{order.vnpayTransactionId}</InfoRow>}

        <SectionTitle>Thông tin nhận hàng</SectionTitle>
        <InfoRow><span>Người nhận: </span>{order.name}</InfoRow>
        <InfoRow><span>Điện thoại: </span>{order.phone}</InfoRow>
        <InfoRow><span>Địa chỉ: </span>{order.address}</InfoRow>

        <ButtonsWrap>
          <SecondaryButton to="/menu">Tiếp tục mua sắm</SecondaryButton>
          <PrimaryButton to={`/orders/${order.id}`}>Theo dõi đơn hàng</PrimaryButton>
        </ButtonsWrap>
      </Card>
    </Page>
  );
};

export default OrderConfirmation;
