import React, { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import dayjs from 'dayjs';
import toast from 'react-hot-toast';
import {
  CancellationReasonCode,
  Order as ApiOrder,
  OrderStatusHistory,
  getOrder,
  getOrderStatusHistory,
} from '@/api/orderApi';
import { useOrders } from '@/context/OrderContext';
import { useAuth } from '@/context/AuthContext';
import { useCart } from '@/context/CartContext';
import { getAllProducts, Product } from '../data/products';
import { formatVND } from '../utils/currency';
import { getStatusLabel, getStatusColors } from '../utils/orderStatus';
import { getPaymentStatusInfo, normalizePaymentStatus } from '../utils/paymentStatus';
import { normalizePaymentStatusValue } from '@/types/status';
import { confirmDeliveryReceived, DeliveryDetails, getDeliveryForOrder } from '@/api/deliveryApi';
import { subscribeDeliveryUpdates } from '@/realtime/socket';
import { createVNPayUrl } from '../services/vnpay';
import CancellationDialog, { CANCELLATION_REASON_LABELS } from '@/components/CancellationDialog';
import { createOrderReview, getOrderReview, Review } from '@/api/reviewApi';
import {
  listSubstitutions,
  respondToSubstitution,
  SubstitutionProposal,
} from '@/api/substitutionApi';
import OrderComplaintPanel from '@/components/OrderComplaintPanel';

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

// Tracker gọn cho khách; READY được gộp vào mốc chuẩn bị để không làm rối màn hình.
const STEPS = [
  { label: 'Chờ xác nhận', icon: '📝' },
  { label: 'Đã xác nhận', icon: '✅' },
  { label: 'Đang chuẩn bị', icon: '👨‍🍳' },
  { label: 'Nhân viên đang giao', icon: '🛵' },
  { label: 'Đã giao', icon: '🎉' },
];

const STATUS_TO_STEP: Record<string, number> = {
  Pending: 0,
  Confirmed: 1,
  'In Progress': 2,
  Ready: 2,
  Delivering: 3,
  Delivered: 4,
};

const Page = styled.div`
  max-width: 700px;
  margin: 0 auto;
  padding: var(--spacing-xl) var(--spacing-lg);

  @media (max-width: 480px) {
    padding: 16px 12px;
  }
`;

const Card = styled.div`
  background: var(--card);
  border-radius: 16px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);
  padding: 32px;

  @media (max-width: 480px) {
    padding: 16px;
  }
`;

const HeaderRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
`;

const OrderCode = styled.h2`
  margin: 0;
  color: var(--text);
`;

const OrderDate = styled.div`
  color: var(--secondaryText);
  font-size: 13px;
  margin-top: 4px;
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

const CancelledBanner = styled.div`
  background: #FFCDD2;
  color: #C62828;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  font-weight: 700;
  margin: 20px 0;
`;

const CancelAction = styled.button`
  margin-top: 14px;
  padding: 10px 16px;
  border: 1px solid #d92d20;
  border-radius: 8px;
  background: transparent;
  color: #b42318;
  font: inherit;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    background: #fff1f0;
  }
`;

const SupportAction = styled(CancelAction)`
  border-color: #f79009;
  color: #9a4a00;
  background: #fffaeb;

  &:hover { background: #fff4d6; }
`;

const StatusOverview = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin: 18px 0;

  @media (max-width: 600px) { grid-template-columns: 1fr; }
`;

const StatusOverviewItem = styled.div`
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface, var(--card));
  small { display: block; margin-bottom: 5px; color: var(--secondaryText); }
  strong { color: var(--text); line-height: 1.35; }
`;

const ReviewCard = styled.section`
  margin-top: 24px;
  padding: 18px;
  border: 1px solid #fedf89;
  border-radius: 12px;
  background: #fffaeb;
`;

const Stars = styled.div`
  display: flex;
  gap: 6px;
  margin: 12px 0;
`;

const StarButton = styled.button<{ $active: boolean }>`
  padding: 0;
  border: none;
  background: transparent;
  color: ${props => props.$active ? '#f79009' : '#d0d5dd'};
  font-size: 30px;
  cursor: pointer;
