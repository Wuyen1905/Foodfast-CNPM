import { describe, expect, it } from 'vitest';

import {
  determineRestaurantFromCartItems,
  getRestaurantIdFromProductId,
  normalizeRestaurantId,
} from '../restaurantNotificationService';

describe('restaurant identity mapping', () => {
  it('returns canonical IDs for product prefixes', () => {
    expect(getRestaurantIdFromProductId('sd-001')).toBe('rest_2');
    expect(getRestaurantIdFromProductId('ak-001')).toBe('restaurant_2');
  });

  it('normalizes legacy aliases to canonical IDs', () => {
    expect(normalizeRestaurantId('sweetdreams')).toBe('rest_2');
    expect(normalizeRestaurantId('aloha')).toBe('restaurant_2');
  });

  it('stores the canonical restaurant ID in a SweetDreams order', () => {
    expect(determineRestaurantFromCartItems([
      { id: 'sd-001', name: 'Bánh Donut', qty: 1, price: 25_000 },
    ])).toBe('rest_2');
  });

  it('rejects a cart containing products from different restaurants', () => {
    expect(determineRestaurantFromCartItems([
      { id: 'sd-001', name: 'Bánh Donut', qty: 1, price: 25_000 },
      { id: 'ak-001', name: 'Hamburger', qty: 1, price: 79_000 },
    ])).toBeNull();
  });

  it('uses the restaurant identity returned by the cart API', () => {
    expect(determineRestaurantFromCartItems([
      {
        id: 'future-product-id',
        name: 'Món mới',
        qty: 1,
        price: 50_000,
        restaurantId: 'aloha',
      },
    ])).toBe('restaurant_2');
  });
});
