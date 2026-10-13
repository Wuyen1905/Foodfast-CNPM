import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import {
  CancellationActor,
  CancellationReasonCode,
} from '@/api/orderApi';

type ReasonOption = {
  value: CancellationReasonCode;
  label: string;
};

const CUSTOMER_REASONS: ReasonOption[] = [
  { value: 'CUSTOMER_ORDERED_BY_MISTAKE', label: 'Tôi đặt nhầm món hoặc nhầm đơn' },
  { value: 'CUSTOMER_WRONG_ADDRESS', label: 'Tôi nhập sai địa chỉ giao hàng' },
  { value: 'CUSTOMER_CHANGED_MIND', label: 'Tôi thay đổi quyết định' },
  { value: 'CUSTOMER_WAIT_TOO_LONG', label: 'Nhà hàng xác nhận quá lâu' },
  { value: 'OTHER', label: 'Lý do khác' },
];

const RESTAURANT_REASONS: ReasonOption[] = [
  { value: 'RESTAURANT_OUT_OF_STOCK', label: 'Món đã hết' },
  { value: 'RESTAURANT_OVERLOADED', label: 'Nhà hàng đang quá tải' },
  { value: 'RESTAURANT_CLOSED', label: 'Nhà hàng sắp đóng cửa hoặc đã đóng cửa' },
  { value: 'RESTAURANT_CANNOT_DELIVER', label: 'Không thể giao tới địa chỉ này' },
  { value: 'OTHER', label: 'Lý do khác' },
];

export const CANCELLATION_REASON_LABELS: Record<CancellationReasonCode, string> = [
  ...CUSTOMER_REASONS,
  ...RESTAURANT_REASONS,
].reduce((result, option) => ({ ...result, [option.value]: option.label }), {} as Record<CancellationReasonCode, string>);

const Backdrop = styled.div`
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.55);
`;

const Dialog = styled.div`
  width: min(480px, 100%);
  padding: 24px;
  border-radius: 16px;
  background: var(--card, #fff);
  color: var(--text, #1f2937);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.28);
`;

const Title = styled.h3`
  margin: 0 0 8px;
`;

const Hint = styled.p`
  margin: 0 0 18px;
  color: var(--secondaryText, #667085);
  line-height: 1.5;
`;

const Label = styled.label`
  display: block;
  margin: 14px 0 6px;
  font-weight: 700;
`;

const Select = styled.select`
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border, #d0d5dd);
  border-radius: 9px;
  background: var(--card, #fff);
  color: inherit;
`;

const Textarea = styled.textarea`
  width: 100%;
  min-height: 90px;
  padding: 12px;
  border: 1px solid var(--border, #d0d5dd);
  border-radius: 9px;
  resize: vertical;
  font: inherit;
  background: var(--card, #fff);
  color: inherit;
`;

const ErrorText = styled.div`
  margin-top: 6px;
  color: #b42318;
  font-size: 13px;
`;

const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
`;

const Button = styled.button<{ $danger?: boolean }>`
  padding: 10px 16px;
  border: ${props => props.$danger ? 'none' : '1px solid #d0d5dd'};
  border-radius: 9px;
  background: ${props => props.$danger ? '#d92d20' : '#fff'};
  color: ${props => props.$danger ? '#fff' : '#344054'};
  font: inherit;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }
`;

type CancellationDialogProps = {
  open: boolean;
  actor: Extract<CancellationActor, 'CUSTOMER' | 'RESTAURANT'>;
  orderId?: string;
  busy?: boolean;
  onClose: () => void;
  onConfirm: (reasonCode: CancellationReasonCode, detail?: string) => Promise<void> | void;
};

const CancellationDialog: React.FC<CancellationDialogProps> = ({
  open,
  actor,
  orderId,
  busy = false,
  onClose,
  onConfirm,
}) => {
  const [reasonCode, setReasonCode] = useState<CancellationReasonCode | ''>('');
  const [detail, setDetail] = useState('');
  const [error, setError] = useState('');
  const reasons = actor === 'CUSTOMER' ? CUSTOMER_REASONS : RESTAURANT_REASONS;

  useEffect(() => {
    if (open) {
      setReasonCode('');
      setDetail('');
      setError('');
    }
  }, [open, orderId]);

  if (!open) return null;

  const submit = async () => {
    if (!reasonCode) {
      setError('Vui lòng chọn lý do');
      return;
    }
    if (reasonCode === 'OTHER' && detail.trim().length < 5) {
      setError('Vui lòng mô tả lý do khác ít nhất 5 ký tự');
      return;
    }
    setError('');
    await onConfirm(reasonCode, detail.trim() || undefined);
  };

  return (
    <Backdrop role="presentation" onMouseDown={event => {
      if (event.target === event.currentTarget && !busy) onClose();
    }}>
      <Dialog role="dialog" aria-modal="true" aria-labelledby="cancel-order-title">
        <Title id="cancel-order-title">
          {actor === 'CUSTOMER' ? 'Hủy đơn hàng' : 'Từ chối / hủy đơn'} {orderId ? `#${orderId}` : ''}
        </Title>
        <Hint>
          Lý do sẽ được lưu vào lịch sử và hiển thị cho bên còn lại. Thao tác này không thể hoàn tác.
        </Hint>

        <Label htmlFor="cancellation-reason">Lý do *</Label>
        <Select
          id="cancellation-reason"
          value={reasonCode}
          disabled={busy}
          onChange={event => {
            setReasonCode(event.target.value as CancellationReasonCode);
            setError('');
          }}
        >
          <option value="">-- Chọn lý do --</option>
          {reasons.map(reason => (
            <option key={reason.value} value={reason.value}>{reason.label}</option>
          ))}
        </Select>

        <Label htmlFor="cancellation-detail">
          Ghi chú {reasonCode === 'OTHER' ? '*' : '(tùy chọn)'}
        </Label>
        <Textarea
          id="cancellation-detail"
          value={detail}
          maxLength={500}
          disabled={busy}
          placeholder="Mô tả thêm để khách hàng/nhà hàng hiểu rõ..."
          onChange={event => {
            setDetail(event.target.value);
            setError('');
          }}
        />
        {error && <ErrorText>{error}</ErrorText>}

        <Actions>
          <Button type="button" disabled={busy} onClick={onClose}>Quay lại</Button>
          <Button type="button" $danger disabled={busy} onClick={submit}>
            {busy ? 'Đang xử lý...' : actor === 'CUSTOMER' ? 'Xác nhận hủy đơn' : 'Xác nhận từ chối / hủy'}
          </Button>
        </Actions>
      </Dialog>
    </Backdrop>
  );
};

export default CancellationDialog;
