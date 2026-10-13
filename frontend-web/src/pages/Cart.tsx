import React, { useMemo, useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { useCart } from '@/context/CartContext';
import { useAuth } from '@/context';
import { getAllProducts, Product } from '../data/products';
import { formatVND } from '../utils/currency';
import CartSkeleton from '../components/CartSkeleton';
import toast from 'react-hot-toast';
import { OrderQuote, quoteOrder } from '@/api/orderApi';
import { determineRestaurantFromCartItems } from '@/services/restaurantNotificationService';

const Page = styled.div`
  padding: var(--spacing-xl) var(--spacing-lg);
  max-width: 900px;
  margin: 0 auto;

  @media (max-width: 480px) {
    padding: 16px 12px;
  }
`;

const Title = styled.h2`
  margin: 0 0 16px 0;
`;

const Row = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);

  @media (max-width: 480px) {
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 0;
  }
`;

const Thumb = styled.img`
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;

  @media (max-width: 480px) {
    width: 44px;
    height: 44px;
  }
`;

const Name = styled.div`
  font-weight: 700;

  @media (max-width: 480px) {
    font-size: 14px;
  }
`;

const Sub = styled.div`
  color: var(--secondaryText);

  @media (max-width: 480px) {
    font-size: 13px;
  }
`;

const Button = styled.button`
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  cursor: pointer;
  transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: var(--shadow-md);
    filter: brightness(1.03);
  }

  @media (max-width: 480px) {
    padding: 8px 12px;
    font-size: 13px;
  }
`;

const QtyBtn = styled(Button)`
  background: var(--border);
  color: var(--text);
  padding: 6px 10px;

  @media (max-width: 480px) {
    padding: 5px 9px;
  }
`;

const TotalBar = styled.div`
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
`;

const Total = styled.div`
  color: var(--primary);
  font-weight: 800;
`;

const EmptyState = styled.div`
  text-align: center;
  padding: 40px 20px;
  color: var(--secondaryText);
