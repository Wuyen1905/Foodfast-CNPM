import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link, useLocation } from 'react-router-dom';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { getProductImage, Product } from '../data/products';
import { getProductById } from '@/services/menuService';
import { ReorderConfigurationItem, useCart } from '@/context/CartContext';
import { useAuth } from '@/context/AuthContext';
import { formatVND } from '../utils/currency';
import toast from 'react-hot-toast';
import Swal from 'sweetalert2';
import {
  askToSwitchCartRestaurant,
  getCartErrorMessage,
} from '@/utils/cartRestaurantConflict';
import { getProductOptionGroups, ProductOptionGroup } from '@/api/productOptionApi';

const Page = styled.div`
  padding: var(--spacing-xl) var(--spacing-lg);
  max-width: 1000px;
  margin: 0 auto;
`;

const Bread = styled.nav`
  color: #777; 
  font-size: 14px; 
  margin-bottom: 12px;
`;

const Wrap = styled.div`
  display: grid; 
  grid-template-columns: 1fr 1fr; 
  gap: 24px; 
  
  @media (max-width: 900px) { 
    grid-template-columns: 1fr; 
    gap: 16px;
  }
`;

const Img = styled.img`
  width: 100%; 
  aspect-ratio: 16/9; 
  object-fit: cover; 
  border-radius: var(--radius-md); 
  box-shadow: var(--shadow-md);
`;

const Title = styled.h2`
  margin: 0;
`;

const Price = styled.div`
  color: var(--primary); 
  font-weight: 800; 
  font-size: 20px;
`;

const Desc = styled.p`
  color: #555;
  line-height: 1.6;
`;

const Row = styled.div`
  display: flex; 
  align-items: center; 
  gap: 10px; 
  margin-top: 12px;
`;

const QtyBtn = styled.button`
  width: 36px; 
  height: 36px; 
  border-radius: 10px; 
  border: 1px solid var(--border); 
  background: var(--card); 
  cursor: pointer;
  color: var(--text);
`;

const Button = styled.button`
  background: var(--primary); 
  color: #fff; 
  border: none; 
  padding: 10px 14px; 
  border-radius: var(--radius); 
  cursor: pointer; 
  box-shadow: var(--shadow);
  transition: transform 0.2s ease;
  
  &:hover {
    transform: translateY(-1px);
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: none;
  }
`;

const Ghost = styled(Button)`
  background: var(--border); 
  color: var(--text);
`;

const OptionGroupCard = styled.section`
  margin-top: 16px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--card);
`;

const OptionChoice = styled.label`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  cursor: pointer;
  border-bottom: 1px dashed var(--border);

  &:last-child { border-bottom: none; }
`;

const UnavailableNotice = styled.div`
  margin: 14px 0;
  padding: 12px 14px;
  border: 1px solid #fda29b;
  border-radius: 10px;
  background: #fff6f5;
  color: #b42318;
  font-weight: 700;
`;

const ReorderNotice = styled.div`
  margin: 0 0 16px;
  padding: 14px 16px;
  border: 1px solid #fdba74;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a3412;
  line-height: 1.5;
`;

type ReorderLocationState = {
  reorderQueue?: ReorderConfigurationItem[];
  copiedItemCount?: number;
};

const REQUIRED_OPTIONS_TOAST_ID = 'product-options-required';
const REORDER_OPTIONS_TOAST_ID = 'reorder-options';

