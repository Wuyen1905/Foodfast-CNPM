import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import toast from 'react-hot-toast';
import dayjs from 'dayjs';
import { formatVND } from '../../utils/currency';
import { useOrders, OrderStatus } from '@/context/OrderContext';
import { getStatusLabel, getStatusColors } from '@/utils/orderStatus';
import {
  DeliveryDetails,
  listRestaurantDeliveries,
} from '@/api/deliveryApi';
import { subscribeDeliveryUpdates } from '@/realtime/socket';
import CancellationDialog, { CANCELLATION_REASON_LABELS } from '@/components/CancellationDialog';
import { CancellationReasonCode } from '@/api/orderApi';
import Swal from 'sweetalert2';
import { getAllProducts, Product } from '@/data/products';
import { proposeSubstitution } from '@/api/substitutionApi';

interface Theme {
  primary: string;
  secondary: string;
  accent: string;
}

interface OrderTrackingProps {
  restaurantId: string;
  theme: Theme;
}

const OrderContainer = styled.div`
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
`;

const SectionHeader = styled.div`
  margin-bottom: 24px;
`;

const SectionTitle = styled.h2<{ $primary: string }>`
  color: ${props => props.$primary};
  margin: 0 0 16px 0;
  font-size: 24px;
  font-weight: 600;
`;

const SearchBar = styled.div`
  position: relative;
  margin-bottom: 24px;
`;

const SearchInput = styled.input`
  width: 100%;
  padding: 14px 48px 14px 16px;
  border: 2px solid #e1e5e9;
  border-radius: 12px;
  font-size: 16px;
  transition: all 0.3s ease;
  
  &:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
  }
  
  &::placeholder {
    color: #999;
  }
`;

const SearchIcon = styled.div`
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 20px;
`;

const StatsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
`;

const StatCard = styled.div<{ $primary: string }>`
  background: linear-gradient(135deg, ${props => props.$primary}, ${props => props.$primary}dd);
  padding: 20px;
  border-radius: 12px;
  color: white;
`;

const StatValue = styled.div`
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 4px;
`;

const StatLabel = styled.div`
  font-size: 14px;
  opacity: 0.9;
`;

const OrderCard = styled(motion.div)<{ $accent: string }>`
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  border: 2px solid ${props => props.$accent};
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    border-color: ${props => props.$accent}dd;
  }
`;

const OrderHeader = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 16px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
  }
`;

const OrderId = styled.div`
  font-weight: 600;
  color: #333;
  font-size: 18px;
`;

const OrderTime = styled.div`
  color: #666;
  font-size: 14px;
`;

const CustomerInfo = styled.div`
  background: white;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
`;

const CustomerLabel = styled.div`
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
`;

const CustomerValue = styled.div`
  font-size: 14px;
  color: #333;
  font-weight: 600;
`;

const OrderDetails = styled.div`
  margin-bottom: 12px;
`;

const OrderItem = styled.div`
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  color: #666;
  font-size: 14px;
  border-bottom: 1px solid #e1e5e9;
  
  &:last-child {
    border-bottom: none;
  }
`;

const OrderFooter = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 2px solid #dee2e6;
  
  @media (max-width: 768px) {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
`;

const OrderTotal = styled.div<{ $primary: string }>`
  font-weight: 700;
  color: ${props => props.$primary};
  font-size: 20px;
`;

const StatusBadge = styled.span<{ $bg: string; $color: string }>`
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  background: ${props => props.$bg};
  color: ${props => props.$color};
`;

const ActionButtons = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const ActionButton = styled.button<{ $primary?: string; $variant?: 'primary' | 'danger' | 'success' }>`
  padding: 8px 16px;
  background: ${props => {
    if (props.$variant === 'danger') return '#dc3545';
    if (props.$variant === 'success') return '#28a745';
    return props.$primary || '#007bff';
  }};
  color: white;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
`;

const NotesSection = styled.div`
  margin-top: 12px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #007bff;
`;

const NotesLabel = styled.div`
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 600;
`;

const NotesContent = styled.div`
  font-size: 13px;
  color: #333;
  white-space: pre-wrap;
  line-height: 1.5;
`;

const NotesInput = styled.textarea`
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  resize: vertical;
  min-height: 60px;
  font-family: inherit;
  
  &:focus {
    outline: none;
    border-color: #007bff;
    box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.1);
  }
