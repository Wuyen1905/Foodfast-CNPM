import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useOrders, Order } from "@/context/OrderContext";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context";
import { formatVND } from "../utils/currency";
import { createVNPayUrl, getVNPayAvailability } from "../services/vnpay";
import styled from "styled-components";
import { motion, AnimatePresence } from "framer-motion";
import toast from "react-hot-toast";
import { checkoutSchema, CheckoutFormData } from "../schemas/checkoutSchema";
import { notifyRestaurant, determineRestaurantFromCartItems } from "../services/restaurantNotificationService";
import { getRestaurant } from "@/api/restaurantApi";
import { OrderQuote, quoteOrder } from "@/api/orderApi";
import { DeliveryAddress, getAddresses } from "@/api/addressApi";
import { AvailableVoucher, getAvailableVouchers } from "@/api/voucherApi";

// Styled Components
const CheckoutContainer = styled.div`
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;

  @media (max-width: 480px) {
    padding: 12px;
  }
`;

const CheckoutCard = styled(motion.div)`
  background: var(--card);
  border-radius: 16px;
  padding: 32px;
  box-shadow: var(--shadow-md);
  border: 1px solid var(--border);

  @media (max-width: 480px) {
    padding: 16px;
  }
`;

const Title = styled.h2`
  text-align: center;
  margin: 0 0 32px 0;
  color: var(--text);
  font-size: 28px;
  font-weight: 700;

  @media (max-width: 480px) {
    font-size: 22px;
    margin-bottom: 20px;
  }
`;

const FormSection = styled.div`
  margin-bottom: 32px;
  overflow: visible;
`;

const SectionTitle = styled.h3`
  margin: 0 0 20px 0;
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
  border-bottom: 2px solid var(--border);
  padding-bottom: 8px;
`;

const SectionDescription = styled.p`
  margin: -10px 0 18px;
  color: var(--secondaryText);
  font-size: 14px;
  line-height: 1.5;
`;

const AccountSummary = styled.div`
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px 18px;
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--background);
  color: var(--text);

  span {
    color: var(--secondaryText);
    font-size: 13px;
  }
`;

const SavedAddressHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;

  a {
    color: var(--primary);
    font-weight: 700;
    font-size: 13px;
  }
`;

const FormGroup = styled.div`
  margin-bottom: 20px;
  overflow: visible;
  position: relative;
`;

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: var(--text);
`;

const Input = styled.input<{ hasError?: boolean }>`
  width: 100%;
  padding: 12px 16px;
  border: 2px solid ${props => props.hasError ? '#f44336' : 'var(--border)'};
  border-radius: 8px;
  background: var(--card);
  color: var(--text);
  font-size: 14px;
  transition: all 0.3s ease;
  
  &:focus {
    outline: none;
    border-color: ${props => props.hasError ? '#f44336' : 'var(--primary)'};
    box-shadow: 0 0 0 3px ${props => props.hasError ? 'rgba(244, 67, 54, 0.1)' : 'rgba(255, 102, 0, 0.1)'};
  }
`;

const ErrorMessage = styled(motion.div)`
  color: #f44336;
  font-size: 12px;
  margin-top: 4px;
`;

const PaymentMethodGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;

  @media (max-width: 480px) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
`;

const PaymentMethodCard = styled(motion.div)<{ isSelected?: boolean }>`
  padding: 16px;
  border: 2px solid ${props => props.isSelected ? 'var(--primary)' : 'var(--border)'};
  border-radius: 12px;
  cursor: pointer;
  background: ${props => props.isSelected ? 'rgba(255, 102, 0, 0.05)' : 'var(--card)'};
  transition: all 0.3s ease;
  
  &:hover {
    border-color: var(--primary);
    transform: translateY(-2px);
  }
`;

const PaymentMethodIcon = styled.div`
  font-size: 24px;
  margin-bottom: 8px;
`;

const PaymentMethodName = styled.div`
  font-weight: 600;
  color: var(--text);
  margin-bottom: 4px;
`;

const PaymentMethodDesc = styled.div`
  font-size: 12px;
  color: var(--secondaryText);
