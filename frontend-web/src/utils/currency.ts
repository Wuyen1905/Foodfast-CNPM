import { APP_CONFIG } from "../constants";

// 🔹 Định dạng VNĐ chuẩn
export const formatVND = (amount: number): string => {
  if (typeof amount !== "number" || isNaN(amount)) return "0 ₫";
  return `${new Intl.NumberFormat("vi-VN").format(amount)} ₫`;
};

// 🔹 Định dạng số có dấu phẩy, không kèm ký hiệu tiền
export const formatVNDNumber = (amount: number): string => {
  if (typeof amount !== "number" || isNaN(amount)) return "0";
  return new Intl.NumberFormat("vi-VN").format(amount);
};

// 🔹 Cho phép đổi ký hiệu tuỳ loại tiền
export const formatCurrency = (
  amount: number,
  currency: string = APP_CONFIG.DEFAULT_CURRENCY,
  symbol: string = "₫"
): string => {
  if (typeof amount !== "number" || isNaN(amount)) return `0 ${symbol}`;
  const formatted = new Intl.NumberFormat("vi-VN").format(amount);
  return `${formatted} ${symbol}`;
};

// 🔹 Chuyển từ chuỗi tiền tệ sang số
export const parseCurrency = (currencyString: string): number => {
  if (!currencyString) return 0;
  const normalized = currencyString.replace(/\D/g, "");
  const parsed = parseFloat(normalized);
  return isNaN(parsed) ? 0 : parsed;
};

// Tổng tiền FoodFast: tiền món + phí giao hàng - ưu đãi.
export const calculateTotal = (
  subtotal: number,
  shippingFee: number = 0,
  discount: number = 0,
): number => {
  return Math.max(0, subtotal + shippingFee - discount);
};

// 🔹 Định dạng tổng đơn hàng với chi tiết
export const formatOrderTotal = (order: {
  subtotal: number;
  shippingFee?: number;
  discount?: number;
}) => {
  const { subtotal, shippingFee = 0, discount = 0 } = order;
  const total = Math.max(0, subtotal + shippingFee - discount);

  return {
    subtotal: formatVND(subtotal),
    shippingFee: formatVND(shippingFee),
    discount: formatVND(discount),
    total: formatVND(total),
    totalNumber: total,
  };
};
