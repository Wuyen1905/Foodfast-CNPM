import React, { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import {
  createManagedVoucher,
  getManagedVouchers,
  ManagedVoucher,
  setManagedVoucherActive,
  updateManagedVoucher,
  VoucherDiscountType,
  VoucherUpsertPayload,
} from '@/api/voucherApi';
import { getRestaurants, Restaurant } from '@/api/restaurantApi';
import { formatVND } from '@/utils/currency';

interface VoucherManagementProps {
  admin?: boolean;
  primary?: string;
}

interface VoucherForm {
  code: string;
  name: string;
  restaurantId: string;
  discountType: VoucherDiscountType;
  discountValue: string;
  minOrderAmount: string;
  maxDiscount: string;
  startsAt: string;
  endsAt: string;
  usageLimit: string;
  perUserLimit: string;
  active: boolean;
}

const toLocalInput = (value: Date | string) => {
  const date = value instanceof Date ? value : new Date(value);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

const newForm = (): VoucherForm => {
  const start = new Date();
  const end = new Date(start.getTime() + 30 * 24 * 60 * 60 * 1000);
  return {
    code: '', name: '', restaurantId: '', discountType: 'PERCENT', discountValue: '10',
    minOrderAmount: '0', maxDiscount: '50000', startsAt: toLocalInput(start),
    endsAt: toLocalInput(end), usageLimit: '0', perUserLimit: '1', active: true,
  };
};

const fieldStyle: React.CSSProperties = {
  width: '100%', padding: '10px 12px', border: '1px solid #d0d5dd', borderRadius: 8,
  fontSize: 14, boxSizing: 'border-box', background: '#fff',
};

const VoucherManagement: React.FC<VoucherManagementProps> = ({ admin = false, primary = '#ff6600' }) => {
  const [vouchers, setVouchers] = useState<ManagedVoucher[]>([]);
  const [restaurants, setRestaurants] = useState<Restaurant[]>([]);
  const [restaurantFilter, setRestaurantFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [editingCode, setEditingCode] = useState<string | null>(null);
  const [form, setForm] = useState<VoucherForm | null>(null);

  const load = async (filter = restaurantFilter) => {
    setLoading(true);
    try {
      setVouchers(await getManagedVouchers(admin && filter ? filter : undefined));
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể tải danh sách ưu đãi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
    if (admin) void getRestaurants().then(setRestaurants).catch(() => setRestaurants([]));
  }, [admin]);

  const restaurantNames = useMemo(() => new Map(
    restaurants.map((restaurant) => [restaurant.id, restaurant.name]),
  ), [restaurants]);

  const edit = (voucher: ManagedVoucher) => {
    setEditingCode(voucher.code);
    setForm({
      code: voucher.code,
      name: voucher.name,
      restaurantId: voucher.restaurantId || '',
      discountType: voucher.discountType,
      discountValue: String(voucher.discountValue),
      minOrderAmount: String(voucher.minOrderAmount),
      maxDiscount: String(voucher.maxDiscount),
      startsAt: toLocalInput(voucher.startsAt),
      endsAt: toLocalInput(voucher.endsAt),
      usageLimit: String(voucher.usageLimit),
      perUserLimit: String(voucher.perUserLimit),
      active: voucher.active,
    });
  };

  const submit = async () => {
    if (!form) return;
    if (!form.code.trim() || !form.name.trim()) {
      toast.error('Vui lòng nhập mã và tên chương trình');
      return;
    }
    const payload: VoucherUpsertPayload = {
      code: form.code.trim().toUpperCase(),
      name: form.name.trim(),
      restaurantId: admin ? (form.restaurantId || null) : undefined,
      discountType: form.discountType,
      discountValue: Number(form.discountValue),
      minOrderAmount: Number(form.minOrderAmount),
      maxDiscount: form.discountType === 'PERCENT' ? Number(form.maxDiscount) : 0,
      startsAt: new Date(form.startsAt).toISOString(),
      endsAt: new Date(form.endsAt).toISOString(),
      usageLimit: Number(form.usageLimit),
      perUserLimit: Number(form.perUserLimit),
      active: form.active,
    };
    setSaving(true);
    try {
      if (editingCode) await updateManagedVoucher(editingCode, payload);
      else await createManagedVoucher(payload);
      toast.success(editingCode ? 'Đã cập nhật mã ưu đãi' : 'Đã tạo mã ưu đãi');
      setForm(null);
      setEditingCode(null);
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể lưu mã ưu đãi');
    } finally {
      setSaving(false);
    }
  };

  const toggle = async (voucher: ManagedVoucher) => {
    try {
      await setManagedVoucherActive(voucher.code, !voucher.active);
      toast.success(voucher.active ? 'Đã tạm dừng mã ưu đãi' : 'Đã kích hoạt mã ưu đãi');
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể đổi trạng thái mã ưu đãi');
    }
  };

  const statusOf = (voucher: ManagedVoucher) => {
    const now = Date.now();
    if (!voucher.active) return { text: 'Tạm dừng', color: '#667085', bg: '#f2f4f7' };
    if (new Date(voucher.startsAt).getTime() > now) return { text: 'Sắp diễn ra', color: '#175cd3', bg: '#eff8ff' };
    if (new Date(voucher.endsAt).getTime() < now) return { text: 'Đã hết hạn', color: '#b42318', bg: '#fef3f2' };
    if (voucher.usageLimit > 0 && voucher.usedCount >= voucher.usageLimit) return { text: 'Hết lượt', color: '#b42318', bg: '#fef3f2' };
    return { text: 'Đang áp dụng', color: '#067647', bg: '#ecfdf3' };
  };

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 14, alignItems: 'center', flexWrap: 'wrap', marginBottom: 20 }}>
        <div>
          <h2 style={{ margin: 0, color: primary }}>🎟️ Khuyến mãi và mã giảm giá</h2>
          <p style={{ margin: '6px 0 0', color: '#667085' }}>Giới hạn thời gian, giá trị đơn và lượt sử dụng được kiểm tra tại backend.</p>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          {admin && (
            <select value={restaurantFilter} onChange={(event) => { setRestaurantFilter(event.target.value); void load(event.target.value); }} style={{ ...fieldStyle, minWidth: 210 }}>
              <option value="">Tất cả nhà hàng</option>
              {restaurants.map((restaurant) => <option key={restaurant.id} value={restaurant.id}>{restaurant.name}</option>)}
            </select>
          )}
          <button type="button" onClick={() => { setEditingCode(null); setForm(newForm()); }} style={{ padding: '11px 16px', border: 0, borderRadius: 9, background: primary, color: '#fff', fontWeight: 700, cursor: 'pointer' }}>+ Tạo mã</button>
        </div>
      </div>

      <div style={{ overflowX: 'auto', background: '#fff', borderRadius: 12, border: '1px solid #eaecf0' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 880 }}>
          <thead><tr style={{ background: '#f9fafb', textAlign: 'left' }}>
            {['Mã / chương trình', ...(admin ? ['Phạm vi'] : []), 'Mức giảm', 'Điều kiện', 'Đã dùng', 'Hiệu lực', 'Trạng thái', 'Hành động'].map((heading) => <th key={heading} style={{ padding: 13, fontSize: 13, color: '#475467' }}>{heading}</th>)}
          </tr></thead>
          <tbody>
            {loading ? <tr><td colSpan={admin ? 9 : 8} style={{ padding: 28, textAlign: 'center' }}>Đang tải...</td></tr>
              : vouchers.length === 0 ? <tr><td colSpan={admin ? 9 : 8} style={{ padding: 28, textAlign: 'center', color: '#667085' }}>Chưa có chương trình ưu đãi</td></tr>
                : vouchers.map((voucher) => {
                  const status = statusOf(voucher);
                  return <tr key={voucher.code} style={{ borderTop: '1px solid #eaecf0' }}>
                    <td style={{ padding: 13 }}><strong>{voucher.code}</strong><div style={{ color: '#667085', fontSize: 13 }}>{voucher.name}</div></td>
                    {admin && <td style={{ padding: 13 }}>{voucher.restaurantId ? (restaurantNames.get(voucher.restaurantId) || voucher.restaurantId) : 'Toàn hệ thống'}</td>}
                    <td style={{ padding: 13 }}>{voucher.discountType === 'PERCENT' ? `${voucher.discountValue}%${voucher.maxDiscount ? ` · tối đa ${formatVND(voucher.maxDiscount)}` : ''}` : formatVND(voucher.discountValue)}</td>
                    <td style={{ padding: 13 }}>Đơn từ {formatVND(voucher.minOrderAmount)}<div style={{ color: '#667085', fontSize: 12 }}>Mỗi khách {voucher.perUserLimit} lượt</div></td>
                    <td style={{ padding: 13 }}>{voucher.usedCount}/{voucher.usageLimit || '∞'}</td>
                    <td style={{ padding: 13, fontSize: 13 }}>{new Date(voucher.startsAt).toLocaleDateString('vi-VN')}<br />đến {new Date(voucher.endsAt).toLocaleDateString('vi-VN')}</td>
                    <td style={{ padding: 13 }}><span style={{ padding: '5px 9px', borderRadius: 999, color: status.color, background: status.bg, fontSize: 12, fontWeight: 700 }}>{status.text}</span></td>
                    <td style={{ padding: 13, whiteSpace: 'nowrap' }}>
                      <button type="button" onClick={() => edit(voucher)} style={{ border: 0, background: 'none', color: primary, cursor: 'pointer', marginRight: 8 }}>Sửa</button>
                      <button type="button" onClick={() => void toggle(voucher)} style={{ border: 0, background: 'none', color: voucher.active ? '#b42318' : '#067647', cursor: 'pointer' }}>{voucher.active ? 'Tạm dừng' : 'Kích hoạt'}</button>
                    </td>
                  </tr>;
                })}
          </tbody>
        </table>
      </div>

      {form && (
        <div style={{ position: 'fixed', inset: 0, zIndex: 1500, background: 'rgba(16,24,40,.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 18 }}>
          <div style={{ width: 'min(760px, 96vw)', maxHeight: '92vh', overflowY: 'auto', background: '#fff', borderRadius: 15, padding: 24 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 18 }}><h3 style={{ margin: 0 }}>{editingCode ? `Sửa mã ${editingCode}` : 'Tạo mã ưu đãi'}</h3><button type="button" onClick={() => setForm(null)} style={{ border: 0, background: 'none', fontSize: 26, cursor: 'pointer' }}>×</button></div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 14 }}>
              <label>Mã ưu đãi<input style={fieldStyle} disabled={!!editingCode} value={form.code} maxLength={40} onChange={(event) => setForm({ ...form, code: event.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '') })} /></label>
              <label>Tên chương trình<input style={fieldStyle} value={form.name} maxLength={120} onChange={(event) => setForm({ ...form, name: event.target.value })} /></label>
              {admin && <label>Phạm vi<select style={fieldStyle} value={form.restaurantId} onChange={(event) => setForm({ ...form, restaurantId: event.target.value })}><option value="">Toàn hệ thống</option>{restaurants.map((restaurant) => <option key={restaurant.id} value={restaurant.id}>{restaurant.name}</option>)}</select></label>}
              <label>Loại giảm<select style={fieldStyle} value={form.discountType} onChange={(event) => setForm({ ...form, discountType: event.target.value as VoucherDiscountType })}><option value="PERCENT">Theo phần trăm</option><option value="FIXED">Số tiền cố định</option></select></label>
              <label>{form.discountType === 'PERCENT' ? 'Phần trăm giảm' : 'Số tiền giảm'}<input style={fieldStyle} type="number" min={1} max={form.discountType === 'PERCENT' ? 100 : undefined} value={form.discountValue} onChange={(event) => setForm({ ...form, discountValue: event.target.value })} /></label>
              {form.discountType === 'PERCENT' && <label>Giảm tối đa<input style={fieldStyle} type="number" min={0} step={1000} value={form.maxDiscount} onChange={(event) => setForm({ ...form, maxDiscount: event.target.value })} /></label>}
              <label>Giá trị đơn tối thiểu<input style={fieldStyle} type="number" min={0} step={1000} value={form.minOrderAmount} onChange={(event) => setForm({ ...form, minOrderAmount: event.target.value })} /></label>
              <label>Bắt đầu<input style={fieldStyle} type="datetime-local" value={form.startsAt} onChange={(event) => setForm({ ...form, startsAt: event.target.value })} /></label>
              <label>Kết thúc<input style={fieldStyle} type="datetime-local" value={form.endsAt} onChange={(event) => setForm({ ...form, endsAt: event.target.value })} /></label>
              <label>Tổng lượt dùng (0 = không giới hạn)<input style={fieldStyle} type="number" min={0} value={form.usageLimit} onChange={(event) => setForm({ ...form, usageLimit: event.target.value })} /></label>
              <label>Lượt dùng mỗi khách<input style={fieldStyle} type="number" min={1} value={form.perUserLimit} onChange={(event) => setForm({ ...form, perUserLimit: event.target.value })} /></label>
              <label style={{ alignSelf: 'end', paddingBottom: 10 }}><input type="checkbox" checked={form.active} onChange={(event) => setForm({ ...form, active: event.target.checked })} /> Kích hoạt ngay</label>
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 22 }}><button type="button" onClick={() => setForm(null)} style={{ padding: '10px 18px', border: '1px solid #d0d5dd', background: '#fff', borderRadius: 8, cursor: 'pointer' }}>Hủy</button><button type="button" disabled={saving} onClick={() => void submit()} style={{ padding: '10px 18px', border: 0, background: primary, color: '#fff', borderRadius: 8, fontWeight: 700, cursor: 'pointer' }}>{saving ? 'Đang lưu...' : 'Lưu mã'}</button></div>
          </div>
        </div>
      )}
    </div>
  );
};

export default VoucherManagement;