`;

const OrderSummary = styled.div`
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
`;

const SummaryRow = styled.div`
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  
  &:last-child {
    border-top: 1px solid var(--border);
    padding-top: 12px;
    margin-top: 12px;
    font-weight: 600;
    font-size: 16px;
  }
`;

const SubmitButton = styled(motion.button)<{ disabled?: boolean }>`
  width: 100%;
  padding: 16px;
  background: ${props => props.disabled ? 'var(--border)' : 'linear-gradient(135deg, var(--primary) 0%, var(--primary-light) 100%)'};
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: ${props => props.disabled ? 'not-allowed' : 'pointer'};
  transition: all 0.3s ease;
  box-shadow: var(--shadow);
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }
`;

const LoadingOverlay = styled(motion.div)`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
`;

const LoadingCard = styled(motion.div)`
  background: var(--card);
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  max-width: 400px;
  margin: 20px;
`;

const LoadingSpinner = styled(motion.div)`
  width: 50px;
  height: 50px;
  border: 4px solid var(--border);
  border-top: 4px solid var(--primary);
  border-radius: 50%;
  margin: 0 auto 20px;
`;

const VNPayQRCode = styled.div`
  width: 200px;
  height: 200px;
  background: #f5f5f5;
  border: 2px solid var(--border);
  border-radius: 12px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--secondaryText);
