import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import toast from 'react-hot-toast';
import {
  getRestaurant,
  Restaurant,
  updateRestaurantOperations,
} from '@/api/restaurantApi';

const Card = styled.section`
  padding: 24px;
  border: 1px solid #e4e7ec;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 4px 14px rgba(16, 24, 40, 0.08);
`;

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
`;

const Status = styled.div<{ $open: boolean }>`
  padding: 8px 12px;
  border-radius: 999px;
  background: ${props => props.$open ? '#dcfae6' : '#fee4e2'};
  color: ${props => props.$open ? '#067647' : '#b42318'};
  font-weight: 800;
`;

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 22px;

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: #344054;
  font-weight: 700;
`;

const Input = styled.input`
  padding: 11px 12px;
  border: 1px solid #d0d5dd;
  border-radius: 9px;
  font: inherit;
`;

const Textarea = styled.textarea`
  width: 100%;
  min-height: 82px;
  margin-top: 8px;
  padding: 11px 12px;
  border: 1px solid #d0d5dd;
  border-radius: 9px;
  resize: vertical;
  font: inherit;
`;

const Actions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
`;

const Button = styled.button<{ $danger?: boolean; $primary?: string }>`
  padding: 10px 16px;
  border: none;
  border-radius: 9px;
  background: ${props => props.$danger ? '#d92d20' : props.$primary || '#ff6600'};
  color: #fff;
  font: inherit;
  font-weight: 800;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

type Props = {
  restaurantId: string;
  primary?: string;
};

const RestaurantOperations: React.FC<Props> = ({ restaurantId, primary }) => {
  const [restaurant, setRestaurant] = useState<Restaurant | null>(null);
  const [openingTime, setOpeningTime] = useState('08:00');
  const [closingTime, setClosingTime] = useState('22:00');
  const [pauseReason, setPauseReason] = useState('');
  const [busy, setBusy] = useState(false);

  const applyRestaurant = (value: Restaurant) => {
    setRestaurant(value);
    setOpeningTime(value.openingTime || '08:00');
    setClosingTime(value.closingTime || '22:00');
    setPauseReason(value.pauseReason || '');
  };

  useEffect(() => {
    getRestaurant(restaurantId)
      .then(applyRestaurant)
      .catch((error: any) => {
        console.error(error);
        toast.error('Không thể tải trạng thái hoạt động của nhà hàng');
      });
  }, [restaurantId]);

  const saveHours = async () => {
    setBusy(true);
    try {
      const updated = await updateRestaurantOperations(restaurantId, {
        openingTime,
        closingTime,
      });
      applyRestaurant(updated);
      toast.success('Đã cập nhật giờ hoạt động');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể cập nhật giờ hoạt động');
    } finally {
      setBusy(false);
    }
  };

  const toggleAcceptingOrders = async () => {
    if (!restaurant) return;
    const nextAccepting = !restaurant.acceptingOrders;
    if (!nextAccepting && pauseReason.trim().length < 5) {
      toast.error('Vui lòng nhập lý do tạm ngừng ít nhất 5 ký tự');
      return;
    }
    setBusy(true);
    try {
      const updated = await updateRestaurantOperations(restaurantId, {
        acceptingOrders: nextAccepting,
        pauseReason: nextAccepting ? undefined : pauseReason.trim(),
      });
      applyRestaurant(updated);
      toast.success(nextAccepting ? 'Nhà hàng đã nhận đơn trở lại' : 'Nhà hàng đã tạm ngừng nhận đơn');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể cập nhật trạng thái nhận đơn');
    } finally {
      setBusy(false);
    }
  };

  if (!restaurant) return <Card>Đang tải trạng thái vận hành...</Card>;

  const availableNow = restaurant.isActive && restaurant.acceptingOrders && restaurant.openNow;
  const statusLabel = !restaurant.isActive
    ? 'Ngừng hoạt động'
    : !restaurant.acceptingOrders
    ? 'Tạm ngừng nhận đơn'
    : restaurant.openNow
    ? 'Đang mở cửa và nhận đơn'
    : 'Ngoài giờ phục vụ';

  return (
    <Card>
      <Header>
        <div>
          <h2 style={{ margin: '0 0 8px' }}>Trạng thái vận hành</h2>
          <div style={{ color: '#667085', lineHeight: 1.5 }}>
            Khách hàng chỉ có thể đặt món khi nhà hàng đang hoạt động, trong giờ mở cửa và đang nhận đơn.
          </div>
        </div>
        <Status $open={availableNow}>{statusLabel}</Status>
      </Header>

      <Grid>
        <Field>
          Giờ mở cửa
          <Input type="time" value={openingTime} disabled={busy} onChange={e => setOpeningTime(e.target.value)} />
        </Field>
        <Field>
          Giờ đóng cửa
          <Input type="time" value={closingTime} disabled={busy} onChange={e => setClosingTime(e.target.value)} />
        </Field>
      </Grid>

      {!restaurant.acceptingOrders && restaurant.pauseReason && (
        <div style={{ marginTop: 16, padding: 12, borderRadius: 9, background: '#fff4e5', color: '#93370d' }}>
          Lý do đang hiển thị cho khách: {restaurant.pauseReason}
        </div>
      )}

      {restaurant.acceptingOrders && (
        <Field style={{ marginTop: 16 }}>
          Lý do nếu cần tạm ngừng nhận đơn
          <Textarea
            value={pauseReason}
            maxLength={300}
            disabled={busy}
            placeholder="Ví dụ: Bếp đang quá tải, tạm hết nguyên liệu..."
            onChange={e => setPauseReason(e.target.value)}
          />
        </Field>
      )}

      <Actions>
        <Button type="button" $primary={primary} disabled={busy} onClick={saveHours}>
          Lưu giờ hoạt động
        </Button>
        <Button
          type="button"
          $primary={primary}
          $danger={restaurant.acceptingOrders}
          disabled={busy}
          onClick={toggleAcceptingOrders}
        >
          {restaurant.acceptingOrders ? 'Tạm ngừng nhận đơn' : 'Nhận đơn trở lại'}
        </Button>
      </Actions>
    </Card>
  );
};

export default RestaurantOperations;
