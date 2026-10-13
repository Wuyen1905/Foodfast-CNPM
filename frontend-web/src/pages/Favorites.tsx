import React, { useEffect, useMemo, useState } from 'react';
import styled from 'styled-components';
import ProductCard from '@/components/ProductCard';
import ProductCardSkeletons from '@/components/ProductCardSkeleton';
import { getFavoriteProducts } from '@/api/favoriteApi';
import { transformProduct } from '@/services/menuService';
import { useWishlist } from '@/context/WishlistContext';
import type { Product } from '@/data/products';
import toast from 'react-hot-toast';

const Page = styled.main`
  max-width: 1200px;
  margin: 0 auto;
  padding: 36px 24px 64px;
`;

const Header = styled.div`
  margin-bottom: 24px;
  h1 { margin: 0 0 8px; color: var(--text); }
  p { margin: 0; color: var(--secondaryText); }
`;

const Grid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
`;

const Empty = styled.div`
  padding: 56px 24px;
  text-align: center;
  border: 1px dashed var(--border);
  border-radius: 16px;
  background: var(--card);
  color: var(--secondaryText);
`;

const Favorites: React.FC = () => {
  const { ids, loading: idsLoading } = useWishlist();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const signature = ids.join('|');

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const result = await getFavoriteProducts();
        setProducts(result.map(transformProduct));
      } catch (error: any) {
        toast.error(error?.response?.data?.message || 'Không thể tải danh sách yêu thích');
      } finally {
        setLoading(false);
      }
    };
    void load();
  }, [signature]);

  const visibleProducts = useMemo(
    () => products.filter(product => ids.includes(product.id)),
    [ids, products],
  );

  return (
    <Page>
      <Header>
        <h1>Món ăn yêu thích</h1>
        <p>Danh sách được lưu theo tài khoản và đồng bộ giữa các lần đăng nhập.</p>
      </Header>
      {loading || idsLoading ? (
        <ProductCardSkeletons count={3} />
      ) : visibleProducts.length > 0 ? (
        <Grid>
          {visibleProducts.map(product => (
            <ProductCard key={product.id} product={product} />
          ))}
        </Grid>
      ) : (
        <Empty>Bạn chưa lưu món nào. Hãy chọn biểu tượng ♡ trong thực đơn.</Empty>
      )}
    </Page>
  );
};

export default Favorites;