`;

const Checkout: React.FC = () => {
  const { user } = useAuth();
  const { addOrder } = useOrders();
  const { items, clear } = useCart();
  const navigate = useNavigate();
  
  const [form, setForm] = useState<CheckoutFormData>({
    phone: user?.phone || "",
    address: "",
    note: "",
    payment: "cod",
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showVNPayModal, setShowVNPayModal] = useState(false);
  const [vnpayDisabledMsg, setVnpayDisabledMsg] = useState<string | null>(null);
  const [voucherCode, setVoucherCode] = useState('');
  const [appliedVoucherCode, setAppliedVoucherCode] = useState('');
  const [isApplyingVoucher, setIsApplyingVoucher] = useState(false);
  const [availableVouchers, setAvailableVouchers] = useState<AvailableVoucher[]>([]);
  const [isLoadingVouchers, setIsLoadingVouchers] = useState(false);
  const [quote, setQuote] = useState<OrderQuote | null>(null);
  const [isLoadingQuote, setIsLoadingQuote] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [addresses, setAddresses] = useState<DeliveryAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState('');
  const orderAttemptKey = useRef(`order-${crypto.randomUUID()}`);

  // All displayed amounts come from the same backend calculation used to save the order.
  const subtotal = quote?.subtotal ?? 0;
  const delivery = quote?.shippingFee ?? 0;
  const discount = quote?.discountAmount ?? 0;
  const total = quote?.total ?? 0;

  const requestQuote = useCallback(async (requestedVoucher = appliedVoucherCode) => {
    if (items.length === 0) {
      setQuote(null);
      return null;
    }
    const restaurantId = determineRestaurantFromCartItems(items);
    if (!restaurantId) {
      setQuote(null);
      setQuoteError('Mỗi đơn chỉ được chứa món của một nhà hàng');
      return null;
    }

    setIsLoadingQuote(true);
    setQuoteError(null);
    try {
      const result = await quoteOrder({
        customerPhone: form.phone || user?.phone || '0000000000',
        address: form.address || 'Chưa nhập địa chỉ',
        restaurantId,
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.qty,
          selectedOptionIds: item.selectedOptionIds,
          itemNote: item.itemNote,
        })),
        paymentMethod: form.payment,
        voucherCode: requestedVoucher || undefined,
      });
      setQuote(result);
      return result;
    } catch (error: any) {
      const message = error?.response?.data?.message || 'Không thể tính lại tổng tiền';
      setQuote(null);
      setQuoteError(message);
      return null;
    } finally {
      setIsLoadingQuote(false);
    }
  }, [appliedVoucherCode, form.address, form.payment, form.phone, items, user?.email, user?.id, user?.name, user?.phone]);

  useEffect(() => {
    void requestQuote();
  }, [requestQuote]);

  const applyVoucher = async (selectedCode = voucherCode) => {
    const restaurantId = determineRestaurantFromCartItems(items);
    if (!user?.id) {
      toast.error('Vui lòng đăng nhập để dùng mã ưu đãi');
      return;
    }
    if (!restaurantId) {
      toast.error('Không xác định được nhà hàng của giỏ hàng');
      return;
    }
    if (!selectedCode.trim()) {
      toast.error('Vui lòng nhập mã ưu đãi');
      return;
    }
    setIsApplyingVoucher(true);
    try {
      const result = await requestQuote(selectedCode.trim());
      if (!result?.voucherCode) {
        throw new Error('Mã ưu đãi không được áp dụng');
      }
      setVoucherCode(result.voucherCode);
      setAppliedVoucherCode(result.voucherCode);
      toast.success(`Đã áp dụng ${result.voucherCode}: giảm ${formatVND(result.discountAmount)}`);
    } catch (error: any) {
      setAppliedVoucherCode('');
      toast.error(error?.response?.data?.message || error?.message || 'Không thể áp dụng mã ưu đãi');
    } finally {
      setIsApplyingVoucher(false);
    }
  };

  useEffect(() => {
    const restaurantId = determineRestaurantFromCartItems(items);
    if (!user?.id || !restaurantId || items.length === 0) {
      setAvailableVouchers([]);
      return;
    }
    let active = true;
    setIsLoadingVouchers(true);
    getAvailableVouchers(restaurantId, subtotal)
      .then(vouchers => { if (active) setAvailableVouchers(vouchers); })
      .catch(error => {
        console.error('Không thể tải mã ưu đãi', error);
        if (active) setAvailableVouchers([]);
      })
      .finally(() => { if (active) setIsLoadingVouchers(false); });
    return () => { active = false; };
  }, [items, subtotal, user?.id]);

  useEffect(() => {
    if (user?.phone) {
      setForm((current) => current.phone ? current : { ...current, phone: user.phone || '' });
    }
  }, [user?.phone]);

  useEffect(() => {
    if (!user?.id) return;
    getAddresses()
      .then(savedAddresses => {
        setAddresses(savedAddresses);
        const preferred = savedAddresses.find(address => address.defaultAddress) || savedAddresses[0];
        if (preferred) {
          setSelectedAddressId(preferred.id);
          setForm(current => ({
            ...current,
            phone: preferred.phone || current.phone,
            address: preferred.addressLine,
          }));
        }
      })
      .catch(() => {
        setAddresses([]);
      });
  }, [user?.id]);

  const chooseSavedAddress = (addressId: string) => {
    setSelectedAddressId(addressId);
    const selected = addresses.find(address => address.id === addressId);
    if (!selected) return;
    setForm(current => ({
      ...current,
      phone: selected.phone,
      address: selected.addressLine,
    }));
    setErrors(current => ({ ...current, phone: '', address: '' }));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
    
    // Clear error when user starts typing
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = async (): Promise<boolean> => {
    try {
      await checkoutSchema.validate(form, { abortEarly: false });
      setErrors({});
      return true;
    } catch (err: any) {
      const validationErrors: Record<string, string> = {};
      err.inner.forEach((error: any) => {
        validationErrors[error.path] = error.message;
      });
      setErrors(validationErrors);
      return false;
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!await validateForm()) {
      toast.error("Vui lòng kiểm tra lại thông tin!");
      return;
    }

    if (items.length === 0) {
      toast.error("Giỏ hàng trống!");
      return;
    }

    const confirmedQuote = await requestQuote();
    if (!confirmedQuote) {
      toast.error(quoteError || 'Không thể xác nhận giá đơn hàng. Vui lòng thử lại.');
      return;
    }

    const restaurantId = determineRestaurantFromCartItems(items);
    if (!restaurantId) {
      toast.error("Mỗi đơn chỉ được chứa món của một nhà hàng. Vui lòng kiểm tra lại giỏ hàng.");
      navigate('/cart');
      return;
    }

    try {
      const restaurant = await getRestaurant(restaurantId);
      if (!restaurant.isActive) {
        toast.error('Nhà hàng hiện không hoạt động');
        return;
      }
      if (!restaurant.acceptingOrders) {
        toast.error(
          `Nhà hàng đang tạm ngừng nhận đơn${restaurant.pauseReason ? `: ${restaurant.pauseReason}` : ''}`
        );
        return;
      }
      if (!restaurant.openNow) {
        toast.error(`Nhà hàng đang đóng cửa. Giờ phục vụ: ${restaurant.openingTime}–${restaurant.closingTime}`);
        return;
      }
    } catch (error: any) {
      toast.error(error?.response?.data?.message || 'Không thể kiểm tra trạng thái nhà hàng');
      return;
    }
    
    setIsSubmitting(true);

    try {
      if (form.payment === 'vnpay') {
        setShowVNPayModal(true);
        
        try {
          const availability = await getVNPayAvailability();
          if (!availability.enabled) {
            const error: any = new Error(availability.message);
            error.vnpayDisabled = true;
            throw error;
          }

          const newOrder: Order = {
            id: Date.now().toString(),
            name: user?.name || 'Khách hàng',
            phone: form.phone,
            customerEmail: user?.email || '',
            address: form.address.trim(),
            items: items.map(item => ({
              productId: item.productId,
              name: item.name,
              qty: item.qty,
              price: item.price,
              selectedOptionIds: item.selectedOptionIds,
              itemNote: item.itemNote,
            })),
            total: confirmedQuote.total,
            status: "Pending" as const,
            paymentMethod: 'vnpay' as const,
            paymentStatus: 'pending' as const,
            restaurantId: restaurantId || undefined,
            createdAt: Date.now(),
            updatedAt: Date.now(),
            internalNotes: form.note || undefined,
            voucherCode: confirmedQuote.voucherCode,
          };

          // Persist first. The gateway callback always reconciles against this
          // server-side order and its server-calculated total.
          const created = await addOrder(newOrder, orderAttemptKey.current);
          const url = await createVNPayUrl(created.id, `${orderAttemptKey.current}:vnpay`);
          window.location.href = url;
        } catch (error: any) {
          console.error('[VNPay] Payment error:', error);
          if (error.vnpayDisabled) {
            setVnpayDisabledMsg(error.message);
          } else {
            toast.error(
              error?.response?.data?.message
              || 'Không thể tạo URL thanh toán VNPay. Vui lòng thử lại.'
            );
          }
        }
      } else {
        // Handle other payment methods (COD, etc.)
        const orderId = Date.now().toString();
        const newOrder: Order = {
          id: orderId,
          name: user?.name || 'Khách hàng',
          phone: form.phone,
          customerEmail: user?.email || '',
          address: form.address.trim(),
          items: items.map(item => ({
              productId: item.productId,
              name: item.name,
              qty: item.qty,
              price: item.price,
              selectedOptionIds: item.selectedOptionIds,
              itemNote: item.itemNote,
          })),
          total: confirmedQuote.total,
          status: "Pending" as const,
          paymentMethod: form.payment as any,
          paymentStatus: 'pending',
          restaurantId: restaurantId || undefined,
          createdAt: Date.now(),
          updatedAt: Date.now(),
          internalNotes: form.note || undefined,
          voucherCode: confirmedQuote.voucherCode,
        };
        
        const created = await addOrder(newOrder, orderAttemptKey.current);

        // Notify restaurant about the new order
        if (restaurantId) {
          notifyRestaurant(created).catch(err => {
            console.error('Failed to notify restaurant:', err);
            // Don't show error to user, order was still created successfully
          });
        }

        const cartItemsSnapshot = items;
        await clear();
        orderAttemptKey.current = `order-${crypto.randomUUID()}`;
        toast.success("Bạn đã đặt hàng thành công!");
        navigate(`/order-confirmation/${created.id}`, { state: { order: created, cartItems: cartItemsSnapshot } });
      }
    } catch (error: any) {
      toast.error(error?.response?.data?.message || "Có lỗi xảy ra khi đặt hàng!");
    } finally {
      setIsSubmitting(false);
      setShowVNPayModal(false);
    }
  };

  return (
    <CheckoutContainer>
      <CheckoutCard
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
      >
        <Title>Xác nhận đơn hàng</Title>
        
        <form onSubmit={handleSubmit}>
          {/* Delivery information */}
          <FormSection>
            <SectionTitle>Thông tin giao hàng</SectionTitle>
            <SectionDescription>
              Tên và email được lấy từ tài khoản. Bạn chỉ cần kiểm tra số điện thoại và nhập địa chỉ nhận hàng.
            </SectionDescription>

            <AccountSummary>
              <strong>{user?.name || 'Khách hàng'}</strong>
              <span>{user?.email || user?.username || 'Tài khoản đang đăng nhập'}</span>
            </AccountSummary>

            {addresses.length > 0 && (
              <FormGroup>
                <SavedAddressHeader>
                  <Label style={{ margin: 0 }}>Chọn địa chỉ đã lưu</Label>
                  <Link to="/addresses">Quản lý sổ địa chỉ</Link>
                </SavedAddressHeader>
                <Input
                  as="select"
                  value={selectedAddressId}
                  onChange={event => chooseSavedAddress(event.target.value)}
                >
                  <option value="">Tự nhập địa chỉ khác</option>
                  {addresses.map(address => (
                    <option key={address.id} value={address.id}>
                      {address.label}{address.defaultAddress ? ' (mặc định)' : ''} — {address.addressLine}
                    </option>
                  ))}
                </Input>
              </FormGroup>
            )}

            <FormGroup>
              <Label>Số điện thoại *</Label>
              <Input 
                name="phone" 
                value={form.phone} 
                onChange={handleChange} 
                placeholder="Nhập số điện thoại"
                type="tel"
                hasError={!!errors.phone}
              />
              <AnimatePresence>
                {errors.phone && (
                  <ErrorMessage
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                  >
                    {errors.phone}
                  </ErrorMessage>
                )}
              </AnimatePresence>
            </FormGroup>

            <FormGroup>
              <Label>Địa chỉ giao hàng *</Label>
              <Input 
                name="address"
                value={form.address}
                onChange={(event) => { setSelectedAddressId(''); handleChange(event); }}
                placeholder="Nhập địa chỉ hoặc hướng dẫn giao hàng theo cách của bạn"
                as="textarea"
                rows={3}
                hasError={!!errors.address}
              />
              <AnimatePresence>
                {errors.address && (
                  <ErrorMessage
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                  >
                    {errors.address}
                  </ErrorMessage>
                )}
              </AnimatePresence>
            </FormGroup>

            <FormGroup>
              <Label>Ghi chú cho đơn hàng</Label>
              <Input 
                name="note" 
                value={form.note} 
                onChange={handleChange} 
                placeholder="Ví dụ: gọi trước khi giao, giao tại bảo vệ... (tùy chọn)"
                as="textarea"
                rows={3}
                hasError={!!errors.note}
              />
              <AnimatePresence>
                {errors.note && (
                  <ErrorMessage
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                  >
                    {errors.note}
                  </ErrorMessage>
                )}
              </AnimatePresence>
            </FormGroup>
          </FormSection>

          {/* Payment Methods */}
          <FormSection>
            <SectionTitle>Chọn cách thanh toán</SectionTitle>
            <SectionDescription>
              Bước này vẫn cần để hệ thống biết bạn trả tiền khi nhận hàng hay thanh toán trực tuyến qua VNPay.
            </SectionDescription>
            
            <PaymentMethodGrid>
              <PaymentMethodCard
                isSelected={form.payment === 'cod'}
                onClick={() => { setForm(prev => ({ ...prev, payment: 'cod' })); setVnpayDisabledMsg(null); }}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <PaymentMethodIcon>💵</PaymentMethodIcon>
                <PaymentMethodName>Thanh toán khi nhận hàng</PaymentMethodName>
                <PaymentMethodDesc>Trả tiền mặt khi giao hàng</PaymentMethodDesc>
              </PaymentMethodCard>

              <PaymentMethodCard
                isSelected={form.payment === 'vnpay'}
                onClick={() => setForm(prev => ({ ...prev, payment: 'vnpay' }))}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <PaymentMethodIcon>🏦</PaymentMethodIcon>
                <PaymentMethodName>VNPay</PaymentMethodName>
                <PaymentMethodDesc>Thanh toán trực tuyến qua VNPay</PaymentMethodDesc>
              </PaymentMethodCard>
            </PaymentMethodGrid>

            {vnpayDisabledMsg && (
              <div style={{
                marginTop: 12,
                padding: '14px 18px',
                background: '#fff8e1',
                border: '1px solid #ffe082',
                borderRadius: 8,
                color: '#5d4037',
                fontSize: 14,
                lineHeight: 1.5,
              }}>
                ⚠️ {vnpayDisabledMsg}
                <button
                  type="button"
                  onClick={() => { setForm(prev => ({ ...prev, payment: 'cod' })); setVnpayDisabledMsg(null); }}
                  style={{
                    display: 'block',
                    marginTop: 10,
                    padding: '8px 18px',
                    background: '#FF6600',
                    color: '#fff',
                    border: 'none',
                    borderRadius: 6,
                    cursor: 'pointer',
                    fontWeight: 600,
                    fontSize: 13,
                  }}
                >
                  Dùng Thanh toán khi nhận hàng (COD)
                </button>
              </div>
            )}
          </FormSection>

          {/* Order Summary */}
          <FormSection>
            <SectionTitle>Tóm tắt đơn hàng</SectionTitle>
            <div style={{ marginBottom: 16 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 700 }}>Mã ưu đãi</label>
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  value={voucherCode}
                  onChange={(event) => {
                    setVoucherCode(event.target.value.toUpperCase());
                    if (appliedVoucherCode) setAppliedVoucherCode('');
                  }}
                  placeholder="Ví dụ: WELCOME20"
                  maxLength={40}
                  style={{
                    flex: 1, padding: '12px 14px', border: '1px solid var(--border)',
                    borderRadius: 8, background: 'var(--card)', color: 'var(--text)',
                  }}
                />
                <button
                  type="button"
                  onClick={() => void applyVoucher()}
                  disabled={isApplyingVoucher || !voucherCode.trim()}
                  style={{
                    padding: '10px 18px', border: 'none', borderRadius: 8,
                    background: 'var(--primary)', color: '#fff', cursor: 'pointer', fontWeight: 700,
                  }}
                >
                  {isApplyingVoucher ? 'Đang kiểm tra...' : 'Áp dụng'}
                </button>
              </div>
              {quote?.voucherCode && (
                <small style={{ display: 'block', marginTop: 8, color: '#15803d' }}>
                  ✓ {quote.voucherName || quote.voucherCode} — giảm {formatVND(quote.discountAmount)}
                </small>
              )}
              <div style={{ marginTop: 14 }}>
                <div style={{ fontWeight: 700, marginBottom: 8 }}>Ưu đãi dành cho đơn này</div>
                {isLoadingVouchers && (
                  <small style={{ color: 'var(--secondaryText)' }}>Đang tải ưu đãi...</small>
                )}
                {!isLoadingVouchers && availableVouchers.length === 0 && (
                  <small style={{ color: 'var(--secondaryText)' }}>
                    Hiện chưa có mã ưu đãi phù hợp với nhà hàng này.
                  </small>
                )}
                <div style={{ display: 'grid', gap: 10 }}>
                  {availableVouchers.map(voucher => {
                    const applied = appliedVoucherCode === voucher.code;
                    const benefit = voucher.discountType === 'PERCENT'
                      ? `Giảm ${voucher.discountValue}%${voucher.maxDiscount > 0 ? `, tối đa ${formatVND(voucher.maxDiscount)}` : ''}`
                      : `Giảm ${formatVND(voucher.discountValue)}`;
                    return (
                      <div key={voucher.code} style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12,
                        padding: 12, border: `1px solid ${voucher.eligible ? '#86efac' : 'var(--border)'}`,
                        borderRadius: 10, background: voucher.eligible ? '#f0fdf4' : 'var(--background)',
                      }}>
                        <div>
                          <div><strong>{voucher.code}</strong> · {voucher.name}</div>
                          <div style={{ fontSize: 13, marginTop: 3 }}>{benefit} · Đơn từ {formatVND(voucher.minOrderAmount)}</div>
                          <div style={{ fontSize: 12, marginTop: 3, color: voucher.eligible ? '#15803d' : '#b45309' }}>
                            {voucher.eligible
                              ? `Đơn này dự kiến giảm ${formatVND(voucher.estimatedDiscount)}`
                              : `Mua thêm ${formatVND(voucher.amountNeeded)} để áp dụng`}
                          </div>
                        </div>
                        <button
                          type="button"
                          disabled={!voucher.eligible || isApplyingVoucher || applied}
                          onClick={() => {
                            setVoucherCode(voucher.code);
                            void applyVoucher(voucher.code);
                          }}
                          style={{
                            padding: '8px 12px', border: 0, borderRadius: 8,
                            background: applied ? '#15803d' : voucher.eligible ? 'var(--primary)' : '#d1d5db',
                            color: '#fff', fontWeight: 700, cursor: voucher.eligible ? 'pointer' : 'not-allowed',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          {applied ? 'Đã áp dụng' : 'Áp dụng'}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>
              {isLoadingQuote && (
                <small style={{ display: 'block', marginTop: 8, color: 'var(--secondaryText)' }}>
                  Đang tính giá từ hệ thống...
                </small>
              )}
              {quoteError && (
                <small style={{ display: 'block', marginTop: 8, color: '#dc2626' }}>
                  {quoteError}
                </small>
              )}
            </div>
            
            <OrderSummary>
              {items.map((item, index) => (
                <SummaryRow key={item.id}>
                  <span>
                    {item.name} x {item.qty}
                    {item.optionSummary && <small style={{ display: 'block', opacity: 0.75 }}>{item.optionSummary}</small>}
                    {item.itemNote && <small style={{ display: 'block', opacity: 0.75 }}>Ghi chú: {item.itemNote}</small>}
                  </span>
                  <span>{formatVND(quote?.items[index]?.lineTotal ?? 0)}</span>
                </SummaryRow>
              ))}
              <SummaryRow>
                <span>Tạm tính</span>
                <span>{formatVND(subtotal)}</span>
              </SummaryRow>
              <SummaryRow>
                <span>Phí giao hàng</span>
                <span>{formatVND(delivery)}</span>
              </SummaryRow>
              {discount > 0 && (
                <SummaryRow>
                  <span>Ưu đãi ({quote?.voucherCode})</span>
                  <span style={{ color: '#15803d' }}>−{formatVND(discount)}</span>
                </SummaryRow>
              )}
              <SummaryRow>
                <span>Tổng cộng</span>
                <span>{formatVND(total)}</span>
              </SummaryRow>
            </OrderSummary>
          </FormSection>

          <SubmitButton
            type="submit"
            disabled={isSubmitting || isLoadingQuote || !quote || items.length === 0}
            whileHover={{ scale: isSubmitting ? 1 : 1.02 }}
            whileTap={{ scale: isSubmitting ? 1 : 0.98 }}
          >
            {isSubmitting ? 'Đang xử lý...' : 'Đặt hàng'}
          </SubmitButton>
        </form>
      </CheckoutCard>

      {/* VNPay Loading Modal */}
      <AnimatePresence>
        {showVNPayModal && (
          <LoadingOverlay
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
          >
            <LoadingCard
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
            >
              <LoadingSpinner
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
              />
              <h3>Đang chuyển hướng đến VNPay...</h3>
              <p>Vui lòng chờ trong giây lát</p>
              <VNPayQRCode>
                Cổng thanh toán VNPay<br />
                (Môi trường Sandbox)
              </VNPayQRCode>
            </LoadingCard>
          </LoadingOverlay>
        )}
      </AnimatePresence>
    </CheckoutContainer>
  );
};

export default Checkout;
