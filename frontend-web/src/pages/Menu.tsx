import React, { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import styled from 'styled-components';
import { motion } from 'framer-motion';
import { Product } from '../data/products';
import ProductCard from '../components/ProductCard';
import ProductCardSkeletons from '../components/ProductCardSkeleton';
import { useAuth } from '@/context';
import { useRoleGuard } from '@/hooks/useRoleGuard';
import { useRestaurantSelection } from '@/context/RestaurantSelectionContext';
import { useMenu } from '@/context/MenuContext';
import { getAvailableMenuByRestaurant } from '@/services/menuService';
import { getRestaurant, Restaurant } from '@/api/restaurantApi';
import toast from 'react-hot-toast';

const Page = styled.div`
  padding: var(--spacing-xl) var(--spacing-lg);
  max-width: 1200px;
  margin: 0 auto;
`;

// Hero section from Home page
const Hero = styled(motion.section)`
  background: linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%);
  color: #fff;
  padding: 48px 32px;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  margin-bottom: 28px;
`;

const HeroTitle = styled.h1`
  margin: 0;
  font-size: 36px;
  letter-spacing: 0.2px;
  
  @media (max-width: 768px) {
    font-size: 28px;
  }
`;

const Sub = styled.p`
  margin: 8px 0 0;
  opacity: 0.95;
`;

const CTA = styled(Link)`
  display: inline-block;
  background: #fff;
  color: var(--primary);
  border: none;
  padding: 12px 18px;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow);
  font-weight: 800;
  transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;
  margin-top: 16px;
  
  &:hover { 
    transform: translateY(-1px); 
    box-shadow: var(--shadow-md); 
    filter: brightness(1.05); 
  }
`;

const LoginPrompt = styled.div`
  text-align: center;
  padding: 40px 20px;
  background: var(--card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  margin-bottom: 28px;
`;

const LoginTitle = styled.h2`
  margin: 0 0 16px 0;
  color: var(--text);
`;

const LoginSubtitle = styled.p`
  color: var(--secondaryText);
  margin: 0 0 24px 0;
`;

// Menu section styling
const Title = styled.h2`
  margin: 0 0 16px 0;
  color: var(--text);
`;

const Controls = styled.div`
  display: grid; 
  grid-template-columns: 1fr 1fr 1fr; 
  gap: 12px; 
  margin-bottom: 16px;
  
  @media (max-width: 768px) { 
    grid-template-columns: 1fr; 
    gap: 8px;
  }
`;

const Input = styled.input`
  padding: 10px 12px; 
  border: 1px solid var(--border); 
  border-radius: var(--radius); 
  outline: none;
  background: var(--card);
  color: var(--text);
  
  &:focus {
    border-color: var(--primary);
  }
`;

const Select = styled.select`
  padding: 10px 12px; 
  border: 1px solid var(--border); 
  border-radius: var(--radius); 
  outline: none; 
  background: var(--card);
  color: var(--text);
  
  &:focus {
    border-color: var(--primary);
  }
`;

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  
  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 16px;
  }
`;

const AdminControls = styled.div`
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
`;

const AdminButton = styled.button`
  background: var(--primary);
  color: #fff;
  border: none;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s ease;
  
  &:hover {
    background: var(--primary-light);
    transform: translateY(-1px);
  }
`;

const Modal = styled.div<{ isOpen: boolean }>`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: ${props => props.isOpen ? 'flex' : 'none'};
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background: var(--card);
  padding: 24px;
  border-radius: var(--radius-lg);
  max-width: 500px;
  width: 90%;
`;

const FormGroup = styled.div`
  margin-bottom: 16px;
`;

const AvailabilityBanner = styled.div<{ $available: boolean }>`
  margin: 18px 0;
  padding: 14px 18px;
  border: 1px solid ${props => props.$available ? '#86efac' : '#fda29b'};
  border-radius: 12px;
  background: ${props => props.$available ? '#f0fdf4' : '#fff6f5'};
  color: ${props => props.$available ? '#166534' : '#b42318'};
  font-weight: 700;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  color: var(--text);
  font-weight: 600;
