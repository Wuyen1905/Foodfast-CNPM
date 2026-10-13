import React, { useCallback, useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import { cancelOrder, getOrders, type Order } from '@/api/orderApi';
import { formatVND } from '@/utils/currency';

const Panel = styled.section`
  background: #fff;
  border-radius: 15px;
  padding: 24px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
`;

const Toolbar = styled.div`
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 175px 175px 150px auto;
  gap: 12px;
  margin-bottom: 20px;
  @media (max-width: 1000px) { grid-template-columns: 1fr 1fr; }
  @media (max-width: 620px) { grid-template-columns: 1fr; }
`;

const Control = styled.input`
  border: 1px solid #dfe3ea;
  border-radius: 9px;
  padding: 11px 13px;
  font: inherit;
  &:focus { outline: 2px solid #667eea33; border-color: #667eea; }
`;

const Select = styled.select`
  border: 1px solid #dfe3ea;
  border-radius: 9px;
  padding: 11px 13px;
  background: #fff;
  font: inherit;
`;

const Button = styled.button<{ $danger?: boolean; $secondary?: boolean }>`
  border: 0;
  border-radius: 9px;
  padding: 10px 16px;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
  background: ${({ $danger, $secondary }) => $danger ? '#dc3545' : $secondary ? '#eef1f6' : '#667eea'};
  color: ${({ $secondary }) => $secondary ? '#333' : '#fff'};
  &:disabled { opacity: .5; cursor: not-allowed; }
`;

const TableWrap = styled.div`overflow-x: auto;`;
const Table = styled.table`
  width: 100%;
  border-collapse: collapse;
  th, td { text-align: left; padding: 14px 12px; border-bottom: 1px solid #edf0f4; vertical-align: top; }
  th { color: #59606d; font-size: 13px; background: #f8f9fb; }
  tbody tr:hover { background: #fafbff; }
`;

const Badge = styled.span<{ $tone: string }>`
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  background: ${({ $tone }) => ({
    pending: '#fff3cd', confirmed: '#d9f2ff', preparing: '#ffe4cc', ready: '#e3defd',
    delivering: '#e6dcff', delivered: '#d9f4df', cancelled: '#f9d7db'
  } as Record<string, string>)[$tone] || '#eceff3'};
  color: #30343b;
`;

const Overlay = styled.div`
  position: fixed;
  inset: 0;
  z-index: 1100;
  background: rgba(15, 23, 42, .55);
  display: grid;
  place-items: center;
  padding: 20px;
`;

const Dialog = styled.div`
  width: min(760px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 16px;
  padding: 26px;
  box-shadow: 0 18px 55px rgba(0, 0, 0, .25);
`;

const DetailGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 18px 0;
  @media (max-width: 620px) { grid-template-columns: 1fr; }
`;

const ItemList = styled.div`
  border: 1px solid #e7e9ef;
  border-radius: 10px;
  overflow: hidden;
  margin: 16px 0;
  > div { display: flex; justify-content: space-between; gap: 16px; padding: 12px; border-bottom: 1px solid #edf0f4; }
  > div:last-child { border-bottom: 0; }
`;

const Reason = styled.textarea`
  width: 100%;
  min-height: 100px;
  padding: 12px;
  border: 2px solid #dfe3ea;
  border-radius: 9px;
  resize: vertical;
  font: inherit;
`;

const Actions = styled.div`display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; flex-wrap: wrap;`;
const Empty = styled.div`text-align: center; padding: 50px 20px; color: #777;`;

const normalizeStatus = (status?: string) => {
  const normalized = (status || 'PENDING').toLowerCase().replace(/ /g, '_');
  return normalized === 'in_progress' ? 'preparing' : normalized;
};
const statusLabel: Record<string, string> = {
  pending: 'Chờ xác nhận', confirmed: 'Đã xác nhận', preparing: 'Đang chuẩn bị',
  ready: 'Sẵn sàng giao', delivering: 'Đang giao', delivered: 'Đã giao', cancelled: 'Đã hủy'
};

const paymentLabel = (status?: string) => ({
  PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán', PENDING: 'Đang chờ',
  REFUND_PENDING: 'Chờ hoàn tiền', REFUNDED: 'Đã hoàn tiền', FAILED: 'Thất bại'
}[String(status || '').toUpperCase()] || status || 'Chưa xác định');

const orderTimestamp = (value?: number) => value ? new Date(value).toLocaleString('vi-VN') : 'Chưa có';

interface AdminOrdersProps {
  initialDateFilter?: 'today';
  initialOrderIds?: string[];
}

const AdminOrders: React.FC<AdminOrdersProps> = ({ initialDateFilter, initialOrderIds = [] }) => {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [paymentFilter, setPaymentFilter] = useState('all');
  const [dateFilter, setDateFilter] = useState<'all' | 'today'>(initialDateFilter || 'all');
  const [selected, setSelected] = useState<Order | null>(null);
  const [cancelMode, setCancelMode] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getOrders();
      setOrders(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error(error);
      toast.error('Không thể tải danh sách đơn hàng');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);
  useEffect(() => { setDateFilter(initialDateFilter || 'all'); }, [initialDateFilter]);

  const filtered = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return orders.filter(order => {
      const text = [order.id, order.customerName, order.name, order.customerPhone, order.phone,
        order.restaurantId, order.restaurant, order.address].filter(Boolean).join(' ').toLowerCase();
      return (!keyword || text.includes(keyword))
        && (!initialOrderIds.length || initialOrderIds.includes(order.id))
        && (statusFilter === 'all' || normalizeStatus(order.status) === statusFilter)
        && (paymentFilter === 'all' || String(order.paymentStatus || '').toUpperCase() === paymentFilter)
        && (dateFilter === 'all' || new Date(order.createdAt || 0).toLocaleDateString('vi-VN') === new Date().toLocaleDateString('vi-VN'));
    });
  }, [orders, query, statusFilter, paymentFilter, dateFilter, initialOrderIds]);

  const closeDialog = () => {
    setSelected(null);
    setCancelMode(false);
    setCancelReason('');
  };

  const submitCancellation = async () => {
    if (!selected || cancelReason.trim().length < 5) return;
    setSubmitting(true);
    try {
      const updated = await cancelOrder(selected.id, { reasonCode: 'OTHER', detail: cancelReason.trim() });
      setOrders(current => current.map(order => order.id === updated.id ? updated : order));
      toast.success('Đã hủy đơn theo quy trình ngoại lệ và lưu nhật ký');
      closeDialog();
    } catch (error: any) {
      toast.error(error?.response?.data?.error || error?.response?.data?.message || 'Không thể hủy đơn');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Panel>
      <Toolbar>
        <Control aria-label="Tìm đơn hàng" placeholder="Tìm theo mã đơn, khách hàng, SĐT, nhà hàng..." value={query} onChange={event => setQuery(event.target.value)} />
        <Select aria-label="Lọc trạng thái đơn" value={statusFilter} onChange={event => setStatusFilter(event.target.value)}>
          <option value="all">Tất cả trạng thái</option>
          {Object.entries(statusLabel).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </Select>
        <Select aria-label="Lọc trạng thái thanh toán" value={paymentFilter} onChange={event => setPaymentFilter(event.target.value)}>
          <option value="all">Tất cả thanh toán</option>
          <option value="PAID">Đã thanh toán</option><option value="UNPAID">Chưa thanh toán</option>
          <option value="PENDING">Đang chờ</option><option value="REFUND_PENDING">Chờ hoàn tiền</option><option value="REFUNDED">Đã hoàn tiền</option>
        </Select>
        <Select aria-label="Lọc thời gian" value={dateFilter} onChange={event => setDateFilter(event.target.value as 'all' | 'today')}>
          <option value="all">Tất cả thời gian</option><option value="today">Hôm nay</option>
        </Select>
        <Button onClick={() => void load()}>Làm mới</Button>
      </Toolbar>

      {loading ? <Empty>Đang tải đơn hàng...</Empty> : <TableWrap><Table>
        <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Nhà hàng</th><th>Tổng tiền</th><th>Đơn hàng</th><th>Thanh toán</th><th>Thời gian</th><th>Thao tác</th></tr></thead>
        <tbody>{filtered.length === 0 ? <tr><td colSpan={8}><Empty>Không tìm thấy đơn hàng phù hợp</Empty></td></tr> : filtered.map(order => {
          const status = normalizeStatus(order.status);
          return <tr key={order.id}>
            <td><strong>{order.id}</strong></td>
            <td>{order.customerName || order.name || 'Chưa có tên'}<br/><small>{order.customerPhone || order.phone || 'Chưa có SĐT'}</small></td>
            <td>{order.restaurant || order.restaurantId || 'Chưa xác định'}</td>
            <td><strong>{formatVND(order.total || 0)}</strong></td>
            <td><Badge $tone={status}>{statusLabel[status] || order.status}</Badge></td>
            <td>{paymentLabel(order.paymentStatus)}</td><td>{orderTimestamp(order.createdAt)}</td>
            <td><Button onClick={() => setSelected(order)}>Xem chi tiết</Button></td>
          </tr>;
        })}</tbody>
      </Table></TableWrap>}

      {selected && <Overlay role="presentation" onMouseDown={event => event.target === event.currentTarget && closeDialog()}>
        <Dialog role="dialog" aria-modal="true" aria-label={`Chi tiết đơn ${selected.id}`}>
          <h2 style={{ marginTop: 0 }}>Đơn hàng {selected.id}</h2>
          <DetailGrid>
            <div><small>Khách hàng</small><br/><strong>{selected.customerName || selected.name}</strong><br/>{selected.customerPhone || selected.phone}</div>
            <div><small>Nhà hàng</small><br/><strong>{selected.restaurant || selected.restaurantId}</strong></div>
            <div><small>Trạng thái</small><br/><Badge $tone={normalizeStatus(selected.status)}>{statusLabel[normalizeStatus(selected.status)] || selected.status}</Badge></div>
            <div><small>Thanh toán</small><br/><strong>{paymentLabel(selected.paymentStatus)}</strong> · {selected.paymentMethod || 'Chưa xác định'}</div>
            <div><small>Địa chỉ giao</small><br/>{selected.address}</div><div><small>Ngày tạo</small><br/>{orderTimestamp(selected.createdAt)}</div>
          </DetailGrid>
          <h3>Món đã đặt</h3>
          <ItemList>{(selected.items || []).map((item, index) => <div key={item.id || `${item.productId}-${index}`}>
            <span><strong>{item.productName || item.name}</strong> × {item.quantity || item.qty || 1}{item.itemNote ? <><br/><small>Ghi chú: {item.itemNote}</small></> : null}</span>
            <strong>{formatVND((item.price || 0) * (item.quantity || item.qty || 1))}</strong>
          </div>)}</ItemList>
          <div style={{ textAlign: 'right', fontSize: 20 }}><strong>Tổng cộng: {formatVND(selected.total || 0)}</strong></div>
          {selected.cancellationReasonDetail && <p><strong>Lý do hủy:</strong> {selected.cancellationReasonDetail}</p>}
          {cancelMode && <div style={{ marginTop: 20 }}>
            <label htmlFor="admin-cancel-reason"><strong>Lý do can thiệp *</strong></label>
            <p style={{ color: '#666', margin: '6px 0 10px' }}>Lý do được lưu vĩnh viễn trong nhật ký quản trị. Không thể hoàn tác thao tác hủy.</p>
            <Reason id="admin-cancel-reason" value={cancelReason} maxLength={500} onChange={event => setCancelReason(event.target.value)} placeholder="Mô tả tình huống và căn cứ xử lý (ít nhất 5 ký tự)" />
          </div>}
          <Actions>
            <Button $secondary onClick={closeDialog}>Đóng</Button>
            {!['cancelled', 'delivered'].includes(normalizeStatus(selected.status)) && !cancelMode && <Button $danger onClick={() => setCancelMode(true)}>Hủy đơn ngoại lệ</Button>}
            {cancelMode && <><Button $secondary onClick={() => { setCancelMode(false); setCancelReason(''); }}>Quay lại</Button>
              <Button $danger disabled={cancelReason.trim().length < 5 || submitting} onClick={() => void submitCancellation()}>{submitting ? 'Đang xử lý...' : 'Xác nhận hủy đơn'}</Button></>}
          </Actions>
        </Dialog>
      </Overlay>}
    </Panel>
  );
};

export default AdminOrders;
