import React, { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import styled from 'styled-components';
import dayjs from 'dayjs';
import { getOrderGroup, OrderGroup } from '@/api/orderApi';
import { formatVND } from '@/utils/currency';
import { getStatusColors, getStatusLabel } from '@/utils/orderStatus';
import { getPaymentStatusInfo } from '@/utils/paymentStatus';

const Page = styled.main`
  max-width: 900px;
  margin: 0 auto;
  padding: 28px 20px;
`;

const Panel = styled.section`
  padding: 22px;
  margin-bottom: 18px;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: var(--card);
  box-shadow: var(--shadow);
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  flex-wrap: wrap;
`;

const ChildGrid = styled.div`
  display: grid;
  gap: 14px;
`;

const Child = styled(Link)`
  display: block;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  text-decoration: none;
  color: var(--text);
  transition: border-color .15s ease, transform .15s ease;
  &:hover { border-color: var(--primary); transform: translateY(-1px); }
`;

const Row = styled.div`
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 7px;
`;

const Badge = styled.span<{ $background: string; $color: string }>`
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  background: ${props => props.$background};
  color: ${props => props.$color};
  font-size: 12px;
  font-weight: 700;
`;

type LocationState = { group?: OrderGroup };

const OrderGroupDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const [group, setGroup] = useState<OrderGroup | null>((location.state as LocationState | null)?.group || null);
  const [loading, setLoading] = useState(!group);

  useEffect(() => {
    if (!id || group?.id === id) return;
    setLoading(true);
    getOrderGroup(id)
      .then(setGroup)
      .finally(() => setLoading(false));
  }, [group?.id, id]);

  if (loading) return <Page>Đang tải nhóm đặt hàng...</Page>;
  if (!group) return <Page>Không tìm thấy nhóm đặt hàng.</Page>;
  const groupPayment = getPaymentStatusInfo(group.paymentMethod, group.paymentStatus);

  return (
    <Page>
      <Panel>
        <Header>
          <div>
            <h2 style={{ margin: 0 }}>Đặt hàng thành công</h2>
            <div style={{ color: 'var(--secondaryText)', marginTop: 6 }}>
              Mã nhóm: <strong>{group.id}</strong> · {dayjs(group.createdAt).format('HH:mm - DD/MM/YYYY')}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ color: 'var(--secondaryText)' }}>Số tiền cần thanh toán</div>
            <strong style={{ color: 'var(--primary)', fontSize: 22 }}>{formatVND(group.payableAmount)}</strong>
            <div style={{ marginTop: 6 }}>
              <Badge $background={groupPayment.bg} $color={groupPayment.color}>{groupPayment.label}</Badge>
            </div>
          </div>
        </Header>
        <p style={{ marginBottom: 0 }}>
          FoodFast đã tách lần đặt này thành <strong>{group.orderCount} đơn</strong>. Mỗi nhà hàng chỉ xử lý đơn của mình và mỗi đơn có trạng thái giao hàng riêng.
        </p>
      </Panel>

      <Panel>
        <h3 style={{ marginTop: 0 }}>Các đơn trong nhóm</h3>
        <ChildGrid>
          {group.orders.map(order => {
            const status = getStatusColors(order.status);
            const payment = getPaymentStatusInfo(order.paymentMethod, order.paymentStatus);
            return (
              <Child key={order.id} to={`/orders/${order.id}`}>
                <Header>
                  <div>
                    <strong>{order.restaurant || order.restaurantId}</strong>
                    <div style={{ marginTop: 4, color: 'var(--secondaryText)' }}>{order.id}</div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <strong>{formatVND(order.total)}</strong>
                    {(order.paymentAllocatedAmount || 0) > 0 && (
                      <div style={{ marginTop: 4, color: 'var(--secondaryText)', fontSize: 12 }}>
                        Đã phân bổ {formatVND(order.paymentAllocatedAmount || 0)}
                      </div>
                    )}
                  </div>
                </Header>
                <Row>
                  <span>{order.items.length} món · Phí giao hàng {formatVND(order.shippingFee || 0)}</span>
                  <span style={{ display: 'flex', gap: 6, flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                    <Badge $background={status.bg} $color={status.color}>{getStatusLabel(order.status)}</Badge>
                    <Badge $background={payment.bg} $color={payment.color}>{payment.label}</Badge>
                  </span>
                </Row>
              </Child>
            );
          })}
        </ChildGrid>
      </Panel>

      <Panel>
        <h3 style={{ marginTop: 0 }}>Tổng hợp chi phí</h3>
        <Row><span>Tạm tính</span><span>{formatVND(group.subtotal)}</span></Row>
        <Row><span>Phí giao hàng</span><span>{formatVND(group.shippingFee)}</span></Row>
        {group.taxAmount > 0 && <Row><span>Thuế</span><span>{formatVND(group.taxAmount)}</span></Row>}
        {group.discountAmount > 0 && <Row><span>Ưu đãi</span><span>−{formatVND(group.discountAmount)}</span></Row>}
        <Row><strong>Tổng cộng</strong><strong>{formatVND(group.total)}</strong></Row>
        {group.payableAmount !== group.total && (
          <Row><strong>Còn phải thanh toán sau khi loại đơn bị từ chối</strong><strong>{formatVND(group.payableAmount)}</strong></Row>
        )}
        {group.partialRefundAmount > 0 && <Row><span>Đã duyệt hoàn</span><span>{formatVND(group.partialRefundAmount)}</span></Row>}
        {group.refundedAmount > 0 && <Row><span>Đã hoàn thực tế</span><span>{formatVND(group.refundedAmount)}</span></Row>}
        {group.vnpayTransactionId && <Row><span>Mã giao dịch VNPay</span><span>{group.vnpayTransactionId}</span></Row>}
        <div style={{ marginTop: 18 }}>
          <strong>Giao đến:</strong> {group.address}
        </div>
      </Panel>

      <div style={{ display: 'flex', gap: 12 }}>
        <Link to="/orders" style={{ color: 'var(--primary)', fontWeight: 700 }}>Xem lịch sử đặt hàng</Link>
        <Link to="/menu" style={{ color: 'var(--primary)', fontWeight: 700 }}>Tiếp tục mua món</Link>
      </div>
    </Page>
  );
};

export default OrderGroupDetail;