`;

const Menu: React.FC = () => {
  const { user, isAdmin } = useAuth();
  const { isRestaurant, isCustomer } = useRoleGuard();
  const { selectedRestaurant } = useRestaurantSelection();
  const { getRestaurantAvailableProducts } = useMenu();
  const [items, setItems] = useState<Product[]>([]);
  const [filteredItems, setFilteredItems] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [catFilter, setCatFilter] = useState('Tất cả');
  const [priceFilter, setPriceFilter] = useState("all");
  const [showModal, setShowModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [restaurantStatus, setRestaurantStatus] = useState<Restaurant | null>(null);

  // Load products by selected restaurant
  useEffect(() => {
    const loadMenu = async () => {
      setLoading(true);
      try {
        // For customers, load products from selected restaurant
        const restaurantId = selectedRestaurant === 'SweetDreams' ? 'rest_2' : 'restaurant_2';
        const [restaurantProducts, restaurant] = await Promise.all([
          getAvailableMenuByRestaurant(selectedRestaurant),
          getRestaurant(restaurantId),
        ]);
        setItems(restaurantProducts);
        setFilteredItems(restaurantProducts);
        setRestaurantStatus(restaurant);
      } catch (error) {
        console.error('Error loading menu:', error);
        toast.error('Không thể tải thực đơn');
        setItems([]);
        setFilteredItems([]);
      } finally {
        setLoading(false);
      }
    };
    
    loadMenu();
  }, [selectedRestaurant]);

  useEffect(() => {
    let result = items;
    
    // Search filter
    if (search) {
      result = result.filter(p =>
        p.name.toLowerCase().includes(search.toLowerCase()) ||
        (p.description && p.description.toLowerCase().includes(search.toLowerCase()))
      );
    }
    
    // Category filter
    if (catFilter !== 'Tất cả') {
      result = result.filter(p => p.category === catFilter);
    }
    
    // Price filter
    const filteredMenuItems = result.filter(item => {
      if (priceFilter === "0-50") return item.price <= 50000;
      if (priceFilter === "50-70") return item.price > 50000 && item.price <= 70000;
      if (priceFilter === "70-100") return item.price > 70000 && item.price <= 100000;
      return true;
    });

    setFilteredItems(filteredMenuItems);
  }, [search, catFilter, priceFilter, items]);

  // Get categories from current restaurant's products
  const categories = ['Tất cả', ...Array.from(new Set(items.map(p => p.category)))];
  const orderingDisabledReason = restaurantStatus && !restaurantStatus.isActive
    ? 'Nhà hàng hiện không hoạt động'
    : restaurantStatus && !restaurantStatus.acceptingOrders
    ? `Nhà hàng đang tạm ngừng nhận đơn${restaurantStatus.pauseReason ? `: ${restaurantStatus.pauseReason}` : ''}`
    : restaurantStatus && !restaurantStatus.openNow
    ? `Nhà hàng đang đóng cửa. Giờ phục vụ: ${restaurantStatus.openingTime}–${restaurantStatus.closingTime}`
    : undefined;

  // Redirect admin users to admin dashboard
  if (user && isAdmin()) {
    return <Navigate to="/admin/dashboard" replace />;
  }

  // Redirect restaurant users to their dashboard
  if (user && isRestaurant) {
    toast('🏪 Chuyển hướng đến bảng điều khiển nhà hàng...', { icon: '🏪' });
    return <Navigate to="/restaurant" replace />;
  }

  return (
    <Page>
      {/* Hero Section - Welcome banner */}
      <Hero
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <HeroTitle>🛵 Đặt món nhanh, giao tận nơi</HeroTitle>
        <Sub>Đặt món ăn yêu thích và theo dõi trạng thái giao hàng rõ ràng.</Sub>
        {user ? (
          <CTA to="/cart">🛒 Xem giỏ hàng</CTA>
        ) : (
          <CTA to="/login">Đăng nhập để đặt món</CTA>
        )}
      </Hero>

      {restaurantStatus && (
        <AvailabilityBanner $available={!orderingDisabledReason}>
          {orderingDisabledReason || `Đang mở cửa và nhận đơn · ${restaurantStatus.openingTime}–${restaurantStatus.closingTime}`}
        </AvailabilityBanner>
      )}
      
      {/* Login Prompt for non-logged in users */}
      {!user && (
        <LoginPrompt>
          <LoginTitle>Chào mừng đến với FoodFast!</LoginTitle>
          <LoginSubtitle>
            Đăng nhập để đặt món ăn và theo dõi quá trình giao hàng theo thời gian thực.
          </LoginSubtitle>
          <CTA to="/login">Đăng nhập ngay</CTA>
        </LoginPrompt>
      )}
      
      {/* Menu Section */}
      <Title>{user ? 'Thực đơn' : 'Khám phá món ăn'}</Title>
      
      {/* Search and Filter Controls */}
      {user && (
        <Controls>
          <Input
            type="search"
            placeholder="🔍 Tìm kiếm món ăn..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <Select value={catFilter} onChange={(e) => setCatFilter(e.target.value)}>
            {categories.map(cat => (
              <option key={cat} value={cat}>
                {cat === 'Tất cả' ? 'Tất cả danh mục' : cat}
              </option>
            ))}
          </Select>
          <Select value={priceFilter} onChange={(e) => setPriceFilter(e.target.value)}>
            <option value="all">Tất cả giá</option>
            <option value="0-50">0 – 50.000 đ</option>
            <option value="50-70">50.000 – 70.000 đ</option>
            <option value="70-100">70.000 – 100.000 đ</option>
          </Select>
        </Controls>
      )}
      
      {/* Product Grid */}
      {loading ? (
        <ProductCardSkeletons count={6} />
      ) : (
        <Grid>
          {filteredItems.length > 0 ? (
            filteredItems.map((p, index) => (
              <motion.div
                key={p.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
              >
                <ProductCard product={p} orderingDisabledReason={orderingDisabledReason} />
              </motion.div>
            ))
          ) : (
            <div style={{ gridColumn: '1 / -1', textAlign: 'center', padding: '40px', color: 'var(--secondaryText)' }}>
              Không tìm thấy món ăn phù hợp. Hãy thử tìm kiếm khác!
            </div>
          )}
        </Grid>
      )}
    </Page>
  );
};

export default Menu;
