import React, { useState, useEffect, useCallback, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useCart } from "@/context/CartContext";
import { useAuth } from "@/context";
import { formatVND } from "../utils/currency";
import { createVNPayGroupUrl, getVNPayAvailability } from "../services/vnpay";
import styled from "styled-components";
import { motion, AnimatePresence } from "framer-motion";
import toast from "react-hot-toast";
import { checkoutSchema, CheckoutFormData } from "../schemas/checkoutSchema";
import { createOrderGroup, OrderGroupQuote, quoteOrderGroup } from "@/api/orderApi";
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
  const [voucherInputs, setVoucherInputs] = useState<Record<string, string>>({});
  const [appliedVoucherCodes, setAppliedVoucherCodes] = useState<Record<string, string>>({});
  const [applyingVoucherFor, setApplyingVoucherFor] = useState<string | null>(null);
  const [availableVouchers, setAvailableVouchers] = useState<Record<string, AvailableVoucher[]>>({});
  const [isLoadingVouchers, setIsLoadingVouchers] = useState(false);
  const [quote, setQuote] = useState<OrderGroupQuote | null>(null);
  const [isLoadingQuote, setIsLoadingQuote] = useState(false);
  const [quoteError, setQuoteError] = useState<string | null>(null);
  const [addresses, setAddresses] = useState<DeliveryAddress[]>([]);
  const [selectedAddressId, setSelectedAddressId] = useState('');
  const orderAttemptKey = useRef(`group-${crypto.randomUUID()}`);

  // All displayed amounts come from the same backend calculation used to save the order.
  const subtotal = quote?.subtotal ?? 0;
  const delivery = quote?.shippingFee ?? 0;
  const discount = quote?.discountAmount ?? 0;
  const total = quote?.total ?? 0;
  const multiRestaurant = (quote?.orders.length || 0) > 1;

  const requestQuote = useCallback(async (
    requestedVouchers: Record<string, string> = appliedVoucherCodes,
  ) => {
    if (items.length === 0) {
      setQuote(null);
      return null;
    }

    setIsLoadingQuote(true);
    setQuoteError(null);
    try {
      const result = await quoteOrderGroup({
        customerPhone: form.phone || user?.phone || '0000000000',
        address: form.address || 'Chưa nhập địa chỉ',
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.qty,
          selectedOptionIds: item.selectedOptionIds,
          itemNote: item.itemNote,
        })),
        paymentMethod: form.payment,
        voucherCodesByRestaurant: requestedVouchers,
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
  }, [appliedVoucherCodes, form.address, form.payment, form.phone, items, user?.phone]);

  useEffect(() => {
    void requestQuote();
  }, [requestQuote]);

  const applyVoucher = async (restaurantId: string, selectedCode = voucherInputs[restaurantId] || '') => {
    if (!user?.id) {
      toast.error('Vui lòng đăng nhập để dùng mã ưu đãi');
      return;
    }
    if (!selectedCode.trim()) {
      toast.error('Vui lòng nhập mã ưu đãi');
      return;
    }
    setApplyingVoucherFor(restaurantId);
    try {
      const nextCodes = { ...appliedVoucherCodes, [restaurantId]: selectedCode.trim().toUpperCase() };
      const result = await requestQuote(nextCodes);
      const child = result?.orders.find(order => order.restaurantId === restaurantId);
      if (!child?.voucherCode) {
        throw new Error('Mã ưu đãi không được áp dụng');
      }
      setVoucherInputs(current => ({ ...current, [restaurantId]: child.voucherCode || '' }));
      setAppliedVoucherCodes(nextCodes);
      toast.success(`Đã áp dụng ${child.voucherCode}: giảm ${formatVND(child.discountAmount)}`);
    } catch (error: any) {
      setAppliedVoucherCodes(current => {
        const next = { ...current };
        delete next[restaurantId];
        return next;
      });
      toast.error(error?.response?.data?.message || error?.message || 'Không thể áp dụng mã ưu đãi');
    } finally {
      setApplyingVoucherFor(null);
    }
  };

  useEffect(() => {
    if (!user?.id || !quote || quote.orders.length === 0) {
      setAvailableVouchers({});
      return;
    }
    let active = true;
    setIsLoadingVouchers(true);
    Promise.all(quote.orders.map(async order => [
      order.restaurantId,
      await getAvailableVouchers(order.restaurantId, order.subtotal),
    ] as const))
      .then(entries => { if (active) setAvailableVouchers(Object.fromEntries(entries)); })
      .catch(error => {
        console.error('Không thể tải mã ưu đãi', error);
        if (active) setAvailableVouchers({});
      })
      .finally(() => { if (active) setIsLoadingVouchers(false); });
    return () => { active = false; };
  }, [quote?.orders, user?.id]);

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

    setIsSubmitting(true);

    try {
      const groupRequest = {
        customerPhone: form.phone,
        address: form.address.trim(),
        note: form.note || undefined,
        paymentMethod: form.payment,
        items: items.map(item => ({
          productId: item.productId,
          name: item.name,
          quantity: item.qty,
          price: item.price,
          selectedOptionIds: item.selectedOptionIds,
          itemNote: item.itemNote,
        })),
        voucherCodesByRestaurant: appliedVoucherCodes,
      };

      if (form.payment === 'vnpay') {
        setShowVNPayModal(true);
        
        try {
          const availability = await getVNPayAvailability();
          if (!availability.enabled) {
            const error: any = new Error(availability.message);
            error.vnpayDisabled = true;
            throw error;
          }

          const created = await createOrderGroup(groupRequest, orderAttemptKey.current);
          const url = await createVNPayGroupUrl(created.id, `${orderAttemptKey.current}:vnpay`);
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
        const created = await createOrderGroup(groupRequest, orderAttemptKey.current);
        await clear();
        orderAttemptKey.current = `group-${crypto.randomUUID()}`;
        toast.success(`Đã tạo ${created.orderCount} đơn cho ${created.orderCount} nhà hàng!`);
        navigate(`/order-groups/${created.id}`, { state: { group: created } });
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
                onClick={() => {
                  if (multiRestaurant) {
                    toast('Giỏ nhiều nhà hàng hiện thanh toán khi nhận hàng để mỗi đơn được đối soát riêng.');
                    return;
                  }
                  setForm(prev => ({ ...prev, payment: 'vnpay' }));
                }}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <PaymentMethodIcon>🏦</PaymentMethodIcon>
                <PaymentMethodName>VNPay</PaymentMethodName>
                <PaymentMethodDesc>
                  {multiRestaurant ? 'Chỉ dùng khi giỏ có một nhà hàng' : 'Thanh toán trực tuyến qua VNPay'}
                </PaymentMethodDesc>
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
            {quote?.orders.map(orderQuote => {
              const vouchers = availableVouchers[orderQuote.restaurantId] || [];
              const inputCode = voucherInputs[orderQuote.restaurantId] || '';
              return (
                <OrderSummary key={orderQuote.restaurantId}>
                  <div style={{ fontWeight: 800, marginBottom: 10 }}>{orderQuote.restaurantName}</div>
                  {orderQuote.items.map(item => (
                    <SummaryRow key={`${orderQuote.restaurantId}-${item.productId}-${item.name}`}>
                      <span>{item.name} x {item.quantity}</span>
                      <span>{formatVND(item.lineTotal)}</span>
                    </SummaryRow>
                  ))}
                  <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
                    <input
                      value={inputCode}
                      onChange={event => setVoucherInputs(current => ({
                        ...current,
                        [orderQuote.restaurantId]: event.target.value.toUpperCase(),
                      }))}
                      placeholder="Mã toàn hệ thống hoặc của nhà hàng"
                      maxLength={40}
                      style={{ flex: 1, padding: '10px 12px', border: '1px solid var(--border)', borderRadius: 8 }}
                    />
                    <button
                      type="button"
                      onClick={() => void applyVoucher(orderQuote.restaurantId)}
                      disabled={applyingVoucherFor === orderQuote.restaurantId || !inputCode.trim()}
                      style={{ padding: '9px 14px', border: 0, borderRadius: 8, background: 'var(--primary)', color: '#fff', fontWeight: 700 }}
                    >
                      {applyingVoucherFor === orderQuote.restaurantId ? 'Đang kiểm tra...' : 'Áp dụng'}
                    </button>
                  </div>
                  {orderQuote.voucherCode && (
                    <small style={{ display: 'block', marginTop: 7, color: '#15803d' }}>
                      ✓ {orderQuote.voucherName || orderQuote.voucherCode} — giảm {formatVND(orderQuote.discountAmount)}
                    </small>
                  )}
                  <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                    {isLoadingVouchers && <small>Đang tải ưu đãi...</small>}
                    {!isLoadingVouchers && vouchers.length === 0 && <small>Chưa có mã phù hợp cho đơn này.</small>}
                    {vouchers.filter(voucher => voucher.eligible).map(voucher => (
                      <button
                        key={voucher.code}
                        type="button"
                        onClick={() => {
                          setVoucherInputs(current => ({ ...current, [orderQuote.restaurantId]: voucher.code }));
                          void applyVoucher(orderQuote.restaurantId, voucher.code);
                        }}
                        style={{ padding: '7px 10px', border: '1px solid #86efac', borderRadius: 8, background: '#f0fdf4', color: '#15803d', cursor: 'pointer' }}
                      >
                        {voucher.code} · {voucher.scope === 'GLOBAL' ? 'Toàn hệ thống' : 'Riêng nhà hàng'} · giảm {formatVND(voucher.estimatedDiscount)}
                      </button>
                    ))}
                  </div>
                  <div style={{ marginTop: 14, paddingTop: 10, borderTop: '1px solid var(--border)' }}>
                    <SummaryRow><span>Tạm tính</span><span>{formatVND(orderQuote.subtotal)}</span></SummaryRow>
                    <SummaryRow><span>Phí giao hàng</span><span>{formatVND(orderQuote.shippingFee)}</span></SummaryRow>
                    {orderQuote.discountAmount > 0 && <SummaryRow><span>Ưu đãi</span><span>−{formatVND(orderQuote.discountAmount)}</span></SummaryRow>}
                    <SummaryRow><span>Tổng đơn</span><span>{formatVND(orderQuote.total)}</span></SummaryRow>
                  </div>
                </OrderSummary>
              );
            })}
            {isLoadingQuote && <small style={{ color: 'var(--secondaryText)' }}>Đang tính giá từ hệ thống...</small>}
            {quoteError && <small style={{ color: '#dc2626' }}>{quoteError}</small>}
            <OrderSummary>
              <SummaryRow>
                <span>Tạm tính toàn bộ</span>
                <span>{formatVND(subtotal)}</span>
              </SummaryRow>
              <SummaryRow>
                <span>Phí giao hàng ({quote?.orders.length || 0} đơn)</span>
                <span>{formatVND(delivery)}</span>
              </SummaryRow>
              {discount > 0 && (
                <SummaryRow>
                  <span>Tổng ưu đãi</span>
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