`;

const Cart: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { items, remove, setQty } = useCart();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [quote, setQuote] = useState<OrderQuote | null>(null);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const productMap = useMemo(() => Object.fromEntries(products.map(p => [p.id, p])), [products]);
  const subtotal = quote?.subtotal ?? 0;
  const delivery = quote?.shippingFee ?? 0;
  const total = quote?.total ?? 0;

  useEffect(() => {
    const loadProducts = async () => {
      try {
        const fetchedProducts = await getAllProducts();
        setProducts(fetchedProducts);
      } catch (error) {
        console.error('Error loading products:', error);
        toast.error('Không thể tải thông tin sản phẩm');
      } finally {
        setLoading(false);
      }
    };
    loadProducts();
  }, []);

  useEffect(() => {
    let mounted = true;
    if (items.length === 0) {
      setQuote(null);
      return;
    }
    const restaurantId = determineRestaurantFromCartItems(items);
    if (!restaurantId) {
      setQuote(null);
      setQuoteError('Giỏ hàng chỉ được chứa món của một nhà hàng');
      return;
    }
    setQuoteError(null);
    quoteOrder({
      customerPhone: user?.phone || '0000000000',
      address: 'Chưa nhập địa chỉ',
      restaurantId,
      items: items.map(item => ({
        productId: item.productId,
        quantity: item.qty,
        selectedOptionIds: item.selectedOptionIds,
        itemNote: item.itemNote,
      })),
      paymentMethod: 'cod',
    }).then(result => {
      if (mounted) setQuote(result);
    }).catch((error: any) => {
      if (!mounted) return;
      setQuote(null);
      setQuoteError(error?.response?.data?.message || 'Không thể tính giá giỏ hàng');
    });
    return () => { mounted = false; };
  }, [items, user?.id, user?.name, user?.phone]);

  const handleRemoveItem = (itemId: string, itemName: string) => {
    remove(itemId);
    toast.success(`✅ Đã xóa "${itemName}" khỏi giỏ hàng`);
  };

  const handleQuantityChange = (itemId: string, newQty: number, itemName: string) => {
    setQty(itemId, newQty);
    toast.success(`📦 Cập nhật số lượng "${itemName}" thành ${newQty}`);
  };

  const handleProceedToCheckout = () => {
    if (items.length === 0) {
      toast.error('Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi xác nhận đơn.');
      return;
    }
    
    // Giỏ hàng là trang được bảo vệ; nhánh này chỉ là lớp an toàn khi phiên hết hạn.
    if (!user) {
      toast.error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
      navigate('/login');
      return;
    }
    
    navigate('/checkout');
  };


  if (loading) {
    return (
      <Page>
        <Title>Giỏ hàng</Title>
        <CartSkeleton count={3} />
      </Page>
    );
  }

  if (items.length === 0) {
    return (
      <Page>
        <Title>Giỏ hàng</Title>
        <EmptyState>
          <p>Giỏ hàng của bạn đang trống — hãy thêm một số món ăn ngon 🍱</p>
          <Link to="/menu" style={{ color: 'var(--primary)', textDecoration: 'underline' }}>
            Xem thực đơn
          </Link>
        </EmptyState>
      </Page>
    );
  }

  return (
    <Page>
      <Title>Giỏ hàng</Title>
      {items.map((c, index) => {
        const p = productMap[c.productId];
        return (
          <motion.div
            key={c.id}
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <Row>
              <Thumb
                src={p?.image || p?.imageUrl || c.image || ''}
                alt={c.name}
                onError={(e) => {
                  e.currentTarget.onerror = null;
                  e.currentTarget.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='80' height='80'%3E%3Crect width='80' height='80' fill='%23e8e8e8'/%3E%3Ctext x='50%25' y='54%25' font-size='11' text-anchor='middle' fill='%23aaa' font-family='sans-serif'%3EKh%C3%B4ng%20c%C3%B3%20%E1%BA%A3nh%3C/text%3E%3C/svg%3E";
                }}
              />
              <div style={{ flex: 1 }}>
                <Name>{c.name}</Name>
                {c.optionSummary && <Sub>{c.optionSummary}</Sub>}
                {c.itemNote && <Sub>Ghi chú: {c.itemNote}</Sub>}
                <Sub>
                  {formatVND(c.price)} • Số lượng:
                  <QtyBtn onClick={() => handleQuantityChange(c.id, Math.max(1, c.qty - 1), c.name)} style={{ marginLeft: 8 }}>
                    -
                  </QtyBtn>
                  <span style={{ margin: '0 8px' }}>{c.qty}</span>
                  <QtyBtn onClick={() => handleQuantityChange(c.id, c.qty + 1, c.name)}>+</QtyBtn>
                </Sub>
              </div>
              <div style={{ fontWeight: 700 }}>{formatVND(quote?.items[index]?.lineTotal ?? 0)}</div>
              <Button onClick={() => handleRemoveItem(c.id, c.name)} style={{ background: 'var(--border)', color: 'var(--text)' }}>
                Xóa
              </Button>
            </Row>
          </motion.div>
        );
      })}
      
      <div style={{ marginTop: 16, borderTop: '1px solid var(--border)', paddingTop: 12 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Tạm tính</span>
          <span>{formatVND(subtotal)}</span>
        </div>
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <span>Phí giao hàng</span>
          <span>{formatVND(delivery)}</span>
        </div>
        <TotalBar>
          <div>Tổng cộng</div>
          <Total>{formatVND(total)}</Total>
        </TotalBar>
        {quoteError && <div style={{ marginTop: 10, color: '#dc2626' }}>{quoteError}</div>}
        <Button style={{ marginTop: 12 }} onClick={handleProceedToCheckout} disabled={!quote}>
          Xác nhận đơn hàng
        </Button>
      </div>
    </Page>
  );
};

export default Cart;
