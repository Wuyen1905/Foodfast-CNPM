import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import apiClient from '../config/axios';
import { realtimeSocket } from '../realtime/socket';
import { useAuth } from './AuthContext';

export type CartItem = {
  id: string;
  productId: string;
  name: string;
  image?: string;
  price: number;
  qty: number;
  restaurantId?: string;
  selectedOptionIds: string[];
  optionSummary?: string;
  itemNote?: string;
};
export type CartItemConfiguration = {
  selectedOptionIds?: string[];
  itemNote?: string;
};
export type ReorderResult = {
  cart: CartItem[];
  copiedItemCount: number;
  requiresConfiguration: ReorderConfigurationItem[];
};
export type ReorderConfigurationItem = {
  productId: string;
  productName: string;
  quantity: number;
  itemNote?: string;
  reason: string;
};
type CartContextValue = {
  items: CartItem[];
  add: (
    id: string,
    qty?: number,
    snapshot?: { name: string; image?: string; price: number },
    configuration?: CartItemConfiguration,
  ) => Promise<void>;
  remove: (id: string) => Promise<void>;
  setQty: (id: string, qty: number) => Promise<void>;
  clear: () => Promise<void>;
  reorder: (orderId: string) => Promise<ReorderResult>;
  subtotal: number;
};

const CartContext = createContext<CartContextValue | undefined>(undefined);

export const CartProvider: React.FC<{ children: React.ReactNode; priceMap: Record<string, number> }>
= ({ children, priceMap }) => {
  const [items, setItems] = useState<CartItem[]>([]);
  const { user } = useAuth();

  // Normalize backend cart item to CartItem format
  const normalizeCartItem = (item: any): CartItem => {
    return {
      id: String(item.id),
      productId: String(item.productId),
      qty: item.quantity || item.qty || 0,
      name: item.productName || item.name || item.id,
      image: item.productImage || item.image,
      price: item.unitPrice || item.price || 0,
      restaurantId: item.restaurantCode || item.restaurantId,
      selectedOptionIds: item.selectedOptionIds || [],
      optionSummary: item.optionSummary,
      itemNote: item.itemNote,
    };
  };

  // Load cart from backend
  const loadCart = useCallback(async () => {
    if (!user?.id) {
      setItems([]);
      return;
    }
    try {
      const response = await apiClient.get('/cart');
      const backendItems = response.data || [];
      const normalizedItems = Array.isArray(backendItems)
        ? backendItems.map(normalizeCartItem)
        : [];
      setItems(normalizedItems);
    } catch (error) {
      console.error('Failed to load cart:', error);
      setItems([]);
    }
  }, [user?.id]);

  // Load cart on mount
  useEffect(() => {
    loadCart();
  }, [loadCart]);

  useEffect(() => {
    const unsubscribeCart = realtimeSocket.onCartUpdate(() => {
      loadCart();
    });
    return () => {
      unsubscribeCart();
    };
  }, [loadCart]);

  const add = async (
    id: string,
    qty: number = 1,
    snapshot?: { name: string; image?: string; price: number },
    configuration?: CartItemConfiguration,
  ) => {
    if (!user?.id) {
      throw new Error('Vui lòng đăng nhập để thêm món vào giỏ hàng');
    }
    try {
      await apiClient.post('/cart/add', {
        productId: id,
        quantity: qty,
        selectedOptionIds: configuration?.selectedOptionIds || [],
        itemNote: configuration?.itemNote || '',
      });
      await loadCart();
    } catch (error) {
      console.error('Failed to add item to cart:', error);
      throw error;
    }
  };

  const remove = async (id: string) => {
    if (!user?.id) return;
    try {
      await apiClient.delete(`/cart/item/${id}`);
      await loadCart();
    } catch (error) {
      console.error('Failed to remove item from cart:', error);
      throw error;
    }
  };

  const setQty = async (id: string, qty: number) => {
    try {
      // If qty is 0 or less, remove the item
      if (qty <= 0) {
        await remove(id);
        return;
      }
      // For setting quantity, we'll add the item with the new quantity
      // This assumes the backend handles quantity updates via add endpoint
      // If not, we may need to remove and re-add, or use a PUT endpoint
      const existingItem = items.find(i => i.id === id);
      if (existingItem && user?.id) {
        await apiClient.put(`/cart/item/${id}`, {
          quantity: qty,
        });
        await loadCart();
      }
    } catch (error) {
      console.error('Failed to update item quantity:', error);
      throw error;
    }
  };

  const clear = async () => {
    if (!user?.id) {
      setItems([]);
      return;
    }
    try {
      await apiClient.delete('/cart/clear');
      await loadCart();
    } catch (error) {
      console.error('Failed to clear cart:', error);
      throw error;
    }
  };

  const reorder = async (orderId: string): Promise<ReorderResult> => {
    if (!user?.id) {
      throw new Error('Vui lòng đăng nhập để đặt lại đơn hàng');
    }
    const response = await apiClient.post(`/cart/reorder/${encodeURIComponent(orderId)}`);
    const backendCart = Array.isArray(response.data?.cart) ? response.data.cart : [];
    const normalizedCart = backendCart.map(normalizeCartItem);
    setItems(normalizedCart);
    return {
      cart: normalizedCart,
      copiedItemCount: Number(response.data?.copiedItemCount) || 0,
      requiresConfiguration: Array.isArray(response.data?.requiresConfiguration)
        ? response.data.requiresConfiguration
        : [],
    };
  };

  const subtotal = useMemo(() => {
    return items.reduce((sum, item) => {
      const price = Number(item.price) || 0;
      const qty = Number(item.qty) || 0;
      return sum + price * qty;
    }, 0);
  }, [items]);
  
  
  return (
    <CartContext.Provider value={{ items, add, remove, setQty, clear, reorder, subtotal }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};