`;

const StatusSelect = styled.select`
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  margin-right: 8px;
  
  &:focus {
    outline: none;
    border-color: #007bff;
  }
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: #999;
`;

const DeliveryPanel = styled.div`
  margin-top: 16px;
  padding: 16px;
  border-radius: 10px;
  background: #fff8ef;
  border: 1px solid #ffd7b0;
`;

const DeliveryTitle = styled.div`
  color: #9a4a00;
  font-weight: 700;
  margin-bottom: 10px;
`;

const DeliveryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 10px;
  margin-bottom: 10px;
`;

const DeliveryField = styled.label`
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
`;

const DeliveryFieldLabel = styled.span`
  color: #5f3a19;
  font-size: 13px;
  font-weight: 600;
`;

const DeliveryInput = styled.input`
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #d8dde3;
  border-radius: 7px;
  font: inherit;
`;

const DeliveryInputWithUnit = styled.div`
  position: relative;
`;

const DeliveryEtaInput = styled(DeliveryInput)`
  padding-right: 58px;
`;

const DeliveryInputUnit = styled.span`
  position: absolute;
  top: 50%;
  right: 12px;
  transform: translateY(-50%);
  color: #6b7280;
  font-size: 14px;
  pointer-events: none;
`;

const DeliverySelect = styled.select`
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #d8dde3;
  border-radius: 7px;
  background: white;
  font: inherit;
`;

const DeliveryInfo = styled.div`
  color: #444;
  line-height: 1.65;
  margin-bottom: 10px;
`;

const DeliveryEtaPreview = styled.div`
  margin: -2px 0 12px;
  color: #6f4a28;
  font-size: 13px;
`;

