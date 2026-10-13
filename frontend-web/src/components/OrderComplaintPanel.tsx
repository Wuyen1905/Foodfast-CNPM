import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import dayjs from 'dayjs';
import {
  COMPLAINT_CATEGORY_LABELS,
  COMPLAINT_STATUS_LABELS,
  Complaint,
  ComplaintCategory,
  createComplaint,
  listComplaints,
  openComplaintEvidence,
  uploadComplaintEvidence,
} from '@/api/complaintApi';
import { formatVND } from '@/utils/currency';

interface Props {
  orderId: string;
  orderStatus: string;
  customerId: string;
  orderTotal: number;
}

const box: React.CSSProperties = {
  marginTop: 22, padding: 18, border: '1px solid var(--border)', borderRadius: 12,
  background: 'var(--background)', color: 'var(--text)',
};
const input: React.CSSProperties = {
  width: '100%', boxSizing: 'border-box', padding: '10px 12px', marginTop: 8,
  border: '1px solid var(--border)', borderRadius: 8, background: 'var(--card)', color: 'var(--text)',
};

const OrderComplaintPanel: React.FC<Props> = ({ orderId, orderStatus, customerId, orderTotal }) => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [category, setCategory] = useState<ComplaintCategory>('MISSING_ITEM');
  const [description, setDescription] = useState('');
  const [evidenceFiles, setEvidenceFiles] = useState<File[]>([]);
  const [evidencePreviews, setEvidencePreviews] = useState<string[]>([]);
  const [refundMode, setRefundMode] = useState<'NONE' | 'PARTIAL' | 'FULL'>('NONE');
  const [refund, setRefund] = useState(0);
  const [busy, setBusy] = useState(false);
  const eligible = orderStatus === 'Delivering' || orderStatus === 'Delivered';

  const load = async () => {
    if (!customerId) return;
    try {
      const all = await listComplaints();
      setComplaints(all.filter(item => item.orderId === orderId));
    } catch (error) {
      console.error('Không thể tải khiếu nại', error);
    }
  };

  useEffect(() => { void load(); }, [customerId, orderId]);
  useEffect(() => {
    const previews = evidenceFiles.map(file => URL.createObjectURL(file));
    setEvidencePreviews(previews);
    return () => previews.forEach(preview => URL.revokeObjectURL(preview));
  }, [evidenceFiles]);
  const requestedRefundAmount = refundMode === 'FULL'
    ? orderTotal
    : refundMode === 'PARTIAL' ? refund : 0;

  const evidenceRequired = category === 'WRONG_ITEM' || category === 'DAMAGED_ITEM';

  const selectEvidence = (files: FileList | null) => {
    const selected = Array.from(files || []);
    const combined = [...evidenceFiles, ...selected].slice(0, 5);
    const invalid = combined.find(file =>
      !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024
    );
    if (invalid) {
      toast.error('Chỉ chọn ảnh JPG, PNG hoặc WEBP, tối đa 5 MB mỗi ảnh');
      return;
    }
    setEvidenceFiles(combined);
  };

  const submit = async () => {
    if (evidenceRequired && evidenceFiles.length === 0) {
      toast.error('Vui lòng chọn ít nhất một ảnh bằng chứng');
      return;
    }
    if (refundMode === 'PARTIAL' && (refund <= 0 || refund > orderTotal)) {
      toast.error('Số tiền đề nghị hoàn phải lớn hơn 0 và không vượt quá tổng đơn');
      return;
    }
    setBusy(true);
    try {
      const evidenceUrls = await Promise.all(evidenceFiles.map(uploadComplaintEvidence));
      await createComplaint({
        orderId,
        category,
        description: description.trim(),
        evidenceUrls,
        requestedRefundAmount,
      });
      toast.success('Đã gửi khiếu nại tới bộ phận xử lý');
      setDescription('');
      setEvidenceFiles([]);
      setRefundMode('NONE');
      setRefund(0);
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể gửi khiếu nại');
    } finally {
      setBusy(false);
    }
  };

  if (!eligible && complaints.length === 0) return null;
  return (
    <section style={box}>
      <strong>🛟 Khiếu nại và yêu cầu hoàn tiền</strong>
      <div style={{ marginTop: 6, color: 'var(--secondaryText)', fontSize: 14 }}>
        Mỗi đơn chỉ tạo một hồ sơ khiếu nại. Mọi quyết định đều được lưu trong lịch sử.
      </div>

      {complaints.map(item => (
        <div key={item.id} style={{ marginTop: 14, padding: 12, border: '1px solid var(--border)', borderRadius: 8 }}>
          <b>{item.id} · {COMPLAINT_CATEGORY_LABELS[item.category]}</b>
          <div>Trạng thái: {COMPLAINT_STATUS_LABELS[item.status]}</div>
          <div>
            {item.requestedRefundAmount > 0
              ? `Đề nghị hoàn: ${formatVND(item.requestedRefundAmount)}`
              : 'Khách chỉ yêu cầu kiểm tra và phản hồi'}
          </div>
          {item.evidenceUrls.length > 0 && (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8 }}>
              {item.evidenceUrls.map((url, index) => (
                <button key={url} type="button" onClick={() => void openComplaintEvidence(url)}>
                  Xem ảnh {index + 1}
                </button>
              ))}
            </div>
          )}
          {item.approvedRefundAmount > 0 && <div>Được duyệt: {formatVND(item.approvedRefundAmount)}</div>}
          {item.resolution && <div>Kết quả: {item.resolution}</div>}
          <details style={{ marginTop: 8 }}>
            <summary>Lịch sử xử lý ({item.history.length})</summary>
            {item.history.map(history => (
              <div key={history.id} style={{ marginTop: 6, fontSize: 13 }}>
                {dayjs(history.createdAt).format('HH:mm DD/MM/YYYY')} · {history.action}
                {history.note ? ` — ${history.note}` : ''}
              </div>
            ))}
          </details>
        </div>
      ))}

      {complaints.length > 0 && (
        <div style={{
          marginTop: 14,
          padding: '12px 14px',
          border: '1px solid #bfdbfe',
          borderRadius: 9,
          background: '#eff6ff',
          color: '#1e3a8a',
          fontSize: 14,
          lineHeight: 1.5,
        }}>
          Hồ sơ khiếu nại của đơn này đã được ghi nhận nên không thể tạo thêm hồ sơ mới.
          Nếu chưa đồng ý với kết quả, bạn có thể mở Trợ lý FoodFast để yêu cầu chuyển tiếp hỗ trợ.
        </div>
      )}

      {eligible && complaints.length === 0 && (
        <div style={{ marginTop: 14 }}>
          <select value={category} onChange={event => setCategory(event.target.value as ComplaintCategory)} style={input}>
            {Object.entries(COMPLAINT_CATEGORY_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <textarea
            value={description}
            onChange={event => setDescription(event.target.value)}
            placeholder="Mô tả cụ thể vấn đề (ít nhất 10 ký tự)"
            maxLength={1200}
            rows={3}
            style={input}
          />
          <div style={{ marginTop: 12 }}>
            <label style={{ display: 'block', fontWeight: 700 }}>
              Ảnh bằng chứng {evidenceRequired ? '(bắt buộc)' : '(không bắt buộc)'}
            </label>
            <div style={{ color: 'var(--secondaryText)', fontSize: 13, marginTop: 4 }}>
              Chọn tối đa 5 ảnh JPG, PNG hoặc WEBP; mỗi ảnh không quá 5 MB.
            </div>
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={event => { selectEvidence(event.target.files); event.target.value = ''; }}
              style={input}
            />
            {evidenceFiles.length > 0 && (
              <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', marginTop: 10 }}>
                {evidenceFiles.map((file, index) => (
                  <div key={`${file.name}-${file.lastModified}`} style={{ width: 92 }}>
                    <img src={evidencePreviews[index]} alt={`Bằng chứng ${index + 1}`} style={{ width: 92, height: 72, objectFit: 'cover', borderRadius: 8 }} />
                    <button type="button" onClick={() => setEvidenceFiles(files => files.filter((_, position) => position !== index))} style={{ width: '100%', marginTop: 4 }}>
                      Xóa
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          <label style={{ display: 'block', marginTop: 14, fontWeight: 700 }}>
            Mong muốn xử lý
            <select value={refundMode} onChange={event => setRefundMode(event.target.value as typeof refundMode)} style={input}>
              <option value="NONE">Kiểm tra và phản hồi, chưa yêu cầu hoàn tiền</option>
              <option value="PARTIAL">Đề nghị hoàn một phần</option>
              <option value="FULL">Đề nghị hoàn toàn bộ đơn ({formatVND(orderTotal)})</option>
            </select>
          </label>
          {refundMode === 'PARTIAL' && (
            <label style={{ display: 'block', marginTop: 12, fontWeight: 700 }}>
              Số tiền đề nghị hoàn (tối đa {formatVND(orderTotal)})
              <input
                type="number" min={1000} max={orderTotal} step={1000} value={refund || ''}
                onChange={event => setRefund(Number(event.target.value))}
                placeholder="Ví dụ: 25.000"
                style={input}
              />
            </label>
          )}
          <button
            type="button" disabled={busy || description.trim().length < 10}
            onClick={submit}
            style={{ marginTop: 10, padding: '10px 16px', border: 0, borderRadius: 8, background: 'var(--primary)', color: '#fff', fontWeight: 700 }}
          >
            {busy ? 'Đang gửi...' : 'Gửi khiếu nại'}
          </button>
        </div>
      )}
    </section>
  );
};

export default OrderComplaintPanel;
