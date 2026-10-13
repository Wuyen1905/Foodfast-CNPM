import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useAuth } from './AuthContext';
import { addFavorite, getFavoriteIds, removeFavorite } from '@/api/favoriteApi';

type WishlistContextValue = {
  ids: string[];
  loading: boolean;
  toggle: (id: string) => Promise<boolean>;
  has: (id: string) => boolean;
  refresh: () => Promise<void>;
};

const WishlistContext = createContext<WishlistContextValue | undefined>(undefined);

export const WishlistProvider: React.FC<{ children: React.ReactNode }>= ({ children }) => {
  const { user, isCustomer } = useAuth();
  const [ids, setIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const refresh = useCallback(async () => {
    if (!user || !isCustomer()) {
      setIds([]);
      return;
    }
    setLoading(true);
    try {
      setIds(await getFavoriteIds());
    } finally {
      setLoading(false);
    }
  }, [isCustomer, user]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const toggle = async (id: string) => {
    const removing = ids.includes(id);
    const updated = removing ? await removeFavorite(id) : await addFavorite(id);
    setIds(updated);
    return !removing;
  };
  const has = (id: string) => ids.includes(id);

  return <WishlistContext.Provider value={{ ids, loading, toggle, has, refresh }}>{children}</WishlistContext.Provider>;
};

export const useWishlist = () => {
  const ctx = useContext(WishlistContext);
  if (!ctx) throw new Error('useWishlist must be used within WishlistProvider');
  return ctx;
};