const OrderTracking: React.FC<OrderTrackingProps> = ({ restaurantId, theme }) => {
  const { orders, updateOrderStatus, confirmOrder, cancelOrder, addOrderNote } = useOrders();
  const [restaurantOrders, setRestaurantOrders] = useState<any[]>([]);
  const [filteredOrders, setFilteredOrders] = useState<any[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedOrders, setExpandedOrders] = useState<Set<string>>(new Set());
  const [noteInputs, setNoteInputs] = useState<Record<string, string>>({});
  const [deliveriesByOrder, setDeliveriesByOrder] = useState<Record<string, DeliveryDetails>>({});
  const [cancellationOrderId, setCancellationOrderId] = useState<string | null>(null);
  const [cancellationBusy, setCancellationBusy] = useState(false);
  const [replacementProducts, setReplacementProducts] = useState<Product[]>([]);

  useEffect(() => {
    getAllProducts().then(setReplacementProducts).catch(error => {
      console.error('Failed to load replacement products:', error);
    });
  }, []);

  const loadDeliveryData = async () => {
    try {
      const deliveryData = await listRestaurantDeliveries(restaurantId);
      setDeliveriesByOrder(
        deliveryData.reduce<Record<string, DeliveryDetails>>((result, delivery) => {
          if (!result[delivery.orderId]) result[delivery.orderId] = delivery;
          return result;
        }, {}),
      );
    } catch (error) {
      console.error('Failed to load delivery data:', error);
    }
  };

  useEffect(() => {
    loadDeliveryData();
    const unsubscribe = subscribeDeliveryUpdates((delivery: DeliveryDetails) => {
      if (delivery.restaurantId === restaurantId) {
        setDeliveriesByOrder(previous => ({ ...previous, [delivery.orderId]: delivery }));
      }
    });
    return unsubscribe;
  }, [restaurantId]);

  useEffect(() => {
    if (orders) {
      // Normalize restaurant IDs for comparison
      const normalizeId = (id: string) => {
        const map: Record<string, string> = {
          'rest_2': 'sweetdreams',
          'restaurant_2': 'aloha',
          'sweetdreams': 'sweetdreams',
          'aloha': 'aloha',
        };
        return map[id?.toLowerCase()] || id;
      };
      
      const normalizedRestaurantId = normalizeId(restaurantId);
      const filtered = orders.filter((order: any) => {
        const orderRestaurantId = order.restaurantId ? normalizeId(order.restaurantId) : null;
        return orderRestaurantId === normalizedRestaurantId;
      });
      setRestaurantOrders(filtered);
      setFilteredOrders(filtered);
    }
  }, [orders, restaurantId]);

  // Listen for new order notifications
  useEffect(() => {
    const handleNewOrder = (event: CustomEvent) => {
      const notification = event.detail;
      if (notification.restaurantId) {
        // Refresh orders when new order notification arrives
        const normalizeId = (id: string) => {
          const map: Record<string, string> = {
            'rest_2': 'sweetdreams',
            'restaurant_2': 'aloha',
            'sweetdreams': 'sweetdreams',
            'aloha': 'aloha',
          };
          return map[id?.toLowerCase()] || id;
        };
        
        const normalizedRestaurantId = normalizeId(restaurantId);
        const normalizedNotificationId = normalizeId(notification.restaurantId);
        
        if (normalizedNotificationId === normalizedRestaurantId) {
          // Trigger a refresh by reloading orders from backend
          // This will be handled by the useOrders hook or component refresh
        }
      }
    };

    const handleOrderUpdate = (event: CustomEvent) => {
      // Refresh orders when order is updated
      // This will be handled by the useOrders hook or component refresh
    };

    window.addEventListener('newOrderNotification', handleNewOrder as EventListener);
    window.addEventListener('orderUpdated', handleOrderUpdate as EventListener);

    return () => {
      window.removeEventListener('newOrderNotification', handleNewOrder as EventListener);
      window.removeEventListener('orderUpdated', handleOrderUpdate as EventListener);
    };
  }, [restaurantId]);

  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredOrders(restaurantOrders);
    } else {
      const query = searchQuery.toLowerCase();
      const filtered = restaurantOrders.filter((order: any) => {
        const customerName = order.name?.toLowerCase() || '';
        const customerPhone = order.phone || '';
        return customerName.includes(query) || customerPhone.includes(query);
      });
      setFilteredOrders(filtered);
    }
  }, [searchQuery, restaurantOrders]);

  const stats = {
    totalOrders: restaurantOrders.length,
    pendingOrders: restaurantOrders.filter((o) => o.status === 'Pending').length,
    confirmedOrders: restaurantOrders.filter((o) => o.status === 'Confirmed').length,
    inProgressOrders: restaurantOrders.filter((o) => o.status === 'In Progress').length,
    readyOrders: restaurantOrders.filter((o) => o.status === 'Ready').length,
    totalRevenue: restaurantOrders.reduce((sum, order) => sum + (order.total || 0), 0),
  };

  const handleConfirmOrder = async (orderId: string) => {
    try {
      await confirmOrder(orderId);
      toast.success(`Đơn hàng ${orderId} đã được xác nhận! ✅`);
    } catch (error) {
      toast.error('Không thể xác nhận đơn hàng');
      console.error(error);
    }
  };

  const handleRejectOrder = async (orderId: string) => {
    setCancellationOrderId(orderId);
  };

  const handleConfirmCancellation = async (
    reasonCode: CancellationReasonCode,
    detail?: string,
  ) => {
    if (!cancellationOrderId) return;
    setCancellationBusy(true);
    try {
      await cancelOrder(cancellationOrderId, {
        reasonCode,
        detail,
      });
      toast.success(`Đơn hàng ${cancellationOrderId} đã được hủy và lưu lý do`);
      setCancellationOrderId(null);
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể hủy đơn hàng');
      console.error(error);
    } finally {
      setCancellationBusy(false);
    }
  };

  const handleUpdateStatus = async (orderId: string, newStatus: OrderStatus) => {
    try {
      await updateOrderStatus(orderId, newStatus);
      toast.success(`Đơn hàng ${orderId} đã được cập nhật thành ${getStatusLabel(newStatus)}! 🎉`);
    } catch (error) {
      toast.error('Không thể cập nhật trạng thái đơn hàng');
      console.error(error);
    }
  };

  const handleAddNote = async (orderId: string) => {
    const note = noteInputs[orderId]?.trim();
    if (!note) {
      toast.error('Vui lòng nhập ghi chú');
      return;
    }

    try {
      await addOrderNote(orderId, note);
      setNoteInputs(prev => ({ ...prev, [orderId]: '' }));
      toast.success('Ghi chú đã được thêm thành công');
    } catch (error) {
      toast.error('Không thể thêm ghi chú');
      console.error(error);
    }
  };

  const toggleOrderExpansion = (orderId: string) => {
    setExpandedOrders(prev => {
      const newSet = new Set(prev);
      if (newSet.has(orderId)) {
        newSet.delete(orderId);
      } else {
        newSet.add(orderId);
      }
      return newSet;
    });
  };

  const handleProposeSubstitution = async (order: any, item: any) => {
    const restaurantName = restaurantId.toLowerCase() === 'rest_2' ? 'sweetdreams'
      : restaurantId.toLowerCase() === 'restaurant_2' ? 'aloha'
      : restaurantId.toLowerCase();
    const candidates = replacementProducts.filter(product =>
      product.available
      && !product.customizable
      && product.id !== item.productId
      && product.price <= Number(item.price)
      && product.restaurant.toLowerCase() === restaurantName
    );
    if (!item.id) {
      toast.error('Đơn cũ chưa có mã dòng món để tạo đề xuất');
      return;
    }
    if (candidates.length === 0) {
      toast.error('Không có món cùng nhà hàng, còn bán và không đắt hơn để đề xuất');
      return;
    }
    const options = Object.fromEntries(
      candidates.map(product => [product.id, `${product.name} — ${formatVND(product.price)}`])
    );
    const selection = await Swal.fire({
      title: `Thay món “${item.name}”`,
      text: 'Khách sẽ có 15 phút để chấp nhận hoặc từ chối.',
      input: 'select',
      inputOptions: options,
      inputPlaceholder: 'Chọn món thay thế',
      showCancelButton: true,
      confirmButtonText: 'Tiếp tục',
      cancelButtonText: 'Hủy',
      inputValidator: value => value ? undefined : 'Vui lòng chọn món thay thế',
    });
    if (!selection.isConfirmed || !selection.value) return;
    const reason = await Swal.fire({
      title: 'Lý do đề xuất thay món',
      input: 'textarea',
      inputPlaceholder: 'Ví dụ: món khách đặt vừa hết nguyên liệu...',
      inputAttributes: { maxlength: '500' },
      showCancelButton: true,
      confirmButtonText: 'Gửi cho khách',
      cancelButtonText: 'Hủy',
      inputValidator: value => value?.trim().length >= 5 ? undefined : 'Vui lòng nhập ít nhất 5 ký tự',
    });
    if (!reason.isConfirmed) return;
    try {
      await proposeSubstitution(order.id, {
        orderItemId: Number(item.id),
        replacementProductId: selection.value,
        reason: reason.value.trim(),
        expiresInMinutes: 15,
      });
      toast.success('Đã gửi đề xuất thay món; vui lòng chờ khách phản hồi');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể gửi đề xuất thay món');
    }
  };

  return (
    <OrderContainer>
      <SectionHeader>
        <SectionTitle $primary={theme.primary}>
          📦 Theo dõi đơn hàng
        </SectionTitle>
      </SectionHeader>

      <SearchBar>
        <SearchInput
          type="text"
          placeholder="🔍 Tìm kiếm theo tên hoặc số điện thoại khách hàng..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
        <SearchIcon>🔍</SearchIcon>
      </SearchBar>

      <StatsGrid>
        <StatCard $primary={theme.primary}>
          <StatValue>{stats.totalOrders}</StatValue>
          <StatLabel>Tổng số đơn</StatLabel>
        </StatCard>
        <StatCard $primary={theme.secondary}>
          <StatValue>{stats.pendingOrders}</StatValue>
          <StatLabel>Chờ xác nhận</StatLabel>
        </StatCard>
        <StatCard $primary={theme.accent}>
          <StatValue>{stats.inProgressOrders}</StatValue>
          <StatLabel>Đang chế biến</StatLabel>
        </StatCard>
        <StatCard $primary={theme.primary}>
          <StatValue>{formatVND(stats.totalRevenue)}</StatValue>
          <StatLabel>Doanh thu</StatLabel>
        </StatCard>
      </StatsGrid>

      {filteredOrders.length === 0 ? (
        <EmptyState>
          <h3>{searchQuery ? 'Không tìm thấy đơn hàng phù hợp' : 'Chưa có đơn nào !'}</h3>
          <p>{searchQuery ? 'Thử tìm kiếm với từ khóa khác' : 'Đơn hàng sẽ xuất hiện ở đây sau khi khách hàng đặt hàng'}</p>
        </EmptyState>
      ) : (
        filteredOrders
          .sort((a, b) => (b.createdAt || 0) - (a.createdAt || 0))
          .map((order) => (
            <OrderCard
              key={order.id}
              $accent={theme.accent}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <OrderHeader>
                <div>
                  <OrderId>Đơn hàng #{order.id}</OrderId>
                  <OrderTime>
                    {order.createdAt ? dayjs(order.createdAt).format('DD/MM/YYYY HH:mm') : 'Không có'}
                    {order.confirmedAt && ` • Xác nhận: ${dayjs(order.confirmedAt).format('HH:mm')}`}
                  </OrderTime>
                </div>
                <StatusBadge $bg={getStatusColors(order.status).bg} $color={getStatusColors(order.status).color}>
                  {getStatusLabel(order.status)}
                </StatusBadge>
              </OrderHeader>

              <CustomerInfo>
                <CustomerLabel>Thông tin khách hàng:</CustomerLabel>
                <CustomerValue>
                  {order.name || 'Không có'} - {order.phone || 'Không có'}
                </CustomerValue>
                {order.address && (
                  <CustomerValue style={{ marginTop: '4px', fontSize: '13px', fontWeight: 'normal' }}>
                    📍 {order.address}
                  </CustomerValue>
                )}
              </CustomerInfo>

              {order.status === 'Cancelled' && order.cancellationReasonCode && (
                <NotesSection style={{ borderColor: '#fda29b', background: '#fff6f5' }}>
                  <NotesLabel>⛔ Lý do hủy / từ chối:</NotesLabel>
                  <div>
                    {CANCELLATION_REASON_LABELS[order.cancellationReasonCode as CancellationReasonCode]
                      || order.cancellationReasonCode}
                    {order.cancellationReasonDetail ? ` — ${order.cancellationReasonDetail}` : ''}
                  </div>
                  {order.paymentStatus === 'refund_pending' && (
                    <div style={{ marginTop: 6, fontWeight: 700, color: '#93370d' }}>
                      Đơn đã thanh toán và đang chờ hoàn tiền.
                    </div>
                  )}
                </NotesSection>
              )}

              <OrderDetails>
                {order.items?.map((item: any, index: number) => (
                  <OrderItem key={index}>
                    <span>
                      {item.qty}x {item.name}
                      {item.selectedOptions?.length > 0 && (
                        <small style={{ display: 'block', opacity: 0.75 }}>
                          {item.selectedOptions.map((option: any) => `${option.groupName}: ${option.optionName}`).join(' · ')}
                        </small>
                      )}
                      {item.itemNote && <small style={{ display: 'block', opacity: 0.75 }}>Ghi chú: {item.itemNote}</small>}
                      {['Pending', 'Confirmed', 'In Progress'].includes(order.status) && (
                        <button
                          type="button"
                          onClick={() => handleProposeSubstitution(order, item)}
                          style={{
                            display: 'block', marginTop: 6, padding: '5px 9px', border: '1px solid #f59e0b',
                            borderRadius: 6, background: '#fffbeb', color: '#92400e', cursor: 'pointer',
                          }}
                        >
                          Đề xuất thay món
                        </button>
                      )}
                    </span>
                    <span>{formatVND(item.price * item.qty)}</span>
                  </OrderItem>
                ))}
              </OrderDetails>

              {order.internalNotes && (
                <NotesSection>
                  <NotesLabel>📝 Ghi chú nội bộ:</NotesLabel>
                  <NotesContent>{order.internalNotes}</NotesContent>
                </NotesSection>
              )}

              {order.status === 'Ready' && (
                <DeliveryPanel>
                  <DeliveryTitle>🛵 Đã chuyển sang hàng đợi giao hàng</DeliveryTitle>
                  <DeliveryInfo>
                    Nhà hàng đã hoàn tất món. Admin FoodFast sẽ phân công hoặc đổi shipper cho đơn này.
                  </DeliveryInfo>
                </DeliveryPanel>
              )}
              {(() => {
                const delivery = deliveriesByOrder[order.id];
                if (!delivery || !['ASSIGNED', 'DELIVERING'].includes(delivery.status)) return null;
                return <DeliveryPanel>
                  <DeliveryTitle>{delivery.status === 'ASSIGNED' ? '📋 Admin đã phân shipper' : '🛵 Shipper đang giao'}</DeliveryTitle>
                  <DeliveryInfo>
                    <strong>{delivery.courierName}</strong><br />
                    {delivery.vehicleType || 'Chưa cập nhật phương tiện'}
                    {delivery.licensePlate ? ` · ${delivery.licensePlate}` : ''}<br />
                    Dự kiến giao: {delivery.estimatedDeliveryAt
                      ? dayjs(delivery.estimatedDeliveryAt).format('HH:mm DD/MM/YYYY') : 'Chưa có'}
                  </DeliveryInfo>
                </DeliveryPanel>;
              })()}

              <OrderFooter>
                <OrderTotal $primary={theme.primary}>
                  Tổng: {formatVND(order.total)}
                </OrderTotal>
                
                <ActionButtons>
                  {order.status === 'Pending' && (
                    <>
                      <ActionButton
                        $variant="success"
                        onClick={() => handleConfirmOrder(order.id)}
                      >
                        ✅ Xác nhận
                      </ActionButton>
                      <ActionButton
                        $variant="danger"
                        onClick={() => handleRejectOrder(order.id)}
                      >
                        ❌ Từ chối
                      </ActionButton>
                    </>
                  )}
                  
                  {(order.status === 'Confirmed' || order.status === 'In Progress') && (
                    <StatusSelect
                      value={order.status}
                      onChange={(e) => handleUpdateStatus(order.id, e.target.value as OrderStatus)}
                    >
                      <option value={order.status}>{getStatusLabel(order.status)} (Hiện tại)</option>
                      {order.status === 'Confirmed' && <option value="In Progress">Bắt đầu chuẩn bị</option>}
                      {order.status === 'In Progress' && <option value="Ready">Sẵn sàng giao</option>}
                    </StatusSelect>
                  )}

                  {(order.status === 'Confirmed' || order.status === 'In Progress' || order.status === 'Ready') && (
                    <ActionButton
                      $variant="danger"
                      onClick={() => handleRejectOrder(order.id)}
                    >
                      Hủy đơn
                    </ActionButton>
                  )}

                  <ActionButton
                    $primary={theme.primary}
                    onClick={() => toggleOrderExpansion(order.id)}
                  >
                    {expandedOrders.has(order.id) ? '📝 Ẩn ghi chú' : '📝 Thêm ghi chú'}
                  </ActionButton>
                </ActionButtons>
              </OrderFooter>

              {expandedOrders.has(order.id) && (
                <NotesSection style={{ marginTop: '12px' }}>
                  <NotesLabel>Thêm ghi chú nội bộ:</NotesLabel>
                  <NotesInput
                    value={noteInputs[order.id] || ''}
                    onChange={(e) => setNoteInputs(prev => ({ ...prev, [order.id]: e.target.value }))}
                    placeholder="Nhập ghi chú (ví dụ: Thời gian chuẩn bị, yêu cầu đặc biệt...)"
                  />
                  <ActionButton
                    $primary={theme.primary}
                    onClick={() => handleAddNote(order.id)}
                    style={{ marginTop: '8px' }}
                  >
                    Lưu ghi chú
                  </ActionButton>
                </NotesSection>
              )}
            </OrderCard>
          ))
      )}
      <CancellationDialog
        open={Boolean(cancellationOrderId)}
        actor="RESTAURANT"
        orderId={cancellationOrderId || undefined}
        busy={cancellationBusy}
        onClose={() => !cancellationBusy && setCancellationOrderId(null)}
        onConfirm={handleConfirmCancellation}
      />
    </OrderContainer>
  );
};

export default OrderTracking;

