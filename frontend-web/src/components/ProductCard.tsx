import React from 'react';
import styled from 'styled-components';
import { Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Product, getProductImage } from '../data/products';
import { useCart } from '@/context/CartContext';
import { useWishlist } from '@/context/WishlistContext';
import { useAuth } from '@/context/AuthContext';
import { formatVND } from '../utils/currency';
import toast from 'react-hot-toast';
import { useRoleGuard } from '@/hooks/useRoleGuard';
import Swal from 'sweetalert2';
import {
  askToSwitchCartRestaurant,
  getCartErrorMessage,
} from '@/utils/cartRestaurantConflict';

const Card = styled(motion.div)<{ isAdmin?: boolean }>`
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: 16px;
  box-shadow: var(--shadow);
  border: 1px solid var(--border);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  
  ${props => props.isAdmin && `
    border: 2px solid #e74c3c;
    box-shadow: 0 0 0 1px rgba(231, 76, 60, 0.1);
    
    &::after {
      content: 'ADMIN';
      position: absolute;
      top: 8px;
      right: 8px;
      background: #e74c3c;
      color: white;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: bold;
      z-index: 10;
    }
  `}
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--primary), var(--primary-light));
    opacity: 0;
    transition: opacity 0.3s ease;
  }
  
  &:hover::before {
    opacity: 1;
  }
  
  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
`;

const ThumbWrap = styled(Link)`
  display: block;
  position: relative;
  border-radius: var(--radius-md);
  overflow: hidden;
  transition: transform 0.3s ease;
  
  &:hover {
    transform: scale(1.02);
  }
`;

const Thumb = styled.img`
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  display: block;
  transition: transform 0.3s ease;
`;

const Title = styled(Link)`
  display: block;
  margin-top: 12px;
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
  transition: color 0.2s ease;
  
  &:hover {
    color: var(--primary);
  }
`;

const PriceRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
`;

const Price = styled.div`
  color: var(--primary);
  font-weight: 800;
  font-size: 18px;
`;

const Category = styled.span`
  color: var(--secondaryText);
  font-size: 12px;
  background: var(--border);
  padding: 4px 8px;
  border-radius: 12px;
`;

const Actions = styled.div`
  display: flex; 
  gap: 8px; 
  margin-top: 12px;
`;

const Button = styled(motion.button)`
  flex: 1;
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--primary-light);
    transform: translateY(-1px);
    box-shadow: var(--shadow-md);
  }
`;

const Ghost = styled(motion.button)`
  background: var(--border); 
  color: var(--text);
  border: none;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 18px;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--primary);
    color: #fff;
    transform: scale(1.05);
  }
`;

const Tag = styled(motion.span)`
  position: absolute; 
  top: 8px; 
  left: 8px;
  font-size: 12px; 
  color: #fff; 
  background: var(--primary);
  padding: 6px 12px; 
  border-radius: 999px;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
`;

const UnavailableTag = styled.span`
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
  padding: 6px 10px;
  border-radius: 999px;
  background: #b42318;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
