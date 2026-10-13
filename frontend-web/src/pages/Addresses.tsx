import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import { useAuth } from '@/context';
import {
  createAddress,
  deleteAddress,
  DeliveryAddress,
  DeliveryAddressInput,
  getAddresses,
  makeDefaultAddress,
  updateAddress,
} from '@/api/addressApi';

const Page = styled.main`
  max-width: 960px;
  margin: 0 auto;
  padding: 36px 24px 64px;
  color: var(--text);
`;

const Layout = styled.div`
  display: grid;
  grid-template-columns: minmax(280px, 360px) 1fr;
  gap: 24px;
  @media (max-width: 820px) { grid-template-columns: 1fr; }
`;

const Panel = styled.section`
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 22px;
  box-shadow: var(--shadow);
`;

const Form = styled.form`
  display: grid;
  gap: 14px;
  label { display: grid; gap: 6px; font-weight: 600; }
  input, textarea {
    width: 100%; padding: 11px 12px; border: 1px solid var(--border);
    border-radius: 8px; background: var(--background); color: var(--text);
  }
`;

const Button = styled.button<{ $secondary?: boolean; $danger?: boolean }>`
  border: 0;
  border-radius: 8px;
  padding: 10px 14px;
  cursor: pointer;
  font-weight: 700;
  color: ${props => props.$secondary ? 'var(--text)' : '#fff'};
  background: ${props => props.$danger ? '#dc2626' : props.$secondary ? 'var(--border)' : 'var(--primary)'};
  &:disabled { opacity: 0.55; cursor: not-allowed; }
`;

const AddressCard = styled.article`
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  margin-bottom: 12px;
  background: var(--background);
  p { margin: 6px 0; color: var(--secondaryText); line-height: 1.45; }
`;

const Actions = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
`;

const initialInput = (name = '', phone = ''): DeliveryAddressInput => ({
  label: 'Nhà riêng',
  recipientName: name,
  phone,
  addressLine: '',
  defaultAddress: false,
});

const Addresses: React.FC = () => {
  const { user } = useAuth();
  const [items, setItems] = useState<DeliveryAddress[]>([]);
  const [form, setForm] = useState<DeliveryAddressInput>(() => initialInput(user?.name, user?.phone));
  const [editingId, setEditingId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = async () => setItems(await getAddresses());

  useEffect(() => {
    void load().catch(() => toast.error('Không thể tải sổ địa chỉ'));
  }, []);

  const reset = () => {
    setEditingId(null);
    setForm(initialInput(user?.name, user?.phone));
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setBusy(true);
    try {
      if (editingId) await updateAddress(editingId, form);
      else await createAddress(form);
      await load();
      reset();
      toast.success(editingId ? 'Đã cập nhật địa chỉ' : 'Đã thêm địa chỉ');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể lưu địa chỉ');
    } finally {
      setBusy(false);
    }
  };

  const edit = (address: DeliveryAddress) => {
    setEditingId(address.id);
    setForm({
      label: address.label,
      recipientName: address.recipientName,
      phone: address.phone,
      addressLine: address.addressLine,
      defaultAddress: address.defaultAddress,
    });
  };

  const setDefault = async (id: string) => {
    try {
      await makeDefaultAddress(id);
      await load();
      toast.success('Đã đặt làm địa chỉ mặc định');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể đổi địa chỉ mặc định');
    }
  };

  const remove = async (id: string) => {
    if (!window.confirm('Xóa địa chỉ này khỏi sổ địa chỉ?')) return;
    try {
      await deleteAddress(id);
      await load();
      if (editingId === id) reset();
      toast.success('Đã xóa địa chỉ');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể xóa địa chỉ');
    }
  };

  return (
    <Page>
      <h1>Sổ địa chỉ</h1>
      <p style={{ color: 'var(--secondaryText)' }}>Lưu địa chỉ thường dùng để chọn nhanh khi xác nhận đơn.</p>
      <Layout>
        <Panel>
          <h2>{editingId ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ'}</h2>
          <Form onSubmit={submit}>
            <label>Tên gợi nhớ
              <input value={form.label} maxLength={50} required onChange={e => setForm({ ...form, label: e.target.value })} />
            </label>
            <label>Tên người nhận
              <input value={form.recipientName} maxLength={120} required onChange={e => setForm({ ...form, recipientName: e.target.value })} />
            </label>
            <label>Số điện thoại
              <input value={form.phone} maxLength={20} required onChange={e => setForm({ ...form, phone: e.target.value })} />
            </label>
            <label>Địa chỉ đầy đủ
              <textarea
                rows={4}
                value={form.addressLine}
                required
                placeholder="Nhập tự do địa chỉ và hướng dẫn giao hàng"
                onChange={e => setForm({ ...form, addressLine: e.target.value })}
              />
            </label>
            <label style={{ display: 'flex', gridTemplateColumns: 'auto 1fr', alignItems: 'center' }}>
              <input type="checkbox" style={{ width: 18 }} checked={form.defaultAddress} onChange={e => setForm({ ...form, defaultAddress: e.target.checked })} />
              Dùng làm địa chỉ mặc định
            </label>
            <Actions>
              <Button disabled={busy}>{busy ? 'Đang lưu...' : editingId ? 'Cập nhật' : 'Thêm địa chỉ'}</Button>
              {editingId && <Button type="button" $secondary onClick={reset}>Hủy sửa</Button>}
            </Actions>
          </Form>
        </Panel>
        <Panel>
          <h2>Địa chỉ đã lưu</h2>
          {items.length === 0 ? <p>Chưa có địa chỉ nào.</p> : items.map(address => (
            <AddressCard key={address.id}>
              <strong>{address.label} {address.defaultAddress && '· Mặc định'}</strong>
              <p>{address.recipientName} · {address.phone}</p>
              <p>{address.addressLine}</p>
              <Actions>
                {!address.defaultAddress && <Button type="button" $secondary onClick={() => void setDefault(address.id)}>Đặt mặc định</Button>}
                <Button type="button" $secondary onClick={() => edit(address)}>Chỉnh sửa</Button>
                <Button type="button" $danger onClick={() => void remove(address.id)}>Xóa</Button>
              </Actions>
            </AddressCard>
          ))}
        </Panel>
      </Layout>
    </Page>
  );
};

export default Addresses;
