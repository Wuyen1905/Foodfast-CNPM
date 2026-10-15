import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import dayjs from 'dayjs';
import {
  COMPLAINT_CATEGORY_LABELS,
  COMPLAINT_STATUS_LABELS,
  Complaint,
  listComplaints,
  openComplaintEvidence,
  reviewComplaint,
} from '@/api/complaintApi';

interface Props {
  role: 'RESTAURANT' | 'ADMIN';
  viewerId: string;
}

const actionButtonBase: React.CSSProperties = {
  minHeight: 44,
  padding: '11px 16px',
  border: 0,
  borderRadius: 10,
  color: '#fff',
  fontSize: 14,
  fontWeight: 700,
  lineHeight: 1.25,
  cursor: 'pointer',
  boxShadow: '0 3px 9px rgba(15, 23, 42, 0.16)',
  transition: 'transform 120ms ease, box-shadow 120ms ease, opacity 120ms ease',
};

const actionButton = (background: string, disabled: boolean): React.CSSProperties => ({
  ...actionButtonBase,
  background,
  cursor: disabled ? 'not-allowed' : 'pointer',
  opacity: disabled ? 0.55 : 1,
  boxShadow: disabled ? 'none' : actionButtonBase.boxShadow,
});

const ComplaintInbox: React.FC<Props> = ({ role, viewerId }) => {
  const [items, setItems] = useState<Complaint[]>([]);
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState<string | null>(null);

  const load = async () => {
    if (!viewerId) return;
    try {
      setItems(await listComplaints());
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể tải báo cáo sự cố');
    }
  };
  useEffect(() => { void load(); }, [role, viewerId]);

  const review = async (item: Complaint, action: 'START_REVIEW' | 'RESOLVE' | 'REJECT') => {
    setBusy(item.id);
    try {
      await reviewComplaint(item.id, {
        action,
        note: notes[item.id],
      });
      toast.success('Đã cập nhật kết quả xử lý');
      await load();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể cập nhật báo cáo sự cố');
    } finally {
      setBusy(null);
    }
  };

  return (
    <section style={{ marginTop: 24 }}>
      <h3>🛟 Báo cáo sự cố đơn hàng ({items.length})</h3>
      {items.length === 0 && <div style={{ color: 'var(--secondaryText)' }}>Chưa có báo cáo sự cố nào.</div>}
      {items.map(item => {
        const terminal = item.status === 'RESOLVED' || item.status === 'REJECTED';
        return (
          <article key={item.id} style={{ marginTop: 12, padding: 16, background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12 }}>
            <b>{item.id} · Đơn {item.orderId}</b>
            <div style={{ marginTop: 6 }}>{COMPLAINT_CATEGORY_LABELS[item.category]} · {COMPLAINT_STATUS_LABELS[item.status]}</div>
            <div style={{ marginTop: 6 }}>{item.description}</div>
            {item.evidenceUrls.length > 0 && (
              <div style={{ marginTop: 6 }}>
                Bằng chứng: {item.evidenceUrls.map((url, index) => (
                  <button key={url} type="button" onClick={() => void openComplaintEvidence(url)} style={{ marginLeft: 6 }}>
                    Xem ảnh {index + 1}
                  </button>
                ))}
              </div>
            )}
            {!terminal && (
              <>
                <textarea
                  value={notes[item.id] || ''}
                  onChange={event => setNotes(current => ({ ...current, [item.id]: event.target.value }))}
                  placeholder="Nhập kết quả xử lý rõ ràng (ít nhất 10 ký tự)"
                  rows={2}
                  style={{ width: '100%', boxSizing: 'border-box', marginTop: 10, padding: 10, border: '1px solid var(--border)', borderRadius: 8 }}
                />
                <div style={{ marginTop: 16 }}>
                  <div style={{ marginBottom: 8, fontSize: 13, fontWeight: 700, color: 'var(--secondaryText)' }}>
                    Thao tác xử lý
                  </div>
                  <div style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
                    gap: 12,
                    alignItems: 'stretch',
                  }}>
                    {item.status === 'OPEN' && (
                      <button
                        type="button"
                        disabled={busy === item.id}
                        onClick={() => review(item, 'START_REVIEW')}
                        style={actionButton('#2563eb', busy === item.id)}
                      >
                        ▶ Bắt đầu xử lý
                      </button>
                    )}
                    <button
                      type="button"
                      disabled={busy === item.id}
                      onClick={() => review(item, 'RESOLVE')}
                      style={actionButton('#16a34a', busy === item.id)}
                    >
                      ✓ Hoàn tất xử lý
                    </button>
                    <button
                      type="button"
                      disabled={busy === item.id}
                      onClick={() => review(item, 'REJECT')}
                      style={actionButton('#dc2626', busy === item.id)}
                    >
                      ✕ Từ chối
                    </button>
                  </div>
                  {busy === item.id && (
                    <div style={{ marginTop: 8, fontSize: 13, color: 'var(--secondaryText)' }}>
                      Đang cập nhật báo cáo sự cố, vui lòng chờ...
                    </div>
                  )}
                </div>
              </>
            )}
            {item.resolution && <div style={{ marginTop: 8 }}>Kết luận: {item.resolution}</div>}
            <details style={{ marginTop: 10 }}>
              <summary>Nhật ký ({item.history.length})</summary>
              {item.history.map(history => (
                <div key={history.id} style={{ marginTop: 5, fontSize: 13 }}>
                  {dayjs(history.createdAt).format('HH:mm DD/MM/YYYY')} · {history.actorRole} · {history.action}
                  {history.note ? ` — ${history.note}` : ''}
                </div>
              ))}
            </details>
          </article>
        );
      })}
    </section>
  );
};

export default ComplaintInbox;
