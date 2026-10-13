import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import {
  getProductOptionGroupsForManagement,
  ProductOptionGroup,
  replaceProductOptionGroups,
} from '@/api/productOptionApi';
import { Product } from '@/data/products';

interface ProductOptionManagerProps {
  product: Product;
  primary: string;
  onClose: () => void;
  onSaved?: () => void;
}

const inputStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px 12px',
  border: '1px solid #d7dce2',
  borderRadius: 8,
  fontSize: 14,
  boxSizing: 'border-box',
};

const ProductOptionManager: React.FC<ProductOptionManagerProps> = ({
  product,
  primary,
  onClose,
  onSaved,
}) => {
  const [groups, setGroups] = useState<ProductOptionGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void getProductOptionGroupsForManagement(product.id)
      .then(setGroups)
      .catch((error: any) => toast.error(error?.response?.data?.message || 'Không thể tải cấu hình topping'))
      .finally(() => setLoading(false));
  }, [product.id]);

  const updateGroup = (index: number, patch: Partial<ProductOptionGroup>) => {
    setGroups((current) => current.map((group, groupIndex) => (
      groupIndex === index ? { ...group, ...patch } : group
    )));
  };

  const addGroup = () => {
    setGroups((current) => [...current, {
      id: '',
      productId: product.id,
      name: '',
      selectionType: 'SINGLE',
      required: false,
      minSelections: 0,
      maxSelections: 1,
      displayOrder: current.length + 1,
      active: true,
      options: [{
        id: '',
        name: '',
        priceDelta: 0,
        displayOrder: 1,
        active: true,
      }],
    }]);
  };

  const addOption = (groupIndex: number) => {
    const group = groups[groupIndex];
    updateGroup(groupIndex, {
      options: [...group.options, {
        id: '',
        name: '',
        priceDelta: 0,
        displayOrder: group.options.length + 1,
        active: true,
      }],
    });
  };

  const updateOption = (
    groupIndex: number,
    optionIndex: number,
    patch: Partial<ProductOptionGroup['options'][number]>,
  ) => {
    const group = groups[groupIndex];
    updateGroup(groupIndex, {
      options: group.options.map((option, index) => (
        index === optionIndex ? { ...option, ...patch } : option
      )),
    });
  };

  const removeOption = (groupIndex: number, optionIndex: number) => {
    const group = groups[groupIndex];
    if (group.options.length === 1) {
      toast.error('Mỗi nhóm phải có ít nhất một lựa chọn');
      return;
    }
    updateGroup(groupIndex, {
      options: group.options.filter((_, index) => index !== optionIndex),
    });
  };

  const save = async () => {
    if (groups.some((group) => !group.name.trim()
      || group.options.some((option) => !option.name.trim()))) {
      toast.error('Vui lòng nhập tên cho tất cả nhóm và lựa chọn');
      return;
    }
    setSaving(true);
    try {
      const saved = await replaceProductOptionGroups(product.id, groups);
      setGroups(saved);
      toast.success('Đã lưu cấu hình topping');
      onSaved?.();
      onClose();
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể lưu cấu hình topping');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1400, background: 'rgba(16,24,40,.55)',
      display: 'flex', justifyContent: 'center', alignItems: 'center', padding: 20,
    }}>
      <div style={{
        width: 'min(920px, 96vw)', maxHeight: '92vh', overflowY: 'auto',
        background: '#fff', borderRadius: 16, padding: 24, boxShadow: '0 24px 70px rgba(0,0,0,.25)',
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 16, marginBottom: 18 }}>
          <div>
            <h2 style={{ margin: 0, color: primary }}>Tùy chọn và topping</h2>
            <div style={{ color: '#667085', marginTop: 5 }}>{product.name} · Giá gốc {product.price.toLocaleString('vi-VN')} đ</div>
          </div>
          <button type="button" onClick={onClose} aria-label="Đóng" style={{ border: 0, background: 'none', fontSize: 28, cursor: 'pointer' }}>×</button>
        </div>

        {loading ? <div style={{ padding: 30, textAlign: 'center' }}>Đang tải cấu hình...</div> : (
          <>
            {groups.length === 0 && (
              <div style={{ padding: 24, background: '#f8fafc', borderRadius: 12, color: '#667085', textAlign: 'center' }}>
                Món này chưa có tùy chọn. Hãy thêm nhóm như “Kích cỡ” hoặc “Topping thêm”.
              </div>
            )}
            {groups.map((group, groupIndex) => (
              <section key={group.id || `new-group-${groupIndex}`} style={{
                border: '1px solid #e4e7ec', borderRadius: 12, padding: 16, marginTop: 14,
              }}>
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr auto auto', gap: 10, alignItems: 'center' }}>
                  <input
                    style={inputStyle}
                    value={group.name}
                    maxLength={100}
                    placeholder="Tên nhóm, ví dụ: Kích cỡ"
                    onChange={(event) => updateGroup(groupIndex, { name: event.target.value })}
                  />
                  <select
                    style={inputStyle}
                    value={group.selectionType}
                    onChange={(event) => {
                      const selectionType = event.target.value as ProductOptionGroup['selectionType'];
                      updateGroup(groupIndex, {
                        selectionType,
                        maxSelections: selectionType === 'SINGLE' ? 1 : Math.max(1, group.maxSelections),
                      });
                    }}
                  >
                    <option value="SINGLE">Chọn một</option>
                    <option value="MULTIPLE">Chọn nhiều</option>
                  </select>
                  <label style={{ whiteSpace: 'nowrap' }}>
                    <input
                      type="checkbox"
                      checked={group.required}
                      onChange={(event) => updateGroup(groupIndex, {
                        required: event.target.checked,
                        minSelections: event.target.checked ? Math.max(1, group.minSelections) : 0,
                      })}
                    /> Bắt buộc
                  </label>
                  <label style={{ whiteSpace: 'nowrap' }}>
                    <input type="checkbox" checked={group.active} onChange={(event) => updateGroup(groupIndex, { active: event.target.checked })} /> Hiển thị
                  </label>
                </div>

                {group.selectionType === 'MULTIPLE' && (
                  <div style={{ display: 'flex', gap: 12, marginTop: 10, alignItems: 'center' }}>
                    <label>Tối thiểu <input type="number" min={group.required ? 1 : 0} value={group.minSelections} onChange={(event) => updateGroup(groupIndex, { minSelections: Number(event.target.value) })} style={{ ...inputStyle, width: 82, display: 'inline-block' }} /></label>
                    <label>Tối đa <input type="number" min={1} value={group.maxSelections} onChange={(event) => updateGroup(groupIndex, { maxSelections: Number(event.target.value) })} style={{ ...inputStyle, width: 82, display: 'inline-block' }} /></label>
                  </div>
                )}

                <div style={{ marginTop: 14, display: 'grid', gap: 8 }}>
                  {group.options.map((option, optionIndex) => (
                    <div key={option.id || `new-option-${optionIndex}`} style={{
                      display: 'grid', gridTemplateColumns: '2fr 1fr auto auto', gap: 10, alignItems: 'center',
                      padding: 10, borderRadius: 9, background: '#f8fafc',
                    }}>
                      <input style={inputStyle} value={option.name} maxLength={120} placeholder="Tên lựa chọn" onChange={(event) => updateOption(groupIndex, optionIndex, { name: event.target.value })} />
                      <input style={inputStyle} type="number" min={0} max={10_000_000} step={1000} value={option.priceDelta} onChange={(event) => updateOption(groupIndex, optionIndex, { priceDelta: Number(event.target.value) })} aria-label="Giá cộng thêm" />
                      <label style={{ whiteSpace: 'nowrap' }}><input type="checkbox" checked={option.active} onChange={(event) => updateOption(groupIndex, optionIndex, { active: event.target.checked })} /> Đang bán</label>
                      <button type="button" onClick={() => removeOption(groupIndex, optionIndex)} style={{ border: 0, color: '#b42318', background: 'none', cursor: 'pointer' }}>Xóa</button>
                    </div>
                  ))}
                </div>

                <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 12 }}>
                  <button type="button" onClick={() => addOption(groupIndex)} style={{ border: `1px solid ${primary}`, color: primary, background: '#fff', borderRadius: 8, padding: '8px 12px', cursor: 'pointer' }}>+ Thêm lựa chọn</button>
                  <button type="button" onClick={() => setGroups((current) => current.filter((_, index) => index !== groupIndex))} style={{ border: 0, color: '#b42318', background: 'none', cursor: 'pointer' }}>Xóa nhóm</button>
                </div>
              </section>
            ))}

            <button type="button" onClick={addGroup} style={{ width: '100%', marginTop: 14, padding: 12, borderRadius: 10, border: '1px dashed #98a2b3', background: '#fff', cursor: 'pointer' }}>+ Thêm nhóm tùy chọn</button>
          </>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 20 }}>
          <button type="button" onClick={onClose} style={{ padding: '10px 18px', borderRadius: 9, border: '1px solid #d0d5dd', background: '#fff', cursor: 'pointer' }}>Hủy</button>
          <button type="button" disabled={saving || loading} onClick={() => void save()} style={{ padding: '10px 18px', borderRadius: 9, border: 0, background: primary, color: '#fff', fontWeight: 700, cursor: 'pointer', opacity: saving ? .65 : 1 }}>{saving ? 'Đang lưu...' : 'Lưu topping'}</button>
        </div>
      </div>
    </div>
  );
};

export default ProductOptionManager;
