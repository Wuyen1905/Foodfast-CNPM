import { describe, expect, it } from 'vitest';
import { Product as ApiProduct } from '@/api/productApi';
import { transformProduct } from '@/services/menuService';

describe('menuService product mapping', () => {
  it('keeps the customizable flag so option products open their detail flow', () => {
    const apiProduct: ApiProduct = {
      id: 'ak-1',
      name: 'Hamburger',
      price: 79_000,
      category: 'Món chính',
      imageUrl: '/images/hamburger.jpg',
      restaurant: 'Aloha',
      available: true,
      customizable: true,
    };

    expect(transformProduct(apiProduct)).toMatchObject({
      id: 'ak-1',
      customizable: true,
      image: '/images/hamburger.jpg',
    });
  });
});
