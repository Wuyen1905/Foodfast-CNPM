import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  createCourier,
  deactivateCourier,
  listCouriers,
  RestaurantCourier,
} from '@/api/deliveryApi';

interface Theme {
  primary: string;
  secondary: string;
  accent: string;
}

interface CourierManagementProps {
  restaurantId: string;
  theme: Theme;
}

const Container = styled.section`
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
`;

const Header = styled.div`
  margin-bottom: 20px;

  h2 { margin: 0 0 8px; }
  p { margin: 0; color: #666; }
`;

const Form = styled.form`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  padding: 16px;
  margin-bottom: 20px;
  background: #f8f9fa;
  border-radius: 12px;
`;

const Input = styled.input`
  padding: 11px 12px;
  border: 1px solid #d8dde3;
  border-radius: 8px;
  font: inherit;
`;

const AddButton = styled.button<{ $primary: string }>`
  padding: 11px 18px;
  border: 0;
  border-radius: 8px;
  background: ${props => props.$primary};
  color: white;
  font-weight: 600;
  cursor: pointer;
`;

const CourierGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 14px;
`;

const CourierCard = styled.article<{ $active: boolean; $accent: string }>`
  padding: 16px;
  border-radius: 12px;
  border: 1px solid ${props => props.$accent};
  opacity: ${props => props.$active ? 1 : 0.62};

  h3 { margin: 0 0 8px; }
  p { margin: 5px 0; color: #555; }
`;

const StatusRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
`;

const Status = styled.span<{ $active: boolean }>`
  color: ${props => props.$active ? '#16794b' : '#777'};
  font-weight: 600;
`;

const DeactivateButton = styled.button`
  border: 1px solid #dc3545;
  border-radius: 7px;
  padding: 7px 10px;
  background: white;
  color: #dc3545;
  cursor: pointer;
`;

const Empty = styled.div`
  padding: 28px;
  text-align: center;
  color: #777;
`;

const initialForm = {
  name: '',
  phone: '',
  vehicleType: 'Xe máy',
  licensePlate: '',
};

const CourierManagement: React.FC<CourierManagementProps> = ({ restaurantId, theme }) => {
  const [couriers, setCouriers] = useState<RestaurantCourier[]>([]);
  const [form, setForm] = useState(initialForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setCouriers(await listCouriers(restaurantId));
    } catch (error) {
      console.error(error);
      toast.error('Không thể tải danh sách nhân viên giao hàng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [restaurantId]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!form.name.trim() || !form.phone.trim()) {
      toast.error('Vui lòng nhập tên và số điện thoại');
      return;
    }
    setSaving(true);
    try {
      await createCourier(restaurantId, {
        name: form.name.trim(),
        phone: form.phone.trim(),
        vehicleType: form.vehicleType.trim(),
        licensePlate: form.licensePlate.trim(),
      });
      setForm(initialForm);
      await load();
      toast.success('Đã thêm nhân viên giao hàng');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể thêm nhân viên');
    } finally {
      setSaving(false);
    }
  };

  const handleDeactivate = async (courier: RestaurantCourier) => {
    if (!window.confirm(`Ngừng hoạt động nhân viên ${courier.name}?`)) return;
    try {
      await deactivateCourier(courier.id);
      await load();
      toast.success('Đã cập nhật trạng thái nhân viên');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể ngừng hoạt động nhân viên');
    }
  };

  return (
    <Container>
      <Header>
        <h2 style={{ color: theme.primary }}>🛵 Nhân viên giao hàng</h2>
        <p>Nhân viên do nhà hàng quản lý, không cần tài khoản đăng nhập riêng.</p>
      </Header>

      <Form onSubmit={handleSubmit}>
        <Input
          aria-label="Tên nhân viên"
          placeholder="Họ tên *"
          value={form.name}
          onChange={event => setForm({ ...form, name: event.target.value })}
        />
        <Input
          aria-label="Số điện thoại"
          placeholder="Số điện thoại *"
          value={form.phone}
          onChange={event => setForm({ ...form, phone: event.target.value })}
        />
        <Input
          aria-label="Loại phương tiện"
          placeholder="Phương tiện"
          value={form.vehicleType}
          onChange={event => setForm({ ...form, vehicleType: event.target.value })}
        />
        <Input
          aria-label="Biển số xe"
          placeholder="Biển số xe"
          value={form.licensePlate}
          onChange={event => setForm({ ...form, licensePlate: event.target.value })}
        />
        <AddButton $primary={theme.primary} disabled={saving}>
          {saving ? 'Đang lưu...' : 'Thêm nhân viên'}
        </AddButton>
      </Form>

      {loading ? (
        <Empty>Đang tải danh sách...</Empty>
      ) : couriers.length === 0 ? (
        <Empty>Nhà hàng chưa có nhân viên giao hàng.</Empty>
      ) : (
        <CourierGrid>
          {couriers.map(courier => (
            <CourierCard key={courier.id} $active={courier.active} $accent={theme.accent}>
              <h3>{courier.name}</h3>
              <p>📞 {courier.phone}</p>
              <p>🛵 {courier.vehicleType || 'Chưa cập nhật phương tiện'}</p>
              <p>🔖 {courier.licensePlate || 'Chưa cập nhật biển số'}</p>
              <StatusRow>
                <Status $active={courier.active}>
                  {courier.active ? 'Đang hoạt động' : 'Đã ngừng hoạt động'}
                </Status>
                {courier.active && (
                  <DeactivateButton onClick={() => handleDeactivate(courier)}>
                    Ngừng hoạt động
                  </DeactivateButton>
                )}
              </StatusRow>
            </CourierCard>
          ))}
        </CourierGrid>
      )}
    </Container>
  );
};

export default CourierManagement;