`;

const ReviewInput = styled.textarea`
  width: 100%;
  min-height: 86px;
  padding: 10px 12px;
  border: 1px solid #d0d5dd;
  border-radius: 8px;
  font: inherit;
  resize: vertical;
`;

const Timeline = styled.div`
  display: flex;
  justify-content: space-between;
  margin: 28px 0;

  @media (max-width: 480px) {
    margin: 20px 0;
  }
`;

const StepWrap = styled.div<{ $lineColor: string }>`
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;

  &:not(:last-child)::after {
    content: '';
    position: absolute;
    top: 20px;
    left: 50%;
    width: 100%;
    height: 3px;
    background: ${props => props.$lineColor};
    z-index: 0;
  }

  @media (max-width: 480px) {
    &:not(:last-child)::after {
      top: 14px;
    }
  }
`;

const StepCircle = styled.div<{ $status: 'done' | 'current' | 'upcoming' }>`
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  z-index: 1;
  position: relative;
  transition: all 0.3s ease;

  ${props => {
    switch (props.$status) {
      case 'done':
        return `background: var(--success); color: #fff;`;
      case 'current':
        return `
          background: var(--primary);
          color: #fff;
          box-shadow: 0 0 0 6px rgba(255, 102, 0, 0.18);
          animation: pulse 1.6s infinite;
        `;
      default:
        return `background: var(--border); color: var(--secondaryText);`;
    }
  }}

  @keyframes pulse {
    0% { box-shadow: 0 0 0 0 rgba(255, 102, 0, 0.35); }
    70% { box-shadow: 0 0 0 10px rgba(255, 102, 0, 0); }
    100% { box-shadow: 0 0 0 0 rgba(255, 102, 0, 0); }
  }

  @media (max-width: 480px) {
    width: 28px;
    height: 28px;
    font-size: 13px;

    ${props => props.$status === 'current' && `
      box-shadow: 0 0 0 4px rgba(255, 102, 0, 0.18);
    `}
  }
`;

const StepLabel = styled.div<{ $current?: boolean }>`
  margin-top: 8px;
  font-size: 12px;
  text-align: center;
  max-width: 80px;
  color: ${props => (props.$current ? 'var(--primary)' : 'var(--text)')};
  font-weight: ${props => (props.$current ? 700 : 500)};

  @media (max-width: 480px) {
    margin-top: 4px;
    font-size: 9px;
    max-width: 58px;
    line-height: 1.2;
  }
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

const PaymentRetryCard = styled.div`
  margin-top: 14px;
  padding: 14px;
  border: 1px solid #ffd18a;
  border-radius: 10px;
  background: #fff8e8;
  color: #67410a;
  font-size: 14px;
  line-height: 1.5;
`;

const PaymentRetryButton = styled.button`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 10px;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  font: inherit;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.65;
  }
`;

const CourierCard = styled.div`
  margin-top: 12px;
  padding: 14px;
  border-radius: 10px;
  border: 1px solid #ffd2ad;
  background: #fff8f2;
  color: var(--text);
  line-height: 1.65;
`;

const ReceiveConfirmation = styled.div`
  margin-top: 14px;
  padding: 14px;
  border-radius: 10px;
  background: #eefaf1;
  border: 1px solid #b8e6c2;
`;

const ReceiveTitle = styled.div`
  margin-bottom: 6px;
  color: #176b2c;
  font-weight: 700;
`;

const ReceiveHint = styled.div`
  margin-bottom: 12px;
  color: #395443;
  font-size: 13px;
  line-height: 1.5;
`;

const ReceiveActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`;

const ReceiveButton = styled.button<{ $secondary?: boolean }>`
  padding: 10px 16px;
  border: ${props => props.$secondary ? '1px solid #9ca3af' : 'none'};
  border-radius: 8px;
  background: ${props => props.$secondary ? '#fff' : '#22a447'};
  color: ${props => props.$secondary ? '#374151' : '#fff'};
  font: inherit;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

const DELIVERY_STATUS_LABELS: Record<string, string> = {
  ASSIGNED: 'Đã phân công',
  DELIVERING: 'Đang giao',
  DELIVERED: 'Đã giao',
  FAILED: 'Giao thất bại',
  CANCELLED: 'Đã hủy phân công',
};