const Details: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { add, clear } = useCart();
  const { user } = useAuth();
  const [product, setProduct] = useState<Product | null>(null);
  const [qty, setQty] = useState(1);
  const [loading, setLoading] = useState(true);
  const [optionGroups, setOptionGroups] = useState<ProductOptionGroup[]>([]);
  const [selectedOptionIds, setSelectedOptionIds] = useState<string[]>([]);
  const [itemNote, setItemNote] = useState('');
  const reorderState = (location.state || {}) as ReorderLocationState;
  const reorderQueue = Array.isArray(reorderState.reorderQueue)
    ? reorderState.reorderQueue
    : [];
  const currentReorderItem = reorderQueue[0]?.productId === id
    ? reorderQueue[0]
    : undefined;

  useEffect(() => {
    setSelectedOptionIds([]);
    setQty(Math.max(1, Number(currentReorderItem?.quantity) || 1));
    setItemNote(currentReorderItem?.itemNote || '');
  }, [id, location.key]);

  useEffect(() => {
    const loadProduct = async () => {
      if (!id) {
        setLoading(false);
        return;
      }
      
      setLoading(true);
      try {
        const fetchedProduct = await getProductById(id);
        setProduct(fetchedProduct);
        const groups = await getProductOptionGroups(id);
        setOptionGroups(groups);
      } catch (error) {
        console.error('Error loading product:', error);
        toast.error('Không thể tải thông tin sản phẩm');
      } finally {
        setLoading(false);
      }
    };
    
    loadProduct();
  }, [id]);

  if (loading) {
    return (
      <Page>
        <div className="skeleton" style={{ height: '20px', width: '200px', marginBottom: '12px' }} />
        <Wrap>
          <div className="skeleton" style={{ height: '300px' }} />
          <div>
            <div className="skeleton" style={{ height: '24px', width: '60%', marginBottom: '12px' }} />
            <div className="skeleton" style={{ height: '20px', width: '40%', marginBottom: '16px' }} />
            <div className="skeleton" style={{ height: '80px', marginBottom: '16px' }} />
            <div className="skeleton" style={{ height: '40px', width: '100%' }} />
          </div>
        </Wrap>
      </Page>
    );
  }

  if (!product) return <Page>Sản phẩm không tìm thấy.</Page>;

  const hasAllRequiredOptions = (optionIds: string[]): boolean => optionGroups.every((group) => {
    const count = group.options.filter((option) => optionIds.includes(option.id)).length;
    const minimum = group.required ? Math.max(1, group.minSelections) : group.minSelections;
    return count >= minimum;
  });

  const toggleOption = (group: ProductOptionGroup, optionId: string) => {
    setSelectedOptionIds((current) => {
      const groupOptionIds = new Set(group.options.map((option) => option.id));
      let next: string[];
      if (group.selectionType === 'SINGLE') {
        next = [...current.filter((id) => !groupOptionIds.has(id)), optionId];
      } else if (current.includes(optionId)) {
        next = current.filter((id) => id !== optionId);
      } else {
        const selectedInGroup = current.filter((id) => groupOptionIds.has(id)).length;
        if (selectedInGroup >= group.maxSelections) {
          toast.error(`Chỉ được chọn tối đa ${group.maxSelections} lựa chọn cho “${group.name}”`);
          return current;
        }
        next = [...current, optionId];
      }

      if (hasAllRequiredOptions(next)) {
        toast.dismiss(REQUIRED_OPTIONS_TOAST_ID);
        toast.dismiss(REORDER_OPTIONS_TOAST_ID);
      }
      return next;
    });
  };

  const validateOptions = (): boolean => {
    for (const group of optionGroups) {
      const count = group.options.filter((option) => selectedOptionIds.includes(option.id)).length;
      const minimum = group.required ? Math.max(1, group.minSelections) : group.minSelections;
      if (count < minimum) {
        toast.error(`Vui lòng chọn đủ lựa chọn cho “${group.name}”`, {
          id: REQUIRED_OPTIONS_TOAST_ID,
          duration: 6_000,
        });
        return false;
      }
    }
    return true;
  };

  const optionPrice = optionGroups
    .flatMap((group) => group.options)
    .filter((option) => selectedOptionIds.includes(option.id))
    .reduce((sum, option) => sum + option.priceDelta, 0);
  const configuredUnitPrice = product.price + optionPrice;

  const addCurrentProduct = async (): Promise<boolean> => {
    if (!product) return false;
    if (!product.available) {
      toast.error(product.soldOutToday
        ? 'Món đã bán hết số lượng hôm nay'
        : 'Món hiện đang tạm ngừng bán');
      return false;
    }
    if (!validateOptions()) return false;
    const snapshot = {
      name: product.name,
      image: getProductImage(product),
      price: product.price,
    };
    try {
      await add(product.id, qty, snapshot, { selectedOptionIds, itemNote });
      return true;
    } catch (error: any) {
      const shouldSwitch = await askToSwitchCartRestaurant(error);
      if (shouldSwitch === null) {
        toast.error(getCartErrorMessage(error));
        return false;
      }
      if (!shouldSwitch) return false;

      try {
        await clear();
        await add(product.id, qty, snapshot, { selectedOptionIds, itemNote });
        return true;
      } catch (replaceError: any) {
        toast.error(getCartErrorMessage(replaceError));
        return false;
      }
    }
  };

  const addToCart = async () => {
    // Check if user is logged in
    if (!user) {
      Swal.fire({
        title: "Hãy đăng nhập để tiếp tục mua hàng!",
        text: "Bạn cần đăng nhập để thêm món vào giỏ hàng và theo dõi đơn hàng của mình.",
        icon: "warning",
        confirmButtonText: "Đăng nhập ngay",
        cancelButtonText: "Hủy",
        showCancelButton: true,
        confirmButtonColor: "#007bff",
        cancelButtonColor: "#6c757d"
      }).then((result) => {
        if (result.isConfirmed) {
          navigate("/login");
        }
      });
      return;
    }

    if (await addCurrentProduct()) {
      toast.dismiss(REQUIRED_OPTIONS_TOAST_ID);
      toast.dismiss(REORDER_OPTIONS_TOAST_ID);
      if (currentReorderItem) {
        const remainingQueue = reorderQueue.slice(1);
        if (remainingQueue.length > 0) {
          const nextItem = remainingQueue[0];
          toast(`Đã thêm ${product.name}. Tiếp tục chọn tùy chọn cho ${nextItem.productName}.`, {
            id: REORDER_OPTIONS_TOAST_ID,
            icon: '⚙️',
          });
          navigate(`/details/${encodeURIComponent(nextItem.productId)}`, {
            replace: true,
            state: {
              reorderQueue: remainingQueue,
              copiedItemCount: reorderState.copiedItemCount,
            },
          });
        } else {
          toast.success('Đã hoàn tất đặt lại đơn. Vui lòng kiểm tra giỏ hàng.');
          navigate('/cart');
        }
        return;
      }
      toast.success('🛒 Đã thêm vào giỏ hàng thành công!');
    }
  };
  
  const buyNow = async () => {
    // Check if user is logged in
    if (!user) {
      Swal.fire({
        title: "Hãy đăng nhập để tiếp tục mua hàng!",
        text: "Bạn cần đăng nhập để mua hàng và theo dõi đơn hàng của mình.",
        icon: "warning",
        confirmButtonText: "Đăng nhập ngay",
        cancelButtonText: "Hủy",
        showCancelButton: true,
        confirmButtonColor: "#007bff",
        cancelButtonColor: "#6c757d"
      }).then((result) => {
        if (result.isConfirmed) {
          navigate("/login");
        }
      });
      return;
    }

    if (await addCurrentProduct()) {
      navigate('/checkout');
    }
  };

  if (loading) {
    return (
      <Page>
        <div className="skeleton" style={{ height: '20px', width: '200px', marginBottom: '12px' }} />
        <Wrap>
          <div className="skeleton" style={{ height: '300px' }} />
          <div>
            <div className="skeleton" style={{ height: '24px', width: '60%', marginBottom: '12px' }} />
            <div className="skeleton" style={{ height: '20px', width: '40%', marginBottom: '16px' }} />
            <div className="skeleton" style={{ height: '80px', marginBottom: '16px' }} />
            <div className="skeleton" style={{ height: '40px', width: '100%' }} />
          </div>
        </Wrap>
      </Page>
    );
  }

  return (
    <Page>
      <Bread>
        <Link to="/">Trang chủ</Link> &gt; <Link to="/menu">Thực đơn</Link> &gt; <span>{product.name}</span>
      </Bread>
      {currentReorderItem && (
        <ReorderNotice>
          <strong>Hoàn tất đặt lại đơn</strong><br />
          Tùy chọn của <strong>{currentReorderItem.productName}</strong> đã thay đổi.
          Hãy chọn lại theo thực đơn hiện tại rồi bấm “Thêm vào giỏ”.
          {reorderQueue.length > 1 && ` Còn ${reorderQueue.length} món cần cấu hình.`}
        </ReorderNotice>
      )}
      <Wrap>
        <Img src={getProductImage(product)} alt={product.name} />
        <div>
          <Title>{product.name}</Title>
          <Price>{formatVND(configuredUnitPrice)}</Price>
          <Desc>{product.description}</Desc>
          {!product.available && (
            <UnavailableNotice>
              {product.soldOutToday
                ? 'Món này đã bán hết số lượng hôm nay và sẽ tự mở lại vào ngày mai.'
                : 'Nhà hàng đang tạm ngừng bán món này.'}
            </UnavailableNotice>
          )}
          {optionGroups.map((group) => (
            <OptionGroupCard key={group.id}>
              <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 6 }}>
                <strong>{group.name}</strong>
                <span style={{ color: 'var(--secondaryText)', fontSize: 13 }}>
                  {group.required ? 'Bắt buộc' : 'Không bắt buộc'}
                  {group.selectionType === 'MULTIPLE' ? ` · tối đa ${group.maxSelections}` : ''}
                </span>
              </div>
              {group.options.map((option) => (
                <OptionChoice key={option.id}>
                  <span>
                    <input
                      type={group.selectionType === 'SINGLE' ? 'radio' : 'checkbox'}
                      name={`option-${group.id}`}
                      checked={selectedOptionIds.includes(option.id)}
                      onChange={() => toggleOption(group, option.id)}
                      style={{ marginRight: 10 }}
                    />
                    {option.name}
                  </span>
                  <span>{option.priceDelta > 0 ? `+${formatVND(option.priceDelta)}` : 'Không thêm phí'}</span>
                </OptionChoice>
              ))}
            </OptionGroupCard>
          ))}
          <OptionGroupCard>
            <strong>Ghi chú cho món</strong>
            <textarea
              value={itemNote}
              maxLength={200}
              onChange={(event) => setItemNote(event.target.value)}
              placeholder="Ví dụ: ít cay, không hành..."
              rows={3}
              style={{
                width: '100%', marginTop: 10, padding: 10, borderRadius: 8,
                border: '1px solid var(--border)', background: 'var(--background)', color: 'var(--text)',
              }}
            />
            <small style={{ display: 'block', textAlign: 'right', color: 'var(--secondaryText)' }}>
              {itemNote.length}/200
            </small>
          </OptionGroupCard>
          <Row>
            <QtyBtn onClick={() => setQty(q => Math.max(1, q - 1))}>-</QtyBtn>
            <span>{qty}</span>
            <QtyBtn onClick={() => setQty(q => q + 1)}>+</QtyBtn>
          </Row>
          <Row>
            <Button disabled={!product.available} onClick={addToCart}>
              {!product.available
                ? product.soldOutToday ? 'Hết hôm nay' : 'Tạm ngừng bán'
                : currentReorderItem
                  ? 'Xác nhận tùy chọn và tiếp tục'
                  : 'Thêm vào giỏ'}
            </Button>
            {!currentReorderItem && (
              <Ghost disabled={!product.available} onClick={buyNow}>Mua ngay</Ghost>
            )}
          </Row>
        </div>
      </Wrap>
    </Page>
  );
};

export default Details;
