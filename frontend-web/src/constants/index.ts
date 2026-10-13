// Application Constants
export const APP_CONFIG = {
  NAME: 'FoodFast',
  VERSION: '1.0.0',
  DEFAULT_CURRENCY: 'VND',
  SUPPORTED_CURRENCIES: ['VND', 'USD'],
} as const;

// API Configuration
export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || '/api',
  TIMEOUT: 10000,
  RETRY_ATTEMPTS: 3,
} as const;

// UI Configuration
export const UI_CONFIG = {
  ANIMATION_DURATION: 300,
  TOAST_DURATION: 3000,
  SKELETON_DURATION: 1500,
  DEBOUNCE_DELAY: 500,
} as const;

// Validation Rules
export const VALIDATION_RULES = {
  NAME: {
    MIN_LENGTH: 2,
    MAX_LENGTH: 50,
    PATTERN: /^[\p{L}\s'-]+$/u,
  },
  EMAIL: {
    PATTERN: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  },
  PHONE: {
    PATTERN: /^(0[0-9]{9}|\+84[0-9]{9})$/,
    MIN_LENGTH: 10,
    MAX_LENGTH: 11,
  },
} as const;

// Theme Configuration
export const THEME_CONFIG = {
  COLORS: {
    PRIMARY: '#ff6600',
    SECONDARY: '#ff8c00',
    SUCCESS: '#28a745',
    WARNING: '#ffc107',
    ERROR: '#dc3545',
    INFO: '#17a2b8',
  },
  BREAKPOINTS: {
    MOBILE: '768px',
    TABLET: '1024px',
    DESKTOP: '1200px',
  },
} as const;

// Backward-compatible export; the canonical status model lives in types/status.ts.
export { ORDER_STATUSES as ORDER_STATUS } from '@/types/status';

// Payment Methods
export const PAYMENT_METHODS = {
  VNPAY: 'VNPay',
  CASH: 'Cash',
} as const;

// Local Storage Keys
export const STORAGE_KEYS = {
  THEME: 'foodfast-theme',
  CART: 'foodfast-cart',
  USER: 'foodfast-user',
} as const;