const friendlyHistoryReason = (reason?: string) => {
  if (!reason) return '';
  return CANCELLATION_REASON_LABELS[reason as CancellationReasonCode] || reason;
};

const BackLink = styled(Link)`
  display: inline-block;
  margin-top: 24px;
  color: var(--primary);
  font-weight: 600;
  text-decoration: none;
`;

const ReorderCard = styled.section`
  margin-top: 22px;
  padding: 16px;
  border: 1px solid #fed7aa;
  border-radius: 12px;
  background: #fff7ed;
  color: #7c2d12;
`;

const ReorderButton = styled.button`
  margin-top: 12px;
  min-height: 44px;
  padding: 11px 18px;
  border: 0;
  border-radius: 9px;
  background: var(--primary);
  color: #fff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 4px 10px rgba(234, 88, 12, 0.24);

  &:hover:not(:disabled) {
    filter: brightness(1.04);
    transform: translateY(-1px);
  }

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
    box-shadow: none;
  }
`;

const Centered = styled.div`
  text-align: center;
  padding: 60px 20px;
  color: var(--secondaryText);
`;

const HistoryPanel = styled.details`
  margin: 18px 0 8px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface, var(--card));

  summary { cursor: pointer; font-weight: 800; }
`;

const HistoryItem = styled.div`
  margin-top: 12px;
  padding-left: 14px;
  border-left: 3px solid var(--primary);
  line-height: 1.45;
`;

const OrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { orders, loading: ordersLoading, cancelOrder } = useOrders();
  const { user } = useAuth();
  const { clear: clearCart, reorder } = useCart();

  const [fallbackOrder, setFallbackOrder] = useState<ApiOrder | undefined>();
  const [latestOrder, setLatestOrder] = useState<ApiOrder | undefined>();
  const [fallbackLoading, setFallbackLoading] = useState(false);
  const [notFound, setNotFound] = useState(false);
  const [products, setProducts] = useState<Product[]>([]);
  const [delivery, setDelivery] = useState<DeliveryDetails | null>(null);
  const [confirmationOpen, setConfirmationOpen] = useState(false);
  const [confirmingReceived, setConfirmingReceived] = useState(false);
  const [retryingPayment, setRetryingPayment] = useState(false);
  const [cancellationOpen, setCancellationOpen] = useState(false);
  const [cancellationBusy, setCancellationBusy] = useState(false);
  const [review, setReview] = useState<Review | null>(null);
  const [reviewRating, setReviewRating] = useState(5);
  const [reviewComment, setReviewComment] = useState('');
  const [reviewSubmitting, setReviewSubmitting] = useState(false);
  const [substitutions, setSubstitutions] = useState<SubstitutionProposal[]>([]);
  const [substitutionBusy, setSubstitutionBusy] = useState<number | null>(null);
  const [reordering, setReordering] = useState(false);
  const [statusHistory, setStatusHistory] = useState<OrderStatusHistory[]>([]);

  const contextOrder = orders.find((o) => o.id === id);
  const order: any = latestOrder || contextOrder || fallbackOrder;

  useEffect(() => {
    if (contextOrder) setLatestOrder(contextOrder as unknown as ApiOrder);
  }, [contextOrder]);

  useEffect(() => {
    if (contextOrder || ordersLoading || !id) return;
    let mounted = true;
    setFallbackLoading(true);
    getOrder(id)
      .then((data) => {
        if (mounted) setFallbackOrder(data);
      })
      .catch(() => {
        if (mounted) setNotFound(true);
      })
      .finally(() => {
        if (mounted) setFallbackLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [contextOrder, ordersLoading, id]);

  useEffect(() => {
    getAllProducts()
      .then(setProducts)
      .catch((err) => console.error('Failed to load products:', err));
  }, []);

  useEffect(() => {
    if (!id) return;
    let mounted = true;
    getDeliveryForOrder(id)
      .then(result => {
        if (mounted) setDelivery(result);
      })
      .catch(error => console.error('Failed to load delivery:', error));

    const unsubscribe = subscribeDeliveryUpdates((updated: DeliveryDetails) => {
      if (updated.orderId === id) setDelivery(updated);
    });
    return () => {
      mounted = false;
      unsubscribe();
    };
  }, [id]);

  useEffect(() => {
    if (!id) return;
    getOrderStatusHistory(id)
      .then(setStatusHistory)
      .catch(error => console.error('Failed to load order status history:', error));
  }, [id, order?.status]);

  const loadSubstitutions = async () => {
    if (!id) return;
    try {
      setSubstitutions(await listSubstitutions(id));
    } catch (error) {
      console.error('Failed to load substitutions:', error);
    }
  };

  useEffect(() => {
    loadSubstitutions();
  }, [id]);

  useEffect(() => {
    if (!id) return;
    getOrderReview(id)
      .then(setReview)
      .catch(error => console.error('Failed to load review:', error));
  }, [id]);

  const productByName = useMemo(
    () => Object.fromEntries(products.map((p) => [p.name, p])),
    [products]
  );

  const loading = ordersLoading || (!order && fallbackLoading);

  const handleConfirmReceived = async () => {
    if (!delivery || !user) return;
    setConfirmingReceived(true);
    try {
      const updated = await confirmDeliveryReceived(delivery.id);
      setDelivery(updated);
      setConfirmationOpen(false);
      toast.success('Cảm ơn bạn đã xác nhận nhận hàng');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể xác nhận nhận hàng');
    } finally {
      setConfirmingReceived(false);
    }
  };

  const handleRetryVNPay = async () => {
    if (!order?.id) return;
    setRetryingPayment(true);
    try {
      const paymentUrl = await createVNPayUrl(order.id);
      window.location.assign(paymentUrl);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Không thể tạo phiên VNPay mới');
      setRetryingPayment(false);
    }
  };

  const handleReorder = async () => {
    if (!order?.id || !user?.id) return;
    if (!window.confirm('Đưa các món còn bán của đơn này vào giỏ theo giá và tùy chọn hiện tại?')) return;
    setReordering(true);
    try {
      let result;
      try {
        result = await reorder(order.id);
      } catch (error: any) {
        if (error?.response?.status === 409
            && error?.response?.data?.code === 'CART_RESTAURANT_CONFLICT') {
          const replaceCart = window.confirm(
            'Giỏ hàng đang có món của nhà hàng khác. Bạn có muốn xóa giỏ hiện tại và đặt lại đơn này không?'
          );
          if (!replaceCart) return;
          await clearCart();
          result = await reorder(order.id);
        } else {
          throw error;
        }
      }
      if (result.requiresConfiguration.length > 0) {
        const firstItem = result.requiresConfiguration[0];
        const copiedMessage = result.copiedItemCount > 0
          ? `Đã đưa ${result.copiedItemCount} món hợp lệ vào giỏ. `
          : '';
        toast(`${copiedMessage}${firstItem.productName} cần bạn chọn lại tùy chọn theo thực đơn hiện tại.`, {
          id: 'reorder-options',
          icon: '⚙️',
          duration: 10_000,
        });
        navigate(`/details/${encodeURIComponent(firstItem.productId)}`, {
          state: {
            reorderQueue: result.requiresConfiguration,
            copiedItemCount: result.copiedItemCount,
          },
        });
        return;
      }
      toast.success(`Đã sao chép ${result.copiedItemCount} món vào giỏ theo giá hiện tại`);
      navigate('/cart');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || error?.message || 'Không thể đặt lại đơn hàng');
    } finally {
      setReordering(false);
    }
  };

  const handleCancelOrder = async (
    reasonCode: CancellationReasonCode,
    detail?: string,
  ) => {
    if (!order?.id || !user?.id) return;
    setCancellationBusy(true);
    try {
      const cancelled = await cancelOrder(order.id, {
        reasonCode,
        detail,
      });
      if (!contextOrder) setFallbackOrder(cancelled as unknown as ApiOrder);
      setCancellationOpen(false);
      toast.success('Đơn hàng đã được hủy và lưu lý do');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể hủy đơn hàng');
    } finally {
      setCancellationBusy(false);
    }
  };

  const handleSubmitReview = async () => {
    if (!order?.id || !user?.id) return;
    setReviewSubmitting(true);
    try {
      const created = await createOrderReview(order.id, {
        rating: reviewRating,
        comment: reviewComment.trim() || undefined,
      });
      setReview(created);
      toast.success('Cảm ơn bạn đã đánh giá nhà hàng');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể gửi đánh giá');
    } finally {
      setReviewSubmitting(false);
    }
  };

  const handleSubstitutionResponse = async (proposal: SubstitutionProposal, accept: boolean) => {
    if (!user?.id || !id) return;
    setSubstitutionBusy(proposal.id);
    try {
      const result = await respondToSubstitution(id, proposal.id, accept);
      if (result.status === 'EXPIRED') {
        toast.error('Đề xuất đã hết thời gian phản hồi');
      } else {
        toast.success(accept ? 'Đã chấp nhận món thay thế' : 'Đã từ chối món thay thế');
      }
      await loadSubstitutions();
      setLatestOrder(await getOrder(id));
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể phản hồi đề xuất thay món');
    } finally {
      setSubstitutionBusy(null);
    }
  };

  if (loading) {
    return (
      <Page>
        <Centered>Đang tải thông tin đơn hàng...</Centered>
      </Page>
    );
  }

  if (!order || notFound) {
    return (
      <Page>
        <Centered>
          Không tìm thấy đơn hàng.
          <div style={{ marginTop: 16 }}>
            <Link to="/orders" style={{ color: 'var(--primary)' }}>Quay lại danh sách đơn hàng</Link>
          </div>
        </Centered>
      </Page>
    );
  }

  const orderItems = order.items || [];
  const calculatedSubtotal = orderItems.reduce((sum: number, item: any) => {
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
  const detailedPaymentStatus = normalizePaymentStatusValue(order.paymentStatus);
  const refundAmountLabel = detailedPaymentStatus === 'refunded'
    || detailedPaymentStatus === 'partial_refunded'
    ? 'Số tiền đã hoàn'
    : 'Số tiền chờ hoàn';
  const canRetryVNPay = order.paymentMethod?.trim().toLowerCase() === 'vnpay'
    && normalizePaymentStatus(order.paymentStatus) !== 'paid'
    && order.status === 'Pending'
    && order.userId === user?.id;
  const canCustomerCancel = order.userId === user?.id && order.status === 'Pending';
  const needsSupportToCancel = order.userId === user?.id
    && ['Confirmed', 'In Progress', 'Ready', 'Delivering'].includes(order.status);
  const canReview = order.userId === user?.id && order.status === 'Delivered';
  const canReorder = order.userId === user?.id
    && (order.status === 'Delivered' || order.status === 'Cancelled');

  const isCancelled = order.status === 'Cancelled';
  const currentStep = STATUS_TO_STEP[order.status] ?? 0;

  return (
    <Page>
      <Card>
        <HeaderRow>
          <div>
            <OrderCode>{order.id}</OrderCode>
            <OrderDate>
              {order.createdAt ? dayjs(order.createdAt).format('HH:mm - DD/MM/YYYY') : ''}
            </OrderDate>
          </div>
          <StatusBadge $bg={statusColors.bg} $color={statusColors.color}>
            {getStatusLabel(order.status)}
          </StatusBadge>
        </HeaderRow>

        <StatusOverview aria-label="Tổng quan trạng thái đơn">
          <StatusOverviewItem>
            <small>Đơn hàng</small>
            <strong>{getStatusLabel(order.status)}</strong>
          </StatusOverviewItem>
          <StatusOverviewItem>
            <small>Thanh toán</small>
            <strong>{paymentStatusInfo.label}</strong>
          </StatusOverviewItem>
          <StatusOverviewItem>
            <small>Giao hàng</small>
            <strong>{delivery
              ? DELIVERY_STATUS_LABELS[delivery.status] || 'Đang cập nhật'
              : order.status === 'Delivered' ? 'Đã giao'
              : order.status === 'Delivering' ? 'Đang giao'
              : 'Chưa bắt đầu giao'}</strong>
          </StatusOverviewItem>
        </StatusOverview>

        {isCancelled ? (
          <CancelledBanner>
            <div>
              {order.cancellationActor === 'RESTAURANT'
                ? 'Nhà hàng đã từ chối / hủy đơn'
                : order.cancellationActor === 'CUSTOMER'
                ? 'Bạn đã hủy đơn hàng'
                : 'Đơn hàng đã bị hủy'}
            </div>
            {order.cancellationReasonCode && (
              <div style={{ marginTop: 6, fontWeight: 500 }}>
                Lý do: {CANCELLATION_REASON_LABELS[order.cancellationReasonCode as CancellationReasonCode]
                  || order.cancellationReasonCode}
                {order.cancellationReasonDetail ? ` — ${order.cancellationReasonDetail}` : ''}
              </div>
            )}
            {order.paymentStatus === 'refund_pending' && (
              <div style={{ marginTop: 6, fontWeight: 500 }}>
                Khoản thanh toán đang chờ quản trị viên xử lý hoàn tiền.
              </div>
            )}
          </CancelledBanner>
        ) : (
          <Timeline>
            {STEPS.map((step, idx) => {
              const status =
                order.status === 'Delivered'
                  ? 'done'
                  : idx < currentStep
                  ? 'done'
                  : idx === currentStep
                  ? 'current'
                  : 'upcoming';
              const lineColor =
                idx < currentStep || order.status === 'Delivered'
                  ? 'var(--success)'
                  : 'var(--border)';
              return (
                <StepWrap key={step.label} $lineColor={lineColor}>
                  <StepCircle $status={status}>
                    {status === 'done' ? '✓' : step.icon}
                  </StepCircle>
                  <StepLabel $current={status === 'current'}>{step.label}</StepLabel>
                </StepWrap>
              );
            })}
          </Timeline>
        )}

        <HistoryPanel>
          <summary>Lịch sử xử lý đơn ({statusHistory.length})</summary>
          {statusHistory.length === 0 ? (
            <p style={{ color: 'var(--secondaryText)' }}>Chưa có dữ liệu lịch sử cho đơn này.</p>
          ) : statusHistory.map(entry => (
            <HistoryItem key={entry.id}>
              <strong>{getStatusLabel(entry.toStatus)}</strong>
              <div>{dayjs(entry.createdAt).format('HH:mm · DD/MM/YYYY')}</div>
              <small style={{ color: 'var(--secondaryText)' }}>
                {entry.actorRole === 'CUSTOMER' ? 'Khách hàng' :
                  entry.actorRole === 'RESTAURANT' ? 'Nhà hàng' :
                  entry.actorRole === 'ADMIN' ? 'Quản trị viên' : 'Hệ thống'}
                {entry.reason ? ` — ${friendlyHistoryReason(entry.reason)}` : ''}
              </small>
            </HistoryItem>
          ))}
        </HistoryPanel>

        <SectionTitle>Món đã đặt</SectionTitle>
        {orderItems.map((item: any, idx: number) => {
          const qty = item.qty ?? item.quantity ?? 0;
          const product = productByName[item.name];
          const imgSrc = item.productImage || product?.image || NO_IMAGE_SVG;
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
                {item.originalProductName && (
                  <small style={{ display: 'block', fontWeight: 400, color: '#b45309' }}>
                    Món thay thế cho: {item.originalProductName}
                  </small>
                )}
                {(item.selectedOptions?.length ?? 0) > 0 && (
                  <small style={{ display: 'block', fontWeight: 400, color: 'var(--secondaryText)' }}>
                    {item.selectedOptions.map((option: any) => `${option.groupName}: ${option.optionName}`).join(' · ')}
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

        {substitutions.length > 0 && (
          <>
            <SectionTitle>Đề xuất thay món</SectionTitle>
            {substitutions.map((proposal) => (
              <ReviewCard key={proposal.id} style={{ marginBottom: 12 }}>
                <strong>{proposal.originalProductName} → {proposal.replacementProductName}</strong>
                <div style={{ marginTop: 8, color: 'var(--secondaryText)' }}>
                  Giá món thay: {formatVND(proposal.replacementUnitPrice)}
                  {proposal.replacementUnitPrice < proposal.originalUnitPrice
                    ? ` · Giảm ${formatVND(proposal.originalUnitPrice - proposal.replacementUnitPrice)} mỗi phần`
                    : ' · Không đổi giá'}
                </div>
                {proposal.reason && <div style={{ marginTop: 6 }}>Lý do: {proposal.reason}</div>}
                <div style={{ marginTop: 6, fontSize: 13, color: 'var(--secondaryText)' }}>
                  Trạng thái: {{
                    PENDING_CUSTOMER: 'Chờ bạn phản hồi', ACCEPTED: 'Đã chấp nhận',
                    REJECTED: 'Đã từ chối', EXPIRED: 'Đã hết hạn', CANCELLED: 'Đã hủy',
                  }[proposal.status]}
                </div>
                {proposal.status === 'PENDING_CUSTOMER' && order.userId === user?.id && (
                  <ReceiveActions style={{ marginTop: 12 }}>
                    <ReceiveButton
                      type="button"
                      disabled={substitutionBusy === proposal.id}
                      onClick={() => handleSubstitutionResponse(proposal, true)}
                    >
                      Chấp nhận món thay
                    </ReceiveButton>
                    <ReceiveButton
                      type="button"
                      $secondary
                      disabled={substitutionBusy === proposal.id}
                      onClick={() => handleSubstitutionResponse(proposal, false)}
                    >
                      Từ chối
                    </ReceiveButton>
                  </ReceiveActions>
                )}
              </ReviewCard>
            ))}
          </>
        )}

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
        {order.paidAt && <InfoRow><span>Thanh toán lúc: </span>{dayjs(order.paidAt).format('HH:mm · DD/MM/YYYY')}</InfoRow>}
        {order.vnpayTransactionId && <InfoRow><span>Mã giao dịch VNPay: </span>{order.vnpayTransactionId}</InfoRow>}
        {order.partialRefundAmount > 0 && (
          <InfoRow><span>{refundAmountLabel}: </span>{formatVND(order.partialRefundAmount)}</InfoRow>
        )}
        {canRetryVNPay && (
          <PaymentRetryCard>
            Nếu trang VNPay báo phiên đã hết hạn hoặc đơn đã được xử lý, không tải lại
            đường dẫn cũ. Hãy tạo một phiên mới cho chính đơn hàng này.
            <div>
              <PaymentRetryButton
                type="button"
                disabled={retryingPayment}
                onClick={handleRetryVNPay}
              >
                {retryingPayment ? 'Đang tạo phiên mới...' : 'Tạo phiên VNPay mới'}
              </PaymentRetryButton>
            </div>
          </PaymentRetryCard>
        )}

        <SectionTitle>Thông tin nhận hàng</SectionTitle>
        <InfoRow><span>Người nhận: </span>{order.name}</InfoRow>
        <InfoRow><span>Điện thoại: </span>{order.phone}</InfoRow>
        <InfoRow><span>Địa chỉ: </span>{order.address}</InfoRow>
        {canCustomerCancel && (
          <CancelAction type="button" onClick={() => setCancellationOpen(true)}>
            Hủy đơn hàng
          </CancelAction>
        )}
        {needsSupportToCancel && (
          <SupportAction
            type="button"
            onClick={() => window.dispatchEvent(new CustomEvent('foodfast:open-support', {
              detail: { issueType: 'ORDER_CHANGE', orderId: order.id },
            }))}
          >
            Liên hệ hỗ trợ để thay đổi hoặc hủy đơn
          </SupportAction>
        )}
        {delivery && (
          <CourierCard>
            <strong>🛵 {delivery.courierName}</strong><br />
            Trạng thái: {DELIVERY_STATUS_LABELS[delivery.status] || 'Không xác định'}<br />
            Phương tiện: {delivery.vehicleType || 'Chưa cập nhật'}
            {delivery.licensePlate ? ` · ${delivery.licensePlate}` : ''}<br />
            Dự kiến giao: {delivery.estimatedDeliveryAt
              ? dayjs(delivery.estimatedDeliveryAt).format('HH:mm DD/MM/YYYY')
              : 'Chưa có'}
            {delivery.deliveryNote && <><br />Ghi chú: {delivery.deliveryNote}</>}
            {delivery.failureReason && <><br />Lý do thất bại: {delivery.failureReason}</>}
            {delivery.completionSource === 'CUSTOMER' && <><br />Xác nhận bởi: Khách hàng</>}
            {delivery.completionSource === 'RESTAURANT_OVERRIDE' && (
              <><br />Xác nhận bởi: Nhà hàng sau khi người giao báo đã giao</>
            )}
            {delivery.completionNote && <><br />Ghi chú xác nhận: {delivery.completionNote}</>}
            {delivery.status === 'DELIVERING' && order.userId === user?.id && (
              <ReceiveConfirmation>
                <ReceiveTitle>📦 Bạn đã nhận được đơn hàng?</ReceiveTitle>
                <ReceiveHint>
                  Chỉ xác nhận sau khi bạn đã nhận đủ món. Với đơn COD, hãy thanh toán cho người giao trước khi xác nhận.
                </ReceiveHint>
                {!confirmationOpen ? (
                  <ReceiveButton type="button" onClick={() => setConfirmationOpen(true)}>
                    Tôi đã nhận hàng
                  </ReceiveButton>
                ) : (
                  <>
                    <ReceiveHint>
                      Sau bước này đơn sẽ hoàn tất và không thể quay lại trạng thái đang giao.
                    </ReceiveHint>
                    <ReceiveActions>
                      <ReceiveButton
                        type="button"
                        disabled={confirmingReceived}
                        onClick={handleConfirmReceived}
                      >
                        {confirmingReceived ? 'Đang xác nhận...' : 'Xác nhận đã nhận đủ món'}
                      </ReceiveButton>
                      <ReceiveButton
                        type="button"
                        $secondary
                        disabled={confirmingReceived}
                        onClick={() => setConfirmationOpen(false)}
                      >
                        Quay lại
                      </ReceiveButton>
                    </ReceiveActions>
                  </>
                )}
              </ReceiveConfirmation>
            )}
          </CourierCard>
        )}

        {canReview && (
          <ReviewCard>
            <strong>⭐ Đánh giá trải nghiệm</strong>
            {review ? (
              <>
                <Stars aria-label={`${review.rating} trên 5 sao`}>
                  {[1, 2, 3, 4, 5].map(value => (
                    <span key={value} style={{ color: value <= review.rating ? '#f79009' : '#d0d5dd', fontSize: 28 }}>
                      ★
                    </span>
                  ))}
                </Stars>
                <div>{review.comment || 'Bạn không để lại nhận xét.'}</div>
              </>
            ) : (
              <>
                <div style={{ marginTop: 6, color: '#667085' }}>
                  Mỗi đơn đã giao được đánh giá một lần.
                </div>
                <Stars>
                  {[1, 2, 3, 4, 5].map(value => (
                    <StarButton
                      key={value}
                      type="button"
                      $active={value <= reviewRating}
                      aria-label={`${value} sao`}
                      onClick={() => setReviewRating(value)}
                    >
                      ★
                    </StarButton>
                  ))}
                </Stars>
                <ReviewInput
                  value={reviewComment}
                  maxLength={1000}
                  placeholder="Nhận xét về món ăn và dịch vụ (tùy chọn)"
                  onChange={event => setReviewComment(event.target.value)}
                />
                <ReceiveButton
                  type="button"
                  disabled={reviewSubmitting}
                  onClick={handleSubmitReview}
                  style={{ marginTop: 10 }}
                >
                  {reviewSubmitting ? 'Đang gửi...' : 'Gửi đánh giá'}
                </ReceiveButton>
              </>
            )}
          </ReviewCard>
        )}

        {order.userId === user?.id && user?.id && (
          <OrderComplaintPanel
            orderId={order.id}
            orderStatus={order.status}
            customerId={user.id}
            orderTotal={order.total}
          />
        )}

        {canReorder && (
          <ReorderCard>
            <strong>Đặt lại đơn này</strong>
            <div style={{ marginTop: 6, lineHeight: 1.5 }}>
              Món, giá và topping sẽ được kiểm tra lại theo thực đơn hiện tại trước khi đưa vào giỏ.
            </div>
            <ReorderButton type="button" disabled={reordering} onClick={handleReorder}>
              {reordering ? 'Đang kiểm tra thực đơn...' : '↻ Đặt lại đơn'}
            </ReorderButton>
          </ReorderCard>
        )}

        <BackLink to="/orders">← Quay lại đơn hàng của tôi</BackLink>
      </Card>
      <CancellationDialog
        open={cancellationOpen}
        actor="CUSTOMER"
        orderId={order.id}
        busy={cancellationBusy}
        onClose={() => !cancellationBusy && setCancellationOpen(false)}
        onConfirm={handleCancelOrder}
      />
    </Page>
  );
};

export default OrderDetail;