`;

type Props = { 
  product: Product;
  isAdmin?: boolean;
  onEdit?: (product: Product) => void;
  onDelete?: (productId: string) => void;
  onAddToCart?: (product: Product) => void;
  orderingDisabledReason?: string;
};

const ProductCard: React.FC<Props> = ({ product, isAdmin, onEdit, onDelete, onAddToCart, orderingDisabledReason }) => {
  const { add, clear } = useCart();
  const { toggle, has } = useWishlist();
  const { user } = useAuth();
  const { canAddToCart, isRestaurant } = useRoleGuard();
  const navigate = useNavigate();
  const img = getProductImage(product);
  const itemDisabledReason = !product.available
    ? product.soldOutToday
      ? 'Món đã bán hết số lượng hôm nay'
      : 'Món hiện đang tạm ngừng bán'
    : orderingDisabledReason;

  const onAddCart = async () => {
    if (itemDisabledReason) {
      toast.error(itemDisabledReason);
      return;
    }
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

    // Prevent restaurants from adding to cart
    if (!canAddToCart()) {
      toast.error('🚫 Tài khoản nhà hàng không thể thêm món vào giỏ hàng');
      return;
    }

    if (product.customizable) {
      navigate(`/details/${product.id}`);
      return;
    }
    
    if (onAddToCart) {
      onAddToCart(product);
    } else {
      try {
        await add(product.id, 1, { name: product.name, image: img, price: product.price });
        toast.success('🛒 Đã thêm vào giỏ hàng!');
      } catch (error: any) {
        const shouldSwitch = await askToSwitchCartRestaurant(error);
        if (shouldSwitch === null) {
          toast.error(getCartErrorMessage(error));
          return;
        }
        if (!shouldSwitch) return;

        try {
          await clear();
          await add(product.id, 1, { name: product.name, image: img, price: product.price });
          toast.success('Đã chuyển nhà hàng và thêm món vào giỏ');
        } catch (replaceError: any) {
          toast.error(getCartErrorMessage(replaceError));
        }
      }
    }
  };
  
  const onWishlist = async () => {
    // Check if user is logged in
    if (!user) {
      Swal.fire({
        title: "Hãy đăng nhập để sử dụng danh sách yêu thích!",
        text: "Bạn cần đăng nhập để thêm món vào danh sách yêu thích của mình.",
        icon: "info",
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

    // Prevent restaurants from using wishlist
    if (!canAddToCart()) {
      toast.error('🚫 Tài khoản nhà hàng không thể sử dụng danh sách yêu thích');
      return;
    }
    
    try {
      const added = await toggle(product.id);
      toast.success(added ? 'Đã thêm vào danh sách yêu thích ❤️' : 'Đã xóa khỏi danh sách yêu thích');
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể cập nhật danh sách yêu thích');
    }
  };

  return (
    <Card 
      isAdmin={isAdmin}
      whileHover={{ y: -4, scale: 1.01 }} 
      transition={{ type: 'spring', stiffness: 300, damping: 20 }}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <ThumbWrap to={`/details/${product.id}`} aria-label={product.name}>
        {product.tag && (
          <Tag
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2 }}
          >
            {product.tag}
          </Tag>
        )}
        {!product.available && (
          <UnavailableTag>{product.soldOutToday ? 'Hết hôm nay' : 'Tạm ngừng'}</UnavailableTag>
        )}
        <Thumb src={img} alt={product.name} onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='400' height='300'%3E%3Crect width='400' height='300' fill='%23e8e8e8'/%3E%3Ctext x='50%25' y='52%25' font-size='18' text-anchor='middle' fill='%23aaa' font-family='sans-serif'%3EKh%C3%B4ng%20c%C3%B3%20%E1%BA%A3nh%3C/text%3E%3C/svg%3E"; }} />
      </ThumbWrap>
      <Title to={`/details/${product.id}`}>{product.name}</Title>
      <PriceRow>
        <Price>{formatVND(product.price)}</Price>
        <Category>{product.category}</Category>
      </PriceRow>
      <Actions>
        {isAdmin ? (
          <>
            <Button 
              onClick={() => onEdit?.(product)}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              style={{ background: '#2196f3' }}
            >
              ✏️ Chỉnh sửa
            </Button>
            <Ghost 
              onClick={() => onDelete?.(product.id)}
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              style={{ color: '#e74c3c' }}
            >
              🗑️ Xóa
            </Ghost>
          </>
        ) : isRestaurant ? (
          <>
            <Button 
              onClick={() => toast('💡 Hãy vào trang Quản lý thực đơn để chỉnh sửa món ăn', { icon: '💡' })}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              style={{ background: '#FF6600', opacity: 0.7, cursor: 'not-allowed' }}
              disabled
            >
              🏪 Nhà hàng
            </Button>
            <Ghost 
              style={{ opacity: 0.5, cursor: 'not-allowed' }}
              disabled
            >
              🚫
            </Ghost>
          </>
        ) : (
          <>
            <Button 
              onClick={onAddCart}
              disabled={Boolean(itemDisabledReason)}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              style={itemDisabledReason ? { opacity: 0.6, cursor: 'not-allowed' } : undefined}
            >
              {itemDisabledReason ? itemDisabledReason : product.customizable ? 'Tùy chọn món' : 'Thêm vào giỏ'}
            </Button>
            <Ghost 
              onClick={onWishlist}
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
            >
              {has(product.id) ? '♥' : '♡'}
            </Ghost>
          </>
        )}
      </Actions>
    </Card>
  );
};

export default ProductCard;


